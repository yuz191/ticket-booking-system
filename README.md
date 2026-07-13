# High-Concurrency Ticket Booking System

A backend ticket booking system built with **Java, Spring Boot, PostgreSQL, Redis, Kafka, and Spring Security**.
The project simulates a high-concurrency ticket ordering scenario, focusing on inventory consistency, overselling prevention, asynchronous order processing, and JWT-based authentication.

## Features

* User registration and login with JWT authentication
* Event and ticket type management
* Ticket order creation with authenticated user identity
* Redis-based inventory pre-deduction using Lua scripts
* Atomic stock validation and decrement to prevent overselling
* Kafka-based asynchronous order confirmation
* Order status workflow: `PENDING`, `CONFIRMED`, `FAILED`
* PostgreSQL pessimistic locking for final database inventory consistency
* Scheduled compensation task for stale `PENDING` orders
* Idempotent Kafka consumer logic to avoid duplicate stock deduction
* Unit tests with JUnit and Mockito for order processing logic
* Local infrastructure setup with Docker Compose

## Tech Stack

| Layer                     | Technology                  |
| ------------------------- | --------------------------- |
| Backend                   | Java, Spring Boot           |
| Security                  | Spring Security, JWT        |
| Database                  | PostgreSQL, Spring Data JPA |
| Cache / Inventory Control | Redis, Lua Script           |
| Message Queue             | Kafka                       |
| Testing                   | JUnit, Mockito, Postman     |
| DevOps                    | Docker, Docker Compose      |

## System Design Overview

The system uses Redis and Kafka to handle high-concurrency ticket booking more safely and efficiently.

When a user submits an order, the system first checks and deducts ticket stock in Redis using a Lua script. This ensures that stock validation and stock decrement happen atomically, preventing overselling at the request entry point.

After Redis stock is successfully deducted, the system creates a `PENDING` order in PostgreSQL and publishes an order-created event to Kafka after the database transaction commits. A Kafka consumer then processes the order asynchronously, applies database-level inventory deduction with pessimistic locking, and updates the order status to `CONFIRMED` or `FAILED`.

## Order Flow

```text
1. User logs in and receives a JWT token.
2. User submits an order request.
3. Redis Lua script checks and deducts ticket stock atomically.
4. System creates a PENDING order in PostgreSQL.
5. After transaction commit, an order-created event is sent to Kafka.
6. Kafka consumer processes the order asynchronously.
7. Consumer locks the ticket type row in PostgreSQL.
8. If database stock is sufficient:
   - deduct database inventory
   - update order status to CONFIRMED
9. If database stock is insufficient or processing fails:
   - update order status to FAILED when applicable
   - compensate Redis stock if needed
10. A scheduled task retries stale PENDING orders.
```

## Why Redis?

Redis is used as the first layer of inventory control.

In high-concurrency ticket booking, many users may try to purchase the same ticket type at the same time. Directly checking and updating PostgreSQL for every request can cause heavy database load and potential overselling issues.

This project uses Redis with a Lua script to atomically:

```text
check stock -> decrement stock -> return result
```

This prevents multiple users from successfully purchasing more tickets than available.

## Why Kafka?

Kafka is used to decouple order submission from final order confirmation.

Instead of completing all database updates synchronously inside the user request, the system creates a `PENDING` order and sends an event to Kafka. The Kafka consumer then confirms the order asynchronously.

This design helps:

* reduce request latency
* absorb traffic spikes
* reduce direct pressure on the database
* support future asynchronous workflows such as payment, notification, analytics, and order timeout handling

## Why PostgreSQL Locking?

Redis prevents overselling at the request entry point, but PostgreSQL remains the final source of truth for persistent order and inventory data.

The Kafka consumer uses pessimistic locking when updating ticket inventory in PostgreSQL. This protects the final database update from concurrent modification and helps maintain consistency between Redis stock, database stock, and order status.

## Reliability Design

### Idempotent Consumer

