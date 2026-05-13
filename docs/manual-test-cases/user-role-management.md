# Manual Test Cases — User & Role Management

---

## TC-ROLE-001: DEV_ADMIN can update SUPPORT_ADMIN role permissions

**Preconditions:**
- User `devadmin` is logged in with `DEV_ADMIN` role (hierarchy level 1)
- Application is running
- At least one permission (e.g. `USER_VIEW`) exists

**Steps:**
1. Navigate to **Role Management** screen
2. Verify `Support Admin` role appears in the role list
3. Click on the `Support Admin` role to open the permission editor
4. Toggle any permission checkbox (e.g. enable `USER_VIEW`)
5. Click **Save Permissions**

**Expected Result:**
- Permissions are saved successfully
- A success toast appears: "Role permissions updated"
- The role card refreshes with the updated permission count

**Status:** NOT TESTED

---

## TC-ROLE-002: DEV_ADMIN role itself is immutable — nobody can modify it

**Preconditions:**
- User `devadmin` is logged in with `DEV_ADMIN` role
- Application is running

**Steps:**
1. Navigate to **Role Management** screen
2. Verify `Developer Admin` role does **NOT** appear in the editable role list
   (it should be absent because `findAssignableRoles` returns only roles with level > requester's level, and no role is above level 1)

**Expected Result:**
- `Developer Admin` role is not shown in the Role Management list to any user
- Any direct API call `PUT /api/v1/role-management/{devAdminRoleId}/permissions` should return `403 Forbidden`

**Status:** NOT TESTED

---

## TC-ROLE-003: SUPPORT_ADMIN cannot modify their own role's permissions

**Preconditions:**
- User `supportadmin` is logged in with `SUPPORT_ADMIN` role (hierarchy level 2)
- Application is running

**Steps:**
1. Navigate to **Role Management** screen
2. Verify `Support Admin` role does **NOT** appear in the list (it is at the same hierarchy level as the requester)
3. If the role were to appear (e.g. via a direct API call): send `PUT /api/v1/role-management/{supportAdminRoleId}/permissions` with a valid request

**Expected Result:**
- The `Support Admin` role is not listed in the role manager for `supportadmin` users
- Any direct API call returns `403 Forbidden` with message: "You cannot modify a role at or above your own hierarchy level"

**Status:** NOT TESTED

---

## TC-ROLE-004: SUPPORT_ADMIN cannot escalate their own permissions

**Preconditions:**
- User `supportadmin` is logged in with `SUPPORT_ADMIN` role
- Application is running and `supportadmin` does NOT hold `USER_CREATE` permission

**Steps:**
1. Navigate to **Role Management** screen
2. Select any role below `SUPPORT_ADMIN` (e.g. `Admin`)
3. Attempt to grant the `USER_CREATE` permission to that role by ticking the checkbox and saving

**Expected Result:**
- Save fails with `403 Forbidden`: "You do not hold permission 'USER_CREATE' and cannot assign it"
- No permissions are changed

**Status:** NOT TESTED

---

## TC-USERMGMT-001: DEV_ADMIN can see and edit SUPPORT_ADMIN user accounts

**Preconditions:**
- User `devadmin` is logged in with `DEV_ADMIN` role
- At least one user with `SUPPORT_ADMIN` role exists in the system

**Steps:**
1. Navigate to **User Management** screen
2. Verify the `supportadmin` user appears in the user list
3. Click the Edit button on the `supportadmin` user
4. Change the Full Name field and click **Save Changes**

**Expected Result:**
- User details are updated successfully
- A success toast appears: "User updated"

**Status:** NOT TESTED

---

## TC-USERMGMT-002: SUPPORT_ADMIN cannot see or edit DEV_ADMIN user accounts

**Preconditions:**
- User `supportadmin` is logged in with `SUPPORT_ADMIN` role
- Application is running

**Steps:**
1. Navigate to **User Management** screen
2. Look for any user with `Developer Admin` role in the list

**Expected Result:**
- No `Developer Admin` users are shown (backend filters out users at or above the requester's hierarchy level)

**Status:** NOT TESTED

