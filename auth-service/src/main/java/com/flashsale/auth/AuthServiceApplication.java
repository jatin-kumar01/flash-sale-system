package com.flashsale.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.flashsale.auth",
        "com.flashsale.common.exception"
})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
/*## `AuthServiceApplication.java` — Hinglish Explanation

Ye file **Auth Service ka entry point / starting point** hai. Jab tum Auth Service run karte ho, execution **isi class ke `main()` method se start hota hai**.

```text
AuthServiceApplication
        ↓
Spring Boot starts
        ↓
Auth components load
        ↓
Eureka registration
        ↓
Auth Service ready
```

---

### 1. `@SpringBootApplication`

```java
@SpringBootApplication
```

Ye Spring Boot ki main annotation hai. Ye internally mainly 3 kaam karti hai:

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

#### Example

Tumhare Auth Service mein:

```text
controller/
service/
repository/
config/
```

hain.

Spring Boot in components ko detect karke application context mein load karta hai.

For example:

```java
@RestController
public class AuthController {
}
```

Spring is controller ko automatically detect karega.

---

### 2. `@EnableDiscoveryClient`

```java
@EnableDiscoveryClient
```

Iska purpose Auth Service ko **service discovery system (Eureka)** ke saath register karna hai.

Tumhare architecture mein:

```text
                 Eureka Server
                      ↑
                      │
              registers itself
                      │
                Auth Service
```

Suppose Auth Service:

```text
AUTH-SERVICE
Port: 8081
```

par run ho raha hai.

Eureka mein approximately information available hogi:

```text
AUTH-SERVICE
    ↓
localhost:8081
```

Then API Gateway service name ke through Auth Service ko locate kar sakta hai:

```text
Client
  ↓
API Gateway
  ↓
lb://AUTH-SERVICE
  ↓
Auth Service :8081
```

**Important:** Actual registration tabhi successfully hogi jab Eureka Server running ho aur `application.yml` mein discovery configuration properly configured ho.

---

### 3. `@ComponentScan`

```java
@ComponentScan(basePackages = {
        "com.flashsale.auth",
        "com.flashsale.common.exception"
})
```

Ye Spring ko batata hai ki **kin packages ke components scan karne hain**.

#### First package

```text
com.flashsale.auth
```

Isse tumhare Auth Service ke components milenge:

```text
controller
service
repository
config
```

Example:

```java
@RestController
public class AuthController
```

Spring isko detect karega.

---

### 4. `com.flashsale.common.exception`

```java
"com.flashsale.common.exception"
```

Ye tumhare shared `GlobalExceptionHandler` ke liye hai.

Tumhara structure:

```text
common
 └── exception
      └── GlobalExceptionHandler.java
```

Auth Service mein:

```text
AuthController
      ↓
Exception
      ↓
GlobalExceptionHandler
      ↓
Standard ApiResponse
```

Isliye Auth Service explicitly common exception package ko scan kar raha hai.

---

# 5. `main()` method

```java
public static void main(String[] args) {
    SpringApplication.run(
        AuthServiceApplication.class,
        args
    );
}
```

Ye **Java application ka starting point** hai.

Jab tum:

```bash
.\mvnw.cmd spring-boot:run
```

run karte ho, ultimately Spring Boot isi application class ko start karta hai.

---

## `SpringApplication.run()`

```java
SpringApplication.run(
    AuthServiceApplication.class,
    args
);
```

Ye Spring Boot application start karta hai.

Conceptually:

```text
main()
  ↓
SpringApplication.run()
  ↓
Spring Container
  ↓
Auto Configuration
  ↓
Component Scanning
  ↓
Controllers
Services
Repositories
Configs
  ↓
Auth Service Started
```

---

# 6. `args`

```java
String[] args
```

Command-line arguments receive karta hai.

For example:

```bash
java -jar auth-service.jar --server.port=8082
```

to:

```text
args
 ↓
--server.port=8082
```

application ko pass ho sakta hai.

---

# Complete Auth Service Flow

```text
        AuthServiceApplication
                  │
                  ▼
        @SpringBootApplication
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
 Component Scan        Auto Configuration
        │
        ▼
   Auth Components
        │
        ├── Controller
        ├── Service
        ├── Repository
        └── Config
        │
        ▼
 @EnableDiscoveryClient
        │
        ▼
   Eureka Server
```

Aur exception ke liye:

```text
Auth Controller
      ↓
Exception
      ↓
GlobalExceptionHandler
      ↓
ApiResponse
      ↓
Frontend
```

---

## Testing

Tumhare system mein global Maven installed nahi tha, isliye **`mvn` mat use karna**.

Root directory se Maven Wrapper use karo:

```powershell
.\mvnw.cmd clean package -pl auth-service
```

Run:

```powershell
.\mvnw.cmd spring-boot:run -pl auth-service
```

Agar `-pl auth-service` ke saath `common` dependency resolve karne mein issue aaye, root project se:

```powershell
.\mvnw.cmd clean package
```

karna better hoga.

### One important point

Tumhare pasted description mein:

> `Verify registration and health: curl -s http://localhost:8081/actuator/health`

ye tabhi work karega jab **Actuator dependency/configuration** Auth Service mein actually available ho. Sirf `AuthServiceApplication.java` hone se `/actuator/health` automatically guaranteed nahi hai.

Overall, **ye file simple hai: iska main kaam Auth Service ko start karna, Auth components scan karna, aur Eureka discovery integration activate karna hai.**
*/