Kafka messages may be delivered more than once. To avoid duplicate stock deduction, the consumer checks the order status before processing:

```text
Only PENDING orders can be processed.
CONFIRMED or FAILED orders are skipped.
```

This ensures that repeated consumption of the same Kafka message does not deduct inventory multiple times.

### Pending Order Compensation

If an order stays in `PENDING` for too long, it may indicate that the Kafka message was not sent successfully, the consumer failed, or the application was restarted during processing.

A scheduled compensation task scans stale `PENDING` orders and republishes order-created messages to Kafka for retry.

## Main API Endpoints

| Method | Endpoint                             | Description                   |
| ------ | ------------------------------------ | ----------------------------- |
| POST   | `/api/auth/register`                 | Register a new user           |
| POST   | `/api/auth/login`                    | Log in and receive JWT token  |
| POST   | `/api/events`                        | Create an event               |
| GET    | `/api/events`                        | Get all events                |
| GET    | `/api/events/{id}`                   | Get event details             |
| POST   | `/api/ticket-types`                  | Create a ticket type          |
| GET    | `/api/events/{eventId}/ticket-types` | Get ticket types for an event |
| POST   | `/api/orders`                        | Create a ticket order         |
| GET    | `/api/orders/{id}`                   | Get order status              |

## Local Setup

### Prerequisites

For Docker Compose:

* Docker Desktop
* Git

For running the Spring Boot app directly on your machine:

* Java 17+
* Maven or Maven Wrapper
* PostgreSQL, Redis, and Kafka

Optional:

* Postman for API testing

### Run With Docker Compose

```bash
docker compose up --build
```

This starts the full local development stack:

* Spring Boot backend: `http://localhost:8080`
* PostgreSQL: `localhost:5433`
* Redis: `localhost:6379`
* Kafka: `localhost:9092`
* Kafka UI: `http://localhost:8081`

Use this option when you want the backend application and all required infrastructure to run in containers.

### Run Production-Style Compose

Create a local production environment file from the template:

```bash
cp .env.prod.example .env.prod
```

Then update `.env.prod` with real values for `DB_PASSWORD` and `JWT_SECRET`, and start the production-style stack:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up --build -d
```

The production-style compose file only exposes the backend API port. PostgreSQL, Redis, and Kafka stay inside the Docker network.

### Build The Backend Image Manually

```bash
docker build -t ticket-booking-backend:latest .
```

If you publish the image to Docker Hub or another registry, replace the local build section in the compose file with an `image:` reference, for example:

```yaml
app:
  image: your-dockerhub-username/ticket-booking-backend:0.1.0
```

Then other people can pull and run the backend image without rebuilding it locally.

### Run Infrastructure Only

If you want to run the Spring Boot app directly from your IDE or Maven, you can still start only the supporting services:

```bash
docker compose up -d postgres redis kafka kafka-ui
```

### Run the Spring Boot Application

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```bash
.\mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test
```

On Windows PowerShell:

```bash
.\mvnw test
```

## Testing Scenarios

The project was tested with Postman for the following flows:

* Register and log in user
* Create event
* Create ticket type
* Initialize Redis stock
* Submit ticket order with JWT token
* Verify order status changes from `PENDING` to `CONFIRMED`
* Verify Redis stock deduction
* Verify PostgreSQL inventory deduction
* Verify duplicate Kafka message handling through idempotent consumer logic
* Verify Redis compensation when database inventory is insufficient

## Future Improvements

* Implement Transactional Outbox Pattern for more reliable Kafka publishing
* Add payment workflow and order timeout cancellation
* Add integration tests with Testcontainers
* Add API documentation with Swagger / OpenAPI
* Add CI/CD workflow with GitHub Actions
* Add monitoring and structured logging

## Project Highlights

This project demonstrates backend engineering skills in:

* high-concurrency inventory control
* distributed system reliability
* asynchronous event-driven processing
* database transaction handling
* cache and database consistency
* JWT authentication
* unit testing and backend API validation
* application containerization and local environment orchestration with Docker Compose
