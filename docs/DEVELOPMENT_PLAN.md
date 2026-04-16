# 🗓️ CMS Development Plan — Milestone Tracker

> **College Management System** — Step-by-step development plan aligned with the [Implementation Roadmap](../README.md#-implementation-roadmap) and [Technical Standards](TECHNICAL_STANDARDS.md).

---

## 📋 Table of Contents

- [Current State](#-current-state)
- [Milestone Overview](#-milestone-overview)
- [Phase 0: Project Scaffolding](#-phase-0-project-scaffolding)
- [Phase 1: Foundation & Identity](#-phase-1-foundation--identity)
- [Phase 2: Core Academic & Lab Mapping](#-phase-2-core-academic--lab-mapping)
- [Phase 3: Operational Logistics](#-phase-3-operational-logistics)
- [Phase 4: Finance & Asset Management](#-phase-4-finance--asset-management)
- [Phase 5: Assessment & Reporting](#-phase-5-assessment--reporting)
- [Phase 6: Extended Modules](#-phase-6-extended-modules)
- [Definition of Done](#-definition-of-done)

---

## 📍 Current State

The repository currently contains **documentation only**:

| Artifact | Status |
|----------|--------|
| README.md (21 modules, 212 features) | ✅ Complete |
| TECHNICAL_STANDARDS.md | ✅ Complete |
| CONTRIBUTING.md | ✅ Complete |
| CHANGELOG.md | ✅ Created |
| Keycloak realm config (`cms-realm.json`) | ✅ Created |
| Copilot skills & instructions | ✅ Created |
| Manual test case template | ✅ Created |
| Backend project (`backend/`) | ❌ Not started |
| Frontend project (`frontend/`) | ❌ Not started |
| Infrastructure configs (`infrastructure/`) | ❌ Not started |
| Docker Compose | ❌ Not started |

---

## 🏁 Milestone Overview

| Phase | Focus | Modules Covered | Key Outcome |
|-------|-------|-----------------|-------------|
| **Phase 0** | Project Scaffolding | — | Runnable backend + frontend + Docker Compose |
| **Phase 1** | Foundation & Identity | Module 1 (partial) | Keycloak SSO, secured API, authenticated SPA shell |
| **Phase 2** | Core Academic & Lab Mapping | Modules 1, 3, 4, 7.1 | Departments, programs, labs, faculty, curriculum |
| **Phase 3** | Operational Logistics | Modules 2, 5, 7.2 | Students, scheduling, attendance |
| **Phase 4** | Finance & Asset Management | Modules 7.3, 7.4, 8, 16 | Fees, equipment, inventory, assets |
| **Phase 5** | Assessment & Reporting | Modules 6, 7.5–7.10, 13 | Exams, lab evaluation, analytics, accreditation |
| **Phase 6** | Extended Modules | Modules 9–12, 14–15, 17–21 | Library, hostel, transport, LMS, placement, etc. |

---

## 🧱 Phase 0: Project Scaffolding

> **Goal:** Create the runnable project skeleton — backend, frontend, and infrastructure — with no business logic.

### Milestone 0.1 — Backend Project Initialization

- [x] **0.1.1** Initialize Spring Boot 3.4 project with Gradle (Kotlin DSL)
  - Group: `com.cms`, Artifact: `cms-backend`
  - Java 21, Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring OAuth2 Resource Server
  - H2 (runtime), PostgreSQL (runtime), Flyway
  - JaCoCo plugin with 95% coverage threshold
- [x] **0.1.2** Create `application.yml` with common settings
  - Default profile: `local`
  - Virtual threads enabled: `spring.threads.virtual.enabled: true`
  - API base path: `/api/v1`
- [x] **0.1.3** Create `application-local.yml` — H2 in-memory config
  - `spring.jpa.hibernate.ddl-auto: create-drop`
  - Flyway disabled
  - H2 console enabled at `/h2-console`
- [x] **0.1.4** Create `application-prod.yml` — PostgreSQL config
  - `spring.jpa.hibernate.ddl-auto: validate`
  - Flyway enabled
  - Database connection via environment variables
- [x] **0.1.5** Create `application-test.yml` — Test profile
  - H2 in-memory, Flyway disabled, `create-drop`
- [x] **0.1.6** Verify backend starts with `./gradlew bootRun` (local profile)
- [x] **0.1.7** Verify `./gradlew check` passes (empty project baseline)

### Milestone 0.2 — Frontend Project Initialization

- [x] **0.2.1** Initialize Angular 21 project with Angular CLI
  - Standalone components (no NgModules)
  - SCSS + Tailwind CSS styling
  - SSR disabled initially (enable later)
- [x] **0.2.2** Install Angular Material 21 with Material 3 theme
  - Configure `mat.theme()` with azure palette
  - Light and dark mode support
  - Custom `_theme.scss` file
- [x] **0.2.3** Install Keycloak JS SDK (`keycloak-js`)
- [x] **0.2.4** Set up folder-by-feature structure
  - `src/app/core/` — auth, guards, interceptors
  - `src/app/shared/` — components, pipes, directives
  - `src/app/features/` — feature folders (empty initially)
- [x] **0.2.5** Configure Prettier (single quotes, 100 char width)
- [x] **0.2.6** Verify frontend starts with `ng serve`

### Milestone 0.3 — Infrastructure & Docker Compose

- [x] **0.3.1** Create `infrastructure/keycloak/` directory with realm import config
- [x] **0.3.2** Create `docker-compose.yml` with services:
  - Keycloak 26.0 (with realm auto-import)
  - PostgreSQL 17 (for prod profile)
- [ ] **0.3.3** Verify `docker compose up -d keycloak` starts Keycloak with `cms` realm
- [ ] **0.3.4** Verify Keycloak admin console is accessible and realm roles exist:
  - `ROLE_ADMIN`, `ROLE_FACULTY`, `ROLE_STUDENT`, `ROLE_LAB_INCHARGE`, `ROLE_TECHNICIAN`, `ROLE_PARENT`

---

## 🔐 Phase 1: Foundation & Identity

> **Goal:** Secure the platform — Keycloak SSO, JWT-based API security, authenticated Angular shell with role-based navigation.

### Milestone 1.1 — Backend Security Configuration

- [ ] **1.1.1** Create `SecurityConfig` class
  - OAuth2 Resource Server with JWT validation (Keycloak issuer)
  - CORS configuration (whitelist frontend origin)
  - CSRF disabled for stateless API
  - Public endpoints: `/api/v1/health`, `/h2-console/**`
  - All other endpoints require authentication
- [ ] **1.1.2** Create JWT role converter for Keycloak realm roles
  - Map `realm_access.roles` from JWT to Spring Security authorities
- [ ] **1.1.3** Create `GlobalExceptionHandler` (`@RestControllerAdvice`)
  - `ResourceNotFoundException` → 404
  - `MethodArgumentNotValidException` → 400
  - `AccessDeniedException` → 403
  - Generic `Exception` → 500
  - Standard error response record: `ErrorResponse(int status, String message, Instant timestamp)`
- [ ] **1.1.4** Create health check endpoint (`GET /api/v1/health`)
- [ ] **1.1.5** Write unit tests for SecurityConfig and GlobalExceptionHandler
- [ ] **1.1.6** Create manual test cases: `docs/manual-test-cases/security-config.md`

### Milestone 1.2 — Frontend Authentication

- [ ] **1.2.1** Create `AuthService` — initialize Keycloak, login/logout, token management
- [ ] **1.2.2** Create `AuthGuard` — protect routes, redirect unauthenticated users
- [ ] **1.2.3** Create `AuthInterceptor` — attach Bearer token to all API requests
- [ ] **1.2.4** Create `RoleGuard` — restrict routes by user role
- [ ] **1.2.5** Configure `app.config.ts` with providers (router, HTTP client, hydration)
- [ ] **1.2.6** Create manual test cases: `docs/manual-test-cases/authentication.md`

### Milestone 1.3 — Application Shell & Navigation

- [ ] **1.3.1** Create `AppComponent` with Material sidenav layout
  - Top toolbar with app title, user menu, theme toggle
  - Side navigation with role-based menu items
- [ ] **1.3.2** Create `DashboardComponent` (landing page)
  - Placeholder dashboard cards for key metrics
  - "Lab Utilization" widget placeholder
- [ ] **1.3.3** Configure `app.routes.ts` with lazy-loaded feature routes
- [ ] **1.3.4** Implement light/dark theme toggle
- [ ] **1.3.5** Create manual test cases: `docs/manual-test-cases/app-shell.md`

---

## 🧪 Phase 2: Core Academic & Lab Mapping

> **Goal:** Define the institutional structure — departments, programs, academic years, labs, faculty, and curriculum.

### Milestone 2.1 — Department Management (Module 1)

**Backend:**
- [ ] **2.1.1** Create `Department` entity (`id`, `name`, `code`, `description`, `hodName`, `createdAt`, `updatedAt`)
- [ ] **2.1.2** Create `DepartmentRepository` (JpaRepository)
- [ ] **2.1.3** Create `DepartmentService` with CRUD operations
- [ ] **2.1.4** Create `DepartmentController` — REST endpoints:
  - `POST /api/v1/departments` — Create (ROLE_ADMIN)
  - `GET /api/v1/departments` — List all (authenticated)
  - `GET /api/v1/departments/{id}` — Get by ID (authenticated)
  - `PUT /api/v1/departments/{id}` — Update (ROLE_ADMIN)
  - `DELETE /api/v1/departments/{id}` — Delete (ROLE_ADMIN)
- [ ] **2.1.5** Create `DepartmentRequest` and `DepartmentResponse` DTOs (Java records)
- [ ] **2.1.6** Create Flyway migration: `V1__create_departments_table.sql`
- [ ] **2.1.7** Write unit + controller tests (95% coverage)
- [ ] **2.1.8** Create manual test cases: `docs/manual-test-cases/department-management.md`

**Frontend:**
- [ ] **2.1.9** Create `features/department/` folder structure
- [ ] **2.1.10** Create `DepartmentService` (API calls)
- [ ] **2.1.11** Create `DepartmentListComponent` — Material table with search, sort, pagination
- [ ] **2.1.12** Create `DepartmentFormComponent` — Create/Edit form with validation
- [ ] **2.1.13** Create department routes (lazy-loaded)

### Milestone 2.2 — Program & Course Management (Module 1)

**Backend:**
- [ ] **2.2.1** Create `Program` entity (`id`, `name`, `code`, `programLevel`, `durationYears`, `departments`)
- [ ] **2.2.2** Create `Course` entity (`id`, `name`, `code`, `credits`, `theoryCredits`, `labCredits`, `program`, `semester`)
- [ ] **2.2.3** Create repositories, services, controllers for Program and Course
- [ ] **2.2.4** Create DTOs (Java records) for Program and Course
- [ ] **2.2.5** Create Flyway migrations: `V2__create_programs_table.sql`, `V3__create_courses_table.sql`
- [ ] **2.2.6** Write unit + controller tests (95% coverage)
- [ ] **2.2.7** Create manual test cases: `docs/manual-test-cases/program-course-management.md`

**Frontend:**
- [ ] **2.2.8** Create `features/program/` and `features/course/` folder structures
- [ ] **2.2.9** Create list and form components for Program and Course
- [ ] **2.2.10** Create routes (lazy-loaded)

### Milestone 2.3 — Academic Year & Calendar (Module 1)

**Backend:**
- [ ] **2.3.1** Create `AcademicYear` entity (`id`, `name`, `startDate`, `endDate`, `isCurrent`)
- [ ] **2.3.2** Create `Semester` entity (`id`, `name`, `academicYear`, `startDate`, `endDate`, `semesterNumber`)
- [ ] **2.3.3** Create repositories, services, controllers
- [ ] **2.3.4** Create DTOs and Flyway migrations
- [ ] **2.3.5** Write unit + controller tests (95% coverage)
- [ ] **2.3.6** Create manual test cases: `docs/manual-test-cases/academic-year-management.md`

**Frontend:**
- [ ] **2.3.7** Create `features/academic-year/` with list and form components
- [ ] **2.3.8** Create academic calendar view component

### Milestone 2.4 — Lab Setup & Configuration (Module 7.1)

**Backend:**
- [ ] **2.4.1** Create `Lab` entity (`id`, `name`, `labType`, `department`, `building`, `roomNumber`, `capacity`, `status`)
- [ ] **2.4.2** Create `LabType` enum (`COMPUTER`, `PHYSICS`, `CHEMISTRY`, `ELECTRONICS`, `BIOLOGY`, `LANGUAGE`, `MECHANICAL`, `OTHER`)
- [ ] **2.4.3** Create `LabInChargeAssignment` entity (map faculty/technician to lab)
- [ ] **2.4.4** Create repositories, services, controllers
  - `POST /api/v1/labs` — Create lab (ROLE_ADMIN)
  - `GET /api/v1/labs` — List labs (authenticated)
  - `GET /api/v1/labs/{id}` — Get lab details (authenticated)
  - `PUT /api/v1/labs/{id}` — Update lab (ROLE_ADMIN, ROLE_LAB_INCHARGE)
  - `DELETE /api/v1/labs/{id}` — Delete lab (ROLE_ADMIN)
  - `POST /api/v1/labs/{id}/assign` — Assign in-charge (ROLE_ADMIN)
- [ ] **2.4.5** Create DTOs and Flyway migrations
- [ ] **2.4.6** Write unit + controller tests (95% coverage)
- [ ] **2.4.7** Create manual test cases: `docs/manual-test-cases/lab-setup.md`

**Frontend:**
- [ ] **2.4.8** Create `features/lab/` folder structure
- [ ] **2.4.9** Create `LabListComponent` — Material table with filters (by department, type, status)
- [ ] **2.4.10** Create `LabFormComponent` — Create/Edit form
- [ ] **2.4.11** Create `LabDetailComponent` — Lab detail view with assigned staff
- [ ] **2.4.12** Create lab routes (lazy-loaded)

### Milestone 2.5 — Faculty Management (Module 3)

**Backend:**
- [ ] **2.5.1** Create `Faculty` entity (`id`, `employeeCode`, `firstName`, `lastName`, `email`, `phone`, `department`, `designation`, `specialization`, `labExpertise`, `joiningDate`)
- [ ] **2.5.2** Create repositories, services, controllers
  - CRUD endpoints under `/api/v1/faculty`
  - Lab teaching assignment endpoints
- [ ] **2.5.3** Create DTOs and Flyway migrations
- [ ] **2.5.4** Write unit + controller tests (95% coverage)
- [ ] **2.5.5** Create manual test cases: `docs/manual-test-cases/faculty-management.md`

**Frontend:**
- [ ] **2.5.6** Create `features/faculty/` with list, form, and detail components
- [ ] **2.5.7** Create faculty routes (lazy-loaded)

### Milestone 2.6 — Curriculum & Lab-Curriculum Mapping (Module 4)

**Backend:**
- [ ] **2.6.1** Create `Syllabus` entity (course syllabus with theory + lab components)
- [ ] **2.6.2** Create `Experiment` entity (`id`, `name`, `course`, `experimentNumber`, `description`, `learningOutcomes`)
- [ ] **2.6.3** Create `LabCurriculumMapping` entity (map experiments to Course Outcomes / Program Outcomes)
- [ ] **2.6.4** Create repositories, services, controllers
- [ ] **2.6.5** Create DTOs and Flyway migrations
- [ ] **2.6.6** Write unit + controller tests (95% coverage)
- [ ] **2.6.7** Create manual test cases: `docs/manual-test-cases/curriculum-management.md`

**Frontend:**
- [ ] **2.6.8** Create `features/curriculum/` with syllabus and experiment components
- [ ] **2.6.9** Create CO/PO mapping matrix UI component

---

## 📅 Phase 3: Operational Logistics

> **Goal:** Student lifecycle, lab scheduling, and attendance tracking.

### Milestone 3.1 — Student Management (Module 2)

**Backend:**
- [ ] **3.1.1** Create `Student` entity with expanded fields:
  - Core: `id`, `rollNumber`, `firstName`, `lastName`, `email`, `phone`, `program`, `semester`, `admissionDate`, `labBatch`, `status`
  - Personal: `dateOfBirth` (LocalDate), `gender` (Enum: MALE/FEMALE/OTHER), `aadharNumber` (String, encrypted at rest)
  - Demographics: `nationality`, `religion`, `communityCategory` (Enum: SC/ST/BC/MBC/DNC/OC/OTHERS), `caste`, `bloodGroup` (Enum: A_POSITIVE/A_NEGATIVE/B_POSITIVE/B_NEGATIVE/O_POSITIVE/O_NEGATIVE/AB_POSITIVE/AB_NEGATIVE)
  - Family: `fatherName`, `motherName`, `parentMobile`
  - Embedded `Address`: `postalAddress`, `street`, `city`, `district`, `state`, `pincode`
- [ ] **3.1.2** Create `Admission` entity with full field spec:
  - `id`, `student` (FK → Student), `academicYearFrom` (Integer), `academicYearTo` (Integer), `applicationDate` (LocalDate)
  - `status` (Enum: DRAFT/SUBMITTED/UNDER_REVIEW/DOCUMENTS_PENDING/APPROVED/REJECTED)
  - `declarationPlace`, `declarationDate` (LocalDate), `parentConsentGiven` (Boolean), `applicantConsentGiven` (Boolean)
- [ ] **3.1.2a** Create `AcademicQualification` entity:
  - `id`, `admission` (FK → Admission), `qualificationType` (Enum: SSLC/HSC/DIPLOMA/DEGREE/OTHER)
  - `schoolName`, `majorSubject`, `totalMarks` (Integer), `percentage` (BigDecimal), `monthAndYearOfPassing` (String), `universityOrBoard`
  - One admission → many qualifications
- [ ] **3.1.2b** Create `AdmissionDocument` entity:
  - `id`, `admission` (FK → Admission), `documentType` (Enum: TENTH_MARKSHEET/ELEVENTH_MARKSHEET/TWELFTH_MARKSHEET/TRANSFER_CERTIFICATE/COMMUNITY_CERTIFICATE/INCOME_CERTIFICATE/NATIVITY_CERTIFICATE/MIGRATION_CERTIFICATE/FIRST_GRADUATE_CERTIFICATE/PASSPORT_PHOTO/SIGNED_AFFIDAVIT/UNDERTAKING_DOCUMENT/AADHAR_CARD/MEDICAL_FITNESS/ELIGIBILITY_CERTIFICATE)
  - `fileName`, `storageKey`, `uploadedAt` (LocalDateTime)
  - `originalSubmitted` (Boolean), `verifiedBy` (String, nullable), `verifiedAt` (LocalDateTime, nullable)
  - `verificationStatus` (Enum: NOT_UPLOADED/UPLOADED/VERIFIED/REJECTED)
- [ ] **3.1.2c** Create all enums in `com.cms.model.enums`:
  - `Gender`: MALE, FEMALE, OTHER
  - `CommunityCategory`: SC, ST, BC, MBC, DNC, OC, OTHERS
  - `BloodGroup`: A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, O_POSITIVE, O_NEGATIVE, AB_POSITIVE, AB_NEGATIVE
  - `QualificationType`: SSLC, HSC, DIPLOMA, DEGREE, OTHER
  - `AdmissionStatus`: DRAFT, SUBMITTED, UNDER_REVIEW, DOCUMENTS_PENDING, APPROVED, REJECTED
  - `DocumentType`: TENTH_MARKSHEET, ELEVENTH_MARKSHEET, TWELFTH_MARKSHEET, TRANSFER_CERTIFICATE, COMMUNITY_CERTIFICATE, INCOME_CERTIFICATE, NATIVITY_CERTIFICATE, MIGRATION_CERTIFICATE, FIRST_GRADUATE_CERTIFICATE, PASSPORT_PHOTO, SIGNED_AFFIDAVIT, UNDERTAKING_DOCUMENT, AADHAR_CARD, MEDICAL_FITNESS, ELIGIBILITY_CERTIFICATE
  - `DocumentVerificationStatus`: NOT_UPLOADED, UPLOADED, VERIFIED, REJECTED
- [ ] **3.1.3** Create repositories, services, controllers
  - CRUD endpoints under `/api/v1/students`
  - Admission workflow: `/api/v1/admissions`
  - Academic qualifications: `/api/v1/admissions/{id}/qualifications`
  - Document upload/verification: `/api/v1/admissions/{id}/documents`
  - Document checklist status: `GET /api/v1/admissions/{id}/documents/checklist`
  - Lab batch assignment during enrollment
- [ ] **3.1.4** Create DTOs and Flyway migrations
- [ ] **3.1.5** Write unit + controller tests (95% coverage)
- [ ] **3.1.6** Create manual test cases: `docs/manual-test-cases/student-management.md`

**Frontend:**
- [ ] **3.1.7** Create `features/student/` with list, form, detail, and admission components
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
- [ ] **3.1.8** Create student directory with search and filters
- [ ] **3.1.9** Create student routes (lazy-loaded)

### Milestone 3.2 — Lab Scheduling & Timetable (Module 7.2)

**Backend:**
- [ ] **3.2.1** Create `LabSchedule` entity (`id`, `lab`, `course`, `faculty`, `batch`, `dayOfWeek`, `startTime`, `endTime`, `semester`)
- [ ] **3.2.2** Create `LabSlot` entity for reusable time slot definitions
- [ ] **3.2.3** Create scheduling service with conflict detection logic
  - Check lab availability, faculty availability, batch conflicts
- [ ] **3.2.4** Create controllers:
  - `POST /api/v1/lab-schedules` — Create schedule (ROLE_ADMIN, ROLE_FACULTY)
  - `GET /api/v1/lab-schedules` — List with filters (lab, faculty, batch, day)
  - Conflict detection endpoint
- [ ] **3.2.5** Create DTOs and Flyway migrations
- [ ] **3.2.6** Write unit + controller tests (95% coverage)
- [ ] **3.2.7** Create manual test cases: `docs/manual-test-cases/lab-scheduling.md`

**Frontend:**
- [ ] **3.2.8** Create `features/lab-schedule/` folder structure
- [ ] **3.2.9** Create lab calendar view (weekly timetable grid)
- [ ] **3.2.10** Create schedule form with conflict alerts
- [ ] **3.2.11** Create lab schedule routes (lazy-loaded)

### Milestone 3.3 — Attendance Management (Module 5)

**Backend:**
- [ ] **3.3.1** Create `Attendance` entity (`id`, `student`, `course`, `date`, `status`, `type` [THEORY/LAB])
- [ ] **3.3.2** Create `LabAttendance` entity (extends with `experiment`, `labBatch`, `labSession`)
- [ ] **3.3.3** Create attendance service with:
  - Mark attendance (individual + bulk)
  - Attendance percentage calculation
  - Low attendance alert logic
- [ ] **3.3.4** Create controllers:
  - `POST /api/v1/attendance` — Mark attendance (ROLE_FACULTY, ROLE_LAB_INCHARGE)
  - `GET /api/v1/attendance/reports` — Attendance reports with filters
  - `GET /api/v1/attendance/alerts` — Low attendance alerts
- [ ] **3.3.5** Create DTOs and Flyway migrations
- [ ] **3.3.6** Write unit + controller tests (95% coverage)
- [ ] **3.3.7** Create manual test cases: `docs/manual-test-cases/attendance-management.md`

**Frontend:**
- [ ] **3.3.8** Create `features/attendance/` folder structure
- [ ] **3.3.9** Create attendance marking component (batch-wise view)
- [ ] **3.3.10** Create attendance report component with charts
- [ ] **3.3.11** Create attendance routes (lazy-loaded)

---

## 💰 Phase 4: Finance & Asset Management

> **Goal:** Fee lifecycle, lab equipment/inventory tracking, and institutional asset management.

### Milestone 4.1 — Fee Structure & Collection (Module 8)

> **Business Requirements:** See [BR-1](BUSINESS_REQUIREMENTS.md#br-1-fee-structure--academic-year) and [BR-2](BUSINESS_REQUIREMENTS.md#br-2-year-wise-fee-boxes-per-program-duration).

**Backend:**
- [ ] **4.1.1** Create `FeeStructure` entity (`id`, `name`, `program`, `academicYear`, `feeType`, `amount` [BigDecimal], `isMandatory`, `isActive`)
  - Fee structure is scoped to **program + academic year** — fees may vary year to year (BR-1)
- [ ] **4.1.2** Create `FeeStructureYearAmount` entity (`id`, `feeStructure`, `yearNumber`, `yearLabel`, `amount` [BigDecimal])
  - Year-wise fee boxes generated based on program `durationYears` (BR-2)
  - Labels: "First Year", "Second Year", etc.
- [ ] **4.1.3** Create `FeePayment` entity (`id`, `student`, `feeStructure`, `amountPaid` [BigDecimal], `paymentDate`, `paymentMethod`, `transactionId`, `status`)
- [ ] **4.1.4** Create fee service with payment processing logic
  - All monetary values as `BigDecimal` with `RoundingMode.HALF_UP`
  - Year-wise amount storage and retrieval based on program duration
- [ ] **4.1.5** Create controllers:
  - `POST /api/v1/fee-structures` — Define fee structure (ROLE_ADMIN)
  - `GET /api/v1/fee-structures` — List with filters (programId, academicYearId)
  - `POST /api/v1/fee-payments` — Record payment (ROLE_ADMIN)
  - `GET /api/v1/fee-payments/student/{studentId}` — Student fee history
  - `GET /api/v1/fee-payments/reports` — Revenue reports
- [ ] **4.1.6** Create DTOs and Flyway migrations
- [ ] **4.1.7** Write unit + controller tests (95% coverage)
- [ ] **4.1.8** Create manual test cases: `docs/manual-test-cases/fee-management.md`

**Frontend:**
- [ ] **4.1.9** Create `features/finance/` folder structure
- [ ] **4.1.10** Create fee structure management component
  - Dynamic year-wise fee input boxes based on selected program's `durationYears` (BR-2)
  - Labels generated as "First Year", "Second Year", etc.
- [ ] **4.1.11** Create payment recording component
- [ ] **4.1.12** Create high-density fee table with Material density `-2`
- [ ] **4.1.13** Create finance routes (lazy-loaded)

### Milestone 4.1a — Referral Type Master (Module 8)

> **Business Requirements:** See [BR-4](BUSINESS_REQUIREMENTS.md#br-4-referral-type-master).

**Backend:**
- [ ] **4.1a.1** Create `ReferralType` entity (`id`, `name`, `code`, `guidelineValue` [BigDecimal], `description`, `isActive`)
  - Replaces hardcoded `EnquirySource` enum for referral categorization
  - Default types: WALK_IN, PHONE, ONLINE, AGENT_REFERRAL, STAFF, ALUMNI, PARENT, ADVERTISEMENT
- [ ] **4.1a.2** Create ReferralType service with CRUD operations
- [ ] **4.1a.3** Create controllers under `/api/v1/referral-types`
- [ ] **4.1a.4** Create DTOs and Flyway migrations (seed default referral types)
- [ ] **4.1a.5** Write unit + controller tests (95% coverage)
- [ ] **4.1a.6** Create manual test cases: `docs/manual-test-cases/referral-type-management.md`

**Frontend:**
- [ ] **4.1a.7** Create `features/referral-type/` with list and form components
- [ ] **4.1a.8** Create referral type routes (lazy-loaded)

### Milestone 4.1b — Enquiry-to-Admission Workflow Enhancement

> **Business Requirements:** See [BR-3](BUSINESS_REQUIREMENTS.md#br-3-fee-structure-guideline-on-enquiry-screen), [BR-5](BUSINESS_REQUIREMENTS.md#br-5-referral-guideline-amount--final-fee-calculation), [BR-6](BUSINESS_REQUIREMENTS.md#br-6-admin-fee-finalization-workflow), [BR-7](BUSINESS_REQUIREMENTS.md#br-7-payment-collection-by-accounting-team), [BR-8](BUSINESS_REQUIREMENTS.md#br-8-enquiry-status-workflow), [BR-9](BUSINESS_REQUIREMENTS.md#br-9-document-submission), [BR-10](BUSINESS_REQUIREMENTS.md#br-10-convert-enquiry-to-student), [BR-11](BUSINESS_REQUIREMENTS.md#br-11-student-explorer-with-filters).

**Backend:**
- [ ] **4.1b.1** Enhance `Enquiry` entity with fee guideline fields:
  - `feeGuidelineTotal`, `referralTypeId` (FK → ReferralType), `referralAdditionalAmount`, `finalCalculatedFee`
  - Year-wise guideline breakdown stored as related records
- [ ] **4.1b.2** Update `EnquiryStatus` enum to reflect new workflow:
  - ENQUIRED, INTERESTED, NOT_INTERESTED, FEES_FINALIZED, FEES_PAID, PARTIALLY_PAID, DOCUMENTS_SUBMITTED, CONVERTED, CLOSED
- [ ] **4.1b.3** Enhance enquiry service:
  - Fee guideline lookup by program + current academic year (BR-3)
  - Referral additional amount calculation (BR-5)
  - Final fee computation (BR-5)
  - Automatic status transitions (BR-8)
- [ ] **4.1b.4** Create fee finalization endpoints for admin (BR-6):
  - `POST /api/v1/enquiries/{id}/finalize-fees` — Admin finalizes fee structure
  - `GET /api/v1/enquiries/{id}/fee-guideline` — Get fee guideline for enquiry's program
- [ ] **4.1b.5** Enhance payment collection for enquiry-based fees (BR-7)
- [ ] **4.1b.6** Create document submission tracking for enquiries (BR-9):
  - `POST /api/v1/enquiries/{id}/documents` — Submit document
  - `GET /api/v1/enquiries/{id}/documents` — List submitted documents
  - `PATCH /api/v1/enquiries/{id}/documents/{docId}/verify` — Verify document
- [ ] **4.1b.7** Enhance convert-to-student to require DOCUMENTS_SUBMITTED status (BR-10)
- [ ] **4.1b.8** Create student explorer endpoint with filters (BR-11):
  - `GET /api/v1/students/explorer` — Search with filters (program, department, academicYear, semester, status, feeStatus, search)
- [ ] **4.1b.9** Create DTOs and Flyway migrations for all new fields/tables
- [ ] **4.1b.10** Write unit + controller tests (95% coverage)
- [ ] **4.1b.11** Update manual test cases: `docs/manual-test-cases/enquiry-management.md`

**Frontend:**
- [ ] **4.1b.12** Enhance enquiry form with fee structure guideline panel (BR-3)
  - Side panel showing fee structure for selected program in current academic year
  - Year-wise breakdown displayed dynamically
- [ ] **4.1b.13** Add referral type dropdown (from master) with conditional additional amount box (BR-4, BR-5)
  - Show additional amount box only when referral type has non-zero guidelineValue
  - Calculate and display final fee
- [ ] **4.1b.14** Create admin fee finalization screen (BR-6)
  - Display enquiry guideline values as starting point
  - Allow admin to adjust fees, provide discounts, modify year-wise split
- [ ] **4.1b.15** Enhance payment collection screen for enquiry-based workflow (BR-7)
- [ ] **4.1b.16** Create document submission tracking component (BR-9)
- [ ] **4.1b.17** Update enquiry list with new status workflow and automatic transitions (BR-8)
- [ ] **4.1b.18** Create student explorer screen with comprehensive filters (BR-11)

### Milestone 4.2 — Equipment & Inventory Management (Modules 7.3 & 7.4)

**Backend:**
- [ ] **4.2.1** Create `Equipment` entity (`id`, `name`, `model`, `serialNumber`, `lab`, `category`, `status`, `purchaseDate`, `purchaseCost` [BigDecimal], `warrantyExpiry`)
- [ ] **4.2.2** Create `EquipmentStatus` enum (`AVAILABLE`, `IN_USE`, `UNDER_REPAIR`, `DAMAGED`, `DISPOSED`)
- [ ] **4.2.3** Create `Consumable` entity (`id`, `name`, `lab`, `quantity`, `unit`, `minimumStock`, `expiryDate`)
- [ ] **4.2.4** Create `StockTransaction` entity (stock in/out tracking)
- [ ] **4.2.5** Create equipment and inventory services with:
  - Equipment lifecycle tracking
  - Low stock alerts
  - Stock in/out recording
- [ ] **4.2.6** Create controllers under `/api/v1/equipment` and `/api/v1/inventory`
- [ ] **4.2.7** Create DTOs and Flyway migrations
- [ ] **4.2.8** Write unit + controller tests (95% coverage)
- [ ] **4.2.9** Create manual test cases: `docs/manual-test-cases/equipment-inventory.md`

**Frontend:**
- [ ] **4.2.10** Create `features/equipment/` and `features/inventory/` folder structures
- [ ] **4.2.11** Create equipment list with status badges and filters
- [ ] **4.2.12** Create equipment form and detail components
- [ ] **4.2.13** Create inventory stock dashboard
- [ ] **4.2.14** Create equipment/inventory routes (lazy-loaded)

### Milestone 4.3 — Maintenance & Repair (Module 7.7)

**Backend:**
- [ ] **4.3.1** Create `MaintenanceRequest` entity (`id`, `equipment`, `requestedBy`, `description`, `priority`, `status`, `assignedTechnician`, `completedDate`, `repairCost` [BigDecimal])
- [ ] **4.3.2** Create maintenance service with workflow (Request → Assign → In Progress → Complete)
- [ ] **4.3.3** Create controllers under `/api/v1/maintenance`
- [ ] **4.3.4** Create DTOs and Flyway migrations
- [ ] **4.3.5** Write unit + controller tests (95% coverage)
- [ ] **4.3.6** Create manual test cases: `docs/manual-test-cases/maintenance-management.md`

**Frontend:**
- [ ] **4.3.7** Create `features/maintenance/` with list, form, and workflow components
- [ ] **4.3.8** Create maintenance routes (lazy-loaded)

---

## 📝 Phase 5: Assessment & Reporting

> **Goal:** Examination management, lab evaluation, analytics dashboards, and accreditation reports.

### Milestone 5.1 — Examination Management (Module 6)

**Backend:**
- [ ] **5.1.1** Create `Examination` entity (`id`, `name`, `course`, `examType` [THEORY/PRACTICAL/VIVA], `date`, `duration`, `maxMarks`, `semester`)
- [ ] **5.1.2** Create `ExamResult` entity (`id`, `examination`, `student`, `marksObtained`, `grade`, `status`)
- [ ] **5.1.3** Create `LabContinuousEvaluation` entity (experiment-wise marks: record + viva + performance)
- [ ] **5.1.4** Create exam service with:
  - GPA/CGPA calculation (including lab components)
  - Result processing and publishing
  - Lab practical exam management
- [ ] **5.1.5** Create controllers under `/api/v1/examinations` and `/api/v1/results`
- [ ] **5.1.6** Create DTOs and Flyway migrations
- [ ] **5.1.7** Write unit + controller tests (95% coverage)
- [ ] **5.1.8** Create manual test cases: `docs/manual-test-cases/examination-management.md`

**Frontend:**
- [ ] **5.1.9** Create `features/examination/` folder structure
- [ ] **5.1.10** Create exam scheduling and management components
- [ ] **5.1.11** Create marks entry component (batch-wise)
- [ ] **5.1.12** Create result view and transcript component
- [ ] **5.1.13** Create examination routes (lazy-loaded)

### Milestone 5.2 — Lab Reports & Analytics (Modules 7.10 & 13)

**Backend:**
- [ ] **5.2.1** Create report service with aggregation queries for:
  - Lab utilization reports (usage %, peak hours, idle time)
  - Equipment utilization reports
  - Student lab performance reports
  - Lab expense reports
  - Safety incident reports
- [ ] **5.2.2** Create accreditation report service:
  - CO/PO attainment calculation
  - Experiment completion rates
  - NBA/NAAC compliance data
- [ ] **5.2.3** Create controllers under `/api/v1/reports`
- [ ] **5.2.4** Create DTOs for report responses
- [ ] **5.2.5** Write unit + controller tests (95% coverage)
- [ ] **5.2.6** Create manual test cases: `docs/manual-test-cases/reports-analytics.md`

**Frontend:**
- [ ] **5.2.7** Create `features/reports/` folder structure
- [ ] **5.2.8** Create KPI dashboard with Material cards and charts
  - Lab utilization widget
  - Student performance widget
  - Attendance analytics widget
- [ ] **5.2.9** Create detailed report pages with filters and data export (PDF, Excel, CSV)
- [ ] **5.2.10** Create reports routes (lazy-loaded)

---

## 🌐 Phase 6: Extended Modules

> **Goal:** Build out remaining modules based on priority and institutional needs.

### Milestone 6.1 — Lab Safety & Compliance (Module 7.8)

- [ ] **6.1.1** Backend: Safety guidelines, incident reporting, PPE tracking entities and APIs
- [ ] **6.1.2** Backend: Safety training records and audit entities
- [ ] **6.1.3** Frontend: Safety management components
- [ ] **6.1.4** Tests and manual test cases

### Milestone 6.2 — Communication & Portals (Module 12)

- [ ] **6.2.1** Backend: Notice board / announcements API
- [ ] **6.2.2** Backend: Internal messaging system
- [ ] **6.2.3** Frontend: Student portal dashboard
- [ ] **6.2.4** Frontend: Faculty portal dashboard
- [ ] **6.2.5** Frontend: Parent portal (read-only access to ward's progress)
- [ ] **6.2.6** Tests and manual test cases

### Milestone 6.3 — Library Management (Module 9)

- [ ] **6.3.1** Backend: Book cataloging, issue/return, digital library APIs
- [ ] **6.3.2** Frontend: Library management components
- [ ] **6.3.3** Tests and manual test cases

### Milestone 6.4 — Hostel Management (Module 10)

- [ ] **6.4.1** Backend: Room allocation, hostel fee, mess management, attendance APIs
- [ ] **6.4.2** Frontend: Hostel management components
- [ ] **6.4.3** Tests and manual test cases

### Milestone 6.5 — Transport Management (Module 11)

- [ ] **6.5.1** Backend: Route, vehicle, transport fee, driver management APIs
- [ ] **6.5.2** Frontend: Transport management components
- [ ] **6.5.3** Tests and manual test cases

### Milestone 6.6 — Research & Publication (Module 14)

- [ ] **6.6.1** Backend: Research project, publication, patent, grant management APIs
- [ ] **6.6.2** Frontend: Research management components
- [ ] **6.6.3** Tests and manual test cases

### Milestone 6.7 — Placement & Career (Module 15)

- [ ] **6.7.1** Backend: Placement drives, job portal, resume builder APIs
- [ ] **6.7.2** Frontend: Placement management components
- [ ] **6.7.3** Tests and manual test cases

### Milestone 6.8 — Event & Activity Management (Module 17)

- [ ] **6.8.1** Backend: Event, club, certificate generation APIs
- [ ] **6.8.2** Frontend: Event management components
- [ ] **6.8.3** Tests and manual test cases

### Milestone 6.9 — Online Learning / LMS (Module 19)

- [ ] **6.9.1** Backend: LMS content delivery, assignment submission, virtual lab integration APIs
- [ ] **6.9.2** Frontend: LMS components
- [ ] **6.9.3** Tests and manual test cases

### Milestone 6.10 — Feedback & Survey (Module 21)

- [ ] **6.10.1** Backend: Student feedback, 360° feedback, survey, grievance APIs
- [ ] **6.10.2** Frontend: Feedback and survey components
- [ ] **6.10.3** Tests and manual test cases

### Milestone 6.11 — Security & Compliance (Module 18)

- [ ] **6.11.1** Backend: Audit logs, GDPR compliance, document management APIs
- [ ] **6.11.2** Frontend: Security and compliance components
- [ ] **6.11.3** Tests and manual test cases

### Milestone 6.12 — Mobile & Integration (Module 20)

- [ ] **6.12.1** Backend: API optimization for mobile clients
- [ ] **6.12.2** Biometric/RFID integration hooks
- [ ] **6.12.3** Third-party API integration endpoints (ERP, government portals)
- [ ] **6.12.4** IoT integration for lab sensors
- [ ] **6.12.5** Tests and manual test cases

---

## ✅ Definition of Done

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

## 📊 Progress Tracking

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 0: Project Scaffolding | ✅ Complete | 100% |
| Phase 1: Foundation & Identity | ⬜ Not Started | 0% |
| Phase 2: Core Academic & Lab Mapping | ⬜ Not Started | 0% |
| Phase 3: Operational Logistics | ⬜ Not Started | 0% |
| Phase 4: Finance & Asset Management | ⬜ Not Started | 0% |
| Phase 5: Assessment & Reporting | ⬜ Not Started | 0% |
| Phase 6: Extended Modules | ⬜ Not Started | 0% |

---

> **Note:** This plan follows the [Implementation Roadmap](../README.md#-implementation-roadmap) from README.md. Each phase builds on the previous one. Phase 0 is a prerequisite — no business logic until the skeleton is running. Phases 1–5 cover the core 80% of the system. Phase 6 covers extended modules that can be prioritized based on institutional needs.
