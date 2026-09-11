package com.flashsale.payment.controller;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.payment.dto.PaymentRequest;
import com.flashsale.payment.dto.PaymentResponse;
import com.flashsale.payment.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PaymentRequest request) {

        log.info("Received payment processing request for user: {}, orderReference: {}, amount: {}",
                userId, request.getOrderReference(), request.getAmount());

        PaymentResponse response = paymentService.processPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", response));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTransactionId(
            @PathVariable("transactionId") String transactionId) {

        log.debug("Fetching payment details for transactionId: {}", transactionId);
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    @GetMapping("/order/{orderReference}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderReference(
            @PathVariable("orderReference") String orderReference) {

        log.debug("Fetching payment details for orderReference: {}", orderReference);
        PaymentResponse response = paymentService.getPaymentByOrderReference(orderReference);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    @GetMapping("/my-payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getUserPayments(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("Fetching paginated payment history for user: {}", userId);
        Page<PaymentResponse> response = paymentService.getUserPayments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("User payments retrieved successfully", response));
    }
}
/*Yes — this `PaymentController.java` is **clean and appropriately thin**. It correctly acts as the REST/API layer and delegates business logic to `PaymentService`.

## 1. Overall flow

Your payment service now has this complete structure:

```text
Client / Frontend
       ↓
API Gateway
       ↓
PaymentController
       ↓
PaymentService
       ↓
PaymentRepository
       ↓
PostgreSQL
```

And for successful/failed payment events:

```text
PaymentService
      ↓
PaymentProducer
      ↓
Kafka
      ↓
OrderService
      ↓
InventoryService
```

---

## 2. `POST /api/payments`

```java
@PostMapping
public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
        @RequestHeader("X-User-Id") Long userId,
        @Valid @RequestBody PaymentRequest request)
```

This endpoint receives:

```text
POST /api/payments
```

with something like:

```json
{
  "orderReference": "ORD-1001",
  "amount": 999.99,
  "paymentMethod": "UPI",
  "idempotencyKey": "ABC123"
}
```

And:

```java
@RequestHeader("X-User-Id") Long userId
```

gets the user ID from the request header.

Then:

```java
paymentService.processPayment(userId, request);
```

hands the actual work to the service.

### This separation is correct:

```text
Controller
    ↓
Receive request
Validate request
Call service
Return response

Service
    ↓
Business logic
Database
Kafka
Idempotency
```

The controller should **not** contain payment-processing logic.

---

## 3. `@Valid`

```java
@Valid @RequestBody PaymentRequest request
```

This activates the validation annotations from your `PaymentRequest`.

For example:

```java
@NotBlank
private String orderReference;
```

and:

```java
@NotNull
@DecimalMin("0.01")
private BigDecimal amount;
```

So invalid requests are rejected before reaching the main payment logic.

That's good.

---

## 4. `X-User-Id` — important security point ⚠️

Your explanation says:

> verified user ID forwarded by API Gateway following JWT signature verification.

That is fine **if your gateway is actually responsible for validating the JWT and securely setting/overwriting this header**.

The payment service should not blindly trust a client-controlled:

```text
X-User-Id: 999
```

because a malicious client could try to impersonate another user.

Your intended flow should be:

```text
Client
  ↓
JWT
  ↓
API Gateway
  ↓
JWT validation
  ↓
Gateway determines userId
  ↓
Gateway sets X-User-Id
  ↓
Payment Service
```

And ideally the gateway should prevent clients from supplying their own trusted identity header.

So the controller itself is okay; the **security guarantee belongs to the gateway/security architecture**.

---

# 5. `201 CREATED`

```java
return ResponseEntity.status(HttpStatus.CREATED)
```

For a newly created payment transaction, `201 Created` is reasonable.

However, there's a subtle point.

Your service can return an **existing payment** when the same idempotency key is reused:

```java
return PaymentResponse.fromEntity(existingByTxn.get());
```

But the controller still returns:

```text
201 CREATED
```

even when no new payment was created.

That's not necessarily disastrous, but semantically you may eventually want to distinguish:

```text
New payment      → 201
Existing retry   → 200
```

This would require the service to communicate whether the response was newly created or reused.

**Don't change this yet unless you want strict HTTP semantics.**

---

# 6. Transaction lookup

```java
@GetMapping("/transaction/{transactionId}")
```

Example:

```text
GET /api/payments/transaction/TXN-ABC123
```

It calls:

```java
paymentService.getPaymentByTransactionId(transactionId);
```

and returns the payment details.

Good separation.

---

# 7. Order lookup

```java
@GetMapping("/order/{orderReference}")
```

Example:

```text
GET /api/payments/order/ORD-1001
```

Useful for an order-status page:

```text
Order
ORD-1001

Payment
SUCCESS

Transaction
TXN-ABC123
```

Again, controller responsibility is appropriate.

---

# 8. User payment history

```java
@GetMapping("/my-payments")
```

This is a nice endpoint for the user's payment history.

The important part is:

```java
@PageableDefault(
    size = 10,
    sort = "createdAt",
    direction = Sort.Direction.DESC
)
Pageable pageable
```

So by default:

```text
Page size = 10
Sort = createdAt
Order = newest → oldest
```

For example:

```text
GET /api/payments/my-payments?page=0&size=10
```

The service then returns:

```java
Page<PaymentResponse>
```

instead of loading every payment record.

Good choice.

---

# 9. `ApiResponse<T>`

Every endpoint uses:

```java
ApiResponse<PaymentResponse>
```

or:

```java
ApiResponse<Page<PaymentResponse>>
```

This gives your APIs a consistent response structure.

For example, assuming your common `ApiResponse` follows the usual structure:

```json
{
  "success": true,
  "message": "Payment processed successfully",
  "data": {
    "transactionId": "TXN-ABC123",
    "status": "SUCCESS"
  }
}
```

That's useful across microservices because clients don't have to learn a different response format for every service.

---

# 10. Logging

For payment processing:

```java
log.info(...)
```

For queries:

```java
log.debug(...)
```

That's reasonable.

One thing to watch in a real payment system: **don't log sensitive payment information**.

Currently you're logging:

```java
userId
orderReference
amount
```

Those may be acceptable depending on your logging policy, but never log things like:

```text
Card number
CVV
Full bank credentials
UPI authentication information
Payment secrets
```

Your current code doesn't do that, which is good.

---

# 11. One architecture issue to remember

Your current flow is:

```text
PaymentController
       ↓
PaymentService
       ↓
Payment DB
       ↓
PaymentProducer
       ↓
Kafka
```

As we discussed in `PaymentService`, the controller itself is **not the problem**.

The bigger production concern remains:

```text
Payment DB SUCCESS
       ↓
Kafka send
       ↓
❌ failure
```

So eventually you'll want:

```text
PaymentController
       ↓
PaymentService
       ↓
┌─────────────────────┐
│ Payment + Outbox    │
│ SAME DB TRANSACTION │
└──────────┬──────────┘
           ↓
    Outbox Publisher
           ↓
         Kafka
```

That change belongs primarily around the service/outbox architecture, **not in this controller**.

---

# Final verdict

### `PaymentController.java` — ✅ Keep it

Your controller is doing what a controller should do:

```text
Receive HTTP request
       ↓
Validate input
       ↓
Extract authenticated identity
       ↓
Call service
       ↓
Wrap result
       ↓
Return HTTP response
```

No business logic has leaked into the controller. That's exactly what we want.

### Payment service is now:

```text
payment-service/
│
├── controller
│     └── PaymentController       ✅ API layer
│
├── dto
│     ├── PaymentRequest          ✅ Input
│     └── PaymentResponse         ✅ Output
│
├── entity
│     └── Payment                 ✅ Database model
│
├── repository
│     └── PaymentRepository       ✅ Database access
│
├── service
│     └── PaymentService          ⚠️ Business logic
│
└── kafka
      └── PaymentProducer         ⚠️ Event publishing
```

The **Payment Service module is now functionally complete at the basic level**.

The remaining `application.yml` is important because it determines the actual **database, Kafka, Eureka, server port, and payment simulation configuration**. That's the next file I'd review before moving to the next microservice.
*/