package com.flashsale.order.dto;

import com.flashsale.order.entity.Order;
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
public class OrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderReference;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String status;
    private Instant paymentDeadline;
    private Instant createdAt;
    private Instant updatedAt;

    public static OrderResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderReference(order.getOrderReference())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .paymentDeadline(order.getPaymentDeadline())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
/*This `OrderResponse.java` is a **clean outbound DTO** for the Order Service. Its main purpose is to send order information to the frontend/client without exposing the JPA `Order` entity directly.

## Overall flow

```text
Order Entity
     ↓
OrderResponse.fromEntity()
     ↓
OrderResponse DTO
     ↓
Controller
     ↓
Frontend / Client
```

### 1. Why `OrderResponse` is needed

Instead of returning:

```java
Order
```

directly from your API, you return:

```java
OrderResponse
```

This keeps your database entity separate from your API contract.

---

## 2. Order information

It contains:

```text
id
orderReference
userId
productId
quantity
unitPrice
totalAmount
status
```

For example:

```json
{
  "id": 15,
  "orderReference": "ORD-10001",
  "userId": 25,
  "productId": 101,
  "quantity": 2,
  "unitPrice": 999.99,
  "totalAmount": 1999.98,
  "status": "PENDING_PAYMENT"
}
```

That's exactly the kind of response your frontend can consume.

---

## 3. `paymentDeadline`

```java
private Instant paymentDeadline;
```

This is particularly useful for your flash-sale checkout.

Suppose:

```text
Current time:
10:00:00

paymentDeadline:
10:05:00
```

Frontend can calculate:

```text
5 minutes remaining
```

and display:

```text
⏳ Complete payment within 04:59
```

The backend should still enforce the deadline; the frontend countdown is only a UI convenience.

---

## 4. `fromEntity()`

This is the most important method:

```java
public static OrderResponse fromEntity(Order order)
```

It converts:

```text
Order
 ↓
OrderResponse
```

Example:

```java
Order order = ...;

OrderResponse response =
        OrderResponse.fromEntity(order);
```

Internally:

```java
.id(order.getId())
.orderReference(order.getOrderReference())
.productId(order.getProductId())
.quantity(order.getQuantity())
...
```

This prevents controllers/services from repeatedly writing the same mapping code.

---

## 5. Null handling

```java
if (order == null) {
    return null;
}
```

This prevents:

```text
NullPointerException
```

if someone accidentally calls:

```java
OrderResponse.fromEntity(null);
```

Although in many service designs, returning `null` isn't ideal and it's often better to ensure the service never passes a null entity. But for this mapper, the current behavior is safe.

---

## 6. Status conversion

You have:

```java
private String status;
```

while the entity has:

```java
OrderStatus status;
```

and conversion happens here:

```java
.status(order.getStatus().name())
```

So:

```text
OrderStatus.PENDING_PAYMENT
             ↓
      "PENDING_PAYMENT"
```

The JSON response becomes:

```json
{
  "status": "PENDING_PAYMENT"
}
```

This is simple and frontend-friendly.

### Small improvement

You could also use:

```java
private Order.OrderStatus status;
```

in the DTO rather than `String`.

But your current `String` approach is perfectly reasonable if you intentionally want the API contract to expose strings.

---

## 7. `Serializable`

```java
public class OrderResponse implements Serializable
```

This allows the DTO to be serialized by Java mechanisms.

However, for a normal Spring Boot REST API returning JSON, **implementing `Serializable` isn't required**.

Jackson can serialize the DTO without it.

So:

```java
implements Serializable
```

is optional. It doesn't hurt, but you don't need it just because you're returning JSON.

---

# One important thing to watch

This line:

```java
.status(order.getStatus().name())
```

assumes:

```java
order.getStatus() != null
```

Normally your `@PrePersist` in `Order` sets the default status to `PENDING_PAYMENT`, so this should normally be populated.

But if an incompletely constructed entity reaches this mapper, it can throw:

```text
NullPointerException
```

Not a major concern if your entity lifecycle is controlled properly.

---

## Final verdict

### ✅ Keep

* Separate DTO from JPA entity
* `fromEntity()` mapper
* `BigDecimal` for financial values
* `Instant` for timestamps
* `paymentDeadline`
* `orderReference`
* Clear API fields

### 🟡 Optional improvements

* `Serializable` isn't necessary for REST JSON.
* Consider enum status instead of `String`.
* Ensure `Order.status` is never null before mapping.

Overall:

```text
Order.java
    ↓
OrderResponse.fromEntity()
    ↓
OrderResponse
    ↓
REST API
    ↓
Frontend
```

**This file doesn't need any major change.**
*/