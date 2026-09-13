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

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @PathVariable("id") Long productId,
            @RequestParam ProductStatus status) {

        log.info(
                "Updating product ID: {} status to {}",
                productId,
                status
        );

        UpdateProductRequest request = new UpdateProductRequest();
        request.setStatus(status);

        ProductResponse response =
                productService.updateProduct(productId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product status updated successfully",
                        response
                )
        );
    }

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
}