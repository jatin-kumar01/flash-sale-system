package com.flashsale.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO transmitted over REST to inventory-service to claim reservation locks on a SKU.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    private Long productId;
    private Integer quantity;
    private String orderReference;
}
