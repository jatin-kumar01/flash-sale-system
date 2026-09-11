package com.flashsale.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn,
                                  Long userId, String email, String firstName,
                                  String lastName, Set<String> roles) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(roles)
                .build();
    }
}

/*
* Bilkul. `AuthResponse.java` ko tumhare **Flash Sale System** ke context mein simple Hinglish mein, **har part aur method ke example ke saath** samjho.

# `AuthResponse.java` kya hai?

Ye ek **DTO (Data Transfer Object)** hai.

Iska kaam hai authentication successful hone ke baad frontend ko required information ek structured response mein dena.

Flow:

```text
Frontend
   │
   │ Login
   ▼
AuthController
   │
   ▼
AuthService
   │
   ├── User verify
   ├── JWT generate
   └── Refresh token generate
   │
   ▼
AuthResponse
   │
   ▼
Frontend
```

Example frontend login request:

```json
{
  "email": "user@gmail.com",
  "password": "password123"
}
```

Successful login ke baad:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "abc123...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "userId": 101,
  "email": "user@gmail.com",
  "firstName": "Jatin",
  "lastName": "Kumar",
  "roles": ["USER"]
}
```

Ye response structure `AuthResponse` represent karta hai.

---

# 1. Package

```java
package com.flashsale.auth.dto;
```

Ye batata hai ki class kis package ke andar hai.

Tumhari structure:

```text
auth-service
└── src
    └── main
        └── java
            └── com
                └── flashsale
                    └── auth
                        └── dto
                            └── AuthResponse.java
```

`dto` ka matlab:

**Data Transfer Object**

---

# 2. Lombok imports

```java
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
```

Ye Lombok annotations ke liye hain.

Inka purpose boilerplate code kam karna hai.

---

# 3. `@Getter`

```java
@Getter
```

Lombok automatically getters generate karega.

Normally tumhe likhna padta:

```java
public String getAccessToken() {
    return accessToken;
}
```

Lekin `@Getter` ke wajah se automatically generate ho jayega.

Use:

```java
response.getAccessToken();
```

---

# 4. `@Setter`

```java
@Setter
```

Ye setters automatically generate karta hai.

Normally:

```java
public void setEmail(String email) {
    this.email = email;
}
```

Lekin Lombok ye automatically generate karega.

Use:

```java
response.setEmail("user@gmail.com");
```

---

# 5. `@Builder`

```java
@Builder
```

Ye **Builder Pattern** provide karta hai.

Without Builder:

```java
AuthResponse response = new AuthResponse();

response.setAccessToken(token);
response.setRefreshToken(refreshToken);
response.setUserId(101L);
response.setEmail("user@gmail.com");
```

Builder ke saath:

```java
AuthResponse response = AuthResponse.builder()
        .accessToken(token)
        .refreshToken(refreshToken)
        .userId(101L)
        .email("user@gmail.com")
        .build();
```

Ye especially useful hai jab class mein bahut saare fields hain.

Tumhari class mein 9 fields hain, isliye Builder useful hai.

---

# 6. `@NoArgsConstructor`

```java
@NoArgsConstructor
```

Ye empty constructor generate karta hai:

```java
public AuthResponse() {
}
```

Isliye:

```java
AuthResponse response = new AuthResponse();
```

possible hai.

---

# 7. `@AllArgsConstructor`

```java
@AllArgsConstructor
```

Ye saare fields wala constructor generate karta hai.

Conceptually:

```java
public AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    Long userId,
    String email,
    String firstName,
    String lastName,
    Set<String> roles
) {
    ...
}
```

Lekin itne fields wale constructor ko manually likhne ki zarurat nahi.

---

# 8. `Set<String>`

```java
import java.util.Set;
```

Aur:

```java
private Set<String> roles;
```

User ke multiple roles store karne ke liye.

Example:

```text
USER
ADMIN
SELLER
```

Response:

```json
{
  "roles": ["USER", "SELLER"]
}
```

### `Set` kyu?

Set duplicate values allow nahi karta.

For example:

```text
["USER", "USER", "ADMIN"]
```

Set mein duplicate `USER` remove ho jayega.

Result:

```text
["USER", "ADMIN"]
```

---

# 9. `accessToken`

```java
private String accessToken;
```

Ye **short-lived JWT access token** hai.

Login successful:

```text
User
 ↓
Email/password verify
 ↓
JWT generate
 ↓
accessToken
```

Frontend is token ko future authenticated requests mein use karega.

Example:

```http
Authorization: Bearer eyJhbGciOi...
```

For example:

```text
GET /api/orders
```

ke saath access token jayega.

---

# 10. `refreshToken`

```java
private String refreshToken;
```

Ye access token se generally longer-lived credential hota hai.

Example:

```text
Access token → 15 minutes
Refresh token → longer lifetime
```

Suppose access token expire ho gaya:

```text
Frontend
   ↓
Access Token expired
   ↓
Refresh Token
   ↓
Auth Service
   ↓
New Access Token
```

Isse user ko har baar login karne ki zarurat nahi padti.

---

# 11. `tokenType`

```java
@Builder.Default
private String tokenType = "Bearer";
```

Default value:

```text
Bearer
```

Isliye response:

```json
{
  "tokenType": "Bearer"
}
```

Frontend API request mein:

```http
Authorization: Bearer <access-token>
```

format use karega.

### `@Builder.Default` kyu?

Because Lombok Builder use karte waqt normal field initializer ka behavior preserve karne ke liye.

Without it, builder se object banate waqt default `"Bearer"` reliably apply nahi ho sakta.

---

# 12. `expiresIn`

```java
private Long expiresIn;
```

Ye batata hai access token kitne time tak valid hai.

Tumhare explanation ke according unit:

**milliseconds**

Example:

```text
expiresIn = 900000
```

means:

```text
900,000 ms
= 900 seconds
= 15 minutes
```

Frontend is information ka use token refresh timing ke liye kar sakta hai.

---

# 13. `userId`

```java
private Long userId;
```

Authenticated user ki unique ID.

Example:

```text
userId = 101
```

Frontend ko pata chalega current user ka ID `101` hai.

---

# 14. `email`

```java
private String email;
```

Authenticated user's email.

Example:

```text
email = user@gmail.com
```

Frontend account UI mein use kar sakta hai:

```text
Welcome
user@gmail.com
```

---

# 15. `firstName`

```java
private String firstName;
```

User ka first name.

Example:

```text
Jatin
```

Frontend:

```text
Welcome, Jatin
```

dikha sakta hai.

---

# 16. `lastName`

```java
private String lastName;
```

User ka last name.

Example:

```text
Kumar
```

Combined:

```text
Jatin Kumar
```

---

# 17. `roles`

```java
private Set<String> roles;
```

User ke authorization roles.

Example:

```json
{
  "roles": ["USER"]
}
```

Admin:

```json
{
  "roles": ["ADMIN"]
}
```

Multiple roles:

```json
{
  "roles": ["USER", "SELLER"]
}
```

Frontend UI permissions ke liye use kar sakta hai.

For example:

```text
USER
→ Product browse
→ Buy product

ADMIN
→ Admin dashboard
→ Manage products
→ View analytics
```

**Important:** Actual authorization/security backend par enforce honi chahiye. Frontend role ke basis par sirf UI hide/show kare; security ka final decision backend karega.

---

# 18. `of()` method

Ab tumhari class ka main helper method:

```java
public static AuthResponse of(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        Long userId,
        String email,
        String firstName,
        String lastName,
        Set<String> roles) {
```

Iska purpose hai:

> Saari authentication information lekar ek complete `AuthResponse` object banana.

---

## Example

AuthService mein:

```java
String accessToken = "...";
String refreshToken = "...";

Long expiresIn = 900000L;
Long userId = 101L;

String email = "user@gmail.com";
String firstName = "Jatin";
String lastName = "Kumar";

Set<String> roles = Set.of("USER");
```

Then:

```java
AuthResponse response = AuthResponse.of(
        accessToken,
        refreshToken,
        expiresIn,
        userId,
        email,
        firstName,
        lastName,
        roles
);
```

---

# 19. Builder inside `of()`

Method ke andar:

```java
return AuthResponse.builder()
```

Builder start karta hai.

Then:

```java
.accessToken(accessToken)
```

access token set karta hai.

```java
.refreshToken(refreshToken)
```

refresh token set karta hai.

```java
.tokenType("Bearer")
```

token type set karta hai.

```java
.expiresIn(expiresIn)
```

expiry set karta hai.

Then:

```java
.userId(userId)
.email(email)
.firstName(firstName)
.lastName(lastName)
.roles(roles)
```

user information set hoti hai.

Finally:

```java
.build();
```

complete `AuthResponse` object create karta hai.

---

# 20. Complete `of()` flow

```text
AuthService
     │
     │ accessToken
     │ refreshToken
     │ userId
     │ email
     │ roles
     ▼
AuthResponse.of(...)
     │
     ▼
Builder
     │
     ├── accessToken
     ├── refreshToken
     ├── Bearer
     ├── expiresIn
     ├── userId
     ├── email
     ├── firstName
     ├── lastName
     └── roles
     │
     ▼
AuthResponse Object
     │
     ▼
AuthController
     │
     ▼
JSON Response
     │
     ▼
Frontend
```

---

# 21. Actual JSON response

Suppose:

```text
accessToken = "abc.xyz"
refreshToken = "refresh123"
expiresIn = 900000
userId = 101
email = "user@gmail.com"
firstName = "Jatin"
lastName = "Kumar"
roles = ["USER"]
```

`AuthResponse` roughly JSON mein:

```json
{
  "accessToken": "abc.xyz",
  "refreshToken": "refresh123",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "userId": 101,
  "email": "user@gmail.com",
  "firstName": "Jatin",
  "lastName": "Kumar",
  "roles": ["USER"]
}
```

Frontend is response ko receive karega.

---

# 22. Registration mein bhi same DTO

Tumhare description ke according ye successful **registration** mein bhi use ho sakta hai.

Flow:

```text
Register
   ↓
AuthController
   ↓
AuthService
   ↓
Create User
   ↓
Generate tokens
   ↓
AuthResponse
   ↓
Frontend
```

---

# 23. Login example

```text
POST /auth/login
```

Request:

```json
{
  "email": "user@gmail.com",
  "password": "password123"
}
```

AuthService:

```text
1. User find
2. Password verify
3. Access token generate
4. Refresh token generate
5. User roles get
6. AuthResponse create
```

Response:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "userId": 101,
  "email": "user@gmail.com",
  "firstName": "Jatin",
  "lastName": "Kumar",
  "roles": ["USER"]
}
```

---

# 24. Ye DTO kyu important hai?

Agar `AuthResponse` na ho, controller directly multiple individual values manage kar sakta hai, jo messy ho jayega.

Instead:

```text
Access Token
Refresh Token
User ID
Email
Roles
Name
Expiry
```

sabko ek object mein package kar diya:

```text
AuthResponse
```

Then:

```text
AuthService
     ↓
AuthResponse
     ↓
AuthController
     ↓
Frontend
```

---

## Ek important security point

`AuthResponse` mein **password nahi hona chahiye**, aur tumhare current code mein password hai bhi nahi — **ye correct hai**.

Password:

```text
❌ Request/response DTO mein expose nahi karna
```

Access/refresh tokens sensitive hote hain, isliye production frontend mein unko store karne ka mechanism carefully design karna padega; especially refresh-token handling security-sensitive hai.

---

### Short summary

| Field/Method          | Purpose                                         |
| --------------------- | ----------------------------------------------- |
| `accessToken`         | Short-lived authenticated access credential     |
| `refreshToken`        | New access token obtain karne ke liye           |
| `tokenType`           | Usually `Bearer`                                |
| `expiresIn`           | Access-token lifetime                           |
| `userId`              | Current user's ID                               |
| `email`               | User email                                      |
| `firstName`           | User first name                                 |
| `lastName`            | User last name                                  |
| `roles`               | User authorization roles                        |
| `of()`                | Complete `AuthResponse` easily create karta hai |
| `@Builder`            | Builder pattern                                 |
| `@Getter/@Setter`     | Getters/setters automatically                   |
| `@NoArgsConstructor`  | Empty constructor                               |
| `@AllArgsConstructor` | All-fields constructor                          |

**Simple definition:** `AuthResponse` ek DTO hai jo **login/registration/token refresh ke baad Auth Service se Frontend tak token + user ki basic identity/role information ko ek standardized object mein transfer karta hai.**
*/
