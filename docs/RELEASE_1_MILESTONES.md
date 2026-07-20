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
- [R1-M6: Post-R1 Academics Additions (INC Compliance, Promotion, Term Lifecycle)](#-r1-m6-post-r1-academics-additions-inc-compliance-promotion-term-lifecycle)
- [Release 1 Definition of Done](#-release-1-definition-of-done)
- [Release 1 Progress Tracking](#-release-1-progress-tracking)
- [Deferred to Release 2](#-deferred-to-release-2)

---

## 🎯 Release 1 Scope

| Milestone | Phase Origin | Modules Covered | Key Outcome |
|-----------|-------------|-----------------|-------------|
| **R1-M0** | Phase 0 | — | Runnable backend + frontend + Docker Compose |
| **R1-M1** | Phase 1 | Module 1 (partial) | Keycloak SSO, secured API, authenticated SPA shell |
| **R1-M2** | Phase 2 | Modules 1, 3, 4, 7.1 | Specialities, programs, labs, faculty, curriculum |
| **R1-M3** | Phase 3 | Modules 2, 5, 7.2 | Students, scheduling, attendance |
| **R1-M4** | Phase 4 | Modules 7.3, 7.4, 8, 16 | Fees, equipment, inventory, assets |
| **R1-M5** | Phase 5 | Modules 6, 7.5–7.10, 13 | Exams, lab evaluation, analytics, accreditation |
| **R1-M6** | Post-R1 | Modules 4, 5, 6 (extensions) | INC curriculum compliance, student promotion, term lifecycle alerting |

---

## 🧱 R1-M0: Project Scaffolding

> **Goal:** Create the runnable project skeleton — backend, frontend, and infrastructure — with no business logic.

### R1-M0.1 — Backend Project Initialization

- [x] **R1-0.1.1** Initialize Spring Boot 3.4 project with Gradle (Kotlin DSL)
  - Group: `com.cms`, Artifact: `cms-backend`
  - Java 21, Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring OAuth2 Resource Server
  - H2 (runtime), PostgreSQL (runtime), Flyway
  - JaCoCo plugin with 95% coverage threshold
- [x] **R1-0.1.2** Create `application.yml` with common settings
  - Default profile: `local`
  - Virtual threads enabled: `spring.threads.virtual.enabled: true`
  - API base path: `/api/v1`
- [x] **R1-0.1.3** Create `application-local.yml` — H2 in-memory config
  - `spring.jpa.hibernate.ddl-auto: create-drop`
  - Flyway disabled
  - H2 console enabled at `/h2-console`
- [x] **R1-0.1.4** Create `application-prod.yml` — PostgreSQL config
  - `spring.jpa.hibernate.ddl-auto: validate`
  - Flyway enabled
  - Database connection via environment variables
- [x] **R1-0.1.5** Create `application-test.yml` — Test profile
  - H2 in-memory, Flyway disabled, `create-drop`
- [x] **R1-0.1.6** Verify backend starts with `./gradlew bootRun` (local profile)
- [x] **R1-0.1.7** Verify `./gradlew check` passes (empty project baseline)

### R1-M0.2 — Frontend Project Initialization

- [x] **R1-0.2.1** Initialize Angular 21 project with Angular CLI
  - Standalone components (no NgModules)
  - SCSS + Tailwind CSS styling
  - SSR disabled initially (enable later)
- [x] **R1-0.2.2** Install Angular Material 21 with Material 3 theme
  - Configure `mat.theme()` with azure palette
  - Light and dark mode support
  - Custom `_theme.scss` file
- [x] **R1-0.2.3** Install Keycloak JS SDK (`keycloak-js`)
- [x] **R1-0.2.4** Set up folder-by-feature structure
  - `src/app/core/` — auth, guards, interceptors
  - `src/app/shared/` — components, pipes, directives
  - `src/app/features/` — feature folders (empty initially)
- [x] **R1-0.2.5** Configure Prettier (single quotes, 100 char width)
- [x] **R1-0.2.6** Verify frontend starts with `ng serve`

### R1-M0.3 — Infrastructure & Docker Compose

- [x] **R1-0.3.1** Create `infrastructure/keycloak/` directory with realm import config
- [x] **R1-0.3.2** Create `docker-compose.yml` with services:
  - Keycloak 26.0 (with realm auto-import)
  - PostgreSQL 17 (for prod profile)
- [x] **R1-0.3.3** Verify `docker compose up -d keycloak` starts Keycloak with `cms` realm — confirmed on 172.16.7.209: container healthy, `cms` realm responding, `cms-frontend`/`cms-backend` clients configured, users `devadmin`/`supportadmin`/`collegeadmin` enabled with credentials
- [x] **R1-0.3.4** Verify Keycloak admin console accessible and realm roles — admin console accessible at `https://172.16.7.209:8180`; realm roles (`ROLE_ADMIN` etc.) are N/A — design evolved to fully DB-driven permissions (`@perm.has()` resolves `preferred_username` → DB role → permission set); no Keycloak realm roles are used or needed anywhere in the codebase; login flow verified: port 80 → 301 HTTPS → nginx 443 (self-signed cert) → Keycloak with `X-Forwarded-Host: cms.nursing.sksh.ac.in` → JWT `iss: https://cms.nursing.sksh.ac.in/realms/cms` matches backend `KEYCLOAK_ALLOWED_ISSUERS`

---

## 🔐 R1-M1: Foundation & Identity

> **Goal:** Secure the platform — Keycloak SSO, JWT-based API security, authenticated Angular shell with role-based navigation.

### R1-M1.1 — Backend Security Configuration

- [x] **R1-1.1.1** Create `SecurityConfig` class
  - OAuth2 Resource Server with JWT validation (Keycloak issuer)
  - CORS configuration (whitelist frontend origin)
  - CSRF disabled for stateless API
  - Public endpoints: `/api/v1/health`, `/h2-console/**`
  - All other endpoints require authentication
- [x] **R1-1.1.2** Create JWT role converter for Keycloak realm roles
  - Map `realm_access.roles` from JWT to Spring Security authorities
- [x] **R1-1.1.3** Create `GlobalExceptionHandler` (`@RestControllerAdvice`)
  - `ResourceNotFoundException` → 404
  - `MethodArgumentNotValidException` → 400
  - `AccessDeniedException` → 403
  - Generic `Exception` → 500
  - Standard error response record: `ErrorResponse(int status, String message, Instant timestamp)`
- [x] **R1-1.1.4** Create health check endpoint (`GET /api/v1/health`)
- [x] **R1-1.1.5** Write unit tests for SecurityConfig and GlobalExceptionHandler
- [x] **R1-1.1.6** Create manual test cases: `docs/manual-test-cases/security-config.md`

### R1-M1.2 — Frontend Authentication

- [x] **R1-1.2.1** Create `AuthService` — initialize Keycloak, login/logout, token management
- [x] **R1-1.2.2** Create `AuthGuard` — protect routes, redirect unauthenticated users
- [x] **R1-1.2.3** Create `AuthInterceptor` — attach Bearer token to all API requests
- [x] **R1-1.2.4** Create `RoleGuard` — restrict routes by user role
- [x] **R1-1.2.5** Configure `app.config.ts` with providers (router, HTTP client, hydration)
- [x] **R1-1.2.6** Create manual test cases: `docs/manual-test-cases/authentication.md`

### R1-M1.3 — Application Shell & Navigation

- [x] **R1-1.3.1** Create `AppComponent` with Material sidenav layout
  - Top toolbar with app title, user menu, theme toggle
  - Side navigation with role-based menu items
- [x] **R1-1.3.2** Create `DashboardComponent` (landing page)
  - Placeholder dashboard cards for key metrics
  - "Lab Utilization" widget placeholder
- [x] **R1-1.3.3** Configure `app.routes.ts` with lazy-loaded feature routes
- [x] **R1-1.3.4** Implement light/dark theme toggle
- [x] **R1-1.3.5** Create manual test cases: `docs/manual-test-cases/app-shell.md`

---

## 🧪 R1-M2: Core Academic & Lab Mapping

> **Goal:** Define the institutional structure — specialities, programs, academic years, labs, faculty, and curriculum.

### R1-M2.1 — Speciality Management (Module 1)

**Backend:**
- [x] **R1-2.1.1** Create `Speciality` entity (`id`, `name`, `code`, `description`, `hodName`, `createdAt`, `updatedAt`)
- [x] **R1-2.1.2** Create `SpecialityRepository` (JpaRepository)
- [x] **R1-2.1.3** Create `SpecialityService` with CRUD operations
- [x] **R1-2.1.4** Create `SpecialityController` — REST endpoints:
  - `POST /api/v1/specialities` — Create (ROLE_ADMIN)
  - `GET /api/v1/specialities` — List all (authenticated)
  - `GET /api/v1/specialities/{id}` — Get by ID (authenticated)
  - `PUT /api/v1/specialities/{id}` — Update (ROLE_ADMIN)
  - `DELETE /api/v1/specialities/{id}` — Delete (ROLE_ADMIN)
- [x] **R1-2.1.5** Create `SpecialityRequest` and `SpecialityResponse` DTOs (Java records)
- [x] **R1-2.1.6** Create Flyway migration: `V1__create_specialities_table.sql`
- [x] **R1-2.1.7** Write unit + controller tests (95% coverage)
- [x] **R1-2.1.8** Create manual test cases: `docs/manual-test-cases/speciality-management.md`

**Frontend:**
- [x] **R1-2.1.9** Create `features/speciality/` folder structure
- [x] **R1-2.1.10** Create `SpecialityService` (API calls)
- [x] **R1-2.1.11** Create `SpecialityListComponent` — Material table with search, sort, pagination
- [x] **R1-2.1.12** Create `SpecialityFormComponent` — Create/Edit form with validation
- [x] **R1-2.1.13** Create speciality routes (lazy-loaded)

### R1-M2.2 — Program & Course Management (Module 1)

**Backend:**
- [x] **R1-2.2.1** Create `Program` entity (`id`, `name`, `code`, `programLevel`, `durationYears`, `specialities`)
- [x] **R1-2.2.2** Create `Course` entity (`id`, `name`, `code`, `credits`, `theoryCredits`, `labCredits`, `program`, `semester`)
- [x] **R1-2.2.3** Create repositories, services, controllers for Program and Course
- [x] **R1-2.2.4** Create DTOs (Java records) for Program and Course
- [x] **R1-2.2.5** Create Flyway migrations: `V2__create_programs_table.sql`, `V3__create_courses_table.sql`
- [x] **R1-2.2.6** Write unit + controller tests (95% coverage)
- [x] **R1-2.2.7** Create manual test cases: `docs/manual-test-cases/program-course-management.md`

**Frontend:**
- [x] **R1-2.2.8** Create `features/program/` and `features/course/` folder structures
- [x] **R1-2.2.9** Create list and form components for Program and Course
- [x] **R1-2.2.10** Create routes (lazy-loaded)

### R1-M2.3 — Academic Year & Calendar (Module 1)

**Backend:**
- [x] **R1-2.3.1** Create `AcademicYear` entity (`id`, `name`, `startDate`, `endDate`, `isCurrent`)
- [x] **R1-2.3.2** Create `Semester` entity (`id`, `name`, `academicYear`, `startDate`, `endDate`, `semesterNumber`)
- [x] **R1-2.3.3** Create repositories, services, controllers
- [x] **R1-2.3.4** Create DTOs and Flyway migrations
- [x] **R1-2.3.5** Write unit + controller tests (95% coverage)
- [x] **R1-2.3.6** Create manual test cases: `docs/manual-test-cases/academic-year-management.md`

**Frontend:**
- [x] **R1-2.3.7** Create `features/academic-year/` with list and form components
- [x] **R1-2.3.8** Create academic calendar view component

### R1-M2.4 — Lab Setup & Configuration (Module 7.1)

**Backend:**
- [x] **R1-2.4.1** Create `Lab` entity (`id`, `name`, `labType`, `speciality`, `building`, `roomNumber`, `capacity`, `status`)
- [x] **R1-2.4.2** Create `LabType` enum (`COMPUTER`, `PHYSICS`, `CHEMISTRY`, `ELECTRONICS`, `BIOLOGY`, `LANGUAGE`, `MECHANICAL`, `OTHER`)
- [x] **R1-2.4.3** Create `LabInChargeAssignment` entity (map faculty/technician to lab)
- [x] **R1-2.4.4** Create repositories, services, controllers
  - `POST /api/v1/labs` — Create lab (ROLE_ADMIN)
  - `GET /api/v1/labs` — List labs (authenticated)
  - `GET /api/v1/labs/{id}` — Get lab details (authenticated)
  - `PUT /api/v1/labs/{id}` — Update lab (ROLE_ADMIN, ROLE_LAB_INCHARGE)
  - `DELETE /api/v1/labs/{id}` — Delete lab (ROLE_ADMIN)
  - `POST /api/v1/labs/{id}/assign` — Assign in-charge (ROLE_ADMIN)
- [x] **R1-2.4.5** Create DTOs and Flyway migrations
- [x] **R1-2.4.6** Write unit + controller tests (95% coverage)
- [x] **R1-2.4.7** Create manual test cases: `docs/manual-test-cases/lab-setup.md`

**Frontend:**
- [x] **R1-2.4.8** Create `features/lab/` folder structure
- [x] **R1-2.4.9** Create `LabListComponent` — Material table with filters (by speciality, type, status)
- [x] **R1-2.4.10** Create `LabFormComponent` — Create/Edit form
- [x] **R1-2.4.11** Create `LabDetailComponent` — Lab detail view with assigned staff
- [x] **R1-2.4.12** Create lab routes (lazy-loaded)

### R1-M2.5 — Faculty Management (Module 3)

**Backend:**
- [x] **R1-2.5.1** Create `Faculty` entity (`id`, `employeeCode`, `firstName`, `lastName`, `email`, `phone`, `speciality`, `designation`, `specialization`, `labExpertise`, `joiningDate`)
- [x] **R1-2.5.2** Create repositories, services, controllers
  - CRUD endpoints under `/api/v1/faculty`
  - Lab teaching assignment endpoints
- [x] **R1-2.5.3** Create DTOs and Flyway migrations
- [x] **R1-2.5.4** Write unit + controller tests (95% coverage)
- [x] **R1-2.5.5** Create manual test cases: `docs/manual-test-cases/faculty-management.md`

**Frontend:**
- [x] **R1-2.5.6** Create `features/faculty/` with list, form, and detail components
- [x] **R1-2.5.7** Create faculty routes (lazy-loaded)

### R1-M2.6 — Curriculum & Lab-Curriculum Mapping (Module 4)

**Backend:**
- [x] **R1-2.6.1** Create `Syllabus` entity (course syllabus with theory + lab components)
- [x] **R1-2.6.2** Create `Experiment` entity (`id`, `name`, `course`, `experimentNumber`, `description`, `learningOutcomes`)
- [x] **R1-2.6.3** Create `LabCurriculumMapping` entity (map experiments to Course Outcomes / Program Outcomes)
- [x] **R1-2.6.4** Create repositories, services, controllers
- [x] **R1-2.6.5** Create DTOs and Flyway migrations
- [x] **R1-2.6.6** Write unit + controller tests (95% coverage)
- [x] **R1-2.6.7** Create manual test cases: `docs/manual-test-cases/curriculum-management.md`

**Frontend:**
- [x] **R1-2.6.8** Create `features/curriculum/` with syllabus and experiment components
- [x] **R1-2.6.9** Create CO/PO mapping matrix UI component

---

## 📅 R1-M3: Operational Logistics

> **Goal:** Student lifecycle, lab scheduling, and attendance tracking.

### R1-M3.1 — Student Management (Module 2)

**Backend:**
- [x] **R1-3.1.1** Create `Student` entity with expanded fields:
  - Core: `id`, `rollNumber`, `firstName`, `lastName`, `email`, `phone`, `program`, `semester`, `admissionDate`, `labBatch`, `status`
  - Personal: `dateOfBirth` (LocalDate), `gender` (Enum: MALE/FEMALE/OTHER), `aadharNumber` (String, encrypted at rest)
  - Demographics: `nationality`, `religion`, `communityCategory` (Enum: SC/ST/BC/MBC/DNC/OC/OTHERS), `caste`, `bloodGroup` (Enum: A_POSITIVE/A_NEGATIVE/B_POSITIVE/B_NEGATIVE/O_POSITIVE/O_NEGATIVE/AB_POSITIVE/AB_NEGATIVE)
  - Family: `fatherName`, `motherName`, `parentMobile`
  - Embedded `Address`: `postalAddress`, `street`, `city`, `district`, `state`, `pincode`
- [x] **R1-3.1.2** Create `Admission` entity with full field spec:
  - `id`, `student` (FK → Student), `academicYearFrom` (Integer), `academicYearTo` (Integer), `applicationDate` (LocalDate)
  - `status` (Enum: DRAFT/SUBMITTED/UNDER_REVIEW/DOCUMENTS_PENDING/APPROVED/REJECTED)
  - `declarationPlace`, `declarationDate` (LocalDate), `parentConsentGiven` (Boolean), `applicantConsentGiven` (Boolean)
- [x] **R1-3.1.2a** Create `AcademicQualification` entity:
  - `id`, `admission` (FK → Admission), `qualificationType` (Enum: SSLC/HSC/DIPLOMA/DEGREE/OTHER)
  - `schoolName`, `majorSubject`, `totalMarks` (Integer), `percentage` (BigDecimal), `monthAndYearOfPassing` (String), `universityOrBoard`
  - One admission → many qualifications
- [x] **R1-3.1.2b** Create `AdmissionDocument` entity:
  - `id`, `admission` (FK → Admission), `documentType` (Enum: TENTH_MARKSHEET/ELEVENTH_MARKSHEET/TWELFTH_MARKSHEET/TRANSFER_CERTIFICATE/COMMUNITY_CERTIFICATE/INCOME_CERTIFICATE/NATIVITY_CERTIFICATE/MIGRATION_CERTIFICATE/FIRST_GRADUATE_CERTIFICATE/PASSPORT_PHOTO/SIGNED_AFFIDAVIT/UNDERTAKING_DOCUMENT/AADHAR_CARD/MEDICAL_FITNESS/ELIGIBILITY_CERTIFICATE)
  - `fileName`, `storageKey`, `uploadedAt` (LocalDateTime)
  - `originalSubmitted` (Boolean), `verifiedBy` (String, nullable), `verifiedAt` (LocalDateTime, nullable)
  - `verificationStatus` (Enum: NOT_UPLOADED/UPLOADED/VERIFIED/REJECTED)
- [x] **R1-3.1.2c** Create all enums in `com.cms.model.enums`:
  - `Gender`: MALE, FEMALE, OTHER
  - `CommunityCategory`: SC, ST, BC, MBC, DNC, OC, OTHERS
  - `BloodGroup`: A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, O_POSITIVE, O_NEGATIVE, AB_POSITIVE, AB_NEGATIVE
  - `QualificationType`: SSLC, HSC, DIPLOMA, DEGREE, OTHER
  - `AdmissionStatus`: DRAFT, SUBMITTED, UNDER_REVIEW, DOCUMENTS_PENDING, APPROVED, REJECTED
  - `DocumentType`: TENTH_MARKSHEET, ELEVENTH_MARKSHEET, TWELFTH_MARKSHEET, TRANSFER_CERTIFICATE, COMMUNITY_CERTIFICATE, INCOME_CERTIFICATE, NATIVITY_CERTIFICATE, MIGRATION_CERTIFICATE, FIRST_GRADUATE_CERTIFICATE, PASSPORT_PHOTO, SIGNED_AFFIDAVIT, UNDERTAKING_DOCUMENT, AADHAR_CARD, MEDICAL_FITNESS, ELIGIBILITY_CERTIFICATE
  - `DocumentVerificationStatus`: NOT_UPLOADED, UPLOADED, VERIFIED, REJECTED
- [x] **R1-3.1.3** Create repositories, services, controllers
  - CRUD endpoints under `/api/v1/students`
  - Admission workflow: `/api/v1/admissions`
  - Academic qualifications: `/api/v1/admissions/{id}/qualifications`
  - Document upload/verification: `/api/v1/admissions/{id}/documents`
  - Document checklist status: `GET /api/v1/admissions/{id}/documents/checklist`
  - Lab batch assignment during enrollment
- [x] **R1-3.1.4** Create DTOs and Flyway migrations
- [x] **R1-3.1.5** Write unit + controller tests (95% coverage)
- [x] **R1-3.1.6** Create manual test cases: `docs/manual-test-cases/student-management.md`

**Frontend:**
- [x] **R1-3.1.7** Create `features/student/` with list, form, detail, and admission components
  - Multi-step admission form matching the physical admission application form:
    - Step 1: Personal Information (name, DOB, gender, Aadhar, nationality, religion, community, caste, blood group)
    - Step 2: Parent/Guardian Details (father's name, mother's name, parent mobile)
    - Step 3: Address (postal address, street, city, district, state, pincode)
    - Step 4: Academic Qualifications (dynamic table for SSLC, HSC, and additional rows)
    - Step 5: Document Upload (checklist with upload slots for all 15 document types, showing verification status)
    - Step 6: Declaration & Consent (checkboxes for parent and applicant consent, place, date)
    - Step 7: Review & Submit
  - Document upload component with drag-and-drop and progress indicators
  - Admin document verification component (mark originals received, verify/reject each document)
- [x] **R1-3.1.8** Create student directory with search and filters
- [x] **R1-3.1.9** Create student routes (lazy-loaded)

### R1-M3.2 — Lab Scheduling & Timetable (Module 7.2)

**Backend:**
- [x] **R1-3.2.1** Create `LabSchedule` entity (`id`, `lab`, `course`, `faculty`, `batch`, `dayOfWeek`, `startTime`, `endTime`, `semester`)
- [x] **R1-3.2.2** Create `LabSlot` entity for reusable time slot definitions
- [x] **R1-3.2.3** Create scheduling service with conflict detection logic
  - Check lab availability, faculty availability, batch conflicts
- [x] **R1-3.2.4** Create controllers:
  - `POST /api/v1/lab-schedules` — Create schedule (ROLE_ADMIN, ROLE_FACULTY)
  - `GET /api/v1/lab-schedules` — List with filters (lab, faculty, batch, day)
  - Conflict detection endpoint
- [x] **R1-3.2.5** Create DTOs and Flyway migrations
- [x] **R1-3.2.6** Write unit + controller tests (95% coverage)
- [x] **R1-3.2.7** Create manual test cases: `docs/manual-test-cases/lab-scheduling.md`

**Frontend:**
- [x] **R1-3.2.8** Create `features/lab-schedule/` folder structure
- [ ] **R1-3.2.9** Create lab calendar view (weekly timetable grid) — **correction 2026-07-18:** was checked off but never actually built; `lab-schedule-list.component.ts` is a plain sortable Material table, not a week-grid calendar. Left unchecked pending real implementation or a decision to fold it into the R1-M6.4 timetable-generation effort instead of building it standalone.
- [x] **R1-3.2.10** Create schedule form with conflict alerts
- [x] **R1-3.2.11** Create lab schedule routes (lazy-loaded)

### R1-M3.3 — Attendance Management (Module 5)

**Backend:**
- [x] **R1-3.3.1** Create `Attendance` entity (`id`, `student`, `course`, `date`, `status`, `type` [THEORY/LAB])
- [x] **R1-3.3.2** Create `LabAttendance` entity (extends with `experiment`, `labBatch`, `labSession`)
- [x] **R1-3.3.3** Create attendance service with:
  - Mark attendance (individual + bulk)
  - Attendance percentage calculation
  - Low attendance alert logic
- [x] **R1-3.3.4** Create controllers:
  - `POST /api/v1/attendance` — Mark attendance (ROLE_FACULTY, ROLE_LAB_INCHARGE)
  - `GET /api/v1/attendance/reports` — Attendance reports with filters
  - `GET /api/v1/attendance/alerts` — Low attendance alerts
- [x] **R1-3.3.5** Create DTOs and Flyway migrations
- [x] **R1-3.3.6** Write unit + controller tests (95% coverage)
- [x] **R1-3.3.7** Create manual test cases: `docs/manual-test-cases/attendance-management.md`

**Frontend:**
- [x] **R1-3.3.8** Create `features/attendance/` folder structure
- [x] **R1-3.3.9** Create attendance marking component (batch-wise view)
- [x] **R1-3.3.10** Create attendance report component with charts
- [x] **R1-3.3.11** Create attendance routes (lazy-loaded)

---

## 💰 R1-M4: Finance & Asset Management

> **Goal:** Fee lifecycle, lab equipment/inventory tracking, and institutional asset management.

### R1-M4.1 — Fee Structure & Collection (Module 8)

> **Business Requirements:** See [BR-1](BUSINESS_REQUIREMENTS.md#br-1-fee-structure--academic-year), [BR-2](BUSINESS_REQUIREMENTS.md#br-2-year-wise-fee-boxes-per-program-duration), and [BR-30](BUSINESS_REQUIREMENTS.md#br-30-multi-dimension-fee-structure-quota--state--gender--student-type) (multi-dimension extension).

### R1-M4.1b — Multi-Dimension Fee Structure (BR-30)

> **Business Requirement:** [BR-30](BUSINESS_REQUIREMENTS.md#br-30-multi-dimension-fee-structure-quota--state--gender--student-type)

**Backend:**
- [x] **R1-4.1b.1** Create `AdmissionQuota` enum (`MANAGEMENT`, `COUNSELLING`)
- [x] **R1-4.1b.2** Create `FeeState` master entity + repository + seeder (Tamil Nadu = default, Other State = fallback)
- [x] **R1-4.1b.3** Create `FeeStructureGroup` entity with unique constraint on `(program, academicYear, course, quota, feeState, gender, studentType)`
- [x] **R1-4.1b.4** Refactor `FeeStructure` — replaced direct program/academicYear/course FKs with `feeStructureGroup` FK
- [x] **R1-4.1b.5** Add `admissionQuota` and `feeState` FK fields to `Enquiry`
- [x] **R1-4.1b.6** Rewrite `FeeStructureService` — group-based create/update/delete; `findForEnquiry()` with Other State fallback
- [x] **R1-4.1b.7** Add `GET /api/v1/fee-states` endpoint
- [x] **R1-4.1b.8** Add `GET /api/v1/fee-structures/guideline` endpoint
- [x] **R1-4.1b.9** Flyway migrations V165–V168 (fee_states, fee_structure_groups, fee_structures refactor, enquiry columns)

**Frontend:**
- [x] **R1-4.1b.10** Update `finance.model.ts` — add `FeeState`, update `BulkFeeStructureRequest`, `FeeStructure`, `GroupedFeeStructure` with 4 new dimension fields
- [x] **R1-4.1b.11** Update fee structure admin form — 7-field Combination Picker (AY + Program + Course + 4 dimensions)
- [x] **R1-4.1b.12** Update fee structure list — 4 new filter dropdowns, dimension badges on cards and new table columns
- [x] **R1-4.1b.13** Update enquiry form — Quota dropdown; state auto-derived from address; fee re-loads on any dimension change; submission blocked when no fee configured
- [x] **R1-4.1b.14** Update fee finalization — Quota column in list table; Gender/Quota/State info rows; `initYearRows` fallback uses guideline endpoint
- [x] **R1-4.1b.15** Update manual test cases: fee-structure-management.md, fee-structure-classification-and-enquiry-flow.md, fee-finalization-payment.md, student-fee-workflow.md

**Review-pass fixes (Phases 3–5 audit):**
- [x] **R1-4.1b.16** Enquiry form: gender `(change)` wired to `onDimensionChange()`; fee banner shows contextual "what's missing" text; `updateCourseValidator` triggers fee load for no-course programs; `tryLoadFeeGuideline` guard prevents false "not found" when course not yet selected
- [x] **R1-4.1b.17** `applyAuthoritativeFees` null-guard: removed `courseId == null` check — programs without courses correctly have fee calculated
- [x] **R1-4.1b.18** Fee finalization: Quota filter dropdown in toolbar; `filteredEnquiries` includes quota in text search; `applyEqualSplitFallback` uses `program.durationYears` (not hardcoded 4); `discountReason` signal synced to FormControl; "Fee Basis" section divider added to info panel
- [x] **R1-4.1b.19** API: `applyAuthoritativeFees` error shows fee state name not raw ID; `GET /fee-structures/grouped` extended with `quota`, `feeStateId`, `gender`, `studentType` filter params; `DataIntegrityViolationException` handler improved for fee-structure-specific constraint names
- [x] **R1-4.1b.20** Update manual test cases: TC-FSCLS-106/107/108, TC-FIN-103/104/105, TC-ENQ-BR30-004, TC-FEE-106/107

**Backend:**
- [x] **R1-4.1.1** Create `FeeStructure` entity (`id`, `name`, `program`, `academicYear`, `feeType`, `amount` [BigDecimal], `isMandatory`)
  - Fee structure is scoped to **program + academic year** — fees may vary year to year (BR-1)
- [x] **R1-4.1.1a** Create `FeeStructureYearAmount` entity (`id`, `feeStructure`, `yearNumber`, `yearLabel`, `amount` [BigDecimal])
  - Year-wise fee boxes generated based on program `durationYears` (BR-2)
  - Labels: "First Year", "Second Year", etc.
- [x] **R1-4.1.2** Create `FeePayment` entity (`id`, `student`, `feeStructure`, `amountPaid` [BigDecimal], `paymentDate`, `paymentMethod`, `transactionId`, `status`)
- [x] **R1-4.1.3** Create fee service with payment processing logic
  - All monetary values as `BigDecimal` with `RoundingMode.HALF_UP`
- [x] **R1-4.1.4** Create controllers:
  - `POST /api/v1/fees/structure` — Define fee structure (ROLE_ADMIN)
  - `POST /api/v1/fee-payments` — Record payment (ROLE_ADMIN)
  - `GET /api/v1/fee-payments/student/{studentId}` — Student fee history
  - `GET /api/v1/fee-payments/reports` — Revenue reports
- [x] **R1-4.1.5** Create DTOs and Flyway migrations
- [x] **R1-4.1.6** Write unit + controller tests (95% coverage)
- [x] **R1-4.1.7** Create manual test cases: `docs/manual-test-cases/fee-management.md`

**Frontend:**
- [x] **R1-4.1.8** Create `features/finance/` folder structure
- [x] **R1-4.1.9** Create fee structure management component
- [x] **R1-4.1.9a** Enhance fee structure form with dynamic year-wise fee input boxes based on program `durationYears` (BR-2)
- [x] **R1-4.1.10** Create payment recording component
- [x] **R1-4.1.11** Create high-density fee table with Material density `-2`
- [x] **R1-4.1.12** Create finance routes (lazy-loaded)

### R1-M4.1c — Excess Bank Payment with Auto-Generated Refund (BR-36)

> **Business Requirement:** [BR-36](BUSINESS_REQUIREMENTS.md#br-36-excess-bank-payment-with-auto-generated-non-rejectable-refund)

**Backend:**
- [x] **R1-4.1c.1** Add `CollectPaymentRequest.allowExcess` (opt-in flag); honored only by `PaymentCollectionService.collectAdvancePayment` (per-student Advance Payment flow, not the term-gated bulk Collect Payment list or enquiry payments)
- [x] **R1-4.1c.2** Bypass the total-outstanding cap only when `allowExcess=true`, payment mode is `DEMAND_DRAFT`/`BANK_TRANSFER`, and the caller holds `FEE_COLLECT_EXCESS` (checked server-side via `PermSecurityBean`, not just frontend)
- [x] **R1-4.1c.3** Receipt records the full amount physically received (not just the portion applied to fees); excess is auto-carved into a new `FeeRefund` (`source = AUTO_EXCESS`) via `FeeRefundService.createAutoExcessRefund`
- [x] **R1-4.1c.4** `FeeRefund.source` column (`MANUAL` | `AUTO_EXCESS`); `rejectRefund()` hard-blocks rejecting `AUTO_EXCESS` refunds; `approveRefund()`/`completeOneBookRefund()` skip installment/enquiry-payment soft-flagging for `AUTO_EXCESS` (the excess was never allocated to a fee)
- [x] **R1-4.1c.5** Flyway migration V259 — `fee_refunds.source` column + `FEE_COLLECT_EXCESS` permission seed with DEV_ADMIN/SUPPORT_ADMIN catch-all sync
- [x] **R1-4.1c.6** Update existing tests for the new constructor dependencies (`PaymentCollectionServiceTest`, `StudentFeeControllerTest`, `StudentImportService`)
- [x] **R1-4.1c.7** Create manual test cases: `docs/manual-test-cases/excess-payment-auto-refund.md` — 16 TCs covering happy path, permission/payment-mode gating, no upper cap, form reactivity, immutability, installment-integrity on approval, and the one-active-refund-per-receipt interaction

**Frontend:**
- [x] **R1-4.1c.8** Student Fee Detail → Advance Payment form: "Allow payment above total outstanding (bank excess)" checkbox, gated by `FEE_COLLECT_EXCESS` + DD/Bank Transfer mode; relaxes `maxOutstandingValidator`; live excess preview + confirmation modal breakdown
- [x] **R1-4.1c.9** Fee Refund List: "Auto" source chip (list + detail panel) for `AUTO_EXCESS` refunds; Reject action hidden for them (backend also blocks it — defense in depth)

**Explicitly out of scope (see BR-36):** general partial refunds (refunding less than the full receipt amount), and a dedicated payment/receipt cancellation ("void") flow — neither exists in the system and this feature does not introduce them.

### R1-M4.1a — Referral Type Master (Module 8)

> **Business Requirements:** See [BR-4](BUSINESS_REQUIREMENTS.md#br-4-referral-type-master).

**Backend:**
- [x] **R1-4.1a.1** Create `ReferralType` entity (`id`, `name`, `code`, `commissionAmount`, `hasCommission`, `description`, `isActive`, `isSystemDefined`)
  - Replaces hardcoded `EnquirySource` enum for referral categorization
  - Default types: WALK_IN, PHONE, ONLINE, AGENT_REFERRAL, STAFF, ALUMNI, PARENT, ADVERTISEMENT
- [x] **R1-4.1a.2** Create ReferralType service with CRUD operations
- [x] **R1-4.1a.3** Create controllers under `/referral-types` (GET, POST, PUT, DELETE, PATCH /status, /name-exists, /code-exists, /page)
- [x] **R1-4.1a.4** Create DTOs and Flyway migrations (seed default referral types — V37_1, V41, V54, V107, V108)
- [x] **R1-4.1a.5** Write unit + controller tests (95% coverage)
- [x] **R1-4.1a.6** Create manual test cases: `docs/manual-test-cases/referral-type-management.md`

**Frontend:**
- [x] **R1-4.1a.7** Create `features/referral-type/` with list (server-side pagination, card+table toggle) and form (uniqueness validators, preview card)
- [x] **R1-4.1a.8** Create referral type routes (lazy-loaded) + nav entry in sidebar

### R1-M4.1b — Enquiry-to-Admission Workflow Enhancement

> **Business Requirements:** See [BR-3](BUSINESS_REQUIREMENTS.md#br-3-fee-structure-guideline-on-enquiry-screen), [BR-5](BUSINESS_REQUIREMENTS.md#br-5-referral-guideline-amount--final-fee-calculation), [BR-6](BUSINESS_REQUIREMENTS.md#br-6-admin-fee-finalization-workflow), [BR-7](BUSINESS_REQUIREMENTS.md#br-7-payment-collection-by-accounting-team), [BR-8](BUSINESS_REQUIREMENTS.md#br-8-enquiry-status-workflow), [BR-9](BUSINESS_REQUIREMENTS.md#br-9-document-submission), [BR-10](BUSINESS_REQUIREMENTS.md#br-10-convert-enquiry-to-student), [BR-11](BUSINESS_REQUIREMENTS.md#br-11-student-explorer-with-filters).

**Backend:**
- [x] **R1-4.1b.1** Enhance `Enquiry` entity with fee guideline fields:
  - `feeGuidelineTotal`, `referralTypeId` (FK → ReferralType), `referralAdditionalAmount`, `finalCalculatedFee`
  - Year-wise guideline breakdown stored as related records
- [x] **R1-4.1b.2** Update `EnquiryStatus` enum to reflect new workflow:
  - ENQUIRED, INTERESTED, NOT_INTERESTED, FEES_FINALIZED, FEES_PAID, PARTIALLY_PAID, DOCUMENTS_SUBMITTED, DOCUMENTS_VERIFIED, ADMITTED, CLOSED
- [x] **R1-4.1b.3** Enhance enquiry service:
  - Fee guideline lookup by program + current academic year (BR-3)
  - Referral additional amount calculation (BR-5)
  - Final fee computation (BR-5)
  - Automatic status transitions (BR-8)
- [x] **R1-4.1b.4** Create fee finalization endpoints for admin (BR-6):
  - `POST /api/v1/enquiries/{id}/finalize-fees` — Admin finalizes fee structure
  - `GET /api/v1/enquiries/{id}/fee-guideline` — Get fee guideline for enquiry's program
- [x] **R1-4.1b.5** Enhance payment collection for enquiry-based fees (BR-7)
- [x] **R1-4.1b.6** Create submit documents tracking for enquiries (BR-9):
  - `POST /api/v1/enquiries/{id}/documents` — Submit document
  - `GET /api/v1/enquiries/{id}/documents` — List submitted documents
  - `PATCH /api/v1/enquiries/{id}/documents/{docId}/verify` — Verify document
- [x] **R1-4.1b.7** Enhance complete-admission conversion to require DOCUMENTS_VERIFIED status (BR-10)
- [x] **R1-4.1b.8** Create student explorer endpoint with filters (BR-11):
  - `GET /api/v1/students/explorer` — Search with filters (program, speciality, academicYear, semester, status, feeStatus, search)
- [x] **R1-4.1b.9** Create DTOs and Flyway migrations for all new fields/tables
- [x] **R1-4.1b.10** Write unit + controller tests (95% coverage)
- [x] **R1-4.1b.11** Update manual test cases: `docs/manual-test-cases/enquiry-management.md`

**Frontend:**
- [x] **R1-4.1b.12** Enhance enquiry form with fee structure guideline panel (BR-3)
  - Side panel showing fee structure for selected program in current academic year
  - Year-wise breakdown displayed dynamically
- [x] **R1-4.1b.13** Add referral type dropdown (from master) with conditional additional amount box (BR-4, BR-5)
  - Show additional amount box only when referral type has non-zero guidelineValue
  - Calculate and display final fee
- [x] **R1-4.1b.14** Create admin fee finalization screen (BR-6)
  - Display enquiry guideline values as starting point
  - Allow admin to adjust fees, provide discounts, modify year-wise split
- [x] **R1-4.1b.15** Enhance payment collection screen for enquiry-based workflow (BR-7)
- [x] **R1-4.1b.16** Create submit documents tracking component (BR-9)
- [x] **R1-4.1b.17** Update enquiry list with new status workflow and automatic transitions (BR-8)
- [x] **R1-4.1b.18** Create student explorer screen with comprehensive filters (BR-11)

### R1-M4.2 — Equipment & Inventory Management (Modules 7.3 & 7.4)

**Backend:**
- [x] **R1-4.2.1** Create `Equipment` entity (`id`, `name`, `model`, `serialNumber`, `lab`, `category`, `status`, `purchaseDate`, `purchaseCost` [BigDecimal], `warrantyExpiry`)
- [x] **R1-4.2.2** Create `EquipmentStatus` enum (`AVAILABLE`, `IN_USE`, `UNDER_REPAIR`, `DAMAGED`, `DISPOSED`)
- [x] **R1-4.2.3** Create `Consumable` entity (`id`, `name`, `lab`, `quantity`, `unit`, `minimumStock`, `expiryDate`)
- [x] **R1-4.2.4** Create `StockTransaction` entity (stock in/out tracking)
- [x] **R1-4.2.5** Create equipment and inventory services with:
  - Equipment lifecycle tracking
  - Low stock alerts
  - Stock in/out recording
- [x] **R1-4.2.6** Create controllers under `/api/v1/equipment` and `/api/v1/inventory`
- [x] **R1-4.2.7** Create DTOs and Flyway migrations
- [x] **R1-4.2.8** Write unit + controller tests (95% coverage)
- [x] **R1-4.2.9** Create manual test cases: `docs/manual-test-cases/equipment-inventory.md`

**Frontend:**
- [x] **R1-4.2.10** Create `features/equipment/` and `features/inventory/` folder structures
- [x] **R1-4.2.11** Create equipment list with status badges and filters
- [x] **R1-4.2.12** Create equipment form and detail components
- [x] **R1-4.2.13** Create inventory stock dashboard
- [x] **R1-4.2.14** Create equipment/inventory routes (lazy-loaded)

### R1-M4.3 — Maintenance & Repair (Module 7.7)

**Backend:**
- [x] **R1-4.3.1** Create `MaintenanceRequest` entity (`id`, `equipment`, `requestedBy`, `description`, `priority`, `status`, `assignedTechnician`, `completedDate`, `repairCost` [BigDecimal])
- [x] **R1-4.3.2** Create maintenance service with workflow (Request → Assign → In Progress → Complete)
- [x] **R1-4.3.3** Create controllers under `/api/v1/maintenance`
- [x] **R1-4.3.4** Create DTOs and Flyway migrations
- [x] **R1-4.3.5** Write unit + controller tests (95% coverage)
- [x] **R1-4.3.6** Create manual test cases: `docs/manual-test-cases/maintenance-management.md`

**Frontend:**
- [x] **R1-4.3.7** Create `features/maintenance/` with list, form, and workflow components
- [x] **R1-4.3.8** Create maintenance routes (lazy-loaded)

### R1-M4.4 — Commission Explorer & OneBook Payment Gateway Integration

> **Business Requirements:** See [BR-34](BUSINESS_REQUIREMENTS.md#br-34-onebook-payment-gateway-integration).

**Backend:**
- [x] **R1-4.4.1** Create `StaffReferrer` entity, CRUD, and permissions
- [x] **R1-4.4.2** Create `CommissionPayout` model (replaces `AgentCommissionPayout`) and Commission Explorer service/controller — push-to-OneBook workflow, approve/reject (with required reason, reopenable) split from settle, full-amount-only payout settlement
- [x] **R1-4.4.3** Create `OneBookConfigService` and `OneBookIntegrationService` — outbound payment-register push for commission, fee refund, and scholarship disbursement against OneBook's real published API contract (JWT auth via `/authserver/api/auth`; `payment-registers-add-from-other-applications`; invoice/document-shaped payload with generated invoice numbers); bank-detail guard blocking pushes with missing recipient bank details
- [x] **R1-4.4.4** Create `OneBookWebhookController`/`OneBookWebhookService` — two real inbound callbacks (`/webhooks/onebook/posting-track-update`, `/webhooks/onebook/posting-track-completion`), secret-header authenticated, correlated by the generated `invoiceNumber`, each entry processed independently
- [x] **R1-4.4.5** Add bank detail fields (6) to `Student` entity/DTOs/service required by the bank-detail guard
- [x] **R1-4.4.6** Add `COMMISSION_SETTLE` permission separate from `COMMISSION_MANAGE`; `REJECTED` `CommissionPaymentStatus` value with rejection reason/by/at on `Enquiry`
- [x] **R1-4.4.7** Flyway migrations V219–V231, V236
- [x] **R1-4.4.7b** Add `ApplicationNumberSequenceService.nextCommissionNumber`/`nextDisbursementNumber`; `commission_number`/`disbursement_number` columns on `Enquiry`/`ScholarshipDisbursement`
- [x] **R1-4.4.8** Surface scholarship OneBook rejection/failure status — new `OneBookPaymentSummaryResponse` DTO + `GET /{id}/onebook-payments` endpoint; student detail Scholarships tab shows a OneBook Payments panel with PENDING/TRANSMITTED/PAID/FAILED chips; FAILED rows surface `errorMessage` with icon + tooltip truncation
- [x] **R1-4.4.9** Write unit + controller tests — `OneBookWebhookServiceTest` (17), `OneBookWebhookControllerTest` (9), `OneBookIntegrationServiceTest` (7 guard-clause), `ScholarshipApplicationControllerTest` (4); also fixed pre-existing broken tests: `ApplicationNumberSequenceServiceTest` rewritten for `NumberSeriesDefinition`/`NumberSequenceCounter` architecture, `FeeReportServiceTest` updated with missing `PaymentReceiptRepository`
- [x] **R1-4.4.10** Create manual test cases: `docs/manual-test-cases/onebook-integration.md` — 20 TCs covering config, scholarship/commission/refund pushes, webhook callbacks (track-update + track-completion), UI surface, and E2E full cycle
- [x] **R1-4.4.10b** Out of scope: OneBook edit/delete/fetch-by-id register endpoints and Supplier Master Sync — no post-push cancellation flow exists in OneCMS to call them from, and supplier sync is pharmacy-only (see BR-34)

**Frontend:**
- [x] **R1-4.4.11** Create `IntegrationsSettingsComponent` (Settings → Integrations) for OneBook credentials with show/hide password
- [x] **R1-4.4.12** Create Commission Explorer screen with side-flyout payout recording, approve/reject/settle actions gated by permission
- [x] **R1-4.4.13** Add OneBook push button with retry + status tracking to Fee Refund List

> **Status:** Fully built and wired end-to-end; `onebook.enabled` defaults to `false` and all credential keys are seeded blank, so the integration is dormant until a college's real OneBook org/branch/login is entered.

---

## 📝 R1-M5: Assessment & Reporting

> **Goal:** Examination management, lab evaluation, analytics dashboards, and accreditation reports.

### R1-M5.1 — Examination Management (Module 6)

**Backend:**
- [x] **R1-5.1.1** Create `Examination` entity (`id`, `name`, `course`, `examType` [THEORY/PRACTICAL/VIVA], `date`, `duration`, `maxMarks`, `semester`)
- [x] **R1-5.1.2** Create `ExamResult` entity (`id`, `examination`, `student`, `marksObtained`, `grade`, `status`)
- [x] **R1-5.1.3** Create `LabContinuousEvaluation` entity (experiment-wise marks: record + viva + performance)
- [x] **R1-5.1.4** Create exam service with:
  - GPA/CGPA calculation (including lab components)
  - Result processing and publishing
  - Lab practical exam management
- [x] **R1-5.1.5** Create controllers under `/api/v1/examinations` and `/api/v1/results`
- [x] **R1-5.1.6** Create DTOs and Flyway migrations
- [x] **R1-5.1.7** Write unit + controller tests (95% coverage)
- [x] **R1-5.1.8** Create manual test cases: `docs/manual-test-cases/examination-management.md`

**Frontend:**
- [x] **R1-5.1.9** Create `features/examination/` folder structure
- [x] **R1-5.1.10** Create exam scheduling and management components
- [x] **R1-5.1.11** Create marks entry component (batch-wise)
- [x] **R1-5.1.12** Create result view and transcript component
- [x] **R1-5.1.13** Create examination routes (lazy-loaded)

### R1-M5.2 — Lab Reports & Analytics (Modules 7.10 & 13)

**Backend:**
- [x] **R1-5.2.1** Create report service with aggregation queries for:
  - Lab utilization reports (usage %, peak hours, idle time)
  - Equipment utilization reports
  - Student lab performance reports
  - Lab expense reports
  - Safety incident reports
- [x] **R1-5.2.2** Create accreditation report service:
  - CO/PO attainment calculation
  - Experiment completion rates
  - NBA/NAAC compliance data
- [x] **R1-5.2.3** Create controllers under `/api/v1/reports`
- [x] **R1-5.2.4** Create DTOs for report responses
- [x] **R1-5.2.5** Write unit + controller tests (95% coverage)
- [x] **R1-5.2.6** Create manual test cases: `docs/manual-test-cases/reports-analytics.md`

**Frontend:**
- [x] **R1-5.2.7** Create `features/reports/` folder structure
- [x] **R1-5.2.8** Create KPI dashboard with Material cards and charts
  - Lab utilization widget
  - Student performance widget
  - Attendance analytics widget
- [x] **R1-5.2.9** Create detailed report pages with filters and data export (PDF, Excel, CSV)
- [x] **R1-5.2.10** Create reports routes (lazy-loaded)

---

## 🎓 R1-M6: Post-R1 Academics Additions (INC Compliance, Promotion, Term Lifecycle)

> **Goal:** Close the gap between the original Academics scope (R1-M2/M3/M5, written before these features existed) and what has actually shipped since. Each item below is real, working, deployed functionality — this section exists purely so the tracker stops under-representing Academics. See linked BRs in `BUSINESS_REQUIREMENTS.md` for full detail; this is a summary, not the source of truth.

### R1-M6.1 — INC Nursing Curriculum Compliance (BR-49)

> **Business Requirement:** [BR-49](BUSINESS_REQUIREMENTS.md#br-49-inc-nursing-curriculum-compliance--per-semester-hours-electives-attendance-thresholds--batches)

**Backend:**
- [x] **R1-6.1.1** Per-semester Theory/Lab/Clinical hours + `SubjectType` (CORE/FOUNDATIONAL/ELECTIVE) on `curriculum_term_courses` (curriculum-mapping row, not `Subject` master)
- [x] **R1-6.1.2** `CurriculumElectiveGroup` — choice-based elective groups scoped to one curriculum version + term; bulk course-registration generation auto-skips elective offerings
- [x] **R1-6.1.3** `CourseRegistrationServiceImpl.assignElectiveChoice()` + `POST /course-registrations/elective-assignment` — admin single-pick assignment, idempotent, rejects a second pick within the same group
- [x] **R1-6.1.4** Per-component attendance thresholds (`attendance_thresholds` table, keyed on curriculum mapping + `AttendanceType` incl. new `CLINICAL`) replacing the flat 75% constant; `AttendanceService.getAttendanceReport()` returns one entry per component type
- [x] **R1-6.1.5** Real `Batch` entity (lab/clinical roster splitting) with enforced capacity and real membership (`batch_students`); `LabSchedule.batch_id` nullable FK alongside pre-existing free-text `batch_name`
- [x] **R1-6.1.6** `PUT /curriculum-semester-courses/{id}` — in-place edit of hours/type/elective/sort-order (Curriculum Map previously only supported add/remove)
- [x] **R1-6.1.7** Migrations V265–V274; `CURRICULUM_ELECTIVE_GROUP_VIEW/MANAGE`, `ATTENDANCE_THRESHOLD_VIEW/MANAGE`, `BATCH_VIEW/MANAGE`, `COURSE_REGISTRATION_ELECTIVE_ASSIGN` permissions

**Frontend:**
- [x] **R1-6.1.8** Curriculum Map screen — Theory/Lab/Clinical checkboxes, inline attendance-threshold editing
- [x] **R1-6.1.9** Batch Manage dialog nested under Course Offering list row
- [x] **R1-6.1.10** New **Elective Assignment** screen (academic year → term → elective group → per-student assignment) — the first course-registration UI in the app
- [x] **R1-6.1.11** Wire Course Offerings + Elective Assignment into the Academics sidebar nav — **fixed 2026-07-18**: both screens had working routes/permissions since BR-49 shipped but no inbound nav link anywhere in the app, making them reachable only by typing the URL directly. Added to `app.ts` Academics group.
- [x] **R1-6.1.13** Migration V288 backfills `lab_schedules.batch_id` for any pre-existing row that unambiguously matches a real `Batch` by `(term_instance_id, subject_id via course_offerings, normalized name)` — additive only, never guesses across ambiguous same-named batches, idempotent (`batch_id IS NULL` guard). Verified syntactically valid against the local dev DB (empty `lab_schedules` table there, so 0 rows affected locally — real backfill effect will show wherever real data exists).
- [ ] **R1-6.1.12** Remaining deferred technical debt (still explicitly out of scope, unchanged from BR-49): the Lab Schedule form (`lab-schedule-form.component.html`) still accepts a free-text `batchName` input alongside the roster dropdown, so `batch_name` remains load-bearing and cannot be dropped yet. Full cutover needs a deliberate decision to make the roster dropdown mandatory and remove free-text entry — a UX/behavior change, not a plain cleanup, so it wasn't done in this pass.

### R1-M6.2 — Student Promotion / Progression (BR-52)

> **Business Requirement:** [BR-52](BUSINESS_REQUIREMENTS.md#br-52-student-promotion--progression)

**Backend:**
- [x] **R1-6.2.1** Removed pre-existing blind auto-advance on term-open (`TermInstanceService`'s `OPEN` transition previously advanced every active student by calendar years-since-admission with zero eligibility check)
- [x] **R1-6.2.2** `StudentPromotionService` — subject-wise arrears carried forward (cleared only before Final Year), max duration = double program length, per-subject attendance detention, mandatory preview before irreversible bulk commit
- [x] **R1-6.2.3** `ExamResult.outcome` (PASS/FAIL, external-marks-only for v1 — CIA marks don't exist yet)
- [x] **R1-6.2.4** Cohort-driven term auto-detection (`GET /student-promotions/active-terms`, `GET /student-promotions/suggested-next-term`) with manual cascade fallback
- [x] **R1-6.2.5** `student_promotion_decisions` audit trail; `STUDENT_PROMOTION_VIEW/MANAGE` permissions
- [x] **R1-6.2.6** Migrations V284–V286

**Frontend:**
- [x] **R1-6.2.7** `features/student-promotion/` — select → preview → result screen; per-student editable decision table; bulk execute
- [x] **R1-6.2.8** Added to Academics nav group

### R1-M6.3 — Term Lifecycle Confirmation & Overdue Alerting (BR-53)

> **Business Requirement:** [BR-53](BUSINESS_REQUIREMENTS.md#br-53-term-lifecycle-confirmation--overdue-alerting)

**Backend:**
- [x] **R1-6.3.1** Consequence-confirmation dialog before `PLANNED → OPEN` / `OPEN → LOCKED` term transitions
- [x] **R1-6.3.2** `AcademicTermAlertService` — daily job raising an in-app alert when a term is still `PLANNED` within 14 days of `startDate`, auto-resolving once advanced
- [x] **R1-6.3.3** `Notification`/`NotificationDismissal` entities — broadcast-style with per-user dismissal; first real slice of BR-28's notification-sending backend
- [x] **R1-6.3.4** `GET /notifications/feed`, `POST /notifications/{id}/dismiss`; new `academicTermAlerts` preference category
- [x] **R1-6.3.5** Migration V287

**Frontend:**
- [x] **R1-6.3.6** Toolbar notification bell wired to the real feed (previously a dead hardcoded badge)
- [x] **R1-6.3.7** Academic Year form's `advanceTermStatus()` wrapped with shared `ConfirmDialogComponent`

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
| **Business Documentation** | Any business/workflow changes documented in `docs/BUSINESS_REQUIREMENTS.md` |
| **Code Review** | Pull request reviewed and approved |
| **CHANGELOG** | `CHANGELOG.md` updated with the new feature |

---

## 📊 Release 1 Progress Tracking

| Milestone | Status | Progress |
|-----------|--------|----------|
| R1-M0: Project Scaffolding | ✅ Complete | 100% |
| R1-M1: Foundation & Identity | ✅ Complete | 100% |
| R1-M2: Core Academic & Lab Mapping | ✅ Complete | 100% |
| R1-M3: Operational Logistics | ⚠️ Near-complete | 99% — R1-3.2.9 (lab calendar/week-grid view) was found unbuilt 2026-07-18 despite being checked; a plain table exists instead |
| R1-M4: Finance & Asset Management | ✅ Complete | 100% |
| R1-M5: Assessment & Reporting | ✅ Complete | 100% |
| R1-M6: Post-R1 Academics Additions | ⚠️ Mostly complete | BR-49/52/53 all shipped; one deferred item open (`lab_schedules.batch_name` cleanup) |

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
