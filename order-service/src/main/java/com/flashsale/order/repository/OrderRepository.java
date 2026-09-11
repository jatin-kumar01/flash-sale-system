package com.flashsale.order.repository;

import com.flashsale.order.entity.Order;
import com.flashsale.order.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderReference(String orderReference);

    boolean existsByOrderReference(String orderReference);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.paymentDeadline < :now")
    List<Order> findExpiredOrders(
            @Param("status") OrderStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );
}
/*This `OrderRepository.java` is a **clean and appropriate repository layer** for the current Order Service. It provides the database queries that `OrderService` and the expiration scheduler will need.

The repository is designed around the `Order` entity and its status/deadline fields.

## Overall flow

```text
OrderService
     ↓
OrderRepository
     ↓
PostgreSQL
```

It has four main operations.

### 1. `findByOrderReference()`

```java
Optional<Order> findByOrderReference(String orderReference);
```

Example:

```text
ORD-1001
   ↓
PostgreSQL
   ↓
Order
```

Useful for:

* payment processing
* cancellation
* checking current status
* idempotency checks

Because `orderReference` is unique in `Order`, this is an efficient lookup.

---

### 2. `existsByOrderReference()`

```java
boolean existsByOrderReference(String orderReference);
```

Example:

```text
New checkout
     ↓
Does ORD-1001 already exist?
   ↙          ↘
 YES           NO
  ↓             ↓
Reject        Create
```

This is useful for preventing duplicate checkout requests.

**Important:** this check alone does not guarantee idempotency under concurrency.

Two requests could do:

```text
Request A → exists? NO
Request B → exists? NO
```

and both attempt to create the order.

The **unique database constraint** on `orderReference` is the final protection. Your `Order` entity already has that uniqueness requirement. So the service should also handle a duplicate-key/database exception appropriately.

---

### 3. `findByUserId()`

```java
Page<Order> findByUserId(Long userId, Pageable pageable);
```

This gives paginated order history.

For example:

```text
userId = 25

Page 0 → orders 1-10
Page 1 → orders 11-20
Page 2 → orders 21-30
```

This is much better than loading every order for a user at once.

---

### 4. `findExpiredOrders()`

```java
@Query("""
    SELECT o FROM Order o
    WHERE o.status = :status
    AND o.paymentDeadline < :now
""")
List<Order> findExpiredOrders(
        @Param("status") OrderStatus status,
        @Param("now") Instant now,
        Pageable pageable
);
```

This is for your future expiration scheduler.

Example:

```text
Current time = 10:10

Orders:
ORD-1 → PENDING → deadline 10:05 ❌ expired
ORD-2 → PENDING → deadline 10:15 ✅
ORD-3 → PAID    → deadline 10:01 → ignore
```

Query:

```text
status = PENDING_PAYMENT
deadline < 10:10
```

returns only `ORD-1`.

Then:

```text
OrderExpirationScheduler
          ↓
findExpiredOrders()
          ↓
order.expire()
          ↓
publish order.expired
          ↓
Inventory Service
          ↓
release stock
```

---

## ⚠️ One small correction

The explanation says:

> "`findExpiredOrders` ... without table scans."

The index **helps the database efficiently locate matching rows**, but you shouldn't guarantee that PostgreSQL will never perform a table scan. The optimizer decides the execution plan based on table size, statistics, selectivity, etc.

A better statement is:

> The `(status, paymentDeadline)` index is designed to make expired pending-order lookups efficient.

---

## ⚠️ Another improvement I'd consider later

For high-throughput expiration processing, you may eventually want a query that supports concurrent schedulers safely, potentially using PostgreSQL row locking such as `FOR UPDATE SKIP LOCKED`.

For example:

```text
Scheduler instance A ─┐
                      ├── PostgreSQL
Scheduler instance B ─┘
```

Both shouldn't process the same expired order simultaneously.

But **don't add this yet just for the sake of complexity**. We can introduce it when we implement `OrderExpirationScheduler`.

---

# Verdict

### ✅ Keep

```text
findByOrderReference()
existsByOrderReference()
findByUserId()
findExpiredOrders()
```

They match the current Order entity well.

### 🔴 Remember for `OrderService`

The important idempotency pattern should ultimately be:

```text
Request
   ↓
orderReference
   ↓
Check existing order
   ↓
Create order
   ↓
DB unique constraint
   ↓
Handle duplicate safely
```

So the repository is good, but **idempotency will be completed in `OrderService`, not this repository alone**.

**Next logical file:** `OrderService.java`. That is where the actual checkout flow will connect **Order → Inventory Service → Kafka → payment lifecycle**.
*/