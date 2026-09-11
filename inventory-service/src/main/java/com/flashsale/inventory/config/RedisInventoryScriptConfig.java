package com.flashsale.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisInventoryScriptConfig {

    /**
     * Lua Script for Atomic Inventory Reservation.
     *
     * KEYS[1]: Redis key for available stock (e.g., "inventory:stock:{productId}")
     * KEYS[2]: Redis key for locked stock (e.g., "inventory:locked:{productId}")
     * ARGV[1]: Quantity to reserve
     *
     * Returns:
     *   1: Reservation successful
     *   0: Insufficient available stock
     *  -1: Key not initialized / missing
     */
    private static final String DEDUCT_STOCK_LUA =
            "local stockKey = KEYS[1] " +
            "local lockedKey = KEYS[2] " +
            "local quantity = tonumber(ARGV[1]) " +
            "local currentStock = redis.call('GET', stockKey) " +
            "if not currentStock then " +
            "    return -1 " +
            "end " +
            "if tonumber(currentStock) < quantity then " +
            "    return 0 " +
            "end " +
            "redis.call('DECRBY', stockKey, quantity) " +
            "redis.call('INCRBY', lockedKey, quantity) " +
            "return 1 ";

    /**
     * Lua Script for Atomic Inventory Release / Rollback.
     *
     * KEYS[1]: Redis key for available stock (e.g., "inventory:stock:{productId}")
     * KEYS[2]: Redis key for locked stock (e.g., "inventory:locked:{productId}")
     * ARGV[1]: Quantity to release back to available
     *
     * Returns:
     *   1: Release successful
     *   0: Locked stock is less than release quantity
     *  -1: Keys do not exist
     */
    private static final String RELEASE_STOCK_LUA =
            "local stockKey = KEYS[1] " +
            "local lockedKey = KEYS[2] " +
            "local quantity = tonumber(ARGV[1]) " +
            "local currentLocked = redis.call('GET', lockedKey) " +
            "if not currentLocked then " +
            "    return -1 " +
            "end " +
            "if tonumber(currentLocked) < quantity then " +
            "    return 0 " +
            "end " +
            "redis.call('DECRBY', lockedKey, quantity) " +
            "redis.call('INCRBY', stockKey, quantity) " +
            "return 1 ";

    @Bean(name = "inlineDeductStockScript")
    public RedisScript<Long> deductStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DEDUCT_STOCK_LUA);
        script.setResultType(Long.class);
        return script;
    }

    @Bean(name = "inlineReleaseStockScript")
    public RedisScript<Long> inlineReleaseStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RELEASE_STOCK_LUA);
        script.setResultType(Long.class);
        return script;
    }
}
/*Yes — **Redis** is an important part of your Flash Sale project.

For your **Inventory Service**, Redis can be used mainly for **fast stock/reservation handling**, while PostgreSQL remains the persistent source of truth.

### In your project, Redis will handle

1. **Hot inventory data**

   ```text
   product:101:stock → 50
   ```

2. **Fast reservation**

   ```text
   reservation:user:5001:product:101 → 2
   ```

3. **Reservation expiry**

   ```text
   reservation:101:5001 → expires in 5 minutes
   ```

4. **Atomic stock operations**

   Redis Lua script can make:

   ```text
   Check stock
        ↓
   Decrease stock
        ↓
   Create reservation
   ```

   as **one atomic operation**.

5. **Reduce PostgreSQL pressure**

   During a flash sale, millions of requests shouldn't all directly compete for the same PostgreSQL row.

### Recommended flow

```text
User
  ↓
API Gateway
  ↓
Inventory Service
  ↓
Redis
  │
  ├── Stock available?
  ├── Atomic reservation
  └── Reservation TTL
  ↓
PostgreSQL
  │
  └── Persistent inventory state
  ↓
Kafka / Outbox
```

### Important distinction

Don't think:

> Redis replaces PostgreSQL.

Think:

> **Redis = extremely fast concurrency/hot-state layer**
> **PostgreSQL = durable source of truth**

For your project, I'd implement Redis in the **Inventory Service after the repository**, roughly:

```text
inventory/
├── config/
│   └── RedisConfig.java
├── entity/
│   └── Inventory.java
├── repository/
│   └── InventoryRepository.java
├── redis/
│   ├── RedisInventoryService.java
│   └── ReservationLuaScript.java
└── service/
    └── InventoryService.java
```

The next important file would be **`RedisConfig.java`**, then the Redis inventory/reservation logic.



Yes. This is the **current `RedisInventoryScriptConfig.java`** file in your Inventory Service.

### What this file does

It creates two Redis Lua scripts:

```text
RedisInventoryScriptConfig
        │
        ├── deductStockScript()
        │       └── available → locked
        │
        └── releaseStockScript()
                └── locked → available
```

The important part is that each operation happens **atomically inside Redis**.

### Example: stock = 1

Suppose:

```text
inventory:stock:101  = 1
inventory:locked:101 = 0
```

Two users simultaneously try to buy 1 item:

```text
User A ──┐
         ├── Redis Lua Script
User B ──┘
```

Redis executes one script completely before the other:

**User A:**

```text
currentStock = 1
1 >= 1 → YES
stock  = 0
locked = 1
return 1
```

**User B:**

```text
currentStock = 0
0 >= 1 → NO
return 0
```

So:

```text
Successful reservations = 1
Overselling = prevented
```

### One important correction

Your description says:

> "server-side SHA1 script caching"

`DefaultRedisScript` **does not itself mean the script is permanently registered/cached by SHA1 simply because you declared the bean**. Spring Data Redis handles script execution and can use Redis's script facilities, but the exact execution/caching behavior depends on how the script is executed by the Redis template.

Also, your current file contains **only two scripts**, despite the purpose saying:

> "stock deduction, release, and pre-warming operations"

There is currently **no pre-warming Lua script** in this file.

So for the current implementation, I would describe the purpose as:

> **Spring configuration providing Redis Lua scripts for atomic stock reservation and stock release under high concurrency.**

Everything else in your file is structurally fine for the next step.

**Next file:** `InventoryRedisService.java` — this will actually execute these Lua scripts against Redis.


*/