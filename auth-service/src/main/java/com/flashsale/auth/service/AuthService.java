//package com.flashsale.auth.service;
//
//import com.flashsale.auth.config.JwtProvider;
//import com.flashsale.auth.dto.AuthRequest;
//import com.flashsale.auth.dto.AuthResponse;
//import com.flashsale.auth.dto.RefreshTokenRequest;
//import com.flashsale.auth.dto.RegisterRequest;
//import com.flashsale.auth.entity.User;
//import com.flashsale.auth.repository.UserRepository;
//import com.flashsale.common.exception.DuplicateRequestException;
//import com.flashsale.common.exception.ResourceNotFoundException;
//import com.flashsale.common.security.SecurityConstants;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Duration;
//import java.util.Set;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtProvider jwtProvider;
//    private final AuthenticationManager authenticationManager;
//    private final StringRedisTemplate stringRedisTemplate;
//
//    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
//
//    @Transactional
//    public AuthResponse register(RegisterRequest request) {
//        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
//            throw new DuplicateRequestException("Email is already registered: " + request.getEmail());
//        }
//
//        User user = User.builder()
//                .email(request.getEmail().toLowerCase().trim())
//                .passwordHash(passwordEncoder.encode(request.getPassword()))
//                .firstName(request.getFirstName().trim())
//                .lastName(request.getLastName().trim())
//                .enabled(true)
//                .roles(Set.of(SecurityConstants.ROLE_USER))
//                .build();
//
//        User savedUser = userRepository.save(user);
//        log.info("Successfully registered new user with ID: {}", savedUser.getId());
//
//        return generateAuthResponse(savedUser);
//    }
//
//    public AuthResponse login(AuthRequest request) {
//        String email = request.getEmail().toLowerCase().trim();
//
//        try {
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
//            );
//        } catch (Exception ex) {
//            log.warn("Authentication failed for email: {}", email);
//            throw new BadCredentialsException("Invalid email or password");
//        }
//
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
//
//        log.info("User successfully authenticated: ID {}", user.getId());
//        return generateAuthResponse(user);
//    }
//
//    public AuthResponse refreshToken(RefreshTokenRequest request) {
//        String tokenKey = REFRESH_TOKEN_KEY_PREFIX + request.getRefreshToken();
//        String userIdStr = stringRedisTemplate.opsForValue().get(tokenKey);
//
//        if (userIdStr == null) {
//            throw new BadCredentialsException("Invalid or expired refresh token");
//        }
//
//        Long userId = Long.parseLong(userIdStr);
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
//
//        // Invalidate previous refresh token (token rotation)
//        stringRedisTemplate.delete(tokenKey);
//
//        log.info("Refreshed access token for user ID: {}", userId);
//        return generateAuthResponse(user);
//    }
//
//    public void logout(String refreshToken) {
//        if (refreshToken != null && !refreshToken.isBlank()) {
//            String tokenKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
//            stringRedisTemplate.delete(tokenKey);
//            log.info("Invalidated refresh token on logout");
//        }
//    }
//
//    private AuthResponse generateAuthResponse(User user) {
//        String accessToken = jwtProvider.generateAccessToken(user);
//        String refreshToken = jwtProvider.generateRefreshToken();
//
//        // Store refresh token in Redis with TTL matching configuration
//        String redisKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
//        stringRedisTemplate.opsForValue().set(
//                redisKey,
//                String.valueOf(user.getId()),
//                Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMs())
//        );
//
//        return AuthResponse.of(
//                accessToken,
//                refreshToken,
//                jwtProvider.getAccessTokenExpirationMs(),
//                user.getId(),
//                user.getEmail(),
//                user.getFirstName(),
//                user.getLastName(),
//                user.getRoles()
//        );
//    }
//}



//2bd time

package com.flashsale.auth.service;

