package com.flashsale.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent extends BaseEvent {

    private Long productId;
    private Integer quantity;
    private String orderReference;

    @Override
    public String getPartitionKey() {
        return productId != null ? String.valueOf(productId) : getAggregateId();
    }
}
