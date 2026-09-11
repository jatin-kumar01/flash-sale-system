package com.flashsale.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
/*
Bilkul. Is `EurekaServerApplication.java` ko tumhare **Flash Sale Microservices Project** ke context mein simple Hinglish mein samjho.

---

# 1. Eureka Server kya hai?

Tumhare project mein multiple microservices hain:

```text
Auth Service
Product Service
Inventory Service
Order Service
Payment Service
Notification Service
Analytics Service
```

Ab problem ye hai ki ek service ko doosri service ka **IP address aur port** kaise pata chalega?

Example:

```text
Order Service
     ↓
Inventory Service ko call karna hai
     ↓
Inventory Service kis machine/port par hai?
```

Yahin **Eureka Server** ka kaam aata hai.

Eureka ek **Service Discovery / Service Registry** hai.

```text
                 Eureka Server
                     │
       ┌─────────────┼─────────────┐
       ↓             ↓             ↓
   Auth Service  Product Service  Inventory
       │             │             │
       └──── register themselves ──┘
```

Eureka ko tum ek **phone directory** ki tarah samajh sakte ho.

---

# 2. `EurekaServerApplication` kya hai?

Tumhari file:

```text
eureka-server/
└── src/main/java/com/flashsale/eureka/
    └── EurekaServerApplication.java
```

Ye Eureka Server application ka **main/entry point** hai.

Jab tum ise run karte ho:

```text
Java Application
      ↓
Spring Boot starts
      ↓
Eureka Server starts
      ↓
Port 8761
```

---

# 3. Package

```java
package com.flashsale.eureka;
```

Iska matlab class is package ke andar hai:

```text
com.flashsale.eureka
```

File structure bhi isi package ke according hai:

```text
src/main/java/
└── com/
    └── flashsale/
        └── eureka/
            └── EurekaServerApplication.java
```

Package aur folder structure properly match hona chahiye.

---

# 4. Imports

### `SpringApplication`

```java
import org.springframework.boot.SpringApplication;
```

Ye Spring Boot application ko start karne ke liye use hota hai.

Main method mein:

```java
SpringApplication.run(...);
```

application start karta hai.

---

### `@SpringBootApplication`

```java
import org.springframework.boot.autoconfigure.SpringBootApplication;
```

Ye Spring Boot ki main annotation hai.

Iske through Spring Boot:

* configuration detect karta hai
* required beans configure karta hai
* component scanning karta hai
* auto-configuration karta hai

Basically ye application ko Spring Boot application banane mein help karta hai.

---

### `@EnableEurekaServer`

```java
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
```

Ye specifically **Eureka Server functionality enable** karta hai.

Without this:

```text
Spring Boot application
```

to start ho sakti hai, lekin Eureka Registry ki functionality activate nahi hogi.

---

# 5. `@SpringBootApplication`

Code:

```java
@SpringBootApplication
```

Isko simple language mein:

> "Spring Boot, is class ko application ka main configuration point samjho aur required Spring configuration automatically setup karo."

Tumhare Eureka application mein:

```text
@SpringBootApplication
        ↓
Spring Boot setup
        ↓
Application context
        ↓
Required components/configuration
```

---

# 6. `@EnableEurekaServer`

Code:

```java
@EnableEurekaServer
```

Ye sabse important annotation hai **Eureka ke liye**.

Ye Spring Cloud ko batata hai:

> "Is Spring Boot application ko Eureka Discovery Server ke roop mein configure karo."

Iske baad Eureka registry functionality available hoti hai.

---

# 7. Class

```java
public class EurekaServerApplication {
```

Ye tumhari main application class hai.

Naam:

```text
EurekaServerApplication
```

Ye sirf naming convention hai. Tum technically different naam rakh sakte ho, lekin ye naam clear hai.

---

# 8. `main()` method

```java
public static void main(String[] args) {
```

Ye Java application ka starting point hai.

Jab tum application run karte ho, execution yahin se start hoti hai.

---

# 9. `SpringApplication.run()`

Sabse important line:

```java
SpringApplication.run(
    EurekaServerApplication.class,
    args
);
```

Ye Spring Boot application ko actually start karti hai.

Flow:

```text
main()
  ↓
SpringApplication.run()
  ↓
Spring Boot starts
  ↓
Spring Context
  ↓
Eureka configuration
  ↓
Eureka Server
```

---

# 10. Real example

Suppose tumhare project mein:

```text
eureka-server → 8761
product-service → 8081
inventory-service → 8082
order-service → 8083
payment-service → 8084
```

Sabse pehle:

```text
Eureka Server
http://localhost:8761
```

start hota hai.

Then Product Service start hoti hai:

```text
Product Service
     ↓
Eureka ko register
     ↓
PRODUCT-SERVICE
     ↓
localhost:8081
```

Inventory Service:

```text
Inventory Service
     ↓
Eureka ko register
     ↓
INVENTORY-SERVICE
     ↓
localhost:8082
```

Order Service:

```text
Order Service
     ↓
Eureka ko register
     ↓
ORDER-SERVICE
     ↓
localhost:8083
```

Eureka ke paas registry ho jayegi:

```text
EUREKA REGISTRY

PRODUCT-SERVICE      → 8081
INVENTORY-SERVICE    → 8082
ORDER-SERVICE        → 8083
PAYMENT-SERVICE      → 8084
```

---

# 11. Order Service ko Inventory Service chahiye

Suppose flash sale mein user buy karta hai:

```text
Frontend
   ↓
Order Service
   ↓
Inventory Service
```

Order Service ko manually ye nahi pata hona chahiye:

```text
localhost:8082
```

Instead service discovery use kar sakti hai:

```text
Order Service
      ↓
"INVENTORY-SERVICE"
      ↓
Eureka Server
      ↓
Inventory Service ka available instance
      ↓
Request
```

Ye microservices architecture ka major benefit hai.

---

# 12. Eureka Server ko phone directory samjho

Simple analogy:

### Without Eureka

Tumhare paas 10 friends hain aur tumhe har friend ka:

```text
Name
Phone number
Current location
```

manually maintain karna pade.

### With Eureka

Sab friends ek directory mein register karte hain:

```text
Rahul → 987...
Amit → 876...
Raj → 765...
```

Tum directory se required person find kar lete ho.

Microservices mein:

```text
Service Name → Service Instance/Location
```

Eureka maintain karta hai.

---

# 13. `application.yml` ka role

Tumhari description mein:

```text
DEPENDS ON:
eureka-server/pom.xml
application.yml
```

Ye important hai.

`EurekaServerApplication.java` mainly **Eureka functionality enable** karta hai.

Lekin actual configuration generally `application.yml` mein hoti hai.

For example conceptually:

```yaml
server:
  port: 8761
```

Iska matlab:

```text
Eureka Server
      ↓
Port 8761
```

Aur client/server configuration bhi `application.yml` mein define ki ja sakti hai.

---

# 14. `pom.xml` ka role

`pom.xml` dependencies provide karta hai.

Eureka Server ko required Spring Cloud Eureka Server dependency chahiye.

Conceptually:

```text
pom.xml
   ↓
Eureka Server dependency
   ↓
@EnableEurekaServer
   ↓
Eureka functionality
```

Agar required dependency hi nahi hai, to:

```java
@EnableEurekaServer
```

resolve nahi hoga.

---

# 15. `http://localhost:8761`

Jab application successfully start ho jaye:

```text
http://localhost:8761
```

open kar sakte ho.

Yahan Eureka dashboard available ho sakta hai.

Tum dekh sakte ho ki kaun-kaun se services registered hain.

Example:

```text
Instances currently registered with Eureka

APPLICATION          STATUS

PRODUCT-SERVICE      UP
INVENTORY-SERVICE    UP
ORDER-SERVICE        UP
PAYMENT-SERVICE      UP
```

---

# 16. `/eureka/apps`

Tumhari description mein:

```text
/eureka/apps
```

mentioned hai.

Ye Eureka ka REST endpoint hai jahan registered applications/service information available hoti hai.

Conceptually:

```text
GET /eureka/apps
       ↓
Eureka Registry information
```

---

# 17. `/actuator/health`

Testing mein:

```bash
curl -s http://localhost:8761/actuator/health
```

use kiya hai.

Ye **Eureka registration endpoint nahi**, balki Spring Boot Actuator ka health endpoint hai.

Agar application healthy hai to response conceptually:

```json
{
  "status": "UP"
}
```

iska matlab:

```text
Eureka Server application
        ↓
Running correctly
        ↓
UP
```

---

# 18. Build command

Tumhare document mein:

```bash
mvn clean package -pl eureka-server
```

Hai.

Iska purpose:

```text
common project
eureka-server
other modules
```

mein specifically `eureka-server` module ko build karna.

`-pl` ka matlab **project list / selected Maven module**.

Agar tum Maven Wrapper use kar rahe ho, jaise tumhare project mein `mvnw.cmd` hai, Windows PowerShell mein:

```powershell
.\mvnw.cmd clean package -pl eureka-server
```

use karna better hai.

---

# 19. Run command

Given:

```bash
mvn spring-boot:run -pl eureka-server
```

Windows Maven Wrapper ke saath:

```powershell
.\mvnw.cmd spring-boot:run -pl eureka-server
```

Flow:

```text
Maven
 ↓
eureka-server module
 ↓
EurekaServerApplication
 ↓
SpringApplication.run()
 ↓
Eureka Server
 ↓
Port 8761
```

---

# 20. Ye file project mein kyu required hai?

Short mein:

**`EurekaServerApplication.java` tumhare Eureka Server ko start karne wali bootstrap class hai.**

Ismein:

```java
@SpringBootApplication
```

Spring Boot application setup karta hai.

```java
@EnableEurekaServer
```

Eureka Server functionality enable karta hai.

```java
SpringApplication.run(...)
```

application ko start karta hai.

### Complete picture:

```text
EurekaServerApplication.java
            │
            ├── @SpringBootApplication
            │       ↓
            │   Spring Boot setup
            │
            ├── @EnableEurekaServer
            │       ↓
            │   Eureka Registry
            │
            └── main()
                    ↓
            SpringApplication.run()
                    ↓
             Eureka Server :8761
                    ↓
       ┌────────────┼────────────┐
       ↓            ↓            ↓
   Product      Inventory      Order
   Service       Service       Service
```

**Tumhare flash-sale project mein iska role bahut important hai because jaise-jaise microservices badhenge, services ko dynamically discover karne ke liye Eureka Service Registry use hogi.**

* * */