import com.flashsale.auth.config.JwtProvider;
import com.flashsale.auth.dto.AuthRequest;
import com.flashsale.auth.dto.AuthResponse;
import com.flashsale.auth.dto.RefreshTokenRequest;
import com.flashsale.auth.dto.RegisterRequest;
import com.flashsale.auth.entity.User;
import com.flashsale.auth.repository.UserRepository;
import com.flashsale.common.exception.DuplicateRequestException;
import com.flashsale.common.exception.ResourceNotFoundException;
import com.flashsale.common.security.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";

    @Transactional
//    public AuthResponse register(RegisterRequest request) {
//        String identifier = request.getEmail() != null ? request.getEmail() : request.getUsername();
//        if (identifier == null || identifier.isBlank()) {
//            throw new IllegalArgumentException("Email or username must be provided");
//        }
//
//        String cleanIdentifier = identifier.toLowerCase().trim();
//
//        if (userRepository.existsByEmail(cleanIdentifier)) {
//            throw new DuplicateRequestException("Email/Username is already registered: " + cleanIdentifier);
//        }
//
//        User user = User.builder()
//                .email(cleanIdentifier)
//                .passwordHash(passwordEncoder.encode(request.getPassword()))
//                .firstName(request.getFirstName() != null ? request.getFirstName().trim() : "User")
//                .lastName(request.getLastName() != null ? request.getLastName().trim() : "")
//                .enabled(true)
//                .roles(Set.of(SecurityConstants.ROLE_USER))
//                .build();
//
//        User savedUser = userRepository.save(user);
//        log.info("Successfully registered new user with ID: {}", savedUser.getId());
//
//        return generateAuthResponse(savedUser);
//    }

    public AuthResponse register(RegisterRequest request) {
        String identifier = request.getEmail();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Email or username must be provided");
        }

        String cleanIdentifier = identifier.toLowerCase().trim();

        if (userRepository.existsByEmail(cleanIdentifier)) {
            throw new DuplicateRequestException("Email/Username is already registered: " + cleanIdentifier);
        }

        User user = User.builder()
                .email(cleanIdentifier)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName() != null ? request.getFirstName().trim() : "User")
                .lastName(request.getLastName() != null ? request.getLastName().trim() : "")
                .enabled(true)
                .roles(Set.of(SecurityConstants.ROLE_USER))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered new user with ID: {}", savedUser.getId());

        return generateAuthResponse(savedUser);
    }

    public AuthResponse login(AuthRequest request) {
        String identifier = request.getEmail() != null ? request.getEmail() : request.getUsername();
        if (identifier == null || identifier.isBlank()) {
            throw new BadCredentialsException("Invalid credentials provided");
        }

        String cleanIdentifier = identifier.toLowerCase().trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanIdentifier, request.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Authentication failed for identifier: {}", cleanIdentifier);
            throw new BadCredentialsException("Invalid email/username or password");
        }

        User user = userRepository.findByEmail(cleanIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email/username", cleanIdentifier));

        log.info("User successfully authenticated: ID {}", user.getId());
        return generateAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenKey = REFRESH_TOKEN_KEY_PREFIX + request.getRefreshToken();
        String userIdStr = stringRedisTemplate.opsForValue().get(tokenKey);

        if (userIdStr == null) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Invalidate previous refresh token (token rotation)
        stringRedisTemplate.delete(tokenKey);

        log.info("Refreshed access token for user ID: {}", userId);
        return generateAuthResponse(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
            stringRedisTemplate.delete(tokenKey);
            log.info("Invalidated refresh token on logout");
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken();

        // Store refresh token in Redis with TTL matching configuration
        String redisKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
        stringRedisTemplate.opsForValue().set(
                redisKey,
                String.valueOf(user.getId()),
                Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMs())
        );

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtProvider.getAccessTokenExpirationMs(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles()
        );
    }
}
/*Bilkul. `AuthService.java` tumhare project ka **authentication ka main business-logic layer** hai. Iska kaam Controller se request lena nahi, balki **registration, login, JWT generation, refresh token management aur logout** ka actual logic handle karna hai.

Pehle overall flow:

```text
AuthController
      ↓
AuthService
      ↓
 ┌────┼──────────┬─────────────┐
 ↓    ↓          ↓             ↓
User DB   PasswordEncoder   JwtProvider   Redis
```

Ab **har method** ko example ke saath samjho.

---

# 1. Class-level annotations

### `@Slf4j`

```java
@Slf4j
```

Ye Lombok ka annotation hai jo automatically:

```java
log
```

object provide karta hai.

Isliye tum directly:

```java
log.info("User registered");
log.warn("Login failed");
```

likh sakte ho.

---

### `@Service`

```java
@Service
```

Spring ko batata hai:

> `AuthService` ek service/business-logic class hai. Iska object Spring khud manage karega.

Controller mein phir:

```java
private final AuthService authService;
```

inject ho sakta hai.

---

### `@RequiredArgsConstructor`

```java
@RequiredArgsConstructor
```

Lombok automatically constructor bana deta hai:

```java
public AuthService(
    UserRepository userRepository,
    PasswordEncoder passwordEncoder,
    JwtProvider jwtProvider,
    AuthenticationManager authenticationManager,
    StringRedisTemplate stringRedisTemplate
) {
    ...
}
```

Isliye manually constructor likhne ki zarurat nahi.

---

# 2. Dependencies

```java
private final UserRepository userRepository;
```

Database mein users ke saath kaam karega.

Example:

```text
Find user
Save user
Check email
```

---

```java
private final PasswordEncoder passwordEncoder;
```

Password ko BCrypt/hash mein convert karega.

```text
"mypassword123"
       ↓
BCrypt
       ↓
"$2a$10$...."
```

**Actual password database mein store nahi hoga.**

---

```java
private final JwtProvider jwtProvider;
```

JWT access aur refresh tokens generate karne ke liye.

---

```java
private final AuthenticationManager authenticationManager;
```

Login ke time username/password verify karne ke liye Spring Security ka authentication mechanism.

---

```java
private final StringRedisTemplate stringRedisTemplate;
```

Redis mein refresh tokens store/delete karne ke liye.

---

# 3. `REFRESH_TOKEN_KEY_PREFIX`

```java
private static final String REFRESH_TOKEN_KEY_PREFIX =
        "refresh_token:";
```

Ye Redis key ka prefix hai.

Suppose refresh token:

```text
abc123xyz
```

hai.

Redis key:

```text
refresh_token:abc123xyz
```

banegi.

Structure:

```text
Redis
│
└── refresh_token:abc123xyz
        ↓
      userId
```

---

# 4. `register()`

Ye method **new user registration** ke liye hai.

```java
@Transactional
public AuthResponse register(RegisterRequest request)
```

Flow:

```text
Register Request
       ↓
Check email
       ↓
Hash password
       ↓
Create User
       ↓
Save DB
       ↓
Generate JWT
       ↓
Store refresh token in Redis
       ↓
Return AuthResponse
```

---

## Step 1 — Email check

```java
if (userRepository.existsByEmail(
        request.getEmail().toLowerCase().trim())) {
```

Maan lo user enter karta hai:

```text
Jatin@Gmail.com
```

`toLowerCase()`:

```text
jatin@gmail.com
```

`trim()` extra spaces remove karta hai.

Phir database mein check:

```text
Is jatin@gmail.com already registered?
```

---

## Agar email already exists

```java
throw new DuplicateRequestException(
    "Email is already registered: "
    + request.getEmail()
);
```

Example:

```text
User enters:
jatin@gmail.com

DB:
jatin@gmail.com already exists
```

Then:

```text
DuplicateRequestException
       ↓
GlobalExceptionHandler
       ↓
409 Conflict
```

---

# 5. User object create karna

```java
User user = User.builder()
```

Yahan `User` entity create ho rahi hai.

---

### Email

```java
.email(
    request.getEmail()
        .toLowerCase()
        .trim()
)
```

Input:

```text
" Jatin@Gmail.com "
```

Store:

```text
jatin@gmail.com
```

---

# 6. Password hashing

```java
.passwordHash(
    passwordEncoder.encode(
        request.getPassword()
    )
)
```

Suppose user enters:

```text
Password123
```

Database mein:

```text
Password123
```

**store nahi hoga.**

Instead BCrypt hash:

```text
$2a$10$xxxxxxxxxxxxxxxx...
```

store hoga.

Important:

```text
Password
   ↓
PasswordEncoder
   ↓
Hash
   ↓
Database
```

---

# 7. First name / last name

```java
.firstName(request.getFirstName().trim())
.lastName(request.getLastName().trim())
```

Extra spaces remove karta hai.

Example:

```text
" Jatin "
```

becomes:

```text
"Jatin"
```

---

# 8. User enabled

```java
.enabled(true)
```

Matlab account currently active hai.

```text
enabled = true
```

---

# 9. Default role

```java
.roles(Set.of(SecurityConstants.ROLE_USER))
```

New user ko automatically:

```text
ROLE_USER
```

milta hai.

Example:

```text
New user
   ↓
ROLE_USER
```

Admin role automatically nahi diya ja raha.

---

# 10. Database mein save

```java
User savedUser =
    userRepository.save(user);
```

Ab user PostgreSQL/database mein save hota hai.

Example:

```text
id = 101
email = jatin@gmail.com
role = ROLE_USER
```

---

# 11. Logging

```java
log.info(
    "Successfully registered new user with ID: {}",
    savedUser.getId()
);
```

Backend console:

```text
Successfully registered new user with ID: 101
```

---

# 12. `generateAuthResponse()`

```java
return generateAuthResponse(savedUser);
```

Registration successful hone ke baad user ko manually login karne ki zarurat nahi.

Immediately:

```text
Access Token
+
Refresh Token
```

mil jayega.

---

# 13. `login()`

```java
public AuthResponse login(AuthRequest request)
```

Iska kaam:

> Existing user ke credentials verify karke tokens generate karna.

Flow:

```text
Email + Password
       ↓
AuthenticationManager
       ↓
Credentials valid?
       ↓
Find User
       ↓
Generate JWT
       ↓
Store Refresh Token
       ↓
Response
```

---

# 14. Email normalize karna

```java
String email =
    request.getEmail()
        .toLowerCase()
        .trim();
```

Example:

```text
" JATIN@GMAIL.COM "
```

becomes:

```text
jatin@gmail.com
```

---

# 15. `AuthenticationManager`

```java
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        email,
        request.getPassword()
    )
);
```

Ye login credentials verify karne ka main step hai.

Suppose:

```text
Email: jatin@gmail.com
Password: Password123
```

Spring Security check karega:

```text
User exists?
       ↓
Password correct?
       ↓
Account valid?
```

Agar correct:

```text
Authentication SUCCESS
```

---

# 16. `catch`

```java
catch (Exception ex) {
```

Agar authentication fail:

```text
wrong password
wrong credentials
```

etc.

to catch execute hoga.

---

```java
log.warn(
    "Authentication failed for email: {}",
    email
);
```

Log:

```text
Authentication failed for email: jatin@gmail.com
```

---

```java
throw new BadCredentialsException(
    "Invalid email or password"
);
```

Frontend ko generic message milta hai:

```text
Invalid email or password
```

Instead of exposing:

```text
Password incorrect
```

or whether a particular email exists.

---

# 17. User database se find

Authentication successful hone ke baad:

```java
User user =
    userRepository.findByEmail(email)
```

Database mein user find karega.

---

```java
.orElseThrow(
    () -> new ResourceNotFoundException(
        "User",
        "email",
        email
    )
);
```

Agar user somehow nahi mila:

```text
ResourceNotFoundException
```

throw hoga.

---

# 18. Login successful

```java
log.info(
    "User successfully authenticated: ID {}",
    user.getId()
);
```

Example:

```text
User successfully authenticated: ID 101
```

Then:

```java
return generateAuthResponse(user);
```

Access + refresh token generate honge.

---

# 19. `refreshToken()`

Ye method important hai.

Access token short-lived ho sakta hai.

Example:

```text
Access Token = 15 minutes
Refresh Token = 7 days
```

15 minutes ke baad user ko password dobara enter nahi karna chahiye.

Frontend refresh token bhejega:

```text
POST /auth/refresh
```

Then:

```text
Refresh Token
      ↓
Redis
      ↓
User ID
      ↓
Generate new Access Token
```

---

# 20. Redis key banana

```java
String tokenKey =
    REFRESH_TOKEN_KEY_PREFIX
    + request.getRefreshToken();
```

Suppose:

```text
refreshToken = abc123
```

Then:

```text
tokenKey = refresh_token:abc123
```

---

# 21. Redis se user ID nikalna

```java
String userIdStr =
    stringRedisTemplate
        .opsForValue()
        .get(tokenKey);
```

Redis:

```text
KEY:
refresh_token:abc123

VALUE:
101
```

To:

```text
userIdStr = "101"
```

---

# 22. Token invalid/expired check

```java
if (userIdStr == null) {
    throw new BadCredentialsException(
        "Invalid or expired refresh token"
    );
}
```

Agar Redis mein token nahi mila:

```text
Token expired
       OR
Token invalid
       OR
Token already used
       OR
Token logged out
```

Then:

```text
BadCredentialsException
```

---

# 23. String → Long

```java
Long userId =
    Long.parseLong(userIdStr);
```

Redis value:

```text
"101"
```

String hai.

Convert:

```text
101
```

Long mein.

---

# 24. User find

```java
User user =
    userRepository.findById(userId)
```

Database:

```text
ID = 101
```

user find karega.

Agar nahi mila:

```java
ResourceNotFoundException
```

---

# 25. Refresh token rotation

Sabse important security feature:

```java
stringRedisTemplate.delete(tokenKey);
```

Suppose old token:

```text
RT-ABC
```

use hua.

Ye token delete ho jayega.

Then:

```text
Old Refresh Token
       ↓
DELETE
       ↓
New Access Token
+
New Refresh Token
```

Isko **Refresh Token Rotation** kehte hain.

### Why?

Agar attacker kisi old refresh token ko chura bhi le aur wo already use ho chuka hai, to old token Redis mein available nahi hoga.

---

# 26. New authentication response

```java
return generateAuthResponse(user);
```

New:

```text
Access Token
+
Refresh Token
```

generate hoga.

---

# 27. `logout()`

```java
public void logout(String refreshToken)
```

Logout ke time refresh token invalidate karta hai.

Example:

```text
User clicks Logout
       ↓
Frontend sends refreshToken
       ↓
AuthService.logout()
       ↓
Redis token delete
```

---

# 28. Null/blank check

```java
if (refreshToken != null
        && !refreshToken.isBlank()) {
```

Agar:

```text
null
```

ya:

```text
"   "
```

hai, Redis operation nahi karega.

---

# 29. Redis key

```java
String tokenKey =
    REFRESH_TOKEN_KEY_PREFIX
    + refreshToken;
```

Example:

```text
refresh_token:abc123
```

---

# 30. Delete token

```java
stringRedisTemplate.delete(tokenKey);
```

Redis:

Before:

```text
refresh_token:abc123 → 101
```

After logout:

```text
refresh_token:abc123 → DELETED
```

Ab old refresh token use karke new access token nahi banaya ja sakta.

---

# 31. `generateAuthResponse()`

Ye private helper method hai:

```java
private AuthResponse generateAuthResponse(
    User user)
```

Registration aur login dono mein same token-generation logic chahiye.

Instead of duplicate code:

```text
register()
   ↓
generateAuthResponse()

login()
   ↓
generateAuthResponse()

refreshToken()
   ↓
generateAuthResponse()
```

---

# 32. Access token

```java
String accessToken =
    jwtProvider.generateAccessToken(user);
```

JWT generate hota hai.

Conceptually:

```text
User
 ↓
JwtProvider
 ↓
Access Token
```

Access token API requests mein use hoga:

```http
Authorization: Bearer <access-token>
```

---

# 33. Refresh token

```java
String refreshToken =
    jwtProvider.generateRefreshToken();
```

New refresh token generate hota hai.

---

# 34. Redis key

```java
String redisKey =
    REFRESH_TOKEN_KEY_PREFIX
    + refreshToken;
```

Example:

```text
refresh_token:XYZ123
```

---

# 35. Redis mein token store

```java
stringRedisTemplate.opsForValue().set(
    redisKey,
    String.valueOf(user.getId()),
    Duration.ofMillis(
        jwtProvider.getRefreshTokenExpirationMs()
    )
);
```

Ye bahut important hai.

Redis mein:

```text
KEY:
refresh_token:XYZ123

VALUE:
101

TTL:
7 days
```

Example:

```text
refresh_token:XYZ123
        ↓
       101
        ↓
      TTL 7d
```

---

# 36. TTL kya hai?

TTL = **Time To Live**

Suppose refresh token expiry:

```text
7 days
```

hai.

Redis automatically 7 days ke baad key delete kar dega.

```text
Day 1 → token exists
Day 5 → token exists
Day 7 → token expires
       ↓
Redis automatically deletes
```

Manual cleanup job ki zarurat nahi.

---

# 37. `AuthResponse.of()`

Finally:

```java
return AuthResponse.of(
    accessToken,
    refreshToken,
    jwtProvider.getAccessTokenExpirationMs(),
    user.getId(),
    user.getEmail(),
    user.getFirstName(),
    user.getLastName(),
    user.getRoles()
);
```

Frontend ko complete authentication response milta hai.

Conceptually:

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "abc123...",
  "expiresIn": 900000,
  "userId": 101,
  "email": "jatin@gmail.com",
  "firstName": "Jatin",
  "lastName": "Kumar",
  "roles": [
    "ROLE_USER"
  ]
}
```

---

# Complete Authentication Flow

## Registration

```text
POST /auth/register
        ↓
AuthController
        ↓
AuthService.register()
        ↓
Check duplicate email
        ↓
Hash password
        ↓
Create User
        ↓
PostgreSQL
        ↓
Generate Access Token
        ↓
Generate Refresh Token
        ↓
Redis
        ↓
AuthResponse
        ↓
Frontend
```

---

## Login

```text
Email + Password
        ↓
AuthService.login()
        ↓
AuthenticationManager
        ↓
Password verification
        ↓
User DB
        ↓
JWT Access Token
        ↓
Refresh Token
        ↓
Redis
        ↓
AuthResponse
```

---

## Refresh

```text
Expired Access Token
        ↓
Refresh Token
        ↓
AuthService.refreshToken()
        ↓
Redis lookup
        ↓
User ID
        ↓
Delete old refresh token
        ↓
Generate new token pair
        ↓
Store new refresh token
        ↓
Return AuthResponse
```

---

## Logout

```text
Logout
  ↓
Refresh Token
  ↓
AuthService.logout()
  ↓
Redis DELETE
  ↓
Session invalidated
```

---

# Tumhare Flash-Sale project mein iska role

Ye service directly flash-sale inventory handle nahi karti. Iska responsibility **identity and session management** hai:

```text
                    Auth Service
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
    Register           Login           Logout
        │                │                │
        ↓                ↓                ↓
   PostgreSQL       JWT + Redis       Redis
```

Aur future architecture mein:

```text
Frontend
   ↓
API Gateway
   ↓
Auth Service
   ├── PostgreSQL → User data
   └── Redis → Refresh tokens
```

**Sabse important distinction:** JWT access token generally stateless authentication ke liye use hota hai, jabki tumhara refresh token Redis mein stateful rakha gaya hai. Isse logout aur refresh-token rotation ko server-side invalidate karna possible hota hai.
*/