package com.flashsale.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_notification_order_reference", columnList = "orderReference"),
        @Index(name = "idx_notification_user_id", columnList = "userId"),
        @Index(name = "idx_notification_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String orderReference;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChannelType channel;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeliveryStatus status;

    @Column(length = 512)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public enum ChannelType {
        EMAIL,
        SMS,
        IN_APP
    }

    public enum DeliveryStatus {
        PENDING,
        SENT,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = DeliveryStatus.PENDING;
        }
    }

    public void markSent() {
        this.status = DeliveryStatus.SENT;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = DeliveryStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
/*Yes — this `NotificationLog.java` is a **JPA Entity** for your `notification-service`.

One important correction in the explanation:

> The indexes are **not composite indexes**.
> You have **three separate single-column indexes**:
>
> * `orderReference`
> * `userId`
> * `status`

Because each `@Index` has only one column.

### What this class does

Think of `NotificationLog` as a PostgreSQL table:

```text
notification_logs
------------------------------------------------
id
orderReference
userId
recipient
channel
eventType
subject
content
status
errorMessage
createdAt
```

For example, when an order is created:

```text
orderReference = ORD-1001
userId         = 25
recipient      = user@gmail.com
channel        = EMAIL
eventType      = ORDER_CREATED
subject        = Order Created
content        = Your order has been created...
status         = SENT
errorMessage   = null
createdAt      = current timestamp
```

### Important annotations

**`@Entity`**

Tells JPA/Hibernate:

```text
Java class → Database table
```

**`@Table(name = "notification_logs")`**

Specifies the table name.

**`@Id` + `@GeneratedValue`**

```java
private Long id;
```

The database automatically generates the ID.

**`@Enumerated(EnumType.STRING)`**

Instead of storing:

```text
0
1
2
```

it stores:

```text
EMAIL
SMS
IN_APP
```

and:

```text
PENDING
SENT
FAILED
```

This is much easier to understand in PostgreSQL.

### `@PrePersist`

This method runs **before a new entity is inserted**:

```java
@PrePersist
protected void onCreate() {
    this.createdAt = Instant.now();

    if (this.status == null) {
        this.status = DeliveryStatus.PENDING;
    }
}
```

So if you create:

```java
NotificationLog.builder()
    .orderReference("ORD-1001")
    .userId(25L)
    .recipient("user@gmail.com")
    .channel(ChannelType.EMAIL)
    .eventType("ORDER_CREATED")
    .subject("Order Created")
    .content("Your order has been created")
    .build();
```

you don't need to manually set:

```java
status
createdAt
```

They are automatically set to:

```text
status    → PENDING
createdAt → current time
```

### Status helpers

Instead of doing this everywhere:

```java
notification.setStatus(DeliveryStatus.SENT);
notification.setErrorMessage(null);
```

you can simply do:

```java
notification.markSent();
```

For failure:

```java
notification.markFailed("Email server unavailable");
```

Result:

```text
status       = FAILED
errorMessage = Email server unavailable
```

### One more thing

Your command:

```bash
mvn clean test-compile -pl notification-service
```

will work properly **only if the root `pom.xml` defines `notification-service` as a Maven module**.

Your project should therefore eventually look something like:

```text
flash-sale-system/
├── pom.xml                  ← Parent Maven POM
├── common/
│   └── pom.xml
├── eureka-server/
│   └── pom.xml
├── api-gateway/
│   └── pom.xml
├── auth-service/
│   └── pom.xml
├── product-service/
│   └── pom.xml
├── inventory-service/
│   └── pom.xml
├── order-service/
│   └── pom.xml
├── payment-service/
│   └── pom.xml
└── notification-service/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            └── resources/
                └── application.yml
```

So here **`pom.xml` is Maven configuration**, while **`application.yml` is Spring Boot configuration**.
*/