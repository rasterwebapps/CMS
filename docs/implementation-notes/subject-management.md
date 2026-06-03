# Subject Management — Implementation Notes

**Date:** May 7, 2026

---

## Changes Completed

### 1. API Path Fix
`SubjectController` base path corrected from `/subjects` to `/api/v1/subjects` to match the project-wide `/api/v1` prefix convention.

### 2. Test Updates
All 9 methods in `SubjectControllerTest` updated to use `/api/v1/subjects`.

### 3. Bug Fix — `CourseControllerTest` Compilation
4 compilation errors fixed: missing `rollNumberCode` parameter in `CourseRequest` and `CourseResponse` constructors (lines 78, 214–216, 221–222, 240).

### 4. Manual Test Cases
Created `docs/manual-test-cases/subject-management.md` with 23 test cases covering CRUD, validation, authorization, business rules (code uniqueness, credits calculation, semester validation), and integration.

---

## Test Results

| Suite | Tests | Passed |
|-------|-------|--------|
| SubjectControllerTest | 9 | 9 ✅ |
| CourseControllerTest | 15 | 15 ✅ |

---

## Files Modified

1. `backend/src/main/java/com/cms/controller/SubjectController.java` — path fix
2. `backend/src/test/java/com/cms/controller/SubjectControllerTest.java` — path updates
3. `backend/src/test/java/com/cms/controller/CourseControllerTest.java` — constructor fixes
4. `docs/manual-test-cases/subject-management.md` — new (23 test cases)
5. `CHANGELOG.md` — updated
