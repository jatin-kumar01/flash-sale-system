package com.flashsale.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.flashsale.payment",
        "com.flashsale.common.exception"
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
/*This `PaymentServiceApplication.java` is **correct and should remain very simple**. It is the bootstrap/configuration entry point for the payment microservice.

## 1. What happens when you run it?

When you execute:

```bash
mvn spring-boot:run -pl payment-service
```

the `main()` method runs:

```java
SpringApplication.run(PaymentServiceApplication.class, args);
```

Spring Boot then creates the application context and discovers your payment-service components.

Conceptually:

```text
PaymentServiceApplication
          ↓
    Spring Boot starts
          ↓
 ┌──────────────────────────┐
 │ Controller                │
 │ Service                   │
 │ Repository                │
 │ Kafka Producer            │
 │ Exception Handler         │
 └──────────────────────────┘
          ↓
   Payment Service running
```

---

## 2. `@SpringBootApplication`

```java
@SpringBootApplication
```

This is the main Spring Boot annotation.

It effectively combines:

```text
@Configuration
@EnableAutoConfiguration
@ComponentScan
```

So Spring can automatically configure things such as:

```text
JPA
Kafka
Web/MVC
Validation
Database
Redis (if dependency/config exists)
```

and scan your payment-service classes.

---

## 3. `@EnableDiscoveryClient`

```java
@EnableDiscoveryClient
```

This enables service discovery integration.

Your intended architecture is:

```text
Payment Service
      │
      │ register
      ▼
Eureka Server
      │
      ▼
PAYMENT-SERVICE
```

Then another service, such as the API Gateway, can discover the payment service through Eureka instead of relying on a hard-coded instance address.

### Small correction

The statement:

> "Register the service instance with Netflix Eureka under `PAYMENT-SERVICE`"

is **not determined by this annotation alone**.

The actual service name normally comes from:

```yaml
spring:
  application:
    name: payment-service
```

And Eureka configuration determines where it registers.

So `@EnableDiscoveryClient` enables the mechanism; **`application.yml` defines the service identity and Eureka connection**.

---

## 4. `@ComponentScan`

```java
@ComponentScan(basePackages = {
        "com.flashsale.payment",
        "com.flashsale.common.exception"
})
```

This tells Spring to scan:

```text
com.flashsale.payment
```

and:

```text
com.flashsale.common.exception
```

Your payment package includes:

```text
controller/
dto/
entity/
kafka/
repository/
service/
```

So Spring can discover things such as:

```java
@RestController
@Service
@Component
@Repository
```

automatically.

---

## 5. Why scan `common.exception`?

Your common module contains the shared exception handler:

```text
common/
└── src/
    └── main/
        └── java/
            └── com/flashsale/common/exception/
                └── GlobalExceptionHandler.java
```

The payment service needs that handler registered in its Spring context.

Therefore:

```java
"com.flashsale.common.exception"
```

is explicitly included.

Your flow becomes:

```text
PaymentController
      ↓
PaymentService
      ↓
Exception occurs
      ↓
GlobalExceptionHandler
      ↓
ApiResponse
```

For example, invalid payment amount:

```json
{
  "success": false,
  "message": "Payment amount must be greater than zero"
}
```

assuming that matches your common `ApiResponse`/exception-handler implementation.

---

# 6. Port `8085`

Your description says:

> Launch the application context on port `8085`.

But **this Java class does not configure port 8085**.

The port should be in:

```yaml
server:
  port: 8085
```

inside:

```text
payment-service/src/main/resources/application.yml
```

So the actual relationship is:

```text
PaymentServiceApplication
        ↓
Spring Boot
        ↓
application.yml
        ↓
server.port = 8085
```

Don't put the port directly into this Java class.

---

# 7. Eureka URL

Similarly, this class does **not** contain:

```text
http://localhost:8761/eureka/
```

That should come from `application.yml`, typically through Eureka client configuration.

So your Java class shouldn't be modified just to specify the Eureka URL.

---

# 8. One important requirement

For this:

```java
@ComponentScan("com.flashsale.common.exception")
```

to work, the payment service must actually have the `common` module available as a dependency.

Your dependency relationship should be approximately:

```text
payment-service
      ↓
    common
      ↓
ApiResponse
PaymentEvent
Exceptions
GlobalExceptionHandler
```

If `common` isn't on the payment-service classpath, the component scan won't magically make those classes available.

---

# 9. Testing commands — Windows correction

Since you're developing on Windows and have previously used the Maven Wrapper, I recommend:

```powershell
.\mvnw.cmd clean package -pl payment-service
```

instead of:

```bash
mvn clean package -pl payment-service
```

if Maven isn't installed globally.

For running:

```powershell
.\mvnw.cmd spring-boot:run -pl payment-service
```

And then:

```text
http://localhost:8085
```

for the payment service, assuming `application.yml` specifies port `8085`.

---

# 10. Actuator test

Your test:

```bash
curl -s http://localhost:8085/actuator/health
```

will only work if **Spring Boot Actuator is actually included and configured** in the payment-service dependencies.

If it is, the expected basic response is:

```json
{
  "status": "UP"
}
```

Additional fields can appear depending on configuration.

---

# Final verdict

### `PaymentServiceApplication.java` → ✅ Keep as-is

```text
@SpringBootApplication       → Starts/configures Spring Boot
@EnableDiscoveryClient       → Enables Eureka/discovery integration
@ComponentScan               → Finds payment + common exception components
main()                       → Starts application
```

The class should **not** contain:

```text
❌ port 8085
❌ Eureka URL
❌ database URL
❌ Kafka configuration
❌ business logic
```

Those belong in `application.yml` or other configuration classes.

Your payment-service Java structure is now complete:

```text
payment-service/
└── com.flashsale.payment/
    ├── controller/
    │   └── PaymentController.java
    ├── dto/
    │   ├── PaymentRequest.java
    │   └── PaymentResponse.java
    ├── entity/
    │   └── Payment.java
    ├── kafka/
    │   └── PaymentProducer.java
    ├── repository/
    │   └── PaymentRepository.java
    ├── service/
    │   └── PaymentService.java
    └── PaymentServiceApplication.java
```

**The next and final important payment-service file is `application.yml`.** That's where we can verify whether `8085`, PostgreSQL, Kafka, Eureka, JPA, and the payment simulation properties are configured consistently with all the Java files you've shown.
*/