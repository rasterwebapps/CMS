# Fee Increase Prevention — Manual Test Cases

This document contains manual test cases to verify that fee increases are prevented during finalization and only discounts are allowed.

---

## TC-FEE-INCR-001: Prevent fee increase during finalization (Backend)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- An INTERESTED enquiry exists with `finalCalculatedFee` set to ₹100,000

**Steps:**
1. Send a POST request to `/api/v1/enquiries/{enquiryId}/finalize-fees` with:
   ```json
   {
     "totalFee": 120000,
     "discountAmount": 0,
     "discountReason": null
   }
   ```
2. Verify the response status is 400 Bad Request
3. Verify the error message contains:
   - "Fee increase is not allowed"
   - "Only discounts can be applied during finalization"

**Expected Result:**
- Fee increase is rejected
- Clear error message is returned
- Fee finalization does not occur

**Status:** NOT TESTED

---

## TC-FEE-INCR-002: Allow fee finalization at original amount (Backend)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- An INTERESTED enquiry exists with `finalCalculatedFee` set to ₹100,000

**Steps:**
1. Send a POST request to `/api/v1/enquiries/{enquiryId}/finalize-fees` with:
   ```json
   {
     "totalFee": 100000,
     "discountAmount": 0,
     "discountReason": null
   }
   ```
2. Verify the response status is 200 OK
3. Verify the response contains:
   - `finalizedTotalFee`: 100000
   - `finalizedNetFee`: 100000
   - `status`: "FEES_FINALIZED"

**Expected Result:**
- Fee is finalized successfully at the original amount
- No increase or discount is applied
- Enquiry status changes to FEES_FINALIZED

**Status:** NOT TESTED

---

## TC-FEE-INCR-003: Allow fee finalization with discount (Backend)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- An INTERESTED enquiry exists with `finalCalculatedFee` set to ₹100,000

**Steps:**
1. Send a POST request to `/api/v1/enquiries/{enquiryId}/finalize-fees` with:
   ```json
   {
     "totalFee": 100000,
     "discountAmount": 10000,
     "discountReason": "Merit scholarship"
   }
   ```
2. Verify the response status is 200 OK
3. Verify the response contains:
   - `finalizedTotalFee`: 100000
   - `finalizedDiscountAmount`: 10000
   - `finalizedNetFee`: 90000
   - `finalizedDiscountReason`: "Merit scholarship"
   - `status`: "FEES_FINALIZED"

**Expected Result:**
- Fee is finalized successfully with the discount
- Net fee = Total fee - Discount
- Discount reason is recorded

**Status:** NOT TESTED

---

## TC-FEE-INCR-004: Reject finalization when no calculated fee exists (Backend)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- An INTERESTED enquiry exists WITHOUT `finalCalculatedFee` set (null)

**Steps:**
1. Send a POST request to `/api/v1/enquiries/{enquiryId}/finalize-fees` with:
   ```json
   {
     "totalFee": 100000,
     "discountAmount": 0,
     "discountReason": null
   }
   ```
2. Verify the response status is 400 Bad Request
3. Verify the error message contains "no calculated fee found"

**Expected Result:**
- Fee finalization is rejected
- Error message indicates that a calculated fee must exist first
- User needs to calculate fees before finalizing

**Status:** NOT TESTED

---

## TC-FEE-INCR-005: Prevent year-wise fee increase (Frontend)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- Navigate to Fee Finalization screen (`/finance/fee-finalization`)
- Select an INTERESTED enquiry with:
  - Year 1 original fee: ₹25,000
  - Year 2 original fee: ₹25,000
  - Year 3 original fee: ₹25,000
  - Year 4 original fee: ₹25,000
  - Total: ₹100,000

**Steps:**
1. In the year-wise fee table, try to increase Year 1 final fee to ₹30,000
2. Observe the input behavior and validation messages
3. Verify the input value is automatically capped at ₹25,000 (original amount)
4. Check if an error message appears: "Fee increase not allowed. Year fee cannot exceed the original calculated amount. Only discounts can be applied."
5. Verify the "Finalize Fee" button status

