package com.flashsale.auth.config;

import com.flashsale.auth.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
/*Bilkul. Is `SecurityConfig.java` ko tumhare **Flash Sale microservices project** ke context mein simple Hinglish mein samjhte hain. Is file ka main kaam hai **Auth Service ke security rules define karna** — kaun login/register kar sakta hai, kaunse endpoints public hain, password kaise secure hoga, aur baaki requests ko authentication chahiye ya nahi.

---

# 1. Overall `SecurityConfig` ka kaam

Ye class basically Auth Service ka **security guard** hai.

```text
Frontend
   ↓
API Request
   ↓
SecurityConfig
   ↓
┌──────────────────────────┐
│ Public endpoint?         │
│ Authentication required? │
│ Password verification?   │
└──────────────────────────┘
   ↓
Allow / Reject
```

Example:

```http
POST /api/auth/login
```

→ Public hai → allow.

Lekin:

```http
GET /api/user/profile
```

→ Authentication required → token/authentication check hoga.

---

# 2. Class annotations

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
```

Ab ek-ek:

---

## `@Configuration`

```java
@Configuration
```

Spring ko batata hai:

> Ye class application ki configuration define karti hai.

Is class ke andar jo `@Bean` methods hain, Spring unhe application context mein register karega.

Example:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

Spring automatically `PasswordEncoder` bean bana dega.

---

# 3. `@EnableWebSecurity`

```java
@EnableWebSecurity
```

Ye Spring Security ko enable karta hai.

Matlab Spring application ke HTTP requests ke security rules apply karega.

Without security configuration, tumhare authentication/authorization rules properly establish nahi honge.

---

# 4. `@EnableMethodSecurity`

```java
@EnableMethodSecurity
```

Ye **method-level security** enable karta hai.

For example, future mein:

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteProduct(Long productId) {
    ...
}
```

Iska matlab:

> Sirf ADMIN role wala user ye method execute kar sakta hai.

Tumhare project mein useful example:

```text
ADMIN
 ↓
delete product
```

Normal user:

```text
USER
 ↓
delete product
 ↓
DENIED
```

---

# 5. `@RequiredArgsConstructor`

```java
@RequiredArgsConstructor
```

Ye Lombok annotation hai.

Tumhare class mein:

```java
private final UserDetailsServiceImpl userDetailsService;
```

hai.

Lombok automatically constructor generate kar dega:

```java
public SecurityConfig(
    UserDetailsServiceImpl userDetailsService
) {
    this.userDetailsService = userDetailsService;
}
```

Isliye manually constructor likhne ki need nahi.

---

# 6. `UserDetailsServiceImpl`

```java
private final UserDetailsServiceImpl userDetailsService;
```

Ye service Spring Security ko batati hai:

> User ka username/email database mein kaise find karna hai.

Flow:

```text
Login
 ↓
AuthenticationManager
 ↓
UserDetailsServiceImpl
 ↓
UserRepository
 ↓
Database
 ↓
User mila?
 ↓
Password verify
```

Example:

User login karta hai:

```json
{
  "email": "jatin@example.com",
  "password": "mypassword"
}
```

`UserDetailsServiceImpl` database mein email search karegi.

---

# 7. `passwordEncoder()`

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

Ye password ko securely hash karne ke liye hai.

### Important

Password ko database mein plain text mein **kabhi store nahi karna**.

Bad:

```text
password = mypassword123
```

Better:

```text
password = $2a$12$....
```

BCrypt password ko hash karta hai.

---

## `12` kya hai?

```java
new BCryptPasswordEncoder(12)
```

`12` work factor/cost hai.

Higher cost generally hashing ko computationally more expensive banata hai, jisse brute-force attacks harder ho sakte hain.

Example:

```text
User password
      ↓
BCrypt
      ↓
Hashed password
      ↓
Database
```

Login ke time:

```text
Entered password
      ↓
BCrypt verification
      ↓
Stored hash se compare
```

Password ko decrypt nahi kiya jata; hash verify kiya jata hai.

---

# 8. `authenticationProvider()`

```java
@Bean
public AuthenticationProvider authenticationProvider() {
```

Ye Spring Security ko batata hai:

> User ko authenticate karne ke liye kaunsa mechanism use karna hai.

---

## `DaoAuthenticationProvider`

```java
DaoAuthenticationProvider authProvider =
    new DaoAuthenticationProvider();
```

`DaoAuthenticationProvider` database-backed user authentication ke liye use hota hai.

`DAO` ko yahan broadly database/data-access based authentication samajh sakte ho.

---

# 9. `setUserDetailsService()`

```java
authProvider.setUserDetailsService(
    userDetailsService
);
```

Spring Security ko batata hai:

> User ki information `UserDetailsServiceImpl` se retrieve karo.

Flow:

```text
Email
 ↓
UserDetailsServiceImpl
 ↓
UserRepository
 ↓
PostgreSQL
 ↓
UserDetails
```

---

# 10. `setPasswordEncoder()`

```java
authProvider.setPasswordEncoder(
    passwordEncoder()
);
```

Spring Security ko batata hai:

> Password verification ke liye BCryptPasswordEncoder use karo.

Example:

```text
User enters:
mypassword123

        ↓

BCrypt verification

        ↓

Database hash

        ↓

MATCH?
```

Match hua:

```text
Authentication SUCCESS
```

Match nahi hua:

```text
Authentication FAILED
```

---

# 11. `authenticationManager()`

```java
@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config)
        throws Exception {

    return config.getAuthenticationManager();
}
```

`AuthenticationManager` actual authentication process ko coordinate karta hai.

Example login:

```text
POST /api/auth/login
        ↓
AuthService
        ↓
AuthenticationManager
        ↓
AuthenticationProvider
        ↓
UserDetailsServiceImpl
        ↓
Database
        ↓
PasswordEncoder
        ↓
SUCCESS / FAILURE
```

### Example

User:

```text
email = jatin@example.com
password = 12345678
```

AuthenticationManager verify karwata hai:

```text
User exists?        YES
Password correct?   YES
                    ↓
              Authenticated
```

Then tumhara authentication service future mein JWT generate kar sakta hai.

---

# 12. `securityFilterChain()`

Ye **sabse important method** hai.

```java
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http) throws Exception {
```

Ye define karta hai:

> HTTP requests ke security rules kya honge?

---

# 13. CSRF disable

```java
.csrf(AbstractHttpConfigurer::disable)
```

CSRF = **Cross-Site Request Forgery**.

Tumhara architecture REST API + stateless authentication use kar raha hai.

Is configuration mein CSRF disable kiya gaya hai.

Simple concept:

```text
REST API
   +
Stateless authentication
   ↓
CSRF disabled
```

**Note:** CSRF ko blindly disable karna har architecture mein correct nahi hota; yahan given stateless API design ke context mein kiya gaya hai.

---

# 14. Stateless session

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(
        SessionCreationPolicy.STATELESS
    )
)
```

Ye tumhare project ke liye important hai.

`STATELESS` ka matlab:

> Server HTTP session mein user login state store nahi karega.

Traditional session-based system:

```text
Login
 ↓
Server Session
 ↓
Session ID
 ↓
Requests
```

Tumhare token-based architecture mein:

```text
Login
 ↓
JWT
 ↓
Client
 ↓
Every request
 ↓
JWT
```

Example:

```http
Authorization: Bearer eyJhbGciOi...
```

Server ko har request mein token se authentication establish karni hoti hai.

---

# 15. `authorizeHttpRequests()`

```java
.authorizeHttpRequests(auth -> auth
```

Yahan tum define karte ho:

> Kaunse URLs public hain aur kaunse protected.

---

# 16. Public endpoints

```java
.requestMatchers(
    "/api/auth/**",
    "/actuator/**",
    "/v3/api-docs/**",
    "/swagger-ui/**"
).permitAll()
```

`permitAll()` means:

> In endpoints ko authentication ke bina access kar sakte ho.

### `/api/auth/**`

Iska matlab auth-related endpoints.

For example:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
```

Login ke liye token abhi hai hi nahi, isliye login ko public hona chahiye.

Registration bhi public honi chahiye.

---

# 17. `/actuator/**`

```text
/actuator/**
```

Spring Boot Actuator endpoints ke liye hai.

For example:

```text
/actuator/health
```

Application health check ke liye.

Example:

```text
GET /actuator/health
```

Response:

```json
{
  "status": "UP"
}
```

Tumhari architecture mein monitoring/health checks ke liye useful hai.

---

# 18. `/v3/api-docs/**`

Ye OpenAPI documentation endpoint hai.

Example:

```text
/v3/api-docs
```

Swagger/OpenAPI specification provide karta hai.

---

# 19. `/swagger-ui/**`

Swagger UI ko access karne deta hai.

Example:

```text
/swagger-ui/index.html
```

Isse API ko browser mein interactively test/document kar sakte ho.

---

# 20. `anyRequest().authenticated()`

```java
.anyRequest().authenticated()
```

Ye **bahut important security rule** hai.

Meaning:

> Jo endpoint upar public list mein nahi hai, uske liye authentication required hai.

Example:

```http
GET /api/users/profile
```

Public list mein nahi hai.

Therefore:

```text
Authentication required
```

Without authentication:

```text
401 Unauthorized
```

---

# 21. `authenticationProvider()`

```java
.authenticationProvider(
    authenticationProvider()
);
```

Yahan configured authentication provider ko Spring Security ke filter chain ke saath associate kiya ja raha hai.

Basically:

```text
SecurityFilterChain
       ↓
AuthenticationProvider
       ↓
UserDetailsService
       ↓
PasswordEncoder
```

---

# 22. `http.build()`

```java
return http.build();
```

Finally configured `HttpSecurity` ko actual `SecurityFilterChain` mein build karta hai.

Matlab tumhari saari settings:

```text
CSRF
Session
Public URLs
Protected URLs
Authentication Provider
```

combine hokar final security configuration ban jaati hain.

---

# Complete login example

Suppose user login karta hai:

```http
POST /api/auth/login
```

### Step 1

Security checks:

```text
/api/auth/** ?
```

Yes.

```text
permitAll()
```

So request allowed to reach login logic.

### Step 2

Auth service:

```text
AuthenticationManager
```

ko credentials deta hai.

### Step 3

AuthenticationManager:

```text
AuthenticationProvider
```

ko use karta hai.

### Step 4

Provider:

```text
UserDetailsServiceImpl
```

ko call karta hai.

### Step 5

User database se milta hai.

### Step 6

BCrypt password verify hota hai.

### Step 7

Password correct:

```text
Authentication SUCCESS
```

### Step 8

Tumhara authentication logic JWT generate kar sakta hai.

```text
Login
 ↓
Authentication
 ↓
JWT
 ↓
Frontend
```

---

# Protected request example

Ab user ke paas JWT hai.

Frontend:

```http
GET /api/orders
Authorization: Bearer <JWT>
```

Flow:

```text
Request
  ↓
Spring Security
  ↓
Authentication check
  ↓
JWT/token authentication
  ↓
Authenticated?
  ↓
YES
  ↓
Order Controller
```

Agar authentication nahi hai:

```text
Request
  ↓
Security
  ↓
Not authenticated
  ↓
401 Unauthorized
```

---

# `SecurityConfig` ka complete role

```text
                  SecurityConfig
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   Password        Authentication   HTTP Security
    Encoder          Provider          Rules
        │              │              │
      BCrypt      UserDetails      Public/Protected
                       │              │
                  UserRepository      │
                       │              │
                   Database            │
                                      │
                              Stateless Session
```

### Methods ka quick summary

| Method                     | Main kaam                                                                      |
| -------------------------- | ------------------------------------------------------------------------------ |
| `passwordEncoder()`        | BCrypt se password hashing/verification                                        |
| `authenticationProvider()` | UserDetailsService + BCrypt ko authentication mechanism mein connect karta hai |
| `authenticationManager()`  | Authentication process ko coordinate karta hai                                 |
| `securityFilterChain()`    | HTTP security rules define karta hai                                           |

**Sabse important concept:** `SecurityConfig` khud JWT generate nahi kar raha. Tumhare code mein `JwtProvider.java` alag responsibility rakhta hai. `SecurityConfig` mainly **Spring Security ko configure karta hai**, while authentication service/JWT provider login ke baad token-related work handle karenge.
*/