# online-shopping-system
A web-based Online Shopping System that allows customers to browse products, manage carts, apply discounts, make secure payments, and track orders, with seller and delivery management features.

## Architecture & Technology Stack

- **Backend:** Java 21 LTS, Spring Boot 3.4.2
- **Build System:** Maven 3.9.x with Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Database:** MySQL 8.x
- **Security:** Spring Security (stateless REST configuration)

## Backend Project Structure

The Spring Boot backend service is located in the `backend/` directory:

```
backend/
├── .mvn/wrapper/              # Maven Wrapper distribution and jar
├── mvnw / mvnw.cmd            # Maven Wrapper executables
├── pom.xml                    # Project build & dependencies
└── src/
    ├── main/
    │   ├── java/com/team5/onlineshopping/
    │   │   ├── OnlineShoppingApplication.java   # Application entry point
    │   │   ├── config/SecurityConfig.java       # Security filter configuration
    │   │   └── health/HealthController.java     # Health check endpoint (/api/health)
    │   └── resources/
    │       └── application.yml                  # Application & datasource configuration
    └── test/
        └── java/com/team5/onlineshopping/
            └── health/HealthControllerTest.java # WebMvc slice test
```

## Prerequisites

- **Java Development Kit (JDK):** Version 21 LTS (`JAVA_HOME` configured)
- **Database:** MySQL Server 8.0+

## Configuration & Environment Variables

Database and server properties can be configured via environment variables:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `SERVER_PORT` | HTTP port for the backend server | `8080` |
| `DB_HOST` | MySQL hostname / IP | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | MySQL database name | `online_shopping_db` |
| `DB_USERNAME` | Database user | `root` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `DB_URL` | Full JDBC connection string | *Constructed from DB_HOST, DB_PORT, DB_NAME* |

## Running the Backend

From the repository root, change to the `backend/` directory:

```bash
cd backend
```

### Run Tests
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### Build the Application
```bash
# Windows
.\mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```

### Run the Application
Ensure MySQL 8 service is running, then execute:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### Health Check Endpoint
Once running, check system health at:
```
GET http://localhost:8080/api/health
```
Response format:
```json
{
  "status": "UP",
  "timestamp": "2026-11-07T18:15:00.000Z",
  "service": "online-shopping-system"
}
```
