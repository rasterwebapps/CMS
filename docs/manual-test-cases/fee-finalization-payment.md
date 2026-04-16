# Fee Finalization & Payment Collection — Manual Test Cases

## TC-FIN-001: Fee finalization screen lists INTERESTED enquiries

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one enquiry exists with status INTERESTED

**Steps:**
1. Navigate to `/student-fees/finalize`
2. Verify the page title shows "Fee Finalization"
3. Verify the table lists only enquiries with status INTERESTED
4. Verify columns show: Name, Program, Course, Referral Type, Proposed Fee, Actions

**Expected Result:**
- Only INTERESTED enquiries are displayed in the table
- Each row has a "Finalize" button

**Status:** NOT TESTED

---

## TC-FIN-002: Select an enquiry for fee finalization

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one INTERESTED enquiry exists with program, course, and fee data

**Steps:**
1. Navigate to `/student-fees/finalize`
2. Click "Finalize" on an enquiry row
3. Verify the finalization form opens
4. Verify the enquiry summary shows: Name, Program, Course, Referral Type, Guideline Fee
5. Verify the Total Fee field is pre-filled with the enquiry's `finalCalculatedFee`
6. Verify year-wise breakdown is displayed if available

**Expected Result:**
- Form opens with all enquiry data pre-populated
- Year-wise fee boxes appear if `yearWiseFees` was saved with the enquiry

**Status:** NOT TESTED

---

## TC-FIN-003: Finalize fee with discount

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An INTERESTED enquiry is selected for finalization

**Steps:**
1. Select an INTERESTED enquiry
2. Modify Total Fee if needed
3. Enter Discount Amount (e.g., 5000)
4. Enter Discount Reason (e.g., "Merit scholarship")
5. Verify Net Fee = Total Fee - Discount
6. Click "Finalize Fee"
7. Verify success message appears

**Expected Result:**
- Fee is finalized with the discount applied
- Enquiry status changes to FEES_FINALIZED
- Enquiry disappears from the INTERESTED list

**Status:** NOT TESTED

---

## TC-FIN-004: Finalize fee API call

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status INTERESTED

**Steps:**
1. Send a POST request to `/api/v1/enquiries/{id}/finalize-fees` with body:
   ```json
   {
     "totalFee": 400000,
     "discountAmount": 10000,
     "discountReason": "Early bird discount"
   }
   ```
2. Verify response status is 200 OK
3. Verify response includes `finalizedTotalFee`, `finalizedNetFee`, `finalizedBy`, `finalizedAt`
4. Verify `status` is "FEES_FINALIZED"
5. GET the enquiry and verify finalization data is persisted

**Expected Result:**
- Fee finalization is recorded with audit trail
- Net fee = totalFee - discountAmount

**Status:** NOT TESTED

---

## TC-FIN-005: Commission amount shown in finalization summary

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An INTERESTED enquiry exists with a referral type that has `hasCommission=true`

**Steps:**
1. Navigate to `/student-fees/finalize`
2. Click "Finalize" on the enquiry with commission
3. Verify the summary section shows "Commission Amount" with the value

**Expected Result:**
- Commission information is visible in the enquiry summary panel

**Status:** NOT TESTED

---

## TC-PAY-001: Payment collection screen lists FEES_FINALIZED enquiries

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one enquiry exists with status FEES_FINALIZED

**Steps:**
1. Navigate to `/student-fees/collect-payment`
2. Verify the page title shows "Payment Collection"
3. Verify the table lists only enquiries with status FEES_FINALIZED
4. Verify columns show: Name, Program, Course, Net Fee, Finalized At, Actions

**Expected Result:**
- Only FEES_FINALIZED enquiries are displayed
- Each row has a "Collect" button

**Status:** NOT TESTED

---

## TC-PAY-002: Select an enquiry for payment collection

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one FEES_FINALIZED enquiry exists

**Steps:**
1. Navigate to `/student-fees/collect-payment`
2. Click "Collect" on an enquiry row
3. Verify the payment form opens in a side-by-side layout
4. Verify left panel shows: Fee Summary (name, program, course, total fee, discount, net fee)
5. Verify right panel shows: Payment form (amount, date, mode, reference, remarks)
6. Verify the amount is pre-filled with the net fee

**Expected Result:**
- Two-panel layout with fee summary on left and payment form on right
- Amount pre-filled with finalized net fee

**Status:** NOT TESTED

---

## TC-PAY-003: Collect full payment

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A FEES_FINALIZED enquiry is selected for payment

**Steps:**
1. Select a FEES_FINALIZED enquiry
2. Verify amount is pre-filled with net fee
3. Set payment date to today
4. Select payment mode (e.g., "CASH")
5. Optionally enter transaction reference and remarks
6. Click "Collect Payment"
7. Verify success message: "Full payment collected"

**Expected Result:**
- Payment is recorded
- Enquiry status changes to FEES_PAID
- Enquiry disappears from the FEES_FINALIZED list

**Status:** NOT TESTED

---

## TC-PAY-004: Collect partial payment

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A FEES_FINALIZED enquiry is selected for payment with net fee of ₹100,000

**Steps:**
1. Select a FEES_FINALIZED enquiry
2. Change amount to less than the net fee (e.g., ₹50,000)
3. Set payment date and payment mode
4. Click "Collect Payment"
5. Verify success message: "Partial payment collected"

**Expected Result:**
- Payment is recorded with the partial amount
- Enquiry status changes to PARTIALLY_PAID

**Status:** NOT TESTED

---

## TC-PAY-005: Payment mode validation

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Select a FEES_FINALIZED enquiry
2. Try to submit payment form without selecting a payment mode
3. Verify validation error appears
4. Try to submit without entering an amount
5. Verify validation error appears
6. Try to submit without a payment date
7. Verify validation error appears

**Expected Result:**
- Form validates required fields: amount, payment date, payment mode
- Submit is blocked until all required fields are filled

**Status:** NOT TESTED

---

## TC-PAY-006: Year-wise breakdown shown in payment collection

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A FEES_FINALIZED enquiry exists with year-wise fee data saved

**Steps:**
1. Navigate to `/student-fees/collect-payment`
2. Click "Collect" on an enquiry that has year-wise breakdown
3. Verify the fee summary panel shows "Year-wise Breakdown" section
4. Verify year boxes display Year 1, Year 2, etc. with amounts

**Expected Result:**
- Year-wise fee breakdown is displayed in the fee summary panel

**Status:** NOT TESTED

---

## TC-FIN-006: Empty state — no INTERESTED enquiries

**Preconditions:**
- User is logged in with ROLE_ADMIN
- No enquiries exist with status INTERESTED

**Steps:**
1. Navigate to `/student-fees/finalize`
2. Verify the empty state message is shown

**Expected Result:**
- Message "No enquiries in INTERESTED status" is displayed with an icon

**Status:** NOT TESTED

---

## TC-PAY-007: Empty state — no FEES_FINALIZED enquiries

**Preconditions:**
- User is logged in with ROLE_ADMIN
- No enquiries exist with status FEES_FINALIZED

**Steps:**
1. Navigate to `/student-fees/collect-payment`
2. Verify the empty state message is shown

**Expected Result:**
- Message "No enquiries in FEES_FINALIZED status" is displayed with an icon

**Status:** NOT TESTED
