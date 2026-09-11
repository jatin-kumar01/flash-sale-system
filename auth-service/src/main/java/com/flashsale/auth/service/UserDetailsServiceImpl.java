package com.flashsale.auth.service;

import com.flashsale.auth.entity.User;
import com.flashsale.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User authentication failed: email not found -> {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .build();
    }
}
/*Bilkul. Ye `UserDetailsServiceImpl.java` **Auth Service ka important security component** hai. Iska main kaam hai: **login ke time email se user ko database se find karna aur Spring Security ko us user ki authentication information dena.**

Chalo **har part aur har method ko example ke saath** samjhte hain.

---

# 1. Overall kaam kya hai?

Login ke time flow kuch aisa hoga:

```text
User
 │
 │ email + password
 ▼
Auth Controller
 │
 ▼
AuthenticationManager
 │
 ▼
UserDetailsServiceImpl
 │
 ▼
UserRepository
 │
 ▼
PostgreSQL
 │
 ▼
User Entity
 │
 ▼
Spring Security UserDetails
 │
 ▼
Password verification + authorities
```

Example:

```text
Email: jatin@gmail.com
Password: ********
```

`UserDetailsServiceImpl` database mein:

```text
jatin@gmail.com
```

ko search karega.

Agar user mil gaya → authentication process continue.

Agar user nahi mila → `UsernameNotFoundException`.

---

# 2. `@Service`

```java
@Service
public class UserDetailsServiceImpl
        implements UserDetailsService {
```

`@Service` Spring ko batata hai:

> Is class ko Spring-managed service bean bana do.

Isliye Spring Security is class ko automatically use kar sakti hai.

Conceptually:

```text
Spring Application
       │
       └── UserDetailsServiceImpl Bean
```

---

# 3. `implements UserDetailsService`

```java
implements UserDetailsService
```

`UserDetailsService` Spring Security ka interface hai.

Iska main method hai:

```java
loadUserByUsername(...)
```

Spring Security authentication ke time is method ko use kar sakti hai.

Yahan "username" naam hone ke bawajood tumhare project mein **email ko username ke roop mein use kiya gaya hai**.

```text
username = email
```

---

# 4. `@RequiredArgsConstructor`

```java
@RequiredArgsConstructor
```

Ye Lombok annotation hai.

Tumhare paas:

```java
private final UserRepository userRepository;
```

hai.

Lombok automatically constructor generate kar deta hai approximately:

```java
public UserDetailsServiceImpl(
        UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

Spring isi constructor ke through `UserRepository` inject kar deta hai.

Isko **constructor dependency injection** kehte hain.

---

# 5. `userRepository`

```java
private final UserRepository userRepository;
```

Iska purpose:

> Database se User ko retrieve karna.

For example repository mein:

```java
Optional<User> findByEmail(String email);
```

hai.

To service:

```java
userRepository.findByEmail(email)
```

call karegi.

Flow:

```text
UserDetailsServiceImpl
        ↓
UserRepository
        ↓
PostgreSQL
```

---

# 6. Main method: `loadUserByUsername()`

```java
@Override
@Transactional(readOnly = true)
public UserDetails loadUserByUsername(
        String email)
        throws UsernameNotFoundException {
```

Ye **is file ka main method** hai.

Spring Security ise authentication ke time call karegi.

---

# 7. `@Override`

```java
@Override
```

Iska matlab:

> Main parent interface `UserDetailsService` ke method ko implement/override kar raha hoon.

Interface mein:

```java
UserDetails loadUserByUsername(String username)
```

defined hota hai.

Tum uska implementation yahan de rahe ho.

---

# 8. `@Transactional(readOnly = true)`

```java
@Transactional(readOnly = true)
```

Iska simple meaning:

> Ye database operation read-only hai; is method mein database data modify nahi karna hai.

Tum yahan sirf user read kar rahe ho:

```text
SELECT user ...
```

No:

```text
INSERT
UPDATE
DELETE
```

Expected operation:

```sql
SELECT * FROM users WHERE email = ?;
```

### `readOnly = true` kyu?

Ye intent clearly define karta hai ki transaction read operation ke liye hai.

**Important:** tumhare explanation mein "ensures session remains open while reading eager/lazy collections" likha hai, lekin `readOnly=true` ka primary purpose session-open guarantee nahi hai. Lazy-loading behavior transaction/session configuration par depend karta hai.

---

# 9. `String email`

```java
String email
```

Spring Security jo username provide karegi, tumhare project mein wo email hai.

Example:

```text
email = "jatin@gmail.com"
```

---

# 10. `findByEmail(email)`

```java
User user = userRepository.findByEmail(email)
```

Repository database mein user search karegi.

Suppose database:

| id | email                                     | passwordHash | enabled |
| -: | ----------------------------------------- | ------------ | ------- |
|  1 | [jatin@gmail.com](mailto:jatin@gmail.com) | `$2a$...`    | true    |
|  2 | [rahul@gmail.com](mailto:rahul@gmail.com) | `$2a$...`    | true    |

Request:

```text
email = jatin@gmail.com
```

Then:

```text
findByEmail()
      ↓
PostgreSQL
      ↓
User #1
```

---

# 11. `Optional<User>`

Usually repository method:

```java
Optional<User> findByEmail(String email);
```

return karti hai.

Because user:

```text
mil bhi sakta hai
```

aur:

```text
nahi bhi mil sakta
```

So `Optional` dono situations handle karta hai.

---

# 12. `.orElseThrow()`

```java
.orElseThrow(() -> {
```

Meaning:

> Agar user mil gaya → user return karo.

> Agar user nahi mila → exception throw karo.

---

# 13. User not found case

```java
log.warn(
    "User authentication failed: email not found -> {}",
    email
);
```

Agar:

```text
email = unknown@gmail.com
```

database mein nahi hai, log hoga:

```text
User authentication failed:
email not found -> unknown@gmail.com
```

Then:

```java
return new UsernameNotFoundException(
    "User not found with email: " + email
);
```

exception create hoti hai.

So:

```text
Unknown Email
     ↓
UserRepository
     ↓
Optional.empty()
     ↓
orElseThrow()
     ↓
UsernameNotFoundException
```

Authentication fail ho jayegi.

---

# 14. Authorities banana

Ab maan lo user mil gaya.

Database mein user ke roles:

```text
ROLE_USER
ROLE_ADMIN
```

ho sakte hain.

Code:

```java
List<SimpleGrantedAuthority> authorities =
        user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
```

Isko parts mein samjho.

---

## `user.getRoles()`

```java
user.getRoles()
```

User ke roles retrieve karta hai.

Example:

```text
[
  "ROLE_USER",
  "ROLE_ADMIN"
]
```

---

## `.stream()`

```java
.stream()
```

Collection ke elements ko process karne ke liye stream start karta hai.

Conceptually:

```text
ROLE_USER
ROLE_ADMIN
    ↓
Stream
```

---

## `.map(SimpleGrantedAuthority::new)`

Ye har role ko:

```java
SimpleGrantedAuthority
```

mein convert karta hai.

Example:

```text
"ROLE_USER"
      ↓
SimpleGrantedAuthority("ROLE_USER")
```

and:

```text
"ROLE_ADMIN"
      ↓
SimpleGrantedAuthority("ROLE_ADMIN")
```

---

## `.collect(Collectors.toList())`

Finally sab authorities ko List mein convert karta hai:

```text
[
  SimpleGrantedAuthority("ROLE_USER"),
  SimpleGrantedAuthority("ROLE_ADMIN")
]
```

---

# 15. Ye authorities important kyu hain?

Spring Security decide kar sakti hai ki kaunsa user kya access kar sakta hai.

Example:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Agar user ke paas:

```text
ROLE_ADMIN
```

hai → allowed.

Agar sirf:

```text
ROLE_USER
```

hai → access denied.

So:

```text
Database Roles
      ↓
SimpleGrantedAuthority
      ↓
Spring Security
      ↓
Authorization decision
```

---

# 16. `return User.builder()`

Ab sabse important part:

```java
return org.springframework.security.core.userdetails.User
        .builder()
```

Yahan tumhari database wali `User` entity ko directly Spring Security user nahi bana rahe.

Tum **Spring Security ka `UserDetails` object** create kar rahe ho.

Important difference:

```text
Your User Entity
        ↓
Spring Security UserDetails
```

---

# 17. `.username(user.getEmail())`

```java
.username(user.getEmail())
```

Spring Security ke liye username set kar rahe ho.

Tumhare system mein:

```text
username = email
```

Example:

```text
jatin@gmail.com
```

---

# 18. `.password(user.getPasswordHash())`

```java
.password(user.getPasswordHash())
```

Database mein stored password hash Spring Security ko diya ja raha hai.

Example database:

```text
passwordHash =
$2a$10$xxxxxxxxxxxxxxxx
```

**Plain password nahi.**

Suppose user login karta hai:

```text
Entered password:
mypassword123
```

Database:

```text
$2a$10$....
```

Spring Security configured password encoder ke through comparison karegi.

Concept:

```text
Entered Password
       ↓
Password Encoder
       ↓
Compare
       ↓
Stored Password Hash
```

---

# 19. `.authorities(authorities)`

```java
.authorities(authorities)
```

Jo authorities humne pehle banayi:

```text
ROLE_USER
ROLE_ADMIN
```

Spring Security UserDetails ke andar attach kar dete hain.

---

# 20. `.disabled(!user.isEnabled())`

Ye thoda confusing hai.

Database mein:

```text
enabled = true
```

to:

```java
user.isEnabled()
```

returns:

```text
true
```

Then:

```java
!true
```

becomes:

```text
false
```

So:

```text
disabled = false
```

User login kar sakta hai.

### Agar database mein:

```text
enabled = false
```

Then:

```text
!false
```

becomes:

```text
true
```

So:

```text
disabled = true
```

Spring Security account ko disabled treat karegi.

### Table:

| DB `enabled` | `!enabled` | Spring Security `disabled` |
| ------------ | ---------- | -------------------------- |
| `true`       | `false`    | Not disabled               |
| `false`      | `true`     | Disabled                   |

---

# 21. `.accountExpired(false)`

```java
.accountExpired(false)
```

Iska matlab:

> Account ko expired mat treat karo.

Tumhare current `User` model mein account-expiry logic nahi hai, isliye `false` set kiya gaya hai.

---

# 22. `.accountLocked(false)`

```java
.accountLocked(false)
```

Meaning:

> Account currently locked nahi hai.

Agar future mein tum implement karo:

```text
5 failed login attempts
        ↓
Account locked
```

to yahan actual user state use ki ja sakti hai.

Currently:

```text
false = not locked
```

---

# 23. `.credentialsExpired(false)`

```java
.credentialsExpired(false)
```

Meaning:

> User ke credentials/password ko expired nahi maana ja raha.

Agar future mein password expiry feature add karo:

```text
Password older than 90 days
        ↓
Credentials expired
```

to yahan dynamic value aa sakti hai.

Currently:

```text
false
```

---

# 24. `.build()`

```java
.build();
```

Finally Spring Security `UserDetails` object create ho jata hai.

Conceptually:

```text
UserDetails
├── username = jatin@gmail.com
├── password = hashed password
├── authorities = ROLE_USER
├── disabled = false
├── accountExpired = false
├── accountLocked = false
└── credentialsExpired = false
```

---

# 25. Complete login example

Suppose user login karta hai:

```json
{
  "email": "jatin@gmail.com",
  "password": "abc12345"
}
```

### Step 1

Authentication system:

```text
email = jatin@gmail.com
```

### Step 2

Calls:

```java
loadUserByUsername("jatin@gmail.com")
```

### Step 3

Repository:

```java
userRepository.findByEmail(email)
```

### Step 4

PostgreSQL:

```text
User found
```

### Step 5

Roles:

```text
ROLE_USER
```

### Step 6

Convert:

```text
ROLE_USER
    ↓
SimpleGrantedAuthority("ROLE_USER")
```

### Step 7

Create Spring Security `UserDetails`:

```text
username = jatin@gmail.com
password = stored hash
authority = ROLE_USER
disabled = false
```

### Step 8

Spring Security password verify karegi.

### Step 9

Password correct:

```text
Authentication SUCCESS
```

Then tumhara authentication flow JWT generate kar sakta hai.

---

# 26. Agar email galat ho?

```text
user@gmail.com
```

database mein nahi hai.

Flow:

```text
Login
 ↓
loadUserByUsername()
 ↓
findByEmail()
 ↓
Optional.empty()
 ↓
UsernameNotFoundException
 ↓
Authentication Failed
```

---

# 27. Agar account disabled ho?

Database:

```text
enabled = false
```

Flow:

```text
User found
 ↓
isEnabled() = false
 ↓
disabled = true
 ↓
Spring Security
 ↓
Login denied
```

---

# 28. Is class ka actual role

Ye class **JWT generate nahi karti**.

Ye class:

```text
Database se user load
        ↓
Roles load
        ↓
Spring Security UserDetails create
        ↓
AuthenticationManager ko information provide
```

JWT generation tumhare project mein separate component, likely `JwtProvider`, karega.

So overall:

```text
                 LOGIN
                   │
                   ▼
          AuthenticationManager
                   │
                   ▼
       UserDetailsServiceImpl
                   │
                   ▼
            UserRepository
                   │
                   ▼
             PostgreSQL
                   │
                   ▼
              User Entity
                   │
                   ▼
          Spring UserDetails
                   │
                   ▼
         Password Verification
                   │
             ┌─────┴─────┐
             │           │
           FAIL        SUCCESS
             │           │
             ▼           ▼
           Denied      JwtProvider
                         │
                         ▼
                        JWT
```

### Ek line mein yaad rakho:

**`UserDetailsServiceImpl` = "Login ke time email ke basis par database se user nikaalo, uske password hash + roles + account status ko Spring Security ke samajhne layak `UserDetails` object mein convert karo."**
*/