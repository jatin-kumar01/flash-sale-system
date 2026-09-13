package com.flashsale.inventory.controller;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.inventory.dto.InventoryReservationRequest;
import com.flashsale.inventory.dto.InventoryResponse;
import com.flashsale.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable("productId") Long productId) {

        log.debug("Fetching inventory status for productId: {}", productId);

        InventoryResponse response =
                inventoryService.getInventory(productId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Inventory retrieved successfully",
                        response
                )
        );
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<InventoryResponse>> reserveStock(
            @Valid @RequestBody InventoryReservationRequest request) {

        log.info(
                "Received stock reservation request for productId: {}, orderRef: {}",
                request.getProductId(),
                request.getOrderReference()
        );

        InventoryResponse response =
                inventoryService.reserveStock(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Stock reserved successfully",
                        response
                )
        );
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @Valid @RequestBody InventoryReservationRequest request) {

        log.info(
                "Received stock release request for productId: {}, orderRef: {}",
                request.getProductId(),
                request.getOrderReference()
        );

        inventoryService.releaseStock(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Stock released successfully",
                        null
                )
        );
    }

    @PostMapping("/settle")
    public ResponseEntity<ApiResponse<Void>> settleOrderDeduction(
            @Valid @RequestBody InventoryReservationRequest request) {

        log.info(
                "Received stock settlement request for productId: {}, orderRef: {}",
                request.getProductId(),
                request.getOrderReference()
        );

        inventoryService.settleOrderDeduction(
                request.getProductId(),
                request.getQuantity(),
                request.getOrderReference()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Stock settled successfully",
                        null
                )
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/replenish")
    public ResponseEntity<ApiResponse<InventoryResponse>> replenishStock(
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") int quantity) {

        log.info(
                "Received stock replenishment request for productId: {}, quantity: {}",
                productId,
                quantity
        );

        InventoryResponse response =
                inventoryService.replenishStock(productId, quantity);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Stock replenished successfully",
                                response
                        )
                );
    }
}