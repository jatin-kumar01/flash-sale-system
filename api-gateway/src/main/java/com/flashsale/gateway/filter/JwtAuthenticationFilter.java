//package com.flashsale.gateway.filter;
//
//import com.flashsale.common.security.SecurityConstants;
//import com.flashsale.gateway.config.JwtUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
//
//    private final JwtUtil jwtUtil;
//
//    private static final List<String> PUBLIC_URL_PREFIXES = List.of(
//            "/api/auth/",
//            "/actuator/",
//            "/swagger-ui",
//            "/v3/api-docs"
//    );
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getURI().getPath();
//
//        // 1. Whitelist open endpoints
//        if (isPublicEndpoint(path, request.getMethod())) {
//            return chain.filter(exchange);
//        }
//
//        // 2. Validate Authorization header presence
//        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//        if (authHeader == null || !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
//            log.warn("Missing or invalid Authorization header for path: {}", path);
//            return onError(exchange, HttpStatus.UNAUTHORIZED);
//        }
//
//        // 3. Extract and validate JWT
//        String token = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
//        if (!jwtUtil.validateToken(token)) {
//            log.warn("JWT validation failed for path: {}", path);
//            return onError(exchange, HttpStatus.UNAUTHORIZED);
//        }
//
//        // 4. Extract user identity and mutate request with downstream propagation headers
//        String userId = jwtUtil.extractUserId(token);
//        List<String> roles = jwtUtil.extractRoles(token);
//        String rolesHeaderValue = String.join(",", roles);
//
//        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
//                .header(SecurityConstants.USER_ID_HEADER, userId)
//                .header(SecurityConstants.USER_ROLES_HEADER, rolesHeaderValue)
//                .build();
//
//        return chain.filter(exchange.mutate().request(mutatedRequest).build());
//    }
//
//    private boolean isPublicEndpoint(String path, HttpMethod method) {
//        // Allow public product catalog browsing (GET /api/products/**)
//        if (path.startsWith("/api/products") && HttpMethod.GET.equals(method)) {
//            return true;
//        }
//
//        return PUBLIC_URL_PREFIXES.stream().anyMatch(path::startsWith);
//    }
//
//    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
//        ServerHttpResponse response = exchange.getResponse();
//        response.setStatusCode(httpStatus);
//        return response.setComplete();
//    }
//
//    @Override
//    public int getOrder() {
//        return -100; // High priority in the filter chain
//    }
//}
///*
//* Bilkul. Is `JwtAuthenticationFilter.java` ko tumhare **Flash Sale microservices project** ke context mein Hinglish mein samjhte hain. Ye file especially **API Gateway ke security layer** ka important part hai.
//
//---
//
//# 1. Sabse pehle — ye file karti kya hai?
//
//Simple language mein:
//
//> **Har incoming request ko downstream microservice tak jaane se pehle check karna ki user authenticated hai ya nahi.**
//
//Architecture:
//
//```text
//Frontend
//   ↓
//API Gateway
//   ↓
//JwtAuthenticationFilter
//   ↓
//JWT valid?
//   ├── NO → 401 Unauthorized
//   │
//   └── YES
//        ↓
//   User ID + Roles extract
//        ↓
//   Headers mein add
//        ↓
//   Microservice
//```
//
//Example:
//
//```text
//User → POST /api/orders
//              ↓
//        API Gateway
//              ↓
//       JWT check
//              ↓
//       Valid token?
//              ↓
//        Order Service
//```
//
//---
//
//# 2. Class declaration
//
//```java
//public class JwtAuthenticationFilter
//        implements GlobalFilter, Ordered {
//```
//
//Yahan class do interfaces implement kar rahi hai.
//
//### `GlobalFilter`
//
//```java
//GlobalFilter
//```
//
//iska matlab:
//
//> Ye filter Gateway se pass hone wali requests par globally apply ho sakta hai.
//
//Tumhe har route mein manually filter add nahi karna padega.
//
//---
//
//### `Ordered`
//
//```java
//Ordered
//```
//
//Ye decide karta hai ki filter **kis priority/order** par execute hoga.
//
//Neeche:
//
//```java
//@Override
//public int getOrder() {
//    return -100;
//}
//```
//
//hai.
//
//Iska matlab filter ko Gateway filter chain mein high priority par run karna hai.
//
//---
//
//# 3. `JwtUtil` inject karna
//
//```java
//private final JwtUtil jwtUtil;
//```
//
//`JwtUtil` ka kaam JWT ke saath related operations karna hai, jaise:
//
//```text
//Token validate karna
//User ID nikalna
//Roles nikalna
//```
//
//Aur:
//
//```java
//@RequiredArgsConstructor
//```
//
//Lombok automatically constructor generate karta hai.
//
//Conceptually:
//
//```java
//public JwtAuthenticationFilter(JwtUtil jwtUtil) {
//    this.jwtUtil = jwtUtil;
//}
//```
//
//likhne ki zarurat nahi padti.
//
//---
//
//# 4. Public URL list
//
//```java
//private static final List<String> PUBLIC_URL_PREFIXES = List.of(
//        "/api/auth/",
//        "/actuator/",
//        "/swagger-ui",
//        "/v3/api-docs"
//);
//```
//
//Ye un URLs ki list hai jahan JWT authentication ki requirement nahi hogi.
//
//### Example
//
//```text
///api/auth/login
///api/auth/register
//```
//
//Login ke time user ke paas JWT hota hi nahi.
//
//Agar login endpoint par JWT mandatory kar doge:
//
//```text
//Login → JWT chahiye
//JWT → Login ke baad milega
//```
//
//Circular problem ho jayegi.
//
//Isliye auth endpoints public hain.
//
//---
//
//# 5. Main `filter()` method
//
//Ye class ki **main method** hai:
//
//```java
//@Override
//public Mono<Void> filter(
//        ServerWebExchange exchange,
//        GatewayFilterChain chain) {
//```
//
//Ye incoming request ko process karti hai.
//
//---
//
//## `ServerWebExchange`
//
//```java
//ServerWebExchange exchange
//```
//
//Iske andar request aur response dono ka context hota hai.
//
//Simple:
//
//```text
//exchange
// ├── request
// └── response
//```
//
//---
//
//## `GatewayFilterChain`
//
//```java
//GatewayFilterChain chain
//```
//
//Ye next filter/downstream route ko continue karne ke liye use hota hai.
//
//Agar request valid hai:
//
//```java
//chain.filter(exchange)
//```
//
//request ko aage bhejega.
//
//Agar invalid hai:
//
//```text
//Request → STOP
//```
//
//---
//
//# 6. Request nikalna
//
//```java
//ServerHttpRequest request =
//        exchange.getRequest();
//```
//
//Incoming HTTP request mil rahi hai.
//
//Example:
//
//```http
//POST /api/orders
//Authorization: Bearer eyJhbGci...
//```
//
//---
//
//# 7. Request path nikalna
//
//```java
//String path =
//        request.getURI().getPath();
//```
//
//Agar request:
//
//```text
//POST /api/orders
//```
//
//hai, to:
//
//```text
//path = /api/orders
//```
//
//---
//
//# 8. Public endpoint check
//
//```java
//if (isPublicEndpoint(path, request.getMethod())) {
//    return chain.filter(exchange);
//}
//```
//
//Pehle check ho raha hai:
//
//> Kya ye public endpoint hai?
//
//Example:
//
//```text
//GET /api/products
//```
//
//Public hai.
//
//To:
//
//```text
//JWT check nahi
//      ↓
//Request directly आगे
//```
//
//---
//
//# 9. Authorization header nikalna
//
//Protected endpoint ke liye:
//
//```java
//String authHeader =
//        request.getHeaders()
//            .getFirst(HttpHeaders.AUTHORIZATION);
//```
//
//Example request:
//
//```http
//Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
//```
//
//To:
//
//```text
//authHeader =
//Bearer eyJhbGciOiJIUzI1NiJ9...
//```
//
//---
//
//# 10. Authorization header check
//
//```java
//if (authHeader == null ||
//    !authHeader.startsWith(
//        SecurityConstants.TOKEN_PREFIX)) {
//```
//
//Yahan do cheezein check ho rahi hain:
//
//### Check 1
//
//```java
//authHeader == null
//```
//
//Matlab Authorization header hai hi nahi.
//
//Example:
//
//```http
//POST /api/orders
//```
//
//without:
//
//```http
//Authorization: Bearer ...
//```
//
//→ reject.
//
//---
//
//### Check 2
//
//```java
//authHeader.startsWith("Bearer ")
//```
//
//assuming:
//
//```java
//SecurityConstants.TOKEN_PREFIX
//```
//
//ki value:
//
//```text
//Bearer
//```
//
//ya:
//
//```text
//Bearer
//```
//
//hai.
//
//Agar user bhejta hai:
//
//```http
//Authorization: Basic abc123
//```
//
//to reject.
//
//---
//
//# 11. Missing/invalid token par 401
//
//```java
//log.warn(
//    "Missing or invalid Authorization header for path: {}",
//    path
//);
//```
//
//Backend log mein warning.
//
//Then:
//
//```java
//return onError(
//    exchange,
//    HttpStatus.UNAUTHORIZED
//);
//```
//
//Response:
//
//```http
//401 Unauthorized
//```
//
//### Example
//
//```text
//Frontend
//   ↓
//POST /api/orders
//   ↓
//No JWT
//   ↓
//Gateway
//   ↓
//401 Unauthorized
//```
//
//Request Order Service tak jayegi hi nahi.
//
//---
//
//# 12. JWT extract karna
//
//```java
//String token =
//    authHeader
//        .substring(
//            SecurityConstants.TOKEN_PREFIX.length()
//        )
//        .trim();
//```
//
//Suppose:
//
//```text
//authHeader =
//Bearer ABC123XYZ
//```
//
//Prefix:
//
//```text
//Bearer
//```
//
//remove karne ke baad:
//
//```text
//token = ABC123XYZ
//```
//
//Ab sirf JWT token bacha.
//
//---
//
//# 13. JWT validate karna
//
//```java
//if (!jwtUtil.validateToken(token)) {
//```
//
//`JwtUtil` check karega ki token valid hai ya nahi.
//
//Conceptually:
//
//```text
//JWT
// ↓
//Signature valid?
// ↓
//Expired?
// ↓
//Correct structure?
// ↓
//Valid?
//```
//
//Agar invalid:
//
//```java
//log.warn(
//    "JWT validation failed for path: {}",
//    path
//);
//```
//
//Then:
//
//```java
//return onError(
//    exchange,
//    HttpStatus.UNAUTHORIZED
//);
//```
//
//Result:
//
//```text
//401 Unauthorized
//```
//
//---
//
//# 14. Valid JWT hone ke baad
//
//Agar:
//
//```java
//jwtUtil.validateToken(token)
//```
//
//`true` return karta hai, request aage process hogi.
//
//Ab user ki identity nikalni hai.
//
//---
//
//# 15. User ID extract karna
//
//```java
//String userId =
//        jwtUtil.extractUserId(token);
//```
//
//Suppose JWT claims:
//
//```json
//{
//  "userId": "USER-101",
//  "roles": ["USER"]
//}
//```
//
//To:
//
//```text
//userId = USER-101
//```
//
//---
//
//# 16. Roles extract karna
//
//```java
//List<String> roles =
//        jwtUtil.extractRoles(token);
//```
//
//Suppose JWT:
//
//```json
//{
//  "userId": "USER-101",
//  "roles": [
//    "USER",
//    "CUSTOMER"
//  ]
//}
//```
//
//To:
//
//```text
//roles =
//[
//  USER,
//  CUSTOMER
//]
//```
//
//---
//
//# 17. Roles ko String banana
//
//```java
//String rolesHeaderValue =
//        String.join(",", roles);
//```
//
//List:
//
//```text
//[USER, CUSTOMER]
//```
//
//convert:
//
//```text
//USER,CUSTOMER
//```
//
//Kyun?
//
//Kyuki HTTP header mein simple string bhejna convenient hai.
//
//---
//
//# 18. Request mutate karna
//
//Ab important part:
//
//```java
//ServerHttpRequest mutatedRequest =
//        exchange.getRequest()
//            .mutate()
//            .header(
//                SecurityConstants.USER_ID_HEADER,
//                userId
//            )
//            .header(
//                SecurityConstants.USER_ROLES_HEADER,
//                rolesHeaderValue
//            )
//            .build();
//```
//
//Original request mein do headers add ho rahe hain.
//
//For example:
//
//```http
//X-User-Id: USER-101
//X-User-Roles: USER,CUSTOMER
//```
//
//---
//
//# 19. Ye headers kyu useful hain?
//
//Tumhara architecture:
//
//```text
//Frontend
//   ↓
//API Gateway
//   ↓
//Order Service
//```
//
//Gateway already JWT validate kar chuka hai.
//
//Ab Order Service ko baar-baar JWT parse karne ki zarurat nahi ho sakti, depending on your security design.
//
//Gateway verified identity forward kar sakta hai:
//
//```text
//X-User-Id
//X-User-Roles
//```
//
//Then:
//
//```text
//Order Service
//     ↓
//X-User-Id = USER-101
//X-User-Roles = USER,CUSTOMER
//```
//
//se context use kar sakta hai.
//
//---
//
//# 20. Downstream request bhejna
//
//```java
//return chain.filter(
//    exchange
//        .mutate()
//        .request(mutatedRequest)
//        .build()
//);
//```
//
//Ab modified request next filter/downstream service ko bheji ja rahi hai.
//
//Flow:
//
//```text
//Original Request
//       ↓
//JWT Validate
//       ↓
//Extract User ID
//       ↓
//Extract Roles
//       ↓
//Add Headers
//       ↓
//Order Service
//```
//
//---
//
//# 21. `isPublicEndpoint()` method
//
//```java
//private boolean isPublicEndpoint(
//        String path,
//        HttpMethod method) {
//```
//
//Is method ka kaam hai decide karna:
//
//> Kya current request ko authentication ke bina allow karna hai?
//
//---
//
//## Product GET check
//
//```java
//if (path.startsWith("/api/products") &&
//    HttpMethod.GET.equals(method)) {
//    return true;
//}
//```
//
//Example:
//
//```http
//GET /api/products
//```
//
//→ public.
//
//```http
//GET /api/products/101
//```
//
//→ public.
//
//But:
//
//```http
//POST /api/products
//```
//
//→ public nahi.
//
//---
//
//# 22. Important flash-sale example
//
//Tumhare system mein product browsing public ho sakti hai:
//
//```text
//GET /api/products
//```
//
//User bina login products dekh sakta hai.
//
//But order:
//
//```text
//POST /api/orders
//```
//
//protected hoga.
//
//So:
//
//```text
//GET /api/products
//       ↓
//No JWT required
//       ↓
//Product Service
//```
//
//but:
//
//```text
//POST /api/orders
//       ↓
//JWT required
//       ↓
//Order Service
//```
//
//---
//
//# 23. Public prefix check
//
//```java
//return PUBLIC_URL_PREFIXES
//        .stream()
//        .anyMatch(path::startsWith);
//```
//
//List:
//
//```text
///api/auth/
///actuator/
///swagger-ui
///v3/api-docs
//```
//
//Suppose:
//
//```text
///api/auth/login
//```
//
//starts with:
//
//```text
///api/auth/
//```
//
//→ `true`
//
//Therefore public.
//
//---
//
//# 24. `onError()` method
//
//```java
//private Mono<Void> onError(
//        ServerWebExchange exchange,
//        HttpStatus httpStatus) {
//```
//
//Ye unauthorized/error response create karne ke liye helper method hai.
//
//---
//
//## Response obtain karna
//
//```java
//ServerHttpResponse response =
//        exchange.getResponse();
//```
//
//Current HTTP response milta hai.
//
//---
//
//## Status set karna
//
//```java
//response.setStatusCode(httpStatus);
//```
//
//Example:
//
//```java
//HttpStatus.UNAUTHORIZED
//```
//
//means:
//
//```http
//401 Unauthorized
//```
//
//---
//
//## Response complete karna
//
//```java
//return response.setComplete();
//```
//
//Matlab:
//
//> Request yahin finish karo; downstream service ko mat bhejo.
//
//---
//
//# 25. `getOrder()`
//
//```java
//@Override
//public int getOrder() {
//    return -100;
//}
//```
//
//Ye filter ki priority set karta hai.
//
//```text
//Lower order value
//      ↓
//Higher priority
//```
//
//`-100` ka purpose hai filter ko early execute karna.
//
//So ideally:
//
//```text
//Request
// ↓
//JWT Filter
// ↓
//Authentication
// ↓
//Routing
// ↓
//Microservice
//```
//
//Invalid request ko routing tak pahunchne se pehle stop karna useful hai.
//
//---
//
//# 26. Complete example — valid request
//
//Suppose user already logged in hai.
//
//Frontend:
//
//```http
//POST /api/orders
//Authorization: Bearer eyJ...
//```
//
//Gateway:
//
//```text
//        Request
//           ↓
// JwtAuthenticationFilter
//           ↓
// Authorization header?
//           ↓
//         YES
//           ↓
//      Extract JWT
//           ↓
//      Validate JWT
//           ↓
//         VALID
//           ↓
//   Extract userId = 101
//           ↓
//   Extract roles = USER
//           ↓
// Add X-User-Id = 101
// Add X-User-Roles = USER
//           ↓
//      Order Service
//```
//
//---
//
//# 27. Invalid token example
//
//```http
//POST /api/orders
//Authorization: Bearer abc-invalid
//```
//
//Flow:
//
//```text
//Request
//  ↓
//Gateway
//  ↓
//JWT validation
//  ↓
//INVALID
//  ↓
//401 Unauthorized
//```
//
//Order Service ko request milegi hi nahi.
//
//---
//
//# 28. No token example
//
//```http
//POST /api/orders
//```
//
//Flow:
//
//```text
//Request
//  ↓
//Authorization header missing
//  ↓
//401 Unauthorized
//```
//
//---
//
//# 29. Public request example
//
//```http
//GET /api/products
//```
//
//Flow:
//
//```text
//Request
//  ↓
//isPublicEndpoint()
//  ↓
//TRUE
//  ↓
//JWT check skip
//  ↓
//Product Service
//```
//
//---
//
//# 30. Overall class ka purpose
//
//```text
//                 API GATEWAY
//                     │
//              Incoming Request
//                     │
//                     ▼
//        JwtAuthenticationFilter
//                     │
//          ┌──────────┴──────────┐
//          │                     │
//       Public?                Protected
//          │                     │
//         YES                    ▼
//          │                 JWT exists?
//          │                     │
//          │                ┌────┴────┐
//          │               NO         YES
//          │                │           │
//          │               401      Validate JWT
//          │                           │
//          │                      ┌────┴────┐
//          │                    Invalid    Valid
//          │                       │          │
//          │                      401         ▼
//          │                              Extract User
//          │                              + Roles
//          │                                  │
//          └──────────────────────────────────┤
//                                             ▼
//                                      Downstream Service
//```
//
//### Ek important security point
//
//`X-User-Id` aur `X-User-Roles` **trusted identity headers** hain. Production architecture mein downstream services ko ensure karna chahiye ki clients directly ye headers inject karke Gateway ko bypass na kar saken. Ideally services sirf trusted Gateway/internal traffic accept karein, aur Gateway incoming client-supplied identity headers ko overwrite/remove kare.
//
//**Short mein:** `JwtAuthenticationFilter` tumhare system ka **API Gateway security gatekeeper** hai — public requests ko allow karta hai, protected requests ka JWT verify karta hai, invalid requests ko `401` deta hai, aur valid user ki identity/roles ko downstream services tak propagate karta hai.
//*/
//
//
//
//
//package com.flashsale.gateway.filter;
//
//import com.flashsale.common.security.SecurityConstants;
//import com.flashsale.gateway.config.JwtUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
//
//    private final JwtUtil jwtUtil;
//
//    private static final List<String> PUBLIC_URL_PREFIXES = List.of(
//            "/api/auth/",
//            "/actuator/",
//            "/swagger-ui",
//            "/v3/api-docs"
//    );
//
//    @Override
//    public Mono<Void> filter(
//            ServerWebExchange exchange,
//            GatewayFilterChain chain) {
//
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getURI().getPath();
//
//        // 1. Allow CORS preflight requests
//        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
//            log.debug("Allowing CORS preflight request for path: {}", path);
//            return chain.filter(exchange);
//        }
//
//        // 2. Allow public endpoints
//        if (isPublicEndpoint(path, request.getMethod())) {
//            return chain.filter(exchange);
//        }
//
//        // 3. Validate Authorization header
//        String authHeader = request.getHeaders()
//                .getFirst(HttpHeaders.AUTHORIZATION);
//
//        if (authHeader == null ||
//                !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
//
//            log.warn(
//                    "Missing or invalid Authorization header for path: {}",
//                    path
//            );
//
//            return onError(exchange, HttpStatus.UNAUTHORIZED);
//        }
//
//        // 4. Extract JWT token
//        String token = authHeader
//                .substring(SecurityConstants.TOKEN_PREFIX.length())
//                .trim();
//
//        // 5. Validate JWT
//        if (!jwtUtil.validateToken(token)) {
//
//            log.warn(
//                    "JWT validation failed for path: {}",
//                    path
//            );
//
//            return onError(exchange, HttpStatus.UNAUTHORIZED);
//        }
//
//        // 6. Extract user information
//        String userId = jwtUtil.extractUserId(token);
//        List<String> roles = jwtUtil.extractRoles(token);
//
//        String rolesHeaderValue = String.join(",", roles);
//
//        // 7. Add trusted user information to request
//        ServerHttpRequest mutatedRequest = request.mutate()
//                .header(
//                        SecurityConstants.USER_ID_HEADER,
//                        userId
//                )
//                .header(
//                        SecurityConstants.USER_ROLES_HEADER,
//                        rolesHeaderValue
//                )
//                .build();
//
//        // 8. Continue request
//        return chain.filter(
//                exchange.mutate()
//                        .request(mutatedRequest)
//                        .build()
//        );
//    }
//
//    private boolean isPublicEndpoint(
//            String path,
//            HttpMethod method) {
//
//        // Public product catalog
//        if (path.startsWith("/api/products")
//                && HttpMethod.GET.equals(method)) {
//            return true;
//        }
//
//        return PUBLIC_URL_PREFIXES.stream()
//                .anyMatch(path::startsWith);
//    }
//
//    private Mono<Void> onError(
//            ServerWebExchange exchange,
//            HttpStatus httpStatus) {
//
//        ServerHttpResponse response = exchange.getResponse();
//
//        response.setStatusCode(httpStatus);
//
//        return response.setComplete();
//    }
//
//    @Override
//    public int getOrder() {
//        return -100;
//    }
//}



