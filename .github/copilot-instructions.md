# College Management System - GitHub Copilot Instructions

This document provides context and guidelines for GitHub Copilot when working on the College Management System (CMS) codebase.

## Project Overview

The College Management System is a full-stack web application for managing college operations including students, faculty, departments, courses, and lab resources. The system uses a modern tech stack with Angular on the frontend and Spring Boot on the backend.

## Technology Stack

### Frontend (Angular 21)
- **Framework**: Angular 21 with standalone components
- **UI Library**: Angular Material 21 with Material Design 3
- **Language**: TypeScript 5.9 with strict mode enabled
- **Styling**: SCSS + Tailwind CSS
- **Build Tool**: Angular CLI with `@angular/build`
- **Testing**: Vitest for unit tests
- **Code Formatting**: Prettier (single quotes, 100 char width)
- **Authentication**: Keycloak JS SDK for OAuth2/OIDC

### Backend (Spring Boot 3.4)
- **Framework**: Spring Boot 3.4.5
- **Language**: Java 21 with virtual threads enabled
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: H2 (in-memory) for local development, PostgreSQL 17 for production/other environments
- **Database Migrations**: Flyway (enabled for PostgreSQL profiles, disabled for local/H2)
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security with OAuth2 Resource Server (JWT)
- **Authentication Provider**: Keycloak 26.0
- **Code Coverage**: JaCoCo with 95% minimum coverage enforcement
- **Testing**: JUnit 5 + Spring Boot Test (backend only — no frontend unit tests required)

### Infrastructure
- **Authentication**: Keycloak 26.0 (realm: `cms`)
- **Database (Production)**: PostgreSQL 17
- **Database (Local Development)**: H2 in-memory
- **Container Orchestration**: Docker Compose

## Project Structure

```
CollegeManagementSystem/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/com/cms/
│   │   ├── config/            # Configuration classes (Security, etc.)
│   │   ├── controller/        # REST controllers
│   │   ├── service/           # Business logic services
│   │   ├── repository/        # JPA repositories
│   │   ├── model/             # JPA entities
│   │   │   └── enums/         # Enum types
│   │   ├── dto/               # Data Transfer Objects (Java records)
│   │   └── exception/         # Custom exceptions
│   └── src/main/resources/
│       ├── application.yml          # Common configuration (default profile: local)
│       ├── application-local.yml    # H2 in-memory database for local development
│       └── db/migration/            # Flyway SQL migrations (PostgreSQL)
├── frontend/                   # Angular frontend
│   └── src/app/
│       ├── core/              # Core services, guards, interceptors
│       │   ├── auth/          # Authentication service
│       │   ├── guards/        # Route guards
│       │   └── interceptors/  # HTTP interceptors
│       ├── features/          # Feature modules (dashboard, etc.)
│       └── shared/            # Shared components
├── infrastructure/             # Infrastructure configs
│   └── keycloak/              # Keycloak realm configuration
└── docker-compose.yml          # Local development setup
```

## Coding Conventions

### Backend (Java/Spring Boot)

1. **DTOs**: Use Java records for all DTOs in `com.cms.dto`
   ```java
   public record StudentRequest(
       @NotBlank String firstName,
       @NotBlank String lastName,
       @Email String email
   ) {}
   ```

2. **Entities**: Place JPA entities in `com.cms.model` with proper annotations
   ```java
   @Entity
   @Table(name = "students")
   public class Student {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       // ...
   }
   ```

3. **Repositories**: Use JpaRepository interfaces in `com.cms.repository`
   ```java
   public interface StudentRepository extends JpaRepository<Student, Long> {
       Optional<Student> findByEmail(String email);
   }
   ```

4. **Controllers**: Follow REST conventions with `/api/v1` prefix
   ```java
   @RestController
   @RequestMapping("/api/v1/students")
   public class StudentController {
       // Use constructor injection
   }
   ```

5. **Security**: Use method-level security with `@PreAuthorize`
   ```java
   @PreAuthorize("hasRole('ROLE_ADMIN')")
   @PostMapping
   public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
       // ...
   }
   ```

6. **Database Migrations**: Create Flyway migrations in `src/main/resources/db/migration`
   - Naming: `V{version}__{description}.sql` (e.g., `V1__create_students_table.sql`)

### Frontend (Angular/TypeScript)

