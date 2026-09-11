package com.flashsale.common.exception;

import lombok.Getter;

@Getter
public abstract class BaseCustomException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    protected BaseCustomException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    protected BaseCustomException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
}

// This is a common parent class for all our custom exceptions.
//
// Example:
// InsufficientStockException
// ReservationExpiredException
// ResourceNotFoundException
// DuplicateRequestException
//
// Instead of writing statusCode and errorCode separately
// in every exception, we keep them here.
//
// Example:
// errorCode  = "INSUFFICIENT_STOCK"
// statusCode = 409
//
// Then a specific exception can extend this class:
//
// class InsufficientStockException extends BaseCustomException {
//     ...
// }
// Constructor 1:
// Used when we only need the error message, status code, and error code.
//
// Example:
// message    = "Product is out of stock"
// statusCode = 409
// errorCode  = "INSUFFICIENT_STOCK"
//
// super(message) sends the message to RuntimeException.
// So getMessage() will return "Product is out of stock".
//
// this.statusCode = statusCode;
// this.errorCode = errorCode;
// These lines store the given values in this exception object.
//
//protected BaseCustomException(String message, int statusCode, String errorCode) {
//    super(message);
//    this.statusCode = statusCode;
//    this.errorCode = errorCode;
//}
//

// Constructor 2:
// Used when we also have the original/root error (cause).
//
// "cause" means the actual error that caused this exception.
//
// Example:
// Database error
//      ↓
// InsufficientStockException
//
// We can keep the original database error as "cause"
// so developers can find the real problem while debugging.
//
// super(message, cause) stores BOTH:
// 1. Our user-friendly error message
// 2. The original error/cause
//
// Example:
// message = "Unable to reserve product"
// cause   = DatabaseException
//
// This is useful for debugging and logging.
//
// The statusCode and errorCode are stored in the same way
// as the first constructor.
//
//protected BaseCustomException(
//        String message,
//        int statusCode,
//        String errorCode,
//        Throwable cause) {
//
//    super(message, cause);
//    this.statusCode = statusCode;
//    this.errorCode = errorCode;
//}