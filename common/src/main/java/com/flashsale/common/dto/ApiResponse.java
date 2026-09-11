package com.flashsale.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Standardized generic response envelope for all REST APIs across all microservices.
 *
 * @param <T> Payload data type
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Object errorDetails;

    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Creates a successful API response with data payload and default message.
     *
     * @param data Payload
     * @param <T> Data type
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a successful API response with custom message and data payload.
     *
     * @param message Custom success message
     * @param data Payload
     * @param <T> Data type
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates an error API response with error message.
     *
     * @param message Error message
     * @param <T> Data type
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates an error API response with error message and detailed error breakdown.
     *
     * @param message Error message
     * @param errorDetails Additional error details
     * @param <T> Data type
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message, Object errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorDetails(errorDetails)
                .timestamp(Instant.now())
                .build();
    }
}


// 1. SUCCESS + DATA
// Example: Product found.
// "Everything is OK, here is the data."
// Usage: ApiResponse.success(product)


// 2. SUCCESS + CUSTOM MESSAGE + DATA
// Example: Order created successfully.
// "Everything is OK, here is the data + my own message."
// Usage: ApiResponse.success("Order created successfully", order)


// 3. ERROR + MESSAGE
// Example: Product not found.
// "Something went wrong, only tell the user why."
// Usage: ApiResponse.error("Product not found")


// 4. ERROR + MESSAGE + DETAILS
// Example: Quantity is invalid.
// "Something went wrong, tell the user why + give extra details."
// Usage: ApiResponse.error("Validation failed", errors)
