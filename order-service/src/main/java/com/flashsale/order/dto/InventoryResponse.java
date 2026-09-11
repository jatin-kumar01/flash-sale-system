package com.flashsale.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private Integer availableStock;
    private Integer lockedStock;
    private Integer totalStock;
    private boolean cached;
    private Instant timestamp;
}

/*This `InventoryResponse.java` is a **DTO inside `order-service`** that represents the response received from `inventory-service`.

### Flow

```text
order-service
     │
     ↓
InventoryClient
     │
     ↓ REST / Feign
inventory-service
     │
     ↓
InventoryResponse
     │
     ↓
order-service
```

The class contains exactly the inventory fields your client needs:

```text
productId
availableStock
lockedStock
totalStock
cached
timestamp
```

### Field-by-field

| Field            | Meaning                                     |
| ---------------- | ------------------------------------------- |
| `productId`      | Product whose inventory is being returned   |
| `availableStock` | Stock currently available for reservation   |
| `lockedStock`    | Stock currently reserved/locked             |
| `totalStock`     | Total inventory represented by the response |
| `cached`         | Whether the response came from Redis/cache  |
| `timestamp`      | Time associated with the inventory response |

### Why duplicate `InventoryResponse`?

You already have:

```text
inventory-service
└── dto/
    └── InventoryResponse.java
```

and now:

```text
order-service
└── dto/
    └── InventoryResponse.java
```

That's **normal in a microservice architecture**.

Don't make `order-service` directly depend on inventory-service's internal DTO class. Each service owns its own API contract.

```text
Inventory Service
      ↓
its InventoryResponse
      ↓
     JSON
      ↓
InventoryClient
      ↓
Order Service's InventoryResponse
```

Jackson/Feign maps the JSON fields into the local DTO.

### Why `Serializable`?

```java
public class InventoryResponse implements Serializable
```

It allows the object to be serialized by Java serialization mechanisms.

However, **Feign/Jackson does not require `Serializable` just to deserialize JSON**. So this isn't harmful, but it isn't necessary for the REST response itself.

### Lombok

```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

provides getters, setters, constructors and builder support without writing boilerplate.

Example:

```java
InventoryResponse response =
        InventoryResponse.builder()
                .productId(101L)
                .availableStock(95)
                .lockedStock(5)
                .totalStock(100)
                .cached(true)
                .build();
```

### One important point

The statement:

> "matches the serialization schema ... so Feign and Jackson can deserialize"

is correct **provided the actual `inventory-service` `InventoryResponse` has these same JSON property names/types**.

Also, if your Inventory Service wraps it like:

```json
{
  "success": true,
  "message": "Inventory retrieved successfully",
  "data": {
    "productId": 101,
    "availableStock": 95,
    "lockedStock": 5,
    "totalStock": 100,
    "cached": true,
    "timestamp": "..."
  }
}
```

then `InventoryClient` must deserialize the **`ApiResponse<InventoryResponse>` wrapper**, not directly `InventoryResponse`.

### Verdict

**✅ Keep this DTO.** It's appropriately placed in `order-service` and keeps the microservices decoupled.

The next file to inspect is **`InventoryClient.java`**, because that is where this DTO is actually used to communicate between Order Service and Inventory Service.
*/
