# Admission and Student Explorer Load Stability — Manual Test Cases

## TC-EXPLR-LOAD-001: Admission Explorer exits loading state when API is slow

**Preconditions:**
- User is logged in with permission to access Admission Explorer.
- Browser DevTools Network tab is open.
- Use Network throttling (Slow 3G) or a proxy to delay `/api/v1/admissions` response beyond 20 seconds.

**Steps:**
1. Navigate to Admission Management -> Admission Explorer.
2. Keep the delayed `/api/v1/admissions` request running for more than 20 seconds.
3. Observe the page after timeout.

**Expected Result:**
- Skeleton loader stops after timeout.
- A toast is shown: "Admissions are taking too long to load. Please retry."
- Page remains interactive (filters and menu clicks still respond).

**Status:** NOT TESTED

---

## TC-EXPLR-LOAD-002: Student Explorer exits loading state when API is slow

**Preconditions:**
- User is logged in with permission to access Student Explorer.
- Browser DevTools Network tab is open.
- Use Network throttling (Slow 3G) or a proxy to delay `/api/v1/students` response beyond 20 seconds.

**Steps:**
1. Navigate to Students -> Student Explorer.
2. Keep the delayed `/api/v1/students` request running for more than 20 seconds.
3. Observe the page after timeout.

**Expected Result:**
- Skeleton loader stops after timeout.
- A toast is shown: "Students are taking too long to load. Please retry."
- Page remains interactive and does not freeze.

**Status:** NOT TESTED

---

## TC-EXPLR-LOAD-003: Search/filter does not crash when name fields are empty

**Preconditions:**
- At least one admission or student record has an empty/null name field (or equivalent sparse data from migration/legacy records).
- User can access both explorer screens.

**Steps:**
1. Open Admission Explorer and type any value in search.
2. Clear search and apply Program/Status/Batch/Course filters.
3. Open Student Explorer and type any value in search.
4. Clear search and apply Program/Status/Semester/Fee filters.

**Expected Result:**
- No console runtime error related to `toLowerCase` on null/undefined.
- Filters/search continue to work.
- UI does not get stuck in skeleton state.

**Status:** NOT TESTED

