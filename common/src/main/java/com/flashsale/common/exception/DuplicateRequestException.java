package com.flashsale.common.exception;

public class DuplicateRequestException extends BaseCustomException {

    private static final int STATUS_CODE = 409;
    private static final String ERROR_CODE = "DUPLICATE_REQUEST";

    public DuplicateRequestException(String message) {
        super(message, STATUS_CODE, ERROR_CODE);
    }

    public DuplicateRequestException(String idempotencyKey, String operation) {
        super(String.format("Duplicate request detected for operation '%s' with idempotency key: '%s'",
                operation, idempotencyKey), STATUS_CODE, ERROR_CODE);
    }
}
/*
* Haan, **`DuplicateRequestException.java` tumhare project mein useful hai**, especially because tumhara project **flash sale + high concurrency + idempotency** par based hai.

Simple Hinglish mein samjho:

### Iska main kaam kya hai?

Maan lo user ne **Buy Now** button double-click kar diya:

```text
User
 ↓
BUY
 ↓
Request 1 → Order create
Request 2 → Same order create
```

Agar system ne check nahi kiya, toh same request se **2 orders** ban sakte hain.

`DuplicateRequestException` aise case ko identify karke bolta hai:

```text
"Ye request already process ho chuki hai."
```

Aur HTTP response deta hai:

```text
409 CONFLICT
```

---

### Example

Maan lo request mein:

```text
Idempotency-Key: ABC123
```

aaya.

Pehli request:

```text
ABC123
↓
Process
↓
Order created
```

Phir same key dobara aayi:

```text
ABC123
↓
Already processed
↓
DuplicateRequestException
↓
409 CONFLICT
```

Isse **duplicate order/payment/reservation** jaise problems prevent karne mein help milti hai.

---

### Tumhare flash-sale project mein kahan useful hai?

Particularly:

```text
User double-click
       ↓
Duplicate order request
```

```text
Payment retry
       ↓
Same payment request
```

```text
Inventory reservation retry
       ↓
Same reservation request
```

```text
Kafka event delivered twice
       ↓
Same event processed again
```

Ye sab **idempotency** se related hain.

---

### Code mein ye kya kar raha hai?

```java
public class DuplicateRequestException
        extends BaseCustomException
```

Matlab ye tumhari existing common exception class ko extend kar raha hai.

Phir:

```java
private static final int STATUS_CODE = 409;
```

ka matlab:

> Duplicate/conflicting request hone par HTTP `409 Conflict` use karo.

Aur:

```java
private static final String ERROR_CODE = "DUPLICATE_REQUEST";
```

ka matlab frontend ko consistent error code milega.

For example:

```json
{
  "errorCode": "DUPLICATE_REQUEST",
  "message": "Duplicate request detected"
}
```

---

### Do constructors kyun hain?

Simple wala:

```java
new DuplicateRequestException("Request already processed");
```

Aur detailed wala:

```java
new DuplicateRequestException(
    idempotencyKey,
    operation
);
```

Ye automatically message bana deta hai:

```text
Duplicate request detected for operation
'CREATE_ORDER' with idempotency key: 'ABC123'
```

Debugging ke liye useful hai.

---

### Important distinction

Ye class **duplicate request ko detect nahi karti**.

Ye sirf **exception represent karti hai**.

Actual detection kisi aur component mein hoga, jaise:

```text
Request
   ↓
Idempotency Check
   ↓
Already exists?
   ↓ YES
DuplicateRequestException
   ↓
Global Exception Handler
   ↓
HTTP 409
```

So:

**`DuplicateRequestException` = problem ko represent karna**

**Idempotency logic/filter/service = problem ko detect karna**

---

### Tumhare project ke liye verdict

**Haan, rakhni chahiye.** 👍

Flash-sale system mein duplicate requests bahut important concern hain, especially:

**Order + Payment + Inventory Reservation + Kafka consumers**.

Bas ek cheez yaad rakho: **exception class khud idempotency implement nahi karti; actual idempotency mechanism baad mein banana padega.**
*/



/*
*
* `idempotencyKey` basically ek **unique identifier** hota hai jo client kisi important request ke saath bhejta hai, taaki server **same request ko dobara process na kare**.

### Simple example

Tumhare flash-sale project mein:

```text
User clicks "Buy Now"
        ↓
Request
        ↓
Idempotency-Key: ABC123
        ↓
Order Service
```

Server `ABC123` ko store kar leta hai.

Agar user accidentally dobara click kare:

```text
Idempotency-Key: ABC123
        ↓
Server checks
        ↓
ABC123 already processed
        ↓
DuplicateRequestException
        ↓
409 Conflict
```

Isse **2 orders create nahi honge**.

### Real-life example

Suppose tumhara stock:

```text
iPhone = 1
```

User Buy karta hai:

```text
Request #1
Idempotency-Key = XYZ789
        ↓
Reservation successful
```

Network slow hone ki wajah se user dobara click karta hai:

```text
Request #2
Idempotency-Key = XYZ789
        ↓
Server: "XYZ789 already processed"
        ↓
Don't reserve again
```

Without idempotency:

```text
Click 1 → Reservation 1
Click 2 → Reservation 2 ❌
```

With idempotency:

```text
Click 1 → Reservation 1
Click 2 → Duplicate → Ignore/return existing result ✅
```

### Key ka format kya ho sakta hai?

Usually random UUID:

```text
550e8400-e29b-41d4-a716-446655440000
```

Request:

```http
POST /api/orders

Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

**Short definition:**

> **Idempotency key = ek unique request ID jo server ko batata hai ki ye request pehle process ho chuki hai ya nahi.**

Tumhare project mein ye particularly **Order, Payment aur Inventory Reservation** ke liye important hai.
*/