# Plan: R2-M1 — Lab Safety & Compliance

## Overview

Implement **Release 2, Milestone 1** — Lab Safety & Compliance (Module 7.8) covering safety guidelines, PPE tracking, incident reporting, safety training, and safety audits.

**Prerequisite:** Release 1 is complete and stable. All builds pass (`./gradlew check`, `ng build`).

---

## Phase 1: R2-1.1 — Safety Guidelines & PPE Tracking

### 1.1 Entities

#### `SafetyGuideline`
| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `lab` | `Lab` (FK) | nullable — if null, applies globally |
| `department` | `Department` (FK) | nullable — if null, applies globally |
| `title` | `String` | `@NotBlank`, max 255 |
| `description` | `String` | `@Column(columnDefinition = "TEXT")` |
| `category` | `SafetyGuidelineCategory` (enum) | GENERAL, CHEMICAL, ELECTRICAL, BIOLOGICAL, FIRE, EQUIPMENT, PPE |
| `priority` | `SafetyPriority` (enum) | LOW, MEDIUM, HIGH, CRITICAL |
| `isActive` | `Boolean` | default `true` |
| `effectiveDate` | `LocalDate` | when the guideline takes effect |
| `reviewDate` | `LocalDate` | nullable — next review date |
| `createdBy` | `String` | who created it |
| `createdAt` | `Instant` | `@CreationTimestamp` |
| `updatedAt` | `Instant` | `@UpdateTimestamp` |

#### `PpeItem`
| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `lab` | `Lab` (FK) | required |
| `name` | `String` | `@NotBlank`, max 255 (e.g., "Safety Goggles", "Lab Coat") |
| `category` | `PpeCategory` (enum) | EYE_PROTECTION, FACE_PROTECTION, HAND_PROTECTION, BODY_PROTECTION, FOOT_PROTECTION, RESPIRATORY, HEAD_PROTECTION, OTHER |
| `totalQuantity` | `Integer` | total stock |
| `availableQuantity` | `Integer` | currently available |
| `minimumRequired` | `Integer` | minimum threshold for alerts |
| `condition` | `PpeCondition` (enum) | GOOD, FAIR, POOR, NEEDS_REPLACEMENT |
| `lastInspectionDate` | `LocalDate` | nullable |
| `nextInspectionDate` | `LocalDate` | nullable |
| `isActive` | `Boolean` | default `true` |
| `createdAt` | `Instant` | `@CreationTimestamp` |
| `updatedAt` | `Instant` | `@UpdateTimestamp` |

### 1.2 Enums

- `SafetyGuidelineCategory`: GENERAL, CHEMICAL, ELECTRICAL, BIOLOGICAL, FIRE, EQUIPMENT, PPE
- `SafetyPriority`: LOW, MEDIUM, HIGH, CRITICAL
- `PpeCategory`: EYE_PROTECTION, FACE_PROTECTION, HAND_PROTECTION, BODY_PROTECTION, FOOT_PROTECTION, RESPIRATORY, HEAD_PROTECTION, OTHER
- `PpeCondition`: GOOD, FAIR, POOR, NEEDS_REPLACEMENT

### 1.3 Repositories

- `SafetyGuidelineRepository extends JpaRepository<SafetyGuideline, Long>`
  - `List<SafetyGuideline> findByLabId(Long labId)`
  - `List<SafetyGuideline> findByDepartmentId(Long departmentId)`
  - `List<SafetyGuideline> findByCategory(SafetyGuidelineCategory category)`
  - `List<SafetyGuideline> findByIsActiveTrue()`
- `PpeItemRepository extends JpaRepository<PpeItem, Long>`
  - `List<PpeItem> findByLabId(Long labId)`
  - `List<PpeItem> findByAvailableQuantityLessThanMinimumRequired()` (custom `@Query`)
  - `List<PpeItem> findByCondition(PpeCondition condition)`

### 1.4 DTOs (Java Records)