1. **Components**: Use standalone components with explicit imports
   ```typescript
   @Component({
     selector: 'app-student-list',
     standalone: true,
     imports: [MatTable, MatPaginator, ...],
     templateUrl: './student-list.html',
     styleUrl: './student-list.scss',
   })
   export class StudentListComponent {
     // Use inject() function over constructor injection
     private readonly studentService = inject(StudentService);
   }
   ```

2. **Services**: Use signals for reactive state management
   ```typescript
   @Injectable({ providedIn: 'root' })
   export class StudentService {
     private readonly _students = signal<Student[]>([]);
     readonly students = this._students.asReadonly();
   }
   ```

3. **HTTP Calls**: Use HttpClient with typed responses
   ```typescript
   getStudents(): Observable<Student[]> {
     return this.http.get<Student[]>(`${environment.apiUrl}/students`);
   }
   ```

4. **Templates**: Use separate `.html` template files (not inline templates)

5. **Styling**: Use SCSS + Tailwind CSS with component-scoped styles

6. **Forms**: Use reactive forms with validators
   ```typescript
   this.form = this.fb.group({
     firstName: ['', [Validators.required]],
     email: ['', [Validators.required, Validators.email]],
   });
   ```

7. **Indian Currency (INR)**: This app is for Indian colleges. All monetary values **must** use the shared `InrPipe` — never `CurrencyPipe`, `| currency:'INR'`, or `toLocaleString()`.
   - Import: `import { InrPipe } from '../../../shared/pipes/inr.pipe';`
   - Add to `@Component imports[]`: `InrPipe`
   - Template: `{{ amount | inr }}` (whole rupees) or `{{ amount | inr:true }}` (with paise)
   - TypeScript: `formatCurrency(value, 'en-IN', '₹', 'INR', '1.0-0')` from `@angular/common`
   - The `en-IN` locale is globally registered in `app.config.ts` (`LOCALE_ID = 'en-IN'`), so `| number` uses Indian grouping (₹1,23,456) automatically.

8. **Tabular Figures**: All numeric and currency displays **must** use `font-variant-numeric: tabular-nums;` to ensure monospaced digits that align vertically in tables and lists.
   - **Where to use**: All classes that display numbers, amounts, codes, IDs, or statistics (`.cell-currency`, `.cell-number`, `.cell-code`, `.code-chip`, `.mlp-stat`, `.kpi-value`, `.stat-value`, `.receipt-card__number`, `.font-mono`, `.code-value`)
   - **Why**: Tabular figures ensure that "1" and "8" take the same horizontal space, preventing text shifting when numbers update and improving visual alignment in columns
   - **Required in**: Table cells, stat cards, KPI displays, receipt numbers, roll numbers, employee codes, fee amounts, currency values
   - This property is already applied to all standard currency/numeric classes in `styles.scss` — always use these classes rather than creating custom number displays without this property
     - **Data Table Alignment Standards (2026)**:
     - **Numeric columns** (currency, counts, IDs): Right-align for vertical rhythm. Decimal points and commas line up perfectly for instant magnitude comparison.
     - **Status badges**: Center-align for visual balance. Creates a clear vertical spine.
     - **Text columns** (names, descriptions): Left-align for natural reading flow.
     - **Dates**: Left-align with fixed-width format (`dd MMM yyyy`).
     - **Empty cells**: Use en-dash `'—'` (not blank).
     - **Column headers**: Must mirror data alignment (right headers for right data).
     - **Sort icons**: Context-aware positioning — right-aligned columns have icon on LEFT, center columns have icon CENTERED, left columns have icon on RIGHT (default).
     - **Sortable columns**: ALL data columns must have `mat-sort-header` directive (except actions column). Table must have `matSort` directive. Import `MatSortModule` in component.
     - See `docs/DATA_TABLE_ALIGNMENT_STANDARDS.md` for full specification.

