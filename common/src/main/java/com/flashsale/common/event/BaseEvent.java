package com.flashsale.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseEvent {

    @lombok.Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;

    private String aggregateId;

    @lombok.Builder.Default
    private Instant occurredAt = Instant.now();

    @lombok.Builder.Default
    private int version = 1;

    public abstract String getPartitionKey();
}
/*\
* Haan, **`BaseEvent.java` tumhare project ke liye important hai**, especially because tumhara architecture **Kafka + Transactional Outbox + multiple microservices** use karega.

Simple Hinglish mein:

### `BaseEvent.java` ki need kyu hai?

Ye basically **har Kafka event ka common template/format** define karta hai.

Tumhare system mein events honge:

```text
ProductReservedEvent
ReservationExpiredEvent
OrderCreatedEvent
PaymentSuccessEvent
PaymentFailedEvent
```

Instead of har event mein separately ye fields banane ke:

```text
eventId
eventType
aggregateId
occurredAt
version
```

sab events `BaseEvent` ko extend karenge:

```text
BaseEvent
   │
   ├── ProductReservedEvent
   ├── ReservationExpiredEvent
   ├── OrderCreatedEvent
   ├── PaymentSuccessEvent
   └── PaymentFailedEvent
```

### Example

Suppose user ne product reserve kiya:

```text
Inventory Service
      ↓
ProductReservedEvent
      ↓
Kafka
      ↓
Order Service
Analytics Service
Notification Service
```

Event ke andar:

```text
eventId     = unique UUID
eventType   = PRODUCT_RESERVED
aggregateId = productId
occurredAt  = event creation time
version     = 1
```

---

### `eventId` kyu important hai?

Kafka systems mein duplicate event delivery ho sakti hai.

Example:

```text
OrderCreatedEvent
eventId = 123
       ↓
Kafka
       ↓
Order Service
```

Agar same event dobara aa gaya:

```text
eventId = 123
```

consumer check kar sakta hai:

```text
"123 already processed?"
        ↓
      YES
        ↓
Ignore duplicate
```

Isse **duplicate order/payment/analytics processing** se protection milti hai.

---

### `aggregateId` kyu?

Ye batata hai event **kis entity se related hai**.

Example:

```text
OrderCreatedEvent
aggregateId = ORDER-5001
```

ya:

```text
ProductReservedEvent
aggregateId = PRODUCT-101
```

Kafka partitioning mein bhi iska use ho sakta hai, taaki same entity ke events same partition mein jaayen.

---

### `occurredAt` kyu?

Event **kab actually create hua**, ye track karne ke liye.

Example:

```text
ORDER_CREATED
occurredAt = 2026-08-28T12:30:10Z
```

Distributed system mein debugging aur event processing ke liye useful hai.

---

### `version` kyu?

Future mein event ka structure change ho sakta hai.

Aaj:

```text
OrderCreatedEvent v1
```

Future:

```text
OrderCreatedEvent v2
```

Version se consumers ko pata chal sakta hai ki event ka schema kaunsa hai.

---

### `getPartitionKey()` kyu?

Ye tumhare Kafka architecture mein important hai.

Har event ko batana padega:

> **Kafka mein is event ko kis key ke basis par partition karna hai?**

For example:

```text
OrderCreatedEvent
→ orderId
```

and:

```text
ProductReservedEvent
→ productId
```

Same order/product ke events ko same partition mein route karne mein help milti hai, jisse us entity ke events ka ordering behavior maintain kiya ja sakta hai.

---

### Short mein

`BaseEvent.java` ko tum **Kafka events ka common parent/template** samajh sakte ho:

```text
              BaseEvent
                  │
       Common Event Information
                  │
    ┌─────────────┼─────────────┐
    ↓             ↓             ↓
 OrderCreated  ProductReserved  PaymentSuccess
```

Iski main need hai:

**Consistency + Event Tracking + Idempotency Support + Kafka Partitioning + Schema Versioning**

Tumhare **flash-sale project** mein ye particularly useful hai because multiple microservices ek hi business event ko consume karenge.

*
* */