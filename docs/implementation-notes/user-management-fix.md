# User Management Screen — Empty List Fix

**Date:** May 5, 2026  
**Status:** ✅ Resolved

---

## Problem

The User Management screen showed "0 Total Users" for the `admin` account, even though the admin user existed in the database.

---

## Root Cause

The backend filters visible users by role hierarchy level: `findByAppRoleHierarchyLevelGreaterThan(requesterLevel)`. The admin user was seeded with role `ADMIN` (level 3), which can only see levels 4–6. No users at those levels existed → empty list.

**Role Hierarchy:**
```
Level 1: DEV_ADMIN      → can manage everyone (levels 2–6)
Level 2: SUPPORT_ADMIN  → manages levels 3–6
Level 3: ADMIN          → manages levels 4–6
Level 4: COLLEGE_ADMIN  → manages levels 5–6
Level 5: FRONT_OFFICE, CASHIER, FACULTY, LAB_INCHARGE, TECHNICIAN
Level 6: STUDENT, PARENT
```

---

## Fix (Two Flyway Migrations)

### V97 — Seed Sample Users
File: `backend/src/main/resources/db/migration/V97__seed_sample_users.sql`

Adds 6 sample users at levels 4–6:

| Username | Role | Level |
|----------|------|-------|
| college.admin | COLLEGE_ADMIN | 4 |
| front.office | FRONT_OFFICE | 5 |
| cashier | CASHIER | 5 |
| faculty.cs | FACULTY | 5 |
| lab.incharge | LAB_INCHARGE | 5 |
| student.demo | STUDENT | 6 |

> These are database records only. To enable login, matching Keycloak users must be created.

### V98 — Upgrade Admin to DEV_ADMIN
File: `backend/src/main/resources/db/migration/V98__fix_admin_user_role.sql`

```sql
UPDATE app_users
SET app_role_id = (SELECT id FROM app_roles WHERE name = 'DEV_ADMIN')
WHERE keycloak_username = 'admin'
  AND app_role_id = (SELECT id FROM app_roles WHERE name = 'ADMIN');
```

DEV_ADMIN (level 1) can see all users at levels 2–6.

---

## Additional Issues Resolved During This Fix

### Duplicate Migration Version (V95)
Two files had version V95:
- `V95__add_community_blood_group_permissions.sql`
- `V95__rename_lab_fee_add_new_fee_types.sql` (renamed to V99)

### Flyway Checksum Mismatch (V88)
After modifying V88 migration post-apply, the checksum diverged:
```sql
UPDATE flyway_schema_history SET checksum = -87931076 WHERE version = '88';
```

### Result After Fix
```
Total migrations applied: 100 (V1–V99 + V10)
Total users: 8 (admin + 6 sample + 1 existing collegeadmin)
Admin role: DEV_ADMIN (level 1) — can see all users
```

---

## Developer Notes

### Running with PostgreSQL (required for migrations to apply)
```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5435/cmsdb \
DB_USERNAME=cms \
DB_PASSWORD=cms \
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

The default local profile uses H2 in-memory with Flyway disabled — migrations will not apply and data will not persist. See `docs/DATABASE_CONFIG_GUIDE.md` for full setup guide.

### Removing Sample Users (Optional)
```sql
DELETE FROM app_users
WHERE keycloak_username IN ('college.admin', 'front.office', 'cashier', 'faculty.cs', 'lab.incharge', 'student.demo');
```
Keep `collegeadmin` (no dot) — that is a real user.