9. **Date Formatting**: All dates **must** use the shared `AppDatePipe` from `shared/pipes/app-date.pipe.ts` — never use Angular's `date` pipe directly.
   - Import: `import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';`
   - Add to `@Component imports[]`: `AppDatePipe`
   - **Template usage**:
     - **Standard format**: `{{ date | appDate }}` → `28-04-2026` (DD-MM-YYYY)
     - **Short format**: `{{ date | appDate:'short' }}` → `28-04-26` (compact tables)
     - **DateTime format**: `{{ date | appDate:'dateTime' }}` → `28-04-2026 02:30 PM` (timestamps, AM/PM, local timezone)
     - **Time only**: `{{ date | appDate:'time' }}` → `02:30 PM` (time-only displays)
     - **Null dates**: `{{ null | appDate }}` → `—` (en-dash, not blank)
   - **Timezone**: The pipe automatically detects the user's browser timezone via `Intl.DateTimeFormat().resolvedOptions().timeZone` and converts UTC ISO-8601 timestamps from the API to local time. All backend timestamps are stored/serialized in UTC.
   - **Configuration**: Change format globally in `frontend/src/app/shared/config/date-format.config.ts`
   - **Why standardized**: Single format ensures consistency, professionalism, and localization (DD-MM-YYYY matches Indian expectations). See `docs/DATE_FORMATTING_STANDARD.md` for full guide.

9. **List Screen Layout (MLP Pattern)**: All list/master-entry screens **must** use the MLP layout classes defined in `styles.scss`. Never use the old `feature-list-page` / `page-header` pattern in new screens.

   ```html
   <div class="mlp-page">
     <!-- Header -->
     <div class="mlp-hdr anim-rise">
       <div class="mlp-hdr-left">
         <h1 class="mlp-title">Resource <em>Name</em></h1>
         <p class="mlp-sub">Brief description</p>
         @if (!loading()) {
           <div class="mlp-meta">
             <span class="mlp-stat"><span class="mlp-stat-dot mlp-stat-dot--blue"></span>{{ total() }} Total</span>
           </div>
         }
       </div>
       <button class="btn-primary" routerLink="/resource/new">
         <svg ...><!-- plus icon --></svg>
         Add Resource
       </button>
     </div>

     <!-- Toolbar: filters + search + view toggle (right-aligned automatically) -->
     <div class="mlp-toolbar anim-rise anim-rise--d1">
       <select class="mlp-filter-select" ...>...</select>
       <div class="search-bar" role="search">...</div>
       <!-- ALWAYS use <cms-view-toggle> — never inline mlp-seg -->
       <cms-view-toggle [mode]="viewMode()" storageKey="resource-list-view-mode" (modeChange)="setViewMode($event)" />
     </div>

     <!-- Table view -->
     <div class="content-card mlp-table-card">
       <table mat-table [dataSource]="dataSource" matSort class="modern-table">
         <!-- columns -->
         <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
         <tr mat-row *matRowDef="let row; columns: displayedColumns" class="table-row"></tr>
         <tr class="mat-row" *matNoDataRow>
           <td class="mat-cell" [attr.colspan]="displayedColumns.length" style="padding: 0">
             <cms-empty-state icon="inventory_2" title="No items found" subtitle="..." />
           </td>
         </tr>
       </table>
       <mat-paginator [pageSizeOptions]="[5, 10, 25, 50]" [pageSize]="10" showFirstLastButtons />
     </div>
   </div>
   ```

   **Key layout rules:**
   - `mlp-page` fills `min-height: calc(100vh - 91px)` — ensures content always fills the screen.
   - `content-card mlp-table-card` has `flex: 1` — grows to fill remaining height so the paginator is always at the bottom of the screen, even with few rows.
   - **Never** use `focus mode` — all screens use the regular `mlp-page` layout.
   - **View toggle**: always use `<cms-view-toggle>` component (import `CmsViewToggleComponent` from `shared/view-toggle/view-toggle.component`). Never create raw `<div class="mlp-seg">` inline. The component handles icons, localStorage persistence, and right-alignment automatically.
   - **Empty state**: always use `<cms-empty-state>` component (import `CmsEmptyStateComponent` from `shared/empty-state/empty-state.component`) — never write raw empty-state markup.
   - For screens with card+table toggle, the component class has `viewMode = signal<'card' | 'table'>()` driven by a `VIEW_MODE_KEY` constant and localStorage.

## Authentication & Authorization

