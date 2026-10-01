package com.flashsale.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeInventoryRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Initial stock is required")
    @Min(value = 1, message = "Initial stock must be at least 1")
    private Integer initialStock;
}
