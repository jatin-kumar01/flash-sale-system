package com.flashsale.order.controller;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.order.dto.OrderCreateRequest;
import com.flashsale.order.dto.OrderResponse;
import com.flashsale.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        log.info("Received checkout request for user: {}, productId: {}, idempotencyKey: {}",
                userId, request.getProductId(), request.getIdempotencyKey());
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully. Please complete payment before deadline.", response));
    }

    @GetMapping("/{orderReference}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByReference(
            @PathVariable("orderReference") String orderReference) {
        log.debug("Fetching order details for reference: {}", orderReference);
        OrderResponse response = orderService.getOrderByReference(orderReference);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", response));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Fetching paginated orders for user: {}", userId);
        Page<OrderResponse> response = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("User orders retrieved successfully", response));
    }

    @PostMapping("/{orderReference}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderReference") String orderReference) {
        log.info("Received cancellation request for order: {} by user: {}", orderReference, userId);
        OrderResponse response = orderService.cancelOrder(userId, orderReference);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }
}
/*This `OrderController.java` is the **REST API entry point for the Order Service**. Overall, the controller is clean and follows the correct principle: **controller handles HTTP, `OrderService` handles business logic**.

## Overall flow

```text id="7jz0kb"
Frontend
   ↓
API Gateway
   ↓
OrderController
   ↓
OrderService
   ├── ProductClient
   ├── InventoryClient
   ├── PostgreSQL
   └── Kafka
```

Your structure has the controller alongside clients, DTOs, entity, Kafka producer, repository, and service.

---

## 1. `POST /api/orders`

```java
@PostMapping
public ResponseEntity<ApiResponse<OrderResponse>> createOrder(...)
```

Example request:

```json
{
  "productId": 101,
  "quantity": 2,
  "idempotencyKey": "REQ-12345"
}
```

The user ID comes from:

```java
@RequestHeader("X-User-Id") Long userId
```

So the intended flow is:

```text
JWT
 ↓
API Gateway
 ↓
X-User-Id: 501
 ↓
OrderController
 ↓
OrderService
```

That's a reasonable microservice pattern **if the gateway securely validates the JWT and prevents clients from spoofing this header**.

### Response

```java
HttpStatus.CREATED
```

`201 Created` makes sense for a newly created order.

---

# 2. `GET /api/orders/{orderReference}`

```java
@GetMapping("/{orderReference}")
```

Example:

```text
GET /api/orders/ORD-10001
```

Returns:

```text
OrderResponse
 ├── orderReference
 ├── productId
 ├── quantity
 ├── price
 ├── status
 └── paymentDeadline
```

One security point: this endpoint currently **doesn't receive `X-User-Id`**.

That means `OrderService.getOrderByReference()` must make sure users cannot retrieve another user's order simply by guessing an order reference.

For example:

```text
User A
 ↓
GET /ORD-USER-B-001
 ↓
Should be rejected ❌
```

So this should eventually be authorization-aware.

---

# 3. `GET /api/orders/my-orders`

```java
@GetMapping("/my-orders")
```

This is nicely designed for pagination:

```java
@PageableDefault(
    size = 10,
    sort = "createdAt",
    direction = Sort.Direction.DESC
)
Pageable pageable
```

Example:

```text
GET /api/orders/my-orders?page=0&size=10
```

Conceptually:

```text
Page 0
 ↓
10 newest orders

Page 1
 ↓
next 10 orders
```

And:

```text
createdAt DESC
```

means newest orders appear first.

### One important security advantage

Here:

```java
@RequestHeader("X-User-Id") Long userId
```

the service can query:

```text
WHERE user_id = currentUser
```

rather than allowing the client to provide an arbitrary user ID.

Good.

---

# 4. `POST /{orderReference}/cancel`

```java
@PostMapping("/{orderReference}/cancel")
```

Example:

```text
POST /api/orders/ORD-10001/cancel
```

with:

```text
X-User-Id: 501
```

The service receives:

```java
orderService.cancelOrder(userId, orderReference);
```

This is good because the **service layer should verify ownership and current order status**.

For example:

```text
PENDING_PAYMENT → CANCELLED    ✅
PAID            → CANCELLED    ❌
EXPIRED         → CANCELLED    ❌
```

Then the cancellation should eventually generate the appropriate inventory compensation event.

---

# ⚠️ Important issue: endpoint ordering

You have:

```text
GET /{orderReference}
GET /my-orders
```

Spring's request mapping generally resolves the more specific literal mapping appropriately, so this isn't necessarily a runtime problem.

But for readability, I'd personally keep:

```text
/my-orders
/{orderReference}
```

grouped with the more specific routes first in the source code.

Not mandatory, just cleaner.

---

# ⚠️ Important issue: `X-User-Id` must not be blindly trusted

The comment says the gateway populates:

```text
X-User-Id
```

That's okay **only if the architecture guarantees that external clients cannot directly reach Order Service**.

Correct:

```text
Internet
   ↓
Gateway
   ↓ JWT validation
X-User-Id
   ↓
Order Service
```

Dangerous:

```text
Internet
   ↓
Order Service directly
   ↓
X-User-Id: 999
```

A malicious client could simply send another user's ID.

So later your architecture should ensure:

```text
Gateway → authentication
Order Service → authorization/business ownership checks
```

---

# ⚠️ Important issue: idempotency

Your create request contains:

```java
request.getIdempotencyKey()
```

This is excellent for flash-sale checkout.

But the controller merely passes it to:

```java
orderService.createOrder(userId, request);
```

The **actual idempotency guarantee must be implemented in `OrderService` + database**.

For example:

```text
Request 1
REQ-123
   ↓
Create order

Request 2
REQ-123
   ↓
Existing request?
   ↓
Return same order
```

Never create:

```text
ORD-001
ORD-002
```

for the same idempotency key.

---

## Controller architecture

Your controller should remain roughly this simple:

```text
                    OrderController
                          │
       ┌──────────────────┼──────────────────┐
       ↓                  ↓                  ↓
    Create              Query             Cancel
       │                  │                  │
       └──────────────────┼──────────────────┘
                          ↓
                    OrderService
                          ↓
             Business logic / transactions
```

Don't put:

* Redis logic
* inventory reservation logic
* database transactions
* Kafka publishing
* payment logic

directly inside this controller.

---

## Final verdict

**Controller structure: ✅ Good**

### Keep

* `@Valid`
* `ApiResponse<T>`
* pagination
* `X-User-Id` propagation
* thin controller
* service delegation
* `201` for new order

### Must verify/fix in `OrderService`

1. **Idempotency must actually be implemented**
2. Order ownership must be checked
3. `GET /{orderReference}` must not expose another user's order
4. `X-User-Id` must only be trusted from the secured gateway path
5. Order creation + inventory reservation needs a well-defined Saga/transaction strategy

The **next most important file is `OrderCreateRequest.java`**, because its fields determine how your flash-sale checkout and idempotency mechanism starts.
*/