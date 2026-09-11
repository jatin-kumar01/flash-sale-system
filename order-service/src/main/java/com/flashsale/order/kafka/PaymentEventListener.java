package com.flashsale.order.kafka;

import com.flashsale.common.event.PaymentEvent;
import com.flashsale.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-completed:payment.completed}",
            groupId = "${spring.kafka.consumer.group-id:order-group}"
    )
    public void handlePaymentCompleted(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received PaymentCompleted event for orderReference: {}, transactionId: {}, partition: {}, offset: {}",
                event.getOrderReference(), event.getTransactionId(), partition, offset);

        if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {
            try {
                orderService.markOrderAsPaid(event.getOrderReference());
                log.info("Order marked as PAID and inventory settlement triggered for: {}", event.getOrderReference());
            } catch (Exception ex) {
                log.error("Failed to process payment completion for order: {}", event.getOrderReference(), ex);
                throw ex;
            }
        } else {
            log.warn("Payment event received with non-success status: {} for order: {}",
                    event.getStatus(), event.getOrderReference());
        }
    }
}
/*This `PaymentEventListener.java` is the **Kafka consumer in Order Service** that reacts when a payment is successfully completed.

### Overall flow

```text
Customer
   ↓
Order Service
   ↓
Payment Service
   ↓
payment.completed
   ↓
PaymentEventListener
   ↓
OrderService.markOrderAsPaid()
   ↓
Order → PAID
   ↓
Inventory settlement
```

## 1. `@KafkaListener`

```java
@KafkaListener(
    topics = "${app.kafka.topics.payment-completed:payment.completed}",
    groupId = "${spring.kafka.consumer.group-id:order-group}"
)
```

This tells Spring:

> Listen to the `payment.completed` Kafka topic as part of the `order-group`.

So whenever Payment Service publishes:

```json
{
  "orderReference": "ORD-1001",
  "transactionId": "TXN-5001",
  "status": "SUCCESS"
}
```

this method receives it.

---

## 2. Payment metadata

The listener receives:

```java
@Payload PaymentEvent event
```

and Kafka metadata:

```java
@Header(KafkaHeaders.RECEIVED_KEY) String key
@Header(KafkaHeaders.RECEIVED_PARTITION) int partition
@Header(KafkaHeaders.OFFSET) long offset
```

Example:

```text
orderReference = ORD-1001
transactionId  = TXN-5001
partition      = 2
offset         = 184
```

The partition and offset are mainly useful for debugging and tracing.

---

## 3. Only successful payments proceed

```java
if ("SUCCESS".equalsIgnoreCase(event.getStatus()))
```

So:

```text
SUCCESS → process order
FAILED  → don't mark PAID
PENDING → don't mark PAID
```

That's correct.

---

## 4. `markOrderAsPaid()`

The important call is:

```java
orderService.markOrderAsPaid(event.getOrderReference());
```

The listener itself does **not** modify the database.

Instead:

```text
PaymentEventListener
        ↓
OrderService
        ↓
Order entity
        ↓
PENDING_PAYMENT → PAID
```

That's good layering.

---

# ⚠️ Important: idempotency

Your description says:

> "Provide consumer idempotency"

But this listener itself **doesn't implement idempotency**.

Consider:

```text
payment.completed
ORD-1001
```

is received.

Order becomes:

```text
PENDING_PAYMENT → PAID
```

Now Kafka redelivers the same event:

```text
payment.completed
ORD-1001
```

The listener calls:

```java
orderService.markOrderAsPaid("ORD-1001");
```

again.

Your `Order.markAsPaid()` currently rejects anything that isn't `PENDING_PAYMENT`.

So the second delivery will throw an exception.

That isn't necessarily terrible—your error handler can retry/DLT it—but **it isn't proper idempotent processing**.

A better design is to make `OrderService` recognize that the order is already `PAID` and safely treat the duplicate event as already processed.

---

# ⚠️ Important: inventory settlement

Your comment says:

> `markOrderAsPaid` ... "trigger inventory settlement"

That behavior isn't visible in this listener.

The listener only calls:

```java
orderService.markOrderAsPaid(...)
```

So whether inventory settlement actually happens depends entirely on the implementation of `OrderService`.

The intended flow should eventually be:

```text
payment.completed
       ↓
PaymentEventListener
       ↓
OrderService
       ↓
Mark order PAID
       ↓
Publish/order settlement event
       ↓
Inventory Service
       ↓
locked → sold
```

This is cleaner than making the Kafka listener directly call Inventory Service.

---

# ⚠️ Transactional Outbox consideration

This is particularly important for your architecture.

Suppose:

```text
OrderService
    ↓
order.status = PAID ✅
    ↓
Kafka publishing ❌
```

Then Order Service knows the order is paid, but Inventory Service may never receive the settlement event.

Therefore `OrderService.markOrderAsPaid()` should eventually use your **Transactional Outbox** pattern:

```text
┌──────────────────────────────┐
│ PostgreSQL Transaction       │
│                              │
│ Order → PAID                 │
│ + outbox event               │
└──────────────┬───────────────┘
               ↓
        Outbox Publisher
               ↓
             Kafka
               ↓
       Inventory Service
```

This is one of the most important reliability pieces in your flash-sale architecture.

---

## Final verdict

### ✅ Keep

* `@KafkaListener`
* Separate payment listener
* SUCCESS status check
* `OrderService` delegation
* Partition/offset logging
* Exception rethrowing
* Kafka-based asynchronous flow

### 🔴 Need to implement later

1. **True idempotency** for duplicate `payment.completed`
2. Transactional Outbox when changing order to `PAID`
3. Ensure settlement event reaches Inventory reliably
4. Decide how duplicate payment events should behave
5. Don't make this listener directly responsible for inventory business logic

So the **listener structure is good**, but its production reliability depends heavily on the `OrderService` implementation.

The next logical file is **`OrderService.java`** because that's where we can verify whether `PENDING_PAYMENT → PAID`, inventory settlement, idempotency, and the Outbox flow are actually implemented correctly.
*/