### User Roles
- `ROLE_ADMIN` - Developer Administrator with full access to all screens
- `ROLE_COLLEGE_ADMIN` - College Administrator: Departments, Programs, Courses, Academic Years, Semesters, Fee Structures, Faculty, Agents, Referral Types, all Admission Management, all Finance, Admin Dashboard, Reports
- `ROLE_FRONT_OFFICE` - Front Office staff: all Admission Management screens, Front Office Dashboard
- `ROLE_CASHIER` - Accountant/Cashier: all Finance screens, Cashier Dashboard
- `ROLE_FACULTY` - Faculty-specific operations
- `ROLE_STUDENT` - Student-specific access
- `ROLE_LAB_INCHARGE` - Lab management access
- `ROLE_TECHNICIAN` - Technical support access
- `ROLE_PARENT` - Parent/guardian read-only access to ward's progress

### Frontend Authentication
- Keycloak JS handles authentication flow
- `AuthService` provides user profile and role information
- `authInterceptor` attaches JWT tokens to API requests
- Route guards protect authenticated routes

### Backend Security
- JWT tokens validated against Keycloak issuer
- Roles extracted from `realm_access.roles` claim
- Method-level security with `@PreAuthorize`

## Build & Test Commands

### Backend
```bash
cd backend

# Build
./gradlew compileJava

# Run tests
./gradlew test

# Run tests with coverage report (HTML report at build/reports/jacoco/test/html/)
./gradlew test jacocoTestReport

# Run full check (tests + 95% coverage verification)
./gradlew check

# Run application (defaults to 'local' profile with H2)
./gradlew bootRun

# Run application with PostgreSQL (production profile)
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

### Frontend
```bash
cd frontend

# Install dependencies
npm install

# Build
npm run build
# or: npx ng build

# Development server
npm run start
```

> **Note:** Frontend unit tests are not required for this project. No `npm run test` is expected.

### Docker Compose (Local Development)
```bash
# Start all services (PostgreSQL, Keycloak) — needed only for non-local profiles
docker compose up -d

# Stop services
docker compose down
```

## Database Profiles

The backend supports multiple Spring profiles for database configuration:

| Profile | Database | Flyway | Use Case |
|---------|----------|--------|----------|
| `local` (default) | H2 in-memory | Disabled | Local development — no external dependencies needed |
| `prod` / others | PostgreSQL 17 | Enabled | Production, staging, CI — requires Docker Compose or external PostgreSQL |

- **Local development**: Run `./gradlew bootRun` — uses H2 in-memory database with `ddl-auto: create-drop`. The H2 console is available at `http://localhost:8080/h2-console`.
- **Production/other environments**: Set `SPRING_PROFILES_ACTIVE=prod` (or omit the `local` profile) — uses PostgreSQL with Flyway migrations.

## Testing & Code Coverage

### Backend Testing Requirements
- **Minimum code coverage: 95%** — enforced by JaCoCo via `./gradlew check`
- All new services, controllers, and repositories **must** have corresponding unit tests
- Use `@SpringBootTest` with `@ActiveProfiles("test")` for integration tests
- Use `@WebMvcTest` for controller-layer unit tests
- Use `@DataJpaTest` for repository-layer tests
- Coverage reports are generated at `backend/build/reports/jacoco/test/html/`

### Frontend Testing
- **No frontend unit tests are required** for this project
- Frontend testing tooling (Vitest) is available but not enforced

## Manual Test Cases

**Every completed task in this project must include manual test cases.** When a task is finished, the developer must create or update manual test case documentation to verify the feature works as expected.

### Manual Test Case Guidelines

1. **When to create**: After completing any backend or frontend task from the milestone tracker
2. **Where to document**: Create a markdown file in `docs/manual-test-cases/` named after the module or feature (e.g., `docs/manual-test-cases/department-management.md`)
3. **Format**: Each test case must include:
   - **Test Case ID**: Unique identifier (e.g., `TC-DEPT-001`)
   - **Title**: Short description of what is being tested
   - **Preconditions**: Setup required before testing
   - **Steps**: Numbered step-by-step instructions
   - **Expected Result**: What the correct behavior should be
   - **Actual Result**: To be filled during testing (leave blank in documentation)
   - **Status**: PASS / FAIL / NOT TESTED

### Manual Test Case Template

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

### Example

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

## API Conventions

- Base path: `/api/v1`
- Use standard HTTP methods: GET, POST, PUT, DELETE
- Return appropriate HTTP status codes
- Use pagination for list endpoints
- Validate request bodies with Bean Validation (`@Valid`)

## Best Practices

