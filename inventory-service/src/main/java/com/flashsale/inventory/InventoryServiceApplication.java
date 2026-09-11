package com.flashsale.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.flashsale.inventory", "com.flashsale.common.exception"})
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
/*This `InventoryServiceApplication.java` is the **bootstrap/entry-point class** of your Inventory microservice. The implementation is simple and appropriate.

### What happens when you start it?

```text
InventoryServiceApplication
          ↓
SpringApplication.run()
          ↓
Spring Boot Application Context
          ↓
┌─────────┬─────────┬─────────┬──────────┐
│ REST    │ Redis   │ Kafka   │ JPA/DB   │
│ APIs    │         │Consumer │          │
└─────────┴─────────┴─────────┴──────────┘
```

### 1. `@SpringBootApplication`

```java
@SpringBootApplication
```

This is the main Spring Boot annotation. It effectively enables:

```text
@Configuration
@EnableAutoConfiguration
@ComponentScan
```

Because your main class is inside:

```text
com.flashsale.inventory
```

Spring automatically discovers your:

```text
controller/
service/
repository/
kafka/
config/
```

classes.

---

### 2. `@EnableDiscoveryClient`

```java
@EnableDiscoveryClient
```

This allows the Inventory Service to participate in service discovery.

Conceptually:

```text
Inventory Service
       ↓
     Eureka
       ↓
INVENTORY-SERVICE
       ↓
API Gateway / other services
```

One small clarification: the application name such as `INVENTORY-SERVICE` normally comes from your configuration (`spring.application.name`), not from `@EnableDiscoveryClient` itself.

---

### 3. `@ComponentScan`

```java
@ComponentScan(basePackages = {
        "com.flashsale.inventory",
        "com.flashsale.common.exception"
})
```

The first package:

```text
com.flashsale.inventory
```

finds your Inventory Service components.

The second:

```text
com.flashsale.common.exception
```

is there because your common module contains the shared exception handler.

So:

```text
Inventory Service
      +
Common Exception Handler
      ↓
Spring Application Context
```

This is useful for your current multi-module structure.

---

### 4. `main()`

```java
public static void main(String[] args) {
    SpringApplication.run(InventoryServiceApplication.class, args);
}
```

This is simply the starting point.

When you run the service:

```text
Java
 ↓
main()
 ↓
SpringApplication.run()
 ↓
Spring Boot starts
 ↓
Inventory Service running
```

---

## ⚠️ One correction to the description

Your description says:

> "Launch the Spring Boot application context on port 8083."

The **Java class itself does not configure port `8083`**.

That should come from:

```yaml
server:
  port: 8083
```

inside `application.yml`.

Likewise, Redis, Kafka, Redisson, and Eureka configuration isn't created by this class. They are **auto-configured based on your dependencies and application configuration**.

So a more accurate purpose would be:

> **Bootstrap the Inventory microservice and configure component discovery, including the shared exception handler package.**

---

## Final verdict

```text
InventoryServiceApplication.java
            │
            ├── Starts Spring Boot ✅
            ├── Discovers inventory components ✅
            ├── Enables Eureka client ✅
            ├── Discovers common exception handler ✅
            └── Starts configured Redis/Kafka/JPA infrastructure ✅
```

**No major code change is needed here.** This is a clean application entry point.

One practical point for your Windows setup: when testing this project, use the project's Maven Wrapper if `mvn` isn't installed globally:

```powershell
.\mvnw.cmd clean package -pl inventory-service
```

and:

```powershell
.\mvnw.cmd spring-boot:run -pl inventory-service
```

The next thing to verify is the **`inventory-service/pom.xml` and `application.yml`**, because those determine whether Redis, Redisson, Kafka, PostgreSQL, Eureka, and Actuator actually start correctly.
*/