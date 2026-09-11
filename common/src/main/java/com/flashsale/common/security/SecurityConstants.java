package com.flashsale.common.security;

public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    // HTTP Headers
    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    // JWT Claims
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";

    // Standard Roles
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}

/*
* Yes, **`SecurityConstants.java` is useful and important** for your project, especially once you build authentication, API Gateway, and RBAC.

### Simple Hinglish

Is file ka main purpose hai:

> **Security se related common names/values ko ek central place par rakhna.**

Instead of har microservice mein baar-baar:

```java
"Authorization"
"Bearer "
"userId"
"roles"
"ROLE_ADMIN"
```

likhne ke, ek common class se use karoge:

```java
SecurityConstants.AUTH_HEADER
SecurityConstants.TOKEN_PREFIX
SecurityConstants.CLAIM_USER_ID
SecurityConstants.ROLE_ADMIN
```

---

### 1. JWT Claims

JWT mein tum kuch information store karoge:

```text id="6i1e3k"
JWT
├── userId
├── email
└── roles
```

Constants ensure karte hain ki har service same names use kare:

```java
SecurityConstants.CLAIM_USER_ID
SecurityConstants.CLAIM_EMAIL
SecurityConstants.CLAIM_ROLES
```

Otherwise ek service `"userId"` aur doosri `"user_id"` use kar sakti hai, causing bugs.

---

### 2. Authorization Header

Frontend request:

```text id="q7s0t8"
Authorization: Bearer <JWT>
```

Instead of hardcoding:

```java
"Authorization"
"Bearer "
```

use:

```java
SecurityConstants.AUTH_HEADER
SecurityConstants.TOKEN_PREFIX
```

---

### 3. Roles

Tumhare system mein:

```text id="4f7s4h"
ROLE_USER
ROLE_ADMIN
```

For example:

```java
hasRole("ADMIN")
```

Spring Security mein role naming consistent rakhna important hai.

---

### 4. `Idempotency-Key`

Ye **flash-sale system ke liye particularly important** hai.

Suppose user Buy button double-click karta hai:

```text id="z6j7s2"
Request 1 → Buy Product
Request 2 → Buy Product
```

Dono requests accidentally same purchase create na kar dein.

Client request mein:

```text id="8u5t8v"
Idempotency-Key: abc-123
```

bhej sakte ho.

Backend same key ko recognize karke duplicate operation prevent kar sakta hai.

Ye tumhare **reservation/payment flow** mein useful hoga.

---

### 5. `X-User-Id` / `X-User-Roles`

Tumhare proposed architecture mein:

```text id="3h3u8g"
Frontend
   ↓
API Gateway
   ↓
JWT Validation
   ↓
Internal Service
```

Gateway JWT se identity extract karke internal request mein headers add kar sakta hai:

```text id="u7p8wh"
X-User-Id: 25
X-User-Roles: ROLE_USER
```

Downstream service ko user context mil sakta hai.

**Lekin ek security caution:** ye headers sirf trusted internal traffic mein accept hone chahiye; external clients ko arbitrary `X-User-Id`/`X-User-Roles` headers se identity spoof karne nahi dena chahiye.

---

### Overall

```text id="r5j8t2"
SecurityConstants
       │
       ├── HTTP Headers
       ├── JWT Claims
       ├── Roles
       └── Idempotency Key
              │
      ┌───────┼────────┐
      ↓       ↓        ↓
   Gateway   Auth    Services
```

**Short mein:** `SecurityConstants.java` ka kaam security-related **magic strings ko centralize aur consistent** rakhna hai.

Tumhare project mein ye **recommended common utility** hai, especially jab `auth-service`, `api-gateway`, aur multiple secured microservices implement karoge.
*/
