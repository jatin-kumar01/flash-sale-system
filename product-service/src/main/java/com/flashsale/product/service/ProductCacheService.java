package com.flashsale.product.service;

import com.flashsale.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:";
    private static final String ACTIVE_FLASH_SALES_CACHE_KEY = "products:flash_sales:active";

    @Value("${product.cache.ttl-minutes:10}")
    private long defaultTtlMinutes;

    @Value("${product.cache.flash-sale-ttl-seconds:60}")
    private long flashSaleTtlSeconds;

    public Optional<ProductResponse> getProduct(Long productId) {
        String key = buildProductKey(productId);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ProductResponse) {
                log.debug("Cache hit for product ID: {}", productId);
                return Optional.of((ProductResponse) cached);
            }
        } catch (Exception ex) {
            log.warn("Failed to retrieve product {} from Redis cache: {}", productId, ex.getMessage());
        }
        return Optional.empty();
    }

    public void putProduct(ProductResponse product) {
        if (product == null || product.getId() == null) {
            return;
        }

        String key = buildProductKey(product.getId());
        Duration ttl = product.isSaleActive()
                ? Duration.ofSeconds(flashSaleTtlSeconds)
                : Duration.ofMinutes(defaultTtlMinutes);

        try {
            redisTemplate.opsForValue().set(key, product, ttl);
            log.debug("Cached product ID: {} with TTL: {}", product.getId(), ttl);
        } catch (Exception ex) {
            log.warn("Failed to save product {} to Redis cache: {}", product.getId(), ex.getMessage());
        }
    }

    public void evictProduct(Long productId) {
        String key = buildProductKey(productId);
        try {
            redisTemplate.delete(key);
            redisTemplate.delete(ACTIVE_FLASH_SALES_CACHE_KEY);
            log.debug("Evicted cache for product ID: {}", productId);
        } catch (Exception ex) {
            log.warn("Failed to evict cache for product {}: {}", productId, ex.getMessage());
        }
    }

    public void evictActiveFlashSales() {
        try {
            redisTemplate.delete(ACTIVE_FLASH_SALES_CACHE_KEY);
        } catch (Exception ex) {
            log.warn("Failed to evict active flash sales cache: {}", ex.getMessage());
        }
    }

    private String buildProductKey(Long productId) {
        return PRODUCT_CACHE_KEY_PREFIX + productId;
    }
}
/*Yes. Next should be **`ProductService.java`**.

Because your existing `Product.java`, `ProductRepository.java`, `CreateProductRequest.java`, `UpdateProductRequest.java`, and `ProductResponse.java` determine the exact fields/methods, **don't let Gemini invent them**.

Use this prompt in Gemini:

```text
FILE: ProductService.java

LOCATION:
product-service/src/main/java/com/flashsale/product/service/ProductService.java

PURPOSE:
Implement the main business/service layer for the Product Service. It must coordinate product CRUD operations between ProductRepository and ProductCacheService while keeping database access, caching, validation, and response mapping out of controllers.

IMPORTANT:
Before generating this file, inspect the existing project files:
- Product.java
- ProductRepository.java
- CreateProductRequest.java
- UpdateProductRequest.java
- ProductResponse.java
- ProductCacheService.java
- Existing exception classes in common
- product-service/pom.xml

Do NOT invent fields, repository methods, constructors, DTO fields, or exception classes that do not already exist.

If an equivalent ProductService.java already exists, modify it only where required. Do not rewrite working logic unnecessarily.

RESPONSIBILITIES:

1. Product creation
   - Accept CreateProductRequest.
   - Validate business-level conditions using existing project logic/classes.
   - Create the Product entity using the existing entity structure.
   - Save it through ProductRepository.
   - Convert the saved entity to ProductResponse using the existing project structure.
   - Cache the resulting ProductResponse through ProductCacheService.
   - Return the ProductResponse.

2. Get product by ID
   - First attempt to retrieve the product through ProductCacheService.
   - If the cache contains the product, return it immediately.
   - If the cache misses, retrieve the product from ProductRepository.
   - If the product does not exist, use the project's existing ResourceNotFoundException/BaseCustomException mechanism.
   - Convert the entity to ProductResponse.
   - Store the response in ProductCacheService.
   - Return the response.

   Expected flow:

   Controller
       ↓
   ProductService
       ↓
   ProductCacheService
       ↓
   Redis

   Cache miss:

   ProductService
       ↓
   ProductRepository
       ↓
   PostgreSQL
       ↓
   ProductResponse
       ↓
   ProductCacheService
       ↓
   Redis

3. Get all products
   - Retrieve products using ProductRepository.
   - Convert them to the existing ProductResponse structure.
   - Do not directly access Redis unless the existing architecture already provides an appropriate aggregate-cache method.
   - Do not introduce new caching behavior that is not supported by ProductCacheService.

4. Update product
   - Find the existing product using ProductRepository.
   - Throw the project's existing not-found exception if it does not exist.
   - Apply only the fields provided by UpdateProductRequest according to the existing DTO/entity design.
   - Save the updated entity.
   - Convert it to ProductResponse.
   - Evict the old cache entry using ProductCacheService.evictProduct(productId).
   - Cache the updated ProductResponse when appropriate.
   - Preserve the existing flash-sale state and inventory-related fields according to the existing entity design.

5. Delete product
   - Verify that the product exists.
   - Delete it using ProductRepository.
   - Evict its Redis cache entry using ProductCacheService.evictProduct(productId).
   - Do not implement inventory deletion logic here unless the existing project explicitly requires it.

6. Cache behavior
   - ProductService must communicate with Redis only through ProductCacheService.
   - NEVER inject RedisTemplate directly into ProductService.
   - Do not duplicate Redis logic inside ProductService.
   - ProductCacheService already handles Redis failures gracefully.

7. DTO mapping
   - Follow the existing ProductResponse structure.
   - Do not create a new mapper dependency unless one already exists.
   - If the project currently uses a constructor, static factory method, or mapper for Product → ProductResponse, reuse it.
   - Do not invent a different mapping strategy.

8. Exception handling
   - Reuse existing exceptions from the common module.
   - Do not create duplicate exception classes.
   - Do not catch generic Exception just to hide errors.
   - Let GlobalExceptionHandler handle exceptions at the REST boundary.

9. Transaction handling
   - Use @Transactional only where required by the existing repository/service architecture.
   - Do not introduce unnecessary transaction boundaries.

10. Logging
   - Use the project's existing logging convention.
   - Log useful service-level events such as product creation, update, deletion, and cache-related decisions.
   - Do not log sensitive information.

DEPENDENCIES:

Use only dependencies that actually exist in the project, such as:

- ProductRepository
- ProductCacheService
- Product
- CreateProductRequest
- UpdateProductRequest
- ProductResponse
- Existing common exceptions
- Existing Spring annotations

Do NOT introduce unnecessary dependencies.

ARCHITECTURE:

The service must maintain:

Controller
    ↓
ProductService
    ├── ProductRepository
    └── ProductCacheService
             ↓
           Redis

ProductService must NOT:

- Access RedisTemplate directly
- Access PostgreSQL directly
- Contain controller logic
- Contain HTTP response construction
- Create API routes
- Make network calls
- Implement frontend logic

IMPORTANT PRESERVATION RULE:

First inspect the existing files and adapt ProductService to their actual APIs.

Do not assume repository methods such as:
- findById()
- existsById()
- findAll()
- deleteById()

unless they are actually available or inherited from the existing repository.

Do not assume Product fields such as:
- name
- price
- stock
- salePrice
- saleStartTime
- saleEndTime

unless they actually exist in Product.java.

Do not assume ProductResponse constructors or methods unless they actually exist.

If an existing utility/mapper is already present, reuse it.

TESTING:

After creating ProductService.java:

1. Compile the product-service module:

./mvnw clean test-compile -pl product-service

Windows PowerShell:

.\mvnw.cmd clean test-compile -pl product-service

2. Fix only compilation errors caused by ProductService.java.

3. Do not modify unrelated files merely to make the code compile.

4. Verify the final dependency flow:

Controller
    ↓
ProductService
    ↓
ProductRepository
    +
ProductCacheService
    ↓
Redis

FINAL REQUIREMENT:

Generate ONLY ProductService.java in this step.

Do not generate ProductController.java, ProductRepository.java, DTOs, entities, configuration, tests, or any other file unless an existing file must be minimally modified to resolve a direct compilation issue.

Before generating the implementation, inspect the existing files and match their exact fields, methods, constructors, annotations, package names, and exception classes.
```

### Why this is the right next file

Your current `ProductCacheService` is only responsible for **Redis caching**:

```text
ProductCacheService
       ↓
      Redis
```

`ProductService` will become the layer that coordinates the actual product operation:

```text
Controller
    ↓
ProductService
    ├── ProductRepository → PostgreSQL
    │
    └── ProductCacheService → Redis
```

This separation is important for your interview architecture because the controller won't contain business logic, and the service won't contain Redis implementation details.
*/