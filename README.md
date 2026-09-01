# Certificate Service — Production Backend

A production-grade, highly reliable Java Spring Boot 3 backend service for issuing, managing, looking up, and revoking tamper-proof educational certificates with immutable historical snapshots, temporal design assignment history, and database-enforced concurrency control.

---

## 1. Project Overview

The **Certificate Service** manages certificate lifecycle operations for educational organizations. It provides capabilities to:
- Define certificate templates (**Designs**) and disable obsolete designs without breaking past certificates.
- Manage educational **Programmes**, support renaming and deprecation without corrupting historical records.
- Track **Temporal Programme $\rightarrow$ Design Assignments**, allowing programmes to evolve their visual layout over time while accurately auditing what design was active on any specific date.
- Issue **Immutable Certificates** to candidates, capturing an exact point-in-time snapshot of the programme title and design template directly onto the certificate record.
- Publicly verify certificates by ID even after programmes or designs have been renamed, replaced, or disabled.
- Enforce the **One Live Certificate Per Person Per Programme** invariant under high-concurrency race conditions using PostgreSQL partial unique indexes and atomic transactional locking.
- Support certificate **Revocation / Cancellation** with reason tracking, auditing, and re-issuance support.

---

## 2. Architecture & Design

The service adheres to standard Spring Boot layered architecture:

```
                  ┌─────────────────────────────────────────┐
                  │          HTTP Clients / Consumers        │
                  └────────────────────┬────────────────────┘
                                       │ JSON / REST
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │           REST Controllers              │
                  │  - DesignController                     │
                  │  - ProgrammeController                  │
                  │  - CertificateController                │
                  │  - GlobalExceptionHandler               │
                  └────────────────────┬────────────────────┘
                                       │ DTOs (Request / Response)
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │             Service Layer               │
                  │  - DesignService                        │
                  │  - ProgrammeService                     │
                  │  - ProgrammeDesignService (Temporal)    │
                  │  - CertificateService (Snapshot+Lock)   │
                  └────────────────────┬────────────────────┘
                                       │ Domain Entities
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │       Spring Data JPA Repositories       │
                  │  - DesignRepository                     │
                  │  - ProgrammeRepository                  │
                  │  - ProgrammeDesignAssignmentRepository  │
                  │  - CertificateRepository                │
                  └────────────────────┬────────────────────┘
                                       │ SQL Queries / Migrations
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │          PostgreSQL Database            │
                  │  - Partial Unique Index (status='ACTIVE')│
                  │  - Flyway Migrations                    │
                  └─────────────────────────────────────────┘
```

### Key Architectural Tenets:
1. **Separation of Concerns**: Controllers handle HTTP formatting and validation; Services encapsulate business rules, locks, and snapshotting; Repositories manage data access.
2. **DTO Layering**: JPA entities are never exposed across HTTP boundaries. All requests and responses use strongly-typed Java DTOs.
3. **Defense-in-Depth Concurrency**: In-memory mutex serialization per `(programme_id, person_email)` combined with database storage engine partial unique constraints (`uq_live_certificate_person_programme`).
4. **Immutability First**: Issued certificates are read-only historical snapshots. They do not join mutable programme/design tables for historical reconstruction.

---

## 3. Technology Stack

