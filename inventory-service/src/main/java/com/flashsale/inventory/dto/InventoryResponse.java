package com.flashsale.inventory.dto;

import com.flashsale.inventory.entity.Inventory;
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

    public static InventoryResponse fromEntity(Inventory inventory, boolean cached) {
        if (inventory == null) {
            return null;
        }

        return InventoryResponse.builder()
                .productId(inventory.getProductId())
                .availableStock(inventory.getAvailableStock())
                .lockedStock(inventory.getLockedStock())
                .totalStock(inventory.getTotalStock())
                .cached(cached)
                .timestamp(Instant.now())
                .build();
    }

    public static InventoryResponse fromCache(Long productId, Integer availableStock, Integer lockedStock) {
        int available = availableStock != null ? availableStock : 0;
        int locked = lockedStock != null ? lockedStock : 0;

        return InventoryResponse.builder()
                .productId(productId)
                .availableStock(available)
                .lockedStock(locked)
                .totalStock(available + locked)
                .cached(true)
                .timestamp(Instant.now())
                .build();
    }
}

/*Yes — this `InventoryReservationRequest.java` is correct as the **request DTO** for the Inventory Service.

### What this class does

It represents the data coming into the inventory API:

```text id="l2q8eu"
Client
  ↓
InventoryController
  ↓
InventoryReservationRequest
  │
  ├── productId
  ├── quantity
  └── orderReference
```

### 1. `productId`

```java
@NotNull(message = "Product ID is required")
private Long productId;
```

Ensures the request contains a product ID.

Example:

```json
{
  "productId": 101
}
```

Without `productId` → validation fails with **400 Bad Request**.

---

### 2. `quantity`

```java
@NotNull(message = "Reservation quantity is required")
@Min(value = 1, message = "Quantity must be at least 1")
private Integer quantity;
```

Two validations happen:

```text
quantity = null → ❌
quantity = 0    → ❌
quantity = -2   → ❌
quantity = 1    → ✅
quantity = 5    → ✅
```

This is important because we don't want something like:

```text
Reserve -5 items
```

which could potentially corrupt inventory calculations.

---

### 3. `orderReference`

```java
@NotBlank(message = "Order reference is required for idempotent tracking")
private String orderReference;
```

This identifies the order/reservation.

Example:

```json
{
  "productId": 101,
  "quantity": 2,
  "orderReference": "ORD-2026-10001"
}
```

Later, if the same request arrives again:

```text
ORD-2026-10001
       ↓
Already processed?
       ↓
     YES
       ↓
Don't reserve stock again
```

**Important:** this DTO only *carries* the idempotency key. The actual duplicate-checking logic still needs to be implemented in the service/database layer.

---

### Lombok annotations

```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

They generate the usual boilerplate:

```text
@Getter          → getters
@Setter          → setters
@Builder         → builder pattern
@NoArgsConstructor → empty constructor
@AllArgsConstructor → constructor with all fields
```

So you can write:

```java
InventoryReservationRequest request =
        InventoryReservationRequest.builder()
                .productId(101L)
                .quantity(2)
                .orderReference("ORD-10001")
                .build();
```

instead of manually calling setters.

### Overall flow

```text
POST /inventory/reserve
          ↓
@Valid
          ↓
InventoryReservationRequest
          ↓
Validation
     ↙          ↘
Invalid          Valid
  ↓                ↓
400 Error    InventoryService
                  ↓
              Redis / DB
```

### One architectural note

Your description says this DTO is also for **stock rollbacks and payment deduction settlements**. That's possible, but it's not necessarily ideal because all three operations don't have exactly the same semantics.

For example:

```text
Reservation:
productId + quantity + orderReference

Release:
productId + quantity + orderReference

Settlement:
productId + quantity + orderReference
```

They currently happen to share the same fields, so reusing this DTO is reasonable **for now**. If the operations later need different fields, separate DTOs would be cleaner.

**Verdict: ✅ Keep this file as-is for the current architecture.**

The next logical file is `InventoryResponse.java`, which defines what the Inventory Service sends back to the client.
*/
