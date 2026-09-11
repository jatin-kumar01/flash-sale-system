package com.flashsale.order.kafka;

import com.flashsale.common.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-created:order.created}")
    private String orderCreatedTopic;

    @Value("${app.kafka.topics.order-cancelled:order.cancelled}")
    private String orderCancelledTopic;

    @Value("${app.kafka.topics.order-expired:order.expired}")
    private String orderExpiredTopic;

    public void sendOrderCreatedEvent(OrderEvent event) {
        String key = event.getOrderReference();
        log.info("Publishing OrderCreated event for orderReference: {}, productId: {}",
                key, event.getProductId());
        kafkaTemplate.send(orderCreatedTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCreated event for order: {}", key, ex);
                    } else {
                        log.debug("Successfully published OrderCreated event to offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void sendOrderCancelledEvent(OrderEvent event) {
        String key = event.getOrderReference();
        log.info("Publishing OrderCancelled event for orderReference: {}, productId: {}",
                key, event.getProductId());
        kafkaTemplate.send(orderCancelledTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCancelled event for order: {}", key, ex);
                    } else {
                        log.debug("Successfully published OrderCancelled event to offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void sendOrderExpiredEvent(OrderEvent event) {
        String key = event.getOrderReference();
        log.info("Publishing OrderExpired event for orderReference: {}, productId: {}",
                key, event.getProductId());
        kafkaTemplate.send(orderExpiredTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderExpired event for order: {}", key, ex);
                    } else {
                        log.debug("Successfully published OrderExpired event to offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
/*This `OrderProducer.java` is the **Kafka producer for the Order Service**. Its structure is good and it fits the event-driven architecture.

## Overall flow

```text id="y9k2zp"
OrderService
     │
     ↓
OrderProducer
     │
     ↓
   Kafka
     │
 ┌───┼───────────────┐
 ↓   ↓               ↓
Inventory       Payment       Analytics
```

It publishes three order lifecycle events:

```text
order.created
order.cancelled
order.expired
```

---

## 1. `sendOrderCreatedEvent()`

```java
public void sendOrderCreatedEvent(OrderEvent event)
```

When a new order is created:

```text id="0k4p7q"
Order created
     ↓
OrderService
     ↓
OrderProducer
     ↓
order.created
```

The key is:

```java
String key = event.getOrderReference();
```

For example:

```text
ORD-10001
```

Kafka uses that key to select a partition.

---

## 2. Why `orderReference` is a good key

Suppose:

```text id="7qj9zt"
ORD-10001 → CREATED
ORD-10001 → CANCELLED
ORD-10001 → EXPIRED
```

Because the same key is used, records for that order are routed to the same partition **within the same Kafka topic**.

But there is an important clarification:

> Kafka does **not** guarantee ordering across different topics.

So:

```text
order.created
order.cancelled
order.expired
```

being separate topics does **not** give you one global ordering between them.

Within a topic/partition, Kafka preserves record order.

---

## 3. `sendOrderCancelledEvent()`

Used when:

```text id="2p1t7n"
User cancels
     OR
Downstream failure
     ↓
Order → CANCELLED
     ↓
order.cancelled
     ↓
Inventory Service
     ↓
Release stock
```

This connects directly to your `InventoryEventListener` from the previous part.

---

## 4. `sendOrderExpiredEvent()`

Used when payment isn't completed before:

```text
paymentDeadline
```

Flow:

```text id="yqj6bc"
PENDING_PAYMENT
       ↓
Deadline reached
       ↓
EXPIRED
       ↓
order.expired
       ↓
Inventory
       ↓
Release reservation
```

### ⚠️ Important

Your current `InventoryEventListener` listens to:

```text
order.cancelled
order.paid
```

but **not `order.expired`**.

Yet this producer explicitly creates:

```text
order.expired
```

So currently:

```text
OrderExpirationScheduler
        ↓
order.expired
        ↓
Kafka
        ↓
Inventory Service
        ↓
❌ No listener
```

That means expired reservations won't automatically be released through the current listener.

You should eventually add an `order.expired` listener, or intentionally consolidate cancellation and expiration into a common inventory-release event.

---

# 5. `whenComplete()`

Each method uses:

```java
kafkaTemplate.send(...)
    .whenComplete((result, ex) -> {
```

This gives asynchronous success/failure feedback.

Success:

```text id="72x4fh"
OrderService
    ↓
Kafka
    ↓
Success callback
```

Failure:

```text id="6n0n4x"
OrderService
    ↓
Kafka
    ↓
Failure
    ↓
log.error()
```

This avoids waiting synchronously for the Kafka send result.

---

# ⚠️ Biggest architecture issue: Transactional Outbox

The producer itself is fine as a **Kafka publishing component**, but it doesn't solve reliable event delivery.

Imagine:

```text id="m0j9k2"
Order DB transaction
       ↓
Order created ✅
       ↓
Kafka send ❌
```

Now PostgreSQL knows the order exists, but Kafka doesn't know about it.

Your project's intended architecture includes **Transactional Outbox**, so eventually you want:

```text id="z1m4qp"
OrderService
     ↓
┌───────────────────────────┐
│ PostgreSQL Transaction    │
│                           │
│ Order                     │
│ + Outbox Event            │
└──────────────┬────────────┘
               ↓
        Outbox Publisher
               ↓
             Kafka
```

Then `OrderProducer` can be the component that actually sends the durable outbox event to Kafka.

So don't interpret:

```java
.whenComplete(...)
```

as guaranteed delivery. It only handles the result of that particular Kafka send attempt.

---

# ⚠️ Idempotency

`orderReference` is excellent for identifying an order, but this producer itself doesn't implement idempotency.

For example, if:

```text
ORD-10001
```

gets published twice:

```text
order.cancelled
ORD-10001

order.cancelled
ORD-10001
```

Inventory must safely handle the duplicate.

That protection belongs primarily in the **consumer/business layer**, using `orderReference` and a persistent processed-event/idempotency record.

---

## Final verdict

### ✅ Keep

* `KafkaTemplate`
* Three separate producer methods
* Configurable topics
* `orderReference` as Kafka key
* Asynchronous send callback
* Logging

### 🔴 Fix later

1. Add handling for `order.expired` in Inventory.
2. Implement Transactional Outbox.
3. Add consumer-side idempotency.
4. Don't claim ordering across separate Kafka topics.
5. Eventually consider a common event-publication mechanism instead of repeating nearly identical producer methods.

Your current Order Service flow should therefore become:

```text id="c1a2zy"
                    OrderService
                         │
              ┌──────────┼──────────┐
              ↓          ↓          ↓
           CREATED    CANCELLED   EXPIRED
              │          │          │
              └──────────┼──────────┘
                         ↓
                   Outbox Event
                         ↓
                        Kafka
                         ↓
                  Other Services
```

**Next logical file: `OrderRepository.java`**, because that's where we can implement the database-side queries needed for order lookup, expiration, and eventually concurrency/idempotency.
*/