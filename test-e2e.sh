#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
MAILHOG_API="${MAILHOG_API:-http://localhost:8025/api/v2}"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_fail()    { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

require_tool() {
  command -v "$1" >/dev/null 2>&1 || log_fail "Missing required dependency: '$1'. Please install it."
}

require_tool curl
require_tool jq

# =============================================================================
# 1. Health Verification
# =============================================================================
log_info "Step 1: Checking API Gateway readiness..."
for i in {1..30}; do
  if curl -sf "${GATEWAY_URL}/actuator/health" | grep -q "UP"; then
    log_success "API Gateway is UP and operational."
    break
  fi
  if [ "$i" -eq 30 ]; then
    log_fail "API Gateway failed health probe after 30 attempts."
  fi
  sleep 2
done

# =============================================================================
# 2. Authentication & JWT Extraction
# =============================================================================
log_info "Step 2: Registering and logging in Admin user..."
TIMESTAMP=$(date +%s)
ADMIN_EMAIL="admin_${TIMESTAMP}@flashsale.com"
PASSWORD="Password123!"

curl -s -X POST "${GATEWAY_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${PASSWORD}\",\"name\":\"Admin User\",\"role\":\"ADMIN\"}" > /dev/null

ADMIN_LOGIN_RES=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${PASSWORD}\"}")

ADMIN_TOKEN=$(echo "${ADMIN_LOGIN_RES}" | jq -r '.data.accessToken // empty')
[ -n "${ADMIN_TOKEN}" ] || log_fail "Failed to retrieve Admin token: ${ADMIN_LOGIN_RES}"
log_success "Admin authenticated successfully."

# =============================================================================
# 3. Create Product & Seed Stock
# =============================================================================
log_info "Step 3: Creating Flash Sale Product (5 Stock allocated)..."
PRODUCT_PAYLOAD=$(cat <<EOF
{
  "name": "Flagship Smartphone Pro",
  "description": "Flash sale exclusive model",
  "price": 499.99,
  "stock": 5
}
EOF
)

PRODUCT_RES=$(curl -s -X POST "${GATEWAY_URL}/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -d "${PRODUCT_PAYLOAD}")

PRODUCT_ID=$(echo "${PRODUCT_RES}" | jq -r '.data.id // empty')
[ -n "${PRODUCT_ID}" ] || log_fail "Product creation failed: ${PRODUCT_RES}"
log_success "Product created with ID: ${PRODUCT_ID}"

# =============================================================================
# 4. Prepare Customer Accounts
# =============================================================================
log_info "Step 4: Creating 10 distinct customer accounts..."
CUSTOMER_TOKENS=()
for i in {1..10}; do
  CUST_EMAIL="customer_${TIMESTAMP}_${i}@test.com"
  curl -s -X POST "${GATEWAY_URL}/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${CUST_EMAIL}\",\"password\":\"${PASSWORD}\",\"name\":\"Customer ${i}\",\"role\":\"USER\"}" > /dev/null

  LOGIN_RES=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${CUST_EMAIL}\",\"password\":\"${PASSWORD}\"}")

  TOKEN=$(echo "${LOGIN_RES}" | jq -r '.data.accessToken // empty')
  CUSTOMER_TOKENS+=("${TOKEN}")
done
log_success "Created and logged in 10 test customers."

# =============================================================================
# 5. Concurrent Flash Sale Execution (Race Condition Test)
# =============================================================================
log_info "Step 5: Firing 10 concurrent purchase requests against 5 available items..."
RESULTS_DIR=$(mktemp -d)

