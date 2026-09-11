package com.flashsale.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent extends BaseEvent {

    private String orderReference;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;

    @Override
    public String getPartitionKey() {
        return productId != null ? String.valueOf(productId) : getAggregateId();
    }
}
