## TC-RBAC-001: `collegeadmin` role exists with admission-only permission scope

**Preconditions:**
- Backend is running against a database with migration `V123__create_collegeadmin_role_and_tighten_permissions.sql` applied
- A DB client is available

**Steps:**
1. Query `app_roles` for role name `collegeadmin`.
2. Query `role_permissions` joined with `permissions` for role `collegeadmin`.
3. Verify only admission flow + required master view + fee completion permissions are present.
4. Verify permissions like `CURRICULUM_MANAGE`, `EXAMINATION_MANAGE`, `INVENTORY_MANAGE`, `ROLE_EDIT`, `USER_DEACTIVATE` are absent.

**Expected Result:**
- Role `collegeadmin` exists.
- Only the scoped permissions are attached.
- Unwanted permissions are removed.

**Status:** NOT TESTED

## TC-RBAC-002: Existing `COLLEGE_ADMIN` users are reassigned to `collegeadmin`

**Preconditions:**
- At least one user existed with role `COLLEGE_ADMIN` before migration

**Steps:**
1. Query `app_users` with join on `app_roles` after migration.
2. Check users previously mapped to `COLLEGE_ADMIN`.
3. Verify `app_role_id` now points to role name `collegeadmin`.

**Expected Result:**
- Legacy `COLLEGE_ADMIN` users are mapped to `collegeadmin`.

**Status:** NOT TESTED

## TC-RBAC-003: Role editor cannot modify immutable default roles

**Preconditions:**
- Logged in user has permission to edit role permissions generally
- Roles `DEV_ADMIN` and `SUPPORT_ADMIN` exist

**Steps:**
1. Call `PUT /api/v1/role-management/{id}/permissions` for `DEV_ADMIN`.
2. Call `PUT /api/v1/role-management/{id}/dashboard-widgets` for `SUPPORT_ADMIN`.

**Expected Result:**
- API returns `403 Forbidden` for both operations.
- Error indicates role is immutable.

**Status:** NOT TESTED

## TC-RBAC-004: Fresh/demo DB no longer keeps seeded business roles or demo users by default

**Preconditions:**
- A database was initialized from scratch with migrations through `V125__rbac_identity_only_final_pass.sql`
- A DB client is available

**Steps:**
1. Query `app_users` for usernames `admin`, `frontoffice`, `cashier`, `faculty1`, `student1`, `labincharge1`, `parent1`.
2. Query `app_roles` for names `ADMIN`, `FRONT_OFFICE`, `CASHIER`, `FACULTY`, `LAB_INCHARGE`, `TECHNICIAN`, `STUDENT`, `PARENT`.
3. Query `app_users` joined with `app_roles` for usernames `devadmin`, `supportadmin`, and `collegeadmin`.
4. Verify `DEV_ADMIN`, `SUPPORT_ADMIN`, and `collegeadmin` role records separately.

**Expected Result:**
- Demo users listed above are absent.
- Seeded business roles are removed when no users reference them.
- Immutable platform roles remain intact.
- `devadmin` maps to `DEV_ADMIN`; `supportadmin` maps to `SUPPORT_ADMIN`.
- `collegeadmin` remains available for assignment.

**Status:** NOT TESTED

## TC-RBAC-005: Keycloak realm export is identity-only

**Preconditions:**
- Realm export `cms-realm.json` is available from root, infrastructure, or deployment config

**Steps:**
1. Open the realm export.
2. Verify there is no `roles.realm` section containing application roles such as `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
3. Verify there is no `defaultRoles` entry assigning `ROLE_STUDENT` or any other application business role.
4. Verify users do not contain `realmRoles` assignments.
5. Verify users `devadmin`, `supportadmin`, and `collegeadmin` exist as identity-only users.

**Expected Result:**
- Keycloak provides authentication identities only.
- Application authorization comes from DB `app_users`, `app_roles`, and `role_permissions`.

**Status:** NOT TESTED

