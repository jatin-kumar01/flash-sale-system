package com.flashsale.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Order reference is required")
    private String orderReference;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
/*Yes — this `PaymentRequest.java` is a **good DTO for the payment submission layer**. Let’s understand it simply, especially how it fits into your flash-sale architecture.

### 1. What this class does

`PaymentRequest` is the object that receives payment information from the client:

```text
Frontend
   ↓
PaymentController
   ↓
PaymentRequest
   ↓
PaymentService
   ↓
PaymentRepository
   ↓
Database
```

It **does not process the payment itself**. It only carries and validates the incoming data.

---

### 2. Fields

| Field            | Example      | Purpose                               |
| ---------------- | ------------ | ------------------------------------- |
| `orderReference` | `ORD-1001`   | Identifies which order is being paid  |
| `amount`         | `999.99`     | Amount customer wants to pay          |
| `paymentMethod`  | `UPI`        | Payment method                        |
| `idempotencyKey` | `PAY-abc123` | Prevents duplicate payment processing |

#### `orderReference`

```java
@NotBlank(message = "Order reference is required")
private String orderReference;
```

It cannot be `null`, empty, or only spaces.

Example:

```text
orderReference = "ORD-1001"  → valid
orderReference = ""          → invalid
orderReference = "   "       → invalid
```

---

### 3. Amount validation

```java
@NotNull(message = "Payment amount is required")
@DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
private BigDecimal amount;
```

Two validations happen:

```text
null       → ❌
0          → ❌
-100       → ❌
0.01       → ✅
999.99     → ✅
```

Using `BigDecimal` is correct for money because you should **not use `double`/`float` for monetary values**.

---

### 4. Payment method

```java
@NotBlank(message = "Payment method is required")
private String paymentMethod;
```

Example:

```json
{
  "orderReference": "ORD-1001",
  "amount": 999.99,
  "paymentMethod": "UPI",
  "idempotencyKey": "abc-123"
}
```

For the current stage, `String` is fine.

Later, you could make it an enum:

```java
public enum PaymentMethod {
    CREDIT_CARD,
    UPI,
    WALLET
}
```

That would prevent invalid values such as:

```text
"ABC_PAYMENT"
"HELLO"
"random"
```

But **you don't need to change it right now** if you're following the current architecture.

---

### 5. Most important: `idempotencyKey`

```java
@NotBlank(message = "Idempotency key is required")
private String idempotencyKey;
```

This is especially important in your **high-concurrency flash-sale system**.

Suppose the user clicks **Pay**:

```text
Client → Payment Service
```

Payment service starts processing.

But the network response is lost:

```text
Client → Payment Service
             ↓
          Payment
             ↓
        SUCCESS

Client ❌ doesn't receive response
```

The client retries:

```text
Client → Payment Service
```

Without idempotency:

```text
Payment 1 → ₹999 → SUCCESS
Payment 2 → ₹999 → SUCCESS

❌ Customer charged ₹1998
```

With the same `idempotencyKey`:

```text
Request 1
idempotencyKey = ABC123
       ↓
Payment SUCCESS

Request 2
idempotencyKey = ABC123
       ↓
Already processed
       ↓
Return existing result
```

So:

```text
Same idempotencyKey
        ↓
Same payment operation
        ↓
Don't process twice
```

**Important:** This DTO only *carries* the idempotency key. The actual guarantee must be implemented in `PaymentService` + database constraints/transaction logic. `@NotBlank` alone does not guarantee deduplication.

---

### 6. Lombok annotations

```java
@Getter
@Setter
```

Automatically creates getters/setters.

Instead of writing:

```java
public String getOrderReference() {
    return orderReference;
}

public void setOrderReference(String orderReference) {
    this.orderReference = orderReference;
}
```

Lombok generates them.

`@Builder` allows:

```java
PaymentRequest request = PaymentRequest.builder()
        .orderReference("ORD-1001")
        .amount(new BigDecimal("999.99"))
        .paymentMethod("UPI")
        .idempotencyKey("ABC123")
        .build();
```

`@NoArgsConstructor` creates:

```java
new PaymentRequest();
```

`@AllArgsConstructor` creates a constructor with all fields.

---

## Overall flow

Your DTO fits nicely into this flow:

```text
             Payment Request
                    │
                    ▼
          ┌──────────────────┐
          │ PaymentController │
          └────────┬─────────┘
                   │
                   ▼
          ┌──────────────────┐
          │  PaymentRequest  │
          │                  │
          │ orderReference   │
          │ amount           │
          │ paymentMethod    │
          │ idempotencyKey   │
          └────────┬─────────┘
                   │
             @Valid checks
                   │
                   ▼
          ┌──────────────────┐
          │  PaymentService  │
          └────────┬─────────┘
                   │
          Idempotency check
                   │
                   ▼
          ┌──────────────────┐
          │PaymentRepository │
          └────────┬─────────┘
                   │
                   ▼
              PostgreSQL
```

### Verdict

**Keep this file as it is for now.** ✅

The main thing to remember is:

> **`PaymentRequest` validates and carries payment input; `PaymentService` will contain the actual idempotency and payment-processing logic.**

The next important file is **`PaymentService.java`**, because that's where we need to check whether the `idempotencyKey` is actually being used correctly.
*/