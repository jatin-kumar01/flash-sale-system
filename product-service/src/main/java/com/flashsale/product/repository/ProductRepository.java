package com.flashsale.product.repository;

import com.flashsale.product.entity.Product;
import com.flashsale.product.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = :status AND :currentTime BETWEEN p.startTime AND p.endTime")
    Page<Product> findActiveFlashSales(@Param("status") ProductStatus status,
                                       @Param("currentTime") Instant currentTime,
                                       Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.endTime < :currentTime")
    List<Product> findExpiredSales(@Param("currentTime") Instant currentTime);
}
/*Bilkul. Ye `ProductRepository.java` **Product Service ka database-access layer** hai. Iska kaam business logic karna nahi, balki **Product table se data read/write karna** hai.

Tumhare flash-sale project ke context mein isko method-by-method samjho.

---

# 1. `ProductRepository` kya hai?

```java
@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long> {
```

Iska basic flow:

```text
ProductController
       ↓
ProductService
       ↓
ProductRepository
       ↓
Hibernate / JPA
       ↓
PostgreSQL
```

Repository directly database se baat karne ka abstraction deta hai.

---

# 2. `extends JpaRepository<Product, Long>`

```java
public interface ProductRepository
        extends JpaRepository<Product, Long>
```

Yahan:

```text
Product = Entity
Long    = Product ki ID ka type
```

Agar tumhari entity:

```java
@Entity
public class Product {

    @Id
    private Long id;

    private String name;
    private BigDecimal price;
}
```

hai, to repository:

```java
JpaRepository<Product, Long>
```

ka matlab hai:

> Ye repository `Product` entity aur uski `Long` type ID ke saath kaam karegi.

---

# 3. `JpaRepository` se already kya milta hai?

Tumhe manually ye methods likhne ki zarurat nahi:

```java
save()
findById()
findAll()
deleteById()
existsById()
count()
```

Example:

```java
productRepository.findById(101L);
```

SQL internally approximately:

```sql
SELECT *
FROM product
WHERE id = 101;
```

Aur:

```java
productRepository.save(product);
```

insert/update operation perform kar sakta hai.

Isliye `ProductRepository` mein sirf **custom queries** likhne ki zarurat hoti hai.

---

# 4. `findByStatus()`

Code:

```java
Page<Product> findByStatus(
        ProductStatus status,
        Pageable pageable
);
```

Ye **Spring Data JPA derived query** hai.

Tumne manually `@Query` nahi likhi.

Spring method name ko samajhkar query generate karta hai.

---

## Example

Suppose Product status enum:

```java
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    ENDED
}
```

Service:

```java
Page<Product> products =
    productRepository.findByStatus(
        ProductStatus.ACTIVE,
        pageable
    );
```

Spring internally approximately:

```sql
SELECT *
FROM product
WHERE status = 'ACTIVE'
LIMIT ...
OFFSET ...;
```

---

# 5. `Page<Product>` kyu?

Suppose database mein:

```text
10,00,000 products
```

hain.

Agar:

```java
List<Product> products =
    productRepository.findAll();
```

karoge, theoretically bahut large result memory mein aa sakta hai.

Flash-sale/e-commerce system mein ye avoid karna important hai.

Isliye:

```java
Page<Product>
```

use kiya hai.

Example:

```text
Total products = 100,000

Page size = 20

Page 0 → products 1–20
Page 1 → products 21–40
Page 2 → products 41–60
```

---

# 6. `Pageable`

```java
Pageable pageable
```

`Pageable` pagination information contain karta hai.

Example:

```java
PageRequest.of(0, 20);
```

means:

```text
page = 0
size = 20
```

Service:

```java
Page<Product> products =
    productRepository.findByStatus(
        ProductStatus.ACTIVE,
        PageRequest.of(0, 20)
    );
```

Database ko limited records retrieve karne ke liye pagination information milti hai.

---

# 7. `findActiveFlashSales()`

Ab tumhare project ka **important custom query**:

```java
@Query("""
    SELECT p FROM Product p
    WHERE p.status = :status
    AND :currentTime BETWEEN p.startTime AND p.endTime
""")
Page<Product> findActiveFlashSales(
        @Param("status") ProductStatus status,
        @Param("currentTime") Instant currentTime,
        Pageable pageable
);
```

Iska purpose:

> Sirf wahi products find karna jo given status mein hain aur jinki flash-sale window abhi active hai.

---

# 8. `@Query`

```java
@Query("SELECT p FROM Product p ...")
```

Yahan JPQL query likhi gayi hai.

Ye directly table/column names ke bajay **Entity aur entity fields** use karti hai.

Example:

```java
Product p
```

means `Product` entity.

Aur:

```java
p.status
p.startTime
p.endTime
```

Product entity ke fields hain.

Hibernate is JPQL ko database-specific SQL mein convert karta hai.

---

# 9. `:status`

Query:

```java
p.status = :status
```

`:status` ek named parameter hai.

Method:

```java
@Param("status")
ProductStatus status
```

is parameter ko query ke `:status` se connect karta hai.

Example:

```java
ProductStatus.ACTIVE
```

pass kiya:

```text
:status = ACTIVE
```

---

# 10. `:currentTime`

Query:

```java
:currentTime BETWEEN p.startTime AND p.endTime
```

Matlab:

```text
startTime ≤ currentTime ≤ endTime
```

Example:

```text
Sale Start = 10:00 AM
Sale End   = 11:00 AM
Current    = 10:30 AM
```

Then:

```text
10:00 ≤ 10:30 ≤ 11:00
```

True.

Product active flash sale mein return hoga.

---

## Example 2

```text
Start = 10:00
End   = 11:00
Now   = 11:30
```

Then:

```text
10:00 ≤ 11:30 ≤ 11:00
```

False.

Product return nahi hoga.

---

# 11. Complete `findActiveFlashSales()` example

Database:

| ID | Product | Status | Start | End   |
| -: | ------- | ------ | ----- | ----- |
|  1 | Laptop  | ACTIVE | 10:00 | 11:00 |
|  2 | Phone   | ACTIVE | 12:00 | 13:00 |
|  3 | Mouse   | ENDED  | 09:00 | 10:00 |

Current time:

```text
10:30
```

Call:

```java
productRepository.findActiveFlashSales(
    ProductStatus.ACTIVE,
    Instant.now(),
    pageable
);
```

Result:

```text
Laptop
```

Phone nahi because sale start nahi hui.

Mouse nahi because status `ENDED` hai.

---

# 12. `Instant` kyu use kiya?

```java
Instant currentTime
```

`Instant` Java ka date/time type hai jo **UTC-based point in time** represent karta hai.

Distributed microservices architecture mein consistent time representation useful hai.

Tumhare project mein:

```text
Flash Sale Service
Inventory Service
Order Service
```

different services/machines par run kar sakte hain.

Time handling consistent rakhna important hai.

---

# 13. `findExpiredSales()`

Code:

```java
@Query("""
    SELECT p FROM Product p
    WHERE p.status = 'ACTIVE'
    AND p.endTime < :currentTime
""")
List<Product> findExpiredSales(
        @Param("currentTime") Instant currentTime
);
```

Iska purpose:

> Aise products find karna jinki sale khatam ho chuki hai lekin database mein status abhi bhi `ACTIVE` hai.

---

# 14. Example

Database:

| Product | Status | End Time |
| ------- | ------ | -------- |
| Laptop  | ACTIVE | 10:00    |
| Phone   | ACTIVE | 12:00    |
| Mouse   | ENDED  | 09:00    |

Current time:

```text
11:00
```

Query:

```text
status = ACTIVE
AND endTime < 11:00
```

Result:

```text
Laptop
```

Because:

```text
ACTIVE
10:00 < 11:00
```

---

# 15. Ye method kahan useful hai?

Ye method **scheduled job/cron process** ke saath use ho sakti hai.

For example:

```text
Every 1 minute
      ↓
findExpiredSales()
      ↓
Expired products
      ↓
status = ENDED
```

Concept:

```text
10:00 → Sale ends
10:01 → Scheduler checks
10:01 → Product found
10:01 → status ACTIVE → ENDED
```

---

# 16. `List<Product>` kyu?

Yahan:

```java
List<Product>
```

use hua hai, `Page<Product>` nahi.

Reason: method ka intended use expired records ko process karna hai.

Example:

```text
100 expired products
       ↓
List<Product>
       ↓
for each product
       ↓
status = ENDED
```

Lekin production mein agar expired products **bahut zyada** ho sakte hain, to pagination/batching consider karna better ho sakta hai. Is repository code se ye assume nahi karna chahiye ki result size hamesha small rahega.

---

# 17. Teen repository methods ka difference

### Method 1

```java
findByStatus(...)
```

Purpose:

```text
Given status ke products find karo
```

Example:

```text
ACTIVE products
DRAFT products
ENDED products
```

---

### Method 2

```java
findActiveFlashSales(...)
```

Purpose:

```text
Status + current sale window
```

Example:

```text
ACTIVE
+
startTime <= now <= endTime
```

---

### Method 3

```java
findExpiredSales(...)
```

Purpose:

```text
ACTIVE products
+
endTime < now
```

Example:

```text
ACTIVE
+
sale already finished
```

---

# 18. Tumhare Flash Sale System ka complete flow

```text
                    ProductRepository
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
   findByStatus()   findActiveFlashSales()  findExpiredSales()
          │                │                │
          ▼                ▼                ▼
    All ACTIVE/       Current Flash      Expired ACTIVE
    DRAFT/ENDED      Sale Products        Products
          │                │                │
          └────────────────┼────────────────┘
                           ▼
                    ProductService
```

---

# 19. High-throughput project mein iska role

Tumhare project ka major challenge hai:

```text
Millions of users
       ↓
Flash-sale product
       ↓
High database reads/writes
```

`ProductRepository` catalog side ke database access ko structured rakhta hai.

Pagination:

```text
Millions of products
      ↓
Pageable
      ↓
Small result set
```

Sale filtering:

```text
All products
      ↓
ACTIVE + valid time window
      ↓
Only relevant flash-sale products
```

Expired sales:

```text
ACTIVE but expired
       ↓
Scheduler
       ↓
ENDED
```

---

# 20. Ek important correction in the explanation

Tumhare provided description mein likha hai:

> `findActiveFlashSales(...)` is supported by the composite index on `(startTime, endTime)`.

**Sirf repository method dekhkar ye guarantee nahi ki ja sakti.**

Repository query index ko automatically create nahi karti.

Index actually `Product` entity/database migration/schema mein defined hona chahiye, for example database level par appropriate index.

So architecture mein ye verify karna hoga ki `Product.java` ya migration/schema mein required index actually present hai.

Similarly, `status`, `startTime`, `endTime` ke query pattern ke liye **actual database execution plan** dekhna best way hai to confirm whether PostgreSQL is using the intended index.

---

## Short mein yaad rakho

`ProductRepository` ka kaam:

> **Database se Product data ko efficiently retrieve/persist karna.**

Aur methods:

```text
findByStatus()
     ↓
Status ke basis par products

findActiveFlashSales()
     ↓
ACTIVE + sale currently running

findExpiredSales()
     ↓
ACTIVE + sale already finished
```

Aur most important architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
JPA/Hibernate
    ↓
PostgreSQL
```

**Repository mein business logic nahi rakhna chahiye; repository ka primary responsibility database access/querying hai.**
*/