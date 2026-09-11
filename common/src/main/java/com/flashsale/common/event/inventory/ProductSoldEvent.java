package com.flashsale.common.event.inventory;

import com.flashsale.common.event.BaseEvent;
import com.flashsale.common.event.EventTopics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSoldEvent extends BaseEvent {

    private String reservationId;
    private String orderId;
    private Long productId;
    private Long userId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Instant soldAt;

    @Override
    public String getPartitionKey() {
        return productId != null ? String.valueOf(productId) : getAggregateId();
    }

    public static ProductSoldEvent of(String reservationId, String orderId, Long productId,
                                      Long userId, Integer quantity, BigDecimal totalPrice,
                                      Instant soldAt) {
        return ProductSoldEvent.builder()
                .eventType(EventTopics.PRODUCT_SOLD)
                .aggregateId(reservationId)
                .reservationId(reservationId)
                .orderId(orderId)
                .productId(productId)
                .userId(userId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .soldAt(soldAt)
                .build();
    }
}

/**Yes, **`ProductSoldEvent.java` is useful in your architecture**, but there is one important design point to understand.

 ### Simple Hinglish

 `ProductSoldEvent` ka purpose hai:

 > **"Reserved product ka payment successfully ho gaya, aur ab woh stock permanently SOLD ho gaya."**

 Flow:

 ```text
 Product
 ↓
 AVAILABLE
 ↓
 RESERVED
 ↓
 Payment Success
 ↓
 ProductSoldEvent
 ↓
 SOLD
 ```

 ### Example

 Suppose:

 ```text
 Product ID = 101
 Stock = 10
 ```

 User reserves 2:

 ```text
 AVAILABLE = 8
 RESERVED = 2
 ```

 Payment successful:

 ```text
 PaymentSuccessEvent
 ↓
 Inventory Service
 ↓
 ProductSoldEvent
 ↓
 2 units permanently sold
 ```

 Now:

 ```text
 AVAILABLE = 8
 RESERVED = 0
 SOLD = 2
 ```

 ---

 ### Is event mein kya information hai?

 ```text
 reservationId
 orderId
 productId
 userId
 quantity
 totalPrice
 soldAt
 ```

 Example:

 ```text
 reservationId = RES-1001
 orderId       = ORD-5001
 productId     = 101
 userId        = 25
 quantity      = 2
 totalPrice    = ₹140,000
 soldAt        = current time
 ```

 Is information se analytics/invoice/audit systems ko sale ka complete snapshot mil sakta hai.

 ---

 ### `getPartitionKey()` kyu `productId` hai?

 ```java
 return productId != null
 ? String.valueOf(productId)
 : getAggregateId();
 ```

 Same product ke inventory events ko same Kafka key se route karne mein help karta hai:

 ```text
 Product 101

 PRODUCT_RESERVED
 ↓
 RESERVATION_EXPIRED / RELEASED
 ↓
 PRODUCT_SOLD
 ```

 Isse same SKU ke state transitions ko ordered processing ke liye organize kiya ja sakta hai.

 ---

 ### Analytics Service

 Ye event analytics ke liye particularly useful hai:

 ```text
 ProductSoldEvent
 ↓
 Analytics Service
 ↓
 Revenue
 Units Sold
 Sell-through Rate
 Top Products
 ```

 Example:

 ```text
 Product 101
 Units Sold = 500
 Revenue = ₹35,00,000
 ```

 ---

 ## Important design point ⚠️

 Tumhare current architecture mein **`PaymentSuccessEvent` already inventory-service ko notify karta hai**:

 ```text
 PaymentSuccessEvent
 ↓
 Inventory Service
 ↓
 RESERVED → SOLD
 ```

 Uske baad `ProductSoldEvent` emit karna reasonable hai if you want a **separate inventory-domain event saying the sale was finalized**:

 ```text
 PaymentSuccessEvent
 ↓
 Inventory Service
 ↓
 RESERVED → SOLD
 ↓
 ProductSoldEvent
 ↓
 Analytics / Invoice / Audit
 ```

 So dono events duplicate nahi necessarily hain:

 * **`PaymentSuccessEvent`** → payment domain ka event: *payment successful hua*
 * **`ProductSoldEvent`** → inventory domain ka event: *inventory permanently sold hua*

 This separation is actually useful in a microservice architecture because each event represents a different **business fact**.

 ### Short mein

 ```text
 PaymentSuccessEvent
 ↓
 "Payment successful"

 ProductSoldEvent
 ↓
 "Inventory officially sold"
 ```

 Tumhare flash-sale system mein `ProductSoldEvent` **useful hai**, especially analytics aur inventory lifecycle ko clearly separate rakhne ke liye.

 */
