package com.flashsale.inventory.controller;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.inventory.dto.InventoryReservationRequest;
import com.flashsale.inventory.dto.InventoryResponse;
import com.flashsale.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable("productId") Long productId) {
        log.debug("Fetching inventory status for productId: {}", productId);
        InventoryResponse response = inventoryService.getInventory(productId);
        return ResponseEntity.ok(ApiResponse.success("Inventory retrieved successfully", response));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<InventoryResponse>> reserveStock(
            @Valid @RequestBody InventoryReservationRequest request) {
        log.info("Received stock reservation request for productId: {}, orderRef: {}",
                request.getProductId(), request.getOrderReference());
        InventoryResponse response = inventoryService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Stock reserved successfully", response));
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @Valid @RequestBody InventoryReservationRequest request) {
        log.info("Received stock release request for productId: {}, orderRef: {}",
                request.getProductId(), request.getOrderReference());
        inventoryService.releaseStock(request);
        return ResponseEntity.ok(ApiResponse.success("Stock released successfully", null));
    }

    @PostMapping("/settle")
    public ResponseEntity<ApiResponse<Void>> settleOrderDeduction(
            @Valid @RequestBody InventoryReservationRequest request) {
        log.info("Received stock settlement request for productId: {}, orderRef: {}",
                request.getProductId(), request.getOrderReference());
        inventoryService.settleOrderDeduction(request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Stock settled successfully", null));
    }

    @PostMapping("/replenish")
    public ResponseEntity<ApiResponse<InventoryResponse>> replenishStock(
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") int quantity) {
        log.info("Received stock replenishment request for productId: {}, quantity: {}", productId, quantity);
        InventoryResponse response = inventoryService.replenishStock(productId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stock replenished successfully", response));
    }
}
/*This `InventoryController.java` is the **API entry point** for your Inventory Service. Its structure is good, but there are a few things I would change before we consider the inventory API final.

## 1. Overall flow

```text
Client / Order Service
        ↓
InventoryController
        ↓
InventoryService
        ↓
┌───────────┬────────────┬─────────┐
│   Redis   │ PostgreSQL │  Kafka  │
└───────────┴────────────┴─────────┘
```

The controller itself should stay **thin**: receive/validate requests, call `InventoryService`, and return `ApiResponse`.

---

## 2. `GET /api/inventory/{productId}`

```java
@GetMapping("/{productId}")
```

Example:

```http
GET /api/inventory/101
```

Flow:

```text
Controller
   ↓
inventoryService.getInventory(101)
   ↓
Redis
   ↓
if missing → PostgreSQL → Redis
   ↓
InventoryResponse
```

This is correctly delegated to the service.

---

## 3. `POST /reserve`

```java
@PostMapping("/reserve")
```

Example request:

```json
{
  "productId": 101,
  "quantity": 2,
  "orderReference": "ORD-1001"
}
```

The important part is:

```java
@Valid @RequestBody InventoryReservationRequest request
```

Spring validates:

```text
productId     → required
quantity      → >= 1
orderReference → required
```

Then:

```java
inventoryService.reserveStock(request);
```

The controller doesn't perform inventory calculations itself. **Good separation.**

---

## 4. `POST /release`

```java
@PostMapping("/release")
```

Example:

```json
{
  "productId": 101,
  "quantity": 2,
  "orderReference": "ORD-1001"
}
```

This is used when:

```text
Payment failed
      OR
Order cancelled
      OR
Reservation expired
```

Then:

```text
locked → available
```

through `InventoryService`.

---

## 5. `POST /settle`

```java
@PostMapping("/settle")
```

This represents successful payment/order settlement.

Currently:

```java
inventoryService.settleOrderDeduction(
        request.getProductId(),
        request.getQuantity()
);
```

### ⚠️ Problem

You are receiving:

```text
orderReference
```

but **not using it**.

That's a problem because your architecture says `orderReference` is being used for idempotency.

Imagine the same settlement request arrives twice:

```text
ORD-1001
quantity = 2
```

The controller passes only:

```text
productId = 101
quantity = 2
```

So `InventoryService` has no way to identify that:

```text
ORD-1001
```

was already settled.

### Better design

Pass the entire request:

```java
inventoryService.settleOrderDeduction(request);
```

Then the service can eventually perform:

```text
orderReference
      ↓
Already settled?
   ↙       ↘
 YES        NO
  ↓          ↓
Ignore    Settle
```

This becomes especially important when Kafka retries or clients retry requests.

---

# 6. `POST /replenish`

```java
@PostMapping("/replenish")
```

Current API:

```http
POST /api/inventory/replenish?productId=101&quantity=50
```

This calls:

```java
inventoryService.replenishStock(productId, quantity);
```

and the service uses the Redisson lock.

That's fine for an **internal/admin endpoint**, but this endpoint should eventually be protected by authorization.

For example:

```text
ADMIN
  ↓
/replenish
  ↓
Allowed

NORMAL USER
  ↓
/replenish
  ↓
403 Forbidden
```

Spring Security should handle this later rather than putting authorization logic inside this controller.

---

# 7. HTTP status codes

Current choices:

```text
GET        → 200 OK
reserve    → 200 OK
release    → 200 OK
settle     → 200 OK
replenish  → 201 CREATED
```

These are reasonable for the current design.

One thing to think about later: a successful **reservation** isn't necessarily resource creation in the traditional REST sense, so `200 OK` is perfectly defensible here.

---

# 8. One thing I would NOT put here

Don't put Redis logic here:

```java
// ❌ Don't do this
redisTemplate.decrement(...)
```

Don't put database logic here:

```java
// ❌ Don't do this
inventoryRepository.save(...)
```

And don't put Kafka logic here:

```java
// ❌ Don't do this
kafkaTemplate.send(...)
```

Keep:

```text
Controller
    ↓
Service
    ↓
Repository / Redis / Kafka
```

This keeps your microservice maintainable.

---

## Final structure

Your current architecture is becoming:

```text
inventory/
│
├── config/
│   └── RedisInventoryScriptConfig
│
├── controller/
│   └── InventoryController
│
├── dto/
│   ├── InventoryReservationRequest
│   └── InventoryResponse
│
├── entity/
│   └── Inventory
│
├── kafka/
│   └── InventoryProducer
│
├── repository/
│   └── InventoryRepository
│
└── service/
    ├── InventoryRedisService
    └── InventoryService
```

That layering is good.

### 🔴 One change I recommend now

Change settlement from:

```java
inventoryService.settleOrderDeduction(
        request.getProductId(),
        request.getQuantity()
);
```

to eventually:

```java
inventoryService.settleOrderDeduction(request);
```

because otherwise your `orderReference`/idempotency design isn't actually being used for settlement.

Apart from that, **the controller is appropriately thin and doesn't need major restructuring**.
*/