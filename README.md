# online-shopping-system
A web-based multi-seller Online Shopping System built with Spring Boot 3.4 and Java 21, adhering to the finalized Version 2.0 Shared Architecture & API Contract. The platform allows customers to browse products, manage carts, apply discounts, make secure payments, and track orders, with seller fulfilment and delivery management features.

---

## Architecture & Technology Stack

- **Backend:** Java 21 LTS, Spring Boot 3.4.2
- **Persistence & ORM:** Spring Data JPA, Hibernate ORM
- **Database:** MySQL 8.x (Production/Integration), H2 Database (Test slice verification)
- **Security:** Spring Security (stateless REST configuration, BCrypt password hashing, JWT authentication & authorization)
- **Architecture Specification:** Version 2.0 Contract (`docs/Team_5_Final_Shared_Architecture_API_Contract_V2.pdf`)
- **Build System:** Maven 3.9.x with Maven Wrapper (`mvnw` / `mvnw.cmd`)

---

## Backend Project Structure

The Spring Boot backend service is located in the `backend/` directory:

```
backend/
├── .mvn/wrapper/                          # Maven Wrapper distribution and jar
├── mvnw / mvnw.cmd                        # Maven Wrapper executables
├── pom.xml                                # Project build & dependencies
└── src/
    ├── main/
    │   ├── java/com/team5/onlineshopping/
    │   │   ├── OnlineShoppingApplication.java      # Application entry point
    │   │   ├── audit/
    │   │   │   ├── entity/StatusHistory.java       # Audit status transition tracking
    │   │   │   └── repository/StatusHistoryRepository.java
    │   │   ├── auth/
    │   │   │   ├── controller/AuthenticationController.java # /api/auth/register, /api/auth/login
    │   │   │   ├── dto/                            # AuthenticationRequest, Response, etc.
    │   │   │   └── service/AuthenticationService.java
    │   │   ├── common/
    │   │   │   ├── dto/ApiResponse.java            # Standardized API response wrapper
    │   │   │   ├── enums/                          # Canonical Version 2.0 lifecycle enums
    │   │   │   │   ├── DeliveryStatus.java
    │   │   │   │   ├── DiscountType.java
    │   │   │   │   ├── OrderItemStatus.java
    │   │   │   │   ├── OrderStatus.java
    │   │   │   │   ├── PaymentMethod.java
    │   │   │   │   ├── PaymentStatus.java
    │   │   │   │   ├── RefundStatus.java
    │   │   │   │   ├── ReservationStatus.java
    │   │   │   │   ├── ReturnStatus.java
    │   │   │   │   └── Role.java
    │   │   │   └── exception/                      # GlobalExceptionHandler, Custom exceptions
    │   │   ├── config/SecurityConfig.java          # SecurityFilterChain and CORS setup
    │   │   ├── coupon/
    │   │   │   ├── entity/                         # Coupon, CouponUsage
    │   │   │   └── repository/                     # CouponRepository, CouponUsageRepository
    │   │   ├── health/HealthController.java        # Health check endpoint (/api/health)
    │   │   ├── inventory/
    │   │   │   ├── entity/                         # StockReservation, StockReservationItem
    │   │   │   └── repository/                     # StockReservationRepository
    │   │   ├── notification/
    │   │   │   ├── entity/Notification.java
    │   │   │   └── repository/NotificationRepository.java
    │   │   ├── order/
    │   │   │   ├── entity/                         # Order, OrderItem
    │   │   │   └── repository/                     # OrderRepository, OrderItemRepository
    │   │   ├── postpurchase/
    │   │   │   ├── entity/                         # ReturnRequest, RefundTransaction
    │   │   │   └── repository/                     # ReturnRequestRepository, RefundTransactionRepository
    │   │   ├── product/
    │   │   │   ├── controller/ProductController.java # /api/products catalog & review endpoints
    │   │   │   ├── dto/                            # Product create/update/review DTOs
    │   │   │   ├── entity/                         # Product, ProductReview
    │   │   │   ├── repository/                     # ProductRepository, ProductReviewRepository
    │   │   │   └── service/ProductService.java
    │   │   ├── security/
    │   │   │   ├── CustomUserDetails.java          # Spring Security UserDetails adapter
    │   │   │   ├── CustomUserDetailsService.java
    │   │   │   ├── JwtAuthenticationFilter.java    # Per-request JWT Bearer token validator
    │   │   │   └── JwtService.java                 # JWT generation, extraction & startup key validation
    │   │   ├── shipment/
    │   │   │   ├── entity/                         # Shipment, ShipmentItem
    │   │   │   └── repository/                     # ShipmentRepository, ShipmentItemRepository
    │   │   └── user/
    │   │       ├── entity/User.java
    │   │       └── repository/UserRepository.java
    │   └── resources/
    │       └── application.yml                     # Datasource, JPA & Security config
    └── test/
        ├── java/com/team5/onlineshopping/
        │   ├── auth/                               # Auth slice & unit tests
        │   ├── common/                             # Enum contract & ApiResponse tests
        │   ├── health/                             # Health endpoint tests
        │   ├── product/                            # Product catalog controller & service tests
        │   └── security/                           # JwtService & UserDetails unit tests
        └── resources/
            └── application.yml                     # Test profile overrides
```

