package com.flashsale.product.dto;

import com.flashsale.product.entity.Product;
import com.flashsale.product.entity.Product.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal flashSalePrice;
    private Integer initialStock;
    private String imageUrl;
    private Instant startTime;
    private Instant endTime;
    private ProductStatus status;
    private boolean saleActive;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .flashSalePrice(product.getFlashSalePrice())
                .initialStock(product.getInitialStock())
                .imageUrl(product.getImageUrl())
                .startTime(product.getStartTime())
                .endTime(product.getEndTime())
                .status(product.getStatus())
                .saleActive(product.isSaleActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
/*Yes, this `ProductResponse.java` is a **reasonable next file** after `Product.java`. Let me explain exactly what every important part is doing in Hinglish.

## 1. `ProductResponse` kya hai?

`ProductResponse` ek **DTO (Data Transfer Object)** hai.

Iska kaam database ki `Product` entity ko directly frontend ko bhejne ke bajay ek controlled response object banana hai.

Flow:

```text
Database
   ↓
Product Entity
   ↓
ProductResponse DTO
   ↓
Controller
   ↓
JSON Response
   ↓
Frontend
```

For example, database/entity mein Product hai:

```text
id = 101
title = Gaming Laptop
originalPrice = 80000
flashSalePrice = 69999
initialStock = 100
```

`ProductResponse` is information ko API response ke suitable format mein provide karega.

---

# 2. `implements Serializable`

```java
public class ProductResponse implements Serializable
```

`Serializable` ka purpose object ko serialization ke liye eligible banana hai.

Tumhare project mein future mein Redis cache use hoga.

Concept:

```text
ProductResponse
      ↓
Serialization
      ↓
Redis
```

Aur Redis se:

```text
Redis
  ↓
Deserialization
  ↓
ProductResponse
```

Important: `Serializable` **Redis use karne ki only possible requirement nahi hai**; Redis serializer configuration bhi matter karti hai. But is DTO ko Serializable banana is architecture mein acceptable hai.

---

# 3. `serialVersionUID`

```java
private static final long serialVersionUID = 1L;
```

Ye Java serialization mechanism ke liye version identifier hai.

Agar class future mein change hoti hai, Java serialization compatibility check kar sakta hai.

Simple:

```text
ProductResponse version 1
        ↓
serialVersionUID = 1L
```

Ye normal API JSON response ke liye directly visible nahi hota.

---

# 4. Lombok annotations

Tumhare code mein:

```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

hain.

Ye boilerplate Java code automatically generate karte hain.

### `@Getter`

Har field ke getter generate karega.

Example:

```java
getTitle()
getPrice()
getId()
```

---

### `@Setter`

Setters generate karega:

```java
setTitle(...)
setOriginalPrice(...)
```

---

### `@NoArgsConstructor`

Empty constructor:

```java
new ProductResponse();
```

possible banata hai.

---

### `@AllArgsConstructor`

All fields wala constructor generate karega.

Conceptually:

```java
new ProductResponse(
    id,
    title,
    description,
    ...
);
```

---

### `@Builder`

Ye particularly useful hai.

Tum:

```java
ProductResponse.builder()
```

se object create kar sakte ho.

Example:

```java
ProductResponse.builder()
        .id(101L)
        .title("Gaming Laptop")
        .originalPrice(new BigDecimal("80000"))
        .build();
```

Isse constructor mein 10–15 parameters pass karne ki zarurat nahi padti.

---

# 5. Product fields

### `id`

```java
private Long id;
```

Product ki unique ID.

Example:

```text
101
```

---

### `title`

```java
private String title;
```

Product ka naam:

```text
"Gaming Laptop"
```

---

### `description`

```java
private String description;
```

Product description.

---

### `originalPrice`

```java
private BigDecimal originalPrice;
```

Original price:

```text
₹80,000
```

`BigDecimal` money ke liye `double` se better choice hai because financial calculations mein precision important hoti hai.

---

### `flashSalePrice`

```java
private BigDecimal flashSalePrice;
```

Flash-sale price.

Example:

```text
₹69,999
```

Frontend:

```text
Original: ₹80,000
Sale: ₹69,999
```

show kar sakta hai.

---

### `initialStock`

```java
private Integer initialStock;
```

Initial inventory quantity.

Example:

```text
100
```

**Important:** naam `initialStock` hai, current stock nahi.

Flash-sale system mein actual current/reserved/available inventory eventually inventory service own karega.

---

### `imageUrl`

```java
private String imageUrl;
```

Product image ka URL/reference.

---

# 6. Sale timing

```java
private Instant startTime;
private Instant endTime;
```

Ye flash-sale ka time window represent karte hain.

Example:

```text
startTime = 2026-08-31T18:00:00Z
endTime   = 2026-08-31T20:00:00Z
```

`Instant` useful hai because distributed microservices mein UTC-based timestamps maintain karna easier hota hai.

---

# 7. `ProductStatus`

```java
private ProductStatus status;
```

Ye product/sale ka defined status represent karega.

For example, agar `Product` entity mein enum hai:

```java
enum ProductStatus {
    UPCOMING,
    ACTIVE,
    ENDED
}
```

to response mein wahi status aa sakta hai.

Example:

```json
{
  "status": "ACTIVE"
}
```

Exact available values tumhare `Product.java` par depend karenge.

---

# 8. `saleActive`

```java
private boolean saleActive;
```

Ye batata hai:

> Abhi flash sale active hai ya nahi?

Example:

```text
startTime = 10:00
endTime   = 12:00
current   = 11:00
```

Then:

```text
saleActive = true
```

Frontend:

```text
🔥 SALE LIVE
[BUY NOW]
```

show kar sakta hai.

Agar current time 13:00 hai:

```text
saleActive = false
```

---

# 9. `createdAt` and `updatedAt`

```java
private Instant createdAt;
private Instant updatedAt;
```

Product kab create hua aur last time kab update hua.

Example:

```json
{
  "createdAt": "2026-08-20T10:00:00Z",
  "updatedAt": "2026-08-30T15:30:00Z"
}
```

Ye auditing/debugging ke liye useful hai.

---

# 10. Sabse important method — `fromEntity()`

```java
public static ProductResponse fromEntity(Product product)
```

Iska purpose:

```text
Product Entity
      ↓
ProductResponse DTO
```

Ye mapping centralized rakhta hai.

---

## Example

Database se:

```java
Product product
```

milta hai.

Service mein:

```java
ProductResponse response =
        ProductResponse.fromEntity(product);
```

Ab entity DTO mein convert ho gayi.

---

# 11. Null check

```java
if (product == null) {
    return null;
}
```

Agar product `null` hai:

```java
ProductResponse.fromEntity(null);
```

to method exception throw karne ke bajay:

```text
null
```

return karegi.

---

# 12. Builder mapping

```java
return ProductResponse.builder()
```

Builder start ho raha hai.

Then:

```java
.id(product.getId())
```

Entity ka ID DTO mein:

```text
product.id → response.id
```

---

Similarly:

```java
.title(product.getTitle())
.description(product.getDescription())
.originalPrice(product.getOriginalPrice())
.flashSalePrice(product.getFlashSalePrice())
```

mapping:

```text
Product Entity              ProductResponse
────────────────────────────────────────────
id                 →        id
title              →        title
description        →        description
originalPrice      →        originalPrice
flashSalePrice     →        flashSalePrice
```

---

# 13. Stock mapping

```java
.initialStock(product.getInitialStock())
```

Entity ka initial stock response mein copy ho raha hai.

Again, **ye current available stock nahi necessarily hai**.

Actual available stock tumhare architecture mein inventory service se aana better hoga.

---

# 14. Sale timing mapping

```java
.startTime(product.getStartTime())
.endTime(product.getEndTime())
```

Entity ke sale timestamps DTO mein aa rahe hain.

---

# 15. Status mapping

```java
.status(product.getStatus())
```

Entity ka status response mein copy hota hai.

---

# 16. `product.isSaleActive()`

```java
.saleActive(product.isSaleActive())
```

Yahan interesting cheez hai.

`Product` entity ke andar likely method hai:

```java
public boolean isSaleActive() {
    ...
}
```

Ye determine karta hai ki current time par sale active hai ya nahi.

Then response mein:

```text
saleActive = true/false
```

set hota hai.

Example:

```text
Sale:
10:00 → 12:00

Current:
11:15

Product.isSaleActive()
        ↓
true

ProductResponse.saleActive
        ↓
true
```

---

# 17. Created/Updated mapping

```java
.createdAt(product.getCreatedAt())
.updatedAt(product.getUpdatedAt())
```

Entity timestamps DTO mein copy ho rahe hain.

Finally:

```java
.build();
```

Builder `ProductResponse` object create kar deta hai.

---

# 18. Complete example

Suppose database mein product:

```text
ID              = 101
Title           = Gaming Laptop
Original Price  = ₹80,000
Flash Price     = ₹69,999
Initial Stock   = 100
Sale Start      = 10:00
Sale End        = 12:00
Status          = ACTIVE
```

Entity:

```text
Product
   ↓
fromEntity()
   ↓
ProductResponse
```

API response roughly:

```json
{
  "id": 101,
  "title": "Gaming Laptop",
  "description": "High-performance gaming laptop",
  "originalPrice": 80000,
  "flashSalePrice": 69999,
  "initialStock": 100,
  "imageUrl": "/images/laptop.jpg",
  "startTime": "2026-08-31T10:00:00Z",
  "endTime": "2026-08-31T12:00:00Z",
  "status": "ACTIVE",
  "saleActive": true,
  "createdAt": "2026-08-20T10:00:00Z",
  "updatedAt": "2026-08-30T15:00:00Z"
}
```

Frontend is response ko consume kar sakta hai.

---

# 19. Entity ko directly return kyu nahi karte?

Bad approach:

```text
Controller
   ↓
Product Entity
   ↓
Frontend
```

Better:

```text
Controller
   ↓
ProductResponse DTO
   ↓
Frontend
```

Reason:

### Entity

Database structure represent karti hai.

### DTO

API ke liye data structure represent karta hai.

Isse database implementation aur API contract loosely coupled rehte hain.

---

# 20. Tumhare flash-sale architecture mein flow

Eventually:

```text
Frontend
    ↓
API Gateway
    ↓
Product Service
    ↓
ProductController
    ↓
ProductService
    ↓
ProductRepository
    ↓
PostgreSQL
```

Data wapas:

```text
PostgreSQL
    ↓
Product Entity
    ↓
ProductResponse.fromEntity()
    ↓
ProductResponse
    ↓
Jackson
    ↓
JSON
    ↓
Frontend
```

So **`ProductResponse.java` ka main purpose hai `Product` entity ko safe, clean aur API-friendly response DTO mein convert karna.**

### Ek important architectural point

Tumhare description mein likha hai **"active sale status"** aur `saleActive` entity ke `isSaleActive()` se calculate ho raha hai. Ye okay hai for the current design, **lekin later flash-sale logic ko dedicated flash-sale service mein move karna bhi possible hai**, especially agar sale rules complex ho jaate hain.

Abhi ke liye, agar `Product.java` mein `isSaleActive()` already defined hai, **is `ProductResponse.java` ko unnecessarily change mat karo.**
*/