* **Language**: Java 21 LTS
* **Framework**: Spring Boot 3.3.3
* **Persistence**: Spring Data JPA / Hibernate 6.x
* **Database**: PostgreSQL 16 (production/docker), H2 (in-memory test profile)
* **Database Migrations**: Flyway
* **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
* **API Documentation**: SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`)
* **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring MVC Test
* **Containerization**: Docker Compose

---

## 4. Database Schema & Flyway Migrations

The database is version-controlled using Flyway migrations located in `src/main/resources/db/migration/postgresql/`.

```sql
-- Designs Table
CREATE TABLE designs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Programmes Table
CREATE TABLE programmes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Programme Design Assignments (Temporal Timeline)
CREATE TABLE programme_design_assignments (
    id UUID PRIMARY KEY,
    programme_id UUID NOT NULL REFERENCES programmes(id),
    design_id UUID NOT NULL REFERENCES designs(id),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_pda_programme_effective ON programme_design_assignments (programme_id, effective_from DESC);

-- Certificates Table (Immutable Snapshots + Audit & Cancellation)
CREATE TABLE certificates (
    id UUID PRIMARY KEY,
    person_name VARCHAR(255) NOT NULL,
    person_email VARCHAR(255) NOT NULL,
    programme_id UUID NOT NULL REFERENCES programmes(id),
    programme_name_snapshot VARCHAR(255) NOT NULL,
    design_id UUID NOT NULL REFERENCES designs(id),
    design_name_snapshot VARCHAR(255) NOT NULL,
    design_content_snapshot TEXT NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_cert_programme FOREIGN KEY (programme_id) REFERENCES programmes(id),
    CONSTRAINT fk_cert_design FOREIGN KEY (design_id) REFERENCES designs(id)
);

CREATE INDEX idx_certificates_person_email ON certificates (LOWER(person_email), issued_at DESC);
CREATE INDEX idx_certificates_programme_id ON certificates (programme_id);

-- PostgreSQL Partial Unique Index: Exactly one LIVE (ACTIVE) certificate per person per programme
CREATE UNIQUE INDEX uq_live_certificate_person_programme 
ON certificates (programme_id, LOWER(person_email)) 
WHERE status = 'ACTIVE';
```

---

## 5. Certificate Immutability Strategy

A issued certificate is a legally binding, historical document. 

### How Immutability is Enforced:
1. **Snapshot Columns on `certificates`**: When `CertificateService.issueCertificate()` is invoked, the service reads the active `programme.name`, `design.name`, and `design.content` at the certificate's issuance instant.
2. **Denormalized Persistence**: The values are persisted directly into `programme_name_snapshot`, `design_name_snapshot`, and `design_content_snapshot`.
3. **Independent Verification**: When looking up a certificate via `GET /api/certificates/{certificateId}`, the service reads directly from the certificate snapshot columns. It **never** queries the live `programmes` or `designs` table to reconstruct certificate text.
4. **Resilience to Upstream Mutations**:
   - Renaming a programme (`PATCH /api/programmes/{id}/name`) does not change issued certificates.
   - Changing a programme's active design does not change issued certificates.
   - Disabling a programme (`POST /api/programmes/{id}/disable`) does not invalidate or alter issued certificates.
   - Disabling a design (`POST /api/designs/{id}/disable`) does not invalidate or alter issued certificates.

---

## 6. Programme $\rightarrow$ Design Temporal History Strategy

A programme uses one design at a time, but designs evolve over time.

### How History is Maintained:
* Rather than storing a mutable `design_id` foreign key directly on `programmes`, every assignment is an immutable entry in `programme_design_assignments` with an `effective_from` timestamp.
* **Current Active Design**: `GET /api/programmes/{id}/designs/current` queries:
  ```sql
  SELECT * FROM programme_design_assignments 
  WHERE programme_id = :id AND effective_from <= NOW() 
  ORDER BY effective_from DESC LIMIT 1;
  ```
* **Point-in-Time Audit**: `GET /api/programmes/{id}/designs/historical?at=2026-03-05T00:00:00Z` executes:
  ```sql
  SELECT * FROM programme_design_assignments 
  WHERE programme_id = :id AND effective_from <= :at 
  ORDER BY effective_from DESC LIMIT 1;
  ```
* **Example Scenario**:
  - March 1: Programme assigned "Gold Border v1" (`effective_from = 2026-03-01T00:00:00Z`).
  - March 12: Programme assigned "Gold Border v2" (`effective_from = 2026-03-12T00:00:00Z`).
  - Query at March 5 returns "Gold Border v1".
  - Query at March 15 returns "Gold Border v2".

---

## 7. Concurrency & One Live Certificate Strategy

### The Invariant
A person can have at most **ONE active (live) certificate** for a given programme at any point in time. If a certificate is cancelled, a new live certificate can be issued.

### The Race Condition Challenge
Two simultaneous issuance requests ($R_1$ and $R_2$) for Priya in "Advanced SQL" may arrive simultaneously on separate threads or cluster nodes. A naive "check `existsBy...` then `save`" approach suffers from Time-of-Check to Time-of-Use (TOCTOU) race conditions, creating duplicate live certificates.

### The Multi-Layered Defense Solution:
1. **Storage Engine ACID Enactment (PostgreSQL)**:
   A partial unique index is defined in PostgreSQL:
   ```sql
   CREATE UNIQUE INDEX uq_live_certificate_person_programme 
   ON certificates (programme_id, LOWER(person_email)) 
   WHERE status = 'ACTIVE';
   ```
   If two requests attempt concurrent insertion, PostgreSQL serializes index insertion and rejects the second with a unique constraint violation (`DataIntegrityViolationException`), which is caught and mapped to an HTTP `409 Conflict`.

2. **JVM Synchronization & Transactional Isolation**:
   Within the application layer, `CertificateService` synchronizes on an interned string token `("CERT_LOCK:" + programmeId + ":" + normalizedEmail).intern()` using `TransactionTemplate`:
   ```java
   String lockKey = ("CERT_LOCK:" + programme.getId() + ":" + normalizedEmail).intern();
   synchronized (lockKey) {
       return transactionTemplate.execute(txStatus -> {
           if (certificateRepository.existsByProgrammeIdAndPersonEmailIgnoreCaseAndStatus(
                   programme.getId(), normalizedEmail, CertificateStatus.ACTIVE)) {
               throw new DuplicateCertificateException(...);
           }
           // Save and flush inside atomic transaction
           return CertificateResponse.fromEntity(certificateRepository.saveAndFlush(certificate));
       });
   }
   ```
3. **Cancellation Behavior**:
   When a certificate is cancelled, its status is updated to `CANCELLED`. Because `status != 'ACTIVE'`, it drops out of the partial unique index, allowing a new certificate to be legitimately issued for the candidate without collision.

---

## 8. REST API Documentation

### Base URL: `/api`

| Category | Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| **Designs** | `POST` | `/api/designs` | Create a certificate design template | `201`, `400` |
| | `GET` | `/api/designs` | List all designs (optional `?status=ACTIVE`) | `200` |
| | `GET` | `/api/designs/{id}` | Get design details by ID | `200`, `404` |
| | `POST` | `/api/designs/{id}/disable` | Disable a design | `200`, `404` |
| **Programmes** | `POST` | `/api/programmes` | Create a new programme | `201`, `400` |
| | `GET` | `/api/programmes` | List programmes (optional `?status=ACTIVE`) | `200` |
| | `GET` | `/api/programmes/{id}` | Get programme by ID | `200`, `404` |
| | `PATCH` / `PUT` | `/api/programmes/{id}/name` | Rename a programme | `200`, `400`, `404` |
| | `POST` | `/api/programmes/{id}/disable` | Disable a programme | `200`, `404` |
| **Design History** | `POST` | `/api/programmes/{id}/designs` | Assign or update design for a programme | `201`, `400`, `404` |
| | `GET` | `/api/programmes/{id}/designs/current` | Get current active design for programme | `200`, `404` |
| | `GET` | `/api/programmes/{id}/designs/historical?at=...` | Get design active at a historical timestamp | `200`, `404` |
| | `GET` | `/api/programmes/{id}/designs/history` | Get complete chronological assignment history | `200`, `404` |
| **Certificates** | `POST` | `/api/certificates` | Issue a new certificate with snapshot | `201`, `400`, `404`, `409` |
| | `GET` | `/api/certificates/{certificateId}` | Public lookup of certificate snapshot | `200`, `404` |
| | `GET` | `/api/certificates?personEmail=...` | List certificates for a person (paginated) | `200`, `400` |
| | `POST` | `/api/certificates/{certificateId}/cancel` | Revoke / cancel an issued certificate | `200`, `400`, `404` |

---

## 9. Example Requests & Responses

### 1. Create a Design
`POST /api/designs`
```json
{
  "name": "Gold Border v1",
  "content": "<svg class=\"cert\"><border color=\"gold\"/><h1>Certificate of Excellence</h1><p>{{personName}} completed {{programmeName}}</p></svg>"
}
```
**Response (`201 Created`):**
```json
{
  "id": "7b8d4e92-3c1a-4f5d-9b6e-1a2b3c4d5e6f",
  "name": "Gold Border v1",
  "content": "<svg class=\"cert\"><border color=\"gold\"/><h1>Certificate of Excellence</h1><p>{{personName}} completed {{programmeName}}</p></svg>",
  "status": "ACTIVE",
  "createdAt": "2026-03-01T08:00:00Z",
  "updatedAt": "2026-03-01T08:00:00Z"
}
```

---

### 2. Create and Assign Design to Programme
`POST /api/programmes`
```json
{
  "name": "Advanced SQL",
  "description": "Deep dive into query optimization, execution plans, and indexing."
}
```
**Response (`201 Created`):**
```json
{
  "id": "e3a89012-789a-4bc1-9123-abcdef012345",
  "name": "Advanced SQL",
  "description": "Deep dive into query optimization, execution plans, and indexing.",
  "status": "ACTIVE",
  "createdAt": "2026-03-01T08:15:00Z",
  "updatedAt": "2026-03-01T08:15:00Z"
}
```

`POST /api/programmes/e3a89012-789a-4bc1-9123-abcdef012345/designs`
```json
{
  "designId": "7b8d4e92-3c1a-4f5d-9b6e-1a2b3c4d5e6f",
  "effectiveFrom": "2026-03-01T00:00:00Z"
}
```
**Response (`201 Created`):**
```json
{
  "id": "f5123456-789a-4bc1-9123-123456789abc",
  "programmeId": "e3a89012-789a-4bc1-9123-abcdef012345",
  "programmeName": "Advanced SQL",
  "designId": "7b8d4e92-3c1a-4f5d-9b6e-1a2b3c4d5e6f",
  "designName": "Gold Border v1",
  "designContent": "<svg class=\"cert\"><border color=\"gold\"/><h1>Certificate of Excellence</h1><p>{{personName}} completed {{programmeName}}</p></svg>",
  "effectiveFrom": "2026-03-01T00:00:00Z",
  "createdAt": "2026-03-01T08:30:00Z"
}
```

---

### 3. Issue an Immutable Certificate
`POST /api/certificates`
```json
{
  "programmeId": "e3a89012-789a-4bc1-9123-abcdef012345",
  "personName": "Priya Sharma",
  "personEmail": "priya.sharma@example.com"
}
```
**Response (`201 Created`):**
```json
{
  "id": "c9a01234-5678-9abc-def0-123456789abc",
  "personName": "Priya Sharma",
  "personEmail": "priya.sharma@example.com",
  "programmeId": "e3a89012-789a-4bc1-9123-abcdef012345",
  "programmeNameSnapshot": "Advanced SQL",
  "designId": "7b8d4e92-3c1a-4f5d-9b6e-1a2b3c4d5e6f",
  "designNameSnapshot": "Gold Border v1",
  "designContentSnapshot": "<svg class=\"cert\"><border color=\"gold\"/><h1>Certificate of Excellence</h1><p>{{personName}} completed {{programmeName}}</p></svg>",
  "issuedAt": "2026-03-05T10:00:00Z",
  "status": "ACTIVE",
  "cancellationReason": null,
  "cancelledAt": null
}
```

---

### 4. Duplicate Issuance Conflict (409)
`POST /api/certificates` (attempting second active certificate for Priya in Advanced SQL)
```json
{
  "programmeId": "e3a89012-789a-4bc1-9123-abcdef012345",
  "personName": "Priya Sharma",
  "personEmail": "priya.sharma@example.com"
}
```
**Response (`409 Conflict`):**
```json
{
  "timestamp": "2026-03-05T10:00:05Z",
  "status": 409,
  "error": "Conflict",
  "message": "A live certificate already exists for person 'Priya Sharma' (priya.sharma@example.com) in programme 'Advanced SQL'",
  "path": "/api/certificates"
}
```

---

### 5. Cancel a Certificate
`POST /api/certificates/c9a01234-5678-9abc-def0-123456789abc/cancel`
```json
{
  "reason": "Administrative revocation: updated identification provided."
}
```
**Response (`200 OK`):**
```json
{
  "id": "c9a01234-5678-9abc-def0-123456789abc",
  "personName": "Priya Sharma",
  "personEmail": "priya.sharma@example.com",
  "programmeId": "e3a89012-789a-4bc1-9123-abcdef012345",
  "programmeNameSnapshot": "Advanced SQL",
  "designId": "7b8d4e92-3c1a-4f5d-9b6e-1a2b3c4d5e6f",
  "designNameSnapshot": "Gold Border v1",
  "designContentSnapshot": "<svg class=\"cert\"><border color=\"gold\"/><h1>Certificate of Excellence</h1><p>{{personName}} completed {{programmeName}}</p></svg>",
  "issuedAt": "2026-03-05T10:00:00Z",
  "status": "CANCELLED",
  "cancellationReason": "Administrative revocation: updated identification provided.",
  "cancelledAt": "2026-03-06T14:20:00Z"
}
```

---

### 6. List Person's Certificates with Pagination
`GET /api/certificates?personEmail=priya.sharma@example.com&page=0&size=10`

**Response (`200 OK`):**
```json
{
  "content": [
    {
      "id": "c9a01234-5678-9abc-def0-123456789abc",
      "personName": "Priya Sharma",
      "personEmail": "priya.sharma@example.com",
      "programmeId": "e3a89012-789a-4bc1-9123-abcdef012345",
      "programmeNameSnapshot": "Advanced SQL",
      "designId": "7b8d4e92-3c1a-4f5d-9b6e-1a2b3c4d5e6f",
      "designNameSnapshot": "Gold Border v1",
      "designContentSnapshot": "<svg class=\"cert\">...</svg>",
      "issuedAt": "2026-03-05T10:00:00Z",
      "status": "CANCELLED",
      "cancellationReason": "Administrative revocation: updated identification provided.",
      "cancelledAt": "2026-03-06T14:20:00Z"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "isFirst": true,
  "isLast": true,
  "hasNext": false,
  "hasPrevious": false
}
```

---

## 10. How to Run PostgreSQL with Docker Compose

1. Ensure Docker is running.
2. Start the PostgreSQL container:
   ```bash
   docker compose up -d
   ```
3. PostgreSQL will start on port `5432` with database `certificatedb`, username `certuser`, password `certpass`.
4. To stop the database:
   ```bash
   docker compose down
   ```

---

## 11. How to Run the Spring Boot Application

### Using Maven:
```bash
mvn spring-boot:run
```

### Environment Variables:
The application can be configured dynamically without changing code:

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/certificatedb` | Database JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `certuser` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `certpass` | Database password |
| `SEED_DATA_ENABLED` | `true` | Load seed test data on startup |
| `PORT` | `8080` | HTTP Server Port |

### Interactive Swagger UI & OpenAPI Docs:
Once running, navigate to:
* **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI 3 JSON Specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 12. How to Run Tests

Run the complete test suite (all unit and integration tests):
```bash
mvn clean test
```

### Test Suite Highlights:
* **`CertificateConcurrencyIntegrationTest`**: Uses a 10-thread parallel executor and `CountDownLatch` barrier to simulate simultaneous requests for the exact same person and programme, proving only 1 certificate is created and 9 receive HTTP 409.
* **`CertificateImmutabilityIntegrationTest`**: Issues a certificate, then renames programme, changes design, disables programme, and disables design. Confirms public certificate lookup returns the exact original snapshot.
* **`ProgrammeDesignHistoryIntegrationTest`**: Verifies temporal design timeline lookup at March 1, March 5, March 12, and March 15.
* **`EndToEndCertificateFlowIntegrationTest`**: Tests full lifecycle over HTTP MockMvc.

---

## 13. How to Load Seed / Test Data

Seed data initialization is pre-configured and enabled by default (`SEED_DATA_ENABLED=true`).

When the application boots against an empty database, `DataInitializer` automatically seeds:
1. **Designs**:
   - `Gold Border v1` (Active)
   - `Gold Border v2` (Active)
   - `Modern Minimalist` (Active)
   - `Classic Vintage` (Disabled)
2. **Programmes**:
   - `Advanced SQL` (Active)
   - `Distributed Systems` (Active)
   - `Cloud Native Architecture` (Active)
3. **Temporal Assignments**:
   - `Advanced SQL` $\rightarrow$ Gold Border v1 (effective 30 days ago)
   - `Advanced SQL` $\rightarrow$ Gold Border v2 (effective 18 days ago)
   - `Distributed Systems` $\rightarrow$ Modern Minimalist (effective 30 days ago)
   - `Cloud Native Architecture` $\rightarrow$ Gold Border v2 (effective 30 days ago)
4. **Certificates**:
   - `Priya Sharma` in `Advanced SQL` (Issued 25 days ago with Gold Border v1 snapshot)
   - `Priya Sharma` in `Distributed Systems` (Active)
   - `John Doe` in `Advanced SQL` (Cancelled with recorded audit reason)
   - `Alice Smith` in `Cloud Native Architecture` (Active)

To disable seed data generation, set `SEED_DATA_ENABLED=false`.

---

## 14. Assumptions & Design Decisions

1. **Email as Person Identifier**: A candidate's email address (`person_email`) is treated as their unique identifier in combination with `programme_id`. Email addresses are normalized to lowercase.
2. **Post-Cancellation Re-Issuance**: As permitted by Requirement #6, if a certificate is `CANCELLED`, a new certificate CAN be issued for that candidate in that programme. The uniqueness constraint strictly guards `status = 'ACTIVE'`.
3. **Soft-Disable Policy**: Designs and Programmes are soft-disabled rather than deleted (`status = DISABLED`), preserving referential integrity and historical auditability.
4. **Public Lookup Transparency**: Looking up a cancelled certificate via `GET /api/certificates/{id}` returns the complete certificate record with `status: "CANCELLED"`, `cancellationReason`, and `cancelledAt` timestamp so verifiers know the certificate was officially revoked.
