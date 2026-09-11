package com.flashsale.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_status", columnList = "status"),
        @Index(name = "idx_products_sale_window", columnList = "startTime, endTime")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal flashSalePrice;

    @Column(nullable = false)
    private Integer initialStock;

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public enum ProductStatus {
        DRAFT,
        ACTIVE,
        ENDED,
        DISABLED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isSaleActive() {
        Instant now = Instant.now();
        return this.status == ProductStatus.ACTIVE
                && !now.isBefore(this.startTime)
                && !now.isAfter(this.endTime);
    }
}
/*Is `product-service/pom.xml` ka purpose simple words mein ye hai ki **Product Service ke liye Maven ko batana ki kaunse frameworks/libraries chahiye aur service kaise build hogi**.

### Important dependencies

| Dependency                                   | Kya kaam hai?                                                    |
| -------------------------------------------- | ---------------------------------------------------------------- |
| `common`                                     | Shared DTOs, exceptions, events use karne ke liye                |
| `spring-boot-starter-web`                    | REST APIs banane ke liye                                         |
| `spring-boot-starter-validation`             | Request validation (`@Valid`, `@NotBlank`, etc.)                 |
| `spring-boot-starter-data-jpa`               | Java objects ko PostgreSQL database se connect/map karne ke liye |
| `postgresql`                                 | PostgreSQL database driver                                       |
| `spring-boot-starter-data-redis`             | Redis se caching ke liye                                         |
| `spring-cloud-starter-netflix-eureka-client` | Product Service ko Eureka mein register karne ke liye            |
| `spring-boot-starter-actuator`               | Health/monitoring endpoints ke liye                              |

### Architecture mein iska role

```text
                 Product Service
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
    PostgreSQL       Redis         Eureka
    Permanent       Fast cache     Discovery
     storage
```

Aur request:

```text
User
 ↓
API Gateway
 ↓
lb://PRODUCT-SERVICE
 ↓
Product Service
 ↓
Redis → Product data available?
 ↓
If not
 ↓
PostgreSQL
```

### Ek important correction

Tumhare pasted POM ke comments mein:

> `Redis for Product Read-Through Caching`

likha hai, lekin **sirf `spring-boot-starter-data-redis` dependency add karne se read-through caching automatically implement nahi hoti**.

Uske liye baad mein actual:

* Redis configuration
* cache/service logic
* cache keys
* TTL
* cache invalidation

implement karna padega.

Similarly, `spring-boot-starter-actuator` add karne se **Prometheus monitoring automatically complete nahi hoti**; metrics exposure/configuration baad mein karni hogi.

### Testing

Tumhare system mein global Maven install nahi tha, isliye:

```powershell
.\mvnw.cmd clean compile -pl product-service
```

use karo.

Agar `common` dependency ko reactor build mein resolve karne ki problem aaye, root se:

```powershell
.\mvnw.cmd clean compile
```

better rahega.

**Overall:** ye POM Product Service ki dependency/build foundation hai. Is file mein actual product logic nahi hai; actual logic baad mein `Entity → Repository → Service → Controller` files mein aayega.
*/