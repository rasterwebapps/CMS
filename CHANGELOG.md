# 📝 Changelog

All notable changes to the College Management System will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Initial project documentation structure
- Technical standards & architecture guide
- GitHub Copilot skills for Angular, Spring Boot, Flyway, and Keycloak
- Manual test case template and guidelines
- Contributing guide

### Business Requirements Documentation
- **`docs/BUSINESS_REQUIREMENTS.md`** — Comprehensive business requirements document (BR-1 to BR-11)
  - BR-1: Fee structure scoped per program per academic year (fees may vary year to year)
  - BR-2: Year-wise fee input boxes based on program duration (First Year, Second Year, etc.)
  - BR-3: Fee structure guideline panel on enquiry screen for selected program's current academic year
  - BR-4: Referral Type as separate master entity (replaces hardcoded EnquirySource enum) with types: WALK_IN, PHONE, ONLINE, AGENT_REFERRAL, STAFF, ALUMNI, PARENT, ADVERTISEMENT
  - BR-5: Referral guideline amount — additional box for non-zero values; final fee = program fee + referral amount
  - BR-6: Admin fee finalization workflow — front office submits, admin reviews/adjusts/finalizes
  - BR-7: Payment collection by accounting team — full/partial payments, multiple modes, receipts
  - BR-8: Enhanced enquiry status workflow: ENQUIRED → INTERESTED → FEES_FINALIZED → FEES_PAID/PARTIALLY_PAID → DOCUMENTS_SUBMITTED → CONVERTED
  - BR-9: Document submission tracking (10th, 12th certificates, etc.)
  - BR-10: Convert enquiry to student (requires DOCUMENTS_SUBMITTED status)
  - BR-11: Student explorer screen with comprehensive filters
  - End-to-end enquiry-to-admission lifecycle documented
  - Mandatory documentation policy: all business/workflow changes must be documented before merge
- **Updated `docs/DEVELOPMENT_PLAN.md`**
  - Added milestones: 4.1a (Referral Type Master), 4.1b (Enquiry-to-Admission Workflow Enhancement)
  - Enhanced milestone 4.1 with year-wise fee boxes (BR-2) and academic year scoping (BR-1)
  - Added "Business Documentation" to Definition of Done
- **Updated `docs/RELEASE_1_MILESTONES.md`**
  - Added milestones: R1-M4.1a (Referral Type Master), R1-M4.1b (Enquiry-to-Admission Workflow Enhancement)
  - Enhanced R1-M4.1 with year-wise fee boxes and academic year scoping
  - Added "Business Documentation" to Definition of Done
- **Updated `docs/TECHNICAL_STANDARDS.md`**
  - Added Section 9: Business & Workflow Documentation (mandatory documentation rule, what to document, documentation workflow)
- **Updated `CONTRIBUTING.md`**
  - Added step 6: Document business/workflow changes in `docs/BUSINESS_REQUIREMENTS.md`
- **Updated `docs/README.md`**
  - Added reference to `BUSINESS_REQUIREMENTS.md` in documentation index
- **Updated `docs/manual-test-cases/enquiry-management.md`**
  - Added TC-ENQ-022 to TC-ENQ-035: Fee guideline display, referral type selection, additional amount box, final fee calculation, status workflow, admin finalization, payment collection, document submission, enhanced conversion, student explorer
- **Updated `docs/manual-test-cases/fee-management.md`**
  - Added TC-FEE-015 to TC-FEE-020: Year-wise fee boxes per program duration, academic year filtering

### R1-M0: Project Scaffolding
- **Backend Initialization**
  - Spring Boot 3.4 project with Gradle (Kotlin DSL), Java 21, virtual threads
  - `application.yml` with default `local` profile, API base path `/api/v1`
  - `application-local.yml` — H2 in-memory, `create-drop`, H2 console enabled
  - `application-prod.yml` — PostgreSQL 17, Flyway enabled, `validate`
  - `application-test.yml` — H2 in-memory for tests
  - JaCoCo plugin with 95% minimum coverage enforcement
