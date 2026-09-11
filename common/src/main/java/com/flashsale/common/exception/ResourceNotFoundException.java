package com.flashsale.common.exception;

public class ResourceNotFoundException extends BaseCustomException {

    private static final int STATUS_CODE = 404;
    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(message, STATUS_CODE, ERROR_CODE);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue), STATUS_CODE, ERROR_CODE);
    }
}




/*
 * ResourceNotFoundException is a custom exception used when a requested
 * resource does not exist in the system.
 *
 * Examples:
 * - Product with ID 101 does not exist
 * - Order with ID 5001 does not exist
 * - User with ID 25 does not exist
 * - Inventory for a particular product does not exist
 *
 * This exception will eventually be handled by the global exception handler
 * and converted into an HTTP 404 NOT_FOUND response.
 */
//public class ResourceNotFoundException extends BaseCustomException {

    /*
     * HTTP status code used when a requested resource cannot be found.
     *
     * 404 means:
     * "The server could not find the requested resource."
     */
  //  private static final int STATUS_CODE = 404;

    /*
     * A machine-readable error code.
     *
     * Instead of checking the error message, the frontend or another service
     * can use this stable error code to understand the type of error.
     *
     * Example:
     * RESOURCE_NOT_FOUND
     */
    //private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    /*
     * Constructor 1
     *
     * This constructor accepts a custom error message.
     *
     * Example:
     *
     * throw new ResourceNotFoundException("Product does not exist");
     *
     * The message, status code, and error code are passed to
     * BaseCustomException.
     */
//    public ResourceNotFoundException(String message) {
//        super(message, STATUS_CODE, ERROR_CODE);
//    }

    /*
     * Constructor 2
     *
     * This constructor is useful when we want to automatically create
     * a standard "resource not found" message.
     *
     * Parameters:
     *
     * resourceName -> Name of the resource, such as "Product"
     * fieldName    -> Field used for searching, such as "id"
     * fieldValue   -> Value that was searched for, such as 101
     *
     * Example:
     *
     * new ResourceNotFoundException("Product", "id", 101)
     *
     * will create:
     *
     * "Product not found with id: '101'"
     */
