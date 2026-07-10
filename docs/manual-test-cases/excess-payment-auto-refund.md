## TC-EXCESS-001: Collect excess payment via Bank Transfer with permission

**Preconditions:**
- User is logged in with `FEE_COLLECT` and `FEE_COLLECT_EXCESS` permissions
- Student has a finalized fee allocation with total outstanding > 0 (e.g. ₹50,000)

**Steps:**
1. Open Student Fee Detail for the student and enable **Advance Payment** mode
2. Set Payment Mode to `Bank Transfer`
3. Confirm the "Allow payment above total outstanding (bank excess)" checkbox is visible and check it
4. Enter an amount greater than total outstanding (e.g. ₹60,000 against ₹50,000 due)
5. Enter transaction reference and submit

**Expected Result:**
- Amount field does **not** show the "exceeds total outstanding" validation error
- Confirmation modal shows an "Excess (auto-refund)" row with ₹10,000
- Payment is accepted; receipt is created with the full ₹60,000 as `amountPaid`
- A new refund request appears in Fee Refund List for ₹10,000, status `PENDING`, tagged with the "Auto" source chip, linked to the new receipt
- Student's outstanding balance drops to ₹0 (only ₹50,000 was applied to installments)

**Status:** NOT TESTED

## TC-EXCESS-002: Collect excess payment via Demand Draft with permission

**Preconditions:**
- Same as TC-EXCESS-001

**Steps:**
1. Repeat TC-EXCESS-001 with Payment Mode = `Demand Draft` instead of `Bank Transfer`

**Expected Result:**
- Same outcome as TC-EXCESS-001 — DD is an equally eligible mode

**Status:** NOT TESTED

## TC-EXCESS-003: Excess checkbox hidden without FEE_COLLECT_EXCESS permission

**Preconditions:**
- User is logged in with `FEE_COLLECT` but **without** `FEE_COLLECT_EXCESS`
- Student has outstanding dues

**Steps:**
1. Open Student Fee Detail, enable Advance Payment mode, set Payment Mode to `Bank Transfer`

**Expected Result:**
- The "Allow payment above total outstanding" checkbox does **not** appear
- Entering an amount above total outstanding shows the existing "Amount cannot exceed total outstanding of ₹X" error and blocks submission

**Status:** NOT TESTED

## TC-EXCESS-004: Server-side block when excess is forced without permission

**Preconditions:**
- User holds `FEE_COLLECT` but not `FEE_COLLECT_EXCESS`
- Direct API access (e.g. Postman) available for the collect-advance endpoint

**Steps:**
1. Call `POST /student-fees/{studentId}/collect-advance` directly with `amount` above total outstanding, `paymentMode: BANK_TRANSFER`, and `allowExcess: true`

**Expected Result:**
- Request is rejected (403 Access Denied) with a message referencing the `FEE_COLLECT_EXCESS` permission
- No receipt or refund is created

**Status:** NOT TESTED

## TC-EXCESS-005: Excess checkbox unavailable for non-bank payment modes

**Preconditions:**
- User holds `FEE_COLLECT_EXCESS`
- Student has outstanding dues

**Steps:**
1. Open Student Fee Detail, enable Advance Payment mode
2. Set Payment Mode to `Cash`, then `UPI`, then `Cheque`, then `Card` in turn

**Expected Result:**
- The excess checkbox is not shown for any of these modes
- Amount above total outstanding is blocked by the standard validator in every mode

**Status:** NOT TESTED

## TC-EXCESS-006: Server-side block when excess is forced on a non-bank mode

**Preconditions:**
- User holds `FEE_COLLECT_EXCESS`
- Direct API access available

**Steps:**
1. Call the collect-advance endpoint with `amount` above total outstanding, `paymentMode: CASH`, `allowExcess: true`

**Expected Result:**
- Request is rejected (400) with a message that excess payment is only allowed for Demand Draft or Bank Transfer
- No receipt or refund is created

**Status:** NOT TESTED

## TC-EXCESS-007: No cap on excess amount

**Preconditions:**
- User holds `FEE_COLLECT_EXCESS`
- Student outstanding is a known small amount (e.g. ₹5,000)

**Steps:**
1. Collect an advance payment via Bank Transfer with excess allowed, entering an amount far above outstanding (e.g. ₹5,00,000)

**Expected Result:**
- Payment is accepted with no upper-bound error
- Auto-refund is created for the full excess (₹4,95,000)

**Status:** NOT TESTED

## TC-EXCESS-008: Unchecking the excess box re-applies the outstanding cap

**Preconditions:**
- User holds `FEE_COLLECT_EXCESS`; Advance Payment mode + Bank Transfer selected; excess checkbox is checked with an amount above outstanding entered

**Steps:**
1. Uncheck "Allow payment above total outstanding"
2. Observe the amount field validation without changing the amount

