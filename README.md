# 💸 FinFlow: High-Concurrency Ledger & Payment System

![Java 17](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)
![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)
![Apache Kafka](https://img.shields.io/badge/Kafka-Event%20Driven-black.svg)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue.svg)

FinFlow is a robust, production-grade financial ledger and payment processing system built with **Spring Boot** and **MySQL**. It is designed to handle high-concurrency environments while guaranteeing absolute data consistency, preventing double-spending, and maintaining a strict double-entry accounting audit trail.

---

## 🚀 Key Features

* **Concurrency & Safety (Optimistic Locking)**: Eliminates "lost updates" and database deadlocks when multiple transactions attempt to deduct funds simultaneously. The system uses a `@Version` entity attribute with a smart retry mechanism (handling `OptimisticLockingFailureException`) to ensure eventual success without overriding concurrent changes.
* **Distributed Idempotency (Redis)**: All transfer endpoints require an `Idempotency-Key` header. The system uses **Redis** (`setIfAbsent`) as a distributed cache to track idempotency keys, ensuring that even if a client encounters a network timeout and retries the exact payment, the transaction is *never* processed twice.
* **Event-Driven Architecture (Kafka + DLQ)**: Employs **Apache Kafka** for asynchronous processing with a **Dead Letter Queue (DLQ)**. Transactions that fail processing after 3 retries are automatically routed to a separate DLQ topic for manual investigation — preventing silent data loss.
* **JWT Authentication**: Secured REST APIs using stateless **JSON Web Tokens (JWT)**. Users must register and login to obtain a Bearer token before accessing any financial endpoints.
* **Rate Limiting (Redis + AOP)**: Custom `@RateLimited` annotation powered by Spring AOP and Redis to throttle API abuse (10 requests/minute per IP per endpoint). Returns `429 Too Many Requests` when exceeded.
* **Scheduled Reconciliation**: An automated hourly job that mathematically verifies `SUM(Credits) == SUM(Debits)` across the entire ledger, ensuring no money is ever created or destroyed in the system.
* **Double-Entry Ledger**: Every transaction inserts two immutable ledger entries (a debit and a credit) tracking the exact account balances before and after the transaction.
* **Paginated APIs**: Ledger history endpoints support pagination (`?page=0&size=10`) to prevent unbounded data responses.
* **Health Monitoring (Actuator)**: Spring Boot Actuator exposes `/actuator/health` with real-time status of MySQL, Redis, and Kafka connectivity.
* **Full Integration Testing**: The project employs **Testcontainers** for true-to-life integration tests against a real MySQL instance, completely bypassing the limitations of in-memory H2 databases.

## 🛠️ Tech Stack

* **Java 17 & Spring Boot 3.3**
* **Spring Security + JWT (jjwt)**
* **MySQL 8.0**
* **Redis 7.0**
* **Apache Kafka & Zookeeper**
* **Spring AOP**
* **Docker & Docker Compose**
* **Testcontainers (JUnit 5)**
* **Swagger/OpenAPI (springdoc)**
* **Maven**

---

## 📡 API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | NO | Create a new user account |
| POST | `/auth/login` | NO | Authenticate and receive JWT token |
| POST | `/accounts` | YES | Create a new bank account |
| GET | `/accounts/{id}/balance` | YES | Check current balance |
| GET | `/accounts/{id}/ledger?page=0&size=10` | Yes | Paginated transaction history |
| POST | `/transactions` | yes + Rate Limited | Initiate a money transfer |
| GET | `/transactions/{ref}` | yes | Check transaction status |
| GET | `/actuator/health` | No | System health check |
| GET | `/swagger-ui.html` | NO | Interactive API documentation |

---

## 🎥 Demo


### Quick Start
```bash
# 1. Start infrastructure (MySQL, Redis, Kafka)
docker-compose up -d

# 2. Run the application
./mvnw spring-boot:run

# 3. Register a user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "password123"}'

# 4. Use the returned JWT token for all subsequent requests
curl -X POST http://localhost:8080/transactions \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Idempotency-Key: demo-tx-1" \
  -H "Content-Type: application/json" \
  -d '{"fromAccount": "ACC-001", "toAccount": "ACC-002", "amount": 100.00}'
```

---

## 🏗️ Architecture & Design

### Services (7 Core + 3 Infrastructure)

| Service | Responsibility |
|---------|---------------|
| `TransactionService` | Validates transfers, checks idempotency, publishes to Kafka |
| `TransferProcessor` | Executes the ACID database transfer with double-entry ledger |
| `LedgerService` | Orchestrates retry logic for optimistic locking conflicts |
| `AccountService` | CRUD operations for bank accounts |
| `IdempotencyService` | Redis-based distributed duplicate detection |
| `JwtService` | JWT token generation and validation |
| `ReconciliationService` | Scheduled hourly ledger balance verification |
| `TransactionProducer` | Kafka message publisher |
| `TransactionConsumer` | Kafka message listener |
| `DlqConsumer` | Dead Letter Queue listener for failed transactions |

### Database Schema

* **`accounts`** — Current balances with `version` column for Optimistic Locking
* **`transactions`** — Transfer intents with status tracking and `idempotency_key` (unique)
* **`ledger_entries`** — Immutable audit trail with `balance_after` for every debit/credit
* **`users`** — JWT authentication credentials

## 🧪 Running Tests

The test suite includes a **ConcurrencyTest** (20 simultaneous threads), a **TransactionFailureTest** (insufficient funds rollback), and context loading tests — all running against real containerized MySQL via Testcontainers.

```bash
# Requires Docker running!
./mvnw clean test
```