//    public ResourceNotFoundException(
//            String resourceName,
//            String fieldName,
//            Object fieldValue) {
//
//        /*
//         * String.format() creates the final error message.
//         *
//         * %s is replaced by the corresponding argument.
//         *
//         * Example:
//         *
//         * resourceName = "Product"
//         * fieldName    = "id"
//         * fieldValue   = 101
//         *
//         * Result:
//         *
//         * "Product not found with id: '101'"
//         *
//         * Then the message, 404 status code, and error code
//         * are passed to the parent BaseCustomException.
//         */
//        super(
//                String.format(
//                        "%s not found with %s: '%s'",
//                        resourceName,
//                        fieldName,
//                        fieldValue
//                ),
//                STATUS_CODE,
//                ERROR_CODE
//        );
//    }
//}
/*
* Haan, **`ResourceNotFoundException.java` ki need hai**, especially tumhare microservices project mein. Simple Hinglish mein:

### Iska kaam kya hai?

Maan lo user ye request karta hai:

```text
GET /api/products/101
```

Lekin database mein **Product ID 101 exist hi nahi karti**.

Agar hum exception handling properly na karein, to application generic error de sakti hai:

```text
500 Internal Server Error
```

Lekin actual problem hai:

```text
Product nahi mila
```

Isliye correct response hona chahiye:

```text
404 NOT FOUND
```

---

### Ye class exactly kya karti hai?

Tumhari class:

```java
public class ResourceNotFoundException extends BaseCustomException
```

ka matlab hai ki ye ek **custom exception** hai jo specifically "resource nahi mila" situation ke liye banayi gayi hai.

Example:

```java
throw new ResourceNotFoundException(
    "Product",
    "id",
    101
);
```

Ye automatically message bana degi:

```text
Product not found with id: '101'
```

aur saath mein:

```text
Status Code = 404
Error Code  = RESOURCE_NOT_FOUND
```

---

### Isse kya benefit hai?

Har service mein baar-baar ye likhne ki zarurat nahi:

```java
404
RESOURCE_NOT_FOUND
```

Instead:

```java
throw new ResourceNotFoundException("Product", "id", 101);
```

Same class ko different microservices use kar sakte hain:

```text
Product Service
      ↓
ResourceNotFoundException

Order Service
      ↓
ResourceNotFoundException

Inventory Service
      ↓
ResourceNotFoundException

Auth/User Service
      ↓
ResourceNotFoundException
```

For example:

```java
throw new ResourceNotFoundException("Order", "id", orderId);
```

Response conceptually:

```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Order not found with id: '5001'"
}
```

---

### `BaseCustomException` kyun use ho rahi hai?

Tumhare project mein different errors ho sakte hain:

```text
ResourceNotFoundException
ValidationException
UnauthorizedException
OutOfStockException
PaymentException
```

Agar sab `BaseCustomException` ko extend karein:

```text
             BaseCustomException
                    │
       ┌────────────┼─────────────┐
       ↓            ↓             ↓
ResourceNotFound  Unauthorized  OutOfStock
```

to **global exception handler** ek common way se in errors ko handle kar sakta hai.

---

### Tumhare project mein important kyun hai?

Tumhare system mein multiple microservices hain:

```text
Product
Inventory
Order
Payment
Auth
Notification
...
```

Aise distributed project mein **consistent error responses** important hote hain.

Instead of:

```text
Product → different error format
Order → different error format
Inventory → different error format
```

sab same pattern follow kar sakte hain:

```text
HTTP 404
RESOURCE_NOT_FOUND
message
timestamp
```

### Short mein

**`ResourceNotFoundException` = jab requested resource database/system mein nahi milti, tab proper `404 NOT_FOUND` error dene ke liye reusable custom exception.**

Aur haan, **ye file abhi useful hai**, kyunki baad mein Product, Order, Inventory etc. services mein repeatedly use hogi.

*
* Sure. `ResourceNotFoundException` mein **2 constructors** hain. Constructor ko simple language mein samjho: **object banate time alag-alag information dene ke 2 tareeke**.

---

## Method/Constructor 1

```java
public ResourceNotFoundException(String message) {
    super(message, STATUS_CODE, ERROR_CODE);
}
```

### Isme kya ho raha hai?

Is constructor mein tum **directly complete message** dete ho.

Example:

```java
throw new ResourceNotFoundException("Product does not exist");
```

Yahan:

```text
message = "Product does not exist"
```

Constructor ke andar:

```java
super(message, STATUS_CODE, ERROR_CODE);
```

actually parent `BaseCustomException` ko ye information bhej raha hai:

```text
message     → Product does not exist
status code → 404
error code  → RESOURCE_NOT_FOUND
```

So conceptually:

```text
ResourceNotFoundException
        ↓
"Product does not exist"
        ↓
404
        ↓
RESOURCE_NOT_FOUND
```

### Kab use karenge?

Jab tumhe **custom message khud likhna ho**.

For example:

```java
throw new ResourceNotFoundException(
    "The requested product is not available"
);
```

---

# Constructor 2

```java
public ResourceNotFoundException(
        String resourceName,
        String fieldName,
        Object fieldValue) {

    super(
        String.format(
            "%s not found with %s: '%s'",
            resourceName,
            fieldName,
            fieldValue
        ),
        STATUS_CODE,
        ERROR_CODE
    );
}
```

Ye thoda different hai.

Isme tum complete message nahi dete.

Tum **3 pieces of information** dete ho:

```text
resourceName
fieldName
fieldValue
```

### Example

```java
throw new ResourceNotFoundException(
    "Product",
    "id",
    101
);
```

Yahan:

```text
resourceName = "Product"
fieldName    = "id"
fieldValue   = 101
```

Ab `String.format()` inko combine karega:

```java
"%s not found with %s: '%s'"
```

Replace hone ke baad:

```text
Product not found with id: '101'
```

Then parent ko:

```text
message     → Product not found with id: '101'
status code → 404
error code  → RESOURCE_NOT_FOUND
```

milega.

---

# Simple real-life example

Suppose database mein:

```text
Products

ID    Name
101   Laptop
102   Phone
103   Mouse
```

Tum request karte ho:

```text
GET /products/999
```

Database mein `999` nahi hai.

Service mein:

```java
throw new ResourceNotFoundException(
    "Product",
    "id",
    999
);
```

Constructor automatically message bana dega:

```text
Product not found with id: '999'
```

---

# Dono ka difference

### Constructor 1

```java
new ResourceNotFoundException("Product does not exist");
```

Tum **poora message khud banate ho**.

### Constructor 2

```java
new ResourceNotFoundException("Product", "id", 999);
```

Tum **resource + field + value** dete ho, aur class automatically standard message bana deti hai.

---

## Ek aur example: Order

### Constructor 1

```java
throw new ResourceNotFoundException(
    "Order was not found"
);
```

Output:

```text
Order was not found
```

### Constructor 2

```java
throw new ResourceNotFoundException(
    "Order",
    "id",
    5001
);
```

Output:

```text
Order not found with id: '5001'
```

---

## Why 2 constructors?

Basically flexibility ke liye:

```text
Need custom message?
       ↓
Constructor 1
       ↓
String message


Need standard message?
       ↓
Constructor 2
       ↓
Resource + Field + Value
```

### Very simple analogy

Jaise tum kisi person ko address dena chahte ho.

**Method 1:**

> "Jatin ka ghar nahi mila."

Tum **complete sentence khud** bana rahe ho.

**Method 2:**

> Person = Jatin
> Field = house number
> Value = 101

System automatically bana de:

> "Jatin not found with house number: '101'"

Bas **yahi difference hai dono constructors mein**.

*
*
* */