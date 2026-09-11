package com.flashsale.product.controller;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.product.dto.CreateProductRequest;
import com.flashsale.product.dto.ProductResponse;
import com.flashsale.product.dto.UpdateProductRequest;
import com.flashsale.product.entity.Product.ProductStatus;
import com.flashsale.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/flash-sales")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getActiveFlashSales(
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        log.debug("Fetching active flash sales list");
        Page<ProductResponse> activeSales = productService.getActiveFlashSales(pageable);
        return ResponseEntity.ok(ApiResponse.success("Active flash sales retrieved successfully", activeSales));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable("id") Long productId) {
        log.debug("Fetching product details for ID: {}", productId);
        ProductResponse response = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(value = "status", required = false) ProductStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Fetching all products with status filter: {}", status);
        Page<ProductResponse> products = productService.getAllProducts(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("Creating new product: {}", request.getTitle());
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable("id") Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("Updating product ID: {}", productId);
        ProductResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("id") Long productId) {
        log.info("Deleting product ID: {}", productId);
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}
/*Bilkul. Ye `ProductController.java` tumhare **Product Service ka API entry point** hai. Iska kaam mainly **HTTP request receive karna → `ProductService` ko kaam dena → standardized `ApiResponse` return karna** hai.

Important: Controller khud database, Redis ya business logic handle **nahi** karta.

```text
Frontend / API Gateway
        ↓
ProductController
        ↓
ProductService
        ↓
Repository / Cache
        ↓
Database / Redis
```

Ab **har method ko example ke saath** samjho.

---

# 1. Class-level annotations

## `@RestController`

```java
@RestController
public class ProductController
```

Spring ko batata hai ki ye class REST API controller hai.

Iske methods HTTP requests handle karenge.

Example:

```http
GET /api/products/101
```

request aayegi → `ProductController` handle karega.

---

## `@RequestMapping("/api/products")`

```java
@RequestMapping("/api/products")
```

Ye common/base URL define karta hai.

Isliye:

```java
@GetMapping
```

actual URL:

```text
GET /api/products
```

Aur:

```java
@GetMapping("/{id}")
```

actual URL:

```text
GET /api/products/101
```

---

## `@RequiredArgsConstructor`

```java
@RequiredArgsConstructor
```

Lombok automatically constructor generate karta hai.

Tumhare paas:

```java
private final ProductService productService;
```

hai.

Lombok effectively ye constructor bana deta hai:

```java
public ProductController(ProductService productService) {
    this.productService = productService;
}
```

Isse Spring `ProductService` inject kar deta hai.

---

## `@Slf4j`

```java
@Slf4j
```

Lombok `log` object create karta hai.

Isliye tum likh sakte ho:

```java
log.debug(...);
log.info(...);
log.warn(...);
log.error(...);
```

---

# 2. `getActiveFlashSales()`

Code:

```java
@GetMapping("/flash-sales")
public ResponseEntity<ApiResponse<Page<ProductResponse>>> getActiveFlashSales(
        @PageableDefault(
            size = 20,
            sort = "startTime",
            direction = Sort.Direction.ASC
        )
        Pageable pageable) {

    log.debug("Fetching active flash sales list");

    Page<ProductResponse> activeSales =
            productService.getActiveFlashSales(pageable);

    return ResponseEntity.ok(
        ApiResponse.success(
            activeSales,
            "Active flash sales retrieved successfully"
        )
    );
}
```

Iska purpose:

> Currently active flash-sale products ki paginated list frontend ko dena.

---

## URL

```http
GET /api/products/flash-sales
```

Example:

```text
GET /api/products/flash-sales?page=0&size=20
```

---

## `@GetMapping("/flash-sales")`

```java
@GetMapping("/flash-sales")
```

Spring ko batata hai:

> Jab GET request `/api/products/flash-sales` par aaye, ye method execute karo.

---

# 3. `Pageable`

```java
Pageable pageable
```

Ye pagination information contain karta hai.

Example:

```text
page = 0
size = 20
sort = startTime
direction = ASC
```

Matlab:

> Pehle 20 products do aur unhe `startTime` ke according ascending order mein arrange karo.

---

## `@PageableDefault`

```java
@PageableDefault(
    size = 20,
    sort = "startTime",
    direction = Sort.Direction.ASC
)
```

Agar frontend pagination details nahi bhejta, default values use hongi.

### Example

Frontend:

```http
GET /api/products/flash-sales
```

to automatically:

```text
size = 20
sort = startTime
order = ASC
```

---

# 4. `log.debug()`

```java
log.debug("Fetching active flash sales list");
```

Developer ke debugging ke liye.

Production mein actual business operation nahi karta.

---

# 5. `productService.getActiveFlashSales()`

```java
Page<ProductResponse> activeSales =
        productService.getActiveFlashSales(pageable);
```

**Important:** Controller khud flash-sale logic nahi kar raha.

Ye Service ko bol raha hai:

> "Mujhe active flash-sale products de do."

Flow:

```text
Request
  ↓
Controller
  ↓
ProductService
  ↓
Repository
  ↓
Database
```

---

# 6. `ApiResponse.success()`

```java
ApiResponse.success(
    activeSales,
    "Active flash sales retrieved successfully"
)
```

Tumhare project mein standard response format maintain karne ke liye.

Conceptually response:

```json
{
  "success": true,
  "message": "Active flash sales retrieved successfully",
  "data": {
    "content": [
      {
        "id": 101,
        "title": "Gaming Laptop",
        "price": 75000
      }
    ],
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

# 7. `ResponseEntity.ok()`

```java
ResponseEntity.ok(...)
```

HTTP:

```text
200 OK
```

return karega.

---

# 8. `getProductById()`

Code:

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponse>>
getProductById(
        @PathVariable("id") Long productId) {

    log.debug(
        "Fetching product details for ID: {}",
        productId
    );

    ProductResponse response =
            productService.getProductById(productId);

    return ResponseEntity.ok(
        ApiResponse.success(
            response,
            "Product retrieved successfully"
        )
    );
}
```

Purpose:

> Ek specific product ki details retrieve karna.

---

## Example

Frontend request:

```http
GET /api/products/101
```

Yahan:

```text
{id} = 101
```

---

## `@PathVariable`

```java
@PathVariable("id") Long productId
```

URL se `101` uthakar:

```java
productId = 101L
```

banata hai.

---

## Service call

```java
productService.getProductById(productId);
```

Controller ProductService ko:

```text
101
```

send karta hai.

Service potentially:

```text
Redis Cache
    ↓
Found?
 ┌──┴──┐
Yes   No
 ↓     ↓
Return Database
       ↓
      Return
```

Tumhare flash-sale system mein ye **cached product details** ke liye particularly useful hai.

---

## Response

Product mil gaya:

```json
{
  "success": true,
  "message": "Product retrieved successfully",
  "data": {
    "id": 101,
    "title": "Gaming Laptop",
    "price": 75000
  }
}
```

HTTP:

```text
200 OK
```

Agar product nahi mila aur Service `ResourceNotFoundException` throw karti hai:

```text
ProductService
      ↓
ResourceNotFoundException
      ↓
GlobalExceptionHandler
      ↓
404 NOT_FOUND
```

Controller ko manually 404 handle karne ki zarurat nahi.

---

# 9. `getAllProducts()`

Code:

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<ProductResponse>>>
getAllProducts(
        @RequestParam(
            value = "status",
            required = false
        )
        ProductStatus status,

        @PageableDefault(
            size = 20,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        )
        Pageable pageable) {
```

Purpose:

> Product catalog ke products retrieve karna, optionally status filter ke saath.

---

## URL without filter

```http
GET /api/products
```

Example response:

```text
Product 105
Product 104
Product 103
...
```

because:

```text
createdAt DESC
```

means newest products first.

---

# 10. `@RequestParam`

```java
@RequestParam(
    value = "status",
    required = false
)
ProductStatus status
```

Frontend optionally status bhej sakta hai.

Example:

```http
GET /api/products?status=ACTIVE
```

Then:

```java
status = ProductStatus.ACTIVE
```

---

### `required = false`

Matlab status optional hai.

Ye bhi valid:

```http
GET /api/products
```

Then:

```java
status = null
```

---

# 11. `ProductStatus`

```java
ProductStatus status
```

Ye tumhari `Product` entity ke enum se aa raha hai:

```java
Product.ProductStatus
```

Possible values project ke implementation par depend karengi, for example:

```text
ACTIVE
INACTIVE
DRAFT
SOLD_OUT
```

---

# 12. Service call

```java
Page<ProductResponse> products =
        productService.getAllProducts(
            status,
            pageable
        );
```

Controller service ko do cheezein deta hai:

```text
status
+
pagination
```

Service actual business/database operation karegi.

---

# 13. `createProduct()`

Code:

```java
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>>
createProduct(
        @Valid
        @RequestBody
        CreateProductRequest request) {
```

Purpose:

> New product create karna.

---

## Request

Frontend:

```http
POST /api/products
Content-Type: application/json
```

Body:

```json
{
  "title": "Gaming Laptop",
  "price": 75000,
  "stock": 100,
  "startTime": "2026-09-01T10:00:00",
  "endTime": "2026-09-01T14:00:00"
}
```

---

# 14. `@RequestBody`

```java
@RequestBody CreateProductRequest request
```

JSON ko Java object mein convert karta hai.

```text
JSON
 ↓
CreateProductRequest
```

Example:

```text
request.getTitle()
        ↓
"Gaming Laptop"
```

---

# 15. `@Valid`

```java
@Valid
```

Request DTO ke validation annotations check karta hai.

Suppose:

```java
@NotBlank
private String title;

@Positive
private BigDecimal price;
```

Frontend sends:

```json
{
  "title": "",
  "price": -100
}
```

Validation fail hogi.

Then:

```text
@Valid
   ↓
MethodArgumentNotValidException
   ↓
GlobalExceptionHandler
   ↓
400 Bad Request
```

`createProduct()` ka service call execute nahi hoga.

---

# 16. `productService.createProduct()`

```java
ProductResponse response =
        productService.createProduct(request);
```

Controller service ko validated request deta hai.

Service:

```text
CreateProductRequest
        ↓
ProductService
        ↓
Product Entity
        ↓
Repository
        ↓
Database
```

---

# 17. `HttpStatus.CREATED`

```java
return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(...);
```

Product successfully create hone par:

```http
201 Created
```

return hota hai.

### Why 201?

Because new resource create hua hai.

---

# 18. `updateProduct()`

Code:

```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponse>>
updateProduct(
        @PathVariable("id") Long productId,
        @Valid @RequestBody UpdateProductRequest request) {
```

Purpose:

> Existing product ko update karna.

Example:

```http
PUT /api/products/101
```

Body:

```json
{
  "title": "Gaming Laptop Pro",
  "price": 70000
}
```

---

## `@PathVariable`

```text
101
```

product ID hai.

---

## `@RequestBody`

Request body:

```json
{
  "title": "Gaming Laptop Pro",
  "price": 70000
}
```

Java object:

```text
UpdateProductRequest
```

mein convert hota hai.

---

## `@Valid`

Updated fields validate honge.

---

## Service

```java
productService.updateProduct(
    productId,
    request
);
```

Service ko:

```text
ID = 101
+
new product data
```

milta hai.

---

## Response

```java
ResponseEntity.ok(...)
```

means:

```http
200 OK
```

---

# 19. `deleteProduct()`

Code:

```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>>
deleteProduct(
        @PathVariable("id") Long productId) {

    log.info("Deleting product ID: {}", productId);

    productService.deleteProduct(productId);

    return ResponseEntity.ok(
        ApiResponse.success(
            null,
            "Product deleted successfully"
        )
    );
}
```

Purpose:

> Product delete karna.

Request:

```http
DELETE /api/products/101
```

---

## PathVariable

```text
productId = 101
```

---

## Service call

```java
productService.deleteProduct(productId);
```

Controller khud database se delete nahi karta.

Service:

```text
ProductController
       ↓
ProductService
       ↓
ProductRepository
       ↓
Database
```

---

## `ApiResponse<Void>`

Yahan actual product data return nahi ho raha.

Isliye:

```java
ApiResponse<Void>
```

use hua.

Response conceptually:

```json
{
  "success": true,
  "message": "Product deleted successfully",
  "data": null
}
```

---

# 20. Saare endpoints ek jagah

Tumhare controller ke paas currently **6 endpoints** hain:

| Method | Endpoint                    | Purpose                    |
| ------ | --------------------------- | -------------------------- |
| GET    | `/api/products/flash-sales` | Active flash-sale products |
| GET    | `/api/products/{id}`        | Single product             |
| GET    | `/api/products`             | Product catalog            |
| POST   | `/api/products`             | Create product             |
| PUT    | `/api/products/{id}`        | Update product             |
| DELETE | `/api/products/{id}`        | Delete product             |

---

# 21. Complete architecture mein iska role

```text
                 Frontend
                    │
                    ▼
               API Gateway
                    │
                    ▼
             ProductController
                    │
          ┌─────────┼─────────┐
          │         │         │
          ▼         ▼         ▼
      Product    Flash Sale  Product
      Details      List      CRUD
          │         │         │
          └─────────┼─────────┘
                    ▼
             ProductService
                    │
              ┌─────┴─────┐
              ▼           ▼
          Redis Cache   Repository
                            │
                            ▼
                       PostgreSQL
```

### Sabse important concept

`ProductController` ko **thin controller** rakhna hai.

❌ Aisa nahi:

```java
@GetMapping("/{id}")
public Product getProduct(...) {

    // database query
    // Redis logic
    // business rules
    // validation
    // calculations
}
```

✅ Tumhare current design ka idea:

```java
@GetMapping("/{id}")
public ResponseEntity<...> getProductById(...) {

    ProductResponse response =
        productService.getProductById(productId);

    return ResponseEntity.ok(...);
}
```

Yani:

**Controller = Request/Response handling**

**Service = Business logic**

**Repository = Database access**

**Redis service = Cache handling**

**GlobalExceptionHandler = Error handling**

Ye separation tumhare microservice architecture ke liye kaafi important hai.
*/
