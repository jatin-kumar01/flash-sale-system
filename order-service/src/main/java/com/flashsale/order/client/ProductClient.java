package com.flashsale.order.client;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.order.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", path = "/api/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ApiResponse<ProductResponse> getProductById(@PathVariable("id") Long id);
}
/*Yes. `ProductClient.java` is a **Feign client** used by `order-service` to communicate with `product-service`.

### Flow

```text
OrderService
     ↓
ProductClient
     ↓
Eureka
     ↓
product-service
     ↓
ProductResponse
```

### `@FeignClient`

```java
@FeignClient(
    name = "product-service",
    path = "/api/products"
)
```

This tells Spring that this interface communicates with the service registered as:

```text
product-service
```

The actual host/port doesn't need to be hardcoded.

For example, if Eureka has:

```text
PRODUCT-SERVICE
 ├── instance 1 → :8082
 └── instance 2 → :8092
```

Feign can resolve the service through discovery and use an available instance.

---

### `getProductById()`

```java
@GetMapping("/{id}")
ApiResponse<ProductResponse> getProductById(
        @PathVariable("id") Long id);
```

Calling:

```java
productClient.getProductById(101L);
```

results conceptually in:

```http
GET /api/products/101
```

and returns:

```text
ApiResponse<ProductResponse>
```

The `ProductResponse` can contain things such as:

```text
productId
name
price
flashSalePrice
status
saleStartTime
saleEndTime
```

Then `OrderService` can validate:

```text
Product exists?
      ↓
Active?
      ↓
Flash sale currently active?
      ↓
Use authoritative price
      ↓
Reserve inventory
```

### Why this is important

The frontend should **not** be trusted for the price.

For example:

```text
Frontend says:
price = ₹100
```

But Product Service says:

```text
flashSalePrice = ₹150
```

`OrderService` should use the **Product Service's authoritative price**, not the value supplied by the client.

---

## ⚠️ One thing to verify

Your dependency says:

```java
import com.flashsale.order.dto.ProductResponse;
```

So you must create:

```text
order-service/
└── src/main/java/com/flashsale/order/
    └── dto/
        └── ProductResponse.java
```

Also, `ProductClient` assumes Product Service exposes:

```text
GET /api/products/{id}
```

and returns the same `ApiResponse<ProductResponse>` structure.

If Product Service's actual controller/DTO differs, this Feign client will need to match it.

### Verdict

**`ProductClient.java` is clean and appropriate.** No major changes are needed right now.

Next file: **`ProductResponse.java`**.
*/