for i in {1..10}; do
  TOKEN="${CUSTOMER_TOKENS[$((i-1))]}"
  ORDER_IDEMPOTENCY="IDEM-ORDER-${TIMESTAMP}-${i}"
  
  (
    HTTP_CODE=$(curl -s -o "${RESULTS_DIR}/order_${i}.json" -w "%{http_code}" \
      -X POST "${GATEWAY_URL}/api/orders" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${TOKEN}" \
      -d "{\"productId\":${PRODUCT_ID},\"quantity\":1,\"idempotencyKey\":\"${ORDER_IDEMPOTENCY}\"}")
    echo "${HTTP_CODE}" > "${RESULTS_DIR}/order_${i}.code"
  ) &
done

wait
log_info "All parallel order requests completed."

SUCCESSFUL_ORDERS=0
REJECTED_ORDERS=0
SUCCESS_ORDER_REFS=()
SUCCESS_TOKENS=()

for i in {1..10}; do
  CODE=$(cat "${RESULTS_DIR}/order_${i}.code")
  BODY=$(cat "${RESULTS_DIR}/order_${i}.json")
  
  if [ "$CODE" -eq 201 ] || [ "$CODE" -eq 200 ]; then
    SUCCESSFUL_ORDERS=$((SUCCESSFUL_ORDERS + 1))
    REF=$(echo "${BODY}" | jq -r '.data.orderReference // empty')
    SUCCESS_ORDER_REFS+=("${REF}")
    SUCCESS_TOKENS+=("${CUSTOMER_TOKENS[$((i-1))]}")
  else
    REJECTED_ORDERS=$((REJECTED_ORDERS + 1))
  fi
done

echo "---------------------------------------------------------"
log_info "Concurrency Results -> Successful: ${SUCCESSFUL_ORDERS} | Rejected (Out of Stock): ${REJECTED_ORDERS}"
echo "---------------------------------------------------------"

if [ "${SUCCESSFUL_ORDERS}" -ne 5 ]; then
  log_fail "Concurrency violated: Expected exactly 5 successful orders, got ${SUCCESSFUL_ORDERS}"
fi
log_success "Stock allocation verified. Zero overselling occurred."

# =============================================================================
# 6. Payment Processing & Kafka Event Saga
# =============================================================================
log_info "Step 6: Processing payment for the first successful order..."
TARGET_ORDER_REF="${SUCCESS_ORDER_REFS[0]}"
TARGET_TOKEN="${SUCCESS_TOKENS[0]}"
PAY_IDEMPOTENCY="IDEM-PAY-${TIMESTAMP}-1"

PAYMENT_PAYLOAD=$(cat <<EOF
{
  "orderReference": "${TARGET_ORDER_REF}",
  "amount": 499.99,
  "paymentMethod": "CREDIT_CARD",
  "idempotencyKey": "${PAY_IDEMPOTENCY}"
}
EOF
)

PAYMENT_RES=$(curl -s -X POST "${GATEWAY_URL}/api/payments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TARGET_TOKEN}" \
  -d "${PAYMENT_PAYLOAD}")

TXN_ID=$(echo "${PAYMENT_RES}" | jq -r '.data.transactionId // empty')
PAY_STATUS=$(echo "${PAYMENT_RES}" | jq -r '.data.status // empty')

[ "${PAY_STATUS}" = "SUCCESS" ] || log_fail "Payment capture failed: ${PAYMENT_RES}"
log_success "Payment captured. Transaction: ${TXN_ID}, Status: ${PAY_STATUS}"

log_info "Waiting 3 seconds for Kafka 'payment.completed' event propagation to order-service..."
sleep 3

ORDER_STATUS_RES=$(curl -s -X GET "${GATEWAY_URL}/api/orders/reference/${TARGET_ORDER_REF}" \
  -H "Authorization: Bearer ${TARGET_TOKEN}")
FINAL_ORDER_STATUS=$(echo "${ORDER_STATUS_RES}" | jq -r '.data.status // empty')

if [ "${FINAL_ORDER_STATUS}" != "PAID" ]; then
  log_warn "Order status is '${FINAL_ORDER_STATUS}' (expected 'PAID'). Polling again in 3s..."
  sleep 3
  ORDER_STATUS_RES=$(curl -s -X GET "${GATEWAY_URL}/api/orders/reference/${TARGET_ORDER_REF}" \
    -H "Authorization: Bearer ${TARGET_TOKEN}")
  FINAL_ORDER_STATUS=$(echo "${ORDER_STATUS_RES}" | jq -r '.data.status // empty')
fi

[ "${FINAL_ORDER_STATUS}" = "PAID" ] || log_fail "Order failed to transition to PAID state: ${ORDER_STATUS_RES}"
log_success "Kafka event saga completed: Order transitioned to PAID."

# =============================================================================
# 7. Notification Audit & MailHog Verification
# =============================================================================
log_info "Step 7: Verifying notifications for order ${TARGET_ORDER_REF}..."

NOTIF_RES=$(curl -s -X GET "${GATEWAY_URL}/api/notifications/order/${TARGET_ORDER_REF}" \
  -H "Authorization: Bearer ${TARGET_TOKEN}")

NOTIF_COUNT=$(echo "${NOTIF_RES}" | jq '.data | length')
if [ "${NOTIF_COUNT}" -lt 1 ]; then
  log_fail "No notification logs recorded for order: ${TARGET_ORDER_REF}"
fi
log_success "Notification audit log confirmed (${NOTIF_COUNT} events recorded)."

if curl -sf "${MAILHOG_API}/messages" >/dev/null 2>&1; then
  MAIL_COUNT=$(curl -s "${MAILHOG_API}/messages" | jq --arg ref "${TARGET_ORDER_REF}" '[.items[] | select(.Content.Headers.Subject[0] | contains($ref))] | length')
  log_success "MailHog captured ${MAIL_COUNT} email message(s) for order ${TARGET_ORDER_REF}."
fi

# Cleanup
rm -rf "${RESULTS_DIR}"

echo "========================================================="
log_success "ALL END-TO-END SYSTEM INTEGRATION TESTS PASSED!"
echo "========================================================="
