package com.flashsale.order.scheduler;

import com.flashsale.common.event.OrderEvent;
import com.flashsale.order.entity.Order;
import com.flashsale.order.entity.Order.OrderStatus;
import com.flashsale.order.kafka.OrderProducer;
import com.flashsale.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelayString = "${app.order.expiration-check-interval-ms:30000}")
    @Transactional
    public void sweepExpiredOrders() {
        Instant now = Instant.now();
        List<Order> expiredOrders = orderRepository.findExpiredOrders(
                OrderStatus.PENDING_PAYMENT,
                now,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} expired pending orders to sweep at timestamp: {}", expiredOrders.size(), now);

        for (Order order : expiredOrders) {
            try {
                order.expire();
                orderRepository.save(order);

                OrderEvent event = OrderEvent.builder()
                        .orderReference(order.getOrderReference())
                        .userId(order.getUserId())
                        .productId(order.getProductId())
                        .quantity(order.getQuantity())
                        .totalAmount(order.getTotalAmount())
                        .eventType("ORDER_EXPIRED")
                        .occurredAt(Instant.now())
                        .build();

                orderProducer.sendOrderExpiredEvent(event);
                log.info("Expired order: {} and dispatched OrderExpired event", order.getOrderReference());
            } catch (Exception ex) {
                log.error("Failed to expire order: {}", order.getOrderReference(), ex);
            }
        }
    }
}
/*This `OrderExpirationScheduler.java` is the **background cleanup/expiration worker** for your Order Service. Its main purpose is correct: find unpaid orders whose deadline has passed, mark them `EXPIRED`, and trigger inventory compensation.

## Overall flow

```text
Every 30 seconds
       ↓
Find PENDING_PAYMENT orders
       ↓
paymentDeadline < now
       ↓
Take max 100 orders
       ↓
order.expire()
       ↓
Save to PostgreSQL
       ↓
Publish ORDER_EXPIRED
       ↓
Kafka
       ↓
Inventory Service
       ↓
Release locked stock
```

The uploaded implementation uses a configurable scheduled interval, a batch size of 100, and `findExpiredOrders(...)` to identify expired pending orders.

### 1. Scheduled execution

```java
@Scheduled(
    fixedDelayString =
        "${app.order.expiration-check-interval-ms:30000}"
)
```

Default:

```text
30000 ms = 30 seconds
```

So approximately:

```text
10:00:00 → sweep
10:00:30 → sweep
10:01:00 → sweep
...
```

And you can change it in `application.yml`.

---

### 2. Why `PageRequest.of(0, 100)`?

```java
PageRequest.of(0, BATCH_SIZE)
```

with:

```java
BATCH_SIZE = 100;
```

means don't load potentially thousands/millions of expired orders at once.

Example:

```text
50,000 expired orders
       ↓
Instead of loading 50,000
       ↓
Load 100
       ↓
Process
```

This is a good idea for a high-throughput system.

### ⚠️ But there is a limitation

The scheduler processes **only the first 100 records per execution**.

If there are:

```text
5,000 expired orders
```

then one execution handles only 100.

The next execution handles another batch depending on how `findExpiredOrders()` is implemented and how the database ordering works.

For a flash-sale system, eventually I'd prefer a loop that keeps processing bounded batches until no more expired orders remain, while still limiting each query to 100.

---

# 3. `order.expire()`

This is good design:

```java
order.expire();
```

rather than:

```java
order.setStatus(OrderStatus.EXPIRED);
```

Why?

Because your `Order` entity contains the state-transition rule:

```text
PENDING_PAYMENT → EXPIRED
```

So the entity protects the business invariant.

---

# 4. Publishing the event

After expiration:

```java
OrderEvent event = OrderEvent.builder()
        .orderReference(order.getOrderReference())
        .userId(order.getUserId())
        .productId(order.getProductId())
        .quantity(order.getQuantity())
        .totalAmount(order.getTotalAmount())
        .eventType("ORDER_EXPIRED")
        .timestamp(Instant.now())
        .build();
```

Then:

```java
orderProducer.sendOrderExpiredEvent(event);
```

The resulting flow is:

```text
Order Service
     ↓
ORDER_EXPIRED
     ↓
Kafka
     ↓
Inventory Service
     ↓
releaseStock()
```

This is a good Saga compensation pattern.

---

# 🔴 Most important problem: Transactional Outbox

Your current method does:

```text
PostgreSQL
   ↓
order.status = EXPIRED
   ↓
save()
   ↓
Kafka send
```

Imagine:

```text
Order saved as EXPIRED ✅
Kafka publishing fails ❌
```

Now:

```text
Order DB → EXPIRED
Inventory → still LOCKED
```

The inventory might never receive the compensation event.

Since your overall architecture is intended to use **Transactional Outbox**, this scheduler should eventually do:

```text
┌──────────────────────────────┐
│ PostgreSQL Transaction       │
│                              │
│ Order → EXPIRED              │
│ Outbox → ORDER_EXPIRED       │
└──────────────┬───────────────┘
               ↓
       Outbox Publisher
               ↓
             Kafka
               ↓
       Inventory Service
```

This is much safer.

---

# 🔴 Another problem: catch + `@Transactional`

You have:

```java
@Transactional
```

around the whole scheduler.

But inside the loop:

```java
try {
    ...
} catch (Exception ex) {
    log.error(...);
}
```

Suppose:

```text
Order 1 → success
Order 2 → success
Order 3 → exception
Order 4 → success
```

Because you're catching the exception, the transaction behavior can become different from what you might expect.

For this type of batch processing, I'd eventually prefer **smaller transaction boundaries**, e.g. process each order/batch in its own transaction, especially when the number of expired orders can be large.

---

# 🔴 Kafka event should not be sent directly after `save()`

This line:

```java
orderRepository.save(order);
```

followed immediately by:

```java
orderProducer.sendOrderExpiredEvent(event);
```

creates the same DB/Kafka consistency gap we discussed in Inventory Service.

This is exactly where the **Transactional Outbox** architecture becomes valuable.

---

# 5. Potential duplicate expiration

Suppose two Order Service instances are running:

```text
Order Service A
Order Service B
       ↓
same expired order
```

Both schedulers could potentially find the same `PENDING_PAYMENT` order before one transaction's update becomes visible.

Then both may try:

```text
ORDER_EXPIRED
```

This is another reason the expiration operation needs **database-level concurrency/idempotency protection**.

For a microservice deployed with multiple instances, don't rely solely on the fact that the scheduler runs every 30 seconds.

---

# Final verdict

### ✅ Good parts

* Scheduled background worker
* Configurable interval
* Bounded batch size
* Uses entity state-transition method
* Publishes compensation event
* Correct Saga concept
* Avoids loading all expired orders into memory

### 🔴 Needs improvement before production

1. **Transactional Outbox**
2. **Idempotent expiration**
3. **Multi-instance scheduler protection**
4. Better transaction boundaries
5. Process multiple batches if a large backlog exists

The core idea is definitely correct:

```text
PENDING_PAYMENT
      ↓
deadline passed
      ↓
EXPIRED
      ↓
ORDER_EXPIRED
      ↓
Kafka
      ↓
Inventory compensation
```

So **keep this class conceptually**, but don't finalize it until `OrderRepository.java` is reviewed. The repository is where we can determine how `findExpiredOrders()` works and design the database-side concurrency protection properly.
*/