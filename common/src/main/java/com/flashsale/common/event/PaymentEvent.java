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
public class PaymentEvent extends BaseEvent {

    private String transactionId;
    private String orderReference;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;

    @Override
    public String getPartitionKey() {
        return orderReference != null ? orderReference : getAggregateId();
    }
}
