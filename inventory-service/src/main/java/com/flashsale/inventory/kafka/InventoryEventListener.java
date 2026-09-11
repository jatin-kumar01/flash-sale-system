package com.flashsale.inventory.kafka;

import com.flashsale.common.event.OrderEvent;
import com.flashsale.inventory.dto.InventoryReservationRequest;
import com.flashsale.inventory.service.InventoryService;
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
public class InventoryEventListener {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-cancelled:order.cancelled}",
            groupId = "${spring.kafka.consumer.group-id:inventory-group}"
    )
    public void handleOrderCancelled(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderCancelled event for orderRef: {}, product: {}, partition: {}, offset: {}",
                event.getOrderReference(), event.getProductId(), partition, offset);

        InventoryReservationRequest request = InventoryReservationRequest.builder()
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .orderReference(event.getOrderReference())
                .build();

        try {
            inventoryService.releaseStock(request);
            log.info("Successfully released stock compensation for cancelled order: {}", event.getOrderReference());
        } catch (Exception ex) {
            log.error("Failed to release stock for cancelled order: {}", event.getOrderReference(), ex);
            throw ex;
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-paid:order.paid}",
            groupId = "${spring.kafka.consumer.group-id:inventory-group}"
    )
    public void handleOrderPaid(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderPaid event for orderRef: {}, product: {}, partition: {}, offset: {}",
                event.getOrderReference(), event.getProductId(), partition, offset);

        try {
            inventoryService.settleOrderDeduction(event.getProductId(), event.getQuantity());
            log.info("Successfully settled and deducted inventory for paid order: {}", event.getOrderReference());
        } catch (Exception ex) {
            log.error("Failed to settle stock deduction for paid order: {}", event.getOrderReference(), ex);
            throw ex;
        }
    }
}

/*This `InventoryEventListener.java` is the **Kafka consumer side of the Inventory Service**. It receives order lifecycle events and tells `InventoryService` what inventory action to perform.

The structure in the uploaded file places it under `inventory/kafka/` alongside `InventoryProducer`.

## Overall flow

```text
                 Kafka
                   │
          ┌────────┴────────┐
          ↓                 ↓
   order.cancelled       order.paid
          │                 │
          ↓                 ↓
InventoryEventListener
          │
          ↓
   InventoryService
       │         │
       ↓         ↓
     Redis   PostgreSQL
```

### 1. `handleOrderCancelled()`

It listens to:

```text
order.cancelled
```

When an order is cancelled:

```text
Order cancelled
      ↓
Kafka
      ↓
InventoryEventListener
      ↓
releaseStock()
      ↓
locked → available
```

It converts the incoming `OrderEvent` into your existing:

```java
InventoryReservationRequest
```

containing:

```text
productId
quantity
orderReference
```

That's a good reuse of the existing DTO.

---

### 2. `handleOrderPaid()`

It listens to:

```text
order.paid
```

Flow:

```text
Payment successful
       ↓
order.paid
       ↓
InventoryEventListener
       ↓
settleOrderDeduction()
       ↓
locked stock → sold
```

### ⚠️ Important issue

You're currently calling:

```java
inventoryService.settleOrderDeduction(
        event.getProductId(),
        event.getQuantity()
);
```

Again, **`orderReference` is being discarded**.

This means your settlement operation still cannot perform proper idempotency based on the order.

I recommend eventually changing the service method to:

```java
inventoryService.settleOrderDeduction(
        event.getProductId(),
        event.getQuantity(),
        event.getOrderReference()
);
```

or preferably:

```java
inventoryService.settleOrderDeduction(event);
```

Then you can handle:

```text
Same order.paid event arrives twice
             ↓
       orderReference
             ↓
       Already settled?
        ↙          ↘
      YES           NO
       ↓             ↓
     Ignore       Settle
```

This is especially important with Kafka because **duplicate event delivery can happen**.

---

## 3. Kafka partition + offset

These:

```java
@Header(KafkaHeaders.RECEIVED_PARTITION) int partition
@Header(KafkaHeaders.OFFSET) long offset
```

give you information such as:

```text
partition = 3
offset    = 18492
```

Useful for debugging:

```text
"Why did this order fail?"
       ↓
orderReference
partition
offset
```

Good for production logging.

The `key` is also received:

```java
@Header(KafkaHeaders.RECEIVED_KEY) String key
```

but currently you only log partition and offset, **not the key**.

That's not a problem, but if you're deliberately using `productId` as the Kafka key, logging it could be useful.

---

# ⚠️ 4. The biggest missing piece: idempotency

Your class description says:

> Ensure idempotency and fault tolerance.

But **this implementation does not actually implement idempotency yet**.

For example:

```text
Kafka event:
order.cancelled ORD-1001
```

Consumer releases 2 items:

```text
locked: 10 → 8
available: 90 → 92
```

Suppose Kafka redelivers the same event:

```text
order.cancelled ORD-1001
```

It may release the same 2 items again:

```text
locked: 8 → 6
available: 92 → 94   ❌
```

So the same order could release stock twice.

The same problem exists for:

```text
order.paid
```

Therefore, **idempotency must be implemented in the service/database layer**, not just claimed in the listener.

---

# 5. Error handling is good

This is good:

```java
catch (Exception ex) {
    log.error(...);
    throw ex;
}
```

Why rethrow?

```text
Listener
   ↓
processing fails
   ↓
throw exception
   ↓
Kafka container error handler
   ↓
retry / DLT
```

If you simply swallowed the exception:

```java
catch (Exception ex) {
    log.error(...);
}
```

Kafka could consider the message successfully processed even though inventory wasn't updated.

So **rethrowing is appropriate**, assuming your Kafka error-handler configuration is set up for retries/DLT.

---

# Final verdict

### ✅ Good

* Correct Kafka listener placement
* Separate handlers for cancellation/payment
* Uses `InventoryService` rather than putting business logic in listener
* Uses `orderReference`
* Logs partition/offset
* Rethrows failures for Kafka error handling
* Good fit for Saga-style compensation

### 🔴 Must fix later

1. **Implement actual idempotency**
2. Pass `orderReference` into settlement
3. Configure Kafka retry/DLT explicitly
4. Ensure duplicate `order.cancelled` cannot release stock twice
5. Ensure duplicate `order.paid` cannot settle stock twice

So I would **keep the overall class structure**, but don't consider the current version fully production-ready yet.

The next file I'd inspect is **`InventoryResponse.java`**; after that we should review `Inventory.java`, because those three together determine exactly what your inventory API represents.
*/
