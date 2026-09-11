package com.flashsale.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;
}
/*
* Bilkul. Is `RegisterRequest.java` ko simple **Hinglish + examples** ke saath samjho.

# `RegisterRequest.java` kya hai?

Ye ek **DTO (Data Transfer Object)** hai.

Iska kaam hai jab koi new user register kare, frontend se aane wali information ko receive karna:

```text
Frontend
   ↓
POST /register
   ↓
RegisterRequest
   ↓
Validation
   ↓
AuthService
```

For example frontend ye data bhejta hai:

```json
{
  "email": "jatin@gmail.com",
  "password": "Password123",
  "firstName": "Jatin",
  "lastName": "Kumar"
}
```

Ye data `RegisterRequest` object mein store hoga.

---

# 1. Class declaration

```java
public class RegisterRequest {
```

Ye registration request ka Java model hai.

Iske andar 4 fields hain:

```text
email
password
firstName
lastName
```

---

# 2. Lombok annotations

## `@Getter`

```java
@Getter
```

Automatically har field ke getter methods generate karta hai.

Normally tumhe manually likhna padta:

```java
public String getEmail() {
    return email;
}
```

`@Getter` ki wajah se Lombok ye automatically kar deta hai.

So:

```java
request.getEmail();
```

use kar sakte ho.

---

## `@Setter`

```java
@Setter
```

Automatically setter methods generate karta hai.

Normally:

```java
public void setEmail(String email) {
    this.email = email;
}
```

Lombok automatically generate karega.

---

## `@Builder`

```java
@Builder
```

Object ko builder pattern se create karne deta hai.

Example:

```java
RegisterRequest request =
    RegisterRequest.builder()
        .email("jatin@gmail.com")
        .password("Password123")
        .firstName("Jatin")
        .lastName("Kumar")
        .build();
```

Ye especially service/test code mein useful hai.

---

## `@NoArgsConstructor`

```java
@NoArgsConstructor
```

Empty constructor generate karta hai:

```java
new RegisterRequest();
```

Spring/Jackson ko request JSON ko Java object mein convert karte waqt ye useful ho sakta hai.

---

## `@AllArgsConstructor`

```java
@AllArgsConstructor
```

Saare fields wala constructor generate karta hai:

```java
new RegisterRequest(
    "jatin@gmail.com",
    "Password123",
    "Jatin",
    "Kumar"
);
```

---

# 3. Email field

```java
@NotBlank(message = "Email is required")
@Email(message = "Invalid email format")
@Size(max = 100, message = "Email cannot exceed 100 characters")
private String email;
```

Yahan **3 validations** hain.

---

## `@NotBlank`

```java
@NotBlank(message = "Email is required")
```

Email empty nahi honi chahiye.

Invalid:

```json
{
  "email": ""
}
```

Invalid:

```json
{
  "email": "   "
}
```

Error:

```text
Email is required
```

---

## `@Email`

```java
@Email(message = "Invalid email format")
```

Email ka format check karega.

Invalid:

```text
jatin
```

Invalid:

```text
jatin@
```

Valid example:

```text
jatin@gmail.com
```

---

## `@Size(max = 100)`

```java
@Size(
    max = 100,
    message = "Email cannot exceed 100 characters"
)
```

Email maximum 100 characters ki ho sakti hai.

Agar 101+ characters hain:

```text
Email cannot exceed 100 characters
```

---

# 4. Password field

```java
@NotBlank(message = "Password is required")
@Size(
    min = 8,
    max = 100,
    message = "Password must be between 8 and 100 characters"
)
private String password;
```

## `@NotBlank`

Password required hai.

Invalid:

```json
{
  "password": ""
}
```

---

## `@Size(min = 8, max = 100)`

Password:

```text
minimum = 8 characters
maximum = 100 characters
```

Valid:

```text
Password123
```

Invalid:

```text
abc
```

Error:

```text
Password must be between 8 and 100 characters
```

### Important

Tumhare provided code mein **actual password complexity** enforce nahi ho rahi.

For example, ye:

```text
abcdefgh
```

8 characters hai, so `@Size(min=8)` ke according valid ho sakta hai.

Agar tumhe future mein requirement ho:

```text
at least 1 uppercase
at least 1 lowercase
at least 1 number
at least 1 special character
```

to additional validation chahiye hogi.

---

# 5. First name

```java
@NotBlank(message = "First name is required")
@Size(
    max = 50,
    message = "First name cannot exceed 50 characters"
)
private String firstName;
```

### `@NotBlank`

First name required:

```text
"Jatin" ✅
"" ❌
"   " ❌
```

### `@Size(max = 50)`

Maximum 50 characters.

Example:

```text
Jatin
```

valid hai.

---

# 6. Last name

```java
@NotBlank(message = "Last name is required")
@Size(
    max = 50,
    message = "Last name cannot exceed 50 characters"
)
private String lastName;
```

Same concept:

```text
Kumar       ✅
""          ❌
"     "     ❌
```

Maximum:

```text
50 characters
```

---

# 7. Ye validation actually trigger kab hogi?

Important point: Sirf DTO mein:

```java
@NotBlank
@Email
@Size
```

likhne se validation automatically nahi chalegi.

Controller mein normally request ke saath:

```java
@Valid
```

use karna hota hai.

Example:

```java
@PostMapping("/register")
public ResponseEntity<?> register(
        @Valid @RequestBody RegisterRequest request) {

    return authService.register(request);
}
```

Flow:

```text
Frontend
   ↓
POST /register
   ↓
JSON
   ↓
RegisterRequest
   ↓
@Valid
   ↓
Validation
   ↓
      ┌───────────────┐
      │               │
      ▼               ▼
   Valid           Invalid
      │               │
      ▼               ▼
AuthService    Validation Exception
                      │
                      ▼
            GlobalExceptionHandler
                      │
                      ▼
                  400 Bad Request
```

---

# 8. Real example — Valid request

Frontend:

```json
{
  "email": "jatin@gmail.com",
  "password": "Password123",
  "firstName": "Jatin",
  "lastName": "Kumar"
}
```

Validation:

```text
email       ✅
password    ✅
firstName   ✅
lastName    ✅
```

Then:

```text
RegisterRequest
       ↓
AuthService
       ↓
Create User
       ↓
Save in database
```

---

# 9. Real example — Invalid request

Frontend:

```json
{
  "email": "jatin",
  "password": "123",
  "firstName": "",
  "lastName": "Kumar"
}
```

Problems:

```text
email → invalid format ❌
password → less than 8 ❌
firstName → blank ❌
lastName → valid ✅
```

`@Valid` validation fail karegi.

Then:

```text
MethodArgumentNotValidException
          ↓
GlobalExceptionHandler
          ↓
400 Bad Request
```

Tumhare previous `GlobalExceptionHandler` ke according, ye errors `ErrorDetail` list mein convert ho sakte hain.

---

# 10. `RegisterRequest` ka actual role

Ye **database entity nahi hai**.

Ye difference important hai.

### `RegisterRequest`

```text
Frontend → Backend
```

Data receive karta hai.

### `User`

```text
Backend → Database
```

Database mein user information represent karta hai.

So:

```text
                  Registration
                       │
Frontend JSON ─────────▼
                RegisterRequest
                       │
                    @Valid
                       │
                       ▼
                 AuthService
                       │
                       ▼
                     User
                       │
                       ▼
                 UserRepository
                       │
                       ▼
                  PostgreSQL
```

---

# 11. Tumhare code mein Lombok ka final role

```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

Basically ye boilerplate code reduce kar raha hai.

Without Lombok tumhe manually:

```text
Getters
Setters
Constructors
Builder
```

likhne padte.

With Lombok:

```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

se automatically generate ho jata hai.

---

# Short summary

`RegisterRequest.java` ka main kaam:

> **Frontend se registration data lena aur AuthService tak bhejne se pehle basic input validation karna.**

Ismein:

| Part                  | Purpose               |
| --------------------- | --------------------- |
| `email`               | User email            |
| `password`            | User password         |
| `firstName`           | First name            |
| `lastName`            | Last name             |
| `@NotBlank`           | Empty value prevent   |
| `@Email`              | Email format validate |
| `@Size`               | Length limit          |
| `@Getter/@Setter`     | Get/set methods       |
| `@Builder`            | Easy object creation  |
| `@NoArgsConstructor`  | Empty constructor     |
| `@AllArgsConstructor` | Full constructor      |

Aur tumhare project ke flow mein:

```text
Frontend
   ↓
RegisterRequest
   ↓
@Valid
   ↓
Validation
   ↓
AuthService
   ↓
User
   ↓
UserRepository
   ↓
Database
```

**Ye file Auth Service ke liye request boundary hai — iska kaam user ko database mein save karna nahi, balki registration request ko receive + validate karna hai.**
*/