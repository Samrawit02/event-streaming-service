# User Event Streaming Service

A high-performance User Event Streaming Service built with **Java 17** and **Spring Boot 3.3.4**. It handles user action events (such as `VIEW`, `ADD_TO_CART`, `PURCHASE`), stores them reliably in **PostgreSQL**, maintains a thread-safe in-memory queue of the latest 500 events for ultra-fast access, uses a custom **LRU (Least Recently Used) cache** to accelerate repeated queries, and enforces secure **token-based authentication (JWT)**.

---

## Key Features

- **PostgreSQL Persistence**: All user accounts and action events are persisted in PostgreSQL.
- **Thread-Safe In-Memory Queue**: An in-memory queue (`ConcurrentLinkedDeque`) stores the latest 500 events with automatic FIFO eviction.
- **LRU Search Cache**: Thread-safe access-ordered LRU cache with configurable capacity (default 100 entries) caching query results from `GET /events` to eliminate redundant database hits. Includes `X-Cache: HIT / MISS` response headers.
- **Token-Based Authentication (JWT)**: Secure HMAC-SHA256 JWT tokens with BCrypt password hashing. All endpoints (except `/auth/**`) require `Authorization: Bearer <token>`.
- **Flexible Event Ingestion**: `POST /events` accepts both a single event object and an array (batch) of events.
- **Fast Summary Aggregation**: `GET /events/summary` groups and counts events by type (`VIEW`, `ADD_TO_CART`, `PURCHASE`), with optional category filtering over the last 500 events in memory.
- **Clean Architecture**: Controller &rarr; Service &rarr; Repository pattern with DTOs, custom specifications for dynamic filtering, and global JSON exception handling.

---

## Requirements & Prerequisites

- **Java 17+**
- **PostgreSQL 16+** (or uses fallback H2 test profile)
- Maven Wrapper (`mvnw.cmd` on Windows or `./mvnw` on Linux/macOS)

---

## Configuration

Settings are in `src/main/resources/application.yml`. You can override them via environment variables:

| Environment Variable | Description | Default |
|---|---|---|
| `DB_HOST` | PostgreSQL hostname | `localhost` |
| `DB_PORT` | PostgreSQL port | `5433` (or `5432`) |
| `DB_NAME` | Database name | `event_stream_db` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `root` |
| `APP_JWT_SECRET` | 256-bit Hex/Base64 secret key | Pre-configured secure key |
| `APP_CACHE_LRU_CAPACITY` | LRU cache max entries | `100` |
| `APP_QUEUE_MAX_CAPACITY` | In-memory queue max events | `500` |

---

## Running the Application

### 1. Run Tests
```powershell
.\mvnw.cmd test
```