1. **Prefer composition over inheritance**
2. **Use constructor injection for dependencies (backend)** and `inject()` function (frontend)
3. **Write unit tests for all backend services and controllers** — maintain 95% code coverage
4. **No frontend unit tests** — frontend testing is not required
5. **Create manual test cases** for every completed task (see [Manual Test Cases](#manual-test-cases))
6. **Document business/workflow changes** — update `docs/BUSINESS_REQUIREMENTS.md` whenever business rules, status transitions, fee logic, or workflows are added or modified
7. **Keep controllers thin** - business logic belongs in services
8. **Use meaningful variable and method names**
9. **Document public APIs with Javadoc (backend)** and JSDoc (frontend)
10. **Handle errors gracefully** with appropriate error responses
11. **Follow existing patterns** in the codebase when adding new features
12. **Use the `local` profile** for development (H2) and `prod` for production (PostgreSQL)

## AI Code Generation Quality

When generating code for this project, adhere to these quality rules to prevent hallucinations and ensure grounded, deterministic output:

### Grounding Rules
- **Only use dependencies declared in** `build.gradle.kts` (backend) or `package.json` (frontend). Do not invent or assume libraries.
- **Follow the skill templates** in `.github/skills/` — they define the canonical code patterns for this project.
- **Verify import paths** match the actual project package structure: `com.cms.controller`, `com.cms.service`, `com.cms.repository`, `com.cms.model`, `com.cms.dto`, `com.cms.exception`, `com.cms.config`.
- **Reference existing implementations** before generating new code — check how similar features are already built.

### Hallucination Prevention
- Do not generate calls to APIs, methods, or classes that do not exist in the project's dependency versions (Spring Boot 3.4.5, Angular 21, Material 21).
- Only use the defined Keycloak roles: `ROLE_ADMIN`, `ROLE_COLLEGE_ADMIN`, `ROLE_FRONT_OFFICE`, `ROLE_CASHIER`, `ROLE_FACULTY`, `ROLE_STUDENT`, `ROLE_LAB_INCHARGE`, `ROLE_TECHNICIAN`, `ROLE_PARENT`.
- Do not fabricate configuration properties — verify against `application.yml` and `application-local.yml`.

### Deterministic Patterns
- DTOs: Always Java records. Services: `@Transactional(readOnly = true)` at class level. Controllers: constructor injection with `/api/v1` prefix.
- Angular components: standalone with `inject()`, signals for state, `@if`/`@for` control flow, separate `.html` templates.
- **Currency**: Always `| inr` pipe (import `InrPipe` from `shared/pipes/inr.pipe`). Never `CurrencyPipe`, `| currency:'INR'`, or `toLocaleString`. Locale `en-IN` is global. **Use `| inr:false:false` in table cells** and put "(₹)" in column headers (2026 pattern — don't repeat symbol).
- **Dates**: Always `| appDate` pipe (import `AppDatePipe` from `shared/pipes/app-date.pipe`). Never use Angular's `date` pipe directly. Standard format: `DD-MM-YYYY` (configurable in `date-format.config.ts`). Null dates show `—`. Time formats use AM/PM (`hh:mm a`). The pipe auto-converts UTC → browser local timezone via `Intl.DateTimeFormat().resolvedOptions().timeZone`. New `time` format available: `{{ ts | appDate:'time' }}` → `02:30 PM`.
- **Tabular figures**: All numeric displays must include `font-variant-numeric: tabular-nums;`. Use existing classes (`.cell-currency`, `.cell-number`, `.mlp-stat`, etc.) — never create custom number styles without this property.
- List screens: Always use the **MLP layout pattern** (`mlp-page` → `mlp-hdr` → `mlp-toolbar` → `content-card mlp-table-card`). Never use `feature-list-page` or focus mode. View toggle always via `<cms-view-toggle>`. Empty state always via `<cms-empty-state>`.
- Tests: `@WebMvcTest` for controllers, `@ExtendWith(MockitoExtension.class)` for services, `@DataJpaTest` for repositories.

### Quality Verification
After generating code, always validate: (1) it compiles, (2) all tests pass, (3) coverage remains ≥ 95%, (4) all imports resolve, (5) it matches skill template patterns. See `docs/TECHNICAL_STANDARDS.md` Section 8 for the full AI output quality checklist.