**Expected Result:**
- The "Amount cannot exceed total outstanding" error reappears immediately
- Submit remains blocked until the amount is reduced or the box is re-checked

**Status:** NOT TESTED

## TC-EXCESS-009: Switching payment mode away from a bank rail clears the checkbox

**Preconditions:**
- User holds `FEE_COLLECT_EXCESS`; excess checkbox is checked with Payment Mode = `Bank Transfer`

**Steps:**
1. Change Payment Mode to `Cash`

**Expected Result:**
- The excess checkbox is unchecked automatically and disappears
- The standard outstanding cap validator re-applies to the entered amount

**Status:** NOT TESTED

## TC-EXCESS-010: Exact-match payment does not generate an excess refund

**Preconditions:**
- User holds `FEE_COLLECT_EXCESS`; student total outstanding is a known amount (e.g. ₹50,000)

**Steps:**
1. Enable Advance Payment, Bank Transfer, check excess allowed
2. Enter an amount exactly equal to total outstanding (₹50,000) and submit

**Expected Result:**
- Payment succeeds normally
- No `AUTO_EXCESS` refund is created (outstanding becomes ₹0 with nothing left over)

**Status:** NOT TESTED

## TC-EXCESS-011: Auto-excess refund cannot be rejected via UI

**Preconditions:**
- An `AUTO_EXCESS` refund exists in `PENDING` status (per TC-EXCESS-001)
- User holds `FEE_REFUND_APPROVE`

**Steps:**
1. Open Fee Refund List, locate the refund tagged "Auto"
2. Open its detail panel

**Expected Result:**
- The "Auto" source chip is visible next to the status in both the list row and the detail panel
- The **Reject** button is not shown for this refund (Approve / Cash Refund actions remain available if applicable)

**Status:** NOT TESTED

## TC-EXCESS-012: Auto-excess refund cannot be rejected via direct API call

**Preconditions:**
- Same `AUTO_EXCESS` refund as TC-EXCESS-011
- Direct API access available

**Steps:**
1. Call `POST /student-fees/refunds/{refundId}/reject` directly with a valid rejection reason

**Expected Result:**
- Request is rejected (400) with a message that auto-generated excess refunds cannot be rejected
- Refund status remains `PENDING`

**Status:** NOT TESTED

## TC-EXCESS-013: Approving an auto-excess refund does not affect the student's paid installments

**Preconditions:**
- Same `AUTO_EXCESS` refund as TC-EXCESS-011, for a receipt where ₹50,000 was legitimately applied to installments and ₹10,000 is the excess
- User holds `FEE_REFUND_APPROVE`

**Steps:**
1. Note the student's current Paid / Outstanding totals on Student Fee Detail before approval
2. Approve the auto-excess refund (record payment mode, date, reference)
3. Return to Student Fee Detail and re-check Paid / Outstanding totals

**Expected Result:**
- Refund status becomes `APPROVED` with a generated refund number
- The student's Paid total and per-installment status are **unchanged** — the ₹50,000 legitimately applied to fees remains marked paid, not reverted to outstanding
- The refund appears in Payment History as a negative (Refund) line for ₹10,000 against the original receipt

**Status:** NOT TESTED

## TC-EXCESS-014: Manual full-receipt refund is blocked while an excess refund is still active

**Preconditions:**
- A receipt has an active (`PENDING` or `APPROVED`) `AUTO_EXCESS` refund (per TC-EXCESS-001)
- User holds `FEE_REFUND`

**Steps:**
1. From Receipts List or Student Fee Detail, attempt to initiate a manual refund against the same receipt number

**Expected Result:**
- Request is blocked with the existing "active refund already exists for this receipt" conflict — the same duplicate-refund protection covered in `fee-refund-duplicate-protection.md`
- No second refund request is created

**Status:** NOT TESTED

## TC-EXCESS-015: Excess is not available on the term-gated bulk Collect Payment screen

**Preconditions:**
- User holds `FEE_COLLECT_EXCESS`
- Student appears in the bulk Collect Payment list with dues in the currently open term only

**Steps:**
1. Open the bulk Collect Payment screen (not Student Fee Detail's Advance Payment) and attempt to enter an amount above the currently-due figure

**Expected Result:**
- The excess checkbox does not appear on this screen
- The standard "exceeds the amount currently due" cap still applies — excess collection is only available via Student Fee Detail's Advance Payment flow

**Status:** NOT TESTED

## TC-EXCESS-016: Regression — normal (non-excess) advance payment behavior is unchanged

**Preconditions:**
- Any user with `FEE_COLLECT`, with or without `FEE_COLLECT_EXCESS`
- Student has outstanding dues

**Steps:**
1. Collect an advance payment via Cash or UPI for an amount at or below total outstanding, without touching the excess checkbox

**Expected Result:**
- Behavior is identical to pre-existing advance payment flow — receipt amount equals amount paid, no refund is created, outstanding reduces accordingly

**Status:** NOT TESTED