- **Frontend Initialization**
  - Angular 21 with standalone components, SCSS + Tailwind CSS
  - Angular Material 21 with Material Design 3 (azure palette, light/dark mode)
  - Keycloak JS SDK installed
  - Folder-by-feature structure (`core/`, `shared/`, `features/`)
  - Prettier configured (single quotes, 100 char width)
- **Infrastructure**
  - Docker Compose with Keycloak 26.0 and PostgreSQL 17
  - Keycloak realm import configuration (`cms` realm)

### R1-M1: Foundation & Identity
- **Backend Security Configuration**
  - `SecurityConfig` with OAuth2 Resource Server, CORS, CSRF, stateless sessions
  - `JwtRoleConverter` for Keycloak realm role extraction
  - `GlobalExceptionHandler` with standardized `ErrorResponse` format
  - `ResourceNotFoundException` custom exception
  - Health check endpoint (`GET /api/v1/health`)
  - Unit tests for all security components (95% JaCoCo coverage)
- **Frontend Authentication**
  - `AuthService` — Keycloak initialization, login/logout, token management with Angular Signals
  - `AuthGuard` — route protection with Keycloak login redirect
  - `RoleGuard` — role-based route access control
  - `AuthInterceptor` — automatic Bearer token injection for API requests
  - Environment configuration for Keycloak and API URL
- **Application Shell & Navigation**
  - `AppComponent` with Material 3 sidenav layout, toolbar, and user menu
  - `DashboardComponent` with placeholder metric cards
  - Lazy-loaded route configuration with auth guard protection
  - Light/dark theme toggle
  - `@angular/animations` dependency added for Material component animations
  - Grouped navigation menu with expandable sections
- **Manual Test Cases**
  - `docs/manual-test-cases/security-config.md` — 6 test cases
  - `docs/manual-test-cases/authentication.md` — 7 test cases
  - `docs/manual-test-cases/app-shell.md` — 8 test cases

### R1-M2: Core Academic & Lab Mapping
- **Department Management**
  - `Department` entity, repository, service, controller with CRUD (`/api/v1/departments`)
  - `DepartmentRequest` / `DepartmentResponse` DTOs (Java records)
  - Flyway migration: `V1__create_departments_table.sql`
  - Frontend: `DepartmentListComponent` (Material table with search/sort/pagination), `DepartmentFormComponent`
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/department-management.md`
- **Program & Course Management**
  - `Program` entity (`name`, `code`, `degreeType`, `durationYears`, `department`)
  - `Course` entity (`name`, `code`, `credits`, `theoryCredits`, `labCredits`, `program`, `semester`)
  - Full CRUD for programs (`/api/v1/programs`) and courses (`/api/v1/courses`)
  - Flyway migrations: `V2__create_programs_table.sql`, `V5__create_courses_table.sql`
  - Frontend: list and form components for both Program and Course
  - Premium SaaS-style Courses page with Zinc palette redesign
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/program-course-management.md`
- **Academic Year & Calendar**
  - `AcademicYear` entity (`name`, `startDate`, `endDate`, `isCurrent`)
  - `Semester` entity (`name`, `academicYear`, `startDate`, `endDate`, `semesterNumber`)
  - Full CRUD for academic years and semesters
  - Flyway migrations: `V3__create_academic_years_table.sql`, `V4__create_semesters_table.sql`
  - Frontend: list/form components and academic calendar view
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/academic-year-management.md`
- **Lab Setup & Configuration**
  - `Lab` entity (`name`, `labType`, `department`, `building`, `roomNumber`, `capacity`, `status`)
  - `LabType` enum (COMPUTER, PHYSICS, CHEMISTRY, ELECTRONICS, etc.)
  - `LabInChargeAssignment` entity (faculty/technician → lab mapping)
  - Full CRUD + assign in-charge endpoints (`/api/v1/labs`)
  - Flyway migrations: `V7__create_labs_table.sql`, `V8__create_lab_incharge_assignments_table.sql`
  - Frontend: `LabListComponent`, `LabFormComponent`, `LabDetailComponent`
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/lab-setup.md`
- **Faculty Management**
  - `Faculty` entity (`employeeCode`, `firstName`, `lastName`, `email`, `department`, `designation`, `specialization`)
  - Full CRUD (`/api/v1/faculty`) with lab teaching assignments
  - Flyway migration: `V6__create_faculty_table.sql`
  - Frontend: list, form, and detail components
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/faculty-management.md`
- **Curriculum & Lab-Curriculum Mapping**
  - `Syllabus` entity (course syllabus with theory + lab components)
  - `Experiment` entity (`name`, `course`, `experimentNumber`, `description`, `learningOutcomes`)
  - `LabCurriculumMapping` entity (experiments ↔ Course Outcomes / Program Outcomes)
  - Flyway migrations: `V9__create_syllabi_table.sql`, `V10__create_experiments_table.sql`, `V11__create_lab_curriculum_mappings_table.sql`
  - Frontend: syllabus, experiment, and CO/PO mapping components
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/curriculum-management.md`

