package com.flashsale.payment.dto;

import com.flashsale.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String transactionId;
    private String orderReference;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .orderReference(payment.getOrderReference())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus().name())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
/*This `PaymentResponse.java` is also structured correctly. Its job is the **opposite of `PaymentRequest`**:

```text
PaymentRequest  → Client → PaymentController → PaymentService
PaymentResponse ← Client ← PaymentController ← PaymentService
```

### 1. What `PaymentResponse` does

It takes the internal `Payment` entity and converts it into a safe API response.

```text
Payment Entity
      ↓
PaymentResponse.fromEntity()
      ↓
PaymentResponse DTO
      ↓
JSON response
```

For example, the API can return:

```json
{
  "id": 1,
  "transactionId": "TXN-928374",
  "orderReference": "ORD-1001",
  "userId": 25,
  "amount": 999.99,
  "paymentMethod": "UPI",
  "status": "SUCCESS",
  "failureReason": null,
  "createdAt": "2026-09-06T10:30:00Z",
  "updatedAt": "2026-09-06T10:30:05Z"
}
```

---

### 2. Why not return `Payment` directly?

This is an important microservice practice.

Instead of:

```java
return payment;
```

you return:

```java
return PaymentResponse.fromEntity(payment);
```

Because `Payment` is your **database/JPA entity**, while `PaymentResponse` is your **API contract**.

```text
Database layer
     ↓
Payment.java
     ↓
PaymentResponse.java
     ↓
API client
```

This keeps your persistence model separate from what your API exposes.

---

### 3. The fields

```java
private Long id;
private String transactionId;
private String orderReference;
private Long userId;
private BigDecimal amount;
private String paymentMethod;
private String status;
private String failureReason;
private Instant createdAt;
private Instant updatedAt;
```

These correspond closely to the fields in your `Payment` entity.

One notable choice is:

```java
private String status;
```

instead of:

```java
private PaymentStatus status;
```

The conversion happens here:

```java
.status(payment.getStatus().name())
```

So:

```text
PaymentStatus.SUCCESS
        ↓
"SUCCESS"
```

That makes the JSON response simple.

---

### 4. `fromEntity()` is the important method

```java
public static PaymentResponse fromEntity(Payment payment)
```

This is a **factory/conversion method**.

It receives:

```text
Payment entity
```

and creates:

```text
PaymentResponse DTO
```

For example:

```java
PaymentResponse response =
        PaymentResponse.fromEntity(payment);
```

Internally:

```java
return PaymentResponse.builder()
        .id(payment.getId())
        .transactionId(payment.getTransactionId())
        .orderReference(payment.getOrderReference())
        .userId(payment.getUserId())
        .amount(payment.getAmount())
        .paymentMethod(payment.getPaymentMethod())
        .status(payment.getStatus().name())
        .failureReason(payment.getFailureReason())
        .createdAt(payment.getCreatedAt())
        .updatedAt(payment.getUpdatedAt())
        .build();
```

So it is basically doing:

```text
Payment.id              → PaymentResponse.id
Payment.transactionId   → PaymentResponse.transactionId
Payment.orderReference  → PaymentResponse.orderReference
Payment.amount          → PaymentResponse.amount
Payment.status          → PaymentResponse.status
...
```

---

### 5. Null check

```java
if (payment == null) {
    return null;
}
```

This prevents:

```java
payment.getId()
```

from causing a `NullPointerException`.

It's a reasonable defensive check.

However, in a properly designed service, you normally shouldn't call `fromEntity(null)` in the first place. So this is **helpful but not essential**.

---

### 6. `Serializable`

```java
public class PaymentResponse implements Serializable
```

and:

```java
private static final long serialVersionUID = 1L;
```

This makes the DTO Java-serializable.

For a normal Spring Boot REST API returning JSON, **you don't strictly need `Serializable`**. Jackson can serialize the DTO without it.

So this is optional:

```java
implements Serializable
```

You can keep it; it doesn't hurt.

---

### 7. One thing I would change later

Currently:

```java
.status(payment.getStatus().name())
```

can throw a `NullPointerException` if `payment.getStatus()` is unexpectedly `null`.

Your `Payment` entity already has a default status of `PENDING`, so under normal operation this shouldn't happen.

Therefore **no change is necessary right now**.

---

## `PaymentRequest` vs `PaymentResponse`

This distinction is important for your project:

| DTO               | Direction        | Purpose               |
| ----------------- | ---------------- | --------------------- |
| `PaymentRequest`  | Client → Backend | Submit payment        |
| `PaymentResponse` | Backend → Client | Return payment result |

### Request

```json
{
  "orderReference": "ORD-1001",
  "amount": 999.99,
  "paymentMethod": "UPI",
  "idempotencyKey": "ABC123"
}
```

### Response

```json
{
  "id": 1,
  "transactionId": "TXN-928374",
  "orderReference": "ORD-1001",
  "userId": 25,
  "amount": 999.99,
  "paymentMethod": "UPI",
  "status": "SUCCESS",
  "failureReason": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

Notice that **`idempotencyKey` is not returned**. That's good because it's an internal request/deduplication concern.

---

## Verdict

**Keep `PaymentResponse.java` as it is. ✅**

The architecture is currently:

```text
PaymentRequest
      ↓
PaymentController
      ↓
PaymentService
      ↓
Payment Entity
      ↓
PaymentResponse.fromEntity()
      ↓
Client
```

The next file that matters most is **`PaymentService.java`**. That's where we'll verify the critical parts: **idempotency, payment state transitions, transaction handling, and how the payment result is published to the Order/Inventory flow.**
*/