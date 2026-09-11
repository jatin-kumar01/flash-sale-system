package com.flashsale.auth.controller;

import com.flashsale.auth.dto.AuthRequest;
import com.flashsale.auth.dto.AuthResponse;
import com.flashsale.auth.dto.RefreshTokenRequest;
import com.flashsale.auth.dto.RegisterRequest;
import com.flashsale.auth.service.AuthService;
import com.flashsale.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        log.info("Received login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Received token refresh request");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
/*This `AuthController.java` is the **REST API entry point for authentication** in your `auth-service`.

I'll explain **every method with a practical example**, specifically for your Flash Sale project.

---

# 1. Overall role of `AuthController`

The flow is:

```text
Frontend
   ↓
API Gateway
   ↓
Auth Service
   ↓
AuthController
   ↓
AuthService
   ↓
Database / Redis
```

The controller's job is mainly to:

1. Receive HTTP requests.
2. Validate request DTOs.
3. Call `AuthService`.
4. Return standardized `ApiResponse`.

It should **not contain authentication business logic**.

---

# 2. Class annotations

## `@RestController`

```java
@RestController
```

Tells Spring:

> This class contains REST API endpoints.

So methods such as:

```java
@PostMapping("/login")
```

can receive HTTP requests.

---

## `@RequestMapping("/api/auth")`

```java
@RequestMapping("/api/auth")
```

This is the common base URL.

Therefore:

```java
@PostMapping("/register")
```

becomes:

```text
POST /api/auth/register
```

Similarly:

```text
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

---

## `@RequiredArgsConstructor`

```java
@RequiredArgsConstructor
```

This is Lombok.

Because:

```java
private final AuthService authService;
```

exists, Lombok automatically creates a constructor approximately equivalent to:

```java
public AuthController(AuthService authService) {
    this.authService = authService;
}
```

Spring then injects `AuthService`.

So you don't need to manually write:

```java
@Autowired
public AuthController(AuthService authService) {
    this.authService = authService;
}
```

---

## `@Slf4j`

```java
@Slf4j
```

Lombok automatically creates a logger:

```java
log.info(...)
log.debug(...)
log.warn(...)
log.error(...)
```

You use it for backend logging.

---

# 3. `register()` method

```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request) {
```

This handles:

```http
POST /api/auth/register
```

---

## `@PostMapping("/register")`

Means:

> When a POST request comes to `/api/auth/register`, execute this method.

Complete endpoint:

```text
POST /api/auth/register
```

---

# 4. `@RequestBody`

```java
@RequestBody RegisterRequest request
```

Suppose frontend sends:

```json
{
  "name": "Jatin",
  "email": "jatin@example.com",
  "password": "Password123"
}
```

Spring automatically converts that JSON into:

```java
RegisterRequest request
```

Conceptually:

```text
JSON
 ↓
Jackson
 ↓
RegisterRequest object
```

---

# 5. `@Valid`

```java
@Valid
```

This tells Spring:

> Validate the `RegisterRequest` according to its validation annotations.

Suppose:

```java
@NotBlank
private String name;

@Email
private String email;

@Size(min = 8)
private String password;
```

And frontend sends:

```json
{
  "name": "",
  "email": "abc",
  "password": "123"
}
```

Validation fails **before `AuthService.register()` is called**.

Then:

```text
@Valid
   ↓
Validation fails
   ↓
MethodArgumentNotValidException
   ↓
GlobalExceptionHandler
   ↓
400 Bad Request
```

This is why your earlier `GlobalExceptionHandler` is connected to this controller.

---

# 6. Logging registration

```java
log.info(
    "Received registration request for email: {}",
    request.getEmail()
);
```

It logs something like:

```text
Received registration request for email: jatin@example.com
```

### Important

You should **never log passwords**.

This code doesn't log the password, which is correct.

---

# 7. Calling `AuthService`

```java
AuthResponse response =
        authService.register(request);
```

This is one of the most important architectural points.

The controller does **not** perform:

```text
Check email
Hash password
Save user
Generate JWT
Store refresh token
```

itself.

Instead:

```text
AuthController
      ↓
AuthService
      ↓
Business logic
```

The controller only delegates.

---

# 8. Returning `201 CREATED`

```java
return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                response,
                "User registered successfully"
            )
        );
```

Successful registration returns:

```http
201 Created
```

Conceptually:

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    ...
  }
}
```

---

# 9. Complete registration flow

```text
Frontend
   │
   │ POST /api/auth/register
   ▼
API Gateway
   │
   ▼
AuthController
   │
   │ @Valid
   ▼
RegisterRequest
   │
   ▼
AuthService.register()
   │
   ├── validate business rules
   ├── hash password
   ├── save user
   └── create authentication response
   │
   ▼
AuthResponse
   │
   ▼
ApiResponse
   │
   ▼
201 CREATED
```

---

# 10. `login()` method

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody AuthRequest request) {
```

Endpoint:

```text
POST /api/auth/login
```

---

## Example request

Frontend sends:

```json
{
  "email": "jatin@example.com",
  "password": "Password123"
}
```

Spring converts it to:

```java
AuthRequest request
```

---

## `@Valid`

Again:

```java
@Valid
```

checks things such as:

```text
email format
password not blank
```

Invalid request:

```json
{
  "email": "abc",
  "password": ""
}
```

goes to:

```text
GlobalExceptionHandler
      ↓
400 Bad Request
```

---

# 11. Login logging

```java
log.info(
    "Received login request for email: {}",
    request.getEmail()
);
```

Logs:

```text
Received login request for email: jatin@example.com
```

Again, password is not logged.

---

# 12. `authService.login()`

```java
AuthResponse response =
        authService.login(request);
```

Business logic belongs to `AuthService`.

Conceptually:

```text
AuthController
      ↓
AuthService
      ↓
UserRepository
      ↓
PostgreSQL
```

Then authentication may involve:

```text
Password verification
       ↓
JWT access token
       +
Refresh token
```

---

# 13. Returning login response

```java
return ResponseEntity.ok(
    ApiResponse.success(
        response,
        "Login successful"
    )
);
```

`ResponseEntity.ok()` means:

```http
200 OK
```

Response conceptually:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 900
  }
}
```

The exact fields depend on your `AuthResponse`.

---

# 14. Login flow

```text
Frontend
   │
   │ POST /api/auth/login
   ▼
AuthController
   │
   ▼
AuthService
   │
   ├── find user
   ├── verify password
   ├── generate access JWT
   └── generate/store refresh token
   │
   ▼
AuthResponse
   │
   ▼
200 OK
```

---

# 15. `refreshToken()` method

```java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<AuthResponse>>
refreshToken(
        @Valid @RequestBody RefreshTokenRequest request) {
```

Endpoint:

```text
POST /api/auth/refresh
```

Purpose:

> User ka access token expire hone par refresh token ke through new tokens obtain karna.

---

# 16. Why refresh token?

Suppose access token short-lived hai:

```text
Access Token
expires → 15 minutes
```

User ko har 15 minutes password se login karwana bad UX hoga.

Instead:

```text
Access Token expired
        ↓
Refresh Token
        ↓
New Access Token
```

---

# 17. Example request

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1Ni..."
}
```

Spring converts it to:

```java
RefreshTokenRequest request
```

---

# 18. `@Valid`

```java
@Valid
```

checks that refresh token request is valid.

For example, if DTO has:

```java
@NotBlank
private String refreshToken;
```

and frontend sends:

```json
{
  "refreshToken": ""
}
```

then:

```text
Validation failure
       ↓
GlobalExceptionHandler
       ↓
400
```

---

# 19. Debug logging

```java
log.debug("Received token refresh request");
```

This records that refresh was requested.

Notice it **doesn't log the actual refresh token**, which is good from a security perspective.

---

# 20. Calling service

```java
AuthResponse response =
        authService.refreshToken(request);
```

Controller delegates to service.

Service may perform:

```text
Refresh token
     ↓
Check Redis
     ↓
Validate token
     ↓
Generate new access token
     ↓
Generate/rotate refresh token
```

---

# 21. Response

```java
return ResponseEntity.ok(
    ApiResponse.success(
        response,
        "Token refreshed successfully"
    )
);
```

Returns:

```http
200 OK
```

---

# 22. Refresh flow

```text
Frontend
   │
   │ Access token expired
   ▼
POST /api/auth/refresh
   │
   ▼
AuthController
   │
   ▼
AuthService
   │
   ▼
Redis
   │
   ▼
Validate refresh token
   │
   ▼
Generate new JWT pair
   │
   ▼
AuthResponse
   │
   ▼
200 OK
```

---

# 23. `logout()` method

This method is slightly different:

```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(
        @RequestBody(required = false)
        RefreshTokenRequest request) {
```

Endpoint:

```text
POST /api/auth/logout
```

---

# 24. `required = false`

```java
@RequestBody(required = false)
```

means request body is optional.

So both can technically be accepted:

### With refresh token

```json
{
  "refreshToken": "abc..."
}
```

### Without body

```http
POST /api/auth/logout
```

---

# 25. Null check

```java
if (request != null &&
        request.getRefreshToken() != null) {
```

This prevents errors.

Because request may be:

```text
null
```

or:

```text
request exists
refreshToken = null
```

Only if both are present does logout call the service.

---

# 26. Calling logout service

```java
authService.logout(
    request.getRefreshToken()
);
```

Suppose Redis contains:

```text
refresh-token:user123 → abcXYZ
```

Logout can remove/revoke that refresh token.

Conceptually:

```text
Logout
  ↓
AuthService
  ↓
Redis
  ↓
Delete/revoke refresh token
```

After that, that refresh token should no longer be usable.

---

# 27. Why Redis?

Your architecture uses Redis for refresh-token/session-related fast access.

For example:

```text
Redis

refresh:user:123
      ↓
refresh-token-data
```

Logout can invalidate it quickly without maintaining a traditional server-side session in application memory.

---

# 28. Return logout response

```java
return ResponseEntity.ok(
    ApiResponse.success(
        null,
        "Logged out successfully"
    )
);
```

Returns:

```http
200 OK
```

There is no data to return, so:

```java
null
```

is used.

Conceptually:

```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

---

# 29. Complete AuthController picture

Your four endpoints are:

```text
/api/auth
    │
    ├── POST /register
    │       ↓
    │    Create user
    │
    ├── POST /login
    │       ↓
    │    Authenticate user
    │
    ├── POST /refresh
    │       ↓
    │    Generate fresh tokens
    │
    └── POST /logout
            ↓
         Revoke refresh token
```

---

# 30. Most important architectural concept

Remember:

### Controller should NOT do this:

```text
Controller
   ├── Database query
   ├── Password hashing
   ├── JWT generation
   ├── Redis operations
   └── Business rules
```

Instead:

```text
Controller
     ↓
AuthService
     ↓
Repository / JWT / Redis
```

The controller is basically the **API boundary**.

### In one sentence:

> **`AuthController` receives HTTP requests, validates their DTOs, delegates the actual authentication work to `AuthService`, and converts the result into standardized HTTP responses.**
*/