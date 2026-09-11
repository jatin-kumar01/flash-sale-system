package com.flashsale.inventory.service;

import com.flashsale.common.event.InventoryEvent;
import com.flashsale.common.exception.InsufficientStockException;
import com.flashsale.common.exception.InvalidRequestException;
import com.flashsale.common.exception.ResourceNotFoundException;
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

    @Value("${app.inventory.lock-wait-seconds:3}")
    private long lockWaitSeconds;

    @Value("${app.inventory.lock-lease-seconds:10}")
    private long lockLeaseSeconds;

    private static final String PRODUCT_LOCK_PREFIX = "lock:inventory:";

    public InventoryResponse reserveStock(InventoryReservationRequest request) {
        Long productId = request.getProductId();
        int quantity = request.getQuantity();

        // 1. Attempt atomic Lua reservation in Redis
        RedisInventoryManager.ReservationResult result = redisInventoryManager.reserveStock(productId, quantity);

        // 2. Handle cold start / uninitialized Redis cache
        if (result == RedisInventoryManager.ReservationResult.NOT_INITIALIZED) {
            log.info("Redis cache uninitialized for productId: {}. Acquiring distributed lock to synchronize from DB.", productId);
            syncStockFromDbToRedisWithLock(productId);
            result = redisInventoryManager.reserveStock(productId, quantity);
        }

        if (result == RedisInventoryManager.ReservationResult.INSUFFICIENT_STOCK) {
            log.warn("Stock reservation failed due to insufficient stock for productId: {}, requested: {}", productId, quantity);
            throw new InsufficientStockException("Insufficient stock for product ID: " + productId);
        }

        if (result != RedisInventoryManager.ReservationResult.SUCCESS) {
            throw new InvalidRequestException("Failed to reserve stock for product ID: " + productId);
        }

        // 3. Publish asynchronous reservation event to Kafka
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

        RedisInventoryManager.ReleaseResult result = redisInventoryManager.releaseStock(productId, quantity);

        if (result == RedisInventoryManager.ReleaseResult.NOT_INITIALIZED) {
            log.warn("Redis stock was not initialized during release for productId: {}. Releasing directly in DB.", productId);
            inventoryRepository.releaseStockDirect(productId, quantity);
        }

        InventoryEvent event = InventoryEvent.builder()
                .productId(productId)
                .quantity(quantity)
                .orderReference(request.getOrderReference())
                .eventType("INVENTORY_RELEASED")
                .occurredAt(Instant.now())
                .build();

        inventoryProducer.sendInventoryReleasedEvent(event);

        log.info("Stock released successfully for order: {}, productId: {}, quantity: {}",
                request.getOrderReference(), productId, quantity);
    }

    @Transactional
    public void settleOrderDeduction(Long productId, int quantity) {
        redisInventoryManager.deductLockedStock(productId, quantity);
        int updatedRows = inventoryRepository.deductStockDirect(productId, quantity);
        if (updatedRows == 0) {
            log.warn("Direct DB deduction affected 0 rows for productId: {}, quantity: {}. Inspecting database state.", productId, quantity);
        } else {
            log.info("Successfully settled and finalized stock deduction in DB for productId: {}, quantity: {}", productId, quantity);
        }
    }

    @Transactional
    public InventoryResponse replenishStock(Long productId, int additionalStock) {
        if (additionalStock <= 0) {
            throw new InvalidRequestException("Replenishment quantity must be greater than zero");
        }

        String lockKey = PRODUCT_LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(lockWaitSeconds, lockLeaseSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new InvalidRequestException("Unable to acquire lock for inventory replenishment. Please retry.");
            }

            try {
                Inventory inventory = inventoryRepository.findByProductId(productId)
                        .orElseGet(() -> Inventory.builder()
                                .productId(productId)
                                .totalStock(0)
                                .availableStock(0)
                                .lockedStock(0)
                                .build());

                inventory.setTotalStock(inventory.getTotalStock() + additionalStock);
                inventory.setAvailableStock(inventory.getAvailableStock() + additionalStock);

                Inventory saved = inventoryRepository.save(inventory);

                redisInventoryManager.prewarmStock(productId, saved.getAvailableStock(), saved.getLockedStock());

                InventoryEvent event = InventoryEvent.builder()
                        .productId(productId)
                        .quantity(additionalStock)
                        .orderReference("REPLENISHMENT-" + System.currentTimeMillis())
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
            throw new RuntimeException("Thread interrupted while waiting for replenishment lock", ex);
        }
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        Integer available = redisInventoryManager.getAvailableStock(productId);
        Integer locked = redisInventoryManager.getLockedStock(productId);

        if (available != null && locked != null) {
            return InventoryResponse.fromCache(productId, available, locked);
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        redisInventoryManager.prewarmStock(productId, inventory.getAvailableStock(), inventory.getLockedStock());

        return InventoryResponse.fromEntity(inventory, false);
    }

    private void syncStockFromDbToRedisWithLock(Long productId) {
        String lockKey = PRODUCT_LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(lockWaitSeconds, lockLeaseSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Could not acquire lock to pre-warm productId: {}", productId);
                return;
            }

            try {
                if (redisInventoryManager.hasKey(productId)) {
                    return; // Another thread synchronized already
                }

                Inventory inventory = inventoryRepository.findByProductId(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

                redisInventoryManager.prewarmStock(productId, inventory.getAvailableStock(), inventory.getLockedStock());
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted during cache warming for product: {}", productId, e);
        }
    }
}
/*This `InventoryService.java` is the **main orchestration layer** of your Inventory Service. It connects **Redis + PostgreSQL + Redisson + Kafka**.

However, there are **some important correctness issues** in the current implementation that we should fix before treating it as production-ready.

### Overall flow

```text
                    InventoryService
                          │
          ┌───────────────┼────────────────┐
          ↓               ↓                ↓
        Redis          PostgreSQL        Kafka
     fast stock       durable state     events
          │
       Redisson
   distributed lock
```

The project structure places `InventoryService` directly under the inventory service layer, with dependencies on the repository, Redis service, producer, entity, and DTOs.

---

## 1. `reserveStock()`

This is the most important method.

```java
public InventoryResponse reserveStock(InventoryReservationRequest request)
```

Flow:

```text
Request
  ↓
Redis Lua
  ↓
Stock initialized?
 ┌──────────────┐
 │              │
NO             YES
 │              │
 ↓              ↓
Redisson      Reserve
Lock          atomically
 │
 ↓
PostgreSQL → Redis
 │
 ↓
Retry Redis reservation
```

The first attempt is:

```java
inventoryRedisService.reserveStock(productId, quantity);
```

This uses the Lua script from your previous file.

For:

```text
available = 10
quantity  = 3
```

Redis atomically does:

```text
available: 10 → 7
locked:     0 → 3
```

Then Kafka receives:

```text
INVENTORY_RESERVED
```

The service finally reads the current Redis counters and returns them.

---

## 2. Cold-start protection

This part is good:

```java
if (result == ReservationResult.NOT_INITIALIZED) {
    syncStockFromDbToRedisWithLock(productId);
    result = inventoryRedisService.reserveStock(productId, quantity);
}
```

Imagine 1,000 users request the same product while Redis has no stock key.

Without protection:

```text
1000 requests
     ↓
1000 PostgreSQL queries ❌
```

With the Redisson lock:

```text
1000 requests
     ↓
Redis key missing
     ↓
One thread gets lock
     ↓
PostgreSQL
     ↓
Redis
     ↓
Other threads see initialized Redis
```

That's the **thundering-herd protection** your architecture is trying to achieve.

---

# ⚠️ Important issue #1 — Redis success happens before PostgreSQL

This is the biggest issue.

Currently:

```text
Redis reservation
      ↓
Kafka event
      ↓
PostgreSQL
```

Actually, `reserveStock()` doesn't update PostgreSQL at all.

That means:

```text
PostgreSQL:
available = 100

Redis:
available = 100
```

User reserves 5:

```text
Redis:
available = 95
locked = 5

PostgreSQL:
available = 100
```

Now the two systems temporarily disagree.

That's not automatically wrong if **Redis is intentionally the live reservation layer**, but you need a clear synchronization mechanism.

Your current code doesn't yet provide that complete synchronization mechanism.

---

# ⚠️ Important issue #2 — Kafka is not Transactional Outbox

This:

```java
inventoryProducer.sendInventoryReservedEvent(event);
```

is asynchronous Kafka publishing.

If Redis successfully reserves stock:

```text
Redis ✅
Kafka ❌
```

the reservation event can be lost.

Your architecture previously intended **Transactional Outbox**, so eventually the flow should be:

```text
Inventory reservation
       ↓
PostgreSQL transaction
       ↓
Inventory + Outbox Event
       ↓
Outbox Publisher
       ↓
Kafka
```

The current method isn't doing that yet.

---

# ⚠️ Important issue #3 — `settleOrderDeduction()`

Current flow:

```java
inventoryRedisService.deductLockedStock(productId, quantity);

int updatedRows =
        inventoryRepository.deductStockDirect(productId, quantity);
```

So:

```text
Redis
locked -= quantity
       ↓
PostgreSQL
locked -= quantity
totalStock -= quantity
```

But if PostgreSQL update returns:

```text
updatedRows = 0
```

Redis has **already been changed**.

Example:

```text
Redis locked = 5
DB locked    = 0

Redis:
5 → 3

DB:
update → 0 rows
```

Now Redis and PostgreSQL are inconsistent.

This is why settlement needs a more carefully designed atomic/idempotent flow.

---

# ⚠️ Important issue #4 — `totalStock`

Your repository currently has:

```java
deductStockDirect()
```

which does:

```text
lockedStock -= quantity
totalStock  -= quantity
```

But if `totalStock` means **original inventory capacity**, it should normally not decrease when an item is sold.

For example:

```text
Initial:
total = 100
available = 100
locked = 0
```

After selling 10:

```text
total = 100
available = 90
locked = 0
```

not:

```text
total = 90 ❌
```

You need to decide what `totalStock` means.

For this project, I would recommend:

```text
totalStock     = physical inventory capacity
availableStock = currently available
lockedStock    = reserved
soldStock      = successfully sold
```

Then:

```text
totalStock = availableStock + lockedStock + soldStock
```

That is much easier to explain in an interview.

---

# ⚠️ Important issue #5 — release failure is ignored

Current code:

```java
InventoryRedisService.ReleaseResult result =
        inventoryRedisService.releaseStock(productId, quantity);

if (result == NOT_INITIALIZED) {
    inventoryRepository.releaseStockDirect(productId, quantity);
}
```

But what if:

```text
result = INVALID_QUANTITY
```

?

The code still publishes:

```text
INVENTORY_RELEASED
```

That is dangerous.

You should explicitly handle:

```text
SUCCESS
NOT_INITIALIZED
INVALID_QUANTITY
```

before publishing the event.

---

# ⚠️ Important issue #6 — `getInventory()` can warm Redis

This is fine for normal usage:

```java
Inventory inventory =
    inventoryRepository.findByProductId(productId);

inventoryRedisService.prewarmStock(...);
```

But `getInventory()` is a **read operation**.

Ideally, cache population should be carefully controlled so a normal read doesn't unexpectedly overwrite hot Redis state during an active flash sale.

---

# What is good in this file?

Despite those issues, the overall architecture is strong:

### ✅ Redis Lua

Handles the high-concurrency reservation:

```text
check stock
   +
decrement
   +
increment locked
```

atomically.

### ✅ Redisson

Used only when synchronization is needed:

```text
Redis cache miss
      ↓
Distributed lock
      ↓
PostgreSQL
      ↓
Redis
```

Good separation.

### ✅ Kafka

Keeps downstream processing asynchronous.

```text
Inventory
   ↓
Kafka
   ↓
Order / Analytics
```

### ✅ PostgreSQL

Remains the persistent database.

### ✅ Service separation

`InventoryService` orchestrates; `InventoryRedisService` handles Redis; `InventoryRepository` handles database operations; `InventoryProducer` handles Kafka.

That's a good layered design.

---

## Recommended final inventory architecture

I would eventually make the flow:

```text
                 ┌───────────────┐
                 │ Inventory API │
                 └───────┬───────┘
                         ↓
                 InventoryService
                         ↓
                Redis Lua Script
                         ↓
                 ┌───────┴───────┐
                 │               │
              Success         Failure
                 │               │
                 ↓               ↓
             Reservation       409
                 │
                 ↓
        PostgreSQL + Outbox
                 │
                 ↓
                Kafka
          ┌──────┼───────┐
          ↓      ↓       ↓
        Order  Payment Analytics
```

So I **would not blindly keep this exact `InventoryService.java` as production-ready**. The basic structure is good, but the Redis/DB consistency, settlement, release handling, and Transactional Outbox need to be fixed.

**Next, the most useful file to inspect is `Inventory.java`**, because `totalStock`, `availableStock`, and `lockedStock` semantics need to be settled before we finalize `InventoryService`.
*/
