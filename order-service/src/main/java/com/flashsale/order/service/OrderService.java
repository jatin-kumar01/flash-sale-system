package com.flashsale.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.dto.ApiResponse;
import com.flashsale.common.event.OrderEvent;
import com.flashsale.common.exception.InsufficientStockException;
import com.flashsale.common.exception.InvalidRequestException;
import com.flashsale.common.exception.ResourceNotFoundException;
import com.flashsale.common.outbox.OutboxStatus;
import com.flashsale.order.client.InventoryClient;
import com.flashsale.order.client.ProductClient;
import com.flashsale.order.dto.InventoryReservationRequest;
import com.flashsale.order.dto.OrderCreateRequest;
import com.flashsale.order.dto.OrderResponse;
import com.flashsale.order.dto.ProductResponse;
import com.flashsale.order.entity.Order;
import com.flashsale.order.entity.Order.OrderStatus;
import com.flashsale.order.entity.OrderOutbox;
import com.flashsale.order.kafka.OrderProducer;
import com.flashsale.order.repository.OrderOutboxRepository;
import com.flashsale.order.repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderProducer orderProducer;
    private final ObjectMapper objectMapper;

    @Value("${app.order.payment-timeout-minutes:15}")
    private int paymentTimeoutMinutes;

    @Value("${app.kafka.topics.order-created:order.created}")
    private String orderCreatedTopic;

    @Value("${app.kafka.topics.order-cancelled:order.cancelled}")
    private String orderCancelledTopic;

    @Value("${app.kafka.topics.order-expired:order.expired}")
    private String orderExpiredTopic;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        String orderReference = "ORD-" + request.getIdempotencyKey();

        // 1. Idempotency Check
        if (orderRepository.existsByOrderReference(orderReference)) {
            log.info("Duplicate order creation detected for reference: {}", orderReference);
            Order existingOrder = orderRepository.findByOrderReference(orderReference)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "orderReference", orderReference));
            return OrderResponse.fromEntity(existingOrder);
        }

        Instant now = Instant.now();

        // 2. Validate Product & Flash Sale Eligibility via ProductClient
        ApiResponse<ProductResponse> productApiResponse;
        try {
            productApiResponse = productClient.getProductById(request.getProductId());
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Product", "id", request.getProductId());
        } catch (FeignException ex) {
            log.error("Failed to fetch product details from product-service for id: {}", request.getProductId(), ex);
            throw new InvalidRequestException("Product service is currently unavailable. Please try again.");
        }

        ProductResponse product = productApiResponse.getData();
        if (product == null) {
            throw new ResourceNotFoundException("Product", "id", request.getProductId());
        }

        if (!product.isSaleActive(now)) {
            throw new InvalidRequestException("Product is not currently available for flash sale purchase");
        }

        BigDecimal unitPrice = product.getApplicablePrice(now);
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // 3. Synchronously Reserve Inventory via OpenFeign
        InventoryReservationRequest reservationRequest = InventoryReservationRequest.builder()
                .productId(product.getId())
                .quantity(request.getQuantity())
                .orderReference(orderReference)
                .build();

        try {
            inventoryClient.reserveStock(reservationRequest);
        } catch (FeignException.BadRequest | FeignException.Conflict ex) {
            log.warn("Inventory reservation rejected for productId: {}, quantity: {}", product.getId(), request.getQuantity());
            throw new InsufficientStockException("Insufficient stock available for product: " + product.getName());
        } catch (FeignException ex) {
            log.error("Error communicating with inventory-service during stock reservation for order: {}", orderReference, ex);
            throw new InvalidRequestException("Inventory service is currently unavailable. Please retry.");
        }

        // 4. Persist Order in PENDING_PAYMENT Status
        Instant paymentDeadline = now.plus(paymentTimeoutMinutes, ChronoUnit.MINUTES);
        Order order = Order.builder()
                .orderReference(orderReference)
                .userId(userId)
                .productId(product.getId())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentDeadline(paymentDeadline)
                .build();

        Order savedOrder;
        try {
            savedOrder = orderRepository.save(order);
        } catch (Exception ex) {
            // Compensating transaction: Release reserved inventory if DB persistence fails
            log.error("Database persistence failed for order: {}. Triggering compensating inventory release.", orderReference, ex);
            compensateStockReservation(reservationRequest);
            throw ex;
        }

        // 5. Stage OrderCreated Outbox Event
        OrderEvent event = OrderEvent.builder()
                .orderReference(savedOrder.getOrderReference())
                .userId(savedOrder.getUserId())
                .productId(savedOrder.getProductId())
                .quantity(savedOrder.getQuantity())
                .totalAmount(savedOrder.getTotalAmount())
                .eventType("ORDER_CREATED")
                .occurredAt(Instant.now())
                .build();
        stageOutboxEvent(orderCreatedTopic, savedOrder.getOrderReference(), "ORDER_CREATED", event);

        log.info("Order successfully created with reference: {}, awaiting payment until: {}",
                savedOrder.getOrderReference(), savedOrder.getPaymentDeadline());

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, String orderReference) {
        Order order = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderReference", orderReference));

        if (!order.getUserId().equals(userId)) {
            throw new InvalidRequestException("You are not authorized to cancel this order");
        }

        order.cancel();
        Order updatedOrder = orderRepository.save(order);

        // Stage OrderCancelled Outbox Event to trigger inventory release saga
        OrderEvent event = OrderEvent.builder()
                .orderReference(updatedOrder.getOrderReference())
                .userId(updatedOrder.getUserId())
                .productId(updatedOrder.getProductId())
                .quantity(updatedOrder.getQuantity())
                .totalAmount(updatedOrder.getTotalAmount())
                .eventType("ORDER_CANCELLED")
                .occurredAt(Instant.now())
                .build();
        stageOutboxEvent(orderCancelledTopic, updatedOrder.getOrderReference(), "ORDER_CANCELLED", event);

        log.info("Order {} cancelled by user {}. Staged cancellation outbox event.", orderReference, userId);
        return OrderResponse.fromEntity(updatedOrder);
    }

    @Transactional
    public void markOrderAsPaid(String orderReference) {
        Order order = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderReference", orderReference));

        order.markAsPaid();
        orderRepository.save(order);

        // Settle inventory deduction directly
        InventoryReservationRequest settleRequest = InventoryReservationRequest.builder()
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .orderReference(order.getOrderReference())
                .build();

        try {
            inventoryClient.settleStock(settleRequest);
        } catch (Exception ex) {
            log.error("Failed to settle stock directly with inventory-service for paid order: {}", orderReference, ex);
        }

        log.info("Order {} marked as PAID and stock settled.", orderReference);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByReference(String orderReference) {
        Order order = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderReference", orderReference));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(OrderResponse::fromEntity);
    }

    private void compensateStockReservation(InventoryReservationRequest reservationRequest) {
        try {
            inventoryClient.releaseStock(reservationRequest);
            log.info("Compensating inventory release succeeded for order: {}", reservationRequest.getOrderReference());
        } catch (Exception releaseEx) {
            log.error("CRITICAL: Failed to compensate inventory release for order: {}. Staging fallback cancellation outbox event.",
                    reservationRequest.getOrderReference(), releaseEx);
            OrderEvent fallbackEvent = OrderEvent.builder()
                    .orderReference(reservationRequest.getOrderReference())
                    .productId(reservationRequest.getProductId())
                    .quantity(reservationRequest.getQuantity())
                    .eventType("ORDER_CANCELLED")
                    .occurredAt(Instant.now())
                    .build();
            stageOutboxEvent(orderCancelledTopic, reservationRequest.getOrderReference(), "ORDER_CANCELLED", fallbackEvent);
        }
    }

    private void stageOutboxEvent(String topic, String orderRef, String eventType, Object eventPayload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(eventPayload);
            OrderOutbox outbox = OrderOutbox.builder()
                    .aggregateType("ORDER")
                    .aggregateId(orderRef)
                    .eventType(eventType)
                    .destinationTopic(topic)
                    .partitionKey(orderRef)
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .build();
            orderOutboxRepository.save(outbox);
            log.info("Staged outbox event [type={}, orderRef={}] for destination topic: {}",
                    eventType, orderRef, topic);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }
}
/*This `OrderService.java` is the **core business/orchestration layer** of your Order Service. It connects:

```text
Order Service
    │
    ├── Product Service
    ├── Inventory Service
    ├── PostgreSQL
    └── Kafka
```

The implementation has a good overall Saga-style flow, but there are **several important consistency problems** that should be fixed before calling it production-ready.

## 1. `createOrder()` — main flow

The intended flow is:

```text
Client
  ↓
OrderService
  ↓
Check idempotency
  ↓
Product Service
  ↓
Validate flash sale + price
  ↓
Inventory Service
  ↓
Reserve stock
  ↓
Save Order
  ↓
Publish ORDER_CREATED
```

This is a sensible high-level sequence.

---

## 2. Idempotency

You create:

```java
String orderReference = "ORD-" + request.getIdempotencyKey();
```

Then:

```java
orderRepository.existsByOrderReference(orderReference)
```

So if the client retries:

```text
Request 1 → idempotencyKey = ABC123
Request 2 → idempotencyKey = ABC123
```

both become:

```text
ORD-ABC123
```

and the existing order can be returned.

### ⚠️ But there is a race condition

This:

```java
if (!existsByOrderReference(...)) {
    save(...)
}
```

isn't sufficient by itself under high concurrency.

Two requests can do:

```text
Request A → exists? NO
Request B → exists? NO

Request A → save
Request B → save
```

Your database `UNIQUE` constraint on `orderReference` is the real protection.

So later we should handle the duplicate-key/database exception and return the existing order appropriately.

---

# 3. Product validation

This section is good:

```java
productClient.getProductById(request.getProductId());
```

Then:

```java
if (!product.isSaleActive(now))
```

and:

```java
BigDecimal unitPrice = product.getApplicablePrice(now);
```

This means the Order Service doesn't blindly trust the client-provided price.

That's important.

```text
Client
  ↓
"I want product 101 for ₹10"
       ↓
Order Service ❌ doesn't trust price
       ↓
Product Service
       ↓
Actual flash-sale price
```

Good design.

---

# 4. Inventory reservation

You create:

```java
InventoryReservationRequest.builder()
    .productId(product.getId())
    .quantity(request.getQuantity())
    .orderReference(orderReference)
    .build();
```

Then synchronously call:

```java
inventoryClient.reserveStock(reservationRequest);
```

This gives you:

```text
Order Service
      ↓
Inventory Service
      ↓
Redis Lua
      ↓
Reservation
```

For a flash sale, this is a reasonable choice because the order needs to know **immediately** whether stock was successfully reserved.

---

# 5. Compensation

This is one of the strongest parts of the design.

Suppose:

```text
Inventory reservation → SUCCESS
```

but:

```text
Order DB save → FAILURE
```

Then:

```text
Order DB ❌
Inventory  ✅ reserved
```

Your code attempts:

```java
compensateStockReservation(reservationRequest);
```

which calls:

```java
inventoryClient.releaseStock(...)
```

So:

```text
Reserve
  ↓
DB fails
  ↓
Release
```

That's the classic **Saga compensation pattern**.

### But there's an important limitation

If this happens:

```text
Inventory reserve ✅
Order save ❌
Inventory release ❌
```

you publish:

```text
ORDER_CANCELLED
```

That can provide another recovery path, but only if the Inventory consumer handles the event correctly and idempotently.

So this is a useful fallback, but it isn't a replacement for a durable Outbox.

---

# ⚠️ 6. Biggest architecture issue: Kafka + DB transaction

You have:

```java
@Transactional
public OrderResponse createOrder(...)
```

and later:

```java
orderRepository.save(order);
orderProducer.sendOrderCreatedEvent(event);
```

These are **not automatically one atomic transaction**.

You can get:

```text
PostgreSQL ✅
Kafka ❌
```

or:

```text
Kafka ✅
PostgreSQL transaction later rolls back ❌
```

For your project's intended **Transactional Outbox architecture**, this should eventually become:

```text
@Transactional
     │
     ├── Save Order
     │
     └── Save Outbox Event
              ↓
        Transaction commits
              ↓
       Outbox Publisher
              ↓
             Kafka
```

So `OrderProducer` shouldn't be the mechanism that guarantees reliable event publication directly from this transaction.

---

# 7. `cancelOrder()`

Current flow:

```text
PENDING_PAYMENT
       ↓
cancel()
       ↓
CANCELLED
       ↓
ORDER_CANCELLED
       ↓
Kafka
       ↓
Inventory Service
       ↓
Release stock
```

That's conceptually good.

### ⚠️ But again: duplicate cancellation

Kafka/client retries could potentially cause the cancellation event to be processed more than once.

Inventory must therefore make:

```text
ORDER_CANCELLED + orderReference
```

idempotent.

This connects directly to the issue we identified in your `InventoryEventListener`.

---

# 8. `markOrderAsPaid()`

Current flow:

```text
Payment
  ↓
markOrderAsPaid()
  ↓
Order = PAID
  ↓
InventoryService.settleStock()
```

### 🔴 This is a serious consistency problem

You do:

```java
order.markAsPaid();
orderRepository.save(order);
```

**before** successfully settling inventory.

Then:

```java
inventoryClient.settleStock(...)
```

If inventory settlement fails:

```text
Order = PAID ✅
Inventory settlement = FAILED ❌
```

And your code only logs the error:

```java
catch (Exception ex) {
    log.error(...)
}
```

So the method can return with:

```text
PAID
```

while inventory hasn't been settled.

That's dangerous.

### Better architecture

Payment success should produce a durable event:

```text
Payment completed
       ↓
Kafka: payment.completed
       ↓
Inventory Service
       ↓
settle inventory
```

And Order Service can independently transition the order based on the payment workflow.

At minimum, **don't silently swallow inventory settlement failures**.

---

# 9. `orderReference` should be preserved during payment

You already build:

```java
InventoryReservationRequest settleRequest
```

with:

```java
.orderReference(order.getOrderReference())
```

That's good.

But Inventory Service currently throws that information away when settlement happens.

So we have a cross-service improvement:

```text
OrderService
      ↓
orderReference
      ↓
InventoryService
      ↓
idempotency check
      ↓
settlement
```

This should be fixed together with the Inventory Service.

---

# 10. Payment deadline

This is straightforward:

```java
Instant paymentDeadline =
        now.plus(paymentTimeoutMinutes, ChronoUnit.MINUTES);
```

If:

```text
now = 10:00
timeout = 15 min
```

then:

```text
deadline = 10:15
```

Later:

```text
paymentDeadline < current time
```

can be used by `OrderExpirationScheduler`.

---

# 11. `getUserOrders()`

```java
return orderRepository.findByUserId(userId, pageable)
        .map(OrderResponse::fromEntity);
```

Good.

You are allowing Spring Data to handle pagination instead of loading every order into memory.

```text
100,000 orders
      ↓
❌ load everything

Pageable
      ↓
✅ only requested page
```

---

# 12. `compensateStockReservation()`

This is a good defensive pattern:

```text
Release inventory
      ↓
Success?
 ┌────┴────┐
YES        NO
 ↓          ↓
Done    ORDER_CANCELLED event
```

But again, the fallback event should ideally go through a **Transactional Outbox**, otherwise the fallback itself can be lost.

---

# Overall assessment

### ✅ Strong parts

* Idempotency concept
* Product validation through Product Service
* Server-side price calculation
* Synchronous inventory reservation
* Saga compensation
* Payment deadline
* Order state transitions
* Pagination
* Separate client/service/repository/producer responsibilities

### 🔴 Fix before production-ready

1. **Transactional Outbox for `ORDER_CREATED` / `ORDER_CANCELLED`**
2. **Database-level idempotency protection**
3. **Don't mark order `PAID` and silently ignore inventory settlement failure**
4. **Pass `orderReference` through inventory settlement**
5. **Make cancellation/payment event processing idempotent**
6. **Define failure/retry strategy between Order and Inventory**
7. **Handle duplicate-key race during order creation**

### Most important flow to aim for

```text
                CREATE ORDER
                     │
                     ↓
              Check Idempotency
                     │
                     ↓
               Product Service
                     │
                     ↓
              Reserve Inventory
                     │
                 SUCCESS
                     ↓
        ┌────────────────────────┐
        │ PostgreSQL Transaction │
        │                        │
        │ Save Order             │
        │ Save Outbox Event      │
        └───────────┬────────────┘
                    ↓
                 COMMIT
                    ↓
             Outbox Publisher
                    ↓
                  Kafka
```

So **don't rewrite this class completely**. The business flow is fundamentally good; we should refine the consistency mechanisms around it.

The next file to inspect should be **`OrderRepository.java`**, because we need to see how the idempotency and expired-order queries are currently implemented.
*/