package com.flashsale.inventory.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisInventoryManager {

    private final StringRedisTemplate stringRedisTemplate;
    @Qualifier("reserveStockScript")
    private final RedisScript<Long> reserveStockScript;
    @Qualifier("releaseStockScript")
    private final RedisScript<Long> releaseStockScript;

    private static final String STOCK_KEY_PREFIX = "inventory:stock:";
    private static final String LOCKED_KEY_PREFIX = "inventory:locked:";

    public enum ReservationResult {
        SUCCESS,
        INSUFFICIENT_STOCK,
        NOT_INITIALIZED
    }

    public enum ReleaseResult {
        SUCCESS,
        INVALID_QUANTITY,
        NOT_INITIALIZED
    }

    public ReservationResult reserveStock(Long productId, int quantity) {
        String stockKey = buildStockKey(productId);
        String lockedKey = buildLockedKey(productId);

        Long result = stringRedisTemplate.execute(
                reserveStockScript,
                List.of(stockKey, lockedKey),
                String.valueOf(quantity)
        );

        if (result == null || result == -1L) {
            log.warn("Stock keys missing in Redis for productId: {}", productId);
            return ReservationResult.NOT_INITIALIZED;
        }

        if (result == 0L) {
            log.info("Insufficient stock in Redis for productId: {}, requested: {}", productId, quantity);
            return ReservationResult.INSUFFICIENT_STOCK;
        }

        log.debug("Successfully reserved {} units in Redis for productId: {}", quantity, productId);
        return ReservationResult.SUCCESS;
    }

    public ReleaseResult releaseStock(Long productId, int quantity) {
        String stockKey = buildStockKey(productId);
        String lockedKey = buildLockedKey(productId);

        Long result = stringRedisTemplate.execute(
                releaseStockScript,
                List.of(stockKey, lockedKey),
                String.valueOf(quantity)
        );

        if (result == null || result == -1L) {
            log.warn("Stock keys missing in Redis during release for productId: {}", productId);
            return ReleaseResult.NOT_INITIALIZED;
        }

        if (result == 0L) {
            log.warn("Locked stock in Redis insufficient to release for productId: {}, requested: {}", productId, quantity);
            return ReleaseResult.INVALID_QUANTITY;
        }

        log.debug("Successfully released {} units in Redis for productId: {}", quantity, productId);
        return ReleaseResult.SUCCESS;
    }

    public void deductLockedStock(Long productId, int quantity) {
        String lockedKey = buildLockedKey(productId);
        Long remaining = stringRedisTemplate.opsForValue().decrement(lockedKey, quantity);
        log.debug("Settled and deducted {} locked units for productId: {}. Remaining locked: {}", quantity, productId, remaining);
    }

    public void prewarmStock(Long productId, int availableStock, int lockedStock) {
        String stockKey = buildStockKey(productId);
        String lockedKey = buildLockedKey(productId);

        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(availableStock));
        stringRedisTemplate.opsForValue().set(lockedKey, String.valueOf(lockedStock));
        log.info("Pre-warmed Redis inventory for productId: {} -> available: {}, locked: {}", productId, availableStock, lockedStock);
    }

    public Integer getAvailableStock(Long productId) {
        String value = stringRedisTemplate.opsForValue().get(buildStockKey(productId));
        return value != null ? Integer.parseInt(value) : null;
    }

    public Integer getLockedStock(Long productId) {
        String value = stringRedisTemplate.opsForValue().get(buildLockedKey(productId));
        return value != null ? Integer.parseInt(value) : null;
    }

    public boolean hasKey(Long productId) {
        Boolean exists = stringRedisTemplate.hasKey(buildStockKey(productId));
        return Boolean.TRUE.equals(exists);
    }

    private String buildStockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    private String buildLockedKey(Long productId) {
        return LOCKED_KEY_PREFIX + productId;
    }
}
