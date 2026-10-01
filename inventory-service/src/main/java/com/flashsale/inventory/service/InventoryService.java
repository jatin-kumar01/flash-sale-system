package com.flashsale.inventory.service;

import com.flashsale.common.event.InventoryEvent;
import com.flashsale.common.exception.InsufficientStockException;
import com.flashsale.common.exception.InvalidRequestException;
import com.flashsale.common.exception.ResourceNotFoundException;
import com.flashsale.inventory.dto.InitializeInventoryRequest;
import com.flashsale.inventory.dto.InventoryReservationRequest;
import com.flashsale.inventory.dto.InventoryResponse;
import com.flashsale.inventory.entity.Inventory;
import com.flashsale.inventory.kafka.InventoryProducer;
import com.flashsale.inventory.redis.RedisInventoryManager;
import com.flashsale.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RedisInventoryManager redisInventoryManager;
    private final InventoryProducer inventoryProducer;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.inventory.lock-wait-seconds:3}")
    private long lockWaitSeconds;

    @Value("${app.inventory.lock-lease-seconds:10}")
    private long lockLeaseSeconds;

    private static final String PRODUCT_LOCK_PREFIX = "lock:inventory:";
    private static final String EVENT_PROCESSED_PREFIX = "inventory:event:";
    private static final long EVENT_MARKER_TTL_HOURS = 48;

    public InventoryResponse reserveStock(InventoryReservationRequest request) {
        Long productId = request.getProductId();
        int quantity = request.getQuantity();

        RedisInventoryManager.ReservationResult result =
                redisInventoryManager.reserveStock(productId, quantity);

        if (result == RedisInventoryManager.ReservationResult.NOT_INITIALIZED) {
            log.info(
                    "Redis cache uninitialized for productId: {}. Acquiring distributed lock.",
                    productId
            );

            syncStockFromDbToRedisWithLock(productId);
            result = redisInventoryManager.reserveStock(productId, quantity);
        }

        if (result == RedisInventoryManager.ReservationResult.INSUFFICIENT_STOCK) {
            log.warn(
                    "Stock reservation failed for productId: {}, requested: {}",
                    productId,
                    quantity
            );

            throw new InsufficientStockException(
                    "Insufficient stock for product ID: " + productId
            );
        }

        if (result != RedisInventoryManager.ReservationResult.SUCCESS) {
            throw new InvalidRequestException(
                    "Failed to reserve stock for product ID: " + productId
            );
        }

        try {
            inventoryRepository.reserveStockDirect(productId, quantity);
        } catch (Exception ex) {
            log.warn("DB reserveStockDirect exception for productId {}: {}", productId, ex.getMessage());
        }

        InventoryEvent event = InventoryEvent.builder()
                .productId(productId)
                .quantity(quantity)
                .orderReference(request.getOrderReference())
                .eventType("INVENTORY_RESERVED")
                .occurredAt(Instant.now())
                .build();

        inventoryProducer.sendInventoryReservedEvent(event);

        Integer available = redisInventoryManager.getAvailableStock(productId);
        Integer locked = redisInventoryManager.getLockedStock(productId);

        return InventoryResponse.fromCache(productId, available, locked);
    }

    public void releaseStock(InventoryReservationRequest request) {
        Long productId = request.getProductId();
        int quantity = request.getQuantity();
        String orderReference = request.getOrderReference();

        String processedKey =
                EVENT_PROCESSED_PREFIX + "RELEASE:" + orderReference;

        String lockKey =
                "lock:inventory:event:RELEASE:" + orderReference;

        RLock eventLock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = eventLock.tryLock(
                    lockWaitSeconds,
                    lockLeaseSeconds,
                    TimeUnit.SECONDS
            );

            if (!acquired) {
                throw new InvalidRequestException(
                        "Unable to acquire inventory event lock. Please retry."
                );
            }

            try {
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(processedKey))) {
                    log.info(
                            "Duplicate inventory release event ignored for order: {}",
                            orderReference
                    );
                    return;
                }

                RedisInventoryManager.ReleaseResult result =
                        redisInventoryManager.releaseStock(productId, quantity);

                if (result == RedisInventoryManager.ReleaseResult.NOT_INITIALIZED || result == RedisInventoryManager.ReleaseResult.INVALID_QUANTITY) {
                    log.warn(
                            "Redis stock release returned {}. Releasing through DB fallback for productId: {}.",
                            result,
                            productId
                    );

                    int updatedRows =
                            inventoryRepository.releaseStockDirect(productId, quantity);

                    if (updatedRows == 0) {
                        log.warn(
                                "DB release direct updated 0 rows for productId: {}, quantity: {}",
                                productId,
                                quantity
                        );
                    }

                    syncStockFromDbToRedisWithLock(productId);
                }

                stringRedisTemplate.opsForValue().set(
                        processedKey,
                        "1",
                        EVENT_MARKER_TTL_HOURS,
                        TimeUnit.HOURS
                );

                InventoryEvent event = InventoryEvent.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .orderReference(orderReference)
                        .eventType("INVENTORY_RELEASED")
                        .occurredAt(Instant.now())
                        .build();

                inventoryProducer.sendInventoryReleasedEvent(event);

                log.info(
                        "Stock released successfully for order: {}, productId: {}, quantity: {}",
                        orderReference,
                        productId,
                        quantity
                );

            } finally {
                if (eventLock.isHeldByCurrentThread()) {
                    eventLock.unlock();
                }
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InvalidRequestException(
                    "Interrupted while processing inventory release"
            );
        }
    }

    @Transactional
    public void settleOrderDeduction(
            Long productId,
            int quantity,
            String orderReference
    ) {
        String processedKey =
                EVENT_PROCESSED_PREFIX + "PAID:" + orderReference;

        String lockKey =
                "lock:inventory:event:PAID:" + orderReference;

        RLock eventLock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = eventLock.tryLock(
                    lockWaitSeconds,
                    lockLeaseSeconds,
                    TimeUnit.SECONDS
            );

            if (!acquired) {
                throw new InvalidRequestException(
                        "Unable to acquire payment settlement lock. Please retry."
                );
            }

            try {
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(processedKey))) {
                    log.info(
                            "Duplicate payment settlement ignored for order: {}",
                            orderReference
                    );
                    return;
                }

                Integer lockedStock =
                        redisInventoryManager.getLockedStock(productId);

                if (lockedStock == null) {
                    syncStockFromDbToRedisWithLock(productId);
                    lockedStock = redisInventoryManager.getLockedStock(productId);
                }

                int updatedRows = inventoryRepository.deductLockedStockDirect(productId, quantity);
                if (updatedRows == 0) {
                    updatedRows = inventoryRepository.deductStockDirect(productId, quantity);
                }
                if (updatedRows == 0) {
                    updatedRows = inventoryRepository.deductStockFallback(productId, quantity);
                }

                if (updatedRows == 0) {
                    throw new InvalidRequestException(
                            "Inventory settlement failed for order: " + orderReference
                    );
                }

                redisInventoryManager.deductLockedStock(
                        productId,
                        quantity
                );

                stringRedisTemplate.opsForValue().set(
                        processedKey,
                        "1",
                        EVENT_MARKER_TTL_HOURS,
                        TimeUnit.HOURS
                );

                log.info(
                        "Successfully settled inventory for order: {}, productId: {}, quantity: {}",
                        orderReference,
                        productId,
                        quantity
                );

            } finally {
                if (eventLock.isHeldByCurrentThread()) {
                    eventLock.unlock();
                }
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InvalidRequestException(
                    "Interrupted while processing payment settlement"
            );
        }
    }

    @Transactional
    public InventoryResponse replenishStock(
            Long productId,
            int additionalStock
    ) {
        if (additionalStock <= 0) {
            throw new InvalidRequestException(
                    "Replenishment quantity must be greater than zero"
            );
        }

        String lockKey = PRODUCT_LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                    lockWaitSeconds,
                    lockLeaseSeconds,
                    TimeUnit.SECONDS
            );

            if (!acquired) {
                throw new InvalidRequestException(
                        "Unable to acquire lock for inventory replenishment. Please retry."
                );
            }

            try {
                Inventory inventory = inventoryRepository
                        .findByProductId(productId)
                        .orElseGet(() -> Inventory.builder()
                                .productId(productId)
                                .totalStock(0)
                                .availableStock(0)
                                .lockedStock(0)
                                .build());

                inventory.setTotalStock(
                        inventory.getTotalStock() + additionalStock
                );

                inventory.setAvailableStock(
                        inventory.getAvailableStock() + additionalStock
                );

                Inventory saved = inventoryRepository.save(inventory);

                redisInventoryManager.prewarmStock(
                        productId,
                        saved.getAvailableStock(),
                        saved.getLockedStock()
                );

                InventoryEvent event = InventoryEvent.builder()
                        .productId(productId)
                        .quantity(additionalStock)
                        .orderReference(
                                "REPLENISHMENT-" + System.currentTimeMillis()
                        )
                        .eventType("STOCK_REPLENISHED")
                        .occurredAt(Instant.now())
                        .build();

                inventoryProducer.sendStockReplenishedEvent(event);

                return InventoryResponse.fromEntity(saved, true);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InvalidRequestException(
                    "Thread interrupted while waiting for replenishment lock"
            );
        }
    }

    @Transactional
    public InventoryResponse initializeInventory(InitializeInventoryRequest request) {
        Long productId = request.getProductId();
        Integer initialStock = request.getInitialStock();

        if (productId == null) {
            throw new InvalidRequestException("Product ID cannot be null");
        }
        if (initialStock == null || initialStock <= 0) {
            throw new InvalidRequestException("Initial stock must be greater than zero");
        }

        String lockKey = PRODUCT_LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                    lockWaitSeconds,
                    lockLeaseSeconds,
                    TimeUnit.SECONDS
            );

            if (!acquired) {
                throw new InvalidRequestException(
                        "Unable to acquire lock for inventory initialization. Please retry."
                );
            }

            try {
                java.util.Optional<Inventory> existingOpt = inventoryRepository.findByProductId(productId);
                if (existingOpt.isPresent()) {
                    Inventory existing = existingOpt.get();
                    log.info(
                            "Inventory record already exists for productId: {}. Returning existing inventory without modifying stock.",
                            productId
                    );

                    redisInventoryManager.prewarmStock(
                            productId,
                            existing.getAvailableStock(),
                            existing.getLockedStock()
                    );

                    return InventoryResponse.fromEntity(existing, false);
                }

                Inventory inventory = Inventory.builder()
                        .productId(productId)
                        .totalStock(initialStock)
                        .availableStock(initialStock)
                        .lockedStock(0)
                        .build();

                Inventory saved = inventoryRepository.save(inventory);

                redisInventoryManager.prewarmStock(
                        productId,
                        saved.getAvailableStock(),
                        saved.getLockedStock()
                );

                log.info(
                        "Successfully initialized inventory for productId: {}, initialStock: {}",
                        productId,
                        initialStock
                );

                return InventoryResponse.fromEntity(saved, false);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InvalidRequestException(
                    "Thread interrupted while waiting for initialization lock"
            );
        }
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        Integer available =
                redisInventoryManager.getAvailableStock(productId);

        Integer locked =
                redisInventoryManager.getLockedStock(productId);

        if (available != null && locked != null) {
            return InventoryResponse.fromCache(
                    productId,
                    available,
                    locked
            );
        }

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory",
                                "productId",
                                productId
                        )
                );

        redisInventoryManager.prewarmStock(
                productId,
                inventory.getAvailableStock(),
                inventory.getLockedStock()
        );

        return InventoryResponse.fromEntity(
                inventory,
                false
        );
    }

    private void syncStockFromDbToRedisWithLock(Long productId) {
        String lockKey = PRODUCT_LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                    lockWaitSeconds,
                    lockLeaseSeconds,
                    TimeUnit.SECONDS
            );

            if (!acquired) {
                log.warn(
                        "Could not acquire lock to pre-warm productId: {}",
                        productId
                );
                return;
            }

            try {
                if (redisInventoryManager.hasKey(productId)) {
                    return;
                }

                Inventory inventory = inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Inventory",
                                        "productId",
                                        productId
                                )
                        );

                redisInventoryManager.prewarmStock(
                        productId,
                        inventory.getAvailableStock(),
                        inventory.getLockedStock()
                );

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error(
                    "Interrupted during cache warming for product: {}",
                    productId,
                    ex
            );
        }
    }
}