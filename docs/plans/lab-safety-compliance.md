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
| `speciality` | `Speciality` (FK) | nullable — if null, applies globally |
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
| `name` | `String` | `@NotBlank`, max 255 |
| `category` | `PpeCategory` (enum) | EYE_PROTECTION, FACE_PROTECTION, HAND_PROTECTION, BODY_PROTECTION, FOOT_PROTECTION, RESPIRATORY, HEAD_PROTECTION, OTHER |
| `totalQuantity` | `Integer` | total stock |
| `availableQuantity` | `Integer` | currently available |
| `minimumRequired` | `Integer` | minimum threshold for alerts |
| `condition` | `PpeCondition` (enum) | GOOD, FAIR, POOR, NEEDS_REPLACEMENT |
| `lastInspectionDate` | `LocalDate` | nullable |
| `nextInspectionDate` | `LocalDate` | nullable |
| `isActive` | `Boolean` | default `true` |

### 1.2 Enums
- `SafetyGuidelineCategory`: GENERAL, CHEMICAL, ELECTRICAL, BIOLOGICAL, FIRE, EQUIPMENT, PPE
- `SafetyPriority`: LOW, MEDIUM, HIGH, CRITICAL
- `PpeCategory`: EYE_PROTECTION, FACE_PROTECTION, HAND_PROTECTION, BODY_PROTECTION, FOOT_PROTECTION, RESPIRATORY, HEAD_PROTECTION, OTHER
- `PpeCondition`: GOOD, FAIR, POOR, NEEDS_REPLACEMENT

### 1.3 Repositories
- `SafetyGuidelineRepository` — `findByLabId`, `findBySpecialityId`, `findByCategory`, `findByIsActiveTrue`
- `PpeItemRepository` — `findByLabId`, `findByAvailableQuantityLessThanMinimumRequired`, `findByCondition`

### 1.4 DTOs (Java Records)
- `SafetyGuidelineRequest` / `SafetyGuidelineResponse`
- `PpeItemRequest` / `PpeItemResponse`

### 1.5 Services
- `SafetyGuidelineService` — CRUD + `findActive()` + `deactivate()`
- `PpeItemService` — CRUD + `findLowStock()` + `findByCondition()`

### 1.6 Controllers

#### `SafetyGuidelineController` — `/api/v1/safety-guidelines`
| Method | Path | Role |
|--------|------|------|
| POST | `/` | ROLE_ADMIN, ROLE_LAB_INCHARGE |
| GET | `/` | authenticated |
| GET | `/{id}` | authenticated |
| PUT | `/{id}` | ROLE_ADMIN, ROLE_LAB_INCHARGE |
| PATCH | `/{id}/deactivate` | ROLE_ADMIN |
| DELETE | `/{id}` | ROLE_ADMIN |

#### `PpeItemController` — `/api/v1/ppe-items`
| Method | Path | Role |
|--------|------|------|
| POST | `/` | ROLE_ADMIN, ROLE_LAB_INCHARGE |
| GET | `/` | authenticated |
| GET | `/{id}` | authenticated |
| GET | `/low-stock` | ROLE_ADMIN, ROLE_LAB_INCHARGE |
| PUT | `/{id}` | ROLE_ADMIN, ROLE_LAB_INCHARGE |
| DELETE | `/{id}` | ROLE_ADMIN |

### 1.7 Flyway Migrations
- `V37__create_safety_guidelines_table.sql`
- `V38__create_ppe_items_table.sql`

---

## Phase 2: R2-1.2 — Incident Reporting

