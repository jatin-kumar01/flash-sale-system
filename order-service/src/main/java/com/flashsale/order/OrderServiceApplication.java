package com.flashsale.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.flashsale.order.client")
@EnableScheduling
@ComponentScan(basePackages = {
    "com.flashsale.order",
    "com.flashsale.common.exception"
})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
/*This `OrderServiceApplication.java` is the **bootstrap class for your Order Service**, and its configuration matches the structure of the Order Service you've provided.

### Overall startup flow

```text
OrderServiceApplication
          ↓
     Spring Boot
          ↓
 ┌────────┬──────────┬──────────┬────────────┐
 ↓        ↓          ↓          ↓
REST    OpenFeign  Scheduler  Eureka
API      Clients
          ↓
   InventoryClient
   ProductClient
```

### 1. `@SpringBootApplication`

```java
@SpringBootApplication
```

Starts Spring Boot and automatically discovers components under:

```text
com.flashsale.order
```

So it can find:

```text
client/
controller/
kafka/
repository/
scheduler/
service/
```

---

### 2. `@EnableFeignClients`

```java
@EnableFeignClients(basePackages = "com.flashsale.order.client")
```

This is important for your microservice communication.

It scans:

```text
client/
├── InventoryClient.java
└── ProductClient.java
```

and creates implementations/proxies automatically.

Conceptually:

```text
OrderService
    ↓
InventoryClient
    ↓
ORDER SERVICE → Eureka → INVENTORY-SERVICE
```

and:

```text
OrderService
    ↓
ProductClient
    ↓
ORDER SERVICE → Eureka → PRODUCT-SERVICE
```

So you don't need to manually write `RestTemplate`/HTTP connection code.

---

### 3. `@EnableScheduling`

```java
@EnableScheduling
```

Activates Spring's scheduler.

Your:

```text
scheduler/
└── OrderExpirationScheduler.java
```

can then periodically find unpaid orders whose:

```text
paymentDeadline < current time
```

and expire them.

Flow:

```text
Scheduler
    ↓
Find expired PENDING_PAYMENT orders
    ↓
OrderService
    ↓
EXPIRED
    ↓
Publish order.expired
    ↓
Inventory Service
    ↓
Release reserved stock
```

This is an important part of your flash-sale reservation lifecycle.

---

### 4. `@EnableDiscoveryClient`

```java
@EnableDiscoveryClient
```

Allows the Order Service to register with Eureka.

Conceptually:

```text
Eureka
 ├── PRODUCT-SERVICE
 ├── INVENTORY-SERVICE
 └── ORDER-SERVICE
```

Then Feign can use service discovery instead of hardcoding service IP addresses.

---

### 5. `@ComponentScan`

```java
@ComponentScan(basePackages = {
        "com.flashsale.order",
        "com.flashsale.common.exception"
})
```

The first package scans your Order Service.

The second scans the shared exception package so your common `GlobalExceptionHandler` can be registered.

That's consistent with the same pattern you used in Inventory Service.

---

## ⚠️ One important point

The description says this class:

> "Launches the application context on port 8084."

The Java class itself **doesn't set port 8084**.

That should be in `application.yml`:

```yaml
server:
  port: 8084
```

Likewise, the service name should normally be configured there:

```yaml
spring:
  application:
    name: ORDER-SERVICE
```

---

## Final verdict

Your class is clean:

```text
@SpringBootApplication      ✅
@EnableDiscoveryClient      ✅
@EnableFeignClients         ✅
@EnableScheduling           ✅
@ComponentScan              ✅
main()                      ✅
```

**No major changes are required to this file.**

The next logical file is **`OrderRepository.java`**, because after the entity and application bootstrap, the repository will define how orders are stored, searched for expiration, and eventually protected against duplicate/concurrent state transitions.
*/