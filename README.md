# High-Throughput Event-Driven Flash Sale Microservices System

A distributed, production-oriented Spring Boot microservices platform engineered to handle high-concurrency flash-sale spikes. The system features distributed inventory concurrency control, hybrid Saga coordination, idempotent payment processing, asynchronous notification pipelines, and strict database-per-service isolation.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Service Boundaries & Data Ownership](#service-boundaries--data-ownership)
- [Technology Stack](#technology-stack)
- [Port & Networking Map](#port--networking-map)
- [Request Lifecycle & Data Flow](#request-lifecycle--data-flow)
- [Saga & Consistency Architecture](#saga--consistency-architecture)
- [Kafka Event Contract](#kafka-event-contract)
- [Concurrency & Inventory Protection](#concurrency--inventory-protection)
- [Idempotency & Resilience](#idempotency--resilience)
- [Security Model](#security-model)
- [Observability](#observability)
- [Repository Structure](#repository-structure)
- [Prerequisites](#prerequisites)
- [Running the Platform](#running-the-platform)
- [Local Development Workflow](#local-development-workflow)
- [End-to-End & Concurrency Testing](#end-to-end--concurrency-testing)
- [API Debugging with Bruno / Postman](#api-debugging-with-bruno--postman)
- [Troubleshooting & Runbook](#troubleshooting--runbook)
- [Current Trade-Offs & Production Roadmaps](#current-trade-offs--production-roadmaps)

---

## Architecture Overview

The system implements a domain-driven microservices architecture where services operate over isolated relational databases. Synchronous REST/HTTP is reserved for immediate transactional decisions (request routing, authentication, and stock reservation), while asynchronous Apache Kafka events handle decoupled side-effects and cross-domain state transitions.

```
                           ┌───────────────────────────┐
                           │        Client / UI        │
                           └─────────────┬─────────────┘
                                         │
                                      HTTPS/REST
                                         │
                                         ▼
                           ┌───────────────────────────┐
                           │    API Gateway (:8080)    │
                           │  JWT Validation & Routing │
                           └─────────────┬─────────────┘
                                         │
           ┌─────────────────────────────┼─────────────────────────────┐
           │ Synchronous REST            │ Synchronous REST            │ Synchronous REST
           ▼                             ▼                             ▼
  ┌─────────────────┐           ┌─────────────────┐           ┌───────────────────┐
  │  Auth Service   │           │ Product Service │           │ Inventory Service │
  │     (:8081)     │           │     (:8082)     │           │      (:8083)      │
  └─────────────────┘           └─────────────────┘           └─────────┬─────────┘
                                                                        │
                                                                Mutual Exclusion
                                                                Distributed Lock
                                                                        ▼
                                                              ┌───────────────────┐
                                                              │   Redis (:6379)   │
                                                              └───────────────────┘

           ┌─────────────────────────────┐
           │ Synchronous REST            │ Synchronous REST
           ▼                             ▼
  ┌─────────────────┐           ┌─────────────────┐
  │  Order Service  │           │ Payment Service │
  │     (:8084)     │           │     (:8085)     │
  └────────┬────────┘           └────────┬────────┘
           │                             │
           │ Synchronous REST (Reserve)  │
           ├─────────────────────────────┘
           │
           │ Domain Events (Kafka Pub/Sub)
           ▼
══════════════════════════════════════════════════════════════════════════════════════════
Apache Kafka KRaft (:9092)
Topics: order.created  |  order.cancelled  |  order.expired  |  payment.completed
══════════════════════════════════════════════════════════════════════════════════════════
│                                             │
│ Subscribes: payment.completed               │ Subscribes: All Topics
▼                                             ▼
┌─────────────────┐                           ┌───────────────────┐
│  Order Service  │                           │Notification Svc   │
│     (:8084)     │                           │      (:8086)      │
└─────────────────┘                           └─────────┬─────────┘
                                                        │
                                                   SMTP Relay
                                                        ▼
                                              ┌───────────────────┐
                                              │  MailHog Sandbox  │
                                              │ (:1025 / UI:8025) │
                                              └───────────────────┘
```

### Communication Invariants

| Channel | Protocol | Participants | Operational Guarantee |
| :--- | :--- | :--- | :--- |
| **Ingress Routing** | HTTP / REST | Client → API Gateway → Downstream Services | Synchronous; fails fast on authentication errors or timeouts. |
| **Stock Reservation** | HTTP / REST | Order Service → Inventory Service | Synchronous; blocking reservation with immediate out-of-stock signaling. |
| **Lifecycle Events** | Kafka (KRaft) | Order Service / Payment Service → Broker | Asynchronous; decoupled event publication partitioned by `orderReference`. |
| **Event Reaction** | Kafka (KRaft) | Broker → Notification Service / Order Service | Asynchronous; at-least-once consumption with consumer-side idempotency. |
| **Distributed Locking**| RESP (Redis) | Inventory Service ↔ Redis | Millisecond TTL locking to guard concurrent inventory read-modify-write. |
| **Data Persistence** | JDBC / TCP | Each Microservice ↔ PostgreSQL | ACID transactions strictly scoped to the service's local schema. |

---

## Service Boundaries & Data Ownership

Every service has an isolated database schema within PostgreSQL. Direct cross-database joins or queries are prohibited.

| Service | Port | Database Schema | Primary Responsibilities | Data Exclusively Owned |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | *None* | JWT validation, CORS, reverse proxy routing | Routing rules, rate limits |
| **Auth Service** | `8081` | `flashsale_auth` | User registration, authentication, JWT generation | Users, credentials, roles |
| **Product Service** | `8082` | `flashsale_product` | Product catalog, base pricing, metadata | Products, descriptions, prices |
| **Inventory Service**| `8083` | `flashsale_inventory` | Real-time stock reservation, releases, deductions | Available stock, reserved stock |
| **Order Service** | `8084` | `flashsale_order` | Order lifecycle state, Saga coordination | Orders, items, checkout status |
| **Payment Service** | `8085` | `flashsale_payment` | Gateway simulation, transaction idempotency | Payments, gateway receipts |
| **Notification Service**| `8086` | `flashsale_notification`| Event consumption, HTML rendering, delivery logs | Notification logs, dispatch receipts |

---

## Technology Stack

- **Runtime & Language:** Java 21 (Eclipse Temurin LTS), Spring Boot 3.3.x
- **Cloud Infrastructure & Routing:** Spring Cloud Gateway, Netflix Eureka
- **Messaging:** Apache Kafka 7.6.0 (KRaft Mode, 3 partitions per topic)
- **Primary Datastores:** PostgreSQL 16 (shared container, isolated logical databases)
- **Distributed Cache & Locking:** Redis 7.2 (Alpine)
- **Mail Mocking:** MailHog (Go-based SMTP server + Web UI)
- **Containerization:** Multi-stage Docker builds utilizing Spring Boot `layertools`
- **Build System:** Apache Maven 3.9+

---

## Port & Networking Map

```text
Host Port     Container Port    Service / Process          Internal Target Address
──────────────────────────────────────────────────────────────────────────────────
8080          8080              API Gateway                api-gateway:8080
8081          8081              Auth Service               auth-service:8081
8082          8082              Product Service            product-service:8082
8083          8083              Inventory Service          inventory-service:8083
8084          8084              Order Service              order-service:8084
8085          8085              Payment Service            payment-service:8085
8086          8086              Notification Service       notification-service:8086
8761          8761              Eureka Discovery Server    eureka-server:8761
5432          5432              PostgreSQL DBMS            postgres:5432
6379          6379              Redis In-Memory Engine     redis:6379
9092          9092              Kafka Broker (Host)        kafka:29092 (Internal)
1025          1025              MailHog SMTP Relay         mailhog:1025
8025          8025              MailHog Browser UI         mailhog:8025
```

---

## Request Lifecycle & Data Flow

```text
[Customer]             [Gateway]           [OrderSvc]         [InventorySvc]         [PaymentSvc]           [Kafka]             [NotificationSvc]
    │                      │                    │                   │                     │                    │                    │
    │ 1. POST /api/orders  │                    │                   │                     │                    │                    │
    ├─────────────────────►│                    │                   │                     │                    │                    │
    │                      │ 2. Forward Req     │                   │                     │                    │                    │
    │                      ├───────────────────►│                   │                     │                    │                    │
    │                      │                    │ 3. Reserve Stock  │                     │                    │                    │
    │                      │                    ├──────────────────►│ (Redis Lock)        │                    │                    │
    │                      │                    │                   │ Deduct available    │                    │                    │
    │                      │                    │ 4. Reservation OK │                     │                    │                    │
    │                      │                    │◄──────────────────┤                     │                    │                    │
    │                      │                    │                                         │                    │                    │
    │                      │                    │ 5. Save Order (PENDING)                 │                    │                    │
    │                      │                    │ 6. Send order.created                   │                    │                    │
    │                      │                    ├─────────────────────────────────────────────────────────────►│                    │
    │                      │ 7. HTTP 201 Created│                                         │                    │ 7b. Consume & Mail │
    │◄─────────────────────┼────────────────────┤                                         │                    ├───────────────────►│
    │                      │                    │                                         │                    │                    │
    │ 8. POST /api/payments                     │                                         │                    │                    │
    ├───────────────────────────────────────────┼────────────────────────────────────────►│                    │                    │
    │                      │                    │                                         │ Check Idempotency  │                    │
    │                      │                    │                                         │ Persist TXN        │                    │
    │                      │                    │                                         │ 9. payment.completed                    │
    │                      │                    │                                         ├───────────────────►│                    │
    │                      │                    │ 10. Consume payment.completed           │                    │ 10b. Send Receipt  │
    │                      │                    │◄─────────────────────────────────────────────────────────────┼───────────────────►│
    │                      │                    │                                                              │                    │
    │                      │                    ├── IF SUCCESS:                                                │                    │
    │                      │                    │     Order status = PAID                                      │                    │
    │                      │                    │     POST /api/inventory/confirm-sell                         │                    │
    │                      │                    │                                                              │                    │
    │                      │                    └── IF FAILED:                                                 │                    │
    │                      │                          Order status = CANCELLED                                 │                    │
    │                      │                          POST /api/inventory/release-stock                        │                    │
```

---

## Saga & Consistency Architecture

This architecture uses a **Hybrid Saga Model**:

1. **Orchestrated Domain Core:** The `order-service` acts as a localized coordinator for stock finalization. When receiving the asynchronous `payment.completed` event from Kafka, it executes local state mutations (`PAID` or `CANCELLED`) and invokes compensation or finalization commands on the `inventory-service`.
2. **Choreographed Edge Broadcast:** The `notification-service` is fully choreographed. It listens across all domain event topics without any service explicitly instructing it to notify users.

### Order State Machine

```
              ┌────────────────────────┐
              │      Order Created     │
              └───────────┬────────────┘
                          │
                          ▼
              ┌────────────────────────┐
              │    PENDING_PAYMENT     │
              └─────┬────────────┬─────┘
                    │            │
   Payment Succeeded│            │ Payment Failed / Window Expired
                    ▼            ▼
         ┌───────────────┐  ┌───────────────┐
         │     PAID      │  │   CANCELLED   │
         └───────┬───────┘  └───────┬───────┘
                 │                  │
                 ▼                  ▼
          Deduct Stock       Release Reserved
           Permanently            Stock
```

---

## Kafka Event Contract

Messages use JSON serialization. To maintain partition-level ordering across state transitions for a given purchase, all events are published with the `orderReference` as the Kafka message key.

### Topic Catalog

```text
Topic Name            Partition Key      Producer            Consumers
─────────────────────────────────────────────────────────────────────────────────────────────
order.created         orderReference     order-service       notification-service
order.cancelled       orderReference     order-service       notification-service
order.expired         orderReference     order-service       inventory-service, notification-service
payment.completed     orderReference     payment-service     order-service, notification-service
```

### Event Schemas

#### 1. `OrderEvent` (`order.created`, `order.cancelled`, `order.expired`)

```json
{
  "orderReference": "ORD-1725624000-A8F9",
  "userId": 42,
  "productId": 101,
  "quantity": 1,
  "totalAmount": 499.99,
  "status": "PENDING",
  "timestamp": "2026-09-06T12:00:00Z"
}
```

#### 2. `PaymentEvent` (`payment.completed`)

```json
{
  "transactionId": "TXN-IDEM-PAY-98124",
  "orderReference": "ORD-1725624000-A8F9",
  "userId": 42,
  "amount": 499.99,
  "paymentMethod": "CREDIT_CARD",
  "status": "SUCCESS",
  "timestamp": "2026-09-06T12:00:05Z"
}
```

---

## Concurrency & Inventory Protection

Flash sales generate extreme contention on a small set of database rows. Directly issuing `SELECT ... UPDATE` against PostgreSQL causes lock escalation, connection pool exhaustion, and deadlocks.

```text
Incoming 10 Parallel Requests (Available Stock = 1)
 ├── Req 1 ──┐
 ├── Req 2 ──┤
 ├── Req 3 ──┼──► [SETNX lock:inventory:item_101 TTL=3s]
 ├── ...     │         │
 └── Req 10 ─┘         ├── Acquired: Read PostgreSQL -> Stock > 0 -> Reserve -> Commit DB -> Release Lock
                       └── Denied: Return HTTP 409 / 422 Conflict (Fail Fast)
```

### Protection Mechanics

1. **Lock Scope:** Locks are defined with per-product granularity: `lock:inventory:{productId}`.
2. **Mutual Exclusion:** `SETNX` prevents concurrent read-modify-write cycles across clustered instances of `inventory-service`.
3. **Deadlock Bounding:** Locks enforce a strict Time-to-Live (TTL = 3000ms). If a container fails while executing stock operations, the lock naturally expires.
4. **Local Transaction Boundary:** Inside the acquired lock, PostgreSQL executes:
```sql
UPDATE inventory 
SET available_quantity = available_quantity - :qty,
    reserved_quantity = reserved_quantity + :qty
WHERE product_id = :productId AND available_quantity >= :qty;
```
5. **Fail-Fast Semantics:** Incoming requests unable to acquire the lock within 200ms fail immediately rather than queuing indefinitely.

---

## Idempotency & Reliability

Distributed execution introduces network timeouts and client retries. Idempotency guards exist at multiple layers:

### 1. HTTP Layer (Payment & Order Ingress)

* Requests supply an `idempotencyKey` string in the JSON payload.
* `payment-service` generates a unique hash `TXN-{idempotencyKey}` backed by a `UNIQUE` constraint in PostgreSQL.
* Re-transmitting an identical idempotency key returns the original recorded transaction receipt rather than initiating a duplicate charge.

### 2. Messaging Layer (Kafka Consumers)

* `notification-service` enforces deduplication via `notificationLogRepository.existsByOrderReferenceAndEventType(...)`. Duplicate events from Kafka rebalances or network re-deliveries are safely ignored.
* `order-service` checks current order status before processing `payment.completed`. If an order is already marked `PAID`, subsequent event arrivals are discarded.

---

## Security Model

The system follows an edge-authenticated architectural pattern:

```text
[External Traffic]
        │
        │ Authorization: Bearer <JWT>
        ▼
┌─────────────────────────────────┐
│       API Gateway (:8080)       │
│                                 │
│ 1. Intercept HTTP Request       │
│ 2. Validate JWT Signature & Exp │
│ 3. Strip External Claims        │
│ 4. Inject Verified Headers:     │
│    - X-User-Id: 42              │
│    - X-User-Role: ROLE_USER     │
└────────────────┬────────────────┘
                 │
                 │ Downstream Internal Routing
                 ▼
┌─────────────────────────────────┐
│     Microservice Instances      │
│  Trusts internal network        │
│  Inspects @RequestHeader        │
└─────────────────────────────────┘
```

* Public endpoints are restricted to `/api/auth/**` and public catalog browsing `/api/products/**`.
* Mutation endpoints (`/api/orders/**`, `/api/payments/**`) require authenticated tokens.
* Internal infrastructure ports (`5432`, `6379`, `29092`) are bound to the internal bridge network (`flashsale-net`).

---

## Observability

### Health Probes & Monitoring

Each Spring Boot service exposes Spring Boot Actuator health and metrics endpoints:

```bash
# Check API Gateway Health
curl -s http://localhost:8080/actuator/health

# Check Database Connection Status from Order Service
curl -s http://localhost:8084/actuator/health/db
```

### Correlated Logging Strategy

Services log key operational events using consistent contextual prefixes:

```text
[order-service] [INFO] Publishing OrderCreated event for orderReference: ORD-1725624000-A8F9, userId: 42
[notification-service] [INFO] Received OrderCreated event for notification: key=ORD-1725624000-A8F9, partition=1, offset=14
[payment-service] [INFO] Payment processed for transactionId: TXN-IDEM-PAY-1, orderReference: ORD-1725624000-A8F9, outcome: SUCCESS
[order-service] [INFO] Received PaymentCompleted event: order ORD-1725624000-A8F9 transitioned to PAID
```

---

## Repository Structure

```text
flash-sale-system/
├── pom.xml                               # Maven multi-module parent configuration
├── docker-compose.yml                    # Full-stack composition (Infra + 7 Services)
├── test-e2e.sh                           # High-concurrency automated verification suite
├── flash-sale-api-collection.json        # Postman / Bruno export
├── docker/
│   ├── Dockerfile                        # Multi-stage layertools build
│   └── postgres/
│       └── init-multiple-databases.sh    # Multi-schema PostgreSQL bootstrap
├── common/                               # Shared event contracts, exceptions, and DTO envelopes
├── eureka-server/                        # Spring Cloud Netflix Eureka Discovery
├── api-gateway/                          # Spring Cloud Gateway with JWT filters
├── auth-service/                         # Authentication, User credentials, JWT issuance
├── product-service/                      # Catalog, item listings, prices
├── inventory-service/                    # Concurrency locks, stock reservations
├── order-service/                        # Order state machine, Saga coordinator
├── payment-service/                      # Mock payment processor, transaction ledger
└── notification-service/                 # Asynchronous Kafka event consumer, Mailer
```

---

## Prerequisites

Ensure the host system has the following installed:

* **Docker Engine:** `24.0.0+`
* **Docker Compose:** `v2.20.0+`
* **Java SDK:** `OpenJDK 21` or `Eclipse Temurin 21`
* **Build Tool:** `Apache Maven 3.9+`
* **Command-Line Utilities:** `bash`, `curl`, `jq`

---

## Running the Platform

### 1. Build Fat JARs

Build all services skipping tests to produce the target JAR files:

```bash
mvn clean package -DskipTests
```

### 2. Start the Docker Stack

Launch infrastructure datastores, Kafka, service discovery, and all microservice containers:

```bash
docker compose up -d --build
```

### 3. Verify Container Health

Verify all 13 containers are running and healthy:

```bash
docker compose ps
```

### 4. Inspect Service Registration

Navigate to the Eureka dashboard at [http://localhost:8761](http://localhost:8761). Confirm that all 6 application clients are listed under **Instances currently registered with Eureka**:

* `API-GATEWAY`
* `AUTH-SERVICE`
* `PRODUCT-SERVICE`
* `INVENTORY-SERVICE`
* `ORDER-SERVICE`
* `PAYMENT-SERVICE`
* `NOTIFICATION-SERVICE`

---

## Local Development Workflow

When modifying a single service, running the entire stack in Docker can slow down feedback loops. Run shared infrastructure inside Docker and run the service under development locally on the host:

```bash
# 1. Start only backing infrastructure
docker compose up -d postgres redis kafka kafka-init-topics mailhog eureka-server

# 2. Run target service locally via Maven
cd inventory-service
mvn spring-boot:run
```

The service will connect to `localhost:5432` and `localhost:6379`, and register itself with Eureka on `localhost:8761`.

---

## End-to-End & Concurrency Testing

An automated integration script (`test-e2e.sh`) is provided to test the system under high concurrency.

### Concurrency Test Scenario

```text
Total Available Stock = 5 Units
Incoming Requests     = 10 Parallel Customer Orders
Expected Outcome      = Exactly 5 Approved (HTTP 201), Exactly 5 Rejected (HTTP 4xx)
Eventual Consistency = Order status transitions to PAID via Kafka within 5 seconds
```

### Running the Test

```bash
chmod +x test-e2e.sh
./test-e2e.sh
```

### Test Phases Executed

1. Probes `http://localhost:8080/actuator/health` until all components are operational.
2. Registers an administrator and creates a flash sale product with **5 units of stock**.
3. Registers and extracts JWT authentication tokens for **10 isolated customer accounts**.
4. Spawns 10 asynchronous subshells in parallel to submit orders simultaneously.
5. Asserts that **exactly 5 orders succeed** and **5 orders fail** with out-of-stock errors.
6. Executes payment processing for a successful order.
7. Polls order status until Kafka propagates `payment.completed` and verifies transition to `PAID`.
8. Asserts database dispatch receipts via `/api/notifications/order/{orderReference}`.
9. Queries the MailHog REST API (`:8025/api/v2/messages`) to confirm email delivery.

---

## API Debugging with Bruno / Postman

Import `flash-sale-api-collection.json` into Postman or Bruno.

### Test Run Order

1. **Authentication:** Run `Register Admin` followed by `Login Admin` (auto-saves `adminToken`).
2. **Catalog Setup:** Run `Create Flash Sale Product` (auto-saves `productId`).
3. **Customer Auth:** Run `Register Customer` followed by `Login Customer` (auto-saves `userToken`).
4. **Checkout:** Run `Create Flash Sale Order` (auto-saves `orderReference`).
5. **Payment Capture:** Run `Process Payment For Order` (auto-saves `transactionId`).
6. **Verification:** Inspect `Get Order By Reference` to verify status has moved to `PAID`.
7. **Audit:** Run `Get Order Notification Logs` to view dispatched email metadata.

---

## Troubleshooting & Runbook

### 1. Containers Exit or Fail Healthchecks on Startup

* **Cause:** PostgreSQL or Kafka initialization may exceed timeout thresholds on machines with slower disk I/O.
* **Remedy:** Inspect logs for the failing container:
```bash
docker compose logs postgres
docker compose logs kafka
```

* If PostgreSQL fails during the multi-database setup, delete the existing volume and reinitialize:
```bash
docker compose down -v
docker compose up -d --build
```

### 2. Services Fail to Register with Eureka

* **Cause:** Network interface initialization delay on Docker bridge network.
* **Remedy:** Review Eureka client retry logs:
```bash
docker compose logs eureka-server
docker compose logs order-service | grep -i eureka
```

### 3. Kafka Consumer Does Not Receive Messages

* **Cause:** Incorrect bootstrap address configuration or consumer group rebalance stalls.
* **Remedy:** Verify consumer group health directly inside the broker:
```bash
docker exec -it flashsale-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group notification-group
```

### 4. Emails Not Visible in MailHog

* **Cause:** `notification-service` failed to contact SMTP port `1025` or message deserialization threw an exception.
* **Remedy:** Verify consumer status:
```bash
docker compose logs notification-service | grep -i "failed"
```

Open the web interface at [http://localhost:8025](http://localhost:8025) and check for messages delivered to `user{id}@flashsale-example.com`.

---

## Current Trade-Offs & Production Roadmaps

| Feature | Current State | Production Recommendation |
| --- | --- | --- |
| **Outbox Pattern** | Direct Kafka dispatch following JPA save (Dual-write hazard). | Implement the **Transactional Outbox Pattern** using Debezium CDC to guarantee reliable event publication. |
| **Distributed Tracing** | Standard SLF4J structured logs. | Integrate **Micrometer Tracing + OpenTelemetry** to propagate `traceId` across HTTP headers and Kafka record headers. |
| **Circuit Breaking** | Unprotected synchronous REST calls. | Introduce **Resilience4j** circuit breakers and fallbacks between `order-service` and `inventory-service`. |
| **Inventory Partitioning** | Single Redis distributed lock key per product. | Transition to Redis Lua scripts (`EVAL`) or partition inventory buckets across cache nodes for extreme scale (>50k req/sec). |
| **Secrets Management** | Docker Compose environment variables. | Mount configurations securely using **HashiCorp Vault** or cloud-native secrets managers. |
