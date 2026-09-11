package com.flashsale.common.event.inventory;

import com.flashsale.common.event.BaseEvent;
import com.flashsale.common.event.EventTopics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationExpiredEvent extends BaseEvent {

    private String reservationId;
    private Long productId;
    private Long userId;
    private Integer releasedQuantity;
    private Instant expiredAt;

    @Override
    public String getPartitionKey() {
        return productId != null ? String.valueOf(productId) : getAggregateId();
    }

    public static ReservationExpiredEvent of(String reservationId, Long productId, Long userId,
                                             Integer releasedQuantity, Instant expiredAt) {
        return ReservationExpiredEvent.builder()
                .eventType(EventTopics.RESERVATION_EXPIRED)
                .aggregateId(reservationId)
                .reservationId(reservationId)
                .productId(productId)
                .userId(userId)
                .releasedQuantity(releasedQuantity)
                .expiredAt(expiredAt)
                .build();
    }
}

/*
 *
 Yes, **`ReservationExpiredEvent.java` is needed** for your flash-sale system.

Simple Hinglish:

### Is file ki need kyu hai?

Jab kisi user ne product reserve kiya, lekin given time ke andar payment complete nahi ki, to reservation **expire** karni hogi.

Example:

```text
Stock = 100
       ↓
User reserves 1
       ↓
Available = 99
Reserved = 1
       ↓
Payment nahi hua
       ↓
Reservation expires
       ↓
ReservationExpiredEvent
       ↓
Kafka
```

Ye event doosri services ko inform karta hai:

> **"Ye reservation expire ho gayi hai aur itni quantity inventory mein release ho gayi."**

### Isme kya information hai?

```text
reservationId
productId
userId
releasedQuantity
expiredAt
```

Example:

```text
reservationId   = RES-1001
productId       = 101
userId          = 25
releasedQuantity = 1
expiredAt       = 18:30 UTC
```

### `releasedQuantity` kyu important hai?

Suppose:

```text
Available stock = 0
Reserved stock  = 5
```

Ek reservation expire hui:

```text
releasedQuantity = 2
```

Inventory ko pata chalega:

```text
Available = 2
Reserved = 3
```

Yani exact quantity return karni hai.

---

### `productId` se partition kyu?

Is event mein:

```java
getPartitionKey()
```

`productId` return karta hai.

`ProductReservedEvent` mein bhi `productId` partition key tha.

Iska purpose hai ki same product/SKU ke related events ko same Kafka partition mein route karne mein help mile.

Example:

```text
Product 101

ProductReservedEvent
        ↓
Partition X

ReservationExpiredEvent
        ↓
Partition X
```

Isse same SKU ke state-related events ka ordering maintain karna easier hota hai.

---

### `order-service` ko iska kya fayda?

Suppose:

```text
Order = PAYMENT_PENDING
Reservation = expired
```

Event:

```text
ReservationExpiredEvent
```

Kafka se `order-service` ko milega:

```text
Reservation expired
       ↓
Order EXPIRED / CANCELLED
```

Isliye order indefinitely `PAYMENT_PENDING` mein nahi rahega.

---

### `analytics-service` ka use

Analytics ko pata chalega:

```text
Reservation created
       ↓
Reservation expired
```

Phir dashboard mein metrics bana sakte ho:

```text
Total reservations: 10,000
Expired reservations: 2,000
Successful purchases: 8,000
```

---

### `ProductReservedEvent` vs `ReservationExpiredEvent`

Dono ko pair ki tarah samjho:

```text
ProductReservedEvent
        ↓
"Product reserve ho gaya"
        ↓
Payment window
        ↓
Payment successful?
     /       \
   YES        NO
    ↓          ↓
  SOLD    ReservationExpiredEvent
```

So:

**`ProductReservedEvent` = reservation successfully create hui**

**`ReservationExpiredEvent` = reservation time par complete nahi hui, isliye expire/release ho gayi**

### Tumhare project mein flow

```text
Inventory Service
      │
      ├── Reserve successful
      │       ↓
      │  ProductReservedEvent
      │
      └── Reservation expires
              ↓
       ReservationExpiredEvent
              ↓
          Outbox
              ↓
            Kafka
          /    |    \
         ↓     ↓     ↓
      Order Analytics Notification
```

So **haan, ye file tumhare flash-sale architecture mein important hai**, especially because tum inventory ko temporary reservation ke concept ke saath design kar rahe ho.
* */
