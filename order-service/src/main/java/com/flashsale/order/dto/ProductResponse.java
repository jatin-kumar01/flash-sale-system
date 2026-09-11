package com.flashsale.order.dto;

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
public class ProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal flashSalePrice;
    private Integer stock;
    private String status;
    private Instant saleStartTime;
    private Instant saleEndTime;

    public boolean isSaleActive(Instant now) {
        if (!"ACTIVE".equalsIgnoreCase(this.status)) {
            return false;
        }
        if (saleStartTime != null && now.isBefore(saleStartTime)) {
            return false;
        }
        if (saleEndTime != null && now.isAfter(saleEndTime)) {
            return false;
        }
        return true;
    }

    public BigDecimal getApplicablePrice(Instant now) {
        if (isSaleActive(now) && flashSalePrice != null && flashSalePrice.compareTo(BigDecimal.ZERO) > 0) {
            return flashSalePrice;
        }
        return originalPrice;
    }
}
/*This `ProductResponse.java` is a **DTO used by `order-service` to receive product information from `product-service`**. It is suitable for the checkout flow.

### Overall flow

```text
OrderService
     ↓
ProductClient
     ↓
product-service
     ↓
ProductResponse
     ↓
OrderService
```

## 1. Product information

The DTO contains:

```java
private Long id;
private String name;
private String description;
private BigDecimal originalPrice;
private BigDecimal flashSalePrice;
private Integer stock;
private String status;
private Instant saleStartTime;
private Instant saleEndTime;
```

So an example response could represent:

```json
{
  "id": 101,
  "name": "Gaming Mouse",
  "originalPrice": 2999.00,
  "flashSalePrice": 1999.00,
  "stock": 50,
  "status": "ACTIVE"
}
```

This gives `OrderService` enough information to determine whether the product can currently be purchased and which price applies.

---

## 2. `isSaleActive()`

```java
public boolean isSaleActive(Instant now)
```

This checks three things.

### Product status

```java
if (!"ACTIVE".equalsIgnoreCase(this.status)) {
    return false;
}
```

So:

```text
ACTIVE   → continue
INACTIVE → false
```

### Sale start

```java
if (saleStartTime != null && now.isBefore(saleStartTime)) {
    return false;
}
```

Example:

```text
Sale starts: 10:00
Current:     09:59
             ↓
        Sale inactive
```

### Sale end

```java
if (saleEndTime != null && now.isAfter(saleEndTime)) {
    return false;
}
```

Example:

```text
Sale ends: 12:00
Current:   12:01
           ↓
       Sale inactive
```

Therefore:

```text
ACTIVE
  +
start time reached
  +
end time not passed
  ↓
SALE ACTIVE
```

---

## 3. `getApplicablePrice()`

This method determines which price should be used:

```java
public BigDecimal getApplicablePrice(Instant now)
```

If flash sale is active:

```text
flashSalePrice exists
        +
flashSalePrice > 0
        ↓
flashSalePrice
```

Otherwise:

```text
originalPrice
```

Example:

```text
Original price    = ₹3,000
Flash-sale price = ₹2,000
Current time     = during sale

Applicable price = ₹2,000
```

Outside the sale:

```text
Applicable price = ₹3,000
```

Using `BigDecimal` here is the right choice for monetary values.

---

## ⚠️ Important architectural point

The explanation says:

> "Enforces promotional pricing authoritatively on the backend."

The **idea is correct**, but `ProductResponse` itself isn't authoritative. It is only a DTO containing data received from `product-service`.

The authoritative pricing decision should ultimately happen in `OrderService` using trusted product-service data, ideally with the product service being the source of truth for product pricing.

So conceptually:

```text
Product Service
     ↓
Trusted Product Data
     ↓
ProductClient
     ↓
ProductResponse
     ↓
OrderService
     ↓
Applicable price
```

---

## ⚠️ `stock` should not be trusted for checkout

You have:

```java
private Integer stock;
```

That's useful for displaying/catalog information, but **OrderService should not use this field to decide whether inventory is available**.

For example:

```text
ProductResponse:
stock = 1

Two users:
A → checkout
B → checkout
```

Both could receive `stock = 1`.

The actual reservation must go through:

```text
OrderService
    ↓
InventoryService
    ↓
Redis Lua
```

because your Inventory Service is responsible for atomic stock reservation.

So:

```text
ProductResponse.stock
        ↓
display/information

InventoryService
        ↓
authoritative reservation
```

That's an important distinction for your flash-sale architecture.

---

## One small improvement

For status:

```java
private String status;
```

A `String` works, especially when matching the Product Service contract.

But if both services share the same status enum through `common`, an enum could provide stronger type safety. **Don't change it blindly**, though—the DTO must match the actual Product Service API contract.

---

### Verdict

**✅ Keep this file.**

Its responsibilities are appropriately limited:

```text
ProductResponse
├── Product metadata
├── Pricing information
├── Sale window
└── Sale-price helper methods
```

And importantly:

```text
ProductResponse.stock
       ≠
Inventory reservation authority
```

The next logical file is **`ProductClient.java`**, because that will show exactly how `order-service` retrieves this DTO from `product-service`.
*/