### 2.1 Entity: `IncidentReport`
| Field | Type | Notes |
|-------|------|-------|
| `lab` | `Lab` (FK) | required |
| `reportedBy` | `String` | `@NotBlank` |
| `incidentDate` | `LocalDate` | `@NotNull` |
| `severity` | `IncidentSeverity` | MINOR, MODERATE, MAJOR, CRITICAL |
| `incidentType` | `IncidentType` | CHEMICAL_SPILL, ELECTRICAL, FIRE, EQUIPMENT_FAILURE, INJURY, NEAR_MISS, PROPERTY_DAMAGE, OTHER |
| `status` | `IncidentStatus` | REPORTED, UNDER_INVESTIGATION, ACTION_TAKEN, RESOLVED, CLOSED |
| `actionTaken` | `String` | TEXT, nullable |
| `preventiveMeasures` | `String` | TEXT, nullable |

### 2.2 Controller — `IncidentReportController` — `/api/v1/incident-reports`
| Method | Path | Role |
|--------|------|------|
| POST | `/` | ROLE_ADMIN, ROLE_LAB_INCHARGE, ROLE_FACULTY |
| GET | `/` | authenticated (params: `labId`, `status`, `severity`, `fromDate`, `toDate`) |
| GET | `/{id}` | authenticated |
| PUT | `/{id}` | ROLE_ADMIN, ROLE_LAB_INCHARGE |
| PATCH | `/{id}/status` | ROLE_ADMIN, ROLE_LAB_INCHARGE |
| DELETE | `/{id}` | ROLE_ADMIN |

### 2.3 Flyway Migration
- `V39__create_incident_reports_table.sql`

---

## Phase 3: R2-1.3 — Safety Training & Audits

### Entities
- `SafetyTrainingRecord` — trainee, type, lab, conductedBy, trainingDate, validUntil, status (SCHEDULED/COMPLETED/FAILED/EXPIRED), score
- `SafetyAudit` — lab, auditorName, auditDate, nextAuditDate, overallRating (EXCELLENT/GOOD/SATISFACTORY/NEEDS_IMPROVEMENT/UNSATISFACTORY), findings, recommendations, status (SCHEDULED/IN_PROGRESS/COMPLETED/FOLLOW_UP_REQUIRED)

### Controllers
- `SafetyTrainingRecordController` — `/api/v1/safety-training`
- `SafetyAuditController` — `/api/v1/safety-audits`

### Flyway Migrations
- `V40__create_safety_training_records_table.sql`
- `V41__create_safety_audits_table.sql`

---

## Phase 4: R2-1.4 — Frontend Components

### Feature Folder: `frontend/src/app/features/lab-safety/`
- `safety-guideline-list/` + `safety-guideline-form/`
- `incident-report-list/` + `incident-report-form/`
- `ppe-dashboard/` + `ppe-form/`
- `training-record-list/`
- `lab-safety.model.ts` + `lab-safety.service.ts`

### Routes
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

### Navigation
Add **"Lab Safety"** expandable section under the Lab Management sidenav group.

---

## Phase 5: R2-1.5 — Tests & Coverage

- Service tests: `@ExtendWith(MockitoExtension.class)`
- Controller tests: `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)`
- Maintain ≥ 95% JaCoCo coverage
- `./gradlew check` ✅ and `ng build` ✅ must pass

---

## Phase 6: R2-1.6 — Manual Test Cases

Create `docs/manual-test-cases/lab-safety.md`:
- Safety guideline CRUD (TC-SAFE-001–006)
- PPE item CRUD + low stock alerts (TC-SAFE-007–012)
- Incident report CRUD + status workflow (TC-SAFE-013–020)
- Safety training CRUD (TC-SAFE-021–025)
- Safety audit CRUD (TC-SAFE-026–030)
- Frontend components (TC-SAFE-031–040)

---

## Implementation Order

1. Enums (10 enums in `com.cms.model.enums`)
2. Entities (5 entities)
3. Flyway migrations V37–V41
4. Repositories (5)
5. DTOs (10 Java records)
6. Services (5)
7. Controllers (5)
8. Backend tests (10 test classes)
9. Frontend model + service
10. Frontend components (7)
11. Routes + navigation
12. Manual test cases
13. CHANGELOG update
