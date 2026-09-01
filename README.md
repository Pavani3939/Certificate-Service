# 🎓 Certificate Service Platform

A production-quality, enterprise-grade backend service built with **Java 21** and **Spring Boot 3.3.x** for issuing, looking up, auditing, and revoking tamper-proof educational certificates with immutable point-in-time snapshots, temporal design assignment history, and database-enforced concurrency control.

🚀 **[Live Demo & Interactive Swagger UI](https://certificate-service-1.onrender.com/swagger-ui/index.html)**

---

## 🚀 Live Interactive API Documentation
* 🌐 **Live Cloud Swagger UI**: [https://certificate-service-1.onrender.com/swagger-ui/index.html](https://certificate-service-1.onrender.com/swagger-ui/index.html)
* 📄 **OpenAPI 3 JSON Specification**: [https://certificate-service-1.onrender.com/v3/api-docs](https://certificate-service-1.onrender.com/v3/api-docs)
* 💻 **Local Development Console**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🌟 Key Features

* **🎨 Template & Design Management**: Create and manage visual certificate templates. Supports soft-disabling obsolete designs without breaking past certificates.
* **📚 Educational Programme Timelines**: Evolve programme names and visual templates over time with point-in-time historical auditing (e.g. resolve which design was active on March 5 vs March 15).
* **📜 Immutable Certificate Snapshots**: Issued certificates permanently capture exact point-in-time snapshots of the programme title and design template. Subsequent programme renames, design updates, or entity disabling have zero effect on issued certificates.
* **🛡️ Database-Enforced Concurrency Guard**: Enforces the strict rule of **One Live Certificate Per Person Per Programme**. A PostgreSQL partial unique index combined with JVM atomic locking prevents race conditions and duplicate creation (rejecting races with `HTTP 409 Conflict`).
* **🔍 Public Certificate Verification**: Anyone with a Certificate ID can publicly verify the certificate snapshot without authentication.
* **🚫 Revocation & Audit Tracking**: Certificates can be cancelled with an official reason and timestamp. Cancelled certificates remain publicly auditable and allow subsequent re-issuance.
* **🌱 Auto-Seeded Test Data**: Automatically populates realistic sample data (designs, programmes, historical assignments, and certificates) on application startup.

---

## 🛠️ Modern Tech Stack & Tooling

* **Backend Core**: Java 21 LTS, Spring Boot 3.3.3 (Spring Web, Spring Data JPA, Spring Validation, Spring Actuator).
* **Database & Persistence**: PostgreSQL 16 (production), H2 (in-memory standalone/test runtime), Hibernate 6.x.
* **Database Migrations**: Flyway schema versioning (`db/migration`).
* **API Documentation**: SpringDoc OpenAPI 3 / Swagger UI.
* **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring MVC MockMvc (**47/47 passing tests**).
* **Containerization**: Docker Compose (`docker-compose.yml`).

---

## 📂 Project Structure

```text
Certificate-Service/
├── pom.xml                                  # Maven dependencies & build plugins
├── docker-compose.yml                       # PostgreSQL 16 container definition
├── run-demo.bat                             # 1-Click interactive demo runner
├── demo.ps1                                 # Automated PowerShell test script
├── README.md                                # System documentation
└── src/
    ├── main/
    │   ├── java/com/certificateservice/
    │   │   ├── CertificateServiceApplication.java   # Spring Boot entry point
    │   │   ├── config/
    │   │   │   ├── OpenApiConfig.java               # Swagger OpenAPI setup
    │   │   │   └── DataInitializer.java            # Seed data loader
    │   │   ├── controller/                          # REST API endpoints
    │   │   │   ├── DesignController.java            # /api/designs
    │   │   │   ├── ProgrammeController.java         # /api/programmes
    │   │   │   └── CertificateController.java       # /api/certificates
    │   │   ├── dto/                                 # Request & Response DTOs
    │   │   ├── exception/                           # Global @RestControllerAdvice
    │   │   ├── model/                               # JPA Domain Entities
    │   │   ├── repository/                          # Spring Data JPA Repositories
    │   │   └── service/                             # Core business logic & snapshots
    │   └── resources/
    │       ├── application.yml                      # Production config (PostgreSQL)
    │       ├── application-local.yml                # Standalone standalone config (H2)
    │       └── db/migration/                        # Flyway migration scripts
    └── test/
        ├── java/com/certificateservice/             # 47 Unit & Integration test suites
        └── resources/
            └── application-test.yml                 # Test profile configuration
```

---

## 🚀 Setup & Launch Instructions

### Prerequisites
* **Java JDK 21+**
* **Apache Maven 3.9+** (or use `mvn`)
* *Optional*: **Docker Desktop** (for PostgreSQL)

---

### Option 1: Run in Standalone Local Mode (Easiest - No External DB Required)

1. Open your terminal in the project directory:
   ```cmd
   cd certificate-service
   ```
2. Run the application with the standalone local profile:
   ```cmd
   java -jar target\certificate-service-1.0.0.jar --spring.profiles.active=local
   ```
   *(Or using Maven: `mvn spring-boot:run -Dspring-boot.run.profiles=local`)*

3. Open **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)** in your browser!

---

### Option 2: Run with PostgreSQL (Docker Compose)

1. Start the PostgreSQL container:
   ```cmd
   docker compose up -d
   ```
2. Start the Spring Boot application:
   ```cmd
   mvn spring-boot:run
   ```

---

### Option 3: Run All 47 Automated Tests

```cmd
mvn clean test
```

---

## 🔑 Pre-Loaded Seed Data for Testing

When booted, the system automatically initializes sample data ready for instant testing:

| Resource | Sample Records | Status |
| :--- | :--- | :--- |
| **Designs** | `Gold Border v1`, `Gold Border v2`, `Modern Minimalist` | `ACTIVE` |
| | `Classic Vintage` | `DISABLED` |
| **Programmes** | `Advanced SQL`, `Distributed Systems`, `Cloud Native Architecture` | `ACTIVE` |
| **Certificates** | **Priya Sharma** (`priya.sharma@example.com`) in `Advanced SQL` | `ACTIVE` |
| | **Priya Sharma** (`priya.sharma@example.com`) in `Distributed Systems` | `ACTIVE` |
| | **John Doe** (`john.doe@example.com`) in `Advanced SQL` | `CANCELLED` |
| | **Alice Smith** (`alice.smith@example.com`) in `Cloud Native Architecture` | `ACTIVE` |

---

## 📜 REST API Endpoint Quick Reference

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/designs` | Create a new certificate design template | `201`, `400` |
| `GET` | `/api/designs` | List all designs (optional `?status=ACTIVE`) | `200` |
| `GET` | `/api/designs/{id}` | Get design details by ID | `200`, `404` |
| `POST` | `/api/designs/{id}/disable` | Disable an obsolete design | `200`, `404` |
| `POST` | `/api/programmes` | Create an educational programme | `201`, `400` |
| `GET` | `/api/programmes` | List programmes (optional `?status=ACTIVE`) | `200` |
| `PATCH` | `/api/programmes/{id}/name` | Rename a programme | `200`, `400`, `404` |
| `POST` | `/api/programmes/{id}/disable` | Disable a programme | `200`, `404` |
| `POST` | `/api/programmes/{id}/designs` | Assign or update design for a programme | `201`, `400`, `404` |
| `GET` | `/api/programmes/{id}/designs/current` | Get current active design for programme | `200`, `404` |
| `GET` | `/api/programmes/{id}/designs/historical` | Get design active at a historical date (`?at=...`) | `200`, `404` |
| `GET` | `/api/programmes/{id}/designs/history` | Get complete chronological assignment history | `200`, `404` |
| `POST` | `/api/certificates` | Issue a certificate with immutable point-in-time snapshot | `201`, `400`, `404`, `409` |
| `GET` | `/api/certificates/{certificateId}` | Public lookup of immutable certificate by ID | `200`, `404` |
| `GET` | `/api/certificates?personEmail=...` | List certificates belonging to a person (paginated) | `200`, `400` |
| `POST` | `/api/certificates/{certificateId}/cancel` | Cancel / revoke an issued certificate with a reason | `200`, `400`, `404` |

---

## 🛡️ Concurrency & Immutability Guarantees

* **Immutable Snapshots**: Certificate text is stored in independent snapshot columns (`programme_name_snapshot`, `design_name_snapshot`, `design_content_snapshot`). Renaming or disabling programmes/designs never mutates issued certificates.
* **PostgreSQL Partial Unique Constraint**:
  ```sql
  CREATE UNIQUE INDEX uq_live_certificate_person_programme 
  ON certificates (programme_id, LOWER(person_email)) 
  WHERE status = 'ACTIVE';
  ```
  Prevents duplicate live certificate issuance under high-concurrency race conditions.
