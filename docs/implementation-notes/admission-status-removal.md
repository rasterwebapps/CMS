# AdmissionStatus Removal

**Date:** May 2, 2026

## Summary

Removed the redundant `AdmissionStatus` enum and field from both backend and frontend. An admission record is now a simple enrollment event; student lifecycle is tracked exclusively via `Student.status`.

---

## Architecture Change

### Before
```java
// Admission had workflow status (never changed after creation)
AdmissionStatus: DRAFT, SUBMITTED, UNDER_REVIEW, DOCUMENTS_PENDING, APPROVED, REJECTED

// Student had lifecycle status
StudentStatus: ACTIVE, INACTIVE, GRADUATED, ON_LEAVE, SUSPENDED, WITHDRAWN, EXPELLED
```

### After
```java
// Admission is a plain enrollment event — no status field
// If admission record exists → student was enrolled

// Student.status is the ONLY lifecycle tracker
StudentStatus: ACTIVE, INACTIVE, GRADUATED, ON_LEAVE, SUSPENDED, WITHDRAWN, EXPELLED
```

---

## Backend Changes

### Removed
- `backend/src/main/java/com/cms/model/enums/AdmissionStatus.java` — enum deleted

### Modified
- `Admission.java` — removed `status` field
- `AdmissionRequest.java` — removed `status` parameter (7 params remaining)
- `AdmissionResponse.java` — removed `status` field (13 fields remaining)
- `AdmissionRepository.java` — removed `findByStatus()`, added `findByJoiningAcademicYearId()`
- `AdmissionService.java` — removed `updateStatus()`, updated create/update/toResponse
- `EnquiryService.java` — removed `AdmissionStatus.APPROVED` from admission creation
- `StudentImportService.java` — removed status parameter from `createAdmission()`
- `DashboardService.java` — replaced `pendingAdmissionsCount` with `totalAdmissions`
- `AdmissionController.java` — removed `PATCH /{id}/status` endpoint
- `FrontOfficeDashboardResponse.java` — renamed `pendingAdmissionsCount` → `totalAdmissions`

### Database Migration
- `V81__remove_admission_status.sql` — `ALTER TABLE admissions DROP COLUMN IF EXISTS status;`

### Test Results
- 1425 tests, 0 failures ✅

---

## Frontend Changes (17 files)

### Models / Services
- `admission.model.ts` — removed `status` from `AdmissionRequest` and `AdmissionResponse`; removed `ADMISSION_STATUSES` constant
- `admission.service.ts` — removed `updateStatus()` method

### Admission List
- Removed status filter dropdown, status-based stat badges (Pending/Approved/Enrolled), status column from table
- Kept only "Total" stat badge

### Admission Form
- Removed `status` dropdown field (was always defaulted to "SUBMITTED")

### Admission Detail
- Removed status chip display

### Front Office Dashboard
- `dashboard.models.ts` — renamed `pendingAdmissionsCount` → `totalAdmissions` in `FrontOfficeDashboard`
- `front-office-dashboard.component.html` — updated KPI card to show "Total Admissions"

### Import Feature
- `import.model.ts` / `import.service.ts` / `import.component.ts` / `import.component.html` — removed `defaultAdmissionStatus` field

### Tours
- `admission.tours.ts` — updated step 2 description (removed status filter reference)

---

## API Changes

### Removed Endpoints
- `PATCH /admissions/{id}/status` — no longer needed

### Updated Endpoints
- `POST/PUT /admissions` — request and response no longer include `status`
- `GET /dashboard/front-office` — field renamed `pendingAdmissionsCount` → `totalAdmissions`

---

## Build Verification
- `./gradlew compileJava` ✅
- `./gradlew test` — 1425 tests, 0 failures ✅
- `npm run build` ✅ (no TypeScript errors)