### R1-M3: Operational Logistics
- **Student Management**
  - `Student` entity with expanded fields (personal, demographics, family, embedded `Address`)
  - `Admission` entity with full workflow (DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED)
  - `AcademicQualification` entity (SSLC, HSC, DIPLOMA, DEGREE, OTHER)
  - `AdmissionDocument` entity with 15 document types and verification workflow
  - Comprehensive enums: `Gender`, `CommunityCategory`, `BloodGroup`, `QualificationType`, `AdmissionStatus`, `DocumentType`, `DocumentVerificationStatus`
  - Full CRUD (`/api/v1/students`), admission workflow (`/api/v1/admissions`), document management
  - Flyway migrations: `V12` – `V15` (students, admissions, qualifications, documents)
  - Frontend: student list, multi-step admission form (7 steps), student detail, document upload/verification
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/student-management.md`
- **Lab Scheduling & Timetable**
  - `LabSchedule` entity (`lab`, `course`, `faculty`, `batch`, `dayOfWeek`, `startTime`, `endTime`, `semester`)
  - `LabSlot` entity for reusable time slot definitions
  - Scheduling service with conflict detection (lab, faculty, batch)
  - Flyway migrations: `V16__create_lab_slots_table.sql`, `V17__create_lab_schedules_table.sql`
  - Frontend: lab calendar view, schedule form with conflict alerts
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/lab-scheduling.md`
- **Attendance Management**
  - `Attendance` entity (`student`, `course`, `date`, `status`, `type` [THEORY/LAB])
  - `LabAttendance` entity (extends with `experiment`, `labBatch`, `labSession`)
  - Mark attendance (individual + bulk), percentage calculation, low attendance alerts
  - Flyway migrations: `V18__create_attendances_table.sql`, `V19__create_lab_attendances_table.sql`
  - Frontend: attendance marking (batch-wise), attendance report with charts
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/attendance-management.md`

### R1-M4: Finance & Asset Management
- **Fee Structure & Collection**
  - `FeeStructure` entity (`name`, `program`, `feeType`, `amount` [BigDecimal], `isMandatory`)
  - `FeePayment` entity (`student`, `feeStructure`, `amountPaid`, `paymentMethod`, `transactionId`, `status`)
  - Fee service with payment processing (all monetary values as `BigDecimal`)
  - Flyway migrations: `V20__create_fee_structures_table.sql`, `V21__create_fee_payments_table.sql`
  - Frontend: fee structure management, payment recording, high-density fee table
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/fee-management.md`
- **Student Fee Workflow**
  - `StudentFeeAllocation` entity (student ↔ fee structure allocation per academic year)
  - `SemesterFee` entity (semester-wise fee breakdown)
  - `FeeInstallment` entity (installment tracking with due dates)
  - `Penalty` entity (late fee penalties)
  - Fee explorer, fee finalization, and student fee detail components
  - Flyway migrations: `V33` – `V36` (allocations, semester fees, installments, penalties)
  - Frontend: fee explorer, finalization workflow, student fee detail page
  - Manual test cases: `docs/manual-test-cases/student-fee-workflow.md`
