package com.flashsale.auth.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
/*
 * Bilkul. Is `User.java` ko **tumhare Flash Sale microservices project ke context mein, har annotation aur method ke example ke saath Hinglish mein** samjho.
 *
 * # `User.java` kya hai?
 *
 * Ye **JPA Entity** hai jo database ke `users` table ko represent karti hai.
 *
 * Simple flow:
 *
 * ```text
 * User.java
 *    ↓
 * JPA / Hibernate
 *    ↓
 * PostgreSQL
 *    ↓
 * users table
 * ```
 *
 * Agar user register karta hai:
 *
 * ```text
 * Frontend
 *    ↓
 * Auth Service
 *    ↓
 * User Entity
 *    ↓
 * PostgreSQL
 * ```
 *
 * ---
 *
 * # 1. `@Entity`
 *
 * ```java
 * @Entity
 * ```
 *
 * Iska matlab:
 *
 * > Ye Java class database ki ek entity/table ko represent karegi.
 *
 * Tumhari:
 *
 * ```java
 * public class User
 * ```
 *
 * database mein roughly:
 *
 * ```text
 * users
 * ```
 *
 * table banayegi.
 *
 * Example:
 *
 * ```text
 * User object
 *    ↓
 * users table
 * ```
 *
 * ---
 *
 * # 2. `@Table`
 *
 * ```java
 * @Table(
 *     name = "users",
 *     indexes = {
 *         @Index(
 *             name = "idx_users_email",
 *             columnList = "email",
 *             unique = true
 *         )
 *     }
 * )
 * ```
 *
 * Yahan hum database table ka naam define kar rahe hain:
 *
 * ```text
 * users
 * ```
 *
 * Aur email ke liye unique index create kar rahe hain.
 *
 * ---
 *
 * # 3. `@Index`
 *
 * ```java
 * @Index(
 *     name = "idx_users_email",
 *     columnList = "email",
 *     unique = true
 * )
 * ```
 *
 * Iska main purpose:
 *
 * ### Duplicate email prevent karna
 *
 * Suppose:
 *
 * ```text
 * User 1 → john@gmail.com
 * User 2 → john@gmail.com
 * ```
 *
 * Database second user ko allow nahi karega.
 *
 * Because:
 *
 * ```text
 * email = UNIQUE
 * ```
 *
 * Ye **flash-sale project ke concurrency perspective se important** hai.
 *
 * Agar simultaneously:
 *
 * ```text
 * User A → signup → abc@gmail.com
 * User B → signup → abc@gmail.com
 * ```
 *
 * dono requests aati hain, database-level unique constraint duplicate identity ko prevent karne mein help karega.
 *
 * ---
 *
 * # 4. Lombok annotations
 *
 * Tumhare code mein:
 *
 * ```java
 * @Getter
 * @Setter
 * @Builder
 * @NoArgsConstructor
 * @AllArgsConstructor
 * ```
 *
 * hain.
 *
 * Ye boilerplate Java code kam karte hain.
 *
 * ---
 *
 * ## `@Getter`
 *
 * ```java
 * @Getter
 * ```
 *
 * Automatically getter methods generate karta hai.
 *
 * Normally tumhe likhna padta:
 *
 * ```java
 * public String getEmail() {
 *     return email;
 * }
 * ```
 *
 * Lombok automatically generate kar deta hai.
 *
 * ---
 *
 * ## `@Setter`
 *
 * ```java
 * @Setter
 * ```
 *
 * Setter generate karta hai.
 *
 * Normally:
 *
 * ```java
 * public void setEmail(String email) {
 *     this.email = email;
 * }
 * ```
 *
 * Lombok automatically bana deta hai.
 *
 * ---
 *
 * ## `@NoArgsConstructor`
 *
 * ```java
 * @NoArgsConstructor
 * ```
 *
 * Empty constructor generate karta hai:
 *
 * ```java
 * public User() {
 * }
 * ```
 *
 * JPA ko entity instantiate karne ke liye no-argument constructor ki requirement hoti hai.
 *
 * ---
 *
 * ## `@AllArgsConstructor`
 *
 * ```java
 * @AllArgsConstructor
 * ```
 *
 * Saare fields ka constructor generate karta hai.
 *
 * Conceptually:
 *
 * ```java
 * new User(
 *     id,
 *     email,
 *     passwordHash,
 *     firstName,
 *     lastName,
 *     enabled,
 *     roles,
 *     createdAt,
 *     updatedAt
 * );
 * ```
 *
 * ---
 *
 * ## `@Builder`
 *
 * ```java
 * @Builder
 * ```
 *
 * Builder pattern provide karta hai.
 *
 * Instead of:
 *
 * ```java
 * User user = new User();
 * user.setEmail("john@gmail.com");
 * user.setFirstName("John");
 * user.setLastName("Doe");
 * ```
 *
 * you can do:
 *
 * ```java
 * User user = User.builder()
 *         .email("john@gmail.com")
 *         .passwordHash("...")
 *         .firstName("John")
 *         .lastName("Doe")
 *         .build();
 * ```
 *
 * Readable hai aur large entities mein convenient hota hai.
 *
 * ---
 *
 * # 5. `id`
 *
 * ```java
 * @Id
 * @GeneratedValue(strategy = GenerationType.IDENTITY)
 * private Long id;
 * ```
 *
 * Ye user ka primary key hai.
 *
 * Database:
 *
 * ```text
 * users
 * --------------------------------
 * id
 * 1
 * 2
 * 3
 * ```
 *
 * ### `@Id`
 *
 * Batata hai:
 *
 * > Ye field primary key hai.
 *
 * ### `@GeneratedValue`
 *
 * ID automatically generate hogi.
 *
 * Example:
 *
 * ```text
 * First user  → id = 1
 * Second user → id = 2
 * Third user  → id = 3
 * ```
 *
 * Application ko manually ID dene ki zarurat nahi.
 *
 * ---
 *
 * # 6. `email`
 *
 * ```java
 * @Column(nullable = false, unique = true, length = 100)
 * private String email;
 * ```
 *
 * Database mein:
 *
 * ```text
 * email VARCHAR(100)
 * ```
 *
 * ### `nullable = false`
 *
 * Email mandatory hai.
 *
 * ```text
 * email = null ❌
 * ```
 *
 * allowed nahi.
 *
 * ### `unique = true`
 *
 * Same email duplicate nahi ho sakti.
 *
 * ```text
 * abc@gmail.com
 * abc@gmail.com ❌
 * ```
 *
 * ### `length = 100`
 *
 * Maximum column length 100 characters.
 *
 * ---
 *
 * # 7. `passwordHash`
 *
 * ```java
 * @Column(nullable = false, length = 255)
 * private String passwordHash;
 * ```
 *
 * Yahan **actual password nahi**, password ka hash store hona chahiye.
 *
 * Example user password:
 *
 * ```text
 * MyPassword123
 * ```
 *
 * Database mein ideally:
 *
 * ```text
 * $2a$10$....hashed-value....
 * ```
 *
 * store hoga.
 *
 * ### Important
 *
 * Kabhi bhi:
 *
 * ```text
 * password = MyPassword123
 * ```
 *
 * plain text database mein store nahi karna.
 *
 * Auth service normally:
 *
 * ```text
 * Password
 *    ↓
 * BCrypt
 *    ↓
 * Password Hash
 *    ↓
 * Database
 * ```
 *
 * ---
 *
 * # 8. `firstName`
 *
 * ```java
 * @Column(nullable = false, length = 50)
 * private String firstName;
 * ```
 *
 * User ka first name.
 *
 * Example:
 *
 * ```text
 * Jatin
 * ```
 *
 * `nullable = false` means required.
 *
 * ---
 *
 * # 9. `lastName`
 *
 * ```java
 * @Column(nullable = false, length = 50)
 * private String lastName;
 * ```
 *
 * Example:
 *
 * ```text
 * Kumar
 * ```
 *
 * Database:
 *
 * ```text
 * first_name | last_name
 * Jatin      | Kumar
 * ```
 *
 * ---
 *
 * # 10. `enabled`
 *
 * ```java
 * @Builder.Default
 * @Column(nullable = false)
 * private boolean enabled = true;
 * ```
 *
 * Ye batata hai account active hai ya nahi.
 *
 * Example:
 *
 * ```text
 * enabled = true
 * ```
 *
 * means:
 *
 * > User login kar sakta hai.
 *
 * Agar admin account disable kar de:
 *
 * ```text
 * enabled = false
 * ```
 *
 * to authentication system user ko login se reject kar sakta hai.
 *
 * ---
 *
 * # 11. `@Builder.Default`
 *
 * ```java
 * @Builder.Default
 * private boolean enabled = true;
 * ```
 *
 * Iska purpose hai ki Lombok builder use karte waqt bhi default value `true` maintain rahe.
 *
 * Example:
 *
 * ```java
 * User user = User.builder()
 *         .email("abc@gmail.com")
 *         .build();
 * ```
 *
 * `enabled` automatically:
 *
 * ```text
 * true
 * ```
 *
 * rahega.
 *
 * ---
 *
 * # 12. `roles`
 *
 * ```java
 * @ElementCollection(fetch = FetchType.EAGER)
 * @CollectionTable(
 *     name = "user_roles",
 *     joinColumns = @JoinColumn(name = "user_id")
 * )
 * @Column(name = "role", nullable = false)
 * @Builder.Default
 * private Set<String> roles = new HashSet<>();
 * ```
 *
 * Ye thoda important hai.
 *
 * User ke multiple roles ho sakte hain:
 *
 * ```text
 * ROLE_USER
 * ROLE_ADMIN
 * ```
 *
 * Isliye `Set<String>` use hua hai.
 *
 * Example:
 *
 * ```java
 * roles = Set.of(
 *     "ROLE_USER",
 *     "ROLE_ADMIN"
 * );
 * ```
 *
 * ---
 *
 * # 13. `@ElementCollection`
 *
 * ```java
 * @ElementCollection
 * ```
 *
 * Iska matlab roles ko `users` table ke andar ek column mein directly store nahi karenge.
 *
 * Separate table banegi:
 *
 * ```text
 * users
 * --------------------
 * id
 * email
 * password_hash
 * ...
 *
 * user_roles
 * --------------------
 * user_id
 * role
 * ```
 *
 * Example:
 *
 * ```text
 * users
 *
 * id | email
 * 1  | abc@gmail.com
 * ```
 *
 * and:
 *
 * ```text
 * user_roles
 *
 * user_id | role
 * 1       | ROLE_USER
 * 1       | ROLE_ADMIN
 * ```
 *
 * Ye **one user → multiple roles** ko represent karta hai.
 *
 * ---
 *
 * # 14. `FetchType.EAGER`
 *
 * ```java
 * fetch = FetchType.EAGER
 * ```
 *
 * Iska matlab:
 *
 * > User load karte waqt roles bhi immediately load karo.
 *
 * Authentication mein useful ho sakta hai because login ke time humein user ki authorities chahiye:
 *
 * ```text
 * Login
 *  ↓
 * User
 *  ↓
 * Roles
 *  ↓
 * Spring Security Authorities
 * ```
 *
 * Example:
 *
 * ```text
 * user = Jatin
 * roles = [ROLE_USER]
 * ```
 *
 * ---
 *
 * # 15. `@CollectionTable`
 *
 * ```java
 * @CollectionTable(
 *     name = "user_roles",
 *     joinColumns = @JoinColumn(name = "user_id")
 * )
 * ```
 *
 * Ye specify karta hai ki roles kis table mein store honge:
 *
 * ```text
 * user_roles
 * ```
 *
 * Aur connection:
 *
 * ```text
 * user_roles.user_id
 *         ↓
 * users.id
 * ```
 *
 * Example:
 *
 * ```text
 * users
 * id = 101
 * ```
 *
 * then:
 *
 * ```text
 * user_roles
 * user_id = 101
 * role = ROLE_USER
 * ```
 *
 * ---
 *
 * # 16. `@Column(name = "role")`
 *
 * ```java
 * @Column(name = "role", nullable = false)
 * ```
 *
 * Roles table mein actual column ka naam:
 *
 * ```text
 * role
 * ```
 *
 * hoga.
 *
 * Example:
 *
 * ```text
 * user_roles
 *
 * user_id | role
 * 101     | ROLE_USER
 * 101     | ROLE_ADMIN
 * ```
 *
 * ---
 *
 * # 17. `Set<String>`
 *
 * ```java
 * private Set<String> roles = new HashSet<>();
 * ```
 *
 * `Set` use karne ka reason:
 *
 * > Same role duplicate nahi hona chahiye.
 *
 * For example:
 *
 * ```text
 * ROLE_USER
 * ROLE_USER
 * ROLE_USER
 * ```
 *
 * ki jagah:
 *
 * ```text
 * ROLE_USER
 * ```
 *
 * hi rahega.
 *
 * ---
 *
 * # 18. `createdAt`
 *
 * ```java
 * @Column(nullable = false, updatable = false)
 * private Instant createdAt;
 * ```
 *
 * Ye batata hai:
 *
 * > User account kab create hua.
 *
 * Example:
 *
 * ```text
 * 2026-08-30T14:30:00Z
 * ```
 *
 * ### `updatable = false`
 *
 * Creation time baad mein change nahi hona chahiye.
 *
 * ```text
 * createdAt → fixed
 * ```
 *
 * ---
 *
 * # 19. `updatedAt`
 *
 * ```java
 * @Column(nullable = false)
 * private Instant updatedAt;
 * ```
 *
 * Ye last modification ka timestamp store karega.
 *
 * Example:
 *
 * ```text
 * createdAt = 2026-08-20
 * updatedAt = 2026-08-30
 * ```
 *
 * Agar user apna name change karta hai:
 *
 * ```text
 * updatedAt
 *     ↓
 * new current time
 * ```
 *
 * ---
 *
 * # 20. `Instant` kyu?
 *
 * ```java
 * import java.time.Instant;
 * ```
 *
 * `Instant` UTC-based timestamp represent karta hai.
 *
 * Microservices architecture mein ye useful hai because different services/machines different time zones mein run kar sakte hain.
 *
 * Instead of:
 *
 * ```text
 * India time
 * US time
 * Europe time
 * ```
 *
 * backend consistent UTC timestamp use kar sakta hai.
 *
 * ---
 *
 * # 21. `@PrePersist`
 *
 * ```java
 * @PrePersist
 * protected void onCreate() {
 *     this.createdAt = Instant.now();
 *     this.updatedAt = Instant.now();
 * }
 * ```
 *
 * Ye method **database mein new user insert hone se just pehle** automatically call hoti hai.
 *
 * Example:
 *
 * ```java
 * User user = User.builder()
 *         .email("abc@gmail.com")
 *         .build();
 *
 * userRepository.save(user);
 * ```
 *
 * JPA insert se pehle:
 *
 * ```text
 * onCreate()
 *    ↓
 * createdAt = current time
 * updatedAt = current time
 *    ↓
 * INSERT
 * ```
 *
 * Database:
 *
 * ```text
 * id | email | created_at | updated_at
 * 1  | abc   | 20:00      | 20:00
 * ```
 *
 * ---
 *
 * # 22. `@PreUpdate`
 *
 * ```java
 * @PreUpdate
 * protected void onUpdate() {
 *     this.updatedAt = Instant.now();
 * }
 * ```
 *
 * Jab existing user update hota hai:
 *
 * ```text
 * User
 *  ↓
 * Name changed
 *  ↓
 * JPA UPDATE
 *  ↓
 * onUpdate()
 *  ↓
 * updatedAt = current time
 * ```
 *
 * Example:
 *
 * Initially:
 *
 * ```text
 * firstName = Jatin
 * updatedAt = 20:00
 * ```
 *
 * User name change karta hai:
 *
 * ```text
 * firstName = Rahul
 * ```
 *
 * then:
 *
 * ```text
 * updatedAt = 21:30
 * ```
 *
 * ho jayega.
 *
 * `createdAt` same rahega.
 *
 * ---
 *
 * # 23. Database ka final structure
 *
 * Ye entity roughly database mein aisa structure create karegi:
 *
 * ```text
 * users
 * ────────────────────────────────────
 * id
 * email
 * password_hash
 * first_name
 * last_name
 * enabled
 * created_at
 * updated_at
 * ```
 *
 * Aur roles ke liye:
 *
 * ```text
 * user_roles
 * ────────────────
 * user_id
 * role
 * ```
 *
 * Example:
 *
 * ```text
 * users
 *
 * id | email           | first_name | enabled
 * 1  | jatin@gmail.com | Jatin      | true
 * ```
 *
 * ```text
 * user_roles
 *
 * user_id | role
 * 1       | ROLE_USER
 * ```
 *
 * ---
 *
 * # 24. Authentication ke time complete flow
 *
 * Tumhare project mein login flow roughly:
 *
 * ```text
 * Frontend
 *    │
 *    │ email + password
 *    ▼
 * API Gateway
 *    │
 *    ▼
 * Auth Service
 *    │
 *    ▼
 * UserRepository
 *    │
 *    ▼
 * users table
 *    │
 *    ▼
 * User
 *    │
 *    ├── email
 *    ├── passwordHash
 *    ├── enabled
 *    └── roles
 *           │
 *           ▼
 *     Spring Security
 *           │
 *           ▼
 *         JWT
 *           │
 *           ▼
 *        Frontend
 * ```
 *
 * ---
 *
 * # 25. Flash-sale project ke perspective se sabse important parts
 *
 * Is entity mein **5 important concepts** hain:
 *
 * ### 1. Unique email
 *
 * ```java
 * unique = true
 * ```
 *
 * Duplicate accounts prevent karne ke liye.
 *
 * ### 2. Password hash
 *
 * ```java
 * passwordHash
 * ```
 *
 * Security ke liye plain password store nahi karna.
 *
 * ### 3. Roles
 *
 * ```java
 * Set<String> roles
 * ```
 *
 * User/Admin permissions ke liye.
 *
 * ### 4. Enabled
 *
 * ```java
 * boolean enabled
 * ```
 *
 * Account ko activate/deactivate karne ke liye.
 *
 * ### 5. Audit timestamps
 *
 * ```java
 * createdAt
 * updatedAt
 * ```
 *
 * Account lifecycle track karne ke liye.
 *
 * ---
 *
 * ## Ek important correction
 *
 * Tumhare description mein likha hai:
 *
 * > "`@ElementCollection(fetch = FetchType.EAGER)` ... eagerly fetched during authentication ... in a single query round-trip."
 *
 * Is statement ko **guaranteed "single query round-trip"** nahi kehna chahiye. `EAGER` ka matlab roles eagerly available hone chahiye, lekin exact SQL/query strategy Hibernate decide kar sakta hai. Isliye interview mein better wording hogi:
 *
 * > **`@ElementCollection(fetch = FetchType.EAGER)` ensures the user's roles are loaded eagerly when the User entity is loaded, which is convenient for authentication and authority construction.**
 *
 * Ye technically safer explanation hai.
 *
 * ### Overall:
 *
 * ```text
 * User.java
 *    │
 *    ├── Identity → id, email
 *    ├── Security → passwordHash, roles, enabled
 *    ├── Profile → firstName, lastName
 *    └── Audit → createdAt, updatedAt
 *              │
 *              ▼
 *         PostgreSQL
 * ```
 *
 * **Basically `User.java` tumhare Auth Service ka database model hai—ye define karta hai ki ek user ki identity, login-related information, roles, account status aur audit information database mein kaise store hogi.**
 */

