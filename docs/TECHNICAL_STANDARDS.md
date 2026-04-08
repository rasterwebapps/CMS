# 🏗️ Technical Standards & Architecture (2026 Edition)

> **College Management System (CMS)** — Definitive guide for frontend, backend, UI/UX, and security standards.

---

## 📋 Table of Contents

- [1. Frontend (Angular 21) Instructions](#1-frontend-angular-21-instructions)
- [2. Angular UI/UX & Design Standards (Material 3)](#2-angular-uiux--design-standards-material-3)
- [3. Backend (Java 21 & Spring Boot 3.x)](#3-backend-java-21--spring-boot-3x)
- [4. General Coding & Security Guidelines](#4-general-coding--security-guidelines)
- [5. Database Profiles](#5-database-profiles)
- [6. Testing & Code Coverage](#6-testing--code-coverage)
- [7. Manual Test Cases](#7-manual-test-cases)

---

## 1. Frontend (Angular 21) Instructions

### 1.1 Modern Framework Architecture

- Use **Angular 21** with **Standalone Components** as the default.
- **Omit NgModules** — all new components, directives, and pipes must be standalone.
- Register application-wide providers in `app.config.ts` using `provideRouter()`, `provideHttpClient()`, etc.

### 1.2 Reactivity

- Prioritize **Angular Signals** for state management and UI logic.
  - Use `signal()`, `computed()`, and `effect()` for reactive state within components and services.
- Use **RxJS** primarily for:
  - Asynchronous data streams (e.g., WebSocket connections).
  - API orchestration (e.g., `switchMap`, `combineLatest` for composing HTTP calls).
- Avoid mixing Signals and Observables unnecessarily; use `toSignal()` and `toObservable()` for bridging when needed.

### 1.3 Flow Control

- Utilize the **New Control Flow** syntax for templates:
  - `@if` / `@else` for conditional rendering.
  - `@for` with the required `track` expression for list rendering.
  - `@switch` / `@case` / `@default` for multi-branch logic.
- These replace `*ngIf`, `*ngFor`, and `ngSwitch` — do **not** use the legacy structural directives in new code.

### 1.4 Modular Layout — Folder-by-Feature

Organize the application using a **Folder-by-Feature** structure:

```
src/
├── app/
│   ├── core/             # Singleton services, guards, interceptors, global providers
│   │   ├── auth/
│   │   ├── interceptors/
│   │   └── guards/
│   ├── shared/           # Reusable UI components, pipes, directives
│   │   ├── components/
│   │   ├── directives/
│   │   └── pipes/
│   ├── features/         # Feature-specific folders
│   │   ├── student/
│   │   ├── faculty/
│   │   ├── finance/
│   │   ├── lab/
│   │   ├── library/
│   │   ├── hostel/
│   │   └── ...
│   ├── app.component.ts
│   ├── app.config.ts
│   └── app.routes.ts
└── assets/
```

- **Core** folder: Global providers, authentication services, HTTP interceptors, and route guards.
- **Shared** folder: Reusable UI components, directives, and pipes used across multiple features.
- **Features** folder: Each feature module is self-contained with its own components, services, models, and routes.

### 1.5 SSR & Hydration

- Enable **Server-Side Rendering (SSR)** with **Client Hydration** for optimized SEO and performance.
- Use Angular's `provideClientHydration()` in `app.config.ts`.
- Ensure all components are SSR-compatible:
  - Avoid direct DOM manipulation; use `Renderer2` or Angular APIs.
  - Guard browser-only APIs (e.g., `window`, `localStorage`) with `isPlatformBrowser()` checks.

### 1.6 Testing

- **No frontend unit tests are required** for this project.
- Frontend testing tooling (Vitest) is available but not enforced.
- Use **`provideHttpClientTesting`** (not the legacy `HttpClientTestingModule`) if tests are written in the future.
- Favor **Component Harnesses** (`HarnessLoader`) for testing Angular Material components if tests are added.

---

## 2. Angular UI/UX & Design Standards (Material 3)

### 2.1 Material 3 (M3)

- Use **Angular Material 21** utilizing **Material 3** design principles (the latest design evolution from Google).
- All new UI components must follow M3 design guidelines for shape, color, and typography.
- Prefer Angular Material components over custom implementations for consistency and accessibility.

### 2.2 Dynamic Theming

- Implement a **CSS variable-based theme** using the `mat.theme()` Sass mixins.
- Support **light and dark mode** with a user-toggleable theme switch.
- Define theme palettes in a centralized `_theme.scss` file:

```scss
@use '@angular/material' as mat;

$light-theme: mat.theme(
  $color: (
    primary: mat.$azure-palette,
    tertiary: mat.$blue-palette,
    theme-type: light,
  ),
  $typography: Roboto,
  $density: 0
);

$dark-theme: mat.theme(
  $color: (
    primary: mat.$azure-palette,
    tertiary: mat.$blue-palette,
    theme-type: dark,
  ),
  $typography: Roboto,
  $density: 0
);
```

### 2.3 Component Tokens

- Use **Design Tokens** to override Material component styles.
- **Do not** use deep selectors (`::ng-deep`) or `!important` to override Material styles.
- Example — customizing a button via tokens:

```scss
@use '@angular/material' as mat;

html {
  @include mat.button-overrides(
    (
      filled-container-color: #e8def8,
      filled-label-text-color: #1d1b20,
    )
  );
}
```

### 2.4 High-Density Accounting UI

- Apply **Material Density** settings to data tables and form fields to ensure high information visibility for financial records.
- Use density level `-2` or `-3` for compact data tables in accounting and finance modules:

```scss
$dense-theme: mat.theme(
  $color: (
    primary: mat.$azure-palette,
    theme-type: light,
  ),
  $density: -2
);

.finance-table {
  @include mat.table-theme($dense-theme);
}
```

- Ensure compact layouts remain accessible — maintain minimum touch target sizes of 44×44px for interactive elements.

---

## 3. Backend (Java 21 & Spring Boot 3.x)

### 3.1 Virtual Threads (Project Loom)

- Use **Virtual Threads** (`Executors.newVirtualThreadPerTaskExecutor()`) to handle high-concurrency accounting transactions with minimal overhead.
- Configure Spring Boot to use virtual threads for request handling:

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true
```

- Virtual threads eliminate the need for reactive programming (e.g., WebFlux) for most I/O-bound operations — prefer the simpler imperative model.

### 3.2 Modern Java Syntax

Leverage modern Java 21 features throughout the codebase:

- **Records** for DTOs:

```java
public record StudentDTO(
    Long id,
    @NotBlank String name,
    @Email String email,
    @NotNull LocalDate enrollmentDate
) {}
```

- **Pattern Matching for Switch**:

```java
return switch (transaction) {
    case FeePayment fp    -> processFeePayment(fp);
    case Refund r         -> processRefund(r);
    case Scholarship s    -> processScholarship(s);
    default               -> throw new UnsupportedTransactionException(transaction);
};
```

- **Sequenced Collections**: Use `SequencedCollection`, `SequencedSet`, and `SequencedMap` interfaces for ordered data access (`getFirst()`, `getLast()`, `reversed()`).

### 3.3 Structured Concurrency

- Use **Structured Concurrency** for parallel API calls (e.g., fetching balance sheets and audit logs simultaneously):

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<BalanceSheet> balanceSheet = scope.fork(() -> fetchBalanceSheet(accountId));
    Subtask<List<AuditLog>> auditLogs = scope.fork(() -> fetchAuditLogs(accountId));

    scope.join().throwIfFailed();

    return new AccountSummary(balanceSheet.get(), auditLogs.get());
}
```

- Structured concurrency ensures all subtasks are properly managed — if one fails, sibling tasks are cancelled automatically.

### 3.4 Data Integrity

- Use **`BigDecimal`** for **all** currency calculations to prevent floating-point errors.
- **Never** use `float` or `double` for monetary values.
- Use `BigDecimal.ROUND_HALF_UP` (or `RoundingMode.HALF_UP`) for rounding:

```java
BigDecimal totalFee = baseFee.add(labFee).setScale(2, RoundingMode.HALF_UP);
```

- Store monetary values in the database as `DECIMAL` / `NUMERIC` types with appropriate precision (e.g., `DECIMAL(15,2)`).

---

## 4. General Coding & Security Guidelines

### 4.1 Clean Code

Follow consistent **Naming Conventions** across the entire codebase:

| Element              | Convention     | Example                          |
|----------------------|----------------|----------------------------------|
| Angular Components   | PascalCase     | `StudentListComponent`           |
| Angular Services     | PascalCase     | `FeePaymentService`              |
| Variables / Methods  | camelCase      | `calculateTotalFee()`            |
| Constants            | UPPER_SNAKE    | `MAX_RETRY_COUNT`                |
| Java Classes         | PascalCase     | `TransactionController`          |
| Java Packages        | lowercase      | `com.cms.finance.service`        |
| Database Tables      | snake_case     | `student_enrollment`             |
| REST Endpoints       | kebab-case     | `/api/v1/fee-payments`           |

Additional clean code practices:
- Keep methods short and focused — single responsibility.
- Favor descriptive names over comments.
- Remove dead code; do not comment out code for future use.

### 4.2 Security

- Implement **Stateless Authentication** using **JWT** with **Spring Security**.
  - Access tokens should have short expiry (15–30 minutes); use refresh tokens for session continuity.
  - Store JWTs in `HttpOnly`, `Secure`, `SameSite=Strict` cookies on the frontend.
- Ensure **CORS policies** are strictly defined:
  - Whitelist only known frontend origins.
  - Restrict allowed HTTP methods and headers explicitly.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://cms.college.edu"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    return new UrlBasedCorsConfigurationSource() {{
        registerCorsConfiguration("/api/**", config);
    }};
}
```

### 4.3 Error Handling

- Use a **Global Exception Handler** (`@ControllerAdvice`) to return standardized error objects to the Angular frontend:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            Instant.now()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
```

- Standard error response format:

```json
{
  "status": 400,
  "message": "amount: must be positive, studentId: must not be null",
  "timestamp": "2026-04-07T06:30:00Z"
}
```

### 4.4 Validation

- Use **Jakarta Bean Validation** annotations for all incoming financial data:

```java
public record FeePaymentRequest(
    @NotNull(message = "Student ID is required")
    Long studentId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotBlank(message = "Payment method is required")
    String paymentMethod,

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    LocalDate paymentDate
) {}
```

- Enable validation on controller methods with `@Valid`:

```java
@PostMapping("/api/v1/fee-payments")
public ResponseEntity<FeePaymentResponse> createPayment(
        @Valid @RequestBody FeePaymentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(feePaymentService.processPayment(request));
}
```

- Apply validation to all API endpoints that accept user input — not just financial endpoints.

---

## 📌 Summary

| Area                  | Technology / Standard                          |
|-----------------------|------------------------------------------------|
| Frontend Framework    | Angular 21 (Standalone Components)             |
| State Management      | Angular Signals + RxJS (for async streams)     |
| UI Design System      | Angular Material 21 / Material 3 (M3)          |
| Theming               | CSS Variables via `mat.theme()` Sass mixins    |
| Backend Framework     | Java 21 + Spring Boot 3.x                      |
| Concurrency           | Virtual Threads (Project Loom)                 |
| Authentication        | Stateless JWT with Spring Security             |
| Data Integrity        | BigDecimal for all currency calculations       |
| Validation            | Jakarta Bean Validation                        |
| Error Handling        | Global `@ControllerAdvice` exception handler   |
| DB (Local Dev)        | H2 in-memory (profile: `local`)               |
| DB (Production)       | PostgreSQL 16 with Flyway migrations           |
| Backend Code Coverage | JaCoCo — 95% minimum enforced                 |
| Frontend Testing      | Not required                                   |
| Manual Test Cases     | Required for every completed task              |

---

## 5. Database Profiles

### 5.1 Profile-Based Database Configuration

The backend uses **Spring Profiles** to switch between databases depending on the environment:

| Profile | Database | Flyway | DDL Strategy | Use Case |
|---------|----------|--------|--------------|----------|
| `local` (default) | H2 in-memory | Disabled | `create-drop` | Local development — no external dependencies |
| `prod` / others | PostgreSQL 16 | Enabled | `validate` | Production, staging, CI environments |

### 5.2 Local Development (H2)

- The default Spring profile is `local`, which uses an **H2 in-memory database**.
- No Docker Compose or PostgreSQL is needed for local development.
- Flyway migrations are **disabled** — Hibernate auto-generates the schema from entities.
- The **H2 console** is available at `http://localhost:8080/h2-console` for inspecting data.
- To run locally: `./gradlew bootRun`

### 5.3 Production / Other Environments (PostgreSQL)

- Set `SPRING_PROFILES_ACTIVE=prod` (or any non-`local` profile) to use PostgreSQL.
- Flyway migrations are **enabled** — all schema changes must go through versioned SQL migration files.
- Requires a running PostgreSQL 16 instance (via Docker Compose or external).
- Hibernate `ddl-auto` is set to `validate` — it only validates the schema, never modifies it.

### 5.4 Test Profile

- Tests use the `test` profile (`application-test.yml`) with an H2 in-memory database.
- Flyway is disabled in tests; Hibernate uses `create-drop` for schema management.

---

## 6. Testing & Code Coverage

### 6.1 Backend Testing Requirements

- **Minimum code coverage: 95%** — enforced by JaCoCo plugin in `build.gradle.kts`.
- Running `./gradlew check` will fail the build if coverage drops below 95%.
- Coverage reports are generated at `backend/build/reports/jacoco/test/html/`.

### 6.2 Backend Test Types

| Test Type | Annotation | Purpose |
|-----------|------------|---------|
| Unit Tests | `@ExtendWith(MockitoExtension.class)` | Test services with mocked dependencies |
| Controller Tests | `@WebMvcTest(Controller.class)` | Test REST endpoints with MockMvc |
| Repository Tests | `@DataJpaTest` | Test JPA repositories with H2 |
| Integration Tests | `@SpringBootTest` + `@ActiveProfiles("test")` | Full application context tests |

### 6.3 Frontend Testing

- **No frontend unit tests are required** for this project.
- Frontend testing tooling (Vitest) is available but not enforced.
- If tests are needed in the future, use `provideHttpClientTesting` and Component Harnesses.

---

## 7. Manual Test Cases

### 7.1 Requirement

**Every completed task in this project must include manual test cases.** When a task is finished, the developer must create or update manual test case documentation to verify the feature works correctly.

### 7.2 Guidelines

1. **When to create**: After completing any backend or frontend task from the milestone tracker.
2. **Where to document**: Create a markdown file in `docs/manual-test-cases/` named after the module or feature (e.g., `docs/manual-test-cases/department-management.md`).
3. **Who creates them**: The developer who completes the task.
4. **Review**: Manual test cases should be included in the pull request for review.

### 7.3 Test Case Format

Each test case must include:

| Field | Description |
|-------|-------------|
| **Test Case ID** | Unique identifier (e.g., `TC-DEPT-001`) |
| **Title** | Short description of what is being tested |
| **Preconditions** | Setup required before testing |
| **Steps** | Numbered step-by-step instructions |
| **Expected Result** | What the correct behavior should be |
| **Actual Result** | To be filled during testing (leave blank in documentation) |
| **Status** | PASS / FAIL / NOT TESTED |

### 7.4 Template

```markdown
## TC-{MODULE}-{NUMBER}: {Title}

**Preconditions:**
- {List any required setup}

**Steps:**
1. {Step 1}
2. {Step 2}
3. {Step 3}

**Expected Result:**
- {What should happen}

**Status:** NOT TESTED
```

### 7.5 Example

```markdown
## TC-DEPT-001: Create a new department

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Send a POST request to `/api/v1/departments` with body: `{"name": "Computer Science", "code": "CS"}`
2. Verify the response status is 201 Created
3. Send a GET request to `/api/v1/departments`
4. Verify the new department appears in the list

**Expected Result:**
- Department is created successfully and returned in the list

**Status:** NOT TESTED
```

---

*This document is the authoritative reference for all technical decisions in the College Management System. All contributors must adhere to these standards for consistency, performance, and security.*