**Expected Result:**
- Input automatically caps at the original amount (₹25,000)
- Error message is displayed
- "Finalize Fee" button is disabled when attempting to increase

**Status:** NOT TESTED

---

## TC-FEE-INCR-006: Allow year-wise fee discount (Frontend)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- Navigate to Fee Finalization screen (`/finance/fee-finalization`)
- Select an INTERESTED enquiry with Year 1 original fee: ₹25,000

**Steps:**
1. In the year-wise fee table, change Year 1 final fee to ₹20,000 (reduction)
2. Verify no error message appears
3. Verify the discount column shows "− ₹5,000"
4. Verify total discount updates accordingly
5. Verify the "Finalize Fee" button is enabled
6. Click "Finalize Fee" and verify success

**Expected Result:**
- Fee reduction is allowed
- Discount is calculated and displayed correctly
- Finalization succeeds

**Status:** NOT TESTED

---

## TC-FEE-INCR-007: Global discount cannot cause negative year fees (Frontend)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- Navigate to Fee Finalization screen with total original fee: ₹100,000

**Steps:**
1. In the "Apply Global Discount" section, enter ₹110,000 (exceeds total)
2. Observe validation messages
3. Verify error message: "Discount cannot exceed the total fee"
4. Verify "Finalize Fee" button is disabled

**Expected Result:**
- Error message is displayed
- Finalization is prevented
- Year-wise fees do not go negative

**Status:** NOT TESTED

---

## TC-FEE-INCR-008: Frontend respects backend validation (Integration)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- Navigate to Fee Finalization screen
- Browser dev tools > Network tab is open

**Steps:**
1. Manually bypass frontend validation by modifying the request in browser dev tools or using Postman
2. Send a fee finalization request with `totalFee` > original calculated fee
3. Observe the backend response
4. Verify the frontend displays the error message from the backend
5. Verify the enquiry remains in INTERESTED status

**Expected Result:**
- Backend validation catches the attempt to increase fee
- Frontend displays the error message: "Fee increase is not allowed. Only discounts can be applied."
- Enquiry is not finalized

**Status:** NOT TESTED

---

## TC-FEE-INCR-009: Visual feedback for reduced fees (Frontend)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- Navigate to Fee Finalization screen

**Steps:**
1. Select an enquiry and reduce Year 1 final fee below original
2. Observe the CSS class applied to the input field
3. Verify the input has class `amount-input--reduced` (visual highlight)
4. Verify the discount column shows the calculated discount with "−" prefix
5. Verify the discount is highlighted in red/accent color

**Expected Result:**
- Reduced fee inputs are visually highlighted
- Discount amounts are clearly displayed
- UI provides clear feedback about discounts

**Status:** NOT TESTED

---

## Summary of Business Rule

**Rule:** During fee finalization, administrators can **only apply discounts** (reduce fees). Fee increases are **not allowed** under any circumstances.

**Rationale:**
- Prevents accidental or unauthorized fee increases
- Maintains fee integrity and transparency
- Ensures original calculated fees are the upper bound
- All reductions must be documented with a discount reason

**Technical Implementation:**
- **Backend:** `EnquiryService.finalizeFees()` validates `request.totalFee()` ≤ `enquiry.getFinalCalculatedFee()`
- **Frontend:** `FeeFinalizationComponent` caps individual year amounts and displays validation errors
- **Both layers** enforce this rule to prevent bypassing

---

## Notes for Testers

1. This validation applies **only during fee finalization**, not during initial fee calculation
2. The `finalCalculatedFee` field must be set on the enquiry before finalization
3. Discounts can be applied:
   - Per year (individual adjustments)
   - Globally (distributed proportionally)
   - Via scholarship integration
4. All discounts must have a reason documented in `discountReason` field (though not enforced by validation)
5. The error messages are designed to be clear and actionable for end users

