package com.flashsale.payment.entity;

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
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_transaction_id", columnList = "transactionId", unique = true),
        @Index(name = "idx_payment_order_reference", columnList = "orderReference"),
        @Index(name = "idx_payment_user_id", columnList = "userId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(nullable = false, length = 64)
    private String orderReference;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 32)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(length = 255)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public enum PaymentStatus {
        PENDING,
        SUCCESS,
        FAILED,
        REFUNDED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }
}

/*This `Payment.java` is the **core JPA entity for the Payment Service**. The structure is good, but there are a couple of important points to understand before moving to `PaymentRepository`.

## Overall role

```text id="2f6h0m"
Order Service
     ↓
orderReference
     ↓
Payment Service
     ↓
   Payment
     ↓
PostgreSQL
```

It stores:

```text id="6o4n5u"
transactionId
orderReference
userId
amount
paymentMethod
status
failureReason
createdAt
updatedAt
```

---

### 1. `transactionId`

```java id="z3j2k6"
@Column(nullable = false, unique = true, length = 64)
private String transactionId;
```

This identifies a payment transaction.

Example:

```text id="7e3i0w"
TXN-20260906-10001
```

The database uniqueness constraint prevents two payment records from using the same transaction ID.

**Important:** this alone does not completely prevent double payment processing. You also need idempotent payment handling in `PaymentService`.

---

### 2. `orderReference`

```java id="7q9h9r"
private String orderReference;
```

This connects the payment to your Order Service's order without creating a direct JPA relationship between microservices.

```text id="h3w7m4"
Order Service
ORD-10001
     ↓
Payment Service
ORD-10001
```

That's appropriate for a microservice architecture.

---

### 3. `amount`

```java id="3k3n8v"
private BigDecimal amount;
```

Good choice for financial values.

For example:

```text id="4ukg9c"
amount = 1499.99
```

Avoiding `double` here is important because payment calculations need exact decimal representation.

---

### 4. `paymentMethod`

```java id="kq7q2s"
private String paymentMethod;
```

Currently this could contain:

```text id="jjf4py"
UPI
CARD
NET_BANKING
WALLET
```

For the current implementation, `String` is acceptable.

If the project later needs strict validation, you could introduce:

```java
enum PaymentMethod
```

but **I wouldn't change it unnecessarily right now**.

---

## 5. Payment status

```java id="x6n7j9"
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
```

Basic lifecycle:

```text id="6v3qjo"
       PENDING
       /     \
      ↓       ↓
 SUCCESS    FAILED
    ↓
 REFUNDED
```

This is easy to understand and suitable for the current project.

---

## 6. `markSuccess()`

```java id="o2v6j8"
public void markSuccess() {
    this.status = PaymentStatus.SUCCESS;
    this.failureReason = null;
}
```

Example:

```text id="2gbr3w"
PENDING
  ↓
Payment gateway confirms payment
  ↓
SUCCESS
```

Clearing `failureReason` is sensible because a successful payment should not retain an old failure reason.

---

## 7. `markFailed()`

```java id="4f9q4c"
public void markFailed(String reason) {
    this.status = PaymentStatus.FAILED;
    this.failureReason = reason;
}
```

Example:

```text id="6z7r4s"
PENDING
   ↓
Payment rejected
   ↓
FAILED
   ↓
failureReason = "Insufficient funds"
```

That reason is useful for auditing/debugging.

---

## 8. `markRefunded()`

```java id="t2n5gc"
public void markRefunded() {
    this.status = PaymentStatus.REFUNDED;
}
```

Conceptually:

```text id="t7g1gc"
SUCCESS
   ↓
Refund
   ↓
REFUNDED
```

### ⚠️ One thing to improve later

Currently the method doesn't check the current state.

So technically:

```text id="9ydqz0"
PENDING → REFUNDED
FAILED  → REFUNDED
```

is possible.

For a strict payment state machine, refund should normally be allowed only from an appropriate successful state.

---

## 9. Timestamps

```java id="z3a9bv"
@PrePersist
protected void onCreate()
```

sets:

```text id="t4q0ba"
createdAt
updatedAt
```

And:

```java id="y8n6pd"
@PreUpdate
```

updates `updatedAt`.

This gives you a basic payment audit trail.

---

# ⚠️ Most important architectural point

Your class description calls the payment entity:

> "immutable financial payment transactions"

But this entity is actually **mutable**:

```java
markSuccess()
markFailed()
markRefunded()
```

So it isn't truly immutable.

A better description would be:

> **JPA entity representing payment transaction records, their lifecycle status, and audit information.**

That's more accurate.

---

## Another important point: payment idempotency

For your flash-sale architecture, imagine:

```text
Payment request
TXN-1001
     ↓
SUCCESS
```

Then due to a network retry:

```text
TXN-1001
     ↓
same payment request again
```

Your database's unique `transactionId` can help prevent duplicate records, but `PaymentService` must correctly handle the duplicate rather than simply crashing.

Eventually:

```text id="f4f14a"
transactionId
      ↓
Already processed?
   ↙       ↘
 YES        NO
  ↓          ↓
Return     Process
existing
result
```

This is especially important because your Order/Inventory services also need idempotent event processing.

---

## Verdict

**`Payment.java` → ✅ Good foundation.**

Keep:

* `BigDecimal`
* unique `transactionId`
* `orderReference`
* status enum
* timestamps
* database indexes

Later strengthen:

1. Payment status transition validation
2. Idempotent payment processing
3. Decide whether `paymentMethod` should remain `String` or become an enum
4. Correct the description from **"immutable"** to **"payment transaction record"**

The next logical file is **`PaymentRepository.java`**, where we'll implement payment lookup and the database-level support for idempotency.
*/
