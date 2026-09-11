package com.flashsale.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
/*Bilkul. Is `RefreshTokenRequest.java` ko **simple Hinglish + example** ke saath samjho.

## 1. Is class ki need kyu hai?

Tumhare authentication system mein do important tokens ho sakte hain:

```text
Access Token
    ↓
Short time ke liye valid
    ↓
API access
```

aur:

```text
Refresh Token
    ↓
Longer time ke liye valid
    ↓
Naya Access Token lene ke liye
```

Example:

```text
User Login
   ↓
Access Token + Refresh Token
   ↓
Access Token expire
   ↓
Frontend Refresh Token bhejta hai
   ↓
Backend new Access Token deta hai
```

`RefreshTokenRequest` isi request ke andar aane wale **refresh token ko hold karta hai**.

---

# 2. Package

```java
package com.flashsale.auth.dto;
```

Iska matlab ye class:

```text
auth-service
   ↓
dto package
```

mein belong karti hai.

`DTO` = **Data Transfer Object**

Iska kaam data ko ek layer se doosri layer tak transfer karna hai.

---

# 3. Class declaration

```java
public class RefreshTokenRequest {
```

Ye ek DTO class hai.

Iska purpose sirf request ka data represent karna hai.

Example frontend request:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Ye JSON backend mein `RefreshTokenRequest` object mein convert ho sakta hai.

---

# 4. Lombok `@Getter`

```java
@Getter
```

Lombok automatically getter method generate karta hai.

Tum manually likhte:

```java
public String getRefreshToken() {
    return refreshToken;
}
```

Lekin `@Getter` ki wajah se ye automatically generate ho jata hai.

To service mein:

```java
request.getRefreshToken()
```

likh sakte ho.

---

# 5. Lombok `@Setter`

```java
@Setter
```

Ye setter generate karta hai.

Normally:

```java
public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
}
```

likhna padta.

`@Setter` se automatically generate ho jata hai.

---

# 6. `@Builder`

```java
@Builder
```

Ye Builder Pattern provide karta hai.

Example:

```java
RefreshTokenRequest request =
    RefreshTokenRequest.builder()
        .refreshToken("abc123")
        .build();
```

Iska benefit especially testing ya internal object creation mein hai.

---

# 7. `@NoArgsConstructor`

```java
@NoArgsConstructor
```

Ye empty constructor generate karta hai:

```java
public RefreshTokenRequest() {
}
```

Ye important hai because Spring/Jackson request JSON ko Java object mein convert karte waqt no-argument constructor use kar sakte hain.

Example:

```json
{
  "refreshToken": "abc123"
}
```

Spring internally object create kar sakta hai:

```text
RefreshTokenRequest()
       ↓
refreshToken set
```

---

# 8. `@AllArgsConstructor`

```java
@AllArgsConstructor
```

Ye constructor generate karta hai jisme saare fields honge.

Equivalent:

```java
public RefreshTokenRequest(String refreshToken) {
    this.refreshToken = refreshToken;
}
```

So tum likh sakte ho:

```java
RefreshTokenRequest request =
    new RefreshTokenRequest("abc123");
```

---

# 9. `@NotBlank`

Sabse important validation:

```java
@NotBlank(message = "Refresh token is required")
private String refreshToken;
```

Iska matlab:

> `refreshToken` empty, null ya sirf spaces nahi ho sakta.

### Valid

```json
{
  "refreshToken": "abc123xyz"
}
```

### Invalid

```json
{
  "refreshToken": ""
}
```

### Invalid

```json
{
  "refreshToken": "   "
}
```

### Invalid

```json
{
  "refreshToken": null
}
```

Validation fail hone par:

```text
@NotBlank
    ↓
Validation failure
    ↓
MethodArgumentNotValidException
    ↓
GlobalExceptionHandler
    ↓
400 Bad Request
```

Tumhare pehle wale `GlobalExceptionHandler` ke context mein ye directly relevant hai.

---

# 10. `private String refreshToken`

```java
private String refreshToken;
```

Ye actual refresh token store karega.

Example:

```text
refreshToken =
"eyJhbGciOiJIUzI1NiJ9..."
```

`private` hone ka matlab direct external access allowed nahi hai.

Access:

```java
request.getRefreshToken()
```

se hoga.

---

# 11. Complete request flow

Maan lo frontend ka access token expire ho gaya.

Frontend backend ko request bhejta hai:

```http
POST /api/auth/refresh
```

Body:

```json
{
  "refreshToken": "abc123xyz"
}
```

Spring is JSON ko convert karega:

```text
JSON
 ↓
RefreshTokenRequest
 ↓
AuthController
 ↓
AuthService
 ↓
Refresh token validate
 ↓
New Access Token
```

---

# 12. Controller mein iska use

Future mein `AuthController` kuch aisa ho sakta hai:

```java
@PostMapping("/refresh")
public AuthResponse refresh(
        @Valid @RequestBody RefreshTokenRequest request) {

    return authService.refreshToken(request);
}
```

Yahan:

### `@RequestBody`

JSON ko:

```text
JSON
 ↓
RefreshTokenRequest
```

mein convert karta hai.

### `@Valid`

`@NotBlank` validation activate karta hai.

So agar:

```json
{
  "refreshToken": ""
}
```

aaya:

```text
@Valid
 ↓
@NotBlank
 ↓
FAIL
 ↓
GlobalExceptionHandler
 ↓
400
```

---

# 13. AuthService mein

Service mein:

```java
String refreshToken =
    request.getRefreshToken();
```

Ab token mil gaya.

Then future implementation mein:

```text
Refresh Token
      ↓
Validate
      ↓
Check expiration
      ↓
Check user/session
      ↓
Generate new Access Token
```

---

# 14. `@Builder` ka real example

Testing ke time:

```java
RefreshTokenRequest request =
    RefreshTokenRequest.builder()
        .refreshToken("test-refresh-token")
        .build();
```

Then:

```java
authService.refreshToken(request);
```

---

# 15. Is DTO ko separate kyu rakha?

Tum directly `String` bhi controller mein le sakte the:

```java
public AuthResponse refresh(String refreshToken)
```

Lekin DTO better hai:

```java
public AuthResponse refresh(
    RefreshTokenRequest request)
```

Kyunki future mein additional fields add kar sakte ho:

```java
public class RefreshTokenRequest {

    private String refreshToken;

    private String deviceId;

    private String clientType;
}
```

Ab request contract expand ho sakta hai without auth ke baaki DTOs ko disturb kiye.

---

# 16. Tumhare Flash Sale project mein overall Auth flow

```text
             LOGIN
               │
               ▼
        AuthController
               │
               ▼
         AuthService
               │
        ┌──────┴──────┐
        ▼             ▼
 Access Token    Refresh Token
        │             │
        ▼             ▼
   API requests    Token renewal
                      │
                      ▼
          RefreshTokenRequest
                      │
                      ▼
               AuthService
                      │
                      ▼
             New Access Token
```

## Short mein

`RefreshTokenRequest` ka kaam **refresh-token request ka input contract** provide karna hai.

```text
JSON Request
     ↓
RefreshTokenRequest
     ↓
Validation
     ↓
AuthController
     ↓
AuthService
     ↓
New Access Token
```

Aur is class ka sabse important part:

```java
@NotBlank(message = "Refresh token is required")
private String refreshToken;
```

Ye ensure karta hai ki **empty/blank refresh token auth logic tak na pahuche**.
*/