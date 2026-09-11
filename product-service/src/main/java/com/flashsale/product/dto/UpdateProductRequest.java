package com.flashsale.product.dto;

import com.flashsale.product.entity.Product.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    private String description;

    @DecimalMin(value = "0.01", message = "Original price must be greater than 0")
    private BigDecimal originalPrice;

    @DecimalMin(value = "0.01", message = "Flash sale price must be greater than 0")
    private BigDecimal flashSalePrice;

    private Integer initialStock;

    @Size(max = 255, message = "Image URL cannot exceed 255 characters")
    private String imageUrl;

    private Instant startTime;

    private Instant endTime;

    private ProductStatus status;
}
/*Bilkul. Ye **`UpdateProductRequest.java`** tumhare `product-service` ka DTO hai. Iska main kaam hai **existing product ko update karne ke liye frontend/API se aane wale data ko receive aur validate karna**.

Is file mein actual business logic nahi hai. Ye mainly **data + validation rules** define karti hai.

---

# 1. `UpdateProductRequest` kya hai?

Simple flow:

```text
Admin / Frontend
       ↓
PUT /api/products/{id}
       ↓
JSON Request
       ↓
UpdateProductRequest
       ↓
Validation
       ↓
ProductService
       ↓
Product Entity
       ↓
Database
```

Example frontend se:

```json
{
  "title": "Gaming Laptop Pro",
  "originalPrice": 90000,
  "flashSalePrice": 75000,
  "initialStock": 100,
  "status": "ACTIVE"
}
```

Ye JSON automatically `UpdateProductRequest` object mein convert ho jayega.

---

# 2. Class declaration

```java
public class UpdateProductRequest {
```

Ye ek **DTO (Data Transfer Object)** hai.

DTO ka purpose hota hai:

> API ke through aane wale data ko entity se separate rakhna.

Yahan directly `Product` entity ko API request ke liye use nahi kar rahe.

Instead:

```text
Request JSON
    ↓
UpdateProductRequest
    ↓
ProductService
    ↓
Product
```

Ye achhi practice hai because client ko directly database entity control nahi milti.

---

# 3. Lombok annotations

## `@Getter`

```java
@Getter
```

Lombok automatically har field ke getter generate karta hai.

Tum manually:

```java
public String getTitle() {
    return title;
}
```

likhne ki zarurat nahi.

Internally approximately:

```java
request.getTitle();
request.getOriginalPrice();
request.getStatus();
```

available ho jayenge.

---

# 4. `@Setter`

```java
@Setter
```

Har field ke setter generate karta hai.

Example:

```java
request.setTitle("Gaming Laptop Pro");
```

Tumhe manually setter likhne ki zarurat nahi.

---

# 5. `@Builder`

```java
@Builder
```

Builder pattern provide karta hai.

Example:

```java
UpdateProductRequest request =
        UpdateProductRequest.builder()
                .title("Gaming Laptop Pro")
                .originalPrice(new BigDecimal("90000"))
                .flashSalePrice(new BigDecimal("75000"))
                .status(ProductStatus.ACTIVE)
                .build();
```

Ye especially tests aur internal object creation mein useful hai.

---

# 6. `@NoArgsConstructor`

```java
@NoArgsConstructor
```

Empty constructor generate karta hai:

```java
new UpdateProductRequest();
```

Ye important hai because Spring/Jackson ko request JSON ko DTO mein convert karte waqt no-argument constructor ki zarurat pad sakti hai.

Flow:

```text
JSON
 ↓
Jackson
 ↓
new UpdateProductRequest()
 ↓
setTitle(...)
setPrice(...)
...
```

---

# 7. `@AllArgsConstructor`

```java
@AllArgsConstructor
```

Saare fields wala constructor generate karta hai.

Conceptually:

```java
new UpdateProductRequest(
    title,
    description,
    originalPrice,
    flashSalePrice,
    initialStock,
    imageUrl,
    startTime,
    endTime,
    status
);
```

---

# 8. `title`

```java
@Size(
    max = 150,
    message = "Title cannot exceed 150 characters"
)
private String title;
```

Product title maximum **150 characters** ho sakta hai.

Example valid:

```text
"Gaming Laptop Pro"
```

Invalid:

```text
150+ characters ka title
```

Validation fail hone par:

```text
Title cannot exceed 150 characters
```

`GlobalExceptionHandler` ise catch karke structured validation response bana sakta hai.

### Important: `title` required nahi hai

Yahan `@NotBlank` nahi hai.

Isliye:

```json
{}
```

possible hai.

Ye intentional hai because ye **Update DTO** hai.

Admin sirf price update karna chahe:

```json
{
  "flashSalePrice": 70000
}
```

to baaki fields bhejne ki zarurat nahi.

---

# 9. `description`

```java
private String description;
```

Product description update karne ke liye.

Example:

```json
{
  "description": "High-performance gaming laptop with 16GB RAM."
}
```

Is par currently koi validation nahi hai.

---

# 10. `originalPrice`

```java
@DecimalMin(
    value = "0.01",
    message = "Original price must be greater than 0"
)
private BigDecimal originalPrice;
```

Original product price ke liye.

Example:

```json
{
  "originalPrice": 90000
}
```

Valid.

But:

```json
{
  "originalPrice": 0
}
```

invalid.

Because minimum:

```text
0.01
```

hai.

---

## `BigDecimal` kyu?

Money ke liye `double`/`float` ke comparison mein `BigDecimal` generally safer hota hai.

Example:

```java
BigDecimal price =
    new BigDecimal("75000.50");
```

Tumhare e-commerce project mein prices ke liye ye appropriate type hai.

---

# 11. `flashSalePrice`

```java
@DecimalMin(
    value = "0.01",
    message = "Flash sale price must be greater than 0"
)
private BigDecimal flashSalePrice;
```

Flash-sale price ke liye.

Example:

```json
{
  "flashSalePrice": 69999
}
```

Valid.

But:

```json
{
  "flashSalePrice": 0
}
```

invalid.

---

### Important

Ye annotation sirf ye check karta hai:

```text
flashSalePrice >= 0.01
```

Ye **check nahi karta** ki:

```text
flashSalePrice < originalPrice
```

For example:

```json
{
  "originalPrice": 50000,
  "flashSalePrice": 60000
}
```

`@DecimalMin` ke according valid ho sakta hai.

Agar business rule hai:

```text
Flash Sale Price < Original Price
```

to wo validation/service/business logic mein separately handle karna padega.

---

# 12. `initialStock`

```java
private Integer initialStock;
```

Product ka initial stock update karne ke liye.

Example:

```json
{
  "initialStock": 500
}
```

Lekin currently ismein:

```java
@Min(...)
```

ya:

```java
@Positive
```

nahi hai.

So DTO level par negative value automatically reject nahi hogi.

For example:

```json
{
  "initialStock": -50
}
```

is field par currently koi validation annotation nahi hai.

**Ye important point hai jo tumhe future mein decide karna hoga.**

---

# 13. `imageUrl`

```java
@Size(
    max = 255,
    message = "Image URL cannot exceed 255 characters"
)
private String imageUrl;
```

Product image ka URL.

Example:

```json
{
  "imageUrl": "https://example.com/products/laptop.jpg"
}
```

Maximum:

```text
255 characters
```

---

# 14. `startTime`

```java
private Instant startTime;
```

Flash sale kab start hogi.

Example:

```text
2026-09-01T10:00:00Z
```

Java:

```java
Instant
```

use kar raha hai because `Instant` UTC-based timestamp represent karta hai.

Flash-sale system mein ye useful hai because multiple users different locations/time zones se aa sakte hain.

---

# 15. `endTime`

```java
private Instant endTime;
```

Flash sale kab end hogi.

Example:

```text
2026-09-01T12:00:00Z
```

So:

```text
startTime
    ↓
10:00 UTC
    │
    │ FLASH SALE
    │
12:00 UTC
    ↓
endTime
```

Again, current DTO ye validate nahi karta ki:

```text
endTime > startTime
```

Ye business validation service layer mein karni hogi.

---

# 16. `status`

```java
private ProductStatus status;
```

Ye product ka lifecycle status represent karta hai.

`Product.ProductStatus` enum se value aayegi.

Tumhare description ke according possible values:

```text
DRAFT
ACTIVE
ENDED
DISABLED
```

Example:

```json
{
  "status": "ACTIVE"
}
```

Java mein:

```java
ProductStatus.ACTIVE
```

banega.

---

# 17. Partial Update ka main benefit

Ye class ka **sabse important design point** hai.

Suppose existing product:

```text
Title: Gaming Laptop
Original Price: ₹90,000
Flash Price: ₹75,000
Stock: 100
Status: ACTIVE
```

Admin sirf price change karna chahta hai:

```json
{
  "flashSalePrice": 70000
}
```

To baaki fields required nahi.

Service existing values maintain kar sakti hai:

```text
Old title        → unchanged
Old description  → unchanged
Old originalPrice → unchanged
flashSalePrice   → ₹70,000
Old stock        → unchanged
Old status       → unchanged
```

That's why fields optional hain.

---

# 18. `UpdateProductRequest` vs `CreateProductRequest`

Ye distinction important hai.

### Create

Usually:

```text
CreateProductRequest
```

mein required fields ho sakte hain:

```text
title        → required
price        → required
stock        → required
```

Because new product create ho raha hai.

### Update

```text
UpdateProductRequest
```

mein fields optional hain:

```text
title        → optional
price        → optional
stock        → optional
status       → optional
```

Because existing product ko partially modify karna hai.

---

# 19. Is class mein methods kaha hain?

Actually tumhare code mein manually koi method nahi hai.

Lombok annotations methods automatically generate kar rahi hain:

```text
@Getter
    ↓
getTitle()
getDescription()
getOriginalPrice()
...

@Setter
    ↓
setTitle()
setDescription()
setOriginalPrice()
...

@Builder
    ↓
builder()

@NoArgsConstructor
    ↓
empty constructor

@AllArgsConstructor
    ↓
all-fields constructor
```

Isliye source code chhota hai, but compile hone ke baad class mein ye methods available hote hain.

---

# 20. Complete real-world flow

Suppose Admin frontend se request bhejta hai:

```http
PUT /api/products/101
```

Body:

```json
{
  "flashSalePrice": 69999,
  "endTime": "2026-09-01T12:00:00Z",
  "status": "ACTIVE"
}
```

Flow:

```text
Admin
  ↓
ProductController
  ↓
UpdateProductRequest
  ↓
@Size / @DecimalMin validation
  ↓
ProductService
  ↓
ProductRepository
  ↓
PostgreSQL
```

Agar validation fail:

```text
UpdateProductRequest
       ↓
Validation Error
       ↓
GlobalExceptionHandler
       ↓
400 Bad Request
       ↓
Frontend
```

---

## Sabse simple definition

**`UpdateProductRequest` = Admin/frontend se existing product ko update karne ke liye aane wale data ka validated container.**

Iska kaam **database update karna nahi**, balki:

```text
Receive data
    ↓
Validate basic input
    ↓
Pass data to ProductService
```

hai.

Aur tumhare architecture mein ye separation important hai:

```text
Controller
   ↓
UpdateProductRequest   ← API input
   ↓
ProductService         ← Business logic
   ↓
Product                ← Database entity
   ↓
ProductRepository      ← Database access
```

Ye **Controller → DTO → Service → Entity → Repository** pattern tumhare microservice architecture mein bahut important rahega.
*/