package com.flashsale.auth.repository;

import com.flashsale.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
/*
* Bilkul. Is `UserRepository.java` ko tumhare **Flash Sale System** ke context mein simple Hinglish mein samjho.

# `UserRepository.java` kya hai?

Ye file **database aur Java application ke beech data access layer** ka kaam karti hai.

Simple flow:

```text
Auth Controller
      ↓
AuthService
      ↓
UserRepository
      ↓
User Table (PostgreSQL)
```

Matlab `AuthService` ko agar database se user ki information chahiye, to woh directly SQL likhne ke bajay `UserRepository` ko use karega.

---

# 1. `JpaRepository<User, Long>`

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
```

Ye sabse important line hai.

Yahan:

```text
User
↓
Database entity

Long
↓
User ka ID type
```

Tumhari `User.java` likely kuch aisi hogi:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String email;
    private String password;
}
```

Isliye:

```java
JpaRepository<User, Long>
```

ka matlab hai:

> Ye repository `User` entity ke database records ko handle karegi aur User ka primary key `Long` type ka hai.

---

# 2. `JpaRepository` tumhe kya deta hai?

Tumhe manually ye methods likhne ki zarurat nahi:

```java
save()
findById()
findAll()
delete()
deleteById()
count()
existsById()
```

Ye sab `JpaRepository` se already mil jaate hain.

Example:

```java
userRepository.save(user);
```

Database mein user insert/update ho sakta hai.

---

### User find karna

```java
userRepository.findById(10L);
```

Conceptually SQL:

```sql
SELECT * FROM users WHERE id = 10;
```

---

### All users

```java
userRepository.findAll();
```

Conceptually:

```sql
SELECT * FROM users;
```

---

### Delete

```java
userRepository.deleteById(10L);
```

Conceptually:

```sql
DELETE FROM users WHERE id = 10;
```

Isliye `JpaRepository` tumhara **CRUD ka kaam automatically handle karta hai**.

---

# 3. `findByEmail(String email)`

Ab tumhara custom method:

```java
Optional<User> findByEmail(String email);
```

Ye authentication ke liye **bahut important** hai.

Spring Data JPA method name ko samajh kar query generate kar sakta hai.

Tumne SQL manually nahi likhi:

```sql
SELECT * FROM users WHERE email = ?;
```

Spring Data JPA method name:

```text
findByEmail
```

se samajh leta hai:

```text
find
  By
email
```

---

## Real login example

User login karta hai:

```json
{
    "email": "user@gmail.com",
    "password": "mypassword"
}
```

Flow:

```text
Login Request
      ↓
AuthController
      ↓
AuthService
      ↓
userRepository.findByEmail("user@gmail.com")
      ↓
Database
      ↓
User
```

Agar user mil gaya:

```text
User object
```

return hoga.

Agar nahi mila:

```text
Optional.empty()
```

return ho sakta hai.

---

# 4. `Optional<User>` kyu?

```java
Optional<User>
```

ka matlab:

> User mil bhi sakta hai aur nahi bhi mil sakta.

Example:

```java
Optional<User> user =
        userRepository.findByEmail(email);
```

Then:

```java
if (user.isEmpty()) {
    // User doesn't exist
}
```

Ya:

```java
if (user.isPresent()) {
    // User exists
}
```

Ye `null` handling ko safer banata hai.

---

# 5. Login ka complete example

Suppose database:

```text
users
--------------------------------
id | email            | password
--------------------------------
1  | abc@gmail.com    | ***
2  | xyz@gmail.com    | ***
```

User login karta hai:

```text
abc@gmail.com
```

Service:

```java
Optional<User> user =
        userRepository.findByEmail("abc@gmail.com");
```

Database se:

```text
User #1
```

mil jayega.

Then password verify hoga.

```text
Email found
     ↓
Password verify
     ↓
Correct
     ↓
JWT generate
     ↓
Login successful
```

---

# 6. `existsByEmail(String email)`

```java
boolean existsByEmail(String email);
```

Iska purpose hai:

> Check karna ki email already database mein exist karti hai ya nahi.

---

## Registration example

User registration karta hai:

```json
{
    "email": "abc@gmail.com",
    "password": "password123"
}
```

AuthService:

```java
if (userRepository.existsByEmail("abc@gmail.com")) {
    throw new DuplicateRequestException(
        "Email already registered"
    );
}
```

Agar email already hai:

```text
existsByEmail()
       ↓
true
       ↓
Registration reject
```

---

# 7. Agar email nahi hai

Suppose:

```text
newuser@gmail.com
```

database mein nahi hai.

Then:

```java
userRepository.existsByEmail(
    "newuser@gmail.com"
);
```

return:

```text
false
```

Then:

```text
false
 ↓
Create User
 ↓
save()
 ↓
Database
```

---

# 8. `existsByEmail()` ka benefit

Tum manually ye nahi likh rahe:

```sql
SELECT COUNT(*)
FROM users
WHERE email = 'abc@gmail.com';
```

Spring Data method:

```java
existsByEmail(email);
```

use karke existence check kar sakta hai.

Tumhara code clean rehta hai.

---

# 9. `@Repository`

```java
@Repository
public interface UserRepository
```

`@Repository` Spring ko indicate karta hai ki:

> Ye class/interface database persistence layer ka part hai.

Spring isko application context mein repository bean ke roop mein manage kar sakta hai.

Then `AuthService` mein:

```java
@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

Spring automatically `UserRepository` provide kar sakta hai.

---

# 10. Imports ka purpose

### `JpaRepository`

```java
import org.springframework.data.jpa.repository.JpaRepository;
```

CRUD aur JPA repository functionality ke liye.

---

### `Repository`

```java
import org.springframework.stereotype.Repository;
```

Repository ko Spring component ke roop mein identify karne ke liye.

---

### `User`

```java
import com.flashsale.auth.entity.User;
```

Repository kis entity ke saath kaam karegi:

```text
User
```

---

### `Optional`

```java
import java.util.Optional;
```

`findByEmail()` ke result ko safely represent karne ke liye.

---

# 11. Tumhare project mein complete flow

## Registration

```text
User
 ↓
POST /auth/register
 ↓
AuthController
 ↓
AuthService
 ↓
existsByEmail()
 ↓
 ┌───────────────┐
 │ Email exists? │
 └───────┬───────┘
         │
    Yes  │  No
     ↓   │   ↓
  Reject │ save(user)
         │   ↓
         │ Database
```

---

# 12. Login

```text
User
 ↓
POST /auth/login
 ↓
AuthController
 ↓
AuthService
 ↓
findByEmail()
 ↓
PostgreSQL
 ↓
User
 ↓
Password verify
 ↓
JWT generate
 ↓
Response
```

---

# 13. Repository ka main kaam

Tum is file ko basically **Database Access Interface** samajh sakte ho.

```text
UserRepository
       │
       ├── save()
       ├── findById()
       ├── findAll()
       ├── delete()
       │
       ├── findByEmail()
       └── existsByEmail()
```

`save()`, `findById()`, `findAll()` etc. **`JpaRepository` se inherited** hain.

`findByEmail()` aur `existsByEmail()` tumhare **custom methods** hain.

---

## Ek important correction

Tumhare description mein likha hai:

> `findByEmail()` executes index-backed retrieval `(idx_users_email)`.

Ye **tabhi true hoga jab database mein email par actual index defined ho**. Sirf repository method likhne se `idx_users_email` automatically guaranteed nahi hota.

Similarly:

> "associated eager-loaded roles"

Ye bhi `User.java` ki relationship mapping par depend karega. Agar roles `EAGER` hain tabhi automatically eager load honge.

So `UserRepository.java` ka actual guaranteed responsibility hai:

**User database records ko access karna + email ke basis par user find karna + email existence check karna + inherited CRUD operations provide karna.**

* */