- `SafetyGuidelineRequest(String title, String description, Long labId, Long departmentId, SafetyGuidelineCategory category, SafetyPriority priority, LocalDate effectiveDate, LocalDate reviewDate, String createdBy)`
- `SafetyGuidelineResponse(Long id, String title, String description, Long labId, String labName, Long departmentId, String departmentName, SafetyGuidelineCategory category, SafetyPriority priority, Boolean isActive, LocalDate effectiveDate, LocalDate reviewDate, String createdBy, Instant createdAt, Instant updatedAt)`
- `PpeItemRequest(Long labId, String name, PpeCategory category, Integer totalQuantity, Integer availableQuantity, Integer minimumRequired, PpeCondition condition, LocalDate lastInspectionDate, LocalDate nextInspectionDate)`
- `PpeItemResponse(Long id, Long labId, String labName, String name, PpeCategory category, Integer totalQuantity, Integer availableQuantity, Integer minimumRequired, PpeCondition condition, LocalDate lastInspectionDate, LocalDate nextInspectionDate, Boolean isActive, Instant createdAt, Instant updatedAt)`

### 1.5 Services

#### `SafetyGuidelineService`
- `@Transactional(readOnly = true)` at class level
- `create(SafetyGuidelineRequest)` → `@Transactional`
- `findAll()` / `findById(Long)` / `findByLabId(Long)` / `findByDepartmentId(Long)` / `findByCategory(SafetyGuidelineCategory)`
- `findActive()` — only active guidelines
- `update(Long, SafetyGuidelineRequest)` → `@Transactional`
- `deactivate(Long)` → `@Transactional` — set `isActive = false`
- `delete(Long)` → `@Transactional`

#### `PpeItemService`
- `@Transactional(readOnly = true)` at class level
- `create(PpeItemRequest)` → `@Transactional`
- `findAll()` / `findById(Long)` / `findByLabId(Long)`
- `findLowStock()` — items where `availableQuantity < minimumRequired`
- `findByCondition(PpeCondition)`
- `update(Long, PpeItemRequest)` → `@Transactional`
- `delete(Long)` → `@Transactional`

### 1.6 Controllers

#### `SafetyGuidelineController` — `/api/v1/safety-guidelines`
| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/` | ROLE_ADMIN, ROLE_LAB_INCHARGE | Create guideline |
| `GET` | `/` | authenticated | List all (with optional query params: `labId`, `departmentId`, `category`) |
| `GET` | `/{id}` | authenticated | Get by ID |
| `PUT` | `/{id}` | ROLE_ADMIN, ROLE_LAB_INCHARGE | Update |
| `PATCH` | `/{id}/deactivate` | ROLE_ADMIN | Deactivate |
| `DELETE` | `/{id}` | ROLE_ADMIN | Delete |

#### `PpeItemController` — `/api/v1/ppe-items`
| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/` | ROLE_ADMIN, ROLE_LAB_INCHARGE | Create PPE item |
| `GET` | `/` | authenticated | List all (with optional `labId`, `condition` params) |
| `GET` | `/{id}` | authenticated | Get by ID |
| `GET` | `/low-stock` | ROLE_ADMIN, ROLE_LAB_INCHARGE | Low stock alerts |
| `PUT` | `/{id}` | ROLE_ADMIN, ROLE_LAB_INCHARGE | Update |
| `DELETE` | `/{id}` | ROLE_ADMIN | Delete |

### 1.7 Flyway Migrations

- `V37__create_safety_guidelines_table.sql`
- `V38__create_ppe_items_table.sql`

### 1.8 Tests

- `SafetyGuidelineServiceTest` — `@ExtendWith(MockitoExtension.class)` — CRUD, filtering, deactivation, not-found exceptions
- `SafetyGuidelineControllerTest` — `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)` — all endpoints
- `PpeItemServiceTest` — CRUD, low stock logic, not-found exceptions
- `PpeItemControllerTest` — all endpoints

---

## Phase 2: R2-1.2 — Incident Reporting

### 2.1 Entities

