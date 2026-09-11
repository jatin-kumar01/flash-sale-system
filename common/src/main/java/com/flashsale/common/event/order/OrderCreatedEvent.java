package com.flashsale.common.event.order;

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
public class OrderCreatedEvent extends BaseEvent {

    private String orderId;
    private String reservationId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private Instant expiresAt;

    @Override
    public String getPartitionKey() {
        return orderId != null ? orderId : getAggregateId();
    }

    public static OrderCreatedEvent of(String orderId, String reservationId, Long userId,
                                       Long productId, Integer quantity, BigDecimal totalAmount,
                                       String status, Instant expiresAt) {
        return OrderCreatedEvent.builder()
                .eventType(EventTopics.ORDER_CREATED)
                .aggregateId(orderId)
                .orderId(orderId)
                .reservationId(reservationId)
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .status(status)
                .expiresAt(expiresAt)
                .build();
    }
}

/*
* Yes, **`OrderCreatedEvent.java` is needed** in your project.

Simple Hinglish mein:

### Is file ki need kyu hai?

Jab **inventory successfully reserve** ho jaati hai aur `order-service` order create karta hai, to baaki services ko inform karna hota hai:

> **"Order successfully create ho gaya hai."**

Us information ko `OrderCreatedEvent` ke through Kafka par publish kiya jaata hai.

```text
User
 ↓
Inventory Reservation
 ↓
Order Service
 ↓
Order Created
 ↓
OrderCreatedEvent
 ↓
Transactional Outbox
 ↓
Kafka
 ↓
┌──────────────┬───────────────┬─────────────┐
↓              ↓               ↓
Payment       Notification    Analytics
Service       Service         Service
```

### Is event mein kya information hai?

```text
orderId
reservationId
userId
productId
quantity
totalAmount
status
expiresAt
```

Example:

```text
Order ID       = ORD-5001
Reservation ID = RES-1001
User ID        = 25
Product ID     = 101
Quantity       = 1
Total Amount   = ₹70,000
Status         = PAYMENT_PENDING
Expires At     = 10 minutes later
```

Isse downstream services ko order ke baare mein required information mil jaati hai.

---

### `reservationId` kyu important hai?

Tumhare flash-sale system mein:

```text
Product Reserved
       ↓
Reservation ID = RES-1001
       ↓
Order Created
       ↓
Order ID = ORD-5001
```

Dono ko link karne ke liye:

```text
ORD-5001 → RES-1001
```

Ye useful hai agar baad mein:

```text
Payment Failed
```

ya:

```text
Reservation Expired
```

ho.

---

### `getPartitionKey()` mein `orderId` kyu?

```java
@Override
public String getPartitionKey() {
    return orderId != null ? orderId : getAggregateId();
}
```

Same order ke events ko same Kafka partition mein route karne mein help karta hai.

For example:

```text
ORD-5001

ORDER_CREATED
      ↓
ORDER_CONFIRMED
      ↓
ORDER_CANCELLED
```

Same order ID key hone se in events ka ordering behavior maintain karna easier hota hai.

---

### `expiresAt` kyu?

Order inventory reservation ke saath linked hai.

Example:

```text
Order Created
     ↓
Payment Pending
     ↓
10-minute payment window
     ↓
Payment?
```

Agar payment nahi hua:

```text
Reservation expires
      ↓
Order expires/cancelled
      ↓
Stock released
```

Isliye order event mein reservation ki expiry information useful hai.

---

### `OrderCreatedEvent` aur `ProductReservedEvent`

In dono ka relationship:

```text
ProductReservedEvent
        ↓
"Stock successfully reserved"
        ↓
Order Service
        ↓
Order created
        ↓
OrderCreatedEvent
        ↓
Kafka
```

So:

**`ProductReservedEvent` = inventory reservation successful**

**`OrderCreatedEvent` = reservation ke basis par order successfully create hua**

---

### Payment Service ka role

`payment-service` `ORDER_CREATED` event consume karke payment process start kar sakta hai:

```text
ORDER_CREATED
      ↓
Payment Service
      ↓
Create Payment Intent/Session
      ↓
Payment
```

Important: actual payment success baad mein separate event ke through communicate hoga, for example:

```text
PAYMENT_SUCCESS
```

---

### Short mein

`OrderCreatedEvent.java` ek **standard Kafka message contract** hai jo batata hai:

> **"Inventory reservation ke baad ek order successfully create ho gaya hai."**

Aur phir:

```text
OrderCreatedEvent
       ↓
Kafka
   ┌───┼────┐
   ↓   ↓    ↓
Payment Notification Analytics
```

Tumhare **flash-sale + Kafka + Transactional Outbox** architecture mein ye file logically required hai.

* */
