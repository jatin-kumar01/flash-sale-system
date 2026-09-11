package com.flashsale.inventory.service;

import com.flashsale.inventory.redis.RedisInventoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryRedisService {

    private final RedisInventoryManager redisInventoryManager;

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
        RedisInventoryManager.ReservationResult result = redisInventoryManager.reserveStock(productId, quantity);
        return switch (result) {
            case SUCCESS -> ReservationResult.SUCCESS;
            case INSUFFICIENT_STOCK -> ReservationResult.INSUFFICIENT_STOCK;
            case NOT_INITIALIZED -> ReservationResult.NOT_INITIALIZED;
        };
    }

    public ReleaseResult releaseStock(Long productId, int quantity) {
        RedisInventoryManager.ReleaseResult result = redisInventoryManager.releaseStock(productId, quantity);
        return switch (result) {
            case SUCCESS -> ReleaseResult.SUCCESS;
            case INVALID_QUANTITY -> ReleaseResult.INVALID_QUANTITY;
            case NOT_INITIALIZED -> ReleaseResult.NOT_INITIALIZED;
        };
    }

    public void deductLockedStock(Long productId, int quantity) {
        redisInventoryManager.deductLockedStock(productId, quantity);
    }

    public void prewarmStock(Long productId, int availableStock, int lockedStock) {
        redisInventoryManager.prewarmStock(productId, availableStock, lockedStock);
    }

    public Integer getAvailableStock(Long productId) {
        return redisInventoryManager.getAvailableStock(productId);
    }

    public Integer getLockedStock(Long productId) {
        return redisInventoryManager.getLockedStock(productId);
    }

    public boolean hasKey(Long productId) {
        return redisInventoryManager.hasKey(productId);
    }
}

/*Yes, this `InventoryRedisService.java` is the **next layer after `RedisInventoryScriptConfig`**. Its job is to provide a clean Java interface for interacting with Redis.

### Flow

```text
InventoryService
      ↓
InventoryRedisService
      ↓
Redis Lua Script
      ↓
Redis
```

### Main methods

| Method                | Purpose                                          |
| --------------------- | ------------------------------------------------ |
| `reserveStock()`      | Atomically moves `available → locked`            |
| `releaseStock()`      | Atomically moves `locked → available`            |
| `deductLockedStock()` | Removes locked quantity after successful payment |
| `prewarmStock()`      | Loads PostgreSQL inventory into Redis            |
| `getAvailableStock()` | Reads available stock                            |
| `getLockedStock()`    | Reads locked stock                               |
| `hasKey()`            | Checks whether Redis inventory exists            |

### Important issue in the current code

`deductLockedStock()` is **not atomic with any validation**:

```java
stringRedisTemplate.opsForValue().decrement(lockedKey, quantity);
```

For example:

```text
locked = 2
quantity = 5

2 - 5 = -3
```

That can create an invalid negative locked-stock value.

More importantly, your Lua scripts currently maintain:

```text
available stock
locked stock
```

but `deductLockedStock()` simply decreases `locked`. For a production-quality design, settlement should validate the locked quantity and ideally be handled atomically.

Also, the `prewarmStock()` method performs **two separate Redis writes**:

```text
SET available
SET locked
```

For high-concurrency initialization, we should consider whether these need atomic initialization.

### One more important architecture point

Your current comments say:

> PostgreSQL → Redis synchronization

But this class itself **doesn't read PostgreSQL**. It only receives:

```java
prewarmStock(productId, availableStock, lockedStock)
```

So the actual flow will later be:

```text
PostgreSQL
    ↓
InventoryService / Prewarm process
    ↓
InventoryRedisService
    ↓
Redis
```

That's perfectly fine—the responsibility is correctly separated.

### Current Redis keys

For product `101`:

```text
inventory:stock:101
inventory:locked:101
```

Example:

```text
inventory:stock:101  = 95
inventory:locked:101 = 5
```

Meaning:

```text
Total currently accounted for = 100
Available = 95
Reserved/Locked = 5
```

**Verdict:** The file is structurally good and fits the architecture, but before calling the Inventory Service production-ready, I would strengthen the **settlement operation** and eventually make **reservation identity/idempotency** explicit so the same payment event cannot accidentally settle stock twice.

The next logical file is **`InventoryService.java`**, where these Redis operations will be combined with PostgreSQL and the reservation business rules.
*/
