# RBAC Implementation — Role-Based Access Control

## Overview

Two separate RBAC efforts are documented here:

1. **Initial RBAC** (April 27, 2026) — Navigation menu filtering and backend API authorization for College Admin, Front Office, and Cashier roles.
2. **RBAC Fixes** (April 27, 2026) — Corrected College Admin and Cashier shortcut access from the Enquiry list.

---

## Part 1 — Initial RBAC Implementation

### Frontend Navigation Menu (`frontend/src/app/app.ts`)

- Added `roles?: string[]` to `NavGroup` interface for group-level role restrictions.
- Added `ROLE_COLLEGE_ADMIN` and `ROLE_CASHIER` to `CMS_ROLE_NAMES` and `primaryRole` priority list.
- Updated `filteredNavEntries` computed signal to filter groups and items by role.

#### Navigation Access Matrix

| Menu Item | College Admin | Front Office | Cashier | Admin |
|-----------|:----:|:----:|:----:|:----:|
| Dashboard | ✓ | ✓ | ✓ | ✓ |
| Preferences (group) | ✓ | ✗ | ✗ | ✓ |
| - Settings | ✗ | - | - | ✓ |
| Admission Management | ✓ | ✓ | ✓* | ✓ |
| - Enquiries | ✓* | ✓* | ✓* | ✓ |
| - Submit Documents | ✓ | ✓ | ✗ | ✓ |
| - Admissions | ✓ | ✓ | ✗ | ✓ |
| - Students | ✓ | ✓ | ✗ | ✓ |
| Finance | ✓ | ✗ | ✓ | ✓ |
| - Student Fees | ✓ | - | ✗ | ✓ |
| - Fee Payments | ✓ | - | ✓ | ✓ |
| - Fee Finalization | ✓ | - | ✗ | ✓ |
| Reports | ✓ | ✓ | ✓ | ✓ |

*Read-only for Cashier.

### Backend API Authorization

#### `FeePaymentController`
- `POST /fee-payments`: Added `ROLE_CASHIER`.

#### `StudentFeeController`
- `POST /student-fees/finalize`: Removed `ROLE_CASHIER` (Cashier cannot finalize fees).
- `POST /student-fees/{studentId}/collect`: Added `ROLE_COLLEGE_ADMIN`.

---

## Part 2 — RBAC Fixes (College Admin + Cashier Shortcuts)

### Problem
- College Admin could not finalize fees or collect payments from Enquiry list shortcuts.
- Cashier could not collect payments from Enquiry list shortcuts.

### Root Cause
- `canFinalizeFee()` only checked `isAdmin()`, missing `isCollegeAdmin()`.
- `canCollectPayment()` was missing `isCollegeAdmin()` and `isCashier()`.
- `EnquiryController` payment endpoints were missing `ROLE_CASHIER`.

### Backend Fixes — `EnquiryController`

```java
// Fee Finalization — removed ROLE_FRONT_OFFICE
@PostMapping("/{id}/finalize-fees")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")

// Collect Payment — added ROLE_CASHIER
@PostMapping("/{id}/payments")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN') or hasRole('ROLE_FRONT_OFFICE') or hasRole('ROLE_CASHIER')")

// Get Payments — added ROLE_CASHIER
@GetMapping("/{id}/payments")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN') or hasRole('ROLE_FRONT_OFFICE') or hasRole('ROLE_CASHIER')")
```

### Frontend Fixes — `EnquiryListComponent`

```typescript
protected canFinalizeFee(item: Enquiry): boolean {
  return item.status === 'INTERESTED' && (this.authService.isAdmin() || this.authService.isCollegeAdmin());
}

protected canCollectPayment(item: Enquiry): boolean {
  return (item.status === 'FEES_FINALIZED' || item.status === 'PARTIALLY_PAID') &&
    (this.authService.isAdmin() || this.authService.isCollegeAdmin() || this.authService.isCashier());
}
```

---

## Final Access Control Matrix

### College Admin (ROLE_COLLEGE_ADMIN)
| Operation | Via Menu | Via Enquiry Shortcut | Backend API |
|-----------|:---:|:---:|:---:|
| Finalize Fees | ✅ | ✅ | ✅ |
| Collect Payments | ✅ | ✅ | ✅ |
| Submit Documents | ✅ | ✅ | ✅ |
| Create Admissions | ✅ | ✅ | ✅ |

### Cashier (ROLE_CASHIER)
| Operation | Via Menu | Via Enquiry Shortcut | Backend API |
|-----------|:---:|:---:|:---:|
| Finalize Fees | ❌ | ❌ | ❌ (403) |
| Collect Payments | ✅ | ✅ | ✅ |
| Submit Documents | ❌ | ❌ | ❌ (403) |
| Create Admissions | ❌ | ❌ | ❌ (403) |

### Front Office (ROLE_FRONT_OFFICE)
| Operation | Via Menu | Via Enquiry Shortcut | Backend API |
|-----------|:---:|:---:|:---:|
| Finalize Fees | ❌ | ❌ | ❌ (403) |
| Collect Payments | ✅ | ✅ | ✅ |
| Submit Documents | ✅ | ✅ | ✅ |
| Create Admissions | ✅ | ✅ | ✅ |

---

## Files Modified

### Backend
- `backend/src/main/java/com/cms/controller/EnquiryController.java`
- `backend/src/main/java/com/cms/controller/FeePaymentController.java`
- `backend/src/main/java/com/cms/controller/StudentFeeController.java`

### Frontend
- `frontend/src/app/app.ts`
- `frontend/src/app/features/enquiry/enquiry-list/enquiry-list.component.ts`

### Documentation
- `docs/manual-test-cases/rbac-updates.md`
- `docs/manual-test-cases/rbac-fixes-college-admin-cashier.md`

---

## Deployment Notes

- No database migrations required.
- Keycloak realm must have: `ROLE_ADMIN`, `ROLE_COLLEGE_ADMIN`, `ROLE_FRONT_OFFICE`, `ROLE_CASHIER`.
- Both frontend (UI) and backend (`@PreAuthorize`) enforce authorization — users cannot bypass UI via direct API calls.
- All builds verified: `./gradlew check` ✅, `ng build` ✅, tests 100% passing.
