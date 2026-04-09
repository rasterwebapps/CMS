# 🚀 Release 1 — Milestone Tracker

> **College Management System — Release 1** covers the foundational platform: project scaffolding, authentication & identity, core academic/lab modules, student lifecycle & scheduling, finance & asset management, and assessment & reporting.
>
> This corresponds to **Phases 0–5** from the [Master Development Plan](DEVELOPMENT_PLAN.md).

---

## 📋 Table of Contents

- [Release 1 Scope](#-release-1-scope)
- [R1-M0: Project Scaffolding](#-r1-m0-project-scaffolding)
- [R1-M1: Foundation & Identity](#-r1-m1-foundation--identity)
- [R1-M2: Core Academic & Lab Mapping](#-r1-m2-core-academic--lab-mapping)
- [R1-M3: Operational Logistics](#-r1-m3-operational-logistics)
- [R1-M4: Finance & Asset Management](#-r1-m4-finance--asset-management)
- [R1-M5: Assessment & Reporting](#-r1-m5-assessment--reporting)
- [Release 1 Definition of Done](#-release-1-definition-of-done)
- [Release 1 Progress Tracking](#-release-1-progress-tracking)
- [Deferred to Release 2](#-deferred-to-release-2)

---

## 🎯 Release 1 Scope

| Milestone | Phase Origin | Modules Covered | Key Outcome |
|-----------|-------------|-----------------|-------------|
| **R1-M0** | Phase 0 | — | Runnable backend + frontend + Docker Compose |
| **R1-M1** | Phase 1 | Module 1 (partial) | Keycloak SSO, secured API, authenticated SPA shell |
| **R1-M2** | Phase 2 | Modules 1, 3, 4, 7.1 | Departments, programs, labs, faculty, curriculum |
| **R1-M3** | Phase 3 | Modules 2, 5, 7.2 | Students, scheduling, attendance |
| **R1-M4** | Phase 4 | Modules 7.3, 7.4, 8, 16 | Fees, equipment, inventory, assets |
| **R1-M5** | Phase 5 | Modules 6, 7.5–7.10, 13 | Exams, lab evaluation, analytics, accreditation |

---

## 🧱 R1-M0: Project Scaffolding

> **Goal:** Create the runnable project skeleton — backend, frontend, and infrastructure — with no business logic.

### R1-M0.1 — Backend Project Initialization

- [ ] **R1-0.1.1** Initialize Spring Boot 3.4 project with Gradle (Kotlin DSL)
  - Group: `com.cms`, Artifact: `cms-backend`
  - Java 21, Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring OAuth2 Resource Server
  - H2 (runtime), PostgreSQL (runtime), Flyway
  - JaCoCo plugin with 95% coverage threshold
- [ ] **R1-0.1.2** Create `application.yml` with common settings
  - Default profile: `local`
  - Virtual threads enabled: `spring.threads.virtual.enabled: true`
  - API base path: `/api/v1`
- [ ] **R1-0.1.3** Create `application-local.yml` — H2 in-memory config
  - `spring.jpa.hibernate.ddl-auto: create-drop`
  - Flyway disabled
  - H2 console enabled at `/h2-console`
- [ ] **R1-0.1.4** Create `application-prod.yml` — PostgreSQL config
  - `spring.jpa.hibernate.ddl-auto: validate`
  - Flyway enabled
  - Database connection via environment variables
- [ ] **R1-0.1.5** Create `application-test.yml` — Test profile
  - H2 in-memory, Flyway disabled, `create-drop`
- [ ] **R1-0.1.6** Verify backend starts with `./gradlew bootRun` (local profile)
- [ ] **R1-0.1.7** Verify `./gradlew check` passes (empty project baseline)

### R1-M0.2 — Frontend Project Initialization

- [ ] **R1-0.2.1** Initialize Angular 21 project with Angular CLI
  - Standalone components (no NgModules)
  - SCSS styling
  - SSR disabled initially (enable later)
- [ ] **R1-0.2.2** Install Angular Material 21 with Material 3 theme
  - Configure `mat.theme()` with azure palette
  - Light and dark mode support
  - Custom `_theme.scss` file
- [ ] **R1-0.2.3** Install Keycloak JS SDK (`keycloak-js`)
- [ ] **R1-0.2.4** Set up folder-by-feature structure
  - `src/app/core/` — auth, guards, interceptors
  - `src/app/shared/` — components, pipes, directives
  - `src/app/features/` — feature folders (empty initially)
- [ ] **R1-0.2.5** Configure Prettier (single quotes, 100 char width)
- [ ] **R1-0.2.6** Verify frontend starts with `ng serve`

### R1-M0.3 — Infrastructure & Docker Compose

- [ ] **R1-0.3.1** Create `infrastructure/keycloak/` directory with realm import config
- [ ] **R1-0.3.2** Create `docker-compose.yml` with services:
  - Keycloak 26.0 (with realm auto-import)
  - PostgreSQL 16 (for prod profile)
- [ ] **R1-0.3.3** Verify `docker compose up -d keycloak` starts Keycloak with `cms` realm
- [ ] **R1-0.3.4** Verify Keycloak admin console is accessible and realm roles exist:
  - `ROLE_ADMIN`, `ROLE_FACULTY`, `ROLE_STUDENT`, `ROLE_LAB_INCHARGE`, `ROLE_TECHNICIAN`, `ROLE_PARENT`

---

## 🔐 R1-M1: Foundation & Identity

> **Goal:** Secure the platform — Keycloak SSO, JWT-based API security, authenticated Angular shell with role-based navigation.

### R1-M1.1 — Backend Security Configuration

- [ ] **R1-1.1.1** Create `SecurityConfig` class
  - OAuth2 Resource Server with JWT validation (Keycloak issuer)
  - CORS configuration (whitelist frontend origin)
  - CSRF disabled for stateless API
  - Public endpoints: `/api/v1/health`, `/h2-console/**`
  - All other endpoints require authentication
- [ ] **R1-1.1.2** Create JWT role converter for Keycloak realm roles
  - Map `realm_access.roles` from JWT to Spring Security authorities
- [ ] **R1-1.1.3** Create `GlobalExceptionHandler` (`@RestControllerAdvice`)
  - `ResourceNotFoundException` → 404
  - `MethodArgumentNotValidException` → 400
  - `AccessDeniedException` → 403
  - Generic `Exception` → 500
  - Standard error response record: `ErrorResponse(int status, String message, Instant timestamp)`
- [ ] **R1-1.1.4** Create health check endpoint (`GET /api/v1/health`)
- [ ] **R1-1.1.5** Write unit tests for SecurityConfig and GlobalExceptionHandler
- [ ] **R1-1.1.6** Create manual test cases: `docs/manual-test-cases/security-config.md`

### R1-M1.2 — Frontend Authentication

- [ ] **R1-1.2.1** Create `AuthService` — initialize Keycloak, login/logout, token management
- [ ] **R1-1.2.2** Create `AuthGuard` — protect routes, redirect unauthenticated users
- [ ] **R1-1.2.3** Create `AuthInterceptor` — attach Bearer token to all API requests
- [ ] **R1-1.2.4** Create `RoleGuard` — restrict routes by user role
- [ ] **R1-1.2.5** Configure `app.config.ts` with providers (router, HTTP client, hydration)
- [ ] **R1-1.2.6** Create manual test cases: `docs/manual-test-cases/authentication.md`

### R1-M1.3 — Application Shell & Navigation

- [ ] **R1-1.3.1** Create `AppComponent` with Material sidenav layout
  - Top toolbar with app title, user menu, theme toggle
  - Side navigation with role-based menu items
- [ ] **R1-1.3.2** Create `DashboardComponent` (landing page)
  - Placeholder dashboard cards for key metrics
  - "Lab Utilization" widget placeholder
- [ ] **R1-1.3.3** Configure `app.routes.ts` with lazy-loaded feature routes
- [ ] **R1-1.3.4** Implement light/dark theme toggle
- [ ] **R1-1.3.5** Create manual test cases: `docs/manual-test-cases/app-shell.md`

---

## 🧪 R1-M2: Core Academic & Lab Mapping

> **Goal:** Define the institutional structure — departments, programs, academic years, labs, faculty, and curriculum.

### R1-M2.1 — Department Management (Module 1)

**Backend:**
- [ ] **R1-2.1.1** Create `Department` entity (`id`, `name`, `code`, `description`, `hodName`, `createdAt`, `updatedAt`)
- [ ] **R1-2.1.2** Create `DepartmentRepository` (JpaRepository)
- [ ] **R1-2.1.3** Create `DepartmentService` with CRUD operations
- [ ] **R1-2.1.4** Create `DepartmentController` — REST endpoints:
  - `POST /api/v1/departments` — Create (ROLE_ADMIN)
  - `GET /api/v1/departments` — List all (authenticated)
  - `GET /api/v1/departments/{id}` — Get by ID (authenticated)
  - `PUT /api/v1/departments/{id}` — Update (ROLE_ADMIN)
  - `DELETE /api/v1/departments/{id}` — Delete (ROLE_ADMIN)
- [ ] **R1-2.1.5** Create `DepartmentRequest` and `DepartmentResponse` DTOs (Java records)
- [ ] **R1-2.1.6** Create Flyway migration: `V1__create_departments_table.sql`
- [ ] **R1-2.1.7** Write unit + controller tests (95% coverage)
- [ ] **R1-2.1.8** Create manual test cases: `docs/manual-test-cases/department-management.md`

**Frontend:**
- [ ] **R1-2.1.9** Create `features/department/` folder structure
- [ ] **R1-2.1.10** Create `DepartmentService` (API calls)
- [ ] **R1-2.1.11** Create `DepartmentListComponent` — Material table with search, sort, pagination
- [ ] **R1-2.1.12** Create `DepartmentFormComponent` — Create/Edit form with validation
- [ ] **R1-2.1.13** Create department routes (lazy-loaded)

### R1-M2.2 — Program & Course Management (Module 1)

**Backend:**
- [ ] **R1-2.2.1** Create `Program` entity (`id`, `name`, `code`, `degreeType`, `durationYears`, `department`)
- [ ] **R1-2.2.2** Create `Course` entity (`id`, `name`, `code`, `credits`, `theoryCredits`, `labCredits`, `program`, `semester`)
- [ ] **R1-2.2.3** Create repositories, services, controllers for Program and Course
- [ ] **R1-2.2.4** Create DTOs (Java records) for Program and Course
- [ ] **R1-2.2.5** Create Flyway migrations: `V2__create_programs_table.sql`, `V3__create_courses_table.sql`
- [ ] **R1-2.2.6** Write unit + controller tests (95% coverage)
- [ ] **R1-2.2.7** Create manual test cases: `docs/manual-test-cases/program-course-management.md`

**Frontend:**
- [ ] **R1-2.2.8** Create `features/program/` and `features/course/` folder structures
- [ ] **R1-2.2.9** Create list and form components for Program and Course
- [ ] **R1-2.2.10** Create routes (lazy-loaded)

### R1-M2.3 — Academic Year & Calendar (Module 1)

**Backend:**
- [ ] **R1-2.3.1** Create `AcademicYear` entity (`id`, `name`, `startDate`, `endDate`, `isCurrent`)
- [ ] **R1-2.3.2** Create `Semester` entity (`id`, `name`, `academicYear`, `startDate`, `endDate`, `semesterNumber`)
- [ ] **R1-2.3.3** Create repositories, services, controllers
- [ ] **R1-2.3.4** Create DTOs and Flyway migrations
- [ ] **R1-2.3.5** Write unit + controller tests (95% coverage)
- [ ] **R1-2.3.6** Create manual test cases: `docs/manual-test-cases/academic-year-management.md`

**Frontend:**
- [ ] **R1-2.3.7** Create `features/academic-year/` with list and form components
- [ ] **R1-2.3.8** Create academic calendar view component

### R1-M2.4 — Lab Setup & Configuration (Module 7.1)

**Backend:**
- [ ] **R1-2.4.1** Create `Lab` entity (`id`, `name`, `labType`, `department`, `building`, `roomNumber`, `capacity`, `status`)
- [ ] **R1-2.4.2** Create `LabType` enum (`COMPUTER`, `PHYSICS`, `CHEMISTRY`, `ELECTRONICS`, `BIOLOGY`, `LANGUAGE`, `MECHANICAL`, `OTHER`)
- [ ] **R1-2.4.3** Create `LabInChargeAssignment` entity (map faculty/technician to lab)
- [ ] **R1-2.4.4** Create repositories, services, controllers
  - `POST /api/v1/labs` — Create lab (ROLE_ADMIN)
  - `GET /api/v1/labs` — List labs (authenticated)
  - `GET /api/v1/labs/{id}` — Get lab details (authenticated)
  - `PUT /api/v1/labs/{id}` — Update lab (ROLE_ADMIN, ROLE_LAB_INCHARGE)
  - `DELETE /api/v1/labs/{id}` — Delete lab (ROLE_ADMIN)
  - `POST /api/v1/labs/{id}/assign` — Assign in-charge (ROLE_ADMIN)
- [ ] **R1-2.4.5** Create DTOs and Flyway migrations
- [ ] **R1-2.4.6** Write unit + controller tests (95% coverage)
- [ ] **R1-2.4.7** Create manual test cases: `docs/manual-test-cases/lab-setup.md`

**Frontend:**
- [ ] **R1-2.4.8** Create `features/lab/` folder structure
- [ ] **R1-2.4.9** Create `LabListComponent` — Material table with filters (by department, type, status)
- [ ] **R1-2.4.10** Create `LabFormComponent` — Create/Edit form
- [ ] **R1-2.4.11** Create `LabDetailComponent` — Lab detail view with assigned staff
- [ ] **R1-2.4.12** Create lab routes (lazy-loaded)

### R1-M2.5 — Faculty Management (Module 3)

**Backend:**
- [ ] **R1-2.5.1** Create `Faculty` entity (`id`, `employeeCode`, `firstName`, `lastName`, `email`, `phone`, `department`, `designation`, `specialization`, `labExpertise`, `joiningDate`)
- [ ] **R1-2.5.2** Create repositories, services, controllers
  - CRUD endpoints under `/api/v1/faculty`
  - Lab teaching assignment endpoints
- [ ] **R1-2.5.3** Create DTOs and Flyway migrations
- [ ] **R1-2.5.4** Write unit + controller tests (95% coverage)
- [ ] **R1-2.5.5** Create manual test cases: `docs/manual-test-cases/faculty-management.md`

**Frontend:**
- [ ] **R1-2.5.6** Create `features/faculty/` with list, form, and detail components
- [ ] **R1-2.5.7** Create faculty routes (lazy-loaded)

### R1-M2.6 — Curriculum & Lab-Curriculum Mapping (Module 4)

**Backend:**
- [ ] **R1-2.6.1** Create `Syllabus` entity (course syllabus with theory + lab components)
- [ ] **R1-2.6.2** Create `Experiment` entity (`id`, `name`, `course`, `experimentNumber`, `description`, `learningOutcomes`)
- [ ] **R1-2.6.3** Create `LabCurriculumMapping` entity (map experiments to Course Outcomes / Program Outcomes)
- [ ] **R1-2.6.4** Create repositories, services, controllers
- [ ] **R1-2.6.5** Create DTOs and Flyway migrations
- [ ] **R1-2.6.6** Write unit + controller tests (95% coverage)
- [ ] **R1-2.6.7** Create manual test cases: `docs/manual-test-cases/curriculum-management.md`

**Frontend:**
- [ ] **R1-2.6.8** Create `features/curriculum/` with syllabus and experiment components
- [ ] **R1-2.6.9** Create CO/PO mapping matrix UI component

---

## 📅 R1-M3: Operational Logistics

> **Goal:** Student lifecycle, lab scheduling, and attendance tracking.

### R1-M3.1 — Student Management (Module 2)

**Backend:**
- [ ] **R1-3.1.1** Create `Student` entity (`id`, `rollNumber`, `firstName`, `lastName`, `email`, `phone`, `program`, `semester`, `admissionDate`, `labBatch`, `status`)
- [ ] **R1-3.1.2** Create `Admission` entity for admission workflow
- [ ] **R1-3.1.3** Create repositories, services, controllers
  - CRUD endpoints under `/api/v1/students`
  - Admission workflow: `/api/v1/admissions`
  - Lab batch assignment during enrollment
- [ ] **R1-3.1.4** Create DTOs and Flyway migrations
- [ ] **R1-3.1.5** Write unit + controller tests (95% coverage)
- [ ] **R1-3.1.6** Create manual test cases: `docs/manual-test-cases/student-management.md`

**Frontend:**
- [ ] **R1-3.1.7** Create `features/student/` with list, form, detail, and admission components
- [ ] **R1-3.1.8** Create student directory with search and filters
- [ ] **R1-3.1.9** Create student routes (lazy-loaded)

### R1-M3.2 — Lab Scheduling & Timetable (Module 7.2)

**Backend:**
- [ ] **R1-3.2.1** Create `LabSchedule` entity (`id`, `lab`, `course`, `faculty`, `batch`, `dayOfWeek`, `startTime`, `endTime`, `semester`)
- [ ] **R1-3.2.2** Create `LabSlot` entity for reusable time slot definitions
- [ ] **R1-3.2.3** Create scheduling service with conflict detection logic
  - Check lab availability, faculty availability, batch conflicts
- [ ] **R1-3.2.4** Create controllers:
  - `POST /api/v1/lab-schedules` — Create schedule (ROLE_ADMIN, ROLE_FACULTY)
  - `GET /api/v1/lab-schedules` — List with filters (lab, faculty, batch, day)
  - Conflict detection endpoint
- [ ] **R1-3.2.5** Create DTOs and Flyway migrations
- [ ] **R1-3.2.6** Write unit + controller tests (95% coverage)
- [ ] **R1-3.2.7** Create manual test cases: `docs/manual-test-cases/lab-scheduling.md`

**Frontend:**
- [ ] **R1-3.2.8** Create `features/lab-schedule/` folder structure
- [ ] **R1-3.2.9** Create lab calendar view (weekly timetable grid)
- [ ] **R1-3.2.10** Create schedule form with conflict alerts
- [ ] **R1-3.2.11** Create lab schedule routes (lazy-loaded)

### R1-M3.3 — Attendance Management (Module 5)

**Backend:**
- [ ] **R1-3.3.1** Create `Attendance` entity (`id`, `student`, `course`, `date`, `status`, `type` [THEORY/LAB])
- [ ] **R1-3.3.2** Create `LabAttendance` entity (extends with `experiment`, `labBatch`, `labSession`)
- [ ] **R1-3.3.3** Create attendance service with:
  - Mark attendance (individual + bulk)
  - Attendance percentage calculation
  - Low attendance alert logic
- [ ] **R1-3.3.4** Create controllers:
  - `POST /api/v1/attendance` — Mark attendance (ROLE_FACULTY, ROLE_LAB_INCHARGE)
  - `GET /api/v1/attendance/reports` — Attendance reports with filters
  - `GET /api/v1/attendance/alerts` — Low attendance alerts
- [ ] **R1-3.3.5** Create DTOs and Flyway migrations
- [ ] **R1-3.3.6** Write unit + controller tests (95% coverage)
- [ ] **R1-3.3.7** Create manual test cases: `docs/manual-test-cases/attendance-management.md`

**Frontend:**
- [ ] **R1-3.3.8** Create `features/attendance/` folder structure
- [ ] **R1-3.3.9** Create attendance marking component (batch-wise view)
- [ ] **R1-3.3.10** Create attendance report component with charts
- [ ] **R1-3.3.11** Create attendance routes (lazy-loaded)

---

## 💰 R1-M4: Finance & Asset Management

> **Goal:** Fee lifecycle, lab equipment/inventory tracking, and institutional asset management.

### R1-M4.1 — Fee Structure & Collection (Module 8)

**Backend:**
- [ ] **R1-4.1.1** Create `FeeStructure` entity (`id`, `name`, `program`, `semester`, `amount` [BigDecimal], `labFeeComponent` [BigDecimal], `dueDate`)
- [ ] **R1-4.1.2** Create `FeePayment` entity (`id`, `student`, `feeStructure`, `amountPaid` [BigDecimal], `paymentDate`, `paymentMethod`, `transactionId`, `status`)
- [ ] **R1-4.1.3** Create fee service with payment processing logic
  - All monetary values as `BigDecimal` with `RoundingMode.HALF_UP`
- [ ] **R1-4.1.4** Create controllers:
  - `POST /api/v1/fees/structure` — Define fee structure (ROLE_ADMIN)
  - `POST /api/v1/fee-payments` — Record payment (ROLE_ADMIN)
  - `GET /api/v1/fee-payments/student/{studentId}` — Student fee history
  - `GET /api/v1/fee-payments/reports` — Revenue reports
- [ ] **R1-4.1.5** Create DTOs and Flyway migrations
- [ ] **R1-4.1.6** Write unit + controller tests (95% coverage)
- [ ] **R1-4.1.7** Create manual test cases: `docs/manual-test-cases/fee-management.md`

**Frontend:**
- [ ] **R1-4.1.8** Create `features/finance/` folder structure
- [ ] **R1-4.1.9** Create fee structure management component
- [ ] **R1-4.1.10** Create payment recording component
- [ ] **R1-4.1.11** Create high-density fee table with Material density `-2`
- [ ] **R1-4.1.12** Create finance routes (lazy-loaded)

### R1-M4.2 — Equipment & Inventory Management (Modules 7.3 & 7.4)

**Backend:**
- [ ] **R1-4.2.1** Create `Equipment` entity (`id`, `name`, `model`, `serialNumber`, `lab`, `category`, `status`, `purchaseDate`, `purchaseCost` [BigDecimal], `warrantyExpiry`)
- [ ] **R1-4.2.2** Create `EquipmentStatus` enum (`AVAILABLE`, `IN_USE`, `UNDER_REPAIR`, `DAMAGED`, `DISPOSED`)
- [ ] **R1-4.2.3** Create `Consumable` entity (`id`, `name`, `lab`, `quantity`, `unit`, `minimumStock`, `expiryDate`)
- [ ] **R1-4.2.4** Create `StockTransaction` entity (stock in/out tracking)
- [ ] **R1-4.2.5** Create equipment and inventory services with:
  - Equipment lifecycle tracking
  - Low stock alerts
  - Stock in/out recording
- [ ] **R1-4.2.6** Create controllers under `/api/v1/equipment` and `/api/v1/inventory`
- [ ] **R1-4.2.7** Create DTOs and Flyway migrations
- [ ] **R1-4.2.8** Write unit + controller tests (95% coverage)
- [ ] **R1-4.2.9** Create manual test cases: `docs/manual-test-cases/equipment-inventory.md`

**Frontend:**
- [ ] **R1-4.2.10** Create `features/equipment/` and `features/inventory/` folder structures
- [ ] **R1-4.2.11** Create equipment list with status badges and filters
- [ ] **R1-4.2.12** Create equipment form and detail components
- [ ] **R1-4.2.13** Create inventory stock dashboard
- [ ] **R1-4.2.14** Create equipment/inventory routes (lazy-loaded)

### R1-M4.3 — Maintenance & Repair (Module 7.7)

**Backend:**
- [ ] **R1-4.3.1** Create `MaintenanceRequest` entity (`id`, `equipment`, `requestedBy`, `description`, `priority`, `status`, `assignedTechnician`, `completedDate`, `repairCost` [BigDecimal])
- [ ] **R1-4.3.2** Create maintenance service with workflow (Request → Assign → In Progress → Complete)
- [ ] **R1-4.3.3** Create controllers under `/api/v1/maintenance`
- [ ] **R1-4.3.4** Create DTOs and Flyway migrations
- [ ] **R1-4.3.5** Write unit + controller tests (95% coverage)
- [ ] **R1-4.3.6** Create manual test cases: `docs/manual-test-cases/maintenance-management.md`

**Frontend:**
- [ ] **R1-4.3.7** Create `features/maintenance/` with list, form, and workflow components
- [ ] **R1-4.3.8** Create maintenance routes (lazy-loaded)

---

## 📝 R1-M5: Assessment & Reporting

> **Goal:** Examination management, lab evaluation, analytics dashboards, and accreditation reports.

### R1-M5.1 — Examination Management (Module 6)

**Backend:**
- [ ] **R1-5.1.1** Create `Examination` entity (`id`, `name`, `course`, `examType` [THEORY/PRACTICAL/VIVA], `date`, `duration`, `maxMarks`, `semester`)
- [ ] **R1-5.1.2** Create `ExamResult` entity (`id`, `examination`, `student`, `marksObtained`, `grade`, `status`)
- [ ] **R1-5.1.3** Create `LabContinuousEvaluation` entity (experiment-wise marks: record + viva + performance)
- [ ] **R1-5.1.4** Create exam service with:
  - GPA/CGPA calculation (including lab components)
  - Result processing and publishing
  - Lab practical exam management
- [ ] **R1-5.1.5** Create controllers under `/api/v1/examinations` and `/api/v1/results`
- [ ] **R1-5.1.6** Create DTOs and Flyway migrations
- [ ] **R1-5.1.7** Write unit + controller tests (95% coverage)
- [ ] **R1-5.1.8** Create manual test cases: `docs/manual-test-cases/examination-management.md`

**Frontend:**
- [ ] **R1-5.1.9** Create `features/examination/` folder structure
- [ ] **R1-5.1.10** Create exam scheduling and management components
- [ ] **R1-5.1.11** Create marks entry component (batch-wise)
- [ ] **R1-5.1.12** Create result view and transcript component
- [ ] **R1-5.1.13** Create examination routes (lazy-loaded)

### R1-M5.2 — Lab Reports & Analytics (Modules 7.10 & 13)

**Backend:**
- [ ] **R1-5.2.1** Create report service with aggregation queries for:
  - Lab utilization reports (usage %, peak hours, idle time)
  - Equipment utilization reports
  - Student lab performance reports
  - Lab expense reports
  - Safety incident reports
- [ ] **R1-5.2.2** Create accreditation report service:
  - CO/PO attainment calculation
  - Experiment completion rates
  - NBA/NAAC compliance data
- [ ] **R1-5.2.3** Create controllers under `/api/v1/reports`
- [ ] **R1-5.2.4** Create DTOs for report responses
- [ ] **R1-5.2.5** Write unit + controller tests (95% coverage)
- [ ] **R1-5.2.6** Create manual test cases: `docs/manual-test-cases/reports-analytics.md`

**Frontend:**
- [ ] **R1-5.2.7** Create `features/reports/` folder structure
- [ ] **R1-5.2.8** Create KPI dashboard with Material cards and charts
  - Lab utilization widget
  - Student performance widget
  - Attendance analytics widget
- [ ] **R1-5.2.9** Create detailed report pages with filters and data export (PDF, Excel, CSV)
- [ ] **R1-5.2.10** Create reports routes (lazy-loaded)

---

## ✅ Release 1 Definition of Done

Every task/milestone is considered **complete** only when ALL of the following are met:

| Criteria | Description |
|----------|-------------|
| **Code Complete** | All backend and frontend code is written and functional |
| **Backend Tests** | Unit + controller tests pass with ≥95% code coverage (JaCoCo) |
| **Build Passes** | `./gradlew check` (backend) and `ng build` (frontend) succeed |
| **Flyway Migration** | Database migration script created (for PostgreSQL profile) |
| **DTOs** | All data transfer objects are Java records with Jakarta validation |
| **Role-Based Access** | `@PreAuthorize` annotations applied to all controller methods |
| **Error Handling** | All errors return standardized `ErrorResponse` via GlobalExceptionHandler |
| **Manual Test Cases** | Manual test case document created in `docs/manual-test-cases/` |
| **Code Review** | Pull request reviewed and approved |
| **CHANGELOG** | `CHANGELOG.md` updated with the new feature |

---

## 📊 Release 1 Progress Tracking

| Milestone | Status | Progress |
|-----------|--------|----------|
| R1-M0: Project Scaffolding | ⬜ Not Started | 0% |
| R1-M1: Foundation & Identity | ⬜ Not Started | 0% |
| R1-M2: Core Academic & Lab Mapping | ⬜ Not Started | 0% |
| R1-M3: Operational Logistics | ⬜ Not Started | 0% |
| R1-M4: Finance & Asset Management | ⬜ Not Started | 0% |
| R1-M5: Assessment & Reporting | ⬜ Not Started | 0% |

---

## 📎 Deferred to Release 2

The following modules are **not** part of Release 1 and will be addressed in [Release 2](RELEASE_2_MILESTONES.md):

- Lab Safety & Compliance (Module 7.8)
- Communication & Portals (Module 12)
- Library Management (Module 9)
- Hostel Management (Module 10)
- Transport Management (Module 11)
- Research & Publication (Module 14)
- Placement & Career (Module 15)
- Event & Activity Management (Module 17)
- Online Learning / LMS (Module 19)
- Feedback & Survey (Module 21)
- Security & Compliance (Module 18)
- Mobile & Integration (Module 20)

---

> **Note:** This release tracker is aligned with the [Master Development Plan](DEVELOPMENT_PLAN.md) Phases 0–5. Each milestone builds on the previous one. R1-M0 is a prerequisite — no business logic until the skeleton is running.