---

## Prerequisites

- **Java Development Kit (JDK):** Version 21 LTS (`JAVA_HOME` configured). Verified with Eclipse Adoptium / Temurin / Oracle OpenJDK 21.
- **Database:** MySQL Server 8.0+
- **Build Tool:** Maven Wrapper (`mvnw` / `mvnw.cmd` included in repository)

---

## Configuration & Environment Variables

The application is configured via environment variables with safe defaults for development. Note that **`JWT_SECRET` is required** and must be at least 256 bits (32 bytes) for secure token generation:

| Variable | Description | Default Value | Required in Production |
| :--- | :--- | :--- | :---: |
| `SERVER_PORT` | HTTP port for the backend server | `8080` | No |
| `DB_HOST` | MySQL hostname / IP | `localhost` | No |
| `DB_PORT` | MySQL port | `3306` | No |
| `DB_NAME` | MySQL database name | `online_shopping_db` | No |
| `DB_USERNAME` | Database username | `root` | Yes |
| `DB_PASSWORD` | Database password | *(empty)* | Yes |
| `DB_URL` | Full JDBC connection string | *Constructed from DB_HOST, DB_PORT, DB_NAME* | No |
| `JWT_SECRET` | 256-bit+ HMAC-SHA256 Secret (Hex or Base64 or plain string $\ge$ 32 bytes) | *(none - validated on startup)* | **Yes** |
| `JWT_EXPIRATION_MS`| JWT validity duration in milliseconds | `86400000` (24 hours) | No |

---

## Running the Backend

From the repository root, change to the `backend/` directory:

```bash
cd backend
```

### 1. Run Unit & Slice Tests
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### 2. Build and Package JAR
```bash
# Windows
.\mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```

### 3. Run the Application
Ensure MySQL 8 service is running and set your `JWT_SECRET`:
```bash
# Windows (PowerShell)
$env:JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
$env:DB_PASSWORD="your_mysql_password"
.\mvnw.cmd spring-boot:run

# Linux / macOS
export JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
export DB_PASSWORD="your_mysql_password"
./mvnw spring-boot:run
```

---

## Feature Implementation Status

### Completed Core Capabilities (Member 1 — Owais)
- **Shared Architecture & Contract:** Full entity mapping and database schema for `User`, `Product`, `ProductReview`, `Order`, `OrderItem`, `StockReservation`, `StockReservationItem`, `Coupon`, `CouponUsage`, `Shipment`, `ShipmentItem`, `ReturnRequest`, `RefundTransaction`, `Notification`, and `StatusHistory`.
- **Security & Identity:** Spring Security 6 stateless filter chain, BCrypt password hashing, JWT creation & claims extraction, startup secret strength validation ($\ge 256$ bits).
- **Authentication Subsystem:** `/api/auth/register` and `/api/auth/login` with custom UserDetails and role assignments.
- **Product Catalog Subsystem:** Product browsing, creation, category filtering, seller-scoped updates, and customer product reviews calculation (`/api/products`).
- **Health Check Endpoint:** Public `/api/health` monitoring service status and timestamp.

### Pending Operations Capabilities (Member 2 — Rashika)
The following modules are owned by Rashika (Member 2) and will be implemented on the operations branch:
- Seller Dashboard & Inventory Management UI
- Coupon Creation & Campaign Management Interface
- Fulfilment & Delivery Partner Dashboard (OTP delivery verification)
- Seller Return Inspection & Restock Decision Workflow
