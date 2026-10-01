package com.flashsale.product.client;

import com.flashsale.common.exception.InvalidRequestException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service/api/inventory/initialize";

    public void initializeInventory(Long productId, Integer initialStock) {
        log.info(
                "Calling Inventory Service to initialize stock for productId: {}, initialStock: {}",
                productId,
                initialStock
        );

        InitializeInventoryPayload payload = InitializeInventoryPayload.builder()
                .productId(productId)
                .initialStock(initialStock)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<InitializeInventoryPayload> requestEntity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    INVENTORY_SERVICE_URL,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully initialized inventory for productId: {}", productId);
            } else {
                log.error(
                        "Failed to initialize inventory for productId: {}. Status code: {}",
                        productId,
                        response.getStatusCode()
                );
                throw new InvalidRequestException(
                        "Failed to initialize inventory for product ID " + productId + " with status: " + response.getStatusCode()
                );
            }
        } catch (Exception ex) {
            log.error("Error communicating with Inventory Service for productId: {}", productId, ex);
            throw new InvalidRequestException(
                    "Failed to initialize inventory for product ID " + productId + ": " + ex.getMessage()
            );
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeInventoryPayload {
        private Long productId;
        private Integer initialStock;
    }
}
