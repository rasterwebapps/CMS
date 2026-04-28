# 🏗️ Technical Standards & Architecture (2026 Edition)

> **College Management System (CMS)** — Definitive guide for frontend, backend, UI/UX, and security standards.

---

## 📋 Table of Contents

- [1. Frontend (Angular 21) Instructions](#1-frontend-angular-21-instructions)
- [2. Angular UI/UX & Design Standards (Material 3)](#2-angular-uiux--design-standards-material-3)
  - [2.1 Material 3 (M3)](#21-material-3-m3)
  - [2.2 Dynamic Theming](#22-dynamic-theming)
  - [2.3 Component Tokens](#23-component-tokens)
  - [2.4 High-Density Accounting UI](#24-high-density-accounting-ui)
  - [2.5 List Screen Layout Standards (MLP Pattern)](#25-list-screen-layout-standards-mlp-pattern)
- [3. Backend (Java 21 & Spring Boot 3.x)](#3-backend-java-21--spring-boot-3x)
- [4. General Coding & Security Guidelines](#4-general-coding--security-guidelines)
- [5. Database Profiles](#5-database-profiles)
- [6. Testing & Code Coverage](#6-testing--code-coverage)
- [7. Manual Test Cases](#7-manual-test-cases)
- [8. AI-Assisted Development Quality Guidelines](#8-ai-assisted-development-quality-guidelines)

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

### 2.5 List Screen Layout Standards (MLP Pattern)

All list / master-entry screens **must** follow the **MLP (Master List Page) layout pattern**. Never use the legacy `feature-list-page` / `page-header` structure or "focus mode" for list screens.

#### 2.5.1 HTML Structure

```html
<div class="mlp-page">

  <!-- ① Page Header ─────────────────────────────────── -->
  <div class="mlp-hdr anim-rise">
    <div class="mlp-hdr-left">
      <h1 class="mlp-title">Resource <em>Name</em></h1>
      <p class="mlp-sub">Brief description of this section</p>
      @if (!loading()) {
        <div class="mlp-meta">
          <span class="mlp-stat">
            <span class="mlp-stat-dot mlp-stat-dot--blue"></span>
            {{ total() }} Total
          </span>
        </div>
      }
    </div>
    <button class="btn-primary" routerLink="/resource/new" aria-label="Add new resource">
      <!-- SVG plus icon (16×16) -->
      Add Resource
    </button>
  </div>

  <!-- ② Toolbar: filters + search + view toggle ──────── -->
  <div class="mlp-toolbar anim-rise anim-rise--d1">
    <!-- Optional filter dropdowns -->
    <select class="mlp-filter-select" aria-label="Filter by ...">
      <option value="">All Items</option>
    </select>

    <!-- Search bar -->
    <div class="search-bar" role="search">
      <!-- SVG search icon -->
      <input class="search-bar__input" placeholder="Search..." type="text" aria-label="Search" />
      @if (searchValue()) {
        <button class="search-bar__clear" (click)="clearFilter()" aria-label="Clear search">
          <!-- SVG ✕ icon -->
        </button>
      }
    </div>

    <!-- View toggle — ALWAYS use the shared component, right-aligned automatically -->
    <cms-view-toggle
      [mode]="viewMode()"
      storageKey="resource-list-view-mode"
      (modeChange)="setViewMode($event)" />
  </div>

  <!-- ③ Loading skeleton ─────────────────────────────── -->
  @if (loading()) {
    <div class="mlp-loading-grid">
      @for (n of [1,2,3,4,5,6]; track n) {
        <div class="mlp-card-skeleton"></div>
      }
    </div>

  <!-- ④a Card view ──────────────────────────────────── -->
  } @else if (viewMode() === 'card') {
    @if (filteredItems().length === 0) {
      <cms-empty-state icon="category" title="No items yet" subtitle="Add your first item to get started" />
    } @else {
      <div class="mlp-cards-grid">
        @for (item of filteredItems(); track item.id) {
          <div class="mlp-card"> ... </div>
        }
      </div>
    }

  <!-- ④b Table view ─────────────────────────────────── -->
  } @else {
    <div class="content-card mlp-table-card">
      <table mat-table [dataSource]="dataSource" matSort class="modern-table">
        <!-- column definitions -->
        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns" class="table-row"></tr>
        <tr class="mat-row" *matNoDataRow>
          <td class="mat-cell" [attr.colspan]="displayedColumns.length" style="padding:0">
            <cms-empty-state icon="category"
              [title]="searchValue() ? 'No results found' : 'No items yet'"
              [subtitle]="searchValue() ? 'Try a different search term' : 'Add your first item to get started'" />
          </td>
        </tr>
      </table>
      <mat-paginator [pageSizeOptions]="[5, 10, 25, 50]" [pageSize]="10" showFirstLastButtons />
    </div>
  }

</div>
```

#### 2.5.2 TypeScript Component Pattern

```typescript
private static readonly VIEW_MODE_KEY = 'resource-list-view-mode';

// View mode — persisted to localStorage
readonly viewMode = signal<'card' | 'table'>(
  (localStorage.getItem(ResourceListComponent.VIEW_MODE_KEY) as 'card' | 'table') ?? 'card'
);

// MatTableDataSource wired in ngAfterViewInit via @ViewChild
dataSource = new MatTableDataSource<Resource>([]);
@ViewChild(MatPaginator) paginator!: MatPaginator;
@ViewChild(MatSort) sort!: MatSort;

ngAfterViewInit(): void {
  this.dataSource.paginator = this.paginator;
  this.dataSource.sort = this.sort;
}

// Keep MatTableDataSource in sync with the filtered signal
constructor() {
  effect(() => { this.dataSource.data = this.filteredItems(); });
}

setViewMode(mode: 'card' | 'table'): void {
  this.viewMode.set(mode);
  localStorage.setItem(ResourceListComponent.VIEW_MODE_KEY, mode);
}
```

#### 2.5.3 Required Imports

```typescript
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
```

#### 2.5.4 Layout Rules (Non-negotiable)

| Rule | Reason |
|------|--------|
| Use `mlp-page` as the root container | Provides `min-height: calc(100vh - 91px)` so content fills the full screen |
| Use `content-card mlp-table-card` for table | `flex: 1` causes the card to grow and fill remaining height; `mat-paginator` gets `margin-top: auto` via global CSS, pinning it to the bottom of the screen at all times |
| Use `<cms-view-toggle>` — never raw `<div class="mlp-seg">` | Shared component ensures uniform icons, localStorage persistence, and automatic right-alignment |
| Use `<cms-empty-state>` — never raw empty-state markup | Consistent empty state presentation across all screens |
| **Never** use focus mode on list screens | Focus mode was removed system-wide; all screens use the regular `mlp-page` layout |
| View toggle must always be the **last item** in `mlp-toolbar` | `CmsViewToggleComponent` has `:host { margin-left: auto }` which pushes it to the right edge automatically |

---

### 2.6 Typography & Numeric Formatting

#### 2.6.1 Tabular Figures (Monospaced Digits)

All numeric and currency displays **must** use `font-variant-numeric: tabular-nums;` to ensure monospaced digits that align vertically in tables and lists.

**Why tabular figures matter:**
- Proportional digits (default) have variable widths: "1" is narrow, "8" is wide
- This causes text shifting when numbers update dynamically
- Vertical misalignment in columns makes financial data hard to scan
- Tabular figures ensure every digit occupies the same horizontal space

**Where to use:**
- All table cells displaying numbers, amounts, codes, or IDs
- KPI cards and stat displays (`.kpi-value`, `.stat-value`, `.mlp-stat`)
- Currency amounts (`.cell-currency`, `.cell-number`)
- Receipt numbers, roll numbers, employee codes (`.receipt-card__number`, `.cell-code`, `.code-chip`, `.code-value`)
- Any monospace font usage (`.font-mono`)

**Standard classes (already configured in `styles.scss`):**
```scss
.cell-currency {
  font-family: var(--cms-font-mono);
  font-variant-numeric: tabular-nums;  // ✅ Ensures digits align
  font-weight: 600;
  color: var(--cms-primary);
}

.cell-number {
  font-family: var(--cms-font-mono);
  font-variant-numeric: tabular-nums;  // ✅ Ensures digits align
  font-weight: 500;
}

.mlp-stat {
  font-variant-numeric: tabular-nums;  // ✅ For "123 Total" badges
}
```

**Implementation rule:**
Always use existing classes (`.cell-currency`, `.cell-number`, `.mlp-stat`, etc.) for numeric displays. If creating a custom class for numbers, **always include** `font-variant-numeric: tabular-nums;`.

#### 2.6.2 Indian Currency (INR) Formatting

All monetary values **must** use the shared `InrPipe` — never `CurrencyPipe`, `| currency:'INR'`, or `toLocaleString()`.

- Import: `import { InrPipe } from '../../../shared/pipes/inr.pipe';`
- Add to `@Component imports[]`: `InrPipe`
- **Template usage**:
  - **Cards/Dialogs/Summaries**: `{{ amount | inr }}` (with ₹ symbol) or `{{ amount | inr:true }}` (with paise)
  - **Table cells (2026 UX pattern)**: `{{ amount | inr:false:false }}` (no symbol, no paise) — put "(₹)" in column header instead
  - **Example**:
    ```html
    <ng-container matColumnDef="totalAmount">
      <th mat-header-cell *matHeaderCellDef>Total Amount (₹)</th>
      <td mat-cell *matCellDef="let row">{{ row.totalAmount | inr:false:false }}</td>
    </ng-container>
    ```
- TypeScript: `formatCurrency(value, 'en-IN', '₹', 'INR', '1.0-0')` from `@angular/common`
- The `en-IN` locale is globally registered in `app.config.ts` (`LOCALE_ID = 'en-IN'`), so `| number` uses Indian grouping (₹1,23,456) automatically.
- **Why no symbol in tables**: Following 2026 best practices — reduces visual noise in dense tabular data while maintaining clarity via header annotation.

#### 2.6.3 Data Table Alignment Standards (2026)

For professional, high-performance data interfaces, alignment is critical for scannability and vertical rhythm:

**Numeric Columns** (currency, counts, IDs)  
→ **Right-align** for vertical rhythm. Decimal points and commas line up perfectly, enabling instant magnitude comparison.

**Status Badges**  
→ **Center-align** for visual balance. Creates a clear vertical spine that breaks up text/number monotony.

**Text Columns** (names, descriptions)  
→ **Left-align** for natural reading flow. Prevents "ragged left" edge.

**Dates**  
→ **Left-align** with fixed-width format (`dd MMM yyyy`). Dates are chronological identifiers, not numeric values.

**Empty Cells**  
→ Use en-dash `'—'` (not blank). Signals intentional absence rather than loading state.

**Column Headers**  
→ **Must mirror data alignment**. Right-aligned numbers demand right-aligned headers for visual coherence.

**Implementation**: Global CSS rules automatically apply correct alignment based on column names (`mat-column-totalAmount`, `mat-column-status`, etc.). See `docs/DATA_TABLE_ALIGNMENT_STANDARDS.md` for complete specification, examples, and checklist.

#### 2.6.4 Date Formatting Standard

All dates **must** use the shared `AppDatePipe` — never use Angular's `date` pipe directly.

- Import: `import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';`
- Add to `@Component imports[]`: `AppDatePipe`
- **Template usage**:
  - **Standard**: `{{ date | appDate }}` → `28-04-2026` (DD-MM-YYYY)
  - **Short**: `{{ date | appDate:'short' }}` → `28-04-26` (compact tables)
  - **DateTime**: `{{ date | appDate:'dateTime' }}` → `28-04-2026 14:30` (timestamps)
  - **Null**: `{{ null | appDate }}` → `—` (en-dash)
  - **Example**:
    ```html
    <ng-container matColumnDef="paymentDate">
      <th mat-header-cell *matHeaderCellDef>Payment Date</th>
      <td mat-cell *matCellDef="let row">
        <span class="cell-date">{{ row.paymentDate | appDate }}</span>
      </td>
    </ng-container>
    ```
- **Configuration**: Change format globally in `frontend/src/app/shared/config/date-format.config.ts`:
  ```typescript
  export const DATE_FORMATS = {
    standard: 'dd-MM-yyyy',  // Change to 'dd/MM/yyyy' or 'dd MMM yyyy' as needed
    short: 'dd-MM-yy',
    long: 'dd-MM-yyyy',
    dateTime: 'dd-MM-yyyy HH:mm',
  };
  ```
- **Why standardized**: Single global format ensures consistency across all screens. DD-MM-YYYY matches Indian regional expectations. See `docs/DATE_FORMATTING_STANDARD.md` for complete guide.

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
| DB (Production)       | PostgreSQL 17 with Flyway migrations           |
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
| `prod` / others | PostgreSQL 17 | Enabled | `validate` | Production, staging, CI environments |

### 5.2 Local Development (H2)

- The default Spring profile is `local`, which uses an **H2 in-memory database**.
- No Docker Compose or PostgreSQL is needed for local development.
- Flyway migrations are **disabled** — Hibernate auto-generates the schema from entities.
- The **H2 console** is available at `http://localhost:8080/h2-console` for inspecting data.
- To run locally: `./gradlew bootRun`

### 5.3 Production / Other Environments (PostgreSQL)

- Set `SPRING_PROFILES_ACTIVE=prod` (or any non-`local` profile) to use PostgreSQL.
- Flyway migrations are **enabled** — all schema changes must go through versioned SQL migration files.
- Requires a running PostgreSQL 17 instance (via Docker Compose or external).
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

## 8. AI-Assisted Development Quality Guidelines

This project uses **GitHub Copilot** with custom skill templates (`.github/skills/`) and project-level instructions (`.github/copilot-instructions.md`) for AI-assisted code generation. The following guidelines ensure AI-generated code meets the quality, correctness, and consistency standards of the CMS project.

### 8.1 Hallucination Prevention

**Hallucination** occurs when an AI confidently generates incorrect, fabricated, or nonsensical code — for example, inventing a library that doesn't exist or calling an API method with the wrong signature.

To prevent hallucinations in the CMS codebase:

- **Verify all imports exist** — Do not accept generated `import` statements without confirming the package/module is declared in `build.gradle.kts` (backend) or `package.json` (frontend).
- **Check API signatures** — Validate that generated method calls match actual Spring Boot, Angular, or Angular Material API signatures for the versions used in this project (Spring Boot 3.4, Angular 21, Material 21).
- **Use only declared dependencies** — Never introduce libraries not already in the project's dependency files without explicit approval. If a new dependency is needed, add it properly via `npm install` or by editing `build.gradle.kts`.
- **Reference existing patterns** — Before generating a new service, controller, or component, review an existing implementation in the codebase for the correct pattern (e.g., look at existing controllers in `com.cms.controller` before creating a new one).
- **Verify Keycloak role names** — Only use the six defined roles: `ROLE_ADMIN`, `ROLE_FACULTY`, `ROLE_STUDENT`, `ROLE_LAB_INCHARGE`, `ROLE_TECHNICIAN`, `ROLE_PARENT`. Do not invent new roles without updating `cms-realm.json`.

### 8.2 Grounding

**Grounding** means anchoring AI-generated code in real, verifiable project context to reduce errors and ensure correctness.

All AI-generated code must be grounded in:

| Source | Purpose |
|--------|---------|
| `.github/copilot-instructions.md` | Project conventions, build commands, architecture |
| `.github/skills/*.md` | Code generation templates for each layer |
| `docs/TECHNICAL_STANDARDS.md` | Architecture and design standards |
| Existing source code | Established patterns and naming conventions |
| `build.gradle.kts` / `package.json` | Actual dependency versions |

Grounding practices:
- Always check the **actual project structure** before generating file paths (e.g., `com.cms.service`, not `com.cms.services`).
- Verify **database column names** against existing Flyway migrations or JPA entities before generating queries.
- Confirm **environment configuration** keys match `application.yml` / `application-local.yml` before referencing properties.
- Use the **skill templates** in `.github/skills/` as the canonical patterns for new code — they define the project's conventions for components, services, controllers, migrations, and authentication.

### 8.3 Deterministic Output

**Deterministic output** means generating the same consistent code patterns every time for the same type of task. This ensures codebase uniformity.

Standards for deterministic code generation:

- **DTOs**: Always use Java `record` types in `com.cms.dto` — never use classes with getters/setters for DTOs.
- **Dependency Injection**: Use constructor injection in backend code; prefer the `inject()` function in frontend standalone components — do not use field-level `@Autowired` in backend code.
- **Signals over BehaviorSubject**: Always use Angular Signals (`signal()`, `computed()`) for reactive state — never use `BehaviorSubject` for component state.
- **Template syntax**: Always use `@if`/`@for`/`@switch` control flow — never use `*ngIf`/`*ngFor`/`ngSwitch` structural directives.
- **REST paths**: Always use `/api/v1/{resource}` prefix with kebab-case — no exceptions.
- **Test annotations**: Use `@WebMvcTest` for controllers, `@ExtendWith(MockitoExtension.class)` for services, `@DataJpaTest` for repositories — follow the patterns in the skill templates exactly.

### 8.4 Latency & Throughput Awareness

**Latency** is the time delay between a request and response. **Throughput** is the number of requests a system can handle per second. AI-generated code must be performance-aware.

- **Use Virtual Threads** — All backend request handling uses virtual threads (configured in `application.yml`). Do not add thread pool configurations or reactive (WebFlux) patterns.
- **Use pagination** — All list endpoints must return `Page<T>` using Spring Data's `Pageable` parameter. Never return unbounded `List<T>` from API endpoints.
- **Lazy-load frontend routes** — Use `loadComponent` for route definitions. Never eagerly import feature components in `app.routes.ts`.
- **Use `FetchType.LAZY`** — All JPA `@ManyToOne` and `@OneToMany` associations must use lazy fetching to prevent N+1 query issues.
- **Minimize bundle size** — Import specific Angular Material components (e.g., `MatButtonModule`), not entire module groups.

### 8.5 Streaming & Incremental Delivery

**Streaming** means delivering data incrementally rather than waiting for a complete response. Apply incremental delivery patterns where appropriate:

- **Paginated API responses** — Use Spring Data `Page`/`Slice` for large datasets instead of loading all records at once.
- **Progressive UI loading** — Show loading spinners (`<mat-spinner>`) during data fetch. Use `@if (loading())` patterns from the Angular component skill template.
- **Optimistic UI updates** — For create/update operations, update the local signal state immediately (as shown in the Angular service skill template) rather than waiting for a full page reload. Include error handling in `catchError` to revert the optimistic change if the API request fails.

### 8.6 Stop Sequences & Scope Boundaries

**Stop sequences** define where AI generation should stop. In the context of CMS development, these are the boundaries that code generation should respect:

- **Single responsibility** — Generate one component, service, or controller per file. Do not generate entire feature modules in a single output.
- **Layer boundaries** — Controllers should not contain business logic; services should not contain HTTP/response logic; repositories should only define queries.
- **Test scope** — Each test class tests exactly one class. Controller tests mock services; service tests mock repositories. Do not create integration tests that span multiple layers unless explicitly requested.
- **Migration scope** — Each Flyway migration handles one logical change (create table, add column, add index). Do not combine unrelated schema changes in a single migration file.
- **Security scope** — Security annotations (`@PreAuthorize`) belong on controller methods. Do not add security checks inside service methods — services trust that the caller is authorized.

### 8.7 AI Output Quality Checklist

Before accepting AI-generated code, verify:

| # | Check | Description |
|---|-------|-------------|
| 1 | **Compiles** | Code compiles without errors (`./gradlew compileJava` or `npx ng build`) |
| 2 | **Tests pass** | All existing tests pass (`./gradlew check`) |
| 3 | **Coverage maintained** | JaCoCo coverage remains ≥ 95% |
| 4 | **No phantom imports** | All imported classes/modules exist in the project's dependencies |
| 5 | **Follows skill template** | Generated code matches the patterns in `.github/skills/` |
| 6 | **Correct roles** | Security annotations use only defined Keycloak roles |
| 7 | **Consistent naming** | Follows the naming conventions in Section 4.1 |
| 8 | **Manual test cases** | New features include test cases in `docs/manual-test-cases/` |
| 9 | **No hardcoded values** | Environment-specific values use configuration, not hardcoded strings |

---

*This document is the authoritative reference for all technical decisions in the College Management System. All contributors must adhere to these standards for consistency, performance, and security.*

---

## 9. Business & Workflow Documentation

### 9.1 Mandatory Documentation Rule

**Any change to business rules, workflows, status transitions, fee calculation logic, or operational processes must be documented in `docs/BUSINESS_REQUIREMENTS.md` before the change is considered complete.** This is a mandatory requirement in the Definition of Done for every task.

### 9.2 What Must Be Documented

| Change Type | Documentation Required |
|-------------|----------------------|
| New business rule or workflow | Add a new BR-{N} section in `docs/BUSINESS_REQUIREMENTS.md` |
| Modified status transition | Update the relevant BR section and status transition diagram |
| New or changed fee calculation logic | Update BR-5 or add a new BR section with formulas |
| New entity relationship | Update the relevant BR section with data model |
| New role-based access pattern | Update the relevant BR section's Roles table |
| New screen or screen modification | Update the relevant BR section with screen layout |

### 9.3 Documentation Workflow

1. **Before coding**: Review existing business requirements in `docs/BUSINESS_REQUIREMENTS.md` for context.
2. **During design**: Draft any new or modified business requirements.
3. **Before merging**: Ensure all business/workflow changes are documented in:
   - `docs/BUSINESS_REQUIREMENTS.md` — business rules and workflows
   - `docs/DEVELOPMENT_PLAN.md` or `docs/RELEASE_1_MILESTONES.md` — milestone updates
   - `docs/manual-test-cases/` — test case updates
   - `CHANGELOG.md` — release notes
4. **Code review**: Reviewers must verify that business documentation is complete and accurate.

### 9.4 Change Log

Every update to `docs/BUSINESS_REQUIREMENTS.md` must include an entry in the Change Log table at the bottom of the document, recording the date, BR IDs affected, description of the change, and the author.
