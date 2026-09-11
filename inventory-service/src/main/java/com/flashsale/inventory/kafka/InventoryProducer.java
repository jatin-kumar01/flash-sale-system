package com.flashsale.inventory.kafka;

import com.flashsale.common.event.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.inventory-reserved:inventory.reserved}")
    private String inventoryReservedTopic;

    @Value("${app.kafka.topics.inventory-released:inventory.released}")
    private String inventoryReleasedTopic;

    @Value("${app.kafka.topics.stock-replenished:inventory.replenished}")
    private String stockReplenishedTopic;

    public void sendInventoryReservedEvent(InventoryEvent event) {
        String key = String.valueOf(event.getProductId());
        log.info("Publishing InventoryReserved event for orderReference: {}, productId: {}",
                event.getOrderReference(), event.getProductId());
        kafkaTemplate.send(inventoryReservedTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InventoryReserved event for order: {}",
                                event.getOrderReference(), ex);
                    } else {
                        log.debug("Successfully published InventoryReserved event to offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void sendInventoryReleasedEvent(InventoryEvent event) {
        String key = String.valueOf(event.getProductId());
        log.info("Publishing InventoryReleased event for orderReference: {}, productId: {}",
                event.getOrderReference(), event.getProductId());
        kafkaTemplate.send(inventoryReleasedTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InventoryReleased event for order: {}",
                                event.getOrderReference(), ex);
                    } else {
                        log.debug("Successfully published InventoryReleased event to offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void sendStockReplenishedEvent(InventoryEvent event) {
        String key = String.valueOf(event.getProductId());
        log.info("Publishing StockReplenished event for productId: {}, quantity: {}",
                event.getProductId(), event.getQuantity());
        kafkaTemplate.send(stockReplenishedTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish StockReplenished event for productId: {}",
                                event.getProductId(), ex);
                    } else {
                        log.debug("Successfully published StockReplenished event to offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

/*This `InventoryProducer.java` fits the Kafka layer, but there are **two important architecture corrections** before we continue.

### What this class does

```text id="8prz4p"
InventoryService
      ↓
InventoryProducer
      ↓
     Kafka
      ↓
 ┌───────────────┬──────────────┐
 │ Order Service │   Analytics  │
 └───────────────┴──────────────┘
```

It publishes three events:

| Method                         | Kafka event             |
| ------------------------------ | ----------------------- |
| `sendInventoryReservedEvent()` | `inventory.reserved`    |
| `sendInventoryReleasedEvent()` | `inventory.released`    |
| `sendStockReplenishedEvent()`  | `inventory.replenished` |

---

### 1. `KafkaTemplate`

```java
private final KafkaTemplate<String, Object> kafkaTemplate;
```

This is Spring Kafka's main object for sending messages.

For example:

```java
kafkaTemplate.send(topic, key, event);
```

means:

```text
Topic + Key + Event
       ↓
      Kafka
```

---

### 2. Why `productId` is used as the key

```java
String key = String.valueOf(event.getProductId());
```

Suppose:

```text
Product 101 → reserved
Product 101 → released
Product 101 → replenished
```

Using `101` as the Kafka key sends these records to the **same partition** for that topic, preserving their order within that partition.

However, the statement in your description:

> "guarantees that events for the same product are consumed in strict sequence across Kafka partitions"

is slightly incorrect.

It guarantees **partition affinity/order within a given topic**, not ordering *across different topics*. For example, `inventory.reserved` and `inventory.released` are separate topics, so Kafka does not provide global ordering between them.

---

### 3. `whenComplete()`

```java
.whenComplete((result, ex) -> {
```

This lets us know whether Kafka accepted the send.

Success:

```text
Application → Kafka
              ↓
           Success
```

Failure:

```text
Application → Kafka
              ↓
            Error
              ↓
          log.error()
```

The important point is that **logging a failure is not the same as reliably recovering the event**.

And this leads to the biggest issue.

---

## ⚠️ Important: Transactional Outbox

Your overall project architecture includes **Transactional Outbox**, but this producer currently does:

```text
Database transaction
      ↓
business operation
      ↓
kafkaTemplate.send()
```

That creates a potential failure:

```text
PostgreSQL
   ↓
Stock reserved successfully ✅

Kafka
   ↓
Publish fails ❌
```

Now the database says the stock was reserved, but downstream services never received the event.

For a production-ready flash-sale system, the intended flow should eventually be:

```text
InventoryService
      ↓
┌───────────────────────────┐
│ PostgreSQL Transaction    │
│                           │
│ Inventory update          │
│ + Outbox event            │
└─────────────┬─────────────┘
              ↓
       Outbox Publisher
              ↓
            Kafka
              ↓
     Order / Analytics
```

So **`InventoryProducer` should eventually be the Kafka publishing component used by an Outbox Publisher**, rather than being responsible for guaranteeing the business transaction itself.

---

## ⚠️ Another important issue: settlement

Your previous `InventoryRedisService` has:

```java
deductLockedStock()
```

but this producer has no:

```text
inventory.settled
```

event.

That's okay **if settlement is represented by an order/payment event flowing into Inventory Service**. We should decide that in `InventoryService`/event listener rather than adding an event blindly.

---

### Overall verdict

**Keep this file**, but understand its responsibility as:

```text
InventoryProducer
        ↓
"Send an already-created event to Kafka"
```

It should **not** be responsible for:

* database transactions
* idempotency
* inventory business rules
* guaranteeing event durability
* deciding whether reservation succeeds

Those belong to other layers.

### Next logical file

Before `InventoryService.java`, we should make sure **`InventoryEvent.java` in `common`** is correctly designed because this producer depends directly on it.

That event object is what travels through:

```text
InventoryService
      ↓
InventoryEvent
      ↓
InventoryProducer
      ↓
Kafka
```
*/
