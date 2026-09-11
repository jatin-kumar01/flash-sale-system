package com.flashsale.order.exception;

/**
 * Explicit domain exception thrown when circuit breaker is OPEN or all retries are exhausted.
 */
public class InventoryUnavailableException extends RuntimeException {
    public InventoryUnavailableException(String message) {
        super(message);
    }

    public InventoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