#### `IncidentReport`
| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `lab` | `Lab` (FK) | required |
| `reportedBy` | `String` | `@NotBlank` — name of the reporter |
| `reportedByEmail` | `String` | nullable |
| `incidentDate` | `LocalDate` | `@NotNull` |
| `incidentTime` | `LocalTime` | nullable |
| `title` | `String` | `@NotBlank`, max 255 |
| `description` | `String` | `@Column(columnDefinition = "TEXT")` |
| `severity` | `IncidentSeverity` (enum) | MINOR, MODERATE, MAJOR, CRITICAL |
| `incidentType` | `IncidentType` (enum) | CHEMICAL_SPILL, ELECTRICAL, FIRE, EQUIPMENT_FAILURE, INJURY, NEAR_MISS, PROPERTY_DAMAGE, OTHER |
| `status` | `IncidentStatus` (enum) | REPORTED, UNDER_INVESTIGATION, ACTION_TAKEN, RESOLVED, CLOSED |
| `actionTaken` | `String` | `TEXT`, nullable — resolution description |
| `investigatedBy` | `String` | nullable |
| `resolvedDate` | `LocalDate` | nullable |
| `preventiveMeasures` | `String` | `TEXT`, nullable |
| `createdAt` | `Instant` | `@CreationTimestamp` |
| `updatedAt` | `Instant` | `@UpdateTimestamp` |

### 2.2 Enums

- `IncidentSeverity`: MINOR, MODERATE, MAJOR, CRITICAL
- `IncidentType`: CHEMICAL_SPILL, ELECTRICAL, FIRE, EQUIPMENT_FAILURE, INJURY, NEAR_MISS, PROPERTY_DAMAGE, OTHER
- `IncidentStatus`: REPORTED, UNDER_INVESTIGATION, ACTION_TAKEN, RESOLVED, CLOSED

### 2.3 Repository

- `IncidentReportRepository extends JpaRepository<IncidentReport, Long>`
  - `List<IncidentReport> findByLabId(Long labId)`
  - `List<IncidentReport> findByStatus(IncidentStatus status)`
  - `List<IncidentReport> findBySeverity(IncidentSeverity severity)`
  - `List<IncidentReport> findByIncidentDateBetween(LocalDate from, LocalDate to)`

### 2.4 DTOs

- `IncidentReportRequest(Long labId, String reportedBy, String reportedByEmail, LocalDate incidentDate, LocalTime incidentTime, String title, String description, IncidentSeverity severity, IncidentType incidentType, IncidentStatus status, String actionTaken, String investigatedBy, LocalDate resolvedDate, String preventiveMeasures)`
- `IncidentReportResponse(Long id, Long labId, String labName, String reportedBy, String reportedByEmail, LocalDate incidentDate, LocalTime incidentTime, String title, String description, IncidentSeverity severity, IncidentType incidentType, IncidentStatus status, String actionTaken, String investigatedBy, LocalDate resolvedDate, String preventiveMeasures, Instant createdAt, Instant updatedAt)`

### 2.5 Service — `IncidentReportService`

- `create(IncidentReportRequest)` → `@Transactional`
- `findAll()` / `findById(Long)` / `findByLabId(Long)` / `findByStatus(IncidentStatus)` / `findBySeverity(IncidentSeverity)`
- `findByDateRange(LocalDate from, LocalDate to)`
- `update(Long, IncidentReportRequest)` → `@Transactional`
- `updateStatus(Long, IncidentStatus)` → `@Transactional` — workflow transition
- `delete(Long)` → `@Transactional`

