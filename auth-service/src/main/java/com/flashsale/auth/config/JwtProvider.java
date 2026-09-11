package com.flashsale.auth.config;

import com.flashsale.auth.entity.User;
import com.flashsale.common.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;

    @Getter
    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Getter
    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(SecurityConstants.CLAIM_USER_ID, user.getId())
                .claim(SecurityConstants.CLAIM_EMAIL, user.getEmail())
                .claim(SecurityConstants.CLAIM_ROLES, new ArrayList<>(user.getRoles()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
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

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get(SecurityConstants.CLAIM_USER_ID);
        if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        } else if (userIdObj instanceof String) {
            return Long.parseLong((String) userIdObj);
        }
        return null;
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

/*Bilkul. `JwtProvider.java` tumhare **auth-service ka core security component** hai. Iska main kaam hai **JWT access token banana, validate karna, aur token ke andar se user ki information nikalna**.

Tumhare Flash Sale System ke context mein isko method-by-method samjho.

---

# 1. `JwtProvider` overall kya karta hai?

Login ke time flow kuch aisa hoga:

```text
User Login
   ↓
Auth Service
   ↓
User verify
   ↓
JwtProvider
   ↓
JWT Access Token
   ↓
Frontend
```

Baad mein user kisi protected API ko call kare:

```text
Frontend
   ↓
Authorization: Bearer <JWT>
   ↓
API Gateway
   ↓
JwtProvider / JWT validation
   ↓
Token valid?
   ↓
Allow / Reject
```

JWT ke andar roughly ye information hogi:

```text
User ID
Email
Roles
Issued Time
Expiration Time
Signature
```

---

# 2. `@Component`

```java
@Component
public class JwtProvider {
```

`@Component` ka matlab Spring is class ka object automatically create karega.

Isliye tum manually:

```java
new JwtProvider()
```

nahi karoge.

Spring automatically object provide karega:

```text
Spring Container
      ↓
JwtProvider Object
      ↓
AuthService
```

---

# 3. JWT Secret

```java
@Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
private String secret;
```

Ye JWT sign karne ke liye secret key ka source hai.

Simple language mein:

> Ye secret decide karta hai ki JWT genuine hai ya tampered.

Example:

```text
Secret Key
    ↓
JWT + Secret
    ↓
Signature
```

Agar attacker JWT mein:

```text
role = USER
```

ko:

```text
role = ADMIN
```

karega, signature match nahi karega.

### Important

Ye default secret development/testing ke liye hai. **Production mein secret ko source code mein hard-code nahi karna chahiye.**

---

# 4. Access-token expiration

```java
@Getter
@Value("${jwt.access-token-expiration-ms:900000}")
private long accessTokenExpirationMs;
```

`900000 ms` = **15 minutes**.

Matlab access token approximately 15 minutes valid hai.

```text
Login
 ↓
Access Token
 ↓
15 minutes
 ↓
Expired
```

Iska benefit:

Agar access token accidentally leak ho jaye, to woh permanently valid nahi rahega.

---

# 5. Refresh-token expiration

```java
@Getter
@Value("${jwt.refresh-token-expiration-ms:604800000}")
private long refreshTokenExpirationMs;
```

`604800000 ms` = **7 days**.

Refresh token longer time ke liye valid conceptually hai.

Flow:

```text
Access Token
15 min
   ↓
Expired
   ↓
Refresh Token
   ↓
New Access Token
```

---

# 6. `signingKey`

```java
private SecretKey signingKey;
```

Ye actual cryptographic key store karega jisse JWT sign/verify hoga.

Initially:

```text
signingKey = null
```

Phir `init()` mein initialize hota hai.

---

# 7. `init()`

```java
@PostConstruct
public void init() {
```

`@PostConstruct` ka matlab:

> Spring ke object create karne ke baad ye method automatically ek baar execute karega.

Flow:

```text
Spring starts
   ↓
JwtProvider object created
   ↓
@PostConstruct
   ↓
init()
   ↓
signingKey ready
```

---

## Key bytes

```java
byte[] keyBytes =
    secret.getBytes(StandardCharsets.UTF_8);
```

Secret:

```text
"404E635266..."
```

ko bytes mein convert kar raha hai.

---

## HMAC key

```java
this.signingKey =
    Keys.hmacShaKeyFor(keyBytes);
```

Ye secret se cryptographic HMAC-SHA key create karta hai.

Ab:

```text
signingKey
```

JWT generate aur verify karne ke liye ready hai.

---

# 8. `generateAccessToken(User user)`

Ye class ka **most important method** hai.

```java
public String generateAccessToken(User user)
```

Iska purpose:

```text
User
 ↓
JWT Access Token
```

---

## Example

Suppose database mein user:

```text
id = 101
email = jatin@gmail.com
roles = [USER]
```

AuthService call karega:

```java
String token =
    jwtProvider.generateAccessToken(user);
```

Result:

```text
eyJhbGciOiJIUzI1NiJ9...
```

Ye JWT string hai.

---

# 9. Current time

```java
Date now = new Date();
```

Current time store ho raha hai.

Example:

```text
2026-08-30 20:00:00
```

---

# 10. Expiry date

```java
Date expiryDate =
    new Date(
        now.getTime() + accessTokenExpirationMs
    );
```

Agar:

```text
now = 20:00
expiration = 15 minutes
```

to:

```text
expiry = 20:15
```

---

# 11. JWT Builder

```java
return Jwts.builder()
```

JWT banana start ho raha hai.

---

# 12. Subject

```java
.subject(user.getEmail())
```

JWT ka subject email set kiya ja raha hai.

Example:

```text
subject = jatin@gmail.com
```

Baad mein:

```java
extractEmail(token)
```

isi subject ko read karega.

---

# 13. User ID claim

```java
.claim(
    SecurityConstants.CLAIM_USER_ID,
    user.getId()
)
```

JWT mein user ID add ho rahi hai.

Example:

```json
{
  "userId": 101
}
```

Actual claim key `SecurityConstants` decide karega.

---

# 14. Email claim

```java
.claim(
    SecurityConstants.CLAIM_EMAIL,
    user.getEmail()
)
```

Email ko bhi claim mein store karta hai.

Example:

```json
{
  "email": "jatin@gmail.com"
}
```

Technically subject mein bhi email hai, lekin explicit email claim consistency ke liye rakha gaya hai.

---

# 15. Roles claim

```java
.claim(
    SecurityConstants.CLAIM_ROLES,
    new ArrayList<>(user.getRoles())
)
```

User ke roles JWT mein add ho rahe hain.

Example:

```text
roles = [USER]
```

Admin:

```text
roles = [ADMIN]
```

Multiple roles:

```text
roles = [USER, SELLER]
```

JWT conceptually:

```json
{
  "userId": 101,
  "email": "jatin@gmail.com",
  "roles": ["USER"]
}
```

---

# 16. `issuedAt`

```java
.issuedAt(now)
```

Token kab create hua, woh store karta hai.

Example:

```text
issuedAt = 2026-08-30 20:00
```

---

# 17. `expiration`

```java
.expiration(expiryDate)
```

Token kab expire hoga, woh store karta hai.

Example:

```text
expiration = 2026-08-30 20:15
```

---

# 18. `signWith(signingKey)`

```java
.signWith(signingKey)
```

Ye **bahut important security step** hai.

JWT ko secret key se digitally sign karta hai.

```text
Header
Payload
   +
Secret Key
   ↓
Signature
```

Agar JWT ke payload mein koi change kare:

```text
USER → ADMIN
```

to signature invalid ho jayega.

---

# 19. `compact()`

```java
.compact();
```

Finally JWT ko String format mein convert karta hai.

Result:

```text
xxxxx.yyyyy.zzzzz
```

JWT ke 3 main parts:

```text
Header.Payload.Signature
```

---

# 20. `generateRefreshToken()`

```java
public String generateRefreshToken() {
    return UUID.randomUUID()
            .toString()
            .replace("-", "");
}
```

Ye access JWT nahi banata.

Ye ek random refresh-token string generate karta hai.

Example:

```text
a7f31c8e9b214...
```

UUID:

```text
550e8400-e29b-41d4-a716-446655440000
```

`replace("-", "")` ke baad:

```text
550e8400e29b41d4a716446655440000
```

### Iska purpose

Access token short-lived hai:

```text
15 minutes
```

Refresh token longer-lived concept ke liye hai:

```text
7 days
```

Tumhare architecture mein refresh token ko **Redis/stateful storage** mein track karne ka plan hai.

---

# 21. `validateToken()`

```java
public boolean validateToken(String token)
```

Iska kaam:

> Check karna ki JWT valid hai ya nahi.

Result:

```text
true
```

ya:

```text
false
```

---

## Step 1

```java
Claims claims = extractAllClaims(token);
```

JWT ko parse karta hai aur claims nikalta hai.

Agar token:

```text
valid signature
```

hai to claims milenge.

Agar token tampered hai:

```text
exception
```

aa sakti hai.

---

## Step 2

```java
return !claims.getExpiration()
        .before(new Date());
```

Check karta hai:

```text
Expiration < Current Time?
```

Agar expired:

```text
false
```

Agar future mein expire hoga:

```text
true
```

Example:

```text
Current = 20:10
Expiry  = 20:15

20:15 before 20:10? ❌

!false = true
```

Token valid.

---

# 22. `catch`

```java
catch (
    JwtException |
    IllegalArgumentException e
)
```

Agar token:

* malformed
* invalid signature
* expired/invalid JWT processing
* invalid argument

etc. ki wajah se parse nahi ho sakta, exception handle hoti hai.

---

## Logging

```java
log.warn(
    "Invalid JWT token: {}",
    e.getMessage()
);
```

Backend log mein warning.

---

## Return false

```java
return false;
```

Frontend/API request ko token invalid maana jayega.

---

# 23. `extractAllClaims()`

```java
public Claims extractAllClaims(String token)
```

Ye JWT ke andar ka payload/claims nikalta hai.

Example token:

```text
Header.Payload.Signature
```

Ye method payload ko read karta hai.

---

## Parser

```java
return Jwts.parser()
```

JWT parser create karta hai.

---

## Verify signature

```java
.verifyWith(signingKey)
```

Ye check karta hai ki JWT same signing key se validly signed hai ya nahi.

Ye **bahut important** hai.

Sirf JWT ko decode karna enough nahi hai.

Verify bhi karna zaroori hai.

---

## Build

```java
.build()
```

Parser ready karta hai.

---

## Parse signed claims

```java
.parseSignedClaims(token)
```

Signed JWT ko parse karta hai.

---

## Payload

```java
.getPayload();
```

Actual claims return karta hai.

Example:

```text
Claims
├── userId = 101
├── email = jatin@gmail.com
├── roles = [USER]
├── iat = ...
└── exp = ...
```

---

# 24. `extractEmail()`

```java
public String extractEmail(String token) {
    return extractAllClaims(token).getSubject();
}
```

JWT ka subject return karta hai.

Tumne token generate karte waqt:

```java
.subject(user.getEmail())
```

set kiya tha.

So:

```text
JWT
 ↓
subject
 ↓
email
```

Example:

```text
token → jatin@gmail.com
```

---

# 25. `extractUserId()`

```java
public Long extractUserId(String token)
```

JWT se user ID nikalta hai.

First:

```java
Claims claims =
    extractAllClaims(token);
```

Claims mil gaye.

Then:

```java
Object userIdObj =
    claims.get(
        SecurityConstants.CLAIM_USER_ID
    );
```

User ID retrieve karta hai.

---

## If Number

```java
if (userIdObj instanceof Number) {
    return ((Number) userIdObj).longValue();
}
```

Suppose Jackson/JJWT ne:

```text
101
```

ko Number ke form mein return kiya.

To:

```text
101 → Long 101
```

---

## If String

```java
else if (userIdObj instanceof String) {
    return Long.parseLong(
        (String) userIdObj
    );
}
```

Agar claim:

```text
"101"
```

string ke form mein aaya:

```text
"101"
 ↓
Long.parseLong()
 ↓
101L
```

---

## Otherwise

```java
return null;
```

Agar user ID available nahi hai ya unexpected type hai.

---

# 26. `extractRoles()`

```java
public List<String> extractRoles(String token)
```

JWT se roles nikalta hai.

Example:

```text
JWT
 ↓
roles
 ↓
[USER, SELLER]
```

---

## Claims

```java
Claims claims =
    extractAllClaims(token);
```

Token parse hota hai.

---

## Roles object

```java
Object rolesObj =
    claims.get(
        SecurityConstants.CLAIM_ROLES
    );
```

Roles retrieve hote hain.

---

## Check List

```java
if (rolesObj instanceof List<?>) {
```

Check karta hai ki retrieved object List hai ya nahi.

Example:

```text
[USER, SELLER]
```

---

## Convert

```java
return (List<String>) rolesObj;
```

List ko `List<String>` ke form mein return karta hai.

---

## No roles

```java
return List.of();
```

Agar roles nahi mile:

```text
[]
```

empty list return hogi.

---

# 27. Complete login example

Ab sabko ek saath connect karo.

Suppose user:

```text
ID = 101
Email = user@gmail.com
Role = USER
```

Login:

```text
POST /auth/login
        ↓
AuthService
        ↓
Database se User verify
        ↓
JwtProvider.generateAccessToken(user)
        ↓
JWT create
```

JWT claims:

```json
{
  "sub": "user@gmail.com",
  "userId": 101,
  "email": "user@gmail.com",
  "roles": ["USER"],
  "iat": "...",
  "exp": "..."
}
```

Frontend ko:

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "a7f31c..."
}
```

mil sakta hai.

---

# 28. Protected Flash-Sale request

User:

```text
Buy Laptop
```

Frontend:

```http
Authorization: Bearer eyJhbGci...
```

bhejta hai.

Then:

```text
Frontend
   ↓
API Gateway
   ↓
JWT validate
   ↓
extractUserId()
   ↓
User ID = 101
   ↓
Order/Inventory Service
```

Agar token invalid:

```text
JWT
 ↓
validateToken()
 ↓
false
 ↓
401 Unauthorized
```

---

# 29. Access vs Refresh Token

Tumhare code mein important distinction:

```text
ACCESS TOKEN
────────────
JWT
Signed
Contains user claims
Short-lived
~15 minutes
Used for API authorization
```

versus:

```text
REFRESH TOKEN
─────────────
Random UUID string
Longer-lived
~7 days conceptually
Can be tracked/revoked server-side
Used to obtain new access token
```

---

# 30. Har method ka short summary

| Method                   | Simple meaning                          |
| ------------------------ | --------------------------------------- |
| `init()`                 | Secret se signing key prepare karta hai |
| `generateAccessToken()`  | User ke liye signed JWT banata hai      |
| `generateRefreshToken()` | Random refresh token banata hai         |
| `validateToken()`        | JWT valid hai ya nahi check karta hai   |
| `extractAllClaims()`     | JWT ka complete payload nikalta hai     |
| `extractEmail()`         | JWT se email nikalta hai                |
| `extractUserId()`        | JWT se user ID nikalta hai              |
| `extractRoles()`         | JWT se roles nikalta hai                |

### Overall flow

```text
                    AUTH SERVICE
                         │
                    User Login
                         │
                         ▼
                  generateAccessToken()
                         │
                         ▼
                  ┌──────────────┐
                  │     JWT      │
                  │              │
                  │ userId       │
                  │ email        │
                  │ roles        │
                  │ issuedAt     │
                  │ expiration   │
                  └──────┬───────┘
                         │
                         ▼
                      Frontend
                         │
                         │ Bearer JWT
                         ▼
                   API Gateway
                         │
                         ▼
                  validateToken()
                         │
                  ┌──────┴──────┐
                  │             │
                Valid         Invalid
                  │             │
                  ▼             ▼
               Allow          Reject
                  │
                  ▼
             Extract User ID
             Extract Roles
                  │
                  ▼
           Protected Services
```

**Ek important architectural point:** tumhare description mein refresh token ko Redis mein track karne ki baat hai, lekin **is `JwtProvider.java` file mein Redis use nahi ho raha**. Ye file sirf refresh-token string generate karti hai. Redis mein save/validate/revoke karne ka actual logic kisi `AuthService`/`RefreshTokenService` type ke component mein hona chahiye.
*/
