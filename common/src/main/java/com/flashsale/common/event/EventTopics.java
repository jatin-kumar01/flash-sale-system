package com.flashsale.common.event;

public final class EventTopics {

    private EventTopics() {
        // Prevent instantiation
    }

    // Kafka Topics
    public static final String INVENTORY_EVENTS_TOPIC = "flashsale.inventory.events";
    public static final String ORDER_EVENTS_TOPIC = "flashsale.order.events";
    public static final String PAYMENT_EVENTS_TOPIC = "flashsale.payment.events";
    public static final String NOTIFICATION_EVENTS_TOPIC = "flashsale.notification.events";

    // Event Types - Inventory
    public static final String PRODUCT_RESERVED = "PRODUCT_RESERVED";
    public static final String RESERVATION_EXPIRED = "RESERVATION_EXPIRED";
    public static final String PRODUCT_SOLD = "PRODUCT_SOLD";
    public static final String RESERVATION_RELEASED = "RESERVATION_RELEASED";

    // Event Types - Order
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String ORDER_SHIPPED = "ORDER_SHIPPED";

    // Event Types - Payment
    public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    // Standard Consumer Group IDs
    public static final String ORDER_SERVICE_GROUP = "order-service-group";
    public static final String INVENTORY_SERVICE_GROUP = "inventory-service-group";
    public static final String PAYMENT_SERVICE_GROUP = "payment-service-group";
    public static final String NOTIFICATION_SERVICE_GROUP = "notification-service-group";
    public static final String ANALYTICS_SERVICE_GROUP = "analytics-service-group";
    public static final String INVOICE_SERVICE_GROUP = "invoice-service-group";
}


/*
* Yes, **`EventTopics.java` is needed** in this architecture.

Simple Hinglish mein:

### Is file ki need kyu hai?

Tumhare project mein multiple services Kafka ke through events exchange karengi:

```text
Inventory
   ↓
Kafka
   ↓
Order
Payment
Notification
Analytics
Invoice
```

Har jagah agar manually topic names likhoge:

```java
"flashsale.inventory.events"
"flashsale.order.events"
"flashsale.payment.events"
```

to spelling mistake ka risk hai.

`EventTopics.java` mein ek baar define kar do:

```java
public static final String INVENTORY_EVENTS_TOPIC =
        "flashsale.inventory.events";
```

Phir kahin bhi:

```java
@KafkaListener(topics = EventTopics.INVENTORY_EVENTS_TOPIC)
```

use kar sakte ho.

### Event types ke liye bhi

Instead of:

```java
"PRODUCT_RESERVED"
```

multiple places par likhne ke:

```java
EventTopics.PRODUCT_RESERVED
```

use karoge.

So:

```text
EventTopics.java
       │
       ├── Topic names
       ├── Event names
       └── Consumer group IDs
```

### Consumer Group ka fayda

Example:

```text
analytics-service-group
```

Analytics service ki multiple instances hain:

```text
Analytics Instance 1
Analytics Instance 2
Analytics Instance 3
```

Same consumer group ke andar Kafka messages ko instances ke beech distribute kar sakta hai, allowing horizontal scaling according to partitions.

### `BaseEvent` vs `EventTopics`

Dono ka role different hai:

```text
BaseEvent
   ↓
Event ke ANDAR kya information hogi?
```

Example:

```text
eventId
eventType
aggregateId
occurredAt
version
```

While:

```text
EventTopics
   ↓
Event KAHAN publish/consume hoga?
```

Example:

```text
flashsale.inventory.events
flashsale.order.events
flashsale.payment.events
```

### Short mein

**`BaseEvent.java` = Event ka common structure**

**`EventTopics.java` = Kafka communication ke common names**

Tumhare Kafka + microservices architecture mein `EventTopics.java` **useful aur recommended hai**, especially consistency aur typo/misconfiguration avoid karne ke liye.
*/