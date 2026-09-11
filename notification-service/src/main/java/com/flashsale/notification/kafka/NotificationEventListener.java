package com.flashsale.notification.kafka;

import com.flashsale.common.event.OrderEvent;
import com.flashsale.common.event.PaymentEvent;
import com.flashsale.notification.service.NotificationService;
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
public class NotificationEventListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-created:order.created}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}"
    )
    public void handleOrderCreated(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderCreated event for notification: key={}, partition={}, offset={}",
                key, partition, offset);
        try {
            notificationService.processOrderCreatedNotification(event);
        } catch (Exception ex) {
            log.error("Failed to process OrderCreated notification for order: {}", event.getOrderReference(), ex);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-cancelled:order.cancelled}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}"
    )
    public void handleOrderCancelled(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderCancelled event for notification: key={}, partition={}, offset={}",
                key, partition, offset);
        try {
            notificationService.processOrderCancelledNotification(event);
        } catch (Exception ex) {
            log.error("Failed to process OrderCancelled notification for order: {}", event.getOrderReference(), ex);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-expired:order.expired}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}"
    )
    public void handleOrderExpired(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderExpired event for notification: key={}, partition={}, offset={}",
                key, partition, offset);
        try {
            notificationService.processOrderExpiredNotification(event);
        } catch (Exception ex) {
            log.error("Failed to process OrderExpired notification for order: {}", event.getOrderReference(), ex);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.payment-completed:payment.completed}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}"
    )
    public void handlePaymentCompleted(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received PaymentCompleted event for notification: key={}, transactionId={}, partition={}, offset={}",
                key, event.getTransactionId(), partition, offset);
        try {
            notificationService.processPaymentCompletedNotification(event);
        } catch (Exception ex) {
            log.error("Failed to process PaymentCompleted notification for order: {}", event.getOrderReference(), ex);
        }
    }
}
/*This `NotificationEventListener.java` is the **Kafka entry point of your notification service**. The basic structure is correct, but there is **one major problem in the error handling** that you should fix before considering Kafka retry behavior reliable.

## 1. Overall role

The flow is:

```text
Kafka
  │
  ├── order.created ──────┐
  ├── order.cancelled ────┤
  ├── order.expired ──────┤
  └── payment.completed ──┤
                           ↓
                 NotificationEventListener
                           ↓
                  NotificationService
                           ↓
                     EmailService
                           ↓
                         SMTP
```

Your file is responsible for **receiving events and delegating them**. The actual notification business logic remains in `NotificationService`, which is the correct separation.

---

# 2. `@KafkaListener`

For example:

```java
@KafkaListener(
        topics = "${app.kafka.topics.order-created:order.created}",
        groupId = "${spring.kafka.consumer.group-id:notification-group}"
)
```

This tells Spring:

> Listen to the `order.created` Kafka topic using the `notification-group` consumer group.

The other listeners do the same for:

```text
order.created
order.cancelled
order.expired
payment.completed
```

This matches the event responsibilities defined in your file.

---

# 3. Why `notification-group` is important

You use:

```text id="3rh2t4"
notification-group
```

This means the notification service has its own Kafka consumption state.

Conceptually:

```text
payment.completed
       │
       ├── order-group
       │      ↓
       │   OrderService
       │
       ├── inventory-group
       │      ↓
       │   InventoryService
       │
       └── notification-group
              ↓
         NotificationService
```

Each consumer group independently tracks its offsets.

So NotificationService consuming an event does not prevent OrderService from consuming the same event.

That's exactly what you want in an event-driven microservice architecture.

---

# 4. Payload conversion

For order events:

```java
@Payload OrderEvent event
```

Spring Kafka converts the Kafka message value into your shared:

```text
OrderEvent
```

For payment:

```java
@Payload PaymentEvent event
```

So:

```text id="4a7p6n"
Kafka JSON
   ↓
Spring Kafka deserialization
   ↓
OrderEvent / PaymentEvent
```

This is why your `common` module is useful: both producer and consumer can share the same event contract.

---

# 5. Kafka metadata

You capture:

```java
@Header(KafkaHeaders.RECEIVED_KEY) String key
@Header(KafkaHeaders.RECEIVED_PARTITION) int partition
@Header(KafkaHeaders.OFFSET) long offset
```

For example:

```text id="l3bx0c"
key       = ORD-1001
partition = 2
offset    = 18492
```

This is useful when debugging Kafka behavior.

You can say:

> The notification event for `ORD-1001` came from partition 2, offset 18492.

Good operational logging.

---

# 6. Delegation to `NotificationService`

For order creation:

```java
notificationService.processOrderCreatedNotification(event);
```

For cancellation:

```java
notificationService.processOrderCancelledNotification(event);
```

For expiration:

```java
notificationService.processOrderExpiredNotification(event);
```

For payment:

```java
notificationService.processPaymentCompletedNotification(event);
```

This is exactly what you want.

The listener should **not** build HTML, send emails, or manipulate notification records itself.

So:

```text id="6qf0do"
Listener
   ↓
Receive + route
   ↓
NotificationService
   ↓
Business logic
```

Clean separation. ✅

---

# 7. ⚠️ Major issue: your exception handling

Currently you have:

```java
try {
    notificationService.processOrderCreatedNotification(event);
} catch (Exception ex) {
    log.error(...);
}
```

The problem is that you're **catching the exception and not rethrowing it**.

Suppose SMTP fails:

```text id="u8b6ny"
Kafka event
    ↓
NotificationService
    ↓
EmailService
    ↓
SMTP ❌
```

`NotificationService` may fail.

But your listener does:

```text id="e6ob5f"
catch exception
     ↓
log error
     ↓
return normally
```

From Kafka's perspective, the listener method completed successfully.

Depending on your Kafka acknowledgment/error-handler configuration, the message may therefore be considered successfully processed rather than being sent through your configured retry/DLT mechanism.

So this explanation in your file is misleading:

> "Fault-Tolerant Processing ... prevent a failing email provider ... from terminating the Kafka listener container."

Catching exceptions does prevent the exception from escaping, **but it can also prevent Kafka's retry/error handling from seeing the failure**.

---

# 8. Better approach

For a retryable failure, you generally want:

```java
catch (Exception ex) {
    log.error(
        "Failed to process OrderCreated notification for order: {}",
        event.getOrderReference(),
        ex
    );

    throw ex;
}
```

Then the flow becomes:

```text id="2tq1v0"
Kafka
 ↓
Listener
 ↓
NotificationService
 ↓
Email fails
 ↓
Exception
 ↓
Listener rethrows
 ↓
Kafka error handler
 ↓
Retry
 ↓
Eventually DLT
```

This is much more appropriate for your architecture.

### However

The exact retry behavior depends on your Spring Kafka error-handler configuration in `application.yml`/Kafka configuration.

So don't assume retries/DLT exist merely because you rethrow. You need to configure them.

---

# 9. Another important point: retrying emails

Suppose:

```text id="f1bj1a"
payment.completed
      ↓
Email attempt #1
      ↓
SMTP timeout
      ↓
retry
      ↓
Email attempt #2
```

That's good **if the failure is transient**.

But if your service has already successfully sent the email and then crashes before recording `SENT`:

```text id="gk4x98"
Send email → SUCCESS
       ↓
Application crashes ❌
       ↓
NotificationLog still PENDING
       ↓
Kafka redelivery
       ↓
Send email again
```

You can still get duplicate emails.

That's why your notification idempotency design needs to be stronger than just:

```java
existsByOrderReferenceAndEventType()
```

This connects directly to the issue we found in `NotificationService`.

---

# 10. The four listeners

### `order.created`

```text id="cr9zj1"
order.created
     ↓
Order confirmation email
```

### `order.cancelled`

```text id="g3r8ax"
order.cancelled
     ↓
Cancellation email
```

### `order.expired`

```text id="u3g4qy"
order.expired
     ↓
Payment deadline expired email
```

### `payment.completed`

```text id="2uq4yk"
payment.completed
     ↓
SUCCESS → Payment successful email
FAILED  → Payment failed email
```

This is a good mapping.

---

# 11. One architectural improvement later

You currently have four separate `@KafkaListener` methods in one class.

That's perfectly fine at this scale.

As the project grows, you might eventually have:

```text
invoice.generated
shipment.created
refund.completed
review.requested
...
```

At that point, the class can become large.

But **don't split it now**. Four listeners are easy to understand and maintain.

---

# Verdict

### `NotificationEventListener.java` → ✅ Good structure, ⚠️ fix error handling

| Area                                 | Status                                       |
| ------------------------------------ | -------------------------------------------- |
| Separate Kafka consumer              | ✅                                            |
| Dedicated `notification-group`       | ✅                                            |
| Four required topics                 | ✅                                            |
| Shared `OrderEvent` / `PaymentEvent` | ✅                                            |
| Kafka metadata logging               | ✅                                            |
| Delegates to service                 | ✅                                            |
| Business logic in listener           | ❌ None — good                                |
| Retry behavior                       | ⚠️ Needs proper exception propagation/config |
| DLT                                  | ⚠️ Not shown/configured                      |
| Duplicate notification protection    | ⚠️ Service/database must handle it           |

### The one change I'd make now

Instead of:

```java
catch (Exception ex) {
    log.error(...);
}
```

use:

```java
catch (Exception ex) {
    log.error(
        "Failed to process notification for order: {}",
        event.getOrderReference(),
        ex
    );
    throw ex;
}
```

**provided your Kafka error handler is configured to perform retries/DLT.**

That gives you the correct responsibility chain:

```text
Kafka
 ↓
Listener
 ↓
NotificationService
 ↓
EmailService
 ↓
Success → acknowledge
Failure → throw → retry/DLT
```

So the next file I would inspect is **`NotificationLog.java`**. That file is particularly important now because we need to make the `existsByOrderReferenceAndEventType()` deduplication strategy actually safe under concurrent Kafka redelivery.
*/