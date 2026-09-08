# Inventory Budgets & Approvals — Exception Handling Manual Test Cases

Covers the third and final Phase 6 ("Budgets & Approvals") slice of the Inventory Management
module (`docs/inventory-management/`): bypassing an approval step with a structured, documented
reason.

## TC-INV-APPROVAL-008: Bypass requires its own permission, distinct from the step's own

**Preconditions:**
- An in-progress approval instance at a stage.
- A user who holds `INVENTORY_APPROVAL_ACT` and `INVENTORY_APPROVAL_BYPASS`, but **not** the
  specific permission that stage's step references.

**Steps:**
1. View the instance detail screen as that user.
2. Bypass the current stage's step, selecting a Reason and an optional note.

**Expected Result:**
- The user sees a "Bypass (Exception)" control on the current stage even though they don't hold
  that step's own required permission (a step-permission holder alone, without
  `INVENTORY_APPROVAL_BYPASS`, sees no Bypass control).
- After bypassing: the step shows status "Approved" with the chosen exception reason displayed
  as a chip, and the resolution note if one was entered; the instance advances exactly as an
  ordinary approval would (to the next stage, or to "Approved" if that was the last stage).

**Status:** NOT TESTED

## TC-INV-APPROVAL-009: Bypass is blocked once already resolved or off-stage

**Preconditions:**
- A step that has already been approved or rejected; separately, a step at a future stage not
  yet reached.

**Steps:**
1. Attempt to bypass the already-resolved step via a direct API call.
2. Attempt to bypass the future-stage step.

**Expected Result:**
- Both rejected with a clear message — bypass follows the same stage/status gating as an
  ordinary approve/reject, it isn't a way to skip ahead or redo a resolved step.

**Status:** NOT TESTED

## TC-INV-APPROVAL-010: Bypass without the permission is rejected server-side

**Preconditions:**
- A user holding `INVENTORY_APPROVAL_ACT` (and the current stage's own required permission) but
  **not** `INVENTORY_APPROVAL_BYPASS`.

**Steps:**
1. Confirm no Bypass control appears for this user on the detail screen.
2. Attempt to call the bypass endpoint directly for that step.

**Expected Result:**
- Step 1: no Bypass button shown (they can still see the normal Approve/Reject controls, since
  they do hold that step's own permission).
- Step 2: rejected with a clear message naming the missing bypass permission.

**Status:** NOT TESTED

## TC-INV-APPROVAL-011: Reason is required; invalid reason codes are rejected

**Preconditions:**
- A user holding `INVENTORY_APPROVAL_BYPASS`, at an actionable stage.

**Steps:**
1. Attempt to bypass with no reason selected (via a direct API call with a blank reason).
2. Attempt to bypass with a reason value that isn't one of the defined codes.

**Expected Result:**
- Both rejected with a clear validation message — a bypass always carries a real, structured
  reason on the audit trail, never a blank one.

**Status:** NOT TESTED
