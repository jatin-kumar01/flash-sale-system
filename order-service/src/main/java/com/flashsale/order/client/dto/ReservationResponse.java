package com.flashsale.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO capturing stock allocation status returned by inventory-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private boolean reserved;
    private Long productId;
    private Integer quantity;
    private String message;
}
