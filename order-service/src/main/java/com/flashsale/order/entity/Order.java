package com.flashsale.order.entity;

import com.flashsale.common.exception.InvalidRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_reference", columnList = "orderReference", unique = true),
        @Index(name = "idx_order_user_id", columnList = "userId"),
        @Index(name = "idx_order_status_deadline", columnList = "status, paymentDeadline")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderReference;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant paymentDeadline;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public enum OrderStatus {
        PENDING_PAYMENT,
        PAID,
        CANCELLED,
        EXPIRED,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = OrderStatus.PENDING_PAYMENT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markAsPaid() {
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidRequestException("Cannot mark order as PAID from current status: " + this.status);
        }
        if (Instant.now().isAfter(this.paymentDeadline)) {
            throw new InvalidRequestException("Payment received after payment deadline. Order has expired.");
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidRequestException("Cannot cancel order from current status: " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void expire() {
        if (this.status != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidRequestException("Cannot expire order from current status: " + this.status);
        }
        this.status = OrderStatus.EXPIRED;
    }

    public void markAsFailed() {
        this.status = OrderStatus.FAILED;
    }
}
/*This `Order.java` is a good **core entity for the Order Service**. It models the order lifecycle and payment deadline clearly.

### Overall role

```text id="yq4m1u"
Order Service
     │
     ↓
   Order
     │
 ┌───┼───────────────┐
 ↓   ↓               ↓
User Product      Payment
ID    ID          Deadline
     │
     ↓
Order Status
```

The entity stores `orderReference`, `userId`, `productId`, quantity, prices, status, and payment deadline as described in the uploaded file. fileciteturn1file0L38-L71

## 1. `orderReference`

```java
@Column(nullable = false, unique = true, length = 64)
private String orderReference;
```

This is very important for your distributed system.

Example:

```text
ORD-2026-000001
```

Instead of relying only on the database-generated `id`, other services can use this reference when communicating through Kafka.

```text
Order
  ↓
ORD-2026-000001
  ↓
Inventory / Payment / Notification
```

The unique index also prevents two orders from having the same reference.

---

## 2. Product and quantity

```java
private Long productId;
private Integer quantity;
```

Example:

```text
productId = 101
quantity  = 2
```

means the customer wants 2 units of product 101.

---

## 3. Money: `BigDecimal`

```java
private BigDecimal unitPrice;
private BigDecimal totalAmount;
```

Good choice.

For example:

```text
unitPrice  = ₹999.99
quantity   = 2
totalAmount = ₹1999.98
```

Using `BigDecimal` avoids the precision problems that can happen with floating-point types such as `double`.

---

## 4. Order status

```java
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    EXPIRED,
    FAILED
}
```

This gives you a clear lifecycle:

```text id="v1a8k0"
              ┌──────────┐
              │ PENDING  │
              │ PAYMENT  │
              └────┬─────┘
           ┌───────┼────────┐
           ↓       ↓        ↓
         PAID   CANCELLED EXPIRED
           │
           ↓
        completed
```

`FAILED` can represent an unsuccessful order-processing state.

---

# 5. `paymentDeadline`

```java
private Instant paymentDeadline;
```

This is useful for your flash-sale checkout.

Example:

```text
Order created
    ↓
10:00:00
    ↓
Payment deadline = 10:05:00
```

If payment isn't completed by then:

```text
PENDING_PAYMENT
       ↓
    EXPIRED
       ↓
Inventory released
```

The composite index:

```java
@Index(name = "idx_order_status_deadline",
       columnList = "status, paymentDeadline")
```

is appropriate for finding expired pending orders.

---

# 6. `@PrePersist`

```java
@PrePersist
protected void onCreate() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();

    if (this.status == null) {
        this.status = OrderStatus.PENDING_PAYMENT;
    }
}
```

When a new order is inserted:

```text
new Order
   ↓
@PrePersist
   ↓
createdAt = now
updatedAt = now
status = PENDING_PAYMENT
```

So callers don't have to manually set these every time.

---

# 7. `@PreUpdate`

```java
@PreUpdate
protected void onUpdate() {
    this.updatedAt = Instant.now();
}
```

Every JPA update automatically refreshes:

```text
updatedAt
```

Useful for auditing and debugging.

---

# 8. `markAsPaid()`

```java
public void markAsPaid()
```

This protects the state transition.

Only:

```text
PENDING_PAYMENT → PAID
```

is allowed.

For example:

```text
PENDING_PAYMENT → PAID       ✅
PAID            → PAID       ❌
CANCELLED       → PAID       ❌
EXPIRED         → PAID       ❌
```

It also checks the deadline.

That's a good business invariant.

---

# 9. `cancel()`

```java
public void cancel()
```

Allows:

```text
PENDING_PAYMENT → CANCELLED
```

but prevents:

```text
PAID → CANCELLED
```

and:

```text
EXPIRED → CANCELLED
```

Good.

---

# 10. `expire()`

```java
public void expire()
```

Allows:

```text
PENDING_PAYMENT → EXPIRED
```

This should eventually be called by a scheduled expiration process.

Example:

```text
paymentDeadline < currentTime
              ↓
       Order expiration job
              ↓
         order.expire()
              ↓
       order.cancelled/expired event
              ↓
       Inventory releases stock
```

---

# ⚠️ One important issue: `markAsPaid()` and race conditions

Suppose two requests arrive almost simultaneously:

```text
Request A → markAsPaid()
Request B → markAsPaid()
```

Both may initially read:

```text
PENDING_PAYMENT
```

The entity method itself doesn't provide database-level concurrency protection.

So eventually `OrderService` should use an appropriate transaction/locking or conditional-update strategy.

For example conceptually:

```text
PENDING_PAYMENT
      ↓
database-level transition
      ↓
PAID
```

rather than relying only on the Java object's state.

---

# ⚠️ Another important issue: payment deadline semantics

You currently have:

```java
if (Instant.now().isAfter(this.paymentDeadline))
```

This means payment exactly at the deadline is still accepted because `isAfter()` is false when the timestamps are equal.

Whether that's what you want is a business-rule decision.

If the rule is:

> Payment must occur **before** the deadline.

then an equality check should also be considered.

---

# ⚠️ `FAILED` has no transition rule

You have:

```java
FAILED
```

but:

```java
markAsFailed()
```

allows it from **any status**:

```java
public void markAsFailed() {
    this.status = OrderStatus.FAILED;
}
```

So currently:

```text
PAID → FAILED       possible
CANCELLED → FAILED  possible
EXPIRED → FAILED    possible
```

That may or may not be intended.

For a strict state machine, I'd define exactly which states can become `FAILED`.

---

## Final verdict

### ✅ Good

- Proper JPA entity
- Unique `orderReference`
- `BigDecimal` for money
- Clear order states
- Payment deadline
- Useful database indexes
- Automatic timestamps
- Business transition methods

### 🔴 Should be addressed later

1. Database-level concurrency protection for payment/status transitions
2. Decide exact deadline semantics
3. Restrict `markAsFailed()` transitions
4. Add idempotency handling at the service/database level

The entity itself is **a solid starting point**. The next logical file is `OrderRepository.java`, because that's where we'll implement the database queries and concurrency/idempotency support for this entity.*/