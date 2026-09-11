package com.flashsale.common.event.payment;

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
public class PaymentFailedEvent extends BaseEvent {

    private String paymentId;
    private String orderId;
    private String reservationId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private String failureReason;
    private Instant failedAt;

    @Override
    public String getPartitionKey() {
        return orderId != null ? orderId : getAggregateId();
    }

    public static PaymentFailedEvent of(String paymentId, String orderId, String reservationId,
                                        Long userId, Long productId, Integer quantity,
                                        String failureReason, Instant failedAt) {
        return PaymentFailedEvent.builder()
                .eventType(EventTopics.PAYMENT_FAILED)
                .aggregateId(paymentId)
                .paymentId(paymentId)
                .orderId(orderId)
                .reservationId(reservationId)
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .failureReason(failureReason)
                .failedAt(failedAt)
                .build();
    }
}
/*
* Yes, **`PaymentFailedEvent.java` is needed** for your flash-sale system.

Simple Hinglish mein:

### Is file ki need kyu hai?

Agar user payment karta hai aur payment:

* fail ho jaati hai
* timeout ho jaati hai
* reject ho jaati hai

to system ko baaki services ko inform karna padega.

Flow:

```text
User
 ↓
Order Created
 ↓
Payment Service
 ↓
❌ Payment Failed
 ↓
PaymentFailedEvent
 ↓
Transactional Outbox
 ↓
Kafka
 ↓
┌─────────────┬──────────────┬──────────────┐
↓             ↓              ↓
Inventory     Order       Notification
Service       Service       Service
↓             ↓              ↓
Release       Cancel/       Failure
Stock         Failed        Email
```

### Sabse important: Stock release

Flash-sale mein ye bahut important hai.

Suppose:

```text
Total Stock = 100

User reserves 1
Available = 99
Reserved = 1
```

Payment fail:

```text
PaymentFailedEvent
        ↓
Inventory Service
        ↓
Reservation release
        ↓
Available = 100
Reserved = 0
```

Agar ye event nahi hoga, failed payment ke baad stock unnecessarily **reserved** reh sakta hai.

---

### `failureReason` kyu hai?

Ye batata hai payment kyu fail hui:

```text
INSUFFICIENT_FUNDS
GATEWAY_TIMEOUT
CARD_EXPIRED
```

Example:

```text
paymentId = PAY-1001
orderId = ORD-5001
failureReason = GATEWAY_TIMEOUT
```

Notification service user ko appropriate message de sakti hai.

Analytics bhi calculate kar sakta hai:

```text
Payment failures:
INSUFFICIENT_FUNDS → 500
GATEWAY_TIMEOUT    → 120
CARD_EXPIRED       → 80
```

---

### `reservationId` kyu important hai?

Payment failed hone par inventory ko **exact reservation** identify karni hogi.

```text
Order
 ↓
Reservation
 ↓
Payment Failed
 ↓
reservationId
 ↓
Release that reservation
```

Isse galat user's/product ki reservation release nahi hogi.

---

### `orderId` partition key kyu?

```java
getPartitionKey()
```

`orderId` return karta hai.

For example:

```text
ORD-5001

ORDER_CREATED
      ↓
PAYMENT_FAILED
      ↓
ORDER_CANCELLED
```

Same order ID ko Kafka key ke roop mein use karne se related events ko same partition par route karne mein help milti hai, jisse ordering maintain karna easier hota hai.

---

### `PaymentSuccessEvent` vs `PaymentFailedEvent`

Dono opposite outcomes hain:

```text
                  Payment
                     │
              ┌──────┴──────┐
              ↓             ↓
          SUCCESS          FAILED
              ↓             ↓
PaymentSuccessEvent   PaymentFailedEvent
              ↓             ↓
          SOLD          Release Stock
              ↓             ↓
        Order PAID    Order CANCELLED/
                         PAYMENT_FAILED
```

### Ab tak tumhare core events

```text
ProductReservedEvent
        ↓
Stock RESERVED

OrderCreatedEvent
        ↓
Order CREATED

PaymentSuccessEvent
        ↓
Payment SUCCESS → Stock SOLD

PaymentFailedEvent
        ↓
Payment FAILED → Stock RELEASED

ReservationExpiredEvent
        ↓
Payment time expired → Stock RELEASED
```

So **`PaymentFailedEvent.java` definitely makes sense in this architecture**, because it handles the **compensation path** when payment doesn't succeed.

*
* */