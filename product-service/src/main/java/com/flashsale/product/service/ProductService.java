package com.flashsale.product.service;

import com.flashsale.common.exception.InvalidRequestException;
import com.flashsale.common.exception.ResourceNotFoundException;
import com.flashsale.product.dto.CreateProductRequest;
import com.flashsale.product.dto.ProductResponse;
import com.flashsale.product.dto.UpdateProductRequest;
import com.flashsale.product.entity.Product;
import com.flashsale.product.entity.Product.ProductStatus;
import com.flashsale.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        validateProductWindow(request.getStartTime(), request.getEndTime());
        validatePricing(request.getOriginalPrice(), request.getFlashSalePrice());

        Product product = Product.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .originalPrice(request.getOriginalPrice())
                .flashSalePrice(request.getFlashSalePrice())
                .initialStock(request.getInitialStock())
                .imageUrl(request.getImageUrl())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created new flash sale product: ID {}", savedProduct.getId());

        ProductResponse response = ProductResponse.fromEntity(savedProduct);
        productCacheService.putProduct(response);
        productCacheService.evictActiveFlashSales();

        return response;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        // Cache-aside strategy
        return productCacheService.getProduct(productId)
                .orElseGet(() -> {
                    log.debug("Cache miss for product ID: {}. Querying database.", productId);
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

                    ProductResponse response = ProductResponse.fromEntity(product);
                    productCacheService.putProduct(response);
                    return response;
                });
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveFlashSales(Pageable pageable) {
        Instant now = Instant.now();
        return productRepository.findActiveFlashSales(ProductStatus.ACTIVE, now, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(ProductStatus status, Pageable pageable) {
        if (status != null) {
            return productRepository.findByStatus(status, pageable)
                    .map(ProductResponse::fromEntity);
        }
        return productRepository.findAll(pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (request.getStartTime() != null && request.getEndTime() != null) {
            validateProductWindow(request.getStartTime(), request.getEndTime());
            product.setStartTime(request.getStartTime());
            product.setEndTime(request.getEndTime());
        }

        if (request.getOriginalPrice() != null && request.getFlashSalePrice() != null) {
            validatePricing(request.getOriginalPrice(), request.getFlashSalePrice());
            product.setOriginalPrice(request.getOriginalPrice());
            product.setFlashSalePrice(request.getFlashSalePrice());
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            product.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getInitialStock() != null && request.getInitialStock() >= 0) {
            product.setInitialStock(request.getInitialStock());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Updated product ID: {}", updatedProduct.getId());

        ProductResponse response = ProductResponse.fromEntity(updatedProduct);
        productCacheService.putProduct(response);
        productCacheService.evictActiveFlashSales();

        return response;
    }

    @Transactional
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        productRepository.deleteById(productId);
        productCacheService.evictProduct(productId);
        log.info("Deleted product ID: {}", productId);
    }

    private void validateProductWindow(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            throw new InvalidRequestException("Start time and end time cannot be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidRequestException("End time must be strictly after start time");
        }
    }

    private void validatePricing(java.math.BigDecimal original, java.math.BigDecimal flashSale) {
        if (original == null || flashSale == null) {
            throw new InvalidRequestException("Prices cannot be null");
        }
        if (flashSale.compareTo(original) > 0) {
            throw new InvalidRequestException("Flash sale price cannot be greater than original price");
        }
    }
}
/*Bilkul. Ye **`ProductService.java`** tumhare Flash Sale System ka **core business/service layer** hai. Is class ka main kaam hai **Controller aur Repository/Cache ke beech business logic coordinate karna**.

Simple flow:

```text
ProductController
       ↓
ProductService
   ↓          ↓
Redis      PostgreSQL
(Cache)    (Database)
```

Ab **har method ko real Flash Sale example ke saath** samjho.

---

# 1. Class aur dependencies

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
```

### `@Service`

Spring ko batata hai ki ye class **business logic/service layer** ka component hai.

Flow:

```text
HTTP Request
     ↓
Controller
     ↓
ProductService
     ↓
Repository / Cache
```

---

### `@RequiredArgsConstructor`

Lombok automatically constructor bana deta hai:

```java
public ProductService(
    ProductRepository productRepository,
    ProductCacheService productCacheService
) {
    this.productRepository = productRepository;
    this.productCacheService = productCacheService;
}
```

Isliye manually constructor likhne ki zarurat nahi.

---

### `@Slf4j`

Logging ke liye:

```java
log.info(...)
log.warn(...)
log.debug(...)
log.error(...)
```

use kar sakte ho.

---

# 2. Dependencies

```java
private final ProductRepository productRepository;
private final ProductCacheService productCacheService;
```

Yahan service ke paas do important dependencies hain.

### `ProductRepository`

PostgreSQL/database se baat karega.

```text
ProductService
      ↓
ProductRepository
      ↓
PostgreSQL
```

### `ProductCacheService`

Redis se baat karega.

```text
ProductService
      ↓
ProductCacheService
      ↓
Redis
```

---

# 3. `createProduct()`

```java
@Transactional
public ProductResponse createProduct(CreateProductRequest request)
```

Is method ka purpose:

> **Naya flash-sale product create karna.**

Example:

Admin create karta hai:

```json
{
  "title": "Gaming Laptop",
  "description": "RTX Gaming Laptop",
  "originalPrice": 80000,
  "flashSalePrice": 65000,
  "initialStock": 100,
  "startTime": "...",
  "endTime": "..."
}
```

Flow:

```text
Admin
 ↓
ProductController
 ↓
createProduct()
 ↓
Validation
 ↓
Product Entity
 ↓
PostgreSQL
 ↓
Redis
 ↓
Response
```

---

# 4. Product window validation

```java
validateProductWindow(
    request.getStartTime(),
    request.getEndTime()
);
```

Ye check karta hai:

```text
startTime < endTime
```

Example:

```text
Start: 10:00 AM
End:   12:00 PM
```

✅ Valid.

But:

```text
Start: 12:00 PM
End:   10:00 AM
```

❌ Invalid.

---

# 5. Price validation

```java
validatePricing(
    request.getOriginalPrice(),
    request.getFlashSalePrice()
);
```

Check:

```text
Flash Sale Price <= Original Price
```

Example:

```text
Original = ₹80,000
Flash = ₹65,000
```

✅ Valid.

But:

```text
Original = ₹80,000
Flash = ₹90,000
```

❌ Invalid.

---

# 6. Product Entity banana

```java
Product product = Product.builder()
```

Yahan request DTO se actual database entity banayi ja rahi hai.

```java
.title(request.getTitle().trim())
.description(request.getDescription())
.originalPrice(request.getOriginalPrice())
.flashSalePrice(request.getFlashSalePrice())
.initialStock(request.getInitialStock())
.imageUrl(request.getImageUrl())
.startTime(request.getStartTime())
.endTime(request.getEndTime())
.status(ProductStatus.ACTIVE)
.build();
```

Suppose request:

```text
title = " Gaming Laptop "
```

`.trim()` ke baad:

```text
"Gaming Laptop"
```

---

# 7. Status ACTIVE

```java
.status(ProductStatus.ACTIVE)
```

New product ko immediately:

```text
ACTIVE
```

set kiya ja raha hai.

Yahan ek architectural point hai: tumhare description mein **DRAFT or ACTIVE** likha hai, lekin current implementation **always ACTIVE** set karti hai.

So current code ka actual behavior:

```text
New Product
    ↓
ACTIVE
```

DRAFT nahi.

---

# 8. Database mein save

```java
Product savedProduct =
    productRepository.save(product);
```

Ab Product PostgreSQL mein save hota hai.

Example:

```text
Product
ID: 101
Title: Gaming Laptop
Original: ₹80,000
Flash: ₹65,000
Stock: 100
```

Database:

```text
products table
-----------------------
101 | Gaming Laptop | ...
```

---

# 9. Logging

```java
log.info(
    "Created new flash sale product: ID {}",
    savedProduct.getId()
);
```

Log:

```text
Created new flash sale product: ID 101
```

---

# 10. Entity → Response DTO

```java
ProductResponse response =
    ProductResponse.fromEntity(savedProduct);
```

Database entity directly frontend ko nahi bhejna chahte.

Isliye:

```text
Product Entity
     ↓
ProductResponse DTO
     ↓
Controller
     ↓
Frontend
```

---

# 11. Redis mein product store

```java
productCacheService.putProduct(response);
```

Ab newly created product Redis mein cache hota hai.

Next time:

```text
GET /products/101
```

database hit karne ke bajay Redis se mil sakta hai.

---

# 12. Active flash-sale cache evict

```java
productCacheService.evictActiveFlashSales();
```

Agar active flash-sale products ki cached list already Redis mein thi, wo stale ho sakti hai.

Example:

Before:

```text
Active Flash Sales:
Laptop
Phone
```

New product:

```text
Headphones
```

create hua.

Old cache:

```text
Laptop
Phone
```

stale hai.

Isliye active-sale aggregate cache remove/refresh kiya jata hai.

---

# 13. `getProductById()`

```java
@Transactional(readOnly = true)
public ProductResponse getProductById(Long productId)
```

Purpose:

> Product ID ke through product details fetch karna.

Example:

```http
GET /products/101
```

---

# 14. Cache-aside pattern

```java
return productCacheService.getProduct(productId)
```

Sabse pehle Redis check hota hai.

```text
Request
  ↓
Redis?
```

### Cache HIT

```text
Redis
 ↓
Product found
 ↓
Return
```

Database query nahi hogi.

---

### Cache MISS

```text
Redis
 ↓
Product not found
 ↓
PostgreSQL
```

---

# 15. `.orElseGet()`

```java
.orElseGet(() -> {
```

Agar Redis mein product nahi mila, ye block execute hoga.

---

# 16. Cache miss logging

```java
log.debug(
    "Cache miss for product ID: {}. Querying database.",
    productId
);
```

Log:

```text
Cache miss for product ID: 101.
Querying database.
```

---

# 17. Database se product

```java
Product product =
    productRepository.findById(productId)
```

PostgreSQL mein search:

```sql
SELECT * FROM products
WHERE id = 101;
```

---

# 18. Product not found

```java
.orElseThrow(
    () -> new ResourceNotFoundException(
        "Product",
        "id",
        productId
    )
);
```

Agar ID `999` exist nahi karti:

```text
Product 999
    ↓
Not found
    ↓
ResourceNotFoundException
```

Then tumhara:

```text
GlobalExceptionHandler
```

exception ko catch karke:

```text
404 NOT_FOUND
```

return kar sakta hai.

---

# 19. Database result ko DTO banana

```java
ProductResponse response =
    ProductResponse.fromEntity(product);
```

---

# 20. Cache ko populate karna

```java
productCacheService.putProduct(response);
```

Ye **cache-aside pattern** ka important part hai.

Flow:

```text
First request

Redis → MISS
   ↓
PostgreSQL
   ↓
Product
   ↓
Redis ← Store
   ↓
Response
```

Next request:

```text
Redis → HIT
   ↓
Response
```

---

# 21. `getActiveFlashSales()`

```java
public Page<ProductResponse>
getActiveFlashSales(Pageable pageable)
```

Purpose:

> Currently active flash-sale products ko paginated form mein retrieve karna.

Example:

Current time:

```text
10:30 AM
```

Products:

```text
Laptop
Start: 10:00
End: 12:00

Phone
Start: 09:00
End: 11:00

TV
Start: 01:00 PM
End: 03:00 PM
```

Active products:

```text
Laptop
Phone
```

TV active nahi hai.

---

# 22. Current time

```java
Instant now = Instant.now();
```

Current UTC timestamp obtain karta hai.

---

# 23. Repository query

```java
productRepository.findActiveFlashSales(
    ProductStatus.ACTIVE,
    now,
    pageable
)
```

Repository likely database query karega:

```text
status = ACTIVE
AND startTime <= now
AND endTime > now
```

Conceptually:

```sql
WHERE status = 'ACTIVE'
AND start_time <= NOW()
AND end_time > NOW()
```

---

# 24. Pagination

```java
Pageable pageable
```

Suppose 10,000 products hain.

Hum ek saath 10,000 nahi bhejna chahte.

Instead:

```text
Page 1 → 20 products
Page 2 → 20 products
Page 3 → 20 products
```

`Pageable` isi ke liye hai.

---

# 25. `.map(ProductResponse::fromEntity)`

```java
.map(ProductResponse::fromEntity);
```

Database se:

```text
Page<Product>
```

milta hai.

Usko:

```text
Page<ProductResponse>
```

mein convert karta hai.

---

# 26. `getAllProducts()`

```java
public Page<ProductResponse>
getAllProducts(
    ProductStatus status,
    Pageable pageable
)
```

Purpose:

> Admin/catalog ke liye products ki list retrieve karna.

Optional status:

```text
ACTIVE
DRAFT
DISABLED
```

---

## Status provided

```java
if (status != null) {
```

Example:

```http
GET /products?status=ACTIVE
```

Then:

```java
productRepository.findByStatus(
    status,
    pageable
)
```

Only ACTIVE products milenge.

---

## Status not provided

```java
return productRepository
    .findAll(pageable)
```

Example:

```http
GET /products
```

All products paginated form mein milenge.

---

# 27. `updateProduct()`

```java
@Transactional
public ProductResponse updateProduct(
    Long productId,
    UpdateProductRequest request
)
```

Purpose:

> Existing product ko update karna.

Example:

Admin changes:

```text
Flash Sale Price:
₹65,000 → ₹60,000
```

---

# 28. Product find karna

```java
Product product =
    productRepository.findById(productId)
```

Example:

```text
ID = 101
```

Database se product retrieve.

Agar nahi mila:

```java
throw new ResourceNotFoundException(...)
```

→ `404`.

---

# 29. Sale time update

```java
if (request.getStartTime() != null &&
    request.getEndTime() != null) {
```

Agar dono times provided hain, validation hogi.

```java
validateProductWindow(...)
```

Then:

```java
product.setStartTime(...)
product.setEndTime(...)
```

---

# 30. Price update

```java
if (request.getOriginalPrice() != null &&
    request.getFlashSalePrice() != null)
```

Dono prices provided hain to:

```java
validatePricing(...)
```

Then:

```java
product.setOriginalPrice(...)
product.setFlashSalePrice(...)
```

Example:

```text
Original = ₹80,000
Flash = ₹60,000
```

---

# 31. Title update

```java
if (request.getTitle() != null &&
    !request.getTitle().isBlank()) {
```

Agar valid title diya gaya hai:

```java
product.setTitle(
    request.getTitle().trim()
);
```

---

# 32. Description update

```java
if (request.getDescription() != null) {
    product.setDescription(
        request.getDescription()
    );
}
```

Description update karega.

---

# 33. Stock update

```java
if (request.getInitialStock() != null &&
    request.getInitialStock() >= 0) {
```

Agar stock `>= 0` hai to update.

Example:

```text
100 → 200
```

But:

```text
100 → -10
```

current code mein simply update nahi karega.

### Important

Ye validation thodi incomplete hai because negative value par exception nahi throw ho rahi; bas update skip ho raha hai.

---

# 34. Image update

```java
if (request.getImageUrl() != null) {
    product.setImageUrl(
        request.getImageUrl()
    );
}
```

Image URL update karta hai.

---

# 35. Status update

```java
if (request.getStatus() != null) {
    product.setStatus(
        request.getStatus()
    );
}
```

Example:

```text
ACTIVE → DISABLED
```

---

# 36. Database update

```java
Product updatedProduct =
    productRepository.save(product);
```

Updated entity database mein save hoti hai.

---

# 37. Cache update

```java
ProductResponse response =
    ProductResponse.fromEntity(updatedProduct);

productCacheService.putProduct(response);
```

Redis mein updated product store hota hai.

Example:

Old:

```text
Laptop → ₹65,000
```

New:

```text
Laptop → ₹60,000
```

Redis ko bhi updated value milti hai.

---

# 38. Active flash-sale cache invalidate

```java
productCacheService.evictActiveFlashSales();
```

Because product ki:

```text
price
status
sale time
```

change ho sakti hai.

Isliye cached active-sale list stale ho sakti hai.

---

# 39. `deleteProduct()`

```java
@Transactional
public void deleteProduct(Long productId)
```

Purpose:

> Product ko permanently database se delete karna aur Redis cache clean karna.

Example:

```text
DELETE /products/101
```

---

# 40. Check product exists

```java
if (!productRepository.existsById(productId)) {
```

Pehle check:

```text
Product 101 exists?
```

Agar nahi:

```java
throw new ResourceNotFoundException(...)
```

→ `404`.

---

# 41. Database delete

```java
productRepository.deleteById(productId);
```

Database se product delete.

---

# 42. Redis cache delete

```java
productCacheService.evictProduct(productId);
```

Ye **very important** hai.

Suppose:

```text
PostgreSQL → Product deleted
Redis → Product still exists
```

Then Redis se deleted product mil sakta tha.

Isliye:

```text
Database Delete
      +
Redis Cache Eviction
```

dono karna zaroori hai.

---

# 43. `validateProductWindow()`

```java
private void validateProductWindow(
    Instant startTime,
    Instant endTime
)
```

Ye internal helper method hai.

### Check 1

```java
if (startTime == null ||
    endTime == null)
```

Agar:

```text
startTime = null
```

ya:

```text
endTime = null
```

to:

```java
throw new InvalidRequestException(
    "Start time and end time cannot be null"
);
```

Frontend ultimately:

```text
400 Bad Request
```

receive kar sakta hai.

---

### Check 2

```java
if (!endTime.isAfter(startTime))
```

Check karta hai:

```text
endTime > startTime
```

Example:

```text
Start = 10:00
End = 12:00
```

✅

But:

```text
Start = 12:00
End = 10:00
```

❌

Exception:

```text
End time must be strictly after start time
```

---

# 44. `validatePricing()`

```java
private void validatePricing(
    java.math.BigDecimal original,
    java.math.BigDecimal flashSale
)
```

Flash-sale pricing rules validate karta hai.

---

## Null check

```java
if (original == null ||
    flashSale == null)
```

Agar price missing:

```text
InvalidRequestException
```

---

## Price comparison

```java
if (flashSale.compareTo(original) > 0)
```

Check:

```text
flashSale > original
```

Example:

```text
Original = ₹80,000
Flash = ₹90,000
```

Condition:

```text
90000 > 80000
```

true.

So:

```java
throw new InvalidRequestException(
    "Flash sale price cannot be greater than original price"
);
```

---

# 45. `BigDecimal.compareTo()` kyu?

Prices ke liye `double` use karna ideal nahi hota.

Example:

```java
double price = 99.99;
```

Floating-point precision issues aa sakte hain.

Money ke liye:

```java
BigDecimal
```

better choice hai.

Comparison:

```java
flashSale.compareTo(original)
```

returns:

```text
< 0  → flashSale smaller
= 0  → equal
> 0  → flashSale greater
```

---

# 46. Complete `ProductService` flow

### Create

```text
CreateProductRequest
        ↓
validate time
        ↓
validate price
        ↓
Product Entity
        ↓
PostgreSQL
        ↓
ProductResponse
        ↓
Redis
```

### Get product

```text
Request
   ↓
Redis
   │
   ├── HIT → Return
   │
   └── MISS
          ↓
      PostgreSQL
          ↓
       Redis
          ↓
       Return
```

### Update

```text
Request
   ↓
Find Product
   ↓
Validate changes
   ↓
Update Entity
   ↓
PostgreSQL
   ↓
Update Redis
   ↓
Evict active-sale cache
   ↓
Response
```

### Delete

```text
Request
   ↓
Check exists
   ↓
PostgreSQL DELETE
   ↓
Redis EVICT
   ↓
Response
```

---

## Sab methods ka simple purpose

| Method                    | Purpose                                         |
| ------------------------- | ----------------------------------------------- |
| `createProduct()`         | New product create karta hai                    |
| `getProductById()`        | Redis → DB fallback se single product deta hai  |
| `getActiveFlashSales()`   | Currently active flash-sale products deta hai   |
| `getAllProducts()`        | Products ko status/pagination ke saath deta hai |
| `updateProduct()`         | Existing product update karta hai               |
| `deleteProduct()`         | Product delete + cache cleanup karta hai        |
| `validateProductWindow()` | Sale start/end time validate karta hai          |
| `validatePricing()`       | Original vs flash-sale price validate karta hai |

### Tumhare Flash Sale project mein iska role

`ProductService` **controller ka business-logic brain** hai:

```text
                 ProductController
                       ↓
                 ProductService
                       ↓
          ┌────────────┴────────────┐
          ↓                         ↓
 ProductRepository          ProductCacheService
          ↓                         ↓
     PostgreSQL                  Redis
```

**Important:** Ye `ProductService` mainly **catalog/product lifecycle** handle kar raha hai. High-concurrency **stock reservation** ka actual critical logic ideally `InventoryService` mein hona chahiye, na ki yahan. Isi separation ki wajah se tumhare flash-sale architecture mein Product Service aur Inventory Service alag rakhe gaye hain.
*/