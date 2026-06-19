# Master Lifecycle Status Management - Manual Test Cases

## TC-MASTER-LIFE-001: Deactivate blood group from list screen

**Preconditions:**
- User is logged in with `ROLE_ADMIN` (or a role with `BLOOD_GROUP_MANAGE`)
- At least one active blood group exists without blocking dependencies

**Steps:**
1. Open `Preferences -> Blood Group` list.
2. Click Deactivate for an active blood group.
3. Confirm the action in the dialog.
4. Refresh the list.

**Expected Result:**
- Request is sent to `PATCH /blood-groups/{id}/status` with `{"isActive": false}`.
- Row status changes to Inactive.
- Success toast is shown.

**Actual Result:**
-

**Status:** NOT TESTED

## TC-MASTER-LIFE-007: India location country/state/district status toggle

**Preconditions:**
- User is logged in with `ROLE_ADMIN` (or a role with `INDIA_LOCATION_MANAGE`)
- At least one country, state, and district exist in active state

**Steps:**
1. Open `Preferences -> Location Master` list.
2. Deactivate one country, one state, and one district using status action buttons.
3. Activate the same records again.

**Expected Result:**
- Actions call `PATCH /india/countries/{id}/status`, `PATCH /india/states/{id}/status`, and `PATCH /india/districts/{id}/status`.
- Status chips update between Active and Inactive after refresh.
- Success toast appears for each toggle.

**Actual Result:**
-

**Status:** NOT TESTED

## TC-MASTER-LIFE-002: Reactivate community from list screen

**Preconditions:**
- User is logged in with `ROLE_ADMIN` (or a role with `COMMUNITY_MANAGE`)
- At least one inactive community exists

**Steps:**
1. Open `Preferences -> Community` list.
2. Click Activate for an inactive community.
3. Confirm the action in the dialog.

**Expected Result:**
- Request is sent to `PATCH /communities/{id}/status` with `{"isActive": true}`.
- Row status changes to Active.
- Success toast is shown.

**Actual Result:**
-

**Status:** NOT TESTED

## TC-MASTER-LIFE-003: Block referral type deactivation when enquiries exist

**Preconditions:**
- User is logged in with `ROLE_ADMIN` (or a role with `REFERRAL_TYPE_MANAGE`)
- Referral type is referenced by at least one enquiry

**Steps:**
1. Open `Preferences -> Referral Type` list.
2. Click Deactivate for the referenced referral type.
3. Confirm the action.

**Expected Result:**
- API returns `409 CONFLICT` with lifecycle code `ACTIVE_REFERENCE_EXISTS`.
- Referral type remains Active.
- Error toast displays conflict message.

**Actual Result:**
-

**Status:** NOT TESTED

## TC-MASTER-LIFE-004: Block agent deactivation when dependencies exist

**Preconditions:**
- User is logged in with `ROLE_ADMIN` (or a role with `AGENT_MANAGE`)
- Agent is referenced by enquiry or commission guideline or payout

**Steps:**
1. Open `Preferences -> Agent` list.
2. Click Deactivate for the referenced agent.
3. Confirm the action.

**Expected Result:**
- API returns `409 CONFLICT` with lifecycle code `ACTIVE_REFERENCE_EXISTS`.
- Agent remains Active.
- Error toast displays conflict message.

**Actual Result:**
-

**Status:** NOT TESTED

## TC-MASTER-LIFE-005: Block staff referrer deactivation when payouts exist

**Preconditions:**
- User is logged in with `ROLE_ADMIN` (or a role with `STAFF_REFERRER_MANAGE`)
- Staff referrer has commission payout records

**Steps:**
1. Open `Preferences -> Staff Referrer` list.
2. Click Deactivate for the referenced staff referrer.
3. Confirm the action.

**Expected Result:**
- API returns `409 CONFLICT` with lifecycle code `ACTIVE_REFERENCE_EXISTS`.
- Staff referrer remains Active.
- Error toast displays conflict message.

**Actual Result:**
-

**Status:** NOT TESTED

## TC-MASTER-LIFE-006: Scholarship type activate/deactivate from list screen

**Preconditions:**
- User is logged in with `ROLE_ADMIN` (or a role with `SCHOLARSHIP_MANAGE`)
- One active scholarship type and one inactive scholarship type exist

**Steps:**
1. Open `Preferences -> Scholarship Types` list.
2. Deactivate an active scholarship type and confirm.
3. Activate an inactive scholarship type and confirm.

**Expected Result:**
- Both actions call `PATCH /scholarships/{id}/status` with `{"isActive": false}` / `{"isActive": true}`.
- Status chips update correctly after reload.
- Success toast appears for both actions.

**Actual Result:**
-

**Status:** NOT TESTED

