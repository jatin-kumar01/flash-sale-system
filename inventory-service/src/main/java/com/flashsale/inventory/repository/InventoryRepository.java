package com.flashsale.inventory.repository;

import com.flashsale.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithPessimisticLock(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE Inventory i SET i.availableStock = i.availableStock - :quantity, " +
           "i.lockedStock = i.lockedStock + :quantity, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId AND i.availableStock >= :quantity")
    int reserveStockDirect(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.lockedStock = i.lockedStock - :quantity, " +
           "i.availableStock = i.availableStock + :quantity, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId AND i.lockedStock >= :quantity")
    int releaseStockDirect(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.lockedStock = i.lockedStock - :quantity, " +
           "i.totalStock = i.totalStock - :quantity, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId AND i.lockedStock >= :quantity")
    int deductStockDirect(@Param("productId") Long productId, @Param("quantity") int quantity);
}

/*Yes, this `InventoryRepository.java` is a **core file** for your flash-sale project because it handles the database operations that protect inventory from overselling.

### What this repository does

Think of the flow as:

```text
InventoryService
       ↓
InventoryRepository
       ↓
PostgreSQL
```

It provides **three important types of operations**:

1. Find inventory
2. Lock an inventory row
3. Atomically reserve/release/deduct stock

---

## 1. `findByProductId()`

```java
Optional<Inventory> findByProductId(Long productId);
```

### What it does

Finds inventory using the product ID.

Example:

```text
Product ID = 101
Available Stock = 50
Locked Stock = 10
```

Calling:

```java
inventoryRepository.findByProductId(101L);
```

returns the inventory record for product `101`.

Conceptually:

```text
Product 101
    ↓
InventoryRepository
    ↓
Inventory record
```

`Optional` is used because the inventory record might not exist.

---

# 2. `findByProductIdWithPessimisticLock()`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Optional<Inventory> findByProductIdWithPessimisticLock(
        @Param("productId") Long productId);
```

This is for **concurrency protection**.

Suppose:

```text
Stock = 1
```

and two requests arrive simultaneously:

```text
User A ──┐
         ├── Product 101
User B ──┘
```

A pessimistic write lock tells PostgreSQL:

> "I'm currently working with this row. Other transactions that need to modify this row should wait."

Conceptually:

```text
User A
  ↓
LOCK inventory row
  ↓
Update stock
  ↓
COMMIT
  ↓
Unlock

User B
  ↓
Waits
  ↓
Gets latest stock
```

This can prevent conflicting updates when you genuinely need row-level locking.

---

# 3. `reserveStockDirect()`

This is **one of the most important methods** in your project.

```java
@Modifying
@Query("""
UPDATE Inventory i
SET i.availableStock = i.availableStock - :quantity,
    i.lockedStock = i.lockedStock + :quantity,
    i.updatedAt = CURRENT_TIMESTAMP
WHERE i.productId = :productId
AND i.availableStock >= :quantity
""")
int reserveStockDirect(...);
```

Suppose:

```text
Available stock = 10
Locked stock = 0
```

User wants:

```text
quantity = 3
```

Database performs:

```text
10 → 7 available
0  → 3 locked
```

Result:

```text
availableStock = 7
lockedStock = 3
```

### Why this is safer

The important part is:

```sql
AND available_stock >= quantity
```

Suppose only:

```text
stock = 2
```

but user wants:

```text
quantity = 5
```

Condition:

```text
2 >= 5
```

is false.

Therefore:

```text
UPDATE does not happen
```

and the method returns:

```text
0
```

If successful:

```text
return = 1
```

So your service can do:

```text
1 → reservation successful
0 → insufficient stock
```

---

# 4. Why this helps with race conditions

Suppose:

```text
Stock = 1
```

Two users simultaneously request one item.

### User A

```text
availableStock >= 1
        ↓
TRUE
        ↓
1 → 0
        ↓
SUCCESS
```

### User B

After the database applies the first update:

```text
availableStock = 0
```

Then:

```text
0 >= 1
```

is false.

Therefore:

```text
UPDATE = 0 rows
```

So:

```text
User A → SUCCESS
User B → OUT OF STOCK
```

That's exactly what your flash-sale system needs.

---

# 5. `releaseStockDirect()`

```java
@Modifying
@Query("""
UPDATE Inventory i
SET i.lockedStock = i.lockedStock - :quantity,
    i.availableStock = i.availableStock + :quantity,
    i.updatedAt = CURRENT_TIMESTAMP
WHERE i.productId = :productId
AND i.lockedStock >= :quantity
""")
int releaseStockDirect(...);
```

This is used when a **reservation needs to be cancelled/released**.

Example:

```text
Before:

Available = 7
Locked    = 3
```

User's payment fails.

Release 3:

```text
Available = 10
Locked    = 0
```

So:

```text
RESERVED
   ↓
PAYMENT FAILED
   ↓
RELEASE
   ↓
AVAILABLE
```

This is important because you don't want failed/expired reservations to permanently consume stock.

---

# 6. `deductStockDirect()`

```java
@Modifying
@Query("""
UPDATE Inventory i
SET i.lockedStock = i.lockedStock - :quantity,
    i.totalStock = i.totalStock - :quantity,
    i.updatedAt = CURRENT_TIMESTAMP
WHERE i.productId = :productId
AND i.lockedStock >= :quantity
""")
int deductStockDirect(...);
```

This is used when the reserved item becomes **actually sold**.

Example:

Before:

```text
Total stock     = 100
Available       = 20
Locked          = 5
```

A reservation of 5 becomes a successful purchase.

After:

```text
Total stock     = 95
Available       = 20
Locked          = 0
```

The 5 items move out of the inventory permanently.

Conceptually:

```text
AVAILABLE
    ↓
RESERVED
    ↓
PAYMENT SUCCESS
    ↓
SOLD
```

---

# 7. `@Modifying` ka meaning

You have:

```java
@Modifying
```

because these queries are not normal `SELECT` queries.

They modify database data:

```text
UPDATE
```

Without `@Modifying`, Spring Data JPA won't treat the query as an update operation correctly.

---

# 8. `@Param`

Example:

```java
@Param("productId") Long productId
```

This connects the Java parameter:

```java
productId
```

with JPQL:

```text
:productId
```

Similarly:

```java
@Param("quantity") int quantity
```

connects:

```text
quantity
```

with:

```text
:quantity
```

---

# 9. Why this file is important for your project

Your flash-sale problem is:

```text
1 product
+
100 stock
+
1,000,000 requests
```

You need to make sure:

```text
Successful reservations <= 100
```

This repository provides the database-level operations needed to enforce that.

The important architecture is:

```text
User
 ↓
API Gateway
 ↓
Inventory Controller
 ↓
Inventory Service
 ↓
Inventory Repository
 ↓
PostgreSQL
```

And the critical operation is:

```text
reserveStockDirect()
        ↓
Atomic UPDATE
        ↓
availableStock >= quantity?
       / \
     YES  NO
      ↓    ↓
   Reserve 0 rows
```

### One important thing

This repository **does not itself decide the complete business flow**.

It only provides database operations.

The actual decision-making should happen in:

```text
InventoryService.java
```

For example:

```text
InventoryService
      ↓
reserveStockDirect()
      ↓
affectedRows == 1?
    /       \
  YES        NO
  ↓           ↓
Success    Out of stock
```

So don't put business logic inside this repository.

**Repository = database operations.**
**Service = business logic.**

That's the separation you want in this project.
*/