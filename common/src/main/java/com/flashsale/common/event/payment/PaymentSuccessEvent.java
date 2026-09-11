package com.flashsale.common.event.payment;

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
public class PaymentSuccessEvent extends BaseEvent {

    private String paymentId;
    private String orderId;
    private String reservationId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;
    private String paymentMethod;
    private Instant paidAt;

    @Override
    public String getPartitionKey() {
        return orderId != null ? orderId : getAggregateId();
    }

    public static PaymentSuccessEvent of(String paymentId, String orderId, String reservationId,
                                         Long userId, Long productId, Integer quantity,
                                         BigDecimal amount, String paymentMethod, Instant paidAt) {
        return PaymentSuccessEvent.builder()
                .eventType(EventTopics.PAYMENT_SUCCESS)
                .aggregateId(paymentId)
                .paymentId(paymentId)
                .orderId(orderId)
                .reservationId(reservationId)
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paidAt(paidAt)
                .build();
    }
}

/*
* Yes, **`PaymentSuccessEvent.java` is needed** and it is a key event in your flash-sale flow.

### Simple Hinglish

Jab payment successfully complete ho jaata hai:

```text
User
 ↓
Order
 ↓
Payment Service
 ↓
Payment SUCCESS
 ↓
PaymentSuccessEvent
 ↓
Transactional Outbox
 ↓
Kafka
```

Ye event baaki services ko batata hai:

> **"Is order ka payment successfully complete ho gaya hai."**

### Isme kya information ja rahi hai?

```text
paymentId
orderId
reservationId
userId
productId
quantity
amount
paymentMethod
paidAt
```

Example:

```text
paymentId     = PAY-1001
orderId       = ORD-5001
reservationId = RES-1001
productId     = 101
quantity      = 1
amount        = ₹70,000
paymentMethod = UPI
paidAt        = 18:30 UTC
```

### Sabse important: Inventory ko kya fayda?

Tumhara flow:

```text
Inventory
   ↓
RESERVED
   ↓
Payment
   ↓
SUCCESS
   ↓
PaymentSuccessEvent
   ↓
Inventory
   ↓
SOLD
```

`reservationId` aur `productId` event mein hone ki wajah se inventory service ko order service se dobara synchronous request karne ki zarurat nahi padti.

---

### `order-service`

Payment successful hone ke baad:

```text
PAYMENT_SUCCESS
      ↓
Order Service
      ↓
Order = PAID / CONFIRMED
```

---

### `invoice-service`

```text
PAYMENT_SUCCESS
      ↓
Invoice Service
      ↓
Generate PDF Invoice
```

Ye asynchronous hai, isliye user ko invoice generation ke liye checkout request mein wait nahi karna padta.

---

### `notification-service`

```text
PAYMENT_SUCCESS
      ↓
Notification Service
      ↓
Payment confirmation email
```

---

### `analytics-service`

```text
PAYMENT_SUCCESS
      ↓
Analytics
      ↓
Revenue + successful sales
```

Example:

```text
Revenue = ₹70,000
Successful Orders = +1
```

---

### `orderId` partition key kyu?

```java
getPartitionKey()
```

mein `orderId` use ho raha hai.

Tumhare previous event mein bhi:

```text
OrderCreatedEvent → orderId
```

Aur ab:

```text
PaymentSuccessEvent → orderId
```

So same order ke events ko same Kafka partition par route karne mein help milti hai.

Conceptually:

```text
ORD-5001

ORDER_CREATED
      ↓
PAYMENT_SUCCESS
      ↓
ORDER_CONFIRMED
```

Ordering maintain karna easier hota hai.

---

### `aggregateId = paymentId` kyu?

Ye event specifically **payment entity** ke around hai.

Isliye:

```text
aggregateId = paymentId
```

Example:

```text
PAY-1001
```

Ye outbox/event tracking ke liye useful hai.

---

### Overall flow

Ab tak tumhare events ko combine karo:

```text
1. ProductReservedEvent
          ↓
   Stock RESERVED
          ↓
2. OrderCreatedEvent
          ↓
   Order CREATED
          ↓
3. Payment Service
          ↓
   Payment successful
          ↓
4. PaymentSuccessEvent
          ↓
       Kafka
    ┌─────┼────────┬─────────┐
    ↓     ↓        ↓         ↓
Inventory Order  Invoice Notification
   ↓       ↓       ↓           ↓
 SOLD     PAID    PDF         Email
              \      |       /
                   Analytics
```

**Short mein:**

`ProductReservedEvent` → **Stock reserve hua**

`OrderCreatedEvent` → **Order create hua**

`PaymentSuccessEvent` → **Payment successful hua**

Ye teen events tumhare flash-sale system ke core business flow ko represent karte hain.
*/
