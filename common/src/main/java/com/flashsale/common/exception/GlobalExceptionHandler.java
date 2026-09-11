package com.flashsale.common.exception;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.common.dto.ErrorDetail;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseCustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseCustomException(BaseCustomException ex) {
        log.warn("Business domain exception occurred: errorCode='{}', statusCode={}, message='{}'",
                ex.getErrorCode(), ex.getStatusCode(), ex.getMessage());

        ErrorDetail errorDetail = ErrorDetail.of(ex.getErrorCode(), ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), List.of(errorDetail));

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation error on API boundary: {}", ex.getMessage());

        List<ErrorDetail> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(ErrorDetail.of(
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage()
            ));
        }

        ApiResponse<Void> response = ApiResponse.error("Validation failed for one or more fields", validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation error: {}", ex.getMessage());

        List<ErrorDetail> validationErrors = ex.getConstraintViolations().stream()
                .map(violation -> ErrorDetail.of(
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue(),
                        violation.getMessage()
                ))
                .toList();

        ApiResponse<Void> response = ApiResponse.error("Constraint validation failed", validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument exception: {}", ex.getMessage());

        ErrorDetail errorDetail = ErrorDetail.of("INVALID_ARGUMENT", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), List.of(errorDetail));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(Exception ex) {
        log.error("Unhandled internal system error:", ex);

        ErrorDetail errorDetail = ErrorDetail.of("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.");
        ApiResponse<Void> response = ApiResponse.error("Internal Server Error", List.of(errorDetail));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}


/*
* Haan. **`GlobalExceptionHandler.java` ki need hai because ye poore microservice ke errors ko ek common/standard format mein handle karta hai.**

Simple Hinglish mein:

### Ye file kya karti hai?

Normally agar kisi controller/service mein error aa gaya:

```text
Controller
   ↓
Exception
   ↓
Spring ka default error response
```

Different errors ke responses different ho sakte hain.

Hum chahte hain:

```text
Koi bhi error
    ↓
GlobalExceptionHandler
    ↓
Standard ApiResponse
    ↓
Frontend
```

### Example 1 — Product nahi mila

Agar user:

```text
GET /products/999
```

kare aur product exist nahi karta:

```text
ResourceNotFoundException
        ↓
GlobalExceptionHandler
        ↓
404 NOT_FOUND
```

Frontend ko structured response milega.

---

### Example 2 — Stock khatam

Tumhare flash-sale project mein ye **bahut important** hai.

Suppose:

```text
Available stock = 0
User → Buy
```

Inventory service:

```text
InsufficientStockException
        ↓
GlobalExceptionHandler
        ↓
409 CONFLICT
```

Frontend ko pata chalega:

```text
"Insufficient stock"
```

---

### Example 3 — Validation error

User registration mein:

```text
email = "abc"
password = ""
```

`@Valid` validation fail karegi.

Ye handler:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
```

validation errors collect karke proper response banata hai.

Instead of random Spring error, frontend ko structured errors milenge.

---

### Example 4 — Unexpected error

Suppose database mein unexpected problem aa gayi:

```text
Database Error
     ↓
GlobalExceptionHandler
     ↓
500 INTERNAL_SERVER_ERROR
```

Client ko **database details/password/SQL stack trace** nahi milega.

Client ko:

```text
Internal Server Error
An unexpected error occurred. Please try again later.
```

milega.

Lekin backend logs mein developer ke liye complete exception rahegi:

```java
log.error("Unhandled internal system error:", ex);
```

---

## Tumhare project mein iska importance

Tumhare architecture mein multiple services hain:

```text
Auth Service
Product Service
Inventory Service
Order Service
Payment Service
Notification Service
Analytics Service
Invoice Service
```

Har service mein alag-alag exception handling likhne ke bajay `common` module mein common handler rakh sakte ho.

```text
                 common
                   │
        GlobalExceptionHandler
                   │
       ┌───────────┼───────────┐
       ↓           ↓           ↓
 Inventory       Order       Payment
 Service        Service      Service
```

Isliye ye **reusable common component** hai.

### Ek important clarification

Tumhare description mein likha hai:

> "`@RestControllerAdvice`: Scans and intercepts exceptions ... across any microservice"

Technically, ye **automatically all microservices ko affect nahi karega sirf file common module mein hone se**.

Har microservice ko `common` dependency mein ye class available honi chahiye aur Spring component scanning/import configuration se discover hona chahiye.

So concept simple hai:

**`GlobalExceptionHandler` = centralized place jahan API errors ko standard response mein convert karte hain.**

Tumhare flash-sale project mein especially **validation, insufficient stock, reservation expired, duplicate request, aur unexpected errors** ke liye useful hai.

* */
