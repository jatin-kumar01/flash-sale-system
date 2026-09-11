package com.flashsale.common.event.inventory;

import com.flashsale.common.event.BaseEvent;
import com.flashsale.common.event.EventTopics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReservedEvent extends BaseEvent {

    private String reservationId;
    private Long productId;
    private Long userId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Instant expiresAt;

    @Override
    public String getPartitionKey() {
        return productId != null ? String.valueOf(productId) : getAggregateId();
    }

    public static ProductReservedEvent of(String reservationId, Long productId, Long userId, 
                                          Integer quantity, BigDecimal unitPrice, Instant expiresAt) {
        return ProductReservedEvent.builder()
                .eventType(EventTopics.PRODUCT_RESERVED)
                .aggregateId(reservationId)
                .reservationId(reservationId)
                .productId(productId)
                .userId(userId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .expiresAt(expiresAt)
                .build();
    }
}

/*
* Yes, **`ProductReservedEvent.java` is needed** for your project.

In simple Hinglish:

### Is file ki need kyu hai?

Jab **Inventory Service successfully kisi product ka stock reserve karega**, tab doosri services ko batana hoga:

> "Product successfully reserved ho gaya hai."

Us information ko Kafka event ke form mein send kiya jayega.

Flow:

```text
User clicks Buy
      ↓
Inventory Service
      ↓
Stock successfully reserved
      ↓
ProductReservedEvent
      ↓
Transactional Outbox
      ↓
Kafka
      ↓
Order Service
Analytics Service
```

### Isme kya information ja rahi hai?

```java
reservationId
productId
userId
quantity
unitPrice
expiresAt
```

Example:

```text
reservationId = RES-1001
productId     = 101
userId        = 25
quantity      = 1
unitPrice     = ₹70,000
expiresAt     = 10 minutes later
```

Isse `order-service` ko pata chalega ki **kis user ke liye, kaunsa product, kitni quantity aur kis price par reserve hua hai**.

### `getPartitionKey()` kyu hai?

```java
return productId != null
        ? String.valueOf(productId)
        : getAggregateId();
```

Yahan `productId` Kafka key ke roop mein use ho raha hai.

Example:

```text
Product 101

Reservation 1 → productId 101
Reservation 2 → productId 101
Reservation 3 → productId 101
```

Same key use hone ki wajah se ye events same Kafka partition par ja sakte hain, helping preserve ordering for that product.

Flash-sale system mein ye useful hai because **same SKU par bahut high contention** hoga.

### `expiresAt` kyu important hai?

Reservation permanent nahi hai.

Example:

```text
Stock = 1

User reserves
   ↓
RESERVED
   ↓
expiresAt = 10 minutes
   ↓
Payment successful?
```

Agar payment nahi hua:

```text
Reservation expires
       ↓
Stock release
       ↓
Product available again
```

Isliye event mein `expiresAt` downstream processing ke liye important hai.

### `of()` method kyu hai?

Instead of manually:

```java
ProductReservedEvent event = ...
event.setEventType(...);
event.setAggregateId(...);
```

factory method:

```java
ProductReservedEvent.of(...)
```

automatically:

```text
eventType   → PRODUCT_RESERVED
aggregateId → reservationId
```

set kar deta hai.

### `BaseEvent` ke saath relationship

Pichli file:

```text
BaseEvent
   │
   └── ProductReservedEvent
```

`BaseEvent` common information deta hai:

```text
eventId
eventType
aggregateId
occurredAt
version
```

`ProductReservedEvent` specific information add karta hai:

```text
reservationId
productId
userId
quantity
unitPrice
expiresAt
```

So:

**`BaseEvent` = har event ka common structure**

**`ProductReservedEvent` = inventory reservation ka specific event**

### Tumhare project mein iska role

```text
Inventory Service
      ↓
ProductReservedEvent
      ↓
Outbox
      ↓
Kafka
      ↓
┌───────────────┬──────────────┐
↓               ↓              ↓
Order Service  Analytics    Notification
```

So yes, **ye file directly tumhare flash-sale architecture ke core event flow ka part hai**.
*/
