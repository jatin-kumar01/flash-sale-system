package com.flashsale.inventory.entity;

import com.flashsale.common.exception.InvalidRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "inventories", indexes = {
        @Index(name = "idx_inventory_product_id", columnList = "productId", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer totalStock;

    @Column(nullable = false)
    private Integer availableStock;

    @Column(nullable = false)
    @Builder.Default
    private Integer lockedStock = 0;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.lockedStock == null) {
            this.lockedStock = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void reserveStock(int quantity) {
        if (quantity <= 0) {
            throw new InvalidRequestException("Quantity to reserve must be greater than zero");
        }
        if (this.availableStock < quantity) {
            throw new InvalidRequestException("Insufficient available stock for reservation");
        }
        this.availableStock -= quantity;
        this.lockedStock += quantity;
    }

    public void releaseStock(int quantity) {
        if (quantity <= 0) {
            throw new InvalidRequestException("Quantity to release must be greater than zero");
        }
        if (this.lockedStock < quantity) {
            throw new InvalidRequestException("Cannot release more stock than currently locked");
        }
        this.lockedStock -= quantity;
        this.availableStock += quantity;
    }

    public void deductLockedStock(int quantity) {
        if (quantity <= 0) {
            throw new InvalidRequestException("Quantity to deduct must be greater than zero");
        }
        if (this.lockedStock < quantity) {
            throw new InvalidRequestException("Cannot deduct more stock than currently locked");
        }
        this.lockedStock -= quantity;
        this.totalStock -= quantity;
    }
}
/*This `Inventory.java` is the **core database/entity class for inventory management**. It is especially important for your flash-sale project because it represents the stock state that must remain correct during concurrent purchases.

### Simple flow

```text
Product
   ↓
Inventory
   │
   ├── totalStock
   ├── availableStock
   ├── lockedStock
   └── version
```

For example, initially:

```text
totalStock     = 100
availableStock = 100
lockedStock    = 0
```

If a user reserves 3:

```text
totalStock     = 100
availableStock = 97
lockedStock    = 3
```

If payment succeeds:

```text
totalStock     = 97
availableStock = 97
lockedStock    = 0
```

If payment fails:

```text
totalStock     = 100
availableStock = 100
lockedStock    = 0
```

---

# 1. `@Entity`

```java
@Entity
```

This tells JPA:

> `Inventory` Java class ko database table ke saath map karna hai.

So:

```text
Java class
Inventory
      ↓
Database table
inventories
```

---

# 2. `@Table`

```java
@Table(name = "inventories", indexes = {
    @Index(
        name = "idx_inventory_product_id",
        columnList = "productId",
        unique = true
    )
})
```

Database table ka naam:

```text
inventories
```

And `productId` par unique index hai.

### Why unique?

Ek product ke liye ideally ek hi inventory record hona chahiye.

Wrong:

```text
productId = 101 → stock 50
productId = 101 → stock 30
```

Correct:

```text
productId = 101 → stock 80
```

---

# 3. `id`

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Ye inventory record ka primary key hai.

Example:

```text
id = 1
productId = 101
```

`id` automatically database generate karega.

---

# 4. `productId`

```java
@Column(nullable = false, unique = true)
private Long productId;
```

Ye batata hai ki inventory kis product ki hai.

Example:

```text
Product:
id = 101
name = Gaming Laptop

Inventory:
productId = 101
```

Important:

**Ye `Product` entity ka object nahi hai.**

Sirf us product ki ID store ho rahi hai.

---

# 5. `totalStock`

```java
@Column(nullable = false)
private Integer totalStock;
```

Ye **actual remaining inventory** ko represent karta hai according to the implemented domain methods.

Example:

Initially:

```text
totalStock = 100
```

3 items sell hone ke baad:

```text
totalStock = 97
```

---

# 6. `availableStock`

```java
private Integer availableStock;
```

Ye stock batata hai jo currently reservation ke liye available hai.

Example:

```text
totalStock     = 100
availableStock = 95
lockedStock    = 5
```

Matlab:

```text
95 → new users reserve kar sakte hain
5  → already temporarily reserved hai
```

---

# 7. `lockedStock`

```java
@Builder.Default
private Integer lockedStock = 0;
```

`lockedStock` ka matlab:

> Kisi user ke liye temporarily reserve/hold kiya hua stock.

Example:

```text
Stock = 100
```

User A:

```text
Buy 2
```

Reservation ke baad:

```text
availableStock = 98
lockedStock    = 2
```

Lekin abhi product **sold nahi hua** hai.

Payment successful hone ka wait ho raha hai.

---

# 8. `@Version`

```java
@Version
@Column(nullable = false)
private Long version;
```

Ye **optimistic locking** ke liye hai.

Flash-sale project mein ye important concept hai.

Suppose:

```text
availableStock = 10
version = 5
```

Do transactions simultaneously update karne ki koshish karti hain.

```text
Transaction A → version 5
Transaction B → version 5
```

A successfully update karta hai:

```text
version 5 → 6
```

B purani version `5` ke basis par update karne ki koshish karega.

Hibernate detect kar sakta hai:

```text
Expected version = 5
Actual version   = 6
```

Aur update fail ho jayega rather than silently overwriting another transaction's change.

### Simple idea

```text
@Version
    ↓
"Someone else changed this record?"
    ↓
YES → prevent stale update
NO  → allow update
```

**Important:** `@Version` alone does not magically solve every flash-sale race condition. The actual reservation strategy in `InventoryService`/repository also matters.

---

# 9. `createdAt`

```java
private Instant createdAt;
```

Inventory record kab create hua.

Example:

```text
2026-09-05T14:30:00Z
```

---

# 10. `updatedAt`

```java
private Instant updatedAt;
```

Last time inventory record update hua.

Example:

```text
Stock reservation
      ↓
updatedAt changes
```

---

# 11. `@PrePersist`

```java
@PrePersist
protected void onCreate() {
```

Ye method **database mein new Inventory save hone se just pehle** execute hoti hai.

```java
this.createdAt = Instant.now();
this.updatedAt = Instant.now();
```

Automatically current timestamp set karta hai.

And:

```java
if (this.lockedStock == null) {
    this.lockedStock = 0;
}
```

Agar locked stock `null` hai:

```text
null → 0
```

---

# 12. `@PreUpdate`

```java
@PreUpdate
protected void onUpdate() {
    this.updatedAt = Instant.now();
}
```

Jab inventory update hoti hai:

```text
availableStock change
       ↓
updatedAt = current time
```

Automatically update time maintain hota hai.

---

# 13. `reserveStock()`

Ye class ka **most important business method** hai.

```java
public void reserveStock(int quantity)
```

Suppose:

```text
availableStock = 10
lockedStock = 0
```

User wants:

```text
quantity = 3
```

### Step 1 — quantity check

```java
if (quantity <= 0)
```

Agar:

```text
quantity = 0
```

ya:

```text
quantity = -2
```

to invalid request.

Exception:

```text
Quantity to reserve must be greater than zero
```

---

### Step 2 — stock check

```java
if (this.availableStock < quantity)
```

Suppose:

```text
availableStock = 2
quantity = 5
```

Then:

```text
2 < 5
```

true.

Exception:

```text
Insufficient available stock for reservation
```

---

### Step 3 — reservation

```java
this.availableStock -= quantity;
this.lockedStock += quantity;
```

Before:

```text
available = 10
locked    = 0
```

Reserve 3:

```text
available = 7
locked    = 3
```

So:

```text
AVAILABLE
    ↓
RESERVED/LOCKED
```

---

# 14. `releaseStock()`

Suppose user reserved 3 products:

```text
available = 7
locked    = 3
```

But payment failed.

We need to release them.

```java
releaseStock(3);
```

Result:

```text
available = 10
locked    = 0
```

### First validation

```java
if (quantity <= 0)
```

Can't release:

```text
0
-5
```

---

### Second validation

```java
if (this.lockedStock < quantity)
```

Suppose:

```text
lockedStock = 2
quantity = 5
```

You can't release 5 because only 2 are locked.

Exception:

```text
Cannot release more stock than currently locked
```

---

# 15. `deductLockedStock()`

This method is used when the reservation becomes an actual sale.

Suppose:

```text
availableStock = 7
lockedStock = 3
totalStock = 100
```

Payment succeeds.

```java
deductLockedStock(3);
```

Result:

```text
availableStock = 7
lockedStock = 0
totalStock = 97
```

Why?

Because those 3 items are no longer merely reserved—they are **sold**.

So:

```text
RESERVED
   ↓
SOLD
```

---

# 16. Why three stock fields?

This is very important for understanding your project.

Suppose:

```text
totalStock = 100
availableStock = 100
lockedStock = 0
```

### User reserves 10

```text
totalStock = 100
availableStock = 90
lockedStock = 10
```

Those 10 are temporarily held.

### Payment succeeds

```text
totalStock = 90
availableStock = 90
lockedStock = 0
```

### Payment fails

Instead:

```text
totalStock = 100
availableStock = 100
lockedStock = 0
```

So the system can distinguish:

```text
Available → Can be reserved
Locked    → Temporarily reserved
Total     → Remaining physical stock after confirmed sales
```

---

# 17. Lombok annotations

These aren't business logic.

### `@Getter`

Automatically creates getters:

```java
getProductId()
getAvailableStock()
```

### `@Setter`

Creates setters:

```java
setAvailableStock()
```

### `@Builder`

Allows:

```java
Inventory inventory = Inventory.builder()
        .productId(101L)
        .totalStock(100)
        .availableStock(100)
        .build();
```

### `@NoArgsConstructor`

Creates:

```java
new Inventory();
```

JPA needs a no-argument constructor.

### `@AllArgsConstructor`

Creates constructor with all fields.

---

# Overall example

Suppose your flash sale has:

```text
iPhone
Product ID = 101
Stock = 100
```

Initial:

```text
┌─────────────────────┐
│ totalStock     100  │
│ availableStock 100  │
│ lockedStock      0  │
└─────────────────────┘
```

100 users successfully reserve one each:

```text
┌─────────────────────┐
│ totalStock     100  │
│ availableStock   0  │
│ lockedStock    100  │
└─────────────────────┘
```

Payment succeeds for 80:

```text
┌─────────────────────┐
│ totalStock      20  │
│ availableStock   0  │
│ lockedStock     20  │
└─────────────────────┘
```

The other 20 reservations expire/release:

```text
┌─────────────────────┐
│ totalStock      20  │
│ availableStock  20  │
│ lockedStock      0  │
└─────────────────────┘
```

So now 20 products remain available for purchase.

---

## The main idea to remember

`Inventory.java` ka main kaam **database mein stock ki state maintain karna + valid stock transitions ko control karna** hai.

```text
                 Inventory
                     │
       ┌─────────────┼──────────────┐
       ▼             ▼              ▼
 totalStock    availableStock   lockedStock
       │             │              │
       │             │              │
       │        New reservation     │
       │             ↓              │
       │       available ↓          │
       │       locked ↑             │
       │                            │
       └──── Payment Success ───────┘
                     ↓
              totalStock ↓
              lockedStock ↓
```

**But one key point:** `Inventory.java` is only the **entity/domain layer**. The actual concurrent reservation workflow, transaction boundaries, Redis/Kafka interaction, and database synchronization will be handled in the upcoming `InventoryRepository` and `InventoryService`.
*/