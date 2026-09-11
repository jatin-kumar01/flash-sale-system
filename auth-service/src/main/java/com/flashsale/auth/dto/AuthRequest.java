package com.flashsale.auth.dto;

import jakarta.validation.constraints.Email;
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
public class AuthRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
    private String username;
}

/*Bilkul. Is `AuthRequest.java` ko **simple Hinglish mein har part aur har annotation ke example ke saath** samjho.

---

# `AuthRequest.java` kya hai?

Ye ek **DTO (Data Transfer Object)** hai.

Iska kaam hai **login ke time frontend se aane wali information ko receive karna**.

Frontend se request:

```json
{
  "email": "user@gmail.com",
  "password": "mypassword123"
}
```

Backend mein ye data:

```java
AuthRequest
```

object mein store hoga.

Flow:

```text
Frontend
   ↓
POST /login
   ↓
AuthController
   ↓
AuthRequest
   ↓
AuthService
   ↓
Database
```

---

# 1. Package

```java
package com.flashsale.auth.dto;
```

Iska matlab ye file `dto` package ke andar hai.

Tumhara structure:

```text
auth/
└── dto/
    └── AuthRequest.java
```

`dto` ka matlab:

> Data Transfer Object

Yaani data ko ek layer se doosri layer tak transfer karne ke liye object.

---

# 2. Imports

## `@Email`

```java
import jakarta.validation.constraints.Email;
```

Ye email format validate karne ke liye hai.

Example:

```text
user@gmail.com
```

✅ Valid

Lekin:

```text
usergmail.com
```

❌ Invalid

---

## `@NotBlank`

```java
import jakarta.validation.constraints.NotBlank;
```

Ye check karta hai ki field:

* `null` na ho
* empty na ho
* sirf spaces na ho

Example:

```text
email = ""
```

❌

```text
email = "   "
```

❌

```text
email = null
```

❌

```text
email = "user@gmail.com"
```

✅

---

# 3. Lombok imports

```java
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
```

Ye Lombok ke annotations hain.

Inka purpose hai unnecessary boilerplate Java code automatically generate karna.

---

# 4. `@Getter`

```java
@Getter
```

Lombok automatically getter methods generate karega.

Tumne manually nahi likha:

```java
public String getEmail() {
    return email;
}
```

Lombok automatically generate karega.

So:

```java
request.getEmail();
```

work karega.

---

# 5. `@Setter`

```java
@Setter
```

Lombok setter methods generate karega.

Normally:

```java
public void setEmail(String email) {
    this.email = email;
}
```

likhna padta.

Lombok automatically generate karega.

So:

```java
request.setEmail("user@gmail.com");
```

work karega.

---

# 6. `@Builder`

```java
@Builder
```

Ye **Builder Pattern** provide karta hai.

Normally object banana:

```java
AuthRequest request =
    new AuthRequest(
        "user@gmail.com",
        "password123"
    );
```

Builder ke saath:

```java
AuthRequest request =
    AuthRequest.builder()
        .email("user@gmail.com")
        .password("password123")
        .build();
```

Ye readable hota hai, especially jab DTO mein bahut fields ho.

---

# 7. `@NoArgsConstructor`

```java
@NoArgsConstructor
```

Ye automatically empty constructor banata hai:

```java
public AuthRequest() {
}
```

Iski need Spring/Jackson ko request JSON ko Java object mein convert karte waqt pad sakti hai.

For example:

```json
{
  "email": "user@gmail.com",
  "password": "abc123"
}
```

Jackson ise `AuthRequest` object mein convert karega.

---

# 8. `@AllArgsConstructor`

```java
@AllArgsConstructor
```

Ye constructor generate karega jisme **saari fields** hongi.

Equivalent:

```java
public AuthRequest(
    String email,
    String password
) {
    this.email = email;
    this.password = password;
}
```

So manually:

```java
new AuthRequest(
    "user@gmail.com",
    "password123"
);
```

kar sakte ho.

---

# 9. Class

```java
public class AuthRequest {
```

Ye login request ka DTO hai.

Ismein currently sirf:

```text
email
password
```

hain.

---

# 10. Email field

```java
@NotBlank(message = "Email is required")
@Email(message = "Invalid email format")
private String email;
```

Yahan **do validation rules** hain.

### Rule 1

```java
@NotBlank(message = "Email is required")
```

Email empty nahi ho sakti.

Agar:

```json
{
  "email": "",
  "password": "abc123"
}
```

to:

```text
Email is required
```

---

### Rule 2

```java
@Email(message = "Invalid email format")
```

Email ka format valid hona chahiye.

Example:

```text
abc@gmail.com
```

✅

But:

```text
abc@gmail
```

ya:

```text
abc.com
```

❌

Then:

```text
Invalid email format
```

---

# 11. Password field

```java
@NotBlank(message = "Password is required")
private String password;
```

Password empty nahi ho sakta.

Example:

```json
{
  "email": "user@gmail.com",
  "password": ""
}
```

Result:

```text
Password is required
```

---

# 12. Important: `@Valid`

Ye validations tab automatically run hongi jab controller mein DTO ke saath `@Valid` use kiya jayega.

Example:

```java
@PostMapping("/login")
public ResponseEntity<?> login(
        @Valid @RequestBody AuthRequest request) {

    ...
}
```

Flow:

```text
Frontend
   ↓
JSON
   ↓
AuthRequest
   ↓
@Valid
   ↓
Validation
   ↓
┌───────────────┐
│ Valid?        │
└───────┬───────┘
        │
    ┌───┴───┐
    ↓       ↓
   YES      NO
    ↓       ↓
AuthService 400
```

Agar validation fail ho gayi, request normally `AuthService` tak unnecessary processing ke liye nahi jayegi.

---

# 13. Complete real example

Frontend:

```json
{
  "email": "jatin@gmail.com",
  "password": "password123"
}
```

Spring:

```text
JSON
 ↓
AuthRequest
 ↓
email = jatin@gmail.com
password = password123
 ↓
@Valid
 ↓
Validation SUCCESS
 ↓
AuthService
```

---

# 14. Invalid example

Frontend:

```json
{
  "email": "jatin",
  "password": ""
}
```

Validation:

```text
email
 ↓
@Email
 ↓
❌ Invalid email format

password
 ↓
@NotBlank
 ↓
❌ Password is required
```

Phir tumhara previously discussed:

```text
GlobalExceptionHandler
```

validation exception ko handle karke structured response de sakta hai.

Conceptually:

```json
{
  "success": false,
  "message": "Validation failed for one or more fields",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    },
    {
      "field": "password",
      "message": "Password is required"
    }
  ]
}
```

---

# 15. Ye `AuthRequest` aur `User` mein difference

Ye important hai.

### `AuthRequest`

```text
DTO
```

Login ke waqt **client se data receive** karta hai.

```text
email
password
```

### `User`

```text
Entity
```

Database mein user ka data represent karega.

For example:

```text
id
name
email
passwordHash
createdAt
role
```

Flow:

```text
Frontend
   ↓
AuthRequest DTO
   ↓
AuthController
   ↓
AuthService
   ↓
UserRepository
   ↓
User Entity
   ↓
Database
```

**Password ko `AuthRequest` mein receive karna normal hai, lekin database mein plain password store nahi karna chahiye; hashed password store hona chahiye.**

---

# 16. Har annotation ka short purpose

| Annotation            | Purpose                        |
| --------------------- | ------------------------------ |
| `@Getter`             | Getters automatically          |
| `@Setter`             | Setters automatically          |
| `@Builder`            | Builder pattern                |
| `@NoArgsConstructor`  | Empty constructor              |
| `@AllArgsConstructor` | All-fields constructor         |
| `@NotBlank`           | Null/empty/blank value prevent |
| `@Email`              | Email format validate          |

---

# 17. Overall picture

```text
             Frontend
                │
                │
                ▼
       {
         email,
         password
       }
                │
                ▼
         AuthRequest
                │
             @Valid
                │
        ┌───────┴────────┐
        │                │
     Valid             Invalid
        │                │
        ▼                ▼
 AuthService       GlobalExceptionHandler
        │                │
        ▼                ▼
   UserRepository      HTTP 400
        │
        ▼
    Database
```

### Ek line mein

**`AuthRequest.java` login request ka gatekeeper hai** — frontend se `email/password` receive karta hai, basic validation karta hai, aur valid request ko `AuthService` tak bhejne ke liye structured DTO provide karta hai.
*/
