package com.flashsale.product.exception;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.common.dto.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductServiceExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied in product-service: {}", ex.getMessage());

        ErrorDetail errorDetail = ErrorDetail.of("ACCESS_DENIED", "You do not have permission to access this product endpoint.");
        ApiResponse<Void> response = ApiResponse.error("Access denied", List.of(errorDetail));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(Exception ex) {
        log.warn("File upload size limit exceeded or multipart error in product-service: {}", ex.getMessage());

        ErrorDetail errorDetail = ErrorDetail.of("FILE_TOO_LARGE", "Image size must not exceed 20 MB.");
        ApiResponse<Void> response = ApiResponse.error("Image size must not exceed 20 MB.", List.of(errorDetail));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state exception in product-service: {}", ex.getMessage());

        if (ex.getMessage() != null && (ex.getMessage().contains("FileSizeLimitExceededException") || ex.getMessage().contains("SizeLimitExceededException"))) {
            ErrorDetail errorDetail = ErrorDetail.of("FILE_TOO_LARGE", "Image size must not exceed 20 MB.");
            ApiResponse<Void> response = ApiResponse.error("Image size must not exceed 20 MB.", List.of(errorDetail));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        ErrorDetail errorDetail = ErrorDetail.of("INVALID_STATE", ex.getMessage() != null ? ex.getMessage() : "Invalid state");
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage() != null ? ex.getMessage() : "Invalid state", List.of(errorDetail));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
