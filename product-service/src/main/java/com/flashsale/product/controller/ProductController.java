//package com.flashsale.product.controller;
//
//import com.flashsale.common.dto.ApiResponse;
//import com.flashsale.product.dto.CreateProductRequest;
//import com.flashsale.product.dto.ProductResponse;
//import com.flashsale.product.dto.UpdateProductRequest;
//import com.flashsale.product.entity.Product.ProductStatus;
//import com.flashsale.product.service.ProductService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/products")
//@RequiredArgsConstructor
//public class ProductController {
//
//    private final ProductService productService;
//
//    @GetMapping("/flash-sales")
//    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getActiveFlashSales(
//            @PageableDefault(
//                    size = 20,
//                    sort = "startTime",
//                    direction = Sort.Direction.ASC
//            ) Pageable pageable) {
//
//        log.debug("Fetching active flash sales list");
//
//        Page<ProductResponse> activeSales =
//                productService.getActiveFlashSales(pageable);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        "Active flash sales retrieved successfully",
//                        activeSales
//                )
//        );
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
//            @PathVariable("id") Long productId) {
//
//        log.debug("Fetching product details for ID: {}", productId);
//
//        ProductResponse response =
//                productService.getProductById(productId);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        "Product retrieved successfully",
//                        response
//                )
//        );
//    }
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
//            @RequestParam(value = "status", required = false)
//            ProductStatus status,
//            @PageableDefault(
//                    size = 20,
//                    sort = "createdAt",
//                    direction = Sort.Direction.DESC
//            ) Pageable pageable) {
//
//        log.debug("Fetching all products with status filter: {}", status);
//
//        Page<ProductResponse> products =
//                productService.getAllProducts(status, pageable);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        "Products retrieved successfully",
//                        products
//                )
//        );
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping
//    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
//            @Valid @RequestBody CreateProductRequest request) {
//
//        log.info("Creating new product: {}", request.getTitle());
//
//        ProductResponse response =
//                productService.createProduct(request);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(
//                        ApiResponse.success(
//                                "Product created successfully",
//                                response
//                        )
//                );
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
//            @PathVariable("id") Long productId,
//            @Valid @RequestBody UpdateProductRequest request) {
//
//        log.info("Updating product ID: {}", productId);
//
//        ProductResponse response =
//                productService.updateProduct(productId, request);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        "Product updated successfully",
//                        response
//                )
//        );
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @PutMapping("/{id}/status")
//    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
//            @PathVariable("id") Long productId,
//            @RequestParam ProductStatus status) {
//
//        log.info(
//                "Updating product ID: {} status to {}",
//                productId,
//                status
//        );
//
//        UpdateProductRequest request = new UpdateProductRequest();
//        request.setStatus(status);
//
//        ProductResponse response =
//                productService.updateProduct(productId, request);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        "Product status updated successfully",
//                        response
//                )
//        );
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> deleteProduct(
//            @PathVariable("id") Long productId) {
//
//        log.info("Deleting product ID: {}", productId);
//
//        productService.deleteProduct(productId);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        "Product deleted successfully",
//                        null
//                )
//        );
//    }
//}



package com.flashsale.product.controller;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.product.dto.CreateProductRequest;
import com.flashsale.product.dto.ProductResponse;
import com.flashsale.product.dto.UpdateProductRequest;
import com.flashsale.product.entity.Product.ProductStatus;
import com.flashsale.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // --------------------------------------------------
    // GET ACTIVE FLASH SALES
    // --------------------------------------------------

    @GetMapping("/flash-sales")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getActiveFlashSales(
            @PageableDefault(
                    size = 20,
                    sort = "startTime",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        log.debug("Fetching active flash sales list");

        Page<ProductResponse> activeSales =
                productService.getActiveFlashSales(pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Active flash sales retrieved successfully",
                        activeSales
                )
        );
    }

    // --------------------------------------------------
    // GET PRODUCT BY ID
    // --------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable("id") Long productId) {

        log.debug("Fetching product details for ID: {}", productId);

        ProductResponse response =
                productService.getProductById(productId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product retrieved successfully",
                        response
                )
        );
    }

    // --------------------------------------------------
    // GET ALL PRODUCTS
    // --------------------------------------------------

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(value = "status", required = false)
            ProductStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        log.debug("Fetching all products with status filter: {}", status);

        Page<ProductResponse> products =
                productService.getAllProducts(status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Products retrieved successfully",
                        products
                )
        );
    }

    // --------------------------------------------------
    // CREATE PRODUCT
    // --------------------------------------------------

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        log.info("Creating new product: {}", request.getTitle());

        ProductResponse response =
                productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Product created successfully",
                                response
                        )
                );
    }

    // --------------------------------------------------
    // UPDATE PRODUCT
    // --------------------------------------------------

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable("id") Long productId,
            @Valid @RequestBody UpdateProductRequest request) {

        log.info("Updating product ID: {}", productId);

        ProductResponse response =
                productService.updateProduct(productId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product updated successfully",
                        response
                )
        );
    }

    // --------------------------------------------------
    // UPDATE PRODUCT STATUS
    // --------------------------------------------------

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @PathVariable("id") Long productId,
            @RequestParam(value = "status", required = false) ProductStatus status,
            @RequestBody(required = false) UpdateProductRequest requestBody) {

        ProductStatus targetStatus = status;
        if (targetStatus == null && requestBody != null) {
            targetStatus = requestBody.getStatus();
        }

        if (targetStatus == null) {
            throw new com.flashsale.common.exception.InvalidRequestException("Product status is required");
        }

        log.info(
                "Updating product ID: {} status to {}",
                productId,
                targetStatus
        );

        UpdateProductRequest request = new UpdateProductRequest();
        request.setStatus(targetStatus);

        ProductResponse response =
                productService.updateProduct(productId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product status updated successfully",
                        response
                )
        );
    }

    // --------------------------------------------------
    // DELETE PRODUCT
    // --------------------------------------------------

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable("id") Long productId) {

        log.info("Deleting product ID: {}", productId);

        productService.deleteProduct(productId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product deleted successfully",
                        null
                )
        );
    }

    // --------------------------------------------------
    // UPLOAD PRODUCT IMAGE
    // --------------------------------------------------

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping(value = "/upload-image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> uploadProductImage(
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile file) {

        log.info(
                "Received image upload request: file={}",
                file != null ? (file.getOriginalFilename() + " (" + file.getSize() + " bytes)") : "null"
        );

        if (file == null || file.isEmpty()) {
            throw new com.flashsale.common.exception.InvalidRequestException(
                    "Please select an image file to upload."
            );
        }

        // Validate max size (20 MB = 20 * 1024 * 1024 bytes)
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new com.flashsale.common.exception.InvalidRequestException(
                    "Image size must not exceed 20 MB."
            );
        }

        // Validate content type
        String contentType = file.getContentType();
        log.info("Uploaded file content-type: {}", contentType);

        if (contentType == null
                || (!contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/jpg")
                && !contentType.equalsIgnoreCase("image/png")
                && !contentType.equalsIgnoreCase("image/webp"))) {

            throw new com.flashsale.common.exception.InvalidRequestException(
                    "Only JPG, PNG, and WEBP image files are allowed."
            );
        }

        // Determine file extension
        String originalFilename = file.getOriginalFilename();
        String extension = ".png";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename
                    .substring(originalFilename.lastIndexOf("."))
                    .toLowerCase();
        } else if (contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/jpg")) {
            extension = ".jpg";
        } else if (contentType.equalsIgnoreCase("image/webp")) {
            extension = ".webp";
        }

        // Generate unique filename using UUID
        String newFilename = "product-" + java.util.UUID.randomUUID().toString() + extension;

        try {
            java.nio.file.Path uploadDir = java.nio.file.Paths
                    .get("uploads", "products")
                    .toAbsolutePath()
                    .normalize();

            if (!java.nio.file.Files.exists(uploadDir)) {
                java.nio.file.Files.createDirectories(uploadDir);
            }

            java.nio.file.Path targetPath = uploadDir.resolve(newFilename).normalize();

            if (!targetPath.startsWith(uploadDir)) {
                throw new com.flashsale.common.exception.InvalidRequestException("Invalid file path security violation.");
            }

            java.nio.file.Files.copy(
                    file.getInputStream(),
                    targetPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            String imageUrl = "/uploads/products/" + newFilename;
            log.info("Successfully stored product image at: {}", targetPath);

            java.util.Map<String, String> responseData =
                    java.util.Collections.singletonMap("imageUrl", imageUrl);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Image uploaded successfully",
                            responseData
                    )
            );
        } catch (java.io.IOException e) {
            log.error("Failed to store image file", e);
            throw new RuntimeException("Failed to store image file: " + e.getMessage());
        }
    }
}