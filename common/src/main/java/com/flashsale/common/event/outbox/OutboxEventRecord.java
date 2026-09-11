package com.flashsale.common.event.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventRecord {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private String topic;

    private String partitionKey;

    private String payload;

    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant processedAt;

    public enum OutboxStatus {
        PENDING,
        PROCESSING,
        PUBLISHED,
        FAILED
    }

    public static OutboxEventRecord create(String aggregateType, String aggregateId, 
                                           String eventType, String topic, 
                                           String partitionKey, String payloadJson) {
        return OutboxEventRecord.builder()
                .id(UUID.randomUUID().toString())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .partitionKey(partitionKey)
                .payload(payloadJson)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
    }
}

/*
* Yes, **`OutboxEventRecord.java` is an important file** for your flash-sale architecture because it implements the core idea behind the **Transactional Outbox Pattern**.

### Simple Hinglish

Main problem samjho:

Suppose `inventory-service` mein:

```text
Stock update
   ↓
Database
   ↓
Success ✅
```

Uske immediately baad Kafka ko event bhejna hai:

```text
Kafka publish
   ↓
FAILED ❌
```

Ab database mein stock update ho chuka hai, but Kafka event nahi gaya.

Result:

```text
Database = Updated ✅
Kafka     = Event missing ❌
```

Ye **dual-write problem** hai.

---

## `OutboxEventRecord` kya karta hai?

Instead of directly:

```text
Database → Kafka
```

pehle event ko database ke **outbox** mein save karte hain:

```text
Business Data
     +
Outbox Event
     ↓
Same Database Transaction
     ↓
Commit ✅
```

Then background publisher:

```text
Outbox Table
     ↓
Publisher
     ↓
Kafka
```

### Example

User ne product reserve kiya:

```text
Inventory Service
      ↓
Reserve stock
      +
Create ProductReservedEvent
      ↓
Same PostgreSQL transaction
      ↓
COMMIT
```

Outbox mein record:

```text
eventType    = PRODUCT_RESERVED
topic        = flashsale.inventory.events
partitionKey = productId
payload      = {...}
status       = PENDING
```

Then background worker:

```text
PENDING
   ↓
PROCESSING
   ↓
Kafka
   ↓
PUBLISHED
```

---

## `OutboxEventRecord` ke fields

### `id`

```java
private String id;
```

Har outbox event ka unique ID.

---

### `aggregateType`

Example:

```text
INVENTORY
ORDER
PAYMENT
```

Batata hai event kis domain/entity type se related hai.

---

### `aggregateId`

Example:

```text
RES-1001
ORD-5001
PAY-1001
```

Specific entity ko identify karta hai.

---

### `eventType`

Example:

```text
PRODUCT_RESERVED
ORDER_CREATED
PAYMENT_SUCCESS
PAYMENT_FAILED
```

---

### `topic`

Kafka mein event kahan jaana hai:

```text
flashsale.inventory.events
```

---

### `partitionKey`

Example:

```text
productId = 101
```

Kafka partitioning ke liye use hota hai.

---

### `payload`

Actual event ka serialized JSON:

```json
{
  "eventId": "abc-123",
  "eventType": "PRODUCT_RESERVED",
  "productId": 101,
  "quantity": 1
}
```

---

### `status`

Iska lifecycle:

```text
PENDING
   ↓
PROCESSING
   ↓
PUBLISHED
```

Agar problem:

```text
PROCESSING
   ↓
FAILED
```

---

### `retryCount`

Kafka publishing fail hone par kitni baar retry hua:

```text
retryCount = 0
     ↓
failure
     ↓
retryCount = 1
```

---

### `createdAt`

Outbox record kab create hua.

### `processedAt`

Successfully publish/process hone ka time.

---

# Flash-sale project mein iska importance

Tumhara important flow:

```text
User
 ↓
Inventory Service
 ↓
Reserve Stock
 ↓
PostgreSQL Transaction
 ├── Update Inventory
 └── Insert Outbox Event
 ↓
COMMIT
 ↓
Outbox Publisher
 ↓
Kafka
 ↓
Order Service / Analytics / etc.
```

Ye **high-throughput system design interview** mein bahut valuable concept hai.

### Why?

Because interviewer agar pooche:

> "What happens if your database transaction succeeds but Kafka publish fails?"

Tum explain kar sakte ho:

> **We use the Transactional Outbox Pattern. The business state change and event record are persisted in the same database transaction. A separate publisher reliably reads pending outbox records and publishes them to Kafka, with retries and status tracking.**

That's a strong system-design answer.

---

### One important distinction

`OutboxEventRecord.java` **khud Kafka publish nahi karta**.

Ye sirf **event ko persist karne ke liye data model/contract** hai.

Actual system later mein kuch aisa hoga:

```text
OutboxEventRecord
       ↓
Outbox Repository
       ↓
Outbox Publisher/Scheduler
       ↓
Kafka Producer
       ↓
Kafka
```

So:

**`OutboxEventRecord` = Outbox mein kya store karna hai**

**Outbox Publisher = Outbox se Kafka ko event kaise bhejna hai**

Dono alag responsibilities hain.
*/