### 2. Start Application
```powershell
.\mvnw.cmd spring-boot:run
```
*(Or specify the test profile with in-memory H2 if PostgreSQL is not currently running:)*
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=test
```

On initial startup, `DataInitializer` automatically seeds a default administrative user:
- **Username**: `admin`
- **Password**: `password123`

---

## API Documentation & Examples

### 1. User Authentication

#### Authenticate / Login
- **Endpoint**: `POST /auth/login`
- **Request Body**:
  ```json
  {
    "username": "admin",
    "password": "password123"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "userId": 1,
    "username": "admin"
  }
  ```

#### Register User
- **Endpoint**: `POST /auth/register`
- **Request Body**:
  ```json
  {
    "username": "john_doe",
    "password": "securepassword"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": 2,
    "username": "john_doe",
    "message": "User registered successfully"
  }
  ```

---

### 2. Event Ingestion

#### Single Event Ingestion
- **Endpoint**: `POST /events`
- **Header**: `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "userId": 101,
    "eventType": "VIEW",
    "productId": 5001,
    "category": "electronics",
    "timestamp": "2026-09-15T10:00:00"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": 1,
    "userId": 101,
    "eventType": "VIEW",
    "productId": 5001,
    "category": "electronics",
    "timestamp": "2026-09-15T10:00:00"
  }
  ```

#### Batch Event Ingestion
- **Endpoint**: `POST /events`
- **Header**: `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  [
    {
      "userId": 101,
      "eventType": "ADD_TO_CART",
      "productId": 5001,
      "category": "electronics"
    },
    {
      "userId": 101,
      "eventType": "PURCHASE",
      "productId": 5001,
      "category": "electronics"
    }
  ]
  ```
- **Response** (`201 Created`): Array of created `EventResponse` objects.

---

### 3. Search & Filter Events (with LRU Caching)

- **Endpoint**: `GET /events`
- **Header**: `Authorization: Bearer <token>`
- **Query Parameters** (all optional):
  - `userId`: `Long`
  - `eventType`: `VIEW`, `ADD_TO_CART`, `PURCHASE`
  - `category`: `String`
  - `productId`: `Long`
  - `from`: ISO 8601 Date (`2026-09-15T00:00:00`)
  - `to`: ISO 8601 Date (`2026-09-15T23:59:59`)
- **Headers Returned**:
  - `X-Cache: HIT` (served from in-memory LRU cache)
  - `X-Cache: MISS` (queried from PostgreSQL and cached)
- **Response** (`200 OK`):
  ```json
  [
    {
      "id": 3,
      "userId": 101,
      "eventType": "PURCHASE",
      "productId": 5001,
      "category": "electronics",
      "timestamp": "2026-09-15T10:05:00"
    }
  ]
  ```

---

### 4. Fetch Recent Events

- **Endpoint**: `GET /events/recent`
- **Header**: `Authorization: Bearer <token>`
- **Function**: Returns the latest 20 events from the in-memory queue without querying the database.
- **Response** (`200 OK`): Array of up to 20 recent events in reverse chronological order (newest first).

---

### 5. Event Summary

- **Endpoint**: `GET /events/summary`
- **Header**: `Authorization: Bearer <token>`
- **Query Parameter**: `category` (optional, e.g. `?category=electronics`)
- **Function**: Summarizes the last 500 events from the in-memory queue, grouped by event type.
- **Response** (`200 OK`):
  ```json
  {
    "VIEW": 150,
    "ADD_TO_CART": 35,
    "PURCHASE": 10
  }
  ```

---

## Project Structure

```
Spring-boot-challenge-project/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/challenge/eventstream/
│   │   │   ├── EventStreamingApplication.java
│   │   │   ├── cache/
│   │   │   │   └── LruCache.java                # Thread-safe LRU Cache
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java        # DB seeding & queue preloader
│   │   │   │   └── SecurityConfig.java         # Spring Security 6 / Stateless
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java         # /auth/login & /auth/register
│   │   │   │   └── EventController.java        # /events endpoints
│   │   │   ├── dto/
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── EventRequest.java
│   │   │   │   ├── EventResponse.java
│   │   │   │   ├── EventSearchCriteria.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   └── RegisterRequest.java
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java # Unified JSON error responses
│   │   │   ├── model/
│   │   │   │   ├── Event.java                  # JPA Entity (indexes on key fields)
│   │   │   │   ├── EventType.java              # Enum: VIEW, ADD_TO_CART, PURCHASE
│   │   │   │   └── User.java                   # User entity with hashed password
│   │   │   ├── queue/
│   │   │   │   └── InMemoryEventQueue.java     # Thread-safe queue (bounded 500)
│   │   │   ├── repository/
│   │   │   │   ├── EventRepository.java        # Spring Data JPA + Specifications
│   │   │   │   ├── EventSpecifications.java    # Dynamic query predicate builder
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── JwtTokenProvider.java
│   │   │   └── service/
│   │   │       ├── AuthService.java
│   │   │       └── EventService.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── java/com/challenge/eventstream/
│       │   ├── EventStreamingIntegrationTest.java
│       │   ├── cache/LruCacheTest.java
│       │   ├── queue/InMemoryEventQueueTest.java
│       │   └── security/JwtTokenProviderTest.java
│       └── resources/
│           └── application.yml
```
