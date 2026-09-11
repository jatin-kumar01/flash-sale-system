package com.flashsale.notification.repository;

import com.flashsale.notification.entity.NotificationLog;
import com.flashsale.notification.entity.NotificationLog.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByOrderReference(String orderReference);

    Page<NotificationLog> findByUserId(Long userId, Pageable pageable);

    List<NotificationLog> findByStatus(DeliveryStatus status);

    Page<NotificationLog> findByStatus(DeliveryStatus status, Pageable pageable);

    boolean existsByOrderReferenceAndEventType(String orderReference, String eventType);
}
/*This `NotificationLogRepository.java` is a **clean Spring Data JPA repository** and fits the notification-service structure well.

Its main job is simple:

> **Provide database operations for notification records.**

It should not contain notification-sending logic; that belongs in `NotificationService`.

---

## 1. Overall flow

Your notification service will eventually work roughly like:

```text
Kafka Event
    ↓
NotificationService
    ↓
NotificationLogRepository
    ↓
PostgreSQL
```

For example:

```text
payment.completed
       ↓
NotificationService
       ↓
Send email / notification
       ↓
Save NotificationLog
```

The repository provides the database operations required by the service.

---

## 2. Extending `JpaRepository`

```java
public interface NotificationLogRepository
        extends JpaRepository<NotificationLog, Long>
```

This automatically gives you common operations such as:

```java
save()
findById()
findAll()
delete()
deleteById()
count()
existsById()
```

So you don't need to manually write SQL for basic CRUD.

Example:

```java
notificationLogRepository.save(notificationLog);
```

---

# 3. `findByOrderReference()`

```java
List<NotificationLog> findByOrderReference(String orderReference);
```

Spring Data generates the query based on the method name.

Conceptually:

```sql
SELECT *
FROM notification_log
WHERE order_reference = ?;
```

Example:

```text
ORD-1001
   ↓
Notification logs
   ├── Order created
   ├── Payment successful
   ├── Invoice generated
   └── Order shipped
```

This is useful for auditing an order's notification history.

---

# 4. `findByUserId()`

```java
Page<NotificationLog> findByUserId(
        Long userId,
        Pageable pageable
);
```

This supports pagination.

For example:

```text
GET notifications for user 25
       ↓
Page 0 → 10 records
Page 1 → 10 records
Page 2 → 10 records
```

That's much better than loading the user's entire notification history at once.

---

# 5. `findByStatus()`

```java
List<NotificationLog> findByStatus(DeliveryStatus status);
```

Example:

```java
notificationLogRepository.findByStatus(
    DeliveryStatus.FAILED
);
```

Conceptually:

```sql
SELECT *
FROM notification_log
WHERE status = 'FAILED';
```

This can be used to find failed notifications.

For example:

```text
Notification
    ↓
FAILED
    ↓
Retry worker
    ↓
Try sending again
```

---

# 6. Paginated status query

```java
Page<NotificationLog> findByStatus(
        DeliveryStatus status,
        Pageable pageable
);
```

This is useful when there are potentially many failed notifications.

Instead of:

```text
10,000 FAILED records
       ↓
load all ❌
```

you can do:

```text
10,000 FAILED records
       ↓
Page 1 → 50
Page 2 → 50
...
```

This is better for a retry worker or admin dashboard.

---

# 7. Most important method: deduplication

```java
boolean existsByOrderReferenceAndEventType(
        String orderReference,
        String eventType
);
```

This is intended to prevent duplicate processing.

Imagine Kafka delivers the same event twice:

```text
payment.completed
      ↓
NotificationService
      ↓
First delivery
      ↓
Email sent
```

Then Kafka redelivers:

```text
payment.completed
      ↓
NotificationService
      ↓
Second delivery
```

Before sending again:

```text
orderReference = ORD-1001
eventType      = PAYMENT_COMPLETED
```

the service can check:

```java
existsByOrderReferenceAndEventType(
    "ORD-1001",
    "PAYMENT_COMPLETED"
);
```

If it returns:

```text
true
```

the service can skip duplicate processing.

This is a good idea for your event-driven architecture.

---

## ⚠️ Important limitation

The repository method **does not itself guarantee deduplication**.

Suppose two Kafka deliveries arrive simultaneously:

```text
Event A ──┐
          ├── exists? → false
Event B ──┘
          └── exists? → false
```

Both could then send the notification.

So for strong idempotency, you eventually want a **database-level uniqueness constraint** representing the notification event identity, for example:

```text
(orderReference, eventType)
```

if your business rule is:

> One notification of a particular event type per order.

Then the architecture becomes:

```text
Application check
       +
Database unique constraint
       +
Proper duplicate handling
```

This is particularly important because your system uses Kafka, where duplicate delivery must be expected.

---

# 8. One thing to verify in `NotificationLog`

This repository assumes `NotificationLog` contains fields equivalent to:

```text
orderReference
userId
status
eventType
```

and that:

```java
DeliveryStatus
```

exists inside `NotificationLog`.

Based on the repository alone, we cannot verify whether those fields are actually mapped correctly.

So the next file, `NotificationLog.java`, is important.

---

# 9. Indexing consideration

Because these queries are likely to run frequently:

```java
findByOrderReference()
findByUserId()
findByStatus()
existsByOrderReferenceAndEventType()
```

the `NotificationLog` entity/database should eventually have suitable indexes.

Especially:

```text
(orderReference, eventType)
```

for the deduplication check.

But **don't add indexes blindly in this repository**. The indexes belong to the entity/database schema, so we should inspect `NotificationLog.java` first.

---

# Verdict

### `NotificationLogRepository.java` → ✅ Good

```text
JpaRepository                    ✅
Order-based history              ✅
User pagination                  ✅
Failed notification lookup      ✅
Paginated retry lookup           ✅
Deduplication query              ✅
Clean separation of concerns     ✅
```

The only important caveat is:

> `existsByOrderReferenceAndEventType()` is an **application-level check**, not a complete concurrency-safe idempotency guarantee by itself.

So I would **keep this repository as it is for now**.

The next logical file is **`NotificationLog.java`**, because we need to verify the entity fields, `DeliveryStatus`, indexes, timestamps, and whether the database can actually enforce the notification deduplication you intend.
*/