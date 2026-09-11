package com.flashsale.auth.config;

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

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
/*
* Bilkul. `RedisConfig.java` ko tumhare **Flash Sale System + Auth Service** ke context mein Hinglish mein, har part aur method ke example ke saath samjho.

---

# 1. `RedisConfig.java` ki overall need kya hai?

Tumhare Auth Service mein Redis ka use mainly fast temporary data ke liye hoga, jaise:

```text
Refresh Tokens
Token Blacklist
Session-related data
Temporary authentication data
```

Flow:

```text
Auth Service
     ↓
 RedisConfig
     ↓
 RedisConnectionFactory
     ↓
    Redis
```

`RedisConfig` basically Spring ko batata hai:

> **Redis se connect kaise karna hai aur Java objects/strings ko Redis mein kis format mein store/read karna hai.**

---

# 2. `@Configuration`

```java
@Configuration
public class RedisConfig {
```

`@Configuration` Spring ko batata hai:

> Is class mein application configuration/beans defined hain.

Spring startup par is class ko read karega.

Example:

```text
Spring Boot Start
      ↓
RedisConfig detect
      ↓
@Bean methods execute
      ↓
RedisTemplate available
```

---

# 3. `stringRedisTemplate()`

Code:

```java
@Bean
public StringRedisTemplate stringRedisTemplate(
        RedisConnectionFactory connectionFactory) {

    return new StringRedisTemplate(connectionFactory);
}
```

Ye class ki **first important method** hai.

### Iska purpose

`StringRedisTemplate` Redis mein primarily **String → String** type data ke liye convenient interface hai.

Tumhare project mein example:

```text
blacklist:{jti} → true
```

ya:

```text
refresh_token:abc123 → user-101
```

---

## `@Bean`

```java
@Bean
```

Spring ko bolta hai:

> Is method ke returned object ko Spring container mein manage karo.

Isliye application ke kisi aur class mein:

```java
@Autowired
private StringRedisTemplate redisTemplate;
```

ya constructor injection ke through use kar sakte ho.

---

# 4. `RedisConnectionFactory connectionFactory`

```java
public StringRedisTemplate stringRedisTemplate(
        RedisConnectionFactory connectionFactory)
```

Yahan Spring automatically `RedisConnectionFactory` provide karega.

Ye basically Redis connection ka management layer hai.

Conceptually:

```text
StringRedisTemplate
       ↓
RedisConnectionFactory
       ↓
Redis Server
```

Tumhe manually har operation ke liye connection create nahi karna padta.

---

# 5. Real example — Token blacklist

Suppose user logout karta hai.

JWT:

```text
eyJhbGciOiJIUzI1Ni...
```

JWT ka JTI:

```text
abc-123
```

Tum Redis mein store kar sakte ho:

```text
Key:
blacklist:abc-123

Value:
true
```

Code conceptually:

```java
stringRedisTemplate.opsForValue()
        .set("blacklist:abc-123", "true");
```

Later request:

```text
JWT
 ↓
JTI = abc-123
 ↓
Redis check
 ↓
blacklist:abc-123 exists?
 ↓
YES
 ↓
Reject request
```

Redis fast hai, isliye har request ke liye database hit karne ki zarurat nahi padti.

---

# 6. `redisTemplate()`

Second major method:

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory) {
```

Ye more general-purpose Redis template hai.

Difference:

```text
StringRedisTemplate
        ↓
String based data

RedisTemplate<String, Object>
        ↓
String keys + complex Java objects
```

---

# 7. `new RedisTemplate<>()`

```java
RedisTemplate<String, Object> template =
        new RedisTemplate<>();
```

Yahan RedisTemplate create ho raha hai.

Generic types:

```text
String → Key
Object → Value
```

Matlab:

```text
Key = String
Value = Java Object
```

Example:

```text
Key:
session:user:101

Value:
UserSession object
```

---

# 8. `setConnectionFactory()`

```java
template.setConnectionFactory(connectionFactory);
```

Isse RedisTemplate ko bataya:

> Redis server ke saath connection kis factory ke through banana hai.

Without proper connection factory, template Redis se communicate nahi kar payega.

Flow:

```text
RedisTemplate
     ↓
ConnectionFactory
     ↓
Redis
```

---

# 9. `StringRedisSerializer`

```java
StringRedisSerializer stringSerializer =
        new StringRedisSerializer();
```

Ye Redis keys ko String format mein serialize karta hai.

### Serialization kya hai?

Java/String data ko Redis-compatible representation mein convert karna.

Example:

```text
Java:
"blacklist:abc123"

        ↓

Redis:
blacklist:abc123
```

Iska benefit hai ki Redis keys readable rahengi.

---

# 10. `GenericJackson2JsonRedisSerializer`

```java
GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer();
```

Ye complex Java objects ko JSON-based representation mein serialize karne ke liye use hota hai.

Example Java object:

```java
UserSession session
```

Conceptually Redis mein:

```json
{
  "userId": 101,
  "username": "jatin",
  "role": "USER"
}
```

store/read kiya ja sakta hai.

---

# 11. `setKeySerializer()`

```java
template.setKeySerializer(stringSerializer);
```

Ye define karta hai:

> Redis keys ko String serializer se handle karo.

Example:

```text
refresh_token:abc123
```

Readable form mein rahega.

---

# 12. `setHashKeySerializer()`

```java
template.setHashKeySerializer(stringSerializer);
```

Agar Redis Hash use kar rahe ho, to uske individual field names bhi String format mein serialize honge.

Example Redis Hash:

```text
user:101

name → Jatin
role → USER
status → ACTIVE
```

Yahan:

```text
name
role
status
```

hash keys hain.

---

# 13. `setValueSerializer()`

```java
template.setValueSerializer(jsonSerializer);
```

Ye normal Redis values ke liye serializer set karta hai.

Example:

```text
Key:
session:101

Value:
UserSession object
```

Value JSON representation mein serialize hogi.

---

# 14. `setHashValueSerializer()`

```java
template.setHashValueSerializer(jsonSerializer);
```

Redis Hash ke values ke liye JSON serializer use hoga.

Example:

```text
Hash:
user:101

profile → UserProfile object
preferences → Preference object
```

Complex Java objects ko JSON format mein handle kiya ja sakta hai.

---

# 15. `afterPropertiesSet()`

```java
template.afterPropertiesSet();
```

Ye Spring's initialization lifecycle ka part hai.

Simple language mein:

> RedisTemplate ko configured properties ke saath initialize/prepare karo.

Tumne pehle:

```text
Connection Factory
Key Serializer
Value Serializer
Hash Key Serializer
Hash Value Serializer
```

set kiya.

Ab:

```java
template.afterPropertiesSet();
```

configuration complete hone ke baad template ko initialize karta hai.

---

# 16. `return template`

```java
return template;
```

Configured RedisTemplate Spring ko return kar diya.

Because method par:

```java
@Bean
```

hai, Spring ise application context mein register kar dega.

Ab other services/classes ise inject kar sakti hain.

---

# 17. `StringRedisTemplate` vs `RedisTemplate`

Ye difference interview mein bhi important hai.

| Feature         | StringRedisTemplate             | RedisTemplate                      |
| --------------- | ------------------------------- | ---------------------------------- |
| Key             | String                          | String                             |
| Value           | String                          | Object                             |
| Complex object  | Limited/manual                  | Yes                                |
| JSON serializer | Usually unnecessary for strings | Useful                             |
| Use case        | Tokens, flags, simple values    | Sessions, objects, structured data |

### Example

**StringRedisTemplate:**

```text
blacklist:jwt123 → true
refresh:abc → user101
```

**RedisTemplate:**

```text
session:101 → UserSession object
```

---

# 18. Tumhare Auth Service mein actual flow

### Login

```text
User
 ↓
Auth Controller
 ↓
Auth Service
 ↓
JWT + Refresh Token
 ↓
Redis
```

Refresh token ke liye:

```text
refresh_token:abc123
        ↓
     userId=101
```

---

### Logout

```text
User Logout
    ↓
JWT JTI
    ↓
Redis
    ↓
blacklist:jti
    ↓
true
```

---

### Future request

```text
Request
  ↓
JWT
  ↓
Extract JTI
  ↓
Redis check
  ↓
blacklist:jti?
  │
  ├── YES → Reject
  │
  └── NO → Continue
```

---

# 19. Ek important correction in description

Tumhare provided description mein likha hai:

> "Establish clean connection pooling via Lettuce."

**Is `RedisConfig.java` ke code mein explicitly connection pooling configure nahi ki gayi hai.**

Ye code:

```java
RedisConnectionFactory connectionFactory
```

ko receive karta hai, lekin pool configuration yahan directly nahi karta.

Spring Boot ka Redis setup/application configuration Lettuce connection handling provide kar sakta hai, depending on dependencies and configuration, but **is particular class mein explicit pooling configuration nahi hai**.

So interview mein ye mat bolna:

> "`RedisConfig` explicitly configures Lettuce connection pooling."

Better bolo:

> **"`RedisConfig` configures the Redis templates and serializers using Spring's `RedisConnectionFactory`; the underlying connection implementation/configuration is provided separately by Spring Boot/Redis configuration."**

---

# 20. Overall `RedisConfig.java`

Simple flow yaad rakho:

```text
                 RedisConfig
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
StringRedisTemplate       RedisTemplate
          │                     │
     String data          Object data
          │                     │
          └──────────┬──────────┘
                     ▼
           RedisConnectionFactory
                     │
                     ▼
                   Redis
```

### Tumhare project mein:

```text
Auth Service
    │
    ├── Refresh Token
    │       ↓
    │   RedisTemplate
    │
    ├── JWT Blacklist
    │       ↓
    │   StringRedisTemplate
    │
    └── Session/Object data
            ↓
        RedisTemplate
```

**Short mein:** `RedisConfig.java` ka main kaam Redis ki connection factory ko use karke properly configured `StringRedisTemplate` aur `RedisTemplate` provide karna hai, taaki Auth Service refresh tokens, blacklist entries aur structured temporary data ko Redis mein efficiently store/read kar sake.
*/
