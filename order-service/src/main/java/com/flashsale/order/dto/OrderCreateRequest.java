package com.flashsale.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 5, message = "Maximum 5 units allowed per flash sale purchase")
    private Integer quantity;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
/*This `OrderCreateRequest.java` is a **clean inbound DTO for creating an order**. It fits the flash-sale requirements well.

## What this DTO does

The request contains exactly three things:

```text id="gkq8cw"
OrderCreateRequest
       │
       ├── productId
       ├── quantity
       └── idempotencyKey
```

---

### 1. `productId`

```java
@NotNull(message = "Product ID is required")
private Long productId;
```

The customer tells the Order Service which product they want.

Example:

```json
{
  "productId": 101,
  "quantity": 2,
  "idempotencyKey": "abc-123"
}
```

Without `productId` → validation fails.

---

### 2. `quantity`

```java
@NotNull(message = "Quantity is required")
@Min(value = 1, message = "Quantity must be at least 1")
@Max(value = 5, message = "Maximum 5 units allowed per flash sale purchase")
private Integer quantity;
```

This gives you two boundaries:

```text
quantity = null → ❌
quantity = 0    → ❌
quantity = -1   → ❌
quantity = 1    → ✅
quantity = 5    → ✅
quantity = 6    → ❌
```

For a flash-sale system, limiting the quantity per request is useful, but **`@Max(5)` alone does not enforce "per-customer" limits**.

For example, a customer could potentially send:

```text
Request 1 → quantity 5
Request 2 → quantity 5
Request 3 → quantity 5
```

So the actual **per-customer purchase limit must eventually be enforced using `userId + productId` in the service/database/business logic**.

The DTO only limits the quantity of a single request.

---

## 3. `idempotencyKey`

```java
@NotBlank(message = "Idempotency key is required")
private String idempotencyKey;
```

This is very important in your distributed system.

Imagine:

```text
Customer clicks Buy
       ↓
Order created
       ↓
Network timeout
       ↓
Customer clicks Buy again
```

Without idempotency:

```text
Request 1 → Order A → reserve 1
Request 2 → Order B → reserve 1 ❌
```

With the same key:

```text
Request 1
idempotencyKey = ABC123
       ↓
Order created

Request 2
idempotencyKey = ABC123
       ↓
Already processed
       ↓
Return existing order
```

So the key should eventually be **uniquely associated with the customer/order operation**.

---

## Important distinction

There are actually **two different protections** here:

### Request idempotency

```text
idempotencyKey
      ↓
"Did I process this exact request before?"
```

### Purchase limit

```text
userId + productId
      ↓
"Has this customer already bought too many units?"
```

They solve different problems.

---

## Lombok

These:

```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

remove boilerplate.

For example:

```java
OrderCreateRequest request =
        OrderCreateRequest.builder()
                .productId(101L)
                .quantity(2)
                .idempotencyKey("ABC-123")
                .build();
```

---

## One recommendation

I would eventually add a length constraint to the idempotency key, for example:

```java
@Size(max = 64)
```

and potentially a stronger format depending on whether you're using UUIDs.

But **don't add it yet unless your API contract has decided the exact key format**.

### Verdict: ✅ Keep this DTO

The current DTO is appropriate. Just remember:

```text
@Max(5)
     ↓
per-request limit

idempotencyKey
     ↓
duplicate-request protection

userId + productId
     ↓
actual per-customer purchase limit
```

The next logical file is **`OrderRepository.java`**, because that's where we can implement the database-side idempotency and order lookup needed to make this DTO's `idempotencyKey` actually useful.
*/