### 2.6 Controller — `IncidentReportController` — `/api/v1/incident-reports`

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/` | ROLE_ADMIN, ROLE_LAB_INCHARGE, ROLE_FACULTY | Report incident |
| `GET` | `/` | authenticated | List all (optional params: `labId`, `status`, `severity`, `fromDate`, `toDate`) |
| `GET` | `/{id}` | authenticated | Get by ID |
| `PUT` | `/{id}` | ROLE_ADMIN, ROLE_LAB_INCHARGE | Update full report |
| `PATCH` | `/{id}/status` | ROLE_ADMIN, ROLE_LAB_INCHARGE | Update status (workflow) |
| `DELETE` | `/{id}` | ROLE_ADMIN | Delete |

### 2.7 Flyway Migration

- `V39__create_incident_reports_table.sql`

### 2.8 Tests

- `IncidentReportServiceTest` — CRUD, workflow status transitions, filtering, not-found
- `IncidentReportControllerTest` — all endpoints

---

## Phase 3: R2-1.3 — Safety Training & Audits

### 3.1 Entities

#### `SafetyTrainingRecord`
| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `trainee` | `String` | `@NotBlank` — name of the person trained |
| `traineeType` | `TraineeType` (enum) | STUDENT, FACULTY, TECHNICIAN |
| `trainingTitle` | `String` | `@NotBlank`, max 255 |
| `description` | `String` | `TEXT`, nullable |
| `lab` | `Lab` (FK) | nullable — if null, general safety training |
| `conductedBy` | `String` | `@NotBlank` |
| `trainingDate` | `LocalDate` | `@NotNull` |
| `validUntil` | `LocalDate` | nullable — certification expiry |
| `status` | `TrainingStatus` (enum) | SCHEDULED, COMPLETED, FAILED, EXPIRED |
| `score` | `Integer` | nullable — assessment score if applicable |
| `createdAt` | `Instant` | `@CreationTimestamp` |
| `updatedAt` | `Instant` | `@UpdateTimestamp` |

#### `SafetyAudit`
| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | `@GeneratedValue(IDENTITY)` |
| `lab` | `Lab` (FK) | required |
| `auditorName` | `String` | `@NotBlank` |
| `auditDate` | `LocalDate` | `@NotNull` |
| `nextAuditDate` | `LocalDate` | nullable |
| `overallRating` | `AuditRating` (enum) | EXCELLENT, GOOD, SATISFACTORY, NEEDS_IMPROVEMENT, UNSATISFACTORY |
| `findings` | `String` | `TEXT`, nullable |
| `recommendations` | `String` | `TEXT`, nullable |
| `status` | `AuditStatus` (enum) | SCHEDULED, IN_PROGRESS, COMPLETED, FOLLOW_UP_REQUIRED |
| `createdAt` | `Instant` | `@CreationTimestamp` |
| `updatedAt` | `Instant` | `@UpdateTimestamp` |

### 3.2 Enums

- `TraineeType`: STUDENT, FACULTY, TECHNICIAN
- `TrainingStatus`: SCHEDULED, COMPLETED, FAILED, EXPIRED
- `AuditRating`: EXCELLENT, GOOD, SATISFACTORY, NEEDS_IMPROVEMENT, UNSATISFACTORY
- `AuditStatus`: SCHEDULED, IN_PROGRESS, COMPLETED, FOLLOW_UP_REQUIRED

### 3.3 Repositories, Services, Controllers, DTOs, Tests

Follow identical patterns to Phase 1 and Phase 2:
- `SafetyTrainingRecordRepository` / `SafetyTrainingRecordService` / `SafetyTrainingRecordController` at `/api/v1/safety-training`
- `SafetyAuditRepository` / `SafetyAuditService` / `SafetyAuditController` at `/api/v1/safety-audits`

### 3.4 Flyway Migrations

- `V40__create_safety_training_records_table.sql`
- `V41__create_safety_audits_table.sql`

---

## Phase 4: R2-1.4 — Frontend Components

### 4.1 Feature Folder Structure

```
frontend/src/app/features/lab-safety/
├── safety-guideline-list/
│   ├── safety-guideline-list.component.ts
│   ├── safety-guideline-list.component.html
│   └── safety-guideline-list.component.scss
├── safety-guideline-form/
│   ├── safety-guideline-form.component.ts
│   ├── safety-guideline-form.component.html
│   └── safety-guideline-form.component.scss
├── incident-report-list/
│   ├── incident-report-list.component.ts
│   ├── incident-report-list.component.html
│   └── incident-report-list.component.scss
├── incident-report-form/
│   ├── incident-report-form.component.ts
│   ├── incident-report-form.component.html
│   └── incident-report-form.component.scss
├── ppe-dashboard/
│   ├── ppe-dashboard.component.ts
│   ├── ppe-dashboard.component.html
│   └── ppe-dashboard.component.scss
├── ppe-form/
│   ├── ppe-form.component.ts
│   ├── ppe-form.component.html
│   └── ppe-form.component.scss
├── training-record-list/
│   ├── training-record-list.component.ts
│   ├── training-record-list.component.html
│   └── training-record-list.component.scss
├── lab-safety.model.ts
└── lab-safety.service.ts
```

### 4.2 Components

- **Safety Guidelines List** — Material table with filters (lab, department, category, priority), search, sort, pagination. Active/inactive toggle.
- **Safety Guidelines Form** — Create/edit form with lab/department dropdowns, category/priority selects, date pickers.
- **Incident Report List** — Material table with severity badges, status chips with workflow dropdown (same pattern as enquiry list), date range filter.
- **Incident Report Form** — Full form with lab dropdown, severity/type selects, date/time pickers, text areas for description/action/preventive measures.
- **PPE Dashboard** — Card grid showing PPE items per lab, low stock alerts highlighted, condition badges.
- **PPE Form** — Create/edit form for PPE items with lab dropdown, quantity fields, condition select, date pickers.
- **Training Records List** — Material table with trainee type filter, status badges, score column.

### 4.3 Routes (add to `app.routes.ts`)

```
/safety-guidelines           → SafetyGuidelineListComponent
/safety-guidelines/new       → SafetyGuidelineFormComponent
/safety-guidelines/:id/edit  → SafetyGuidelineFormComponent
/incident-reports            → IncidentReportListComponent
/incident-reports/new        → IncidentReportFormComponent
/incident-reports/:id/edit   → IncidentReportFormComponent
/ppe-items                   → PpeDashboardComponent
/ppe-items/new               → PpeFormComponent
/ppe-items/:id/edit          → PpeFormComponent
/safety-training             → TrainingRecordListComponent
```

### 4.4 Navigation

Add a **"Lab Safety"** expandable section in the sidenav under the existing Lab Management group with links to all four sub-features.

---

## Phase 5: R2-1.5 — Tests & Coverage

- All backend service tests with `@ExtendWith(MockitoExtension.class)`
- All backend controller tests with `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)`
- Maintain ≥ 95% JaCoCo coverage
- Verify `./gradlew check` passes
- Verify `ng build` passes

---

## Phase 6: R2-1.6 — Manual Test Cases

Create `docs/manual-test-cases/lab-safety.md` with test cases covering:
- Safety guideline CRUD (TC-SAFE-001 through TC-SAFE-006)
- PPE item CRUD + low stock alerts (TC-SAFE-007 through TC-SAFE-012)
- Incident report CRUD + status workflow (TC-SAFE-013 through TC-SAFE-020)
- Safety training CRUD (TC-SAFE-021 through TC-SAFE-025)
- Safety audit CRUD (TC-SAFE-026 through TC-SAFE-030)
- Frontend components (TC-SAFE-031 through TC-SAFE-040)

---

## Implementation Order

1. **Enums** — all 10 enums in `com.cms.model.enums`
2. **Entities** — `SafetyGuideline`, `PpeItem`, `IncidentReport`, `SafetyTrainingRecord`, `SafetyAudit`
3. **Flyway** — `V37` through `V41`
4. **Repositories** — 5 repository interfaces
5. **DTOs** — 10 Java records (request + response for each entity)
6. **Services** — 5 service classes with full CRUD + filtering
7. **Controllers** — 5 REST controllers with `@PreAuthorize`
8. **Backend Tests** — 10 test classes (service + controller for each)
9. **Frontend Model + Service** — `lab-safety.model.ts`, `lab-safety.service.ts`
10. **Frontend Components** — 7 components (list + form pairs + PPE dashboard)
11. **Routes + Navigation** — add routes and sidenav links
12. **Manual Test Cases** — `docs/manual-test-cases/lab-safety.md`
13. **CHANGELOG** — update with R2-M1 entries

