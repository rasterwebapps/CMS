## TC-REFUND-001: Block duplicate refund when an active request already exists

**Preconditions:**
- User is logged in with `FEE_REFUND` permission
- At least one student payment receipt exists

**Steps:**
1. Open Receipts list and choose a student payment receipt
2. Submit refund request with valid reason
3. Without approving/rejecting the first request, attempt refund again for the same receipt

**Expected Result:**
- First request is created with `PENDING` status
- Same receipt shows `Refund Pending` marker
- Refund action is disabled
- If duplicate API call is forced, server returns conflict (`409`) with duplicate refund message

**Status:** NOT TESTED

## TC-REFUND-002: Block refund when receipt is already refunded

**Preconditions:**
- User is logged in with `FEE_REFUND` permission
- Existing refund request for a receipt has been approved

**Steps:**
1. Approve a pending refund from refund approval screen
2. Return to Receipts list and search for the original receipt
3. Verify refund action state for the original receipt

**Expected Result:**
- Original receipt is marked as `Refunded`
- Refund action is disabled and cannot be submitted again
- If duplicate API call is forced, server returns conflict (`409`)

**Status:** NOT TESTED

## TC-REFUND-003: Allow re-request only after rejection

**Preconditions:**
- User is logged in with `FEE_REFUND` permission
- A refund request exists for a receipt and is rejected

**Steps:**
1. Reject a pending refund request
2. Open Receipts list and locate the same original receipt
3. Initiate refund again

**Expected Result:**
- Refund action is available again after rejection
- New request is accepted and created as `PENDING`

**Status:** NOT TESTED

