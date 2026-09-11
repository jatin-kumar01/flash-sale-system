package com.flashsale.notification.service;

import com.flashsale.common.event.OrderEvent;
import com.flashsale.common.event.PaymentEvent;
import com.flashsale.notification.entity.NotificationLog;
import com.flashsale.notification.entity.NotificationLog.ChannelType;
import com.flashsale.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final EmailService emailService;

    @Transactional
    public void processOrderCreatedNotification(OrderEvent event) {
        String eventType = "ORDER_CREATED";
        if (notificationLogRepository.existsByOrderReferenceAndEventType(event.getOrderReference(), eventType)) {
            log.info("Duplicate notification suppressed for order: {} and event: {}", event.getOrderReference(), eventType);
            return;
        }

        String recipient = resolveUserEmail(event.getUserId());
        String subject = "Flash Sale: Order Confirmation [" + event.getOrderReference() + "]";
        String content = buildOrderCreatedEmailHtml(event);

        dispatchEmailNotification(event.getOrderReference(), event.getUserId(), recipient, eventType, subject, content);
    }

    @Transactional
    public void processOrderCancelledNotification(OrderEvent event) {
        String eventType = "ORDER_CANCELLED";
        if (notificationLogRepository.existsByOrderReferenceAndEventType(event.getOrderReference(), eventType)) {
            log.info("Duplicate notification suppressed for order: {} and event: {}", event.getOrderReference(), eventType);
            return;
        }

        String recipient = resolveUserEmail(event.getUserId());
        String subject = "Flash Sale: Order Cancelled [" + event.getOrderReference() + "]";
        String content = buildOrderCancelledEmailHtml(event);

        dispatchEmailNotification(event.getOrderReference(), event.getUserId(), recipient, eventType, subject, content);
    }

    @Transactional
    public void processOrderExpiredNotification(OrderEvent event) {
        String eventType = "ORDER_EXPIRED";
        if (notificationLogRepository.existsByOrderReferenceAndEventType(event.getOrderReference(), eventType)) {
            log.info("Duplicate notification suppressed for order: {} and event: {}", event.getOrderReference(), eventType);
            return;
        }

        String recipient = resolveUserEmail(event.getUserId());
        String subject = "Flash Sale: Order Payment Expired [" + event.getOrderReference() + "]";
        String content = buildOrderExpiredEmailHtml(event);

        dispatchEmailNotification(event.getOrderReference(), event.getUserId(), recipient, eventType, subject, content);
    }

    @Transactional
    public void processPaymentCompletedNotification(PaymentEvent event) {
        String eventType = "PAYMENT_COMPLETED_" + event.getStatus();
        if (notificationLogRepository.existsByOrderReferenceAndEventType(event.getOrderReference(), eventType)) {
            log.info("Duplicate notification suppressed for order: {} and event: {}", event.getOrderReference(), eventType);
            return;
        }

        String recipient = resolveUserEmail(event.getUserId());
        boolean isSuccess = "SUCCESS".equalsIgnoreCase(event.getStatus());
        String subject = isSuccess
                ? "Payment Successful: Order [" + event.getOrderReference() + "]"
                : "Payment Failed: Order [" + event.getOrderReference() + "]";
        String content = buildPaymentEmailHtml(event);

        dispatchEmailNotification(event.getOrderReference(), event.getUserId(), recipient, eventType, subject, content);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsByOrderReference(String orderReference) {
        return notificationLogRepository.findByOrderReference(orderReference);
    }

    @Transactional(readOnly = true)
    public Page<NotificationLog> getUserNotifications(Long userId, Pageable pageable) {
        return notificationLogRepository.findByUserId(userId, pageable);
    }

    private void dispatchEmailNotification(String orderReference, Long userId, String recipient,
                                           String eventType, String subject, String htmlContent) {
        NotificationLog notificationLog = NotificationLog.builder()
                .orderReference(orderReference)
                .userId(userId)
                .recipient(recipient)
                .channel(ChannelType.EMAIL)
                .eventType(eventType)
                .subject(subject)
                .content(htmlContent)
                .build();

        NotificationLog savedLog = notificationLogRepository.save(notificationLog);

        boolean sent = emailService.sendHtmlEmail(recipient, subject, htmlContent);
        if (sent) {
            savedLog.markSent();
        } else {
            savedLog.markFailed("SMTP transmission failure or connection timeout");
        }

        notificationLogRepository.save(savedLog);
    }

    private String resolveUserEmail(Long userId) {
        // Fallback email resolution; can be enriched via Feign user-service client lookup
        return "user" + userId + "@flashsale-example.com";
    }

    private String buildOrderCreatedEmailHtml(OrderEvent event) {
        return "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                + "<h2>Order Reserved Successfully!</h2>"
                + "<p>Thank you for participating in our Flash Sale.</p>"
                + "<p><strong>Order Reference:</strong> " + event.getOrderReference() + "</p>"
                + "<p><strong>Product ID:</strong> " + event.getProductId() + "</p>"
                + "<p><strong>Quantity:</strong> " + event.getQuantity() + "</p>"
                + "<p><strong>Total Amount:</strong> $" + event.getTotalAmount() + "</p>"
                + "<p style='color: #d9534f;'>Please complete your payment before the deadline to secure your item.</p>"
                + "</div>";
    }

    private String buildOrderCancelledEmailHtml(OrderEvent event) {
        return "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                + "<h2>Order Cancelled</h2>"
                + "<p>Your order <strong>" + event.getOrderReference() + "</strong> has been cancelled.</p>"
                + "<p>Any reserved stock has been released back into the sale pool.</p>"
                + "</div>";
    }

    private String buildOrderExpiredEmailHtml(OrderEvent event) {
        return "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                + "<h2>Order Payment Window Expired</h2>"
                + "<p>Your order <strong>" + event.getOrderReference() + "</strong> was not paid within the allowed window.</p>"
                + "<p>The reservation has expired and inventory has been returned to stock.</p>"
                + "</div>";
    }

    private String buildPaymentEmailHtml(PaymentEvent event) {
        boolean isSuccess = "SUCCESS".equalsIgnoreCase(event.getStatus());
        String statusColor = isSuccess ? "#5cb85c" : "#d9534f";
        return "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                + "<h2 style='color: " + statusColor + ";'>Payment " + event.getStatus() + "</h2>"
                + "<p><strong>Order Reference:</strong> " + event.getOrderReference() + "</p>"
                + "<p><strong>Transaction ID:</strong> " + event.getTransactionId() + "</p>"
                + "<p><strong>Amount:</strong> $" + event.getAmount() + "</p>"
                + "<p><strong>Payment Method:</strong> " + event.getPaymentMethod() + "</p>"
                + "</div>";
    }
}
/*This `NotificationService.java` is the **main business-logic layer of your notification service**. The overall design is good, but there are **two important issues**: the current deduplication check is not concurrency-safe, and the `@Transactional` boundary includes the actual email sending.

## 1. Overall flow

Your notification architecture is:

```text
Kafka Event
     ↓
NotificationEventListener
     ↓
NotificationService
     ↓
┌──────────────────────────────┐
│ Check duplicate event        │
│ Resolve recipient             │
│ Build HTML                    │
│ Create PENDING log            │
│ Send email                    │
│ Mark SENT / FAILED            │
└──────────────┬───────────────┘
               ↓
        NotificationLog
               ↓
          PostgreSQL
```

This matches the responsibility described in your uploaded file.

---

# 2. `processOrderCreatedNotification()`

```java
@Transactional
public void processOrderCreatedNotification(OrderEvent event)
```

This handles an `ORDER_CREATED` event.

First:

```java
String eventType = "ORDER_CREATED";
```

Then:

```java
notificationLogRepository.existsByOrderReferenceAndEventType(
        event.getOrderReference(),
        eventType
)
```

checks whether this notification has already been processed.

Example:

```text
ORD-1001 + ORDER_CREATED
        ↓
Already exists?
   ↓           ↓
 YES           NO
 ↓              ↓
Skip          Send email
```

This is intended to protect against duplicate Kafka delivery, which is exactly the purpose described in the file.

---

# 3. Recipient resolution

```java
String recipient = resolveUserEmail(event.getUserId());
```

Currently:

```java
return "user" + userId + "@flashsale-example.com";
```

So:

```text
userId = 25
       ↓
user25@flashsale-example.com
```

This is clearly a **mock/fallback implementation**, which is fine during development.

The comment already indicates the future direction:

```text
Feign user-service client lookup
```

Eventually:

```text
NotificationService
       ↓
User Service
       ↓
Actual email address
```

---

# 4. Email template generation

For order creation:

```java
String content = buildOrderCreatedEmailHtml(event);
```

The generated email includes:

```text
Order Reference
Product ID
Quantity
Total Amount
Payment deadline reminder
```

This is useful for a flash-sale order confirmation.

---

# 5. `dispatchEmailNotification()`

All four notification methods eventually call:

```java
dispatchEmailNotification(...)
```

This is good because you don't repeat the same email-dispatch/database logic four times.

The common flow is:

```text
Event
 ↓
event-specific subject/template
 ↓
dispatchEmailNotification()
```

That's clean.

---

# 6. Creating the notification log

```java
NotificationLog notificationLog = NotificationLog.builder()
        .orderReference(orderReference)
        .userId(userId)
        .recipient(recipient)
        .channel(ChannelType.EMAIL)
        .eventType(eventType)
        .subject(subject)
        .content(htmlContent)
        .build();
```

This creates the audit record.

Conceptually:

```text
NotificationLog
-------------------------
orderReference = ORD-1001
userId         = 25
recipient      = user25@...
channel        = EMAIL
eventType      = ORDER_CREATED
subject        = Order Confirmation
content        = HTML...
status         = PENDING
```

Assuming your entity's default status is `PENDING`, this is the intended audit-first approach described in the source.

---

# 7. Why save before sending?

```java
NotificationLog savedLog =
        notificationLogRepository.save(notificationLog);
```

Then:

```java
boolean sent =
        emailService.sendHtmlEmail(...);
```

The idea is:

```text
PENDING
  ↓
attempt email
  ↓
SENT / FAILED
```

That's a good audit model.

For example:

```text
Notification #50

PENDING
   ↓
SMTP failure
   ↓
FAILED
```

You retain the failure information instead of losing the event.

---

# 8. Updating SENT / FAILED

```java
if (sent) {
    savedLog.markSent();
} else {
    savedLog.markFailed(
        "SMTP transmission failure or connection timeout"
    );
}
```

Then:

```java
notificationLogRepository.save(savedLog);
```

So the database eventually contains:

```text
SUCCESS:

PENDING → SENT
```

or:

```text
FAILURE:

PENDING → FAILED
```

This is a sensible notification state machine.

---

# 9. Payment notification

This part is particularly good:

```java
String eventType = "PAYMENT_COMPLETED_" + event.getStatus();
```

So:

```text
SUCCESS
   ↓
PAYMENT_COMPLETED_SUCCESS
```

and:

```text
FAILED
   ↓
PAYMENT_COMPLETED_FAILED
```

This means a successful and failed payment event are treated as different notification events.

Then:

```java
boolean isSuccess =
        "SUCCESS".equalsIgnoreCase(event.getStatus());
```

determines the email subject:

```text
Payment Successful: Order [ORD-1001]
```

or:

```text
Payment Failed: Order [ORD-1001]
```

Good.

---

# 10. Query methods

### Order notification history

```java
getNotificationsByOrderReference()
```

returns all notifications associated with an order.

Example:

```text
ORD-1001
 ├── ORDER_CREATED
 ├── PAYMENT_COMPLETED_SUCCESS
 └── ...
```

### User notifications

```java
getUserNotifications()
```

returns paginated notification records.

This is suitable for an eventual notification/inbox UI.

---

# 11. ⚠️ Important issue #1 — deduplication race condition

This is the biggest issue.

You currently do:

```java
if (existsByOrderReferenceAndEventType(...)) {
    return;
}
```

But imagine Kafka delivers the same event concurrently:

```text
Kafka
  │
  ├── Consumer A
  │
  └── Consumer B
```

Both execute:

```text
exists(...) → false
```

Then:

```text
A → send email
B → send email
```

Result:

```text
❌ Customer receives 2 emails
```

So this:

```java
existsByOrderReferenceAndEventType()
```

is useful, but **not sufficient for concurrency-safe idempotency**.

### Better protection

Your database should enforce uniqueness on something like:

```text
(orderReference, eventType)
```

Then:

```text
Consumer A ──┐
             ├── INSERT
Consumer B ──┘
```

Only one succeeds.

The other gets a duplicate/constraint violation and should skip processing.

This is especially important because Kafka consumers should be designed assuming **at-least-once delivery**.

---

# 12. ⚠️ Important issue #2 — transaction + SMTP

You have:

```java
@Transactional
public void processOrderCreatedNotification(...)
```

and inside that transaction you eventually call:

```java
emailService.sendHtmlEmail(...)
```

So the database transaction remains open while the SMTP operation happens.

Conceptually:

```text
BEGIN TRANSACTION
      ↓
INSERT notification
      ↓
SMTP request
      ↓
wait...
      ↓
UPDATE notification
      ↓
COMMIT
```

If SMTP takes 5 seconds, the DB transaction can remain open for those 5 seconds.

That's not ideal for a high-throughput system.

A better architecture is to separate:

```text
Event processing
      ↓
Persist notification task
      ↓
Commit
      ↓
Email worker
      ↓
SMTP
      ↓
Update SENT / FAILED
```

This also makes retries easier.

**For your current project stage, I wouldn't redesign it yet.** But this is something to improve before calling the notification system fully production-ready.

---

# 13. ⚠️ HTML construction

You're currently doing:

```java
"<p><strong>Order Reference:</strong> "
+ event.getOrderReference()
+ "</p>"
```

For trusted internal event data, that's fine.

But if event fields can contain untrusted user input, inserting them directly into HTML can create HTML/content injection.

Later, using proper templates and escaping values would be cleaner.

For a portfolio project, your current implementation is understandable.

---

# 14. One more architectural improvement

Currently you have:

```java
private String buildOrderCreatedEmailHtml(...)
private String buildOrderCancelledEmailHtml(...)
private String buildOrderExpiredEmailHtml(...)
private String buildPaymentEmailHtml(...)
```

That's perfectly fine initially.

But as notification types grow:

```text
ORDER_CREATED
ORDER_CANCELLED
ORDER_EXPIRED
PAYMENT_SUCCESS
PAYMENT_FAILED
INVOICE_READY
SHIPMENT_CREATED
...
```

this service can become huge.

Later you could move templates into:

```text
resources/templates/
```

and use a template engine.

But **don't do that yet** unless you actually need many templates.

---

# Final verdict

### `NotificationService.java` → ✅ Good foundation

```text
Business orchestration             ✅
Kafka event handling support       ✅
EmailService separation            ✅
Notification audit                 ✅
SENT / FAILED tracking             ✅
Order-based deduplication          ✅ conceptually
User notification history          ✅
HTML templates                     ✅
Pagination                         ✅
```

### Important improvements later

```text
⚠️ Database-level idempotency
⚠️ Don't hold DB transaction during SMTP
⚠️ Proper retry mechanism for FAILED emails
⚠️ Real user-email lookup
⚠️ Template escaping/template engine
```

The **most important correction** is this:

> `existsByOrderReferenceAndEventType()` should be treated as a preliminary check, not the final guarantee against duplicate Kafka notifications. A database uniqueness constraint + proper duplicate handling is needed for true concurrency-safe idempotency.

Your current notification flow is therefore good for the **first working version**, but not yet the final production-grade implementation. The next logical files are `NotificationEventListener.java` and `NotificationLog.java`; together they determine whether Kafka redelivery and database deduplication are actually handled safely.
*/