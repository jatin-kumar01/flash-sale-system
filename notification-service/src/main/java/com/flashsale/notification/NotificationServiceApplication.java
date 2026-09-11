package com.flashsale.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.flashsale.notification",
        "com.flashsale.common.exception"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
/*## Review of `NotificationServiceApplication.java`

This file is **correct and can be kept as-is**. It is the bootstrap class of the Notification Service and doesn't contain business logic.

### What happens when you start it?

```java
SpringApplication.run(NotificationServiceApplication.class, args);
```

Spring Boot starts the Notification Service and creates the application context:

```text
NotificationServiceApplication
        ↓
Spring Boot starts
        ↓
Scans Notification Service components
        ↓
Loads configuration from application.yml
        ↓
Connects to PostgreSQL / Redis / Kafka / Eureka
        ↓
Starts embedded web server
        ↓
Notification Service is running
```

---

### `@SpringBootApplication`

```java
@SpringBootApplication
```

This is effectively a combination of:

```text
@Configuration
@EnableAutoConfiguration
@ComponentScan
```

So Spring automatically discovers things such as:

```text
controller/
service/
repository/
kafka/
```

inside:

```text
com.flashsale.notification
```

---

### `@EnableDiscoveryClient`

```java
@EnableDiscoveryClient
```

This enables service discovery.

The important point is:

> **This annotation does not decide the service name or port.**

Those should come from `application.yml`.

For example:

```yaml
spring:
  application:
    name: notification-service

server:
  port: 8086
```

Eureka will then know the service as:

```text
NOTIFICATION-SERVICE
```

The API Gateway can discover that service through Eureka.

---

### `@ComponentScan`

```java
@ComponentScan(basePackages = {
        "com.flashsale.notification",
        "com.flashsale.common.exception"
})
```

The first package:

```text
com.flashsale.notification
```

finds your local components:

```text
controller
service
repository
kafka
```

The second:

```text
com.flashsale.common.exception
```

is important because your `GlobalExceptionHandler` is located in the shared `common` module.

Without scanning/importing that package, simply having the class in `common` does **not automatically guarantee** that this service registers it.

So this part is useful.

---

## One correction in your description

You wrote:

> "Launch the Spring Boot application context on port `8086`."

Technically, **this Java class does not configure port 8086**.

The port should be configured in:

```text
notification-service/src/main/resources/application.yml
```

So a more accurate description is:

> Launches the Notification Service; its HTTP port is configured through `application.yml`.

Same applies to Eureka registration.

---

## Actuator testing

You wrote:

```bash
curl -s http://localhost:8086/actuator/health
```

That's valid **only if Spring Boot Actuator is included in `pom.xml` and the endpoint is exposed/configured**.

If Actuator isn't in the dependencies, you'll get a 404.

---

## Windows commands

Since you're developing on Windows, use Maven Wrapper if your project contains `mvnw.cmd`:

```powershell
.\mvnw.cmd clean package -pl notification-service
```

Then:

```powershell
.\mvnw.cmd spring-boot:run -pl notification-service
```

---

# Final verdict

| Part                      | Status                            |
| ------------------------- | --------------------------------- |
| `@SpringBootApplication`  | ✅ Correct                         |
| `@EnableDiscoveryClient`  | ✅ Correct                         |
| Component scanning        | ✅ Correct                         |
| Common exception scanning | ✅ Correct                         |
| Main method               | ✅ Correct                         |
| Business logic            | ✅ None — good                     |
| Port configuration        | ⚠️ Should be in `application.yml` |
| Eureka service name       | ⚠️ Comes from `application.yml`   |
| Actuator check            | ⚠️ Requires Actuator dependency   |

### **No code changes required.** ✅

Keep this file as it is.

The next important file to inspect is **`NotificationLog.java`**, because that's where we need to verify the notification database model and whether your idempotency design is actually protected at the **database level**, rather than only through `existsByOrderReferenceAndEventType()`.
*/