package com.flashsale.gateway.filter;

import com.flashsale.common.security.SecurityConstants;
import com.flashsale.gateway.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_URL_PREFIXES = List.of(
            "/api/auth/",
            "/actuator/",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Allow CORS preflight requests
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            log.debug("Allowing CORS preflight request for path: {}", path);
            return chain.filter(exchange);
        }

        // 2. Allow public endpoints
        if (isPublicEndpoint(path, request.getMethod())) {
            return chain.filter(exchange);
        }

        // 3. Get Authorization header
        String authHeader = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // 4. Reject request if Authorization header is missing
        if (authHeader == null
                || !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {

            log.warn(
                    "Missing or invalid Authorization header for path: {}",
                    path
            );

            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        // 5. Extract JWT token
        String token = authHeader
                .substring(SecurityConstants.TOKEN_PREFIX.length())
                .trim();

        // 6. Validate JWT
        if (!jwtUtil.validateToken(token)) {

            log.warn(
                    "JWT validation failed for path: {}",
                    path
            );

            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        // 7. Extract user information from JWT
        String userId = jwtUtil.extractUserId(token);
        List<String> roles = jwtUtil.extractRoles(token);

        String rolesHeaderValue = String.join(",", roles);

        // 8. Add trusted user information
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(
                        SecurityConstants.USER_ID_HEADER,
                        userId
                )
                .header(
                        SecurityConstants.USER_ROLES_HEADER,
                        rolesHeaderValue
                )
                .build();

        // 9. Continue request to downstream service
        return chain.filter(
                exchange.mutate()
                        .request(mutatedRequest)
                        .build()
        );
    }

    private boolean isPublicEndpoint(
            String path,
            HttpMethod method) {

        // Public product catalog - GET only
        if (path.startsWith("/api/products")
                && HttpMethod.GET.equals(method)) {
            return true;
        }

        // Authentication, actuator and API documentation
        return PUBLIC_URL_PREFIXES.stream()
                .anyMatch(path::startsWith);
    }

    private Mono<Void> onError(
            ServerWebExchange exchange,
            HttpStatus httpStatus) {

        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(httpStatus);

        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}