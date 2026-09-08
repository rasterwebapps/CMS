# Inventory Budgets & Approvals — Multi-Level Approval Routing Manual Test Cases

Covers the second and largest Phase 6 ("Budgets & Approvals") slice of the Inventory Management
module (`docs/inventory-management/`): "Budgets & Approvals" → Approval Workflows and → Approvals.

**Scope note for the tester:** this is a standalone, optional approval-chain engine. It does
**not** replace or block Purchase Requisition's/Purchase Order's own existing single-permission
approve/reject/send actions — a document can be fully approved and sent through its normal flow
whether or not anyone ever starts an approval instance against it.

## TC-INV-APPROVAL-001: Define a sequential workflow and confirm the step-order rule

**Preconditions:**
- Logged in as a user holding `INVENTORY_APPROVAL_WORKFLOW_MANAGE`.
- At least two real permission codes exist to reference (e.g. `INVENTORY_PURCHASE_ORDER_MANAGE`,
  `INVENTORY_BUDGET_MANAGE`).

**Steps:**
1. Go to Budgets & Approvals → Approval Workflows → New Workflow.
2. Name it, choose Document Type "Purchase Order", leave Location blank (all locations).
3. Add two steps: Order 1 / "Manager Sign-off" / a real permission code; Order 2 / "Finance
   Sign-off" / a different real permission code. Save.
4. Attempt to add a step referencing a permission code that doesn't exist and save.

**Expected Result:**
- Step 3: workflow saves with 2 steps; the list shows "2 steps".
- Step 4: rejected with a clear "does not exist" message naming the bad code.

**Status:** NOT TESTED

## TC-INV-APPROVAL-002: Start an approval and confirm sequential gating

**Preconditions:**
- The workflow from TC-INV-APPROVAL-001 is active.
- A Purchase Order in status "Pending" exists at a location, whose total meets any minimum
  amount on the workflow (leave the workflow's minimum blank to apply unconditionally).
- Two different users hold the two steps' respective permissions (or one user holding both, to
  test one step at a time by only using the notes/approve button that's actually offered).

**Steps:**
1. Go to Approvals → Start Approval, select Document Type "Purchase Order", the eligible order,
   and the workflow, Start.
2. As a user holding step 1's permission, approve step 1.
3. As a user holding step 2's permission, view the same instance before step 1 was approved
   (or immediately after starting, before any approval).

**Expected Result:**
- Step 1: the instance is created "In Progress" at stage 1; step 1 shows an Approve/Reject
  control for a step-1-permission holder, step 2 shows "Not yet at this stage" for everyone.
- Step 2: after approving, stage advances to 2; step 2's control now appears for a step-2-
  permission holder; approving it completes the instance as "Approved".
- Step 3 (checked before step 1 resolves): step 2 offers no Approve/Reject control to anyone,
  regardless of permission — it isn't the current stage yet.

**Status:** NOT TESTED

## TC-INV-APPROVAL-003: Parallel steps all require sign-off before advancing

**Preconditions:**
- A workflow with two steps sharing the same Order number (e.g. both Order 1, different
  permissions) exists and is active for Purchase Requisition.
- A Submitted Purchase Requisition exists.

**Steps:**
1. Start an approval using that workflow against the requisition.
2. Approve only one of the two parallel steps.
3. Approve the second parallel step.

**Expected Result:**
- Step 2: the instance stays "In Progress" at the same stage — one parallel step alone isn't
  enough to advance.
- Step 3: once both parallel steps are approved, the instance completes (advances to the next
  stage, or becomes "Approved" if that was the last stage).

**Status:** NOT TESTED

## TC-INV-APPROVAL-004: A single rejection fails the whole instance immediately

**Preconditions:**
- An in-progress instance with at least two stages exists, currently at stage 1.

**Steps:**
1. Reject stage 1 (with an optional note).
2. Check stage 2's row.

**Expected Result:**
- The instance immediately shows "Rejected"; stage 2 stays "Pending" and is never actionable —
  a rejection short-circuits the whole chain, it doesn't just fail one stage.

**Status:** NOT TESTED

## TC-INV-APPROVAL-005: Permission gating is enforced server-side, not just hidden in the UI

**Preconditions:**
- An in-progress instance at a stage requiring a specific permission.
- A user holding `INVENTORY_APPROVAL_ACT` but **not** that specific step's required permission.

**Steps:**
1. As that user, attempt to approve the step via a direct API call to the approve endpoint.

**Expected Result:**
- Rejected with a clear message naming the missing permission — the UI already hides the
  Approve/Reject controls for a non-holder, but the same rule is enforced in the service layer,
  not just by hiding the button.

**Status:** NOT TESTED

## TC-INV-APPROVAL-006: Eligibility respects location and amount scope

**Preconditions:**
- A workflow scoped to a specific Location and a minimum amount for Purchase Order exists.
- A Purchase Order below that location or below that amount exists.

**Steps:**
1. Open Start Approval, select that order.

**Expected Result:**
- The scoped workflow does not appear in the eligible list for an order at a different location,
  or below the minimum amount; it does appear for a matching order. A workflow with no Location
  and no minimum set (or a minimum of 0) appears for any order of that document type.

**Status:** NOT TESTED

## TC-INV-APPROVAL-007: Starting twice against the same document is blocked while in progress

**Preconditions:**
- A document already has an "In Progress" approval instance.

**Steps:**
1. Attempt to start a second approval against the same document.

**Expected Result:**
- Rejected with a clear "already has an approval in progress" message.

**Status:** NOT TESTED
