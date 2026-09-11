package com.flashsale.payment.repository;

import com.flashsale.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    Optional<Payment> findByOrderReference(String orderReference);

    boolean existsByOrderReference(String orderReference);

    List<Payment> findAllByOrderReference(String orderReference);

    Page<Payment> findByUserId(Long userId, Pageable pageable);
}
/*This `PaymentRepository.java` is a **clean Spring Data JPA repository** for the Payment Service. It provides the database lookup methods that `PaymentService` will need for payment idempotency, order checks, and transaction history.

## Overall flow

```text id="p0q5mz"
PaymentService
      ↓
PaymentRepository
      ↓
PostgreSQL
      ↓
payments table
```

## 1. `findByTransactionId()`

```java
Optional<Payment> findByTransactionId(String transactionId);
```

Example:

```text
transactionId = TXN-1001
```

returns the payment if it exists:

```text
TXN-1001 → Payment
```

If it doesn't exist:

```text
Optional.empty()
```

This is useful when a payment gateway sends the same callback more than once.

---

## 2. `existsByTransactionId()`

```java
boolean existsByTransactionId(String transactionId);
```

This only checks existence.

Example:

```text
TXN-1001 exists?
       ↓
     true
```

This is cheaper/simpler when you don't need the complete `Payment` entity.

---

## 3. `findByOrderReference()`

```java
Optional<Payment> findByOrderReference(String orderReference);
```

Example:

```text
ORD-10001
    ↓
Find associated payment
```

This is useful when checking:

```text
Has this order already been paid?
```

---

## 4. `existsByOrderReference()`

```java
boolean existsByOrderReference(String orderReference);
```

Example:

```text
ORD-10001
    ↓
Payment exists?
    ↓
YES → don't create another payment
```

This is useful for basic duplicate-order/payment protection.

---

## 5. `findAllByOrderReference()`

```java
List<Payment> findAllByOrderReference(String orderReference);
```

This is useful if one order can have **multiple payment attempts**.

For example:

```text
ORD-10001
   │
   ├── TXN-001 → FAILED
   ├── TXN-002 → FAILED
   └── TXN-003 → SUCCESS
```

Then all payment attempts can be retrieved.

### ⚠️ Small design inconsistency

You have both:

```java
Optional<Payment> findByOrderReference(...)
```

and:

```java
List<Payment> findAllByOrderReference(...)
```

That's not necessarily wrong, but it reflects two different assumptions:

* `findByOrderReference()` → one payment per order
* `findAllByOrderReference()` → multiple payment records per order

For a payment system with retries, **multiple payment attempts can be useful**, so `findAllByOrderReference()` is the more flexible model.

However, if your business rule is strictly **one payment record per order**, then the database should enforce that with a unique constraint on `orderReference`.

Don't add that constraint until you've decided which model you want.

---

## 6. `findByUserId()`

```java
Page<Payment> findByUserId(Long userId, Pageable pageable);
```

This provides pagination.

Example:

```text
User 101 has 10,000 transactions
```

Instead of loading all 10,000:

```text
Page 1 → 20 payments
Page 2 → 20 payments
Page 3 → 20 payments
...
```

That's much better for a transaction-history API.

---

# Important point about idempotency

The repository gives you the **tools** for idempotency, but it doesn't implement idempotency by itself.

For example:

```text id="j5c8my"
Payment callback
TXN-1001
     ↓
findByTransactionId()
     ↓
Already SUCCESS?
     ↓
YES
     ↓
Return existing result
```

That actual decision belongs in:

```text
PaymentService
```

Also, this pattern:

```java
if (!repository.existsByTransactionId(id)) {
    repository.save(payment);
}
```

is **not completely safe under concurrency**.

Two requests could both see:

```text
exists = false
```

and both attempt to insert.

Your database's:

```java
@Column(unique = true)
```

on `transactionId` is therefore still important.

---

## Verdict

**`PaymentRepository.java` → ✅ Good for the current architecture.**

It provides:

```text
findByTransactionId()       → payment lookup
existsByTransactionId()     → duplicate check
findByOrderReference()      → order payment lookup
existsByOrderReference()    → duplicate/order check
findAllByOrderReference()   → payment attempts
findByUserId()              → paginated history
```

The next logical file is **`PaymentService.java`**, because that's where we need to see how these repository methods are actually used to implement **payment idempotency, status transitions, and order/payment event publishing**.
*/