package com.flashsale.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
/*Bilkul. Is `ApiGatewayApplication.java` ko **simple Hinglish + example** se samjho.

# `ApiGatewayApplication.java` kya hai?

Ye **API Gateway microservice ka main/starting point** hai.

Tumhare architecture mein:

```text
User / Frontend
      ↓
 API Gateway
      ↓
 ┌────┼────────┬─────────┐
 ↓    ↓        ↓         ↓
Auth Product Inventory  Order
```

Frontend directly har microservice ko call nahi karega. Pehle request **API Gateway** par aayegi.

---

# 1. Package

```java
package com.flashsale.gateway;
```

Iska matlab ye class kis Java package ke andar hai.

Tumhari file location:

```text
api-gateway/
└── src/
    └── main/
        └── java/
            └── com/
                └── flashsale/
                    └── gateway/
                        └── ApiGatewayApplication.java
```

Isliye package:

```java
com.flashsale.gateway
```

hona chahiye.

---

# 2. `SpringApplication`

```java
import org.springframework.boot.SpringApplication;
```

Ye Spring Boot application ko **start** karne ke liye use hota hai.

Main method mein:

```java
SpringApplication.run(
    ApiGatewayApplication.class,
    args
);
```

Spring Boot ko bolta hai:

> Application start karo aur Spring configuration load karo.

---

# 3. `@SpringBootApplication`

```java
@SpringBootApplication
```

Ye Spring Boot ka **main annotation** hai.

Actually ye multiple functionalities ko combine karta hai:

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

### Simple meaning

Spring Boot ko bolta hai:

> Is class se application ko bootstrap karo, required configurations automatically setup karo aur components ko scan karo.

---

## Example

Tumhare Gateway mein:

```text
com.flashsale.gateway
├── config/
│   └── JwtUtil.java
├── filter/
│   └── JwtAuthenticationFilter.java
└── ApiGatewayApplication.java
```

`@SpringBootApplication` ki wajah se Spring application ke components/configuration ko discover kar sakta hai.

---

# 4. `@EnableDiscoveryClient`

```java
@EnableDiscoveryClient
```

Ye **service discovery** ke liye hai.

Tumhare architecture mein Eureka Server hai:

```text
             Eureka Server
             :8761
                 ↑
       ┌─────────┼─────────┐
       │         │         │
    Gateway     Auth     Product
```

Gateway ko pata hona chahiye ki:

```text
AUTH-SERVICE
PRODUCT-SERVICE
INVENTORY-SERVICE
ORDER-SERVICE
```

ki actual running instances kahan hain.

Eureka ye information maintain karta hai.

---

# 5. Real example

Suppose Inventory Service:

```text
INVENTORY-SERVICE
```

ki 3 instances running hain:

```text
Instance 1 → localhost:8081
Instance 2 → localhost:8082
Instance 3 → localhost:8083
```

Ye instances Eureka mein register ho sakte hain.

Gateway ko request milti hai:

```http
GET /api/inventory/101
```

Gateway internally service discovery ke through:

```text
INVENTORY-SERVICE
        ↓
Eureka
        ↓
Available instances
        ↓
8081 / 8082 / 8083
```

mein se appropriate instance tak request route kar sakta hai.

---

# 6. `public class`

```java
public class ApiGatewayApplication {
```

Ye tumhari main Java class hai.

Class ka naam:

```text
ApiGatewayApplication
```

aur file ka naam:

```text
ApiGatewayApplication.java
```

same hona chahiye.

---

# 7. `main()` method

```java
public static void main(String[] args) {
```

Java application ka **entry point** hai.

Jab tum application run karte ho:

```bash
mvn spring-boot:run
```

ya IntelliJ se Run karte ho, execution yahin se start hota hai.

---

# 8. `SpringApplication.run()`

```java
SpringApplication.run(
    ApiGatewayApplication.class,
    args
);
```

Ye actual Spring Boot startup trigger karta hai.

Flow:

```text
main()
  ↓
SpringApplication.run()
  ↓
Spring Boot starts
  ↓
Configuration load
  ↓
Gateway components load
  ↓
Netty server starts
  ↓
API Gateway ready
```

---

# 9. Netty yahan kyu?

Tumhare description mein important point hai:

> Spring Cloud Gateway reactive application hai.

Spring Cloud Gateway normally **WebFlux/reactive stack** use karta hai.

Isliye traditional Spring MVC/Tomcat ke instead:

```text
Spring Cloud Gateway
        ↓
Spring WebFlux
        ↓
Reactive Netty
```

use hota hai.

### Simple example

Suppose ek saath bahut saari requests aa rahi hain:

```text
User 1 ──┐
User 2 ──┤
User 3 ──┤
User 4 ──┤──→ API Gateway
User 5 ──┤
...      │
Million requests during flash sale
```

Gateway ka reactive/non-blocking architecture high-concurrency workloads ke liye useful hai.

---

# 10. JWT filter ka relation

Tumhare folder mein:

```text
filter/
└── JwtAuthenticationFilter.java
```

hai.

Gateway request ko receive karke JWT check kar sakta hai.

Example:

```text
Frontend
   ↓
GET /api/orders
Authorization: Bearer <JWT>
   ↓
API Gateway
   ↓
JwtAuthenticationFilter
   ↓
JWT valid?
   ├── YES → Order Service
   └── NO  → 401 Unauthorized
```

Lekin **`ApiGatewayApplication.java` khud JWT verify nahi karta**.

JWT verification ka actual logic `JwtAuthenticationFilter`/related configuration mein hoga.

---

# 11. `JwtUtil.java` ka relation

```text
config/
└── JwtUtil.java
```

`JwtUtil` JWT se related operations provide kar sakta hai, jaise:

```text
Token read
   ↓
Token validate
   ↓
User information extract
```

Aur filter us utility ko use kar sakta hai.

Flow:

```text
Request
   ↓
JwtAuthenticationFilter
   ↓
JwtUtil
   ↓
JWT valid?
```

---

# 12. `ApiGatewayApplication` ka actual kaam

Is class ka kaam **business logic likhna nahi hai**.

Ye primarily:

```text
Application Start
       ↓
Spring Boot Bootstrap
       ↓
Gateway Application Initialize
       ↓
Eureka Discovery Enable
       ↓
Gateway Ready
```

---

# 13. Complete example

Suppose frontend:

```http
POST /api/orders
```

request bhejta hai.

Overall architecture:

```text
                 FRONTEND
                    │
                    ▼
             API GATEWAY :8080
                    │
          JwtAuthenticationFilter
                    │
               JWT valid?
                    │
                    ▼
              Eureka Server
                    │
                    ▼
              ORDER-SERVICE
                    │
                    ▼
              Order Processing
```

`ApiGatewayApplication.java` is poore Gateway application ko **start** karne wala entry point hai.

---

# 14. Har annotation/method ka simple meaning

| Code                      | Simple meaning                                           |
| ------------------------- | -------------------------------------------------------- |
| `package`                 | Class ki location/package define karta hai               |
| `@SpringBootApplication`  | Spring Boot application ko configure/start karne ka base |
| `@EnableDiscoveryClient`  | Eureka/service discovery ke saath integrate karta hai    |
| `main()`                  | Java application ka starting point                       |
| `SpringApplication.run()` | Spring Boot application actually start karta hai         |

### Ek line mein:

**`ApiGatewayApplication.java` = API Gateway ka "ON button" + Spring Boot bootstrap class + Eureka discovery enable karne wali main class.**

Aur yaad rakho: **JWT filtering, routing rules, Eureka URL, port 8080, etc. is class mein directly implement nahi hote**; woh respective configuration/filter/application files mein define honge.
*/