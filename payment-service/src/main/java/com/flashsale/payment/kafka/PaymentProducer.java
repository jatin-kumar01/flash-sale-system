package com.flashsale.payment.kafka;

import com.flashsale.common.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.payment-completed:payment.completed}")
    private String paymentCompletedTopic;

    public void sendPaymentCompletedEvent(PaymentEvent event) {
        String key = event.getOrderReference();
        log.info("Publishing PaymentCompleted event for orderReference: {}, transactionId: {}, status: {}",
                key, event.getTransactionId(), event.getStatus());

        kafkaTemplate.send(paymentCompletedTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentCompleted event for orderReference: {}", key, ex);
                    } else {
                        log.debug("Successfully delivered PaymentCompleted event for orderReference: {} to partition: {} at offset: {}",
                                key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
/*This `PaymentProducer.java` is structurally good, but there is **one important architectural issue** you should understand before calling the payment service production-ready.

## 1. What this class does

`PaymentProducer` is responsible for sending a successful/failed payment event to Kafka.

Flow:

```text
PaymentService
      ↓
PaymentProducer
      ↓
Kafka
      ↓
payment.completed
      ↓
OrderService
      ↓
Order → PAID
      ↓
Inventory → SOLD
```

So this class is basically the **Kafka sender** for the payment service.

---

## 2. KafkaTemplate

```java
private final KafkaTemplate<String, Object> kafkaTemplate;
```

Spring provides `KafkaTemplate` for publishing messages.

Here:

```text
Key   → String
Value → Object
```

The key will be the `orderReference`, while the value will be your `PaymentEvent`.

Example:

```text
Key:
ORD-1001

Value:
PaymentEvent {
    orderReference = "ORD-1001",
    transactionId = "TXN-123",
    status = "SUCCESS"
}
```

---

## 3. Topic configuration

```java
@Value("${app.kafka.topics.payment-completed:payment.completed}")
private String paymentCompletedTopic;
```

This means Spring first looks for:

```yaml
app:
  kafka:
    topics:
      payment-completed: payment.completed
```

If it isn't configured, it uses:

```text
payment.completed
```

The `:payment.completed` part is the **default value**.

This is good because you don't hard-code the topic directly into the method.

---

## 4. `sendPaymentCompletedEvent()`

```java
public void sendPaymentCompletedEvent(PaymentEvent event)
```

The `PaymentService` will call something like:

```java
paymentProducer.sendPaymentCompletedEvent(event);
```

The producer extracts:

```java
String key = event.getOrderReference();
```

For example:

```text
orderReference = ORD-1001
```

So:

```text
Kafka Key = ORD-1001
```

---

## 5. Why use `orderReference` as the Kafka key?

This is an important part of your architecture.

Suppose:

```text
OrderReference = ORD-1001
```

and several events are produced:

```text
Order Created
Payment Completed
Order Cancelled
Payment Failed
```

Using the same key helps Kafka route those records to the **same partition within the relevant topic**, which preserves ordering for that key within that topic.

For example:

```text
payment.completed

Partition 0
    ORD-1001 → Payment SUCCESS
    ORD-1002 → Payment SUCCESS

Partition 1
    ORD-1003 → Payment SUCCESS
```

Kafka guarantees ordering **within a partition**, not across partitions.

### Important correction

Your description says:

> "preserve strict partition-level sequencing across the order lifecycle"

That's slightly too broad.

Using `orderReference` guarantees ordering for records with that key **within the same Kafka topic/partition**.

It does **not** guarantee a global ordering across different topics such as:

```text
order.created
payment.completed
order.cancelled
```

because those are separate topics.

---

## 6. `kafkaTemplate.send()`

```java
kafkaTemplate.send(paymentCompletedTopic, key, event)
```

This sends:

```text
Topic → payment.completed
Key   → ORD-1001
Value → PaymentEvent
```

The send is asynchronous.

That's why you get a future-like result and attach:

```java
.whenComplete(...)
```

---

## 7. Success callback

```java
.whenComplete((result, ex) -> {
```

If Kafka successfully accepts the message:

```java
if (ex == null)
```

you log:

```java
result.getRecordMetadata().partition()
result.getRecordMetadata().offset()
```

For example:

```text
Successfully delivered PaymentCompleted event
partition: 2
offset: 18492
```

This is useful for debugging and operational logging.

---

## 8. Failure callback

If Kafka publishing fails:

```java
if (ex != null)
```

you log:

```java
log.error(
    "Failed to publish PaymentCompleted event...",
    key,
    ex
);
```

This tells you that the producer encountered an error.

### But here's the important problem ⚠️

Logging the error **does not guarantee the event will eventually be delivered**.

Imagine:

```text
Payment DB
    ↓
SUCCESS
    ↓
PaymentProducer
    ↓
Kafka ❌ unavailable
```

Your database says:

```text
Payment = SUCCESS
```

but Kafka never receives:

```text
payment.completed
```

Then `order-service` may never mark the order as paid.

This is the classic **dual-write problem**.

---

# 9. This is where Transactional Outbox becomes important

Your overall project architecture includes Transactional Outbox.

For a production-grade design, you eventually want:

```text
PaymentService
      │
      ├───────────────┐
      ↓               ↓
Payment DB       Outbox DB row
      │               │
      └── SAME TRANSACTION
                      │
                      ↓
              Outbox Publisher
                      │
                      ↓
                    Kafka
```

So if the payment succeeds:

```text
Payment = SUCCESS
Outbox Event = payment.completed
```

are committed together.

Then a publisher can retry sending the outbox event to Kafka.

That is much safer than:

```text
Payment DB transaction
       ↓
Kafka send
```

directly.

### Therefore

**This `PaymentProducer` is fine for the current stage, but it should eventually publish events from an Outbox Publisher rather than being the component responsible for guaranteeing business-event durability.**

---

## 10. Another important point: "completed" topic

Your topic is:

```text
payment.completed
```

but your description says:

> "when a payment transaction succeeds or fails."

That naming is potentially confusing.

If the event can represent:

```text
SUCCESS
FAILED
```

then `payment.completed` can mean "payment processing finished", but you should define that clearly.

Alternatively, you could have:

```text
payment.completed
```

with:

```text
status = SUCCESS
```

and

```text
status = FAILED
```

or separate topics:

```text
payment.completed
payment.failed
```

For your current architecture, **one topic with a `status` field is perfectly workable** if consumers handle both states consistently.

---

# Overall verdict

### Code quality: ✅ Good

```text
KafkaTemplate              ✅
Configurable topic         ✅
orderReference as key      ✅
Async callback             ✅
Logging                    ✅
Clean responsibility       ✅
```

### Architecture:

```text
Current:
PaymentService
      ↓
PaymentProducer
      ↓
Kafka
```

Good for the current implementation.

But eventually:

```text
PaymentService
      ↓
Payment DB + Outbox
      ↓
Outbox Publisher
      ↓
PaymentProducer
      ↓
Kafka
```

is the stronger production architecture.

**Don't redesign this file yet.** The next file I would inspect is `PaymentService.java`, because that's where we can determine whether your payment flow actually implements **idempotency correctly** and whether the database transaction + Kafka event flow has any consistency problems.
*/