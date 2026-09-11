package com.flashsale.product.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }


}
/*Bilkul. Ye `RedisConfig.java` **Product Service mein Redis ko configure karne wali configuration class** hai. Isme mainly **2 Redis templates** banaye gaye hain:

1. `StringRedisTemplate` → simple/high-frequency String operations
2. `RedisTemplate<String, Object>` → Java objects/DTOs ko Redis mein JSON ke form mein store/retrieve karna

Main **har method aur important line ko tumhare Flash Sale project ke example ke saath** explain karta hoon.

---

# 1. Overall `RedisConfig` ka purpose

Tumhare project mein Product Service ko baar-baar database se product data nahi nikalna chahiye.

Normal flow:

```text
Frontend
   ↓
Product Service
   ↓
PostgreSQL
   ↓
Product
```

High traffic flash sale mein:

```text
1000s/10000s requests
          ↓
   Product Service
          ↓
        Redis
          ↓
      Product data
```

Redis memory mein data rakhta hai, isliye frequently accessed data ke liye database ke comparison mein bahut fast hota hai.

`RedisConfig` basically Redis ko batata hai:

> **Java objects ko Redis mein kaise serialize karna hai aur Redis se wapas Java objects mein kaise convert karna hai.**

---

# 2. `@Configuration`

```java
@Configuration
public class RedisConfig {
```

`@Configuration` Spring ko batata hai:

> Is class ke andar application configuration/beans defined hain.

Spring application start hone par is class ko process karega.

For example:

```text
Spring Boot starts
       ↓
RedisConfig
       ↓
@Bean methods
       ↓
Redis templates create
```

---

# 3. First method — `stringRedisTemplate()`

Code:

```java
@Bean
public StringRedisTemplate stringRedisTemplate(
        RedisConnectionFactory connectionFactory) {

    return new StringRedisTemplate(connectionFactory);
}
```

Ye tumhari **first Redis bean** hai.

---

## `@Bean`

```java
@Bean
```

Spring ko bolta hai:

> Is method ke return object ko Spring container mein manage karo.

Iske baad application ke kisi bhi service mein:

```java
@Autowired
private StringRedisTemplate stringRedisTemplate;
```

use kiya ja sakta hai.

---

# 4. `RedisConnectionFactory connectionFactory`

```java
RedisConnectionFactory connectionFactory
```

Ye Redis ke saath actual connection create/manage karne ke liye responsible component hai.

Conceptually:

```text
Product Service
      ↓
RedisTemplate
      ↓
RedisConnectionFactory
      ↓
Redis Server
```

Tumhare `application.yml` mein Redis configuration ho sakti hai, for example:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Spring us configuration se connection factory create kar sakta hai.

---

# 5. `new StringRedisTemplate(connectionFactory)`

```java
return new StringRedisTemplate(connectionFactory);
```

Ye String-based Redis operations ke liye template create karta hai.

For example:

```java
stringRedisTemplate.opsForValue()
        .set("flash-sale:status", "ACTIVE");
```

Redis mein:

```text
flash-sale:status → ACTIVE
```

Store ho sakta hai.

Read:

```java
String status =
    stringRedisTemplate.opsForValue()
        .get("flash-sale:status");
```

Result:

```text
ACTIVE
```

---

# 6. Flash Sale mein iska real use

Suppose flash sale currently active hai.

Redis:

```text
flash-sale:123:status → ACTIVE
```

Thousands of requests:

```text
User 1 → Is sale active?
User 2 → Is sale active?
User 3 → Is sale active?
...
User 10000
```

Instead of PostgreSQL:

```text
10000 requests
      ↓
PostgreSQL
```

Redis:

```text
10000 requests
      ↓
Redis
```

use kiya ja sakta hai.

---

# 7. Second method — `redisTemplate()`

Code:

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory) {
```

Ye second Redis bean hai.

Difference:

```text
StringRedisTemplate
        ↓
String data

RedisTemplate<String, Object>
        ↓
Java Objects
```

Example:

```text
ProductResponse
OrderResponse
FlashSaleResponse
```

etc.

---

# 8. `RedisTemplate<String, Object>`

```java
RedisTemplate<String, Object>
```

Yahan:

```text
String
```

means Redis key String hogi.

Example:

```text
product:101
product:102
product:103
```

And:

```text
Object
```

means value Java ka different object ho sakta hai.

For example:

```text
ProductResponse
FlashSaleResponse
OrderResponse
```

---

# 9. `new RedisTemplate<>()`

```java
RedisTemplate<String, Object> template =
        new RedisTemplate<>();
```

Ek Redis template object create ho raha hai.

Abhi isko connection aur serializers configure karne hain.

---

# 10. `setConnectionFactory()`

```java
template.setConnectionFactory(connectionFactory);
```

Ye RedisTemplate ko Redis connection factory ke saath connect karta hai.

Flow:

```text
RedisTemplate
     ↓
RedisConnectionFactory
     ↓
Redis Server
```

Agar ye properly configured nahi hota, template Redis se communicate nahi kar paayega.

---

# 11. New `ObjectMapper`

```java
ObjectMapper mapper = new ObjectMapper();
```

Jackson ka `ObjectMapper` create ho raha hai.

Iska kaam:

```text
Java Object
     ↕
JSON
```

Example:

```java
ProductResponse product
```

convert:

```json
{
  "id": 101,
  "name": "Gaming Laptop",
  "price": 75000
}
```

---

# 12. `JavaTimeModule`

```java
mapper.registerModule(new JavaTimeModule());
```

Ye Java date/time classes ke support ke liye hai.

Tumhare project mein fields ho sakti hain:

```java
Instant createdAt;
Instant saleStartTime;
Instant saleEndTime;
```

Example:

```java
Instant saleEndTime =
    Instant.parse("2026-08-31T18:00:00Z");
```

JSON representation properly handle karne ke liye `JavaTimeModule` useful hai.

---

# 13. `WRITE_DATES_AS_TIMESTAMPS`

```java
mapper.disable(
    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
);
```

Iska purpose hai date/time ko numeric timestamp ki jagah readable ISO-style format mein serialize karna.

Conceptually:

### Timestamp style

```json
{
  "saleStartTime": 1788199200
}
```

### ISO-style

```json
{
  "saleStartTime": "2026-08-31T18:00:00Z"
}
```

Tumhare flash-sale system ke liye second format much easier to understand/debug hai.

---

# 14. `activateDefaultTyping()`

Ye thoda advanced part hai:

```java
mapper.activateDefaultTyping(
    LaissezFaireSubTypeValidator.instance,
    ObjectMapper.DefaultTyping.NON_FINAL,
    JsonTypeInfo.As.PROPERTY
);
```

Iska purpose hai **polymorphic type information** preserve karna.

Simple example:

Suppose:

```java
Object value = productResponse;
```

Redis ko serialize karte waqt sirf fields nahi, balki object ka type information bhi preserve kiya ja sakta hai.

Conceptually JSON mein type metadata aa sakta hai:

```json
{
  "@class": "com.flashsale.product.dto.ProductResponse",
  "id": 101,
  "name": "Gaming Laptop"
}
```

Later Redis se read karte waqt Jackson ko pata hota hai ki object kis type ka tha.

---

# 15. `LaissezFaireSubTypeValidator`

```java
LaissezFaireSubTypeValidator.instance
```

Ye Jackson ko subtype/type information accept karne ke liye validator provide karta hai.

Tumhare code mein iska use default typing ke saath ho raha hai.

### Important

Ye configuration **trusted internal cache data** ke scenario mein carefully use honi chahiye.

Production mein arbitrary/untrusted JSON ko deserialize karne ke liye permissive default typing avoid karna generally safer hota hai.

Tumhare project mein Redis ko internal application cache ke roop mein use karna hai, phir bhi later production hardening ke time is configuration ko review karna chahiye.

---

# 16. `DefaultTyping.NON_FINAL`

```java
ObjectMapper.DefaultTyping.NON_FINAL
```

Iska matlab roughly:

> Non-final types ke liye type information include karo.

Ye generic `Object` values ko deserialize karte waqt useful ho sakta hai.

Example:

```text
Redis
 ↓
Object
 ↓
Jackson
 ↓
Actual ProductResponse
```

---

# 17. `JsonTypeInfo.As.PROPERTY`

```java
JsonTypeInfo.As.PROPERTY
```

Type information JSON ke ek property ke form mein store karne ka configuration hai.

Conceptually:

```json
{
  "@class": "...ProductResponse",
  "id": 101,
  "name": "Laptop"
}
```

---

# 18. `GenericJackson2JsonRedisSerializer`

```java
GenericJackson2JsonRedisSerializer serializer =
        new GenericJackson2JsonRedisSerializer(mapper);
```

Ye Redis values ko JSON format mein serialize/deserialize karne ke liye hai.

Example:

Java object:

```java
ProductResponse product =
    new ProductResponse(
        101L,
        "Gaming Laptop",
        75000
    );
```

Redis mein JSON representation store ho sakti hai.

```text
product:101
      ↓
JSON representation
```

---

# 19. `StringRedisSerializer`

```java
StringRedisSerializer stringSerializer =
        new StringRedisSerializer();
```

Ye Redis keys ko String format mein serialize karta hai.

Example:

```text
product:101
```

human-readable Redis key rahegi.

Instead of binary-looking data.

---

# 20. `setKeySerializer()`

```java
template.setKeySerializer(stringSerializer);
```

Redis ke normal keys ke liye String serializer set kar raha hai.

Example:

```java
template.opsForValue()
        .set("product:101", product);
```

Key:

```text
product:101
```

---

# 21. `setHashKeySerializer()`

```java
template.setHashKeySerializer(stringSerializer);
```

Agar Redis Hash use karoge:

```text
product:101
```

ke andar fields ho sakti hain:

```text
name
price
stock
category
```

To hash keys bhi String format mein rahengi.

Example:

```text
product:101
    ├── name
    ├── price
    └── stock
```

---

# 22. `setValueSerializer()`

```java
template.setValueSerializer(serializer);
```

Ye **normal Redis values** ke serialization ke liye hai.

Example:

```java
template.opsForValue()
        .set("product:101", productResponse);
```

Yahan:

```text
Key
 ↓
"product:101"

Value
 ↓
ProductResponse
```

`GenericJackson2JsonRedisSerializer` value ko JSON representation mein convert karega.

---

# 23. `setHashValueSerializer()`

```java
template.setHashValueSerializer(serializer);
```

Agar Redis Hash ke andar values Java objects hain, unko bhi JSON serializer handle karega.

---

# 24. `afterPropertiesSet()`

```java
template.afterPropertiesSet();
```

Ye RedisTemplate ko initialization complete karne deta hai after required properties/serializers are configured.

Conceptually:

```text
Create RedisTemplate
       ↓
Set connection
       ↓
Set serializers
       ↓
afterPropertiesSet()
       ↓
Ready to use
```

---

# 25. `return template`

```java
return template;
```

Configured `RedisTemplate` Spring container ko return ho jata hai.

Ab other classes mein inject kar sakte ho:

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;
```

---

# 26. Real Product Cache Example

Suppose database mein product hai:

```text
ID: 101
Name: Gaming Laptop
Price: ₹75,000
Stock: 100
```

Product service:

```text
Request
   ↓
ProductService
   ↓
Redis?
```

### Cache miss

```text
Redis
 ↓
product:101 doesn't exist
 ↓
PostgreSQL
 ↓
Product found
 ↓
Redis
 ↓
product:101 cached
```

### Next request

```text
Request
   ↓
ProductService
   ↓
Redis
   ↓
product:101
   ↓
Response
```

PostgreSQL hit avoid ho gaya.

---

# 27. Why two templates?

Tumhare code mein:

```text
StringRedisTemplate
```

aur:

```text
RedisTemplate<String,Object>
```

dono hain.

### `StringRedisTemplate`

Simple String operations:

```text
flash-sale:status → ACTIVE
flash-sale:123:lock → ...
```

### `RedisTemplate<String,Object>`

Complex Java objects:

```text
product:101 → ProductResponse
```

So:

```text
              Redis
                │
        ┌───────┴────────┐
        ↓                ↓
StringRedisTemplate   RedisTemplate
        ↓                ↓
    String data      Object/JSON data
```

---

# 28. Tumhare Flash Sale project mein iska importance

Normal e-commerce:

```text
Request
 ↓
PostgreSQL
```

High-throughput flash sale:

```text
                 Requests
                    │
          ┌─────────┴─────────┐
          ↓                   ↓
       Product              Inventory
          │                   │
          ↓                   ↓
        Redis               Redis
          │                   │
          ↓                   ↓
      PostgreSQL           PostgreSQL
```

Redis frequently accessed data ka load reduce karne mein help karta hai.

**Lekin important:** ye `RedisConfig` khud inventory reservation/concurrency solve nahi karta. Ye sirf Redis connectivity, templates aur serialization configure karta hai. Actual stock reservation logic `InventoryService`/reservation layer mein hoga.

---

# 29. Har important part ka short summary

| Code                                 | Purpose                                    |
| ------------------------------------ | ------------------------------------------ |
| `@Configuration`                     | Spring configuration class                 |
| `@Bean`                              | Spring-managed object create karta hai     |
| `StringRedisTemplate`                | String-based Redis operations              |
| `RedisTemplate<String,Object>`       | Java objects ke saath Redis operations     |
| `RedisConnectionFactory`             | Redis connection management                |
| `ObjectMapper`                       | Java ↔ JSON conversion                     |
| `JavaTimeModule`                     | `Instant`, `LocalDateTime`, etc. support   |
| `WRITE_DATES_AS_TIMESTAMPS` disabled | Dates readable ISO format mein             |
| `activateDefaultTyping()`            | Type metadata preserve karna               |
| `GenericJackson2JsonRedisSerializer` | Objects ↔ JSON for Redis                   |
| `StringRedisSerializer`              | Human-readable String keys                 |
| `afterPropertiesSet()`               | Template initialization complete           |
| `return template`                    | Configured RedisTemplate provide karta hai |

### Ek line mein:

**`RedisConfig.java` = Product Service ko Redis ke saath properly connect karna + keys ko readable rakhna + Java objects ko JSON mein safely store/retrieve karne ke liye Redis templates configure karna.**
*/