- **Equipment & Inventory Management**
  - `Equipment` entity (`name`, `model`, `serialNumber`, `lab`, `category`, `status`, `purchaseCost`)
  - `EquipmentStatus` enum (AVAILABLE, IN_USE, UNDER_REPAIR, DAMAGED, DISPOSED)
  - `InventoryItem` / `StockTransaction` entities for consumable tracking
  - Low stock alerts, stock in/out recording
  - Flyway migrations: `V22__create_equipment_table.sql`, `V23__create_inventory_items_table.sql`
  - Frontend: equipment list with status badges, inventory stock dashboard
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/equipment-inventory.md`
- **Maintenance & Repair**
  - `MaintenanceRequest` entity (`equipment`, `requestedBy`, `priority`, `status`, `assignedTechnician`, `repairCost`)
  - Maintenance workflow (Request → Assign → In Progress → Complete)
  - Flyway migration: `V24__create_maintenance_requests_table.sql`
  - Frontend: maintenance list, form, and workflow components
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/maintenance-management.md`

### R1-M5: Assessment & Reporting
- **Examination Management**
  - `Examination` entity (`name`, `course`, `examType` [THEORY/PRACTICAL/VIVA], `date`, `maxMarks`, `semester`)
  - `ExamResult` entity (`examination`, `student`, `marksObtained`, `grade`, `status`)
  - `LabContinuousEvaluation` entity (experiment-wise marks: record + viva + performance)
  - GPA/CGPA calculation, result processing/publishing
  - Flyway migrations: `V25` – `V27` (examinations, exam results, lab continuous evaluations)
  - Frontend: exam scheduling, marks entry (batch-wise), result view and transcript
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/examination-management.md`
- **Reports & Analytics**
  - Report service with aggregation queries (lab utilization, equipment usage, student performance, expenses)
  - Accreditation report service (CO/PO attainment, experiment completion, NBA/NAAC compliance)
  - Controllers under `/api/v1/reports`
  - Frontend: KPI dashboard, detailed reports with filters and export (PDF, Excel, CSV)
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/reports-analytics.md`

### Additional Features (Beyond Release 1)
- **System Configuration**
  - `SystemConfiguration` entity for key-value application settings
  - CRUD endpoints (`/api/v1/system-configurations`)
  - Flyway migrations: `V28__create_system_configurations_table.sql`, `V29__insert_default_configurations.sql`
  - Frontend: settings list and form components
  - Manual test cases: `docs/manual-test-cases/system-configuration.md`
- **Agent Management**
  - `Agent` entity (`name`, `phone`, `email`, `city`, `region`, `isActive`)
  - `AgentCommissionGuideline` entity (commission rules per program)
  - CRUD endpoints (`/api/v1/agents`, `/api/v1/agent-commission-guidelines`)
  - Flyway migrations: `V30__create_agents_table.sql`, `V31__create_agent_commission_guidelines_table.sql`
  - Frontend: agent list and form components
  - Manual test cases: `docs/manual-test-cases/agent-management.md`
- **Enquiry Management**
  - `Enquiry` entity (`name`, `email`, `phone`, `program`, `enquiryDate`, `source`, `status`, `agent`, `feeDiscussedAmount`)
  - Status workflow: NEW → CONTACTED → FEE_DISCUSSED → INTERESTED → CONVERTED / NOT_INTERESTED / CLOSED
  - Date range filtering, status filtering, source filtering, agent-based filtering
  - Status update endpoint (`PATCH /api/v1/enquiries/{id}/status`)
  - Convert to student endpoint (`PUT /api/v1/enquiries/{id}/convert`)
  - Flyway migration: `V32__create_enquiries_table.sql`
  - Frontend: enquiry list with date range filter, status chips with dropdown workflow, convert-to-student action
  - Frontend: enquiry form with fee structure panel (total fees, year-wise split, fee breakdown by type)
  - Unit + controller tests (95% coverage)
  - Manual test cases: `docs/manual-test-cases/enquiry-management.md`
- **Data Seeding**
  - Python demo data seeding script (`scripts/seed_demo_data.py`)
  - Manual test cases: `docs/manual-test-cases/data-seeding.md`

### UI/UX Enhancements
- Grouped navigation menu with expandable sections (Academics, Lab Management, Finance, etc.)
- Premium SaaS-style Courses page redesign with Zinc palette
