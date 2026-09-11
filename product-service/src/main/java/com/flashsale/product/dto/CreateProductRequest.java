package com.flashsale.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateProductRequest {

    @NotBlank(message = "Product title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    private String description;

    @NotNull(message = "Original price is required")
    @DecimalMin(value = "0.01", message = "Original price must be greater than 0")
    private BigDecimal originalPrice;

    @NotNull(message = "Flash sale price is required")
    @DecimalMin(value = "0.01", message = "Flash sale price must be greater than 0")
    private BigDecimal flashSalePrice;

    @NotNull(message = "Initial stock is required")
    @Min(value = 1, message = "Initial stock must be at least 1")
    private Integer initialStock;

    @Size(max = 255, message = "Image URL cannot exceed 255 characters")
    private String imageUrl;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;
}
/*`CreateProductRequest.java` ka main purpose hai **Product create karte waqt frontend/client se aane wale data ko validate karna**.

Tumhare code ki har important annotation aur field ko example ke saath samjho:

### 1. `title`

```java
@NotBlank(message = "Product title is required")
@Size(max = 150, message = "Title cannot exceed 150 characters")
private String title;
```

**`@NotBlank`** → title empty, `null`, ya sirf spaces nahi ho sakta.

❌ Invalid:

```json
{"title": ""}
```

❌ Invalid:

```json
{"title": "   "}
```

✅ Valid:

```json
{"title": "Gaming Laptop"}
```

**`@Size(max = 150)`** → title maximum 150 characters ka ho sakta hai.

---

### 2. `description`

```java
private String description;
```

Description optional hai.

Ye:

```json
{
  "description": "High-performance gaming laptop"
}
```

ho sakta hai, aur missing bhi ho sakta hai.

---

### 3. `originalPrice`

```java
@NotNull(message = "Original price is required")
@DecimalMin(value = "0.01", message = "Original price must be greater than 0")
private BigDecimal originalPrice;
```

**`@NotNull`** → price dena compulsory hai.

❌

```json
{
  "originalPrice": null
}
```

**`@DecimalMin("0.01")`** → price minimum `0.01` hona chahiye.

❌

```json
{
  "originalPrice": 0
}
```

❌

```json
{
  "originalPrice": -100
}
```

✅

```json
{
  "originalPrice": 75000
}
```

`BigDecimal` money ke liye `double` se better choice hai because monetary calculations mein precision important hoti hai.

---

### 4. `flashSalePrice`

```java
@NotNull(message = "Flash sale price is required")
@DecimalMin(value = "0.01", message = "Flash sale price must be greater than 0")
private BigDecimal flashSalePrice;
```

Same validation sale price ke liye.

Example:

```json
{
  "originalPrice": 75000,
  "flashSalePrice": 59999
}
```

Invalid:

```json
{
  "originalPrice": 75000,
  "flashSalePrice": 0
}
```

### Important

Current DTO ye ensure **nahi karta** ki:

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

current annotations ke according technically valid ho sakta hai.

Agar business rule hai ki flash-sale price hamesha original price se kam honi chahiye, to **class-level/custom validation** later add karni hogi.

---

### 5. `initialStock`

```java
@NotNull(message = "Initial stock is required")
@Min(value = 1, message = "Initial stock must be at least 1")
private Integer initialStock;
```

Stock compulsory hai.

```text
null ❌
0    ❌
-5   ❌
1    ✅
100  ✅
```

Example:

```json
{
  "initialStock": 100
}
```

Flash-sale system mein ye important hai because product ke initial inventory ko define karta hai.

---

### 6. `imageUrl`

```java
@Size(max = 255, message = "Image URL cannot exceed 255 characters")
private String imageUrl;
```

Image URL optional hai.

Maximum 255 characters.

```json
{
  "imageUrl": "https://example.com/products/laptop.jpg"
}
```

Agar `imageUrl` missing hai, validation fail nahi hogi.

---

### 7. `startTime`

```java
@NotNull(message = "Start time is required")
private Instant startTime;
```

Sale kab start hogi.

Example:

```json
{
  "startTime": "2026-09-01T10:00:00Z"
}
```

`Instant` UTC-based timestamp represent karta hai.

---

### 8. `endTime`

```java
@NotNull(message = "End time is required")
private Instant endTime;
```

Sale kab end hogi.

Example:

```json
{
  "endTime": "2026-09-01T12:00:00Z"
}
```

---

### 9. Important missing validation: Start < End

Tumhare description mein likha hai:

> "Validate temporal constraints (e.g., start time before end time)"

**Lekin current code actually ye validation nahi karta.**

Current code sirf:

```java
@NotNull
private Instant startTime;

@NotNull
private Instant endTime;
```

check karta hai.

Isliye:

```json
{
  "startTime": "2026-09-01T15:00:00Z",
  "endTime": "2026-09-01T10:00:00Z"
}
```

`startTime > endTime` hone ke baad bhi current annotations se automatically reject nahi hoga.

Ye point important hai. **Explanation mein jo claim hai aur implementation mein jo actually hai, dono same nahi hain.**

---

# Lombok annotations

### `@Getter`

```java
@Getter
```

Har field ke getter methods automatically generate karega.

Instead of manually:

```java
public String getTitle() {
    return title;
}
```

Lombok generate karega.

---

### `@Setter`

```java
@Setter
```

Setter methods generate karega:

```java
request.setTitle("Laptop");
```

---

### `@Builder`

```java
@Builder
```

Object ko builder pattern se create kar sakte ho:

```java
CreateProductRequest request =
    CreateProductRequest.builder()
        .title("Gaming Laptop")
        .originalPrice(new BigDecimal("75000"))
        .flashSalePrice(new BigDecimal("59999"))
        .initialStock(100)
        .build();
```

---

### `@NoArgsConstructor`

```java
@NoArgsConstructor
```

Empty constructor generate karta hai:

```java
new CreateProductRequest();
```

Spring/Jackson ke request-body deserialization ke context mein commonly useful hai.

---

### `@AllArgsConstructor`

```java
@AllArgsConstructor
```

Saare fields wala constructor generate karta hai.

Conceptually:

```java
new CreateProductRequest(
    title,
    description,
    originalPrice,
    flashSalePrice,
    initialStock,
    imageUrl,
    startTime,
    endTime
);
```

---

# Complete request example

Frontend se Product Service ko:

```json
{
  "title": "Gaming Laptop",
  "description": "High-performance gaming laptop",
  "originalPrice": 75000,
  "flashSalePrice": 59999,
  "initialStock": 100,
  "imageUrl": "https://example.com/laptop.jpg",
  "startTime": "2026-09-01T10:00:00Z",
  "endTime": "2026-09-01T12:00:00Z"
}
```

milta hai.

Flow:

```text
Frontend
   ↓
POST /products
   ↓
ProductController
   ↓
CreateProductRequest
   ↓
@Valid
   ↓
Validation
   ↓
ProductService
   ↓
ProductRepository
```

Agar validation fail:

```text
@Valid
  ↓
MethodArgumentNotValidException
  ↓
GlobalExceptionHandler
  ↓
400 Bad Request
```

### Is file ka simple purpose

**`CreateProductRequest` database entity nahi hai.**

Ye ek **DTO hai jo incoming product-creation request ko represent aur validate karta hai**.

```text
CreateProductRequest
        ↓
Client ka input
        ↓
Validation
        ↓
ProductService
        ↓
Product Entity
        ↓
Database
```

Aur ek important correction: **is DTO ki current implementation mein `startTime < endTime` aur `flashSalePrice < originalPrice` validation nahi hai**, despite the description suggesting temporal constraints. Agar tum production-ready project bana rahe ho, in dono business rules ko separately implement karna better hoga.
*/