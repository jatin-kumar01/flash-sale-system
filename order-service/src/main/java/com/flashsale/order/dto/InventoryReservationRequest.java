package com.flashsale.order.dto;

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
public class InventoryReservationRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Reservation quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Order reference is required")
    private String orderReference;
}
/*This `InventoryReservationRequest.java` in the **order-service** is correct and is intentionally almost identical to the DTO in `inventory-service`.

### Purpose

It is the object that `order-service` sends to `inventory-service` through OpenFeign:

```text id="r3q7c1"
OrderService
    ↓
InventoryClient
    ↓
InventoryReservationRequest
    ↓
HTTP / OpenFeign
    ↓
InventoryService
```

### Fields

```java
private Long productId;
private Integer quantity;
private String orderReference;
```

Example payload sent to Inventory Service:

```json
{
  "productId": 101,
  "quantity": 2,
  "orderReference": "ORD-10001"
}
```

This matches the Inventory Service DTO you created earlier, which is important for Jackson/OpenFeign serialization.

### Validation

```java
@NotNull
private Long productId;
```

Prevents a missing product ID.

```java
@NotNull
@Min(1)
private Integer quantity;
```

Prevents:

```text
null ❌
0    ❌
-1   ❌
1    ✅
```

And:

```java
@NotBlank
private String orderReference;
```

ensures the order has an identifier that can be carried through the inventory lifecycle.

### Why have the DTO in both services?

You have:

```text
order-service
└── dto/InventoryReservationRequest.java

inventory-service
└── dto/InventoryReservationRequest.java
```

That's actually reasonable in a microservice architecture.

Each service owns its own API contract instead of directly depending on another service's internal DTO class.

```text id="q2h8kx"
Order Service DTO
       │
       │ HTTP/JSON
       ↓
Inventory Service DTO
```

They don't need to be the **same Java class**; they only need compatible JSON fields.

### One small point

The validation annotations on this DTO primarily protect the **Order Service's own boundary**. Simply putting `@Valid` annotations on an object sent through OpenFeign does not automatically mean the remote Inventory Service will validate it. The Inventory Service should continue validating its own incoming request—which it already does.

### Verdict

**✅ Keep this file as-is.**

The architecture is clean:

```text
Order.java
    ↓
OrderService
    ↓
InventoryReservationRequest
    ↓
InventoryClient
    ↓
Inventory Service
```

The next logical file is **`InventoryClient.java`**, because that will show exactly how `order-service` communicates with the Inventory Service.
*/