package com.flashsale.common.exception;

public class InsufficientStockException extends BaseCustomException {

    private static final int STATUS_CODE = 409;
    private static final String ERROR_CODE = "INSUFFICIENT_STOCK";

    public InsufficientStockException(String message) {
        super(message, STATUS_CODE, ERROR_CODE);
    }

    public InsufficientStockException(Long productId, Integer requestedQuantity, Integer availableStock) {
        super(String.format("Insufficient stock for product ID %d. Requested: %d, Available: %d",
                productId, requestedQuantity, availableStock), STATUS_CODE, ERROR_CODE);
    }
}


/*
* Haan, **`InsufficientStockException` ki need hai**, especially tumhare **Flash Sale / Inventory Reservation System** mein.

Simple Hinglish mein:

### Ye class kya karti hai?

Jab user koi product buy/reserve karta hai, lekin **available stock requested quantity se kam hota hai**, tab ye custom exception throw hoti hai.

Example:

```text
Available Stock = 5
User requests = 8
```

To system bolega:

```text
Insufficient stock
Requested: 8
Available: 5
```

---

### `STATUS_CODE = 409` kyu?

```java
private static final int STATUS_CODE = 409;
```

**409 Conflict** ka matlab hai ki request valid thi, lekin current resource state ke saath conflict ho gaya.

Yahan:

```text
User → "Mujhe 8 products chahiye"

System → "Request samajh aa gayi,
          lekin mere paas sirf 5 available hain."
```

Isliye `409 Conflict` suitable hai.

---

### `ERROR_CODE` kyu?

```java
private static final String ERROR_CODE = "INSUFFICIENT_STOCK";
```

Frontend ko sirf message check karne ke bajay ek **stable error code** mil sakta hai.

Example response:

```json
{
  "status": 409,
  "errorCode": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for product ID 101. Requested: 8, Available: 5"
}
```

Frontend easily identify kar sakta hai:

```text
errorCode == "INSUFFICIENT_STOCK"
```

aur show kar sakta hai:

> Only 5 items are available.

---

### Ye constructor kyu hai?

```java
public InsufficientStockException(String message)
```

Ye general situation ke liye hai.

Example:

```java
throw new InsufficientStockException("Product is currently out of stock");
```

---

### Ye wala constructor?

```java
public InsufficientStockException(
    Long productId,
    Integer requestedQuantity,
    Integer availableStock
)
```

Ye **inventory system ke liye zyada useful** hai.

Tum directly details provide kar sakte ho:

```java
throw new InsufficientStockException(
    productId,
    requestedQuantity,
    availableStock
);
```

Aur automatically message ban jayega:

```text
Insufficient stock for product ID 101.
Requested: 8, Available: 5
```

---

### Tumhare Flash Sale project mein flow

```text
User clicks BUY
       ↓
Inventory Service
       ↓
Check/Reserve Stock
       ↓
Available < Requested?
       ↓
      YES
       ↓
InsufficientStockException
       ↓
Global Exception Handler
       ↓
HTTP 409
       ↓
Frontend
       ↓
"Only 5 items available"
```

### Iska main benefit

Normal exception:

```java
throw new RuntimeException("Stock not available");
```

se better hai:

```java
throw new InsufficientStockException(
    productId,
    requestedQuantity,
    availableStock
);
```

because tumhare project mein **inventory ek critical business operation** hai.

Custom exception se code clearly communicate karta hai:

> **Ye technical error nahi hai; ye ek specific business rule violation hai — requested stock available nahi hai.**

Tumhare project mein isi pattern se future mein `ReservationExpiredException`, `DuplicateOrderException`, `PaymentFailedException`, etc. bhi aa sakte hain.

* */