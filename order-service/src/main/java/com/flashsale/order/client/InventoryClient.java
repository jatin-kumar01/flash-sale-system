package com.flashsale.order.client;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.order.client.dto.ReservationRequest;
import com.flashsale.order.client.dto.ReservationResponse;
import com.flashsale.order.dto.InventoryReservationRequest;
import com.flashsale.order.exception.InventoryUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client utilizing RestClient alongside Resilience4j @CircuitBreaker and @Retry annotations
 * to reserve and release inventory with automated fault tolerance.
 */
@Slf4j
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.services.inventory-url:http://inventory-service:8083}") String inventoryBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(inventoryBaseUrl)
                .build();
    }

    /**
     * Attempts to reserve inventory synchronously with Circuit Breaker and Retry protections.
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventoryService")
    public ReservationResponse reserveStock(ReservationRequest request) {
        log.info("Calling inventory-service to reserve stock: orderRef={}, productId={}, qty={}",
                request.getOrderReference(), request.getProductId(), request.getQuantity());

        ApiResponse<ReservationResponse> response = restClient.post()
                .uri("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<ReservationResponse>>() {});

        if (response == null || response.getData() == null) {
            throw new InventoryUnavailableException("Empty response received from inventory-service");
        }

        return response.getData();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventoryService")
    public ReservationResponse reserveStock(InventoryReservationRequest request) {
        ReservationRequest dto = ReservationRequest.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .orderReference(request.getOrderReference())
                .build();
        return reserveStock(dto);
    }

    /**
     * Fallback triggered when the circuit breaker is OPEN or retries are exhausted.
     */
    public ReservationResponse reserveStockFallback(ReservationRequest request, CallNotPermittedException ex) {
        log.error("Circuit breaker is OPEN for inventory-service. Call rejected immediately: {}", ex.getMessage());
        throw new InventoryUnavailableException("Inventory service is temporarily unavailable. Please try again shortly.");
    }

    public ReservationResponse reserveStockFallback(InventoryReservationRequest request, CallNotPermittedException ex) {
        log.error("Circuit breaker is OPEN for inventory-service. Call rejected immediately: {}", ex.getMessage());
        throw new InventoryUnavailableException("Inventory service is temporarily unavailable. Please try again shortly.");
    }

    /**
     * Generic fallback for timeout, I/O errors, or downstream HTTP 5xx responses.
     */
    public ReservationResponse reserveStockFallback(ReservationRequest request, Throwable ex) {
        log.error("Fallback invoked for inventory reservation [orderRef={}]: {}", request.getOrderReference(), ex.getMessage());
        throw new InventoryUnavailableException("Unable to reserve inventory at this moment: " + ex.getMessage(), ex);
    }

    public ReservationResponse reserveStockFallback(InventoryReservationRequest request, Throwable ex) {
        log.error("Fallback invoked for inventory reservation [orderRef={}]: {}", request.getOrderReference(), ex.getMessage());
        throw new InventoryUnavailableException("Unable to reserve inventory at this moment: " + ex.getMessage(), ex);
    }

    public void releaseStock(InventoryReservationRequest request) {
        log.info("Calling inventory-service to release stock: orderRef={}", request.getOrderReference());
        try {
            restClient.post()
                    .uri("/api/inventory/release")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("Failed to release stock for orderRef={}: {}", request.getOrderReference(), ex.getMessage());
            throw new InventoryUnavailableException("Failed to release stock: " + ex.getMessage(), ex);
        }
    }

    public void settleStock(InventoryReservationRequest request) {
        log.info("Calling inventory-service to settle stock: orderRef={}", request.getOrderReference());
        try {
            restClient.post()
                    .uri("/api/inventory/settle")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("Failed to settle stock for orderRef={}: {}", request.getOrderReference(), ex.getMessage());
            throw new InventoryUnavailableException("Failed to settle stock: " + ex.getMessage(), ex);
        }
    }
}