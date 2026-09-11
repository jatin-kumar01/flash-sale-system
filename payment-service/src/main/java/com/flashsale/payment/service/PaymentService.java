package com.flashsale.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.event.PaymentEvent;
import com.flashsale.common.exception.InvalidRequestException;
import com.flashsale.common.exception.ResourceNotFoundException;
import com.flashsale.common.outbox.OutboxStatus;
import com.flashsale.payment.dto.PaymentRequest;
import com.flashsale.payment.dto.PaymentResponse;
import com.flashsale.payment.entity.Payment;
import com.flashsale.payment.entity.Payment.PaymentStatus;
import com.flashsale.payment.entity.PaymentOutbox;
import com.flashsale.payment.kafka.PaymentProducer;
import com.flashsale.payment.repository.PaymentOutboxRepository;
import com.flashsale.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final PaymentProducer paymentProducer;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Value("${app.payment.simulation.mock-delay-ms:200}")
    private long mockDelayMs;

    @Value("${app.payment.simulation.simulated-failure-rate-percent:0}")
    private int failureRatePercent;

    @Value("${app.kafka.topics.payment-completed:payment.completed}")
    private String paymentCompletedTopic;

    @Transactional
    public PaymentResponse processPayment(Long userId, PaymentRequest request) {
        String transactionId = "TXN-" + request.getIdempotencyKey();

        // 1. Idempotency Check by Transaction ID
        Optional<Payment> existingByTxn = paymentRepository.findByTransactionId(transactionId);
        if (existingByTxn.isPresent()) {
            log.info("Duplicate payment detected for transactionId: {}", transactionId);
            return PaymentResponse.fromEntity(existingByTxn.get());
        }

        // 2. Prevent duplicate payment for an order already marked SUCCESS
        Optional<Payment> existingByOrder = paymentRepository.findByOrderReference(request.getOrderReference());
        if (existingByOrder.isPresent() && existingByOrder.get().getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Order {} has already been successfully paid with transactionId: {}",
                    request.getOrderReference(), existingByOrder.get().getTransactionId());
            throw new InvalidRequestException("Order has already been paid");
        }

        // 3. Create initial PENDING payment record
        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .orderReference(request.getOrderReference())
                .userId(userId)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 4. Simulate Payment Gateway Processing
        simulateGatewayCall();

        boolean simulatedFailure = failureRatePercent > 0 && random.nextInt(100) < failureRatePercent;

        if (simulatedFailure) {
            savedPayment.markFailed("Simulated gateway decline: insufficient funds or issuer error");
        } else {
            savedPayment.markSuccess();
        }

        Payment finalizedPayment = paymentRepository.save(savedPayment);

        // 5. Stage payment.completed event in Outbox
        PaymentEvent event = PaymentEvent.builder()
                .transactionId(finalizedPayment.getTransactionId())
                .orderReference(finalizedPayment.getOrderReference())
                .userId(finalizedPayment.getUserId())
                .amount(finalizedPayment.getAmount())
                .paymentMethod(finalizedPayment.getPaymentMethod())
                .status(finalizedPayment.getStatus().name())
                .occurredAt(Instant.now())
                .build();

        stagePaymentOutbox(finalizedPayment, event);

        log.info("Payment processed for transactionId: {}, orderReference: {}, outcome: {}",
                finalizedPayment.getTransactionId(), finalizedPayment.getOrderReference(), finalizedPayment.getStatus());

        return PaymentResponse.fromEntity(finalizedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));
        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderReference(String orderReference) {
        Payment payment = paymentRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderReference", orderReference));
        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(PaymentResponse::fromEntity);
    }

    private void simulateGatewayCall() {
        if (mockDelayMs > 0) {
            try {
                Thread.sleep(mockDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Payment gateway simulation interrupted");
            }
        }
    }

    private void stagePaymentOutbox(Payment payment, PaymentEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            PaymentOutbox outbox = PaymentOutbox.builder()
                    .aggregateType("PAYMENT")
                    .aggregateId(payment.getTransactionId())
                    .eventType("PAYMENT_COMPLETED")
                    .destinationTopic(paymentCompletedTopic)
                    .partitionKey(payment.getOrderReference())
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxRepository.save(outbox);
            log.info("Staged payment outbox event [txnId={}, orderRef={}] for topic: {}",
                    payment.getTransactionId(), payment.getOrderReference(), paymentCompletedTopic);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payment event payload for outbox", e);
        }
    }
}
/*This is the **most important file in the payment service so far**, because this is where the actual payment workflow, idempotency, database transaction, and Kafka event flow are connected.

The basic logic is good, but there are **some important concurrency and architecture issues** you should know about.

## 1. Overall flow

Your current implementation does this:

```text id="9c3v1m"
PaymentRequest
      ↓
PaymentService.processPayment()
      ↓
┌─────────────────────────────┐
│ 1. Check transactionId      │
│ 2. Check order already paid │
│ 3. Create PENDING payment   │
│ 4. Simulate gateway         │
│ 5. SUCCESS / FAILED         │
│ 6. Publish Kafka event      │
└──────────────┬──────────────┘
               ↓
        PaymentResponse
```

That's a sensible basic payment-service flow.

---

# 2. Transaction ID generation

```java
String transactionId = "TXN-" + request.getIdempotencyKey();
```

Suppose the request contains:

```json
{
  "orderReference": "ORD-1001",
  "amount": 999.99,
  "paymentMethod": "UPI",
  "idempotencyKey": "ABC123"
}
```

The transaction becomes:

```text
TXN-ABC123
```

So if the client sends the exact same request again:

```text
idempotencyKey = ABC123
```

the same transaction ID is generated.

That's why this works:

```java
paymentRepository.findByTransactionId(transactionId);
```

---

# 3. First idempotency check

```java
Optional<Payment> existingByTxn =
        paymentRepository.findByTransactionId(transactionId);
```

If found:

```java
if (existingByTxn.isPresent()) {
    return PaymentResponse.fromEntity(existingByTxn.get());
}
```

So:

```text
Request #1
ABC123
   ↓
Payment processed
   ↓
TXN-ABC123 → SUCCESS

Request #2
ABC123
   ↓
TXN-ABC123 already exists
   ↓
Return existing payment
```

This prevents the **normal retry/double-click scenario**.

### But there's an important limitation ⚠️

The `find` check itself is **not concurrency-safe**.

Imagine two requests arrive simultaneously:

```text
Request A ──┐
            ├── findByTransactionId()
Request B ──┘
```

Both might see:

```text
No payment found
```

Then both create:

```text
TXN-ABC123
```

Your database's unique constraint on `transactionId` is therefore still essential.

So the real protection should be:

```text
Application check
       +
Database UNIQUE constraint
       +
Proper duplicate handling
```

The current entity/repository already has the unique transaction ID constraint, which is good.

---

# 4. Second check: order already paid

```java
Optional<Payment> existingByOrder =
        paymentRepository.findByOrderReference(request.getOrderReference());
```

Then:

```java
if (existingByOrder.isPresent()
        && existingByOrder.get().getStatus() == PaymentStatus.SUCCESS) {
```

This prevents:

```text
Order ORD-1001
     ↓
Payment SUCCESS
     ↓
Someone tries another payment
     ↓
❌ Order has already been paid
```

That's a useful business-level protection.

---

# 5. Important issue with `findByOrderReference()`

Your repository currently has both:

```java
Optional<Payment> findByOrderReference(String orderReference);
```

and:

```java
List<Payment> findAllByOrderReference(String orderReference);
```

This means your design is somewhat ambiguous.

Are you allowing:

```text
One order → One payment
```

or:

```text
One order → Multiple payment attempts
```

For example:

```text
ORD-1001
   ├── TXN-1 → FAILED
   ├── TXN-2 → FAILED
   └── TXN-3 → SUCCESS
```

The second model is actually useful for payment retries.

But then:

```java
findByOrderReference()
```

can become problematic because there can be multiple records.

You should eventually decide explicitly whether **multiple payment attempts per order are allowed**.

For your system, I would recommend:

```text
Order
  ↓
Multiple payment attempts
  ↓
Only one SUCCESS
```

That fits the payment retry scenario better.

---

# 6. Creating PENDING payment

```java
Payment payment = Payment.builder()
        .transactionId(transactionId)
        .orderReference(request.getOrderReference())
        .userId(userId)
        .amount(request.getAmount())
        .paymentMethod(request.getPaymentMethod())
        .status(PaymentStatus.PENDING)
        .build();
```

This creates the payment record before gateway processing.

Example:

```text
id = 10
transactionId = TXN-ABC123
orderReference = ORD-1001
amount = ₹999.99
status = PENDING
```

Then:

```java
Payment savedPayment = paymentRepository.save(payment);
```

So the database knows that payment processing has started.

---

# 7. Simulated gateway

```java
simulateGatewayCall();
```

Inside:

```java
Thread.sleep(mockDelayMs);
```

If:

```yaml
mock-delay-ms: 200
```

the request waits approximately:

```text
200 ms
```

This is just simulating an external payment gateway.

For your portfolio project, that's completely reasonable.

---

# 8. Simulated failure

```java
boolean simulatedFailure =
        failureRatePercent > 0
        && random.nextInt(100) < failureRatePercent;
```

Suppose:

```yaml
simulated-failure-rate-percent: 20
```

Then approximately 20% of simulated payments will fail.

If failure:

```java
savedPayment.markFailed(
    "Simulated gateway decline: insufficient funds or issuer error"
);
```

Otherwise:

```java
savedPayment.markSuccess();
```

So the lifecycle is:

```text
PENDING
   ↓
 ┌───────┐
 ↓       ↓
SUCCESS  FAILED
```

That's good.

---

# 9. Saving the final state

```java
Payment finalizedPayment =
        paymentRepository.save(savedPayment);
```

Now PostgreSQL contains the final payment state.

For example:

```text
TXN-ABC123
     ↓
SUCCESS
```

---

# 10. Creating the Kafka event

```java
PaymentEvent event = PaymentEvent.builder()
        .transactionId(finalizedPayment.getTransactionId())
        .orderReference(finalizedPayment.getOrderReference())
        .userId(finalizedPayment.getUserId())
        .amount(finalizedPayment.getAmount())
        .paymentMethod(finalizedPayment.getPaymentMethod())
        .status(finalizedPayment.getStatus().name())
        .timestamp(Instant.now())
        .build();
```

This converts the internal payment result into an event that other services can understand.

For example:

```json
{
  "transactionId": "TXN-ABC123",
  "orderReference": "ORD-1001",
  "userId": 25,
  "amount": 999.99,
  "paymentMethod": "UPI",
  "status": "SUCCESS",
  "timestamp": "..."
}
```

Then:

```java
paymentProducer.sendPaymentCompletedEvent(event);
```

sends it to Kafka.

---

# 11. Payment → Order → Inventory

This is where your microservices start working together:

```text id="m4k0lw"
             Payment Service
                   │
                   │ payment.completed
                   ▼
                 Kafka
                   │
                   ▼
             Order Service
                   │
            status = PAID
                   │
                   ▼
             Inventory Service
                   │
            RESERVED → SOLD
```

For a failed payment:

```text id="8d2fx5"
Payment
   ↓
FAILED
   ↓
Kafka
   ↓
Order Service
   ↓
CANCELLED / FAILED
   ↓
Inventory
   ↓
Release reservation
```

That's the correct high-level relationship for your flash-sale system.

---

# 12. ⚠️ Biggest problem: Transactional Outbox

Your method has:

```java
@Transactional
```

which is good for the database operations.

But you are doing:

```text id="w8f9q2"
Database transaction
       ↓
Payment SUCCESS
       ↓
Kafka send
```

The database transaction and Kafka operation are **not one atomic transaction** here.

Imagine:

```text
Payment DB
   ↓
SUCCESS ✅

Kafka
   ↓
❌ unavailable
```

Then:

```text
Payment = SUCCESS
Order = still PENDING
Inventory = still RESERVED
```

because the `payment.completed` event never reached Kafka.

This is exactly why your architecture should eventually use **Transactional Outbox**.

### Production architecture

```text id="v0s7p1"
@Transactional
     │
     ├── Payment → SUCCESS
     │
     └── Outbox → payment.completed
                    │
                    ▼
             Outbox Publisher
                    │
                    ▼
                  Kafka
```

The current implementation is therefore **good for the initial implementation**, but I would **not call it fully production-ready yet**.

---

# 13. ⚠️ Another important problem: user-supplied amount

Currently:

```java
.amount(request.getAmount())
```

The client is telling your payment service:

```text
"I want to pay ₹100"
```

But imagine the actual order is:

```text
Order total = ₹10,000
```

and malicious/client-side data says:

```text
amount = ₹1
```

Your payment service could potentially process ₹1.

In a real system:

```text
PaymentService
      ↓
OrderService
      ↓
Get authoritative order amount
      ↓
Compare
      ↓
Process payment
```

The amount should ideally come from the trusted order/payment calculation rather than blindly trusting the frontend.

For your current simulation, you can keep it, but this is something to fix before calling the system production-grade.

---

# 14. ⚠️ `Thread.sleep()` and high concurrency

Your project is specifically about **high-throughput flash sales**.

This:

```java
Thread.sleep(mockDelayMs);
```

holds the request thread while waiting.

For example:

```text
1000 requests
×
200 ms
```

can put significant pressure on the server's worker threads.

For a simple mock gateway this is acceptable for demonstrating behavior, but don't use this pattern as your final high-concurrency payment architecture.

Later you can simulate an external gateway through an HTTP client with proper timeout/resilience behavior.

---

# 15. Query methods

### By transaction ID

```java
getPaymentByTransactionId()
```

Flow:

```text
TXN-ABC123
    ↓
Repository
    ↓
Payment
    ↓
PaymentResponse
```

If not found:

```java
throw new ResourceNotFoundException(...)
```

Good.

### By order reference

```java
getPaymentByOrderReference()
```

Same idea.

But again, if you decide to allow multiple payment attempts, this method's semantics should eventually be reconsidered.

### User payment history

```java
public Page<PaymentResponse> getUserPayments(
        Long userId,
        Pageable pageable)
```

This is good.

Instead of loading potentially thousands of payments:

```text
User
 ↓
10,000 payments
```

pagination gives:

```text
Page 1 → 20
Page 2 → 20
Page 3 → 20
...
```

Better for API performance.

---

# 16. Final assessment

### Good parts ✅

```text
PaymentRequest validation              ✅
Idempotency concept                    ✅
Database unique transaction ID         ✅
PENDING → SUCCESS/FAILED               ✅
PaymentStatus state model              ✅
BigDecimal for money                   ✅
Kafka event publishing                 ✅
Order-based Kafka key                  ✅
Pagination                             ✅
Read-only transactions                 ✅
Gateway simulation                     ✅
```

### Things to improve later ⚠️

```text
1. Database-safe concurrent idempotency
2. Clear payment-attempt model
3. Transactional Outbox
4. Don't blindly trust client amount
5. Proper external gateway integration
6. Idempotent Kafka event consumption
7. Payment state transition validation
```

### Most important conclusion

I would **keep this file for now rather than redesigning it immediately**.

Your current payment flow is:

```text id="u2j8x5"
Request
  ↓
Idempotency check
  ↓
Order payment check
  ↓
PENDING
  ↓
Gateway simulation
  ↓
SUCCESS / FAILED
  ↓
PostgreSQL
  ↓
Kafka
  ↓
Order Service
```

That's a solid **first implementation**.

The next missing piece in your `payment-service` structure is likely **`PaymentController.java`**. Once that is reviewed, we can see the complete external API flow and then identify exactly what needs to change before moving toward the Outbox/production-ready version.
*/