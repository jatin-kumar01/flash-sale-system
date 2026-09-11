package com.flashsale.gateway.config;

import com.flashsale.common.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token received at gateway: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get(SecurityConstants.CLAIM_USER_ID);
        return userIdObj != null ? String.valueOf(userIdObj) : claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get(SecurityConstants.CLAIM_ROLES);
        if (rolesObj instanceof List<?>) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }
}
/*
* Bilkul. Is `JwtUtil.java` ko **tumhare Flash Sale microservices project ke context mein**, har method ka example ke saath Hinglish mein samjho.

# `JwtUtil.java` kya hai?

`JwtUtil` ka main kaam hai **JWT token ko read, verify aur uske andar se user information nikalna**.

Tumhare architecture mein ye **API Gateway ke edge par** kaam karega.

```text
Client
  │
  │ JWT Token
  ▼
API Gateway
  │
  ▼
JwtAuthenticationFilter
  │
  ▼
JwtUtil
  │
  ├── Token valid?
  ├── Token expired?
  ├── Signature correct?
  ├── User ID?
  └── Roles?
  │
  ▼
Downstream Microservice
```

---

# 1. `@Value`

```java
@Value("${jwt.secret:404E635266...}")
private String secret;
```

Iska kaam hai **JWT secret key ko configuration se read karna**.

Normally `application.yml` mein:

```yaml
jwt:
  secret: your-secret-key
```

aur Java mein:

```java
@Value("${jwt.secret}")
```

se value mil jayegi.

### `:` ke baad jo value hai?

```text
${jwt.secret:DEFAULT_VALUE}
```

iska matlab:

> Agar `jwt.secret` configuration mein nahi mila, to default value use karo.

---

# 2. `signingKey`

```java
private SecretKey signingKey;
```

Ye actual cryptographic key hai jo JWT signature verify karne ke liye use hogi.

Simple:

```text
JWT Token
   +
Secret Key
   ↓
Signature Verification
```

---

# 3. `init()`

```java
@PostConstruct
public void init() {
    byte[] keyBytes =
        secret.getBytes(StandardCharsets.UTF_8);

    this.signingKey =
        Keys.hmacShaKeyFor(keyBytes);
}
```

Ye method application start hone ke baad automatically execute hoti hai.

### Step 1

```java
secret.getBytes(StandardCharsets.UTF_8);
```

Secret string ko bytes mein convert karta hai.

```text
"my-secret"
     ↓
byte[]
```

### Step 2

```java
Keys.hmacShaKeyFor(keyBytes);
```

Bytes se HMAC-SHA compatible `SecretKey` create hoti hai.

Result:

```text
secret
  ↓
byte[]
  ↓
SecretKey
  ↓
JWT verification
```

### Important

Tumhare code mein secret ko directly source code mein hard-code kiya gaya hai as a fallback.

**Production mein secret ko source code mein rakhna recommended nahi hai.** Environment/secret management use karna better hai.

---

# 4. `validateToken()`

```java
public boolean validateToken(String token)
```

Ye method simply answer deti hai:

> **JWT valid hai ya nahi?**

Return:

```text
true  → valid
false → invalid
```

---

## Iske andar

```java
Claims claims = extractAllClaims(token);
```

Pehle JWT ko parse aur verify karta hai.

Agar token:

```text
signature invalid
```

hai, exception aa sakti hai.

---

## Expiration check

```java
return !claims.getExpiration().before(new Date());
```

JWT mein expiration time hota hai.

Example:

```text
Current time = 8:00 PM
Token expiry = 8:30 PM
```

Token valid:

```text
8:30 PM > 8:00 PM
```

So:

```text
true
```

Agar:

```text
Current time = 9:00 PM
Token expiry = 8:30 PM
```

to token expired hai:

```text
false
```

---

## Exception handling

```java
catch (JwtException | IllegalArgumentException e)
```

Agar JWT invalid hai, malformed hai, signature wrong hai etc., method exception ko handle karke:

```java
return false;
```

karti hai.

Gateway log karega:

```java
log.warn(
    "Invalid JWT token received at gateway: {}",
    e.getMessage()
);
```

### Real example

Client:

```http
Authorization: Bearer abc.xyz.invalid
```

Gateway:

```text
JwtUtil.validateToken()
        ↓
Invalid signature
        ↓
false
        ↓
Request reject
```

---

# 5. `extractAllClaims()`

```java
public Claims extractAllClaims(String token)
```

Ye JWT ke andar ke **saare claims** nikalti hai.

JWT conceptually:

```text
HEADER.PAYLOAD.SIGNATURE
```

Payload mein claims ho sakte hain:

```json
{
  "sub": "123",
  "userId": "123",
  "email": "jatin@example.com",
  "roles": ["USER"],
  "exp": 1788000000
}
```

`extractAllClaims()` in claims ko Java `Claims` object mein convert karta hai.

---

## `Jwts.parser()`

```java
Jwts.parser()
```

JWT parser create karta hai.

---

## `verifyWith(signingKey)`

```java
.verifyWith(signingKey)
```

Ye bahut important hai.

Gateway JWT ki signature ko **secret key ke saath verify** karega.

Agar attacker JWT payload modify kar de:

```text
role = USER
```

ko:

```text
role = ADMIN
```

kar de, signature match nahi karegi.

Result:

```text
Invalid JWT
```

---

## `build()`

```java
.build()
```

Configured JWT parser create karta hai.

---

## `parseSignedClaims(token)`

```java
.parseSignedClaims(token)
```

JWT ko parse karta hai aur signed claims ko verify karta hai.

Finally:

```java
.getPayload()
```

actual claims return karta hai.

Overall:

```text
JWT
 ↓
parse
 ↓
signature verify
 ↓
claims extract
 ↓
Claims object
```

---

# 6. `extractUserId()`

```java
public String extractUserId(String token)
```

Iska kaam JWT se **user ID nikalna** hai.

---

## Pehle claims

```java
Claims claims = extractAllClaims(token);
```

Suppose JWT payload:

```json
{
  "userId": "101",
  "email": "user@example.com",
  "roles": ["USER"]
}
```

---

## User ID claim

```java
Object userIdObj =
    claims.get(SecurityConstants.CLAIM_USER_ID);
```

Agar:

```java
CLAIM_USER_ID = "userId"
```

hai, to:

```text
userIdObj = 101
```

---

## `String.valueOf()`

```java
return userIdObj != null
        ? String.valueOf(userIdObj)
        : claims.getSubject();
```

Agar `userId` available hai:

```text
userId = 101
```

return:

```text
"101"
```

Agar `userId` claim nahi hai, to fallback:

```java
claims.getSubject()
```

use hoga.

JWT ka standard `sub` claim generally identity represent kar sakta hai.

Example:

```json
{
  "sub": "101"
}
```

Then:

```text
extractUserId(token)
        ↓
"101"
```

---

# 7. `extractRoles()`

```java
public List<String> extractRoles(String token)
```

Ye JWT se user's **roles** nikalta hai.

Example JWT:

```json
{
  "userId": "101",
  "roles": [
    "USER",
    "SELLER"
  ]
}
```

Method:

```java
Claims claims = extractAllClaims(token);
```

claims retrieve karega.

Then:

```java
Object rolesObj =
    claims.get(SecurityConstants.CLAIM_ROLES);
```

Suppose:

```text
rolesObj
    ↓
["USER", "SELLER"]
```

---

## Check

```java
if (rolesObj instanceof List<?>)
```

Ye check karta hai ki roles actually List hai ya nahi.

Agar List hai:

```java
return (List<String>) rolesObj;
```

return:

```text
["USER", "SELLER"]
```

---

## Agar roles nahi hain

```java
return List.of();
```

Empty list return hogi:

```text
[]
```

Isse `null` handling ki zarurat kam hoti hai.

---

# 8. `@SuppressWarnings("unchecked")`

```java
@SuppressWarnings("unchecked")
```

Ye compiler ko bolta hai:

> Mujhe pata hai yahan unchecked generic cast ho raha hai; warning suppress karo.

Because:

```java
(List<String>) rolesObj
```

runtime par generic type ko fully verify nahi kar sakta.

---

# 9. Complete real example

Suppose user login karta hai.

Auth Service JWT create karta hai:

```json
{
  "sub": "101",
  "userId": "101",
  "email": "user@example.com",
  "roles": ["USER"],
  "exp": 1788000000
}
```

Client request:

```http
GET /api/orders
Authorization: Bearer <JWT>
```

Gateway:

```text
Request
  ↓
JwtAuthenticationFilter
  ↓
JwtUtil.validateToken()
  ↓
extractAllClaims()
  ↓
Signature verify
  ↓
Expiration check
  ↓
TRUE
```

Then:

```java
extractUserId(token)
```

returns:

```text
101
```

and:

```java
extractRoles(token)
```

returns:

```text
["USER"]
```

Gateway downstream request mein context headers propagate kar sakta hai, depending on your filter implementation:

```http
X-User-Id: 101
X-User-Roles: USER
```

Then:

```text
API Gateway
     ↓
Order Service
```

Order Service ko user context mil jata hai.

---

# 10. Invalid JWT example

Attacker token modify karta hai:

```json
{
  "userId": "101",
  "roles": ["ADMIN"]
}
```

without having the correct signing secret.

Gateway:

```text
JWT
 ↓
verifyWith(signingKey)
 ↓
Signature doesn't match
 ↓
JwtException
 ↓
validateToken() = false
 ↓
Request rejected
```

Isliye attacker sirf payload change karke ADMIN nahi ban sakta.

---

# 11. Har method ka short purpose

| Method               | Kaam                                        |
| -------------------- | ------------------------------------------- |
| `init()`             | Secret se `SecretKey` prepare karta hai     |
| `validateToken()`    | JWT valid/expired/signature check karta hai |
| `extractAllClaims()` | JWT ke saare claims nikalta hai             |
| `extractUserId()`    | User ID nikalta hai                         |
| `extractRoles()`     | User roles nikalta hai                      |

Aur overall:

```text
                    JwtUtil
                       │
             ┌─────────┴─────────┐
             │                   │
          Validate             Extract
             │                   │
             ▼             ┌─────┴─────┐
       JWT valid?           │           │
       Expired?           User ID     Roles
       Signature?
```

### Tumhare architecture mein iska role

`JwtUtil` **business logic nahi karta**. Ye Gateway ka security helper hai.

```text
Client
  ↓
API Gateway
  ↓
JwtAuthenticationFilter
  ↓
JwtUtil
  ↓
JWT verify + claims
  ↓
Request allowed/rejected
  ↓
Microservice
```

**Simple words mein:** `JwtUtil` Gateway ka **JWT security checker + information extractor** hai. Ye decide karne mein help karta hai ki incoming token cryptographically valid hai ya nahi aur valid hone par usmein se `userId` aur `roles` nikal sakta hai.
*/
