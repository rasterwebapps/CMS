# Manual Test Cases: Transaction ID Validation for Fee Collection

## Test Case ID: TC-FEE-TX-001
**Title:** Validate Transaction ID is required for UPI payment mode

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- At least one enquiry or student has outstanding fees

**Steps:**
1. Navigate to Finance → Fee Collection
2. Click "Collect Payment" for any entry with outstanding fees
3. Fill in:
   - Amount: Any valid amount
   - Payment Date: Today's date
   - Payment Mode: Select "UPI"
   - Leave Transaction Reference empty
4. Click "Collect Payment"

**Expected Result:**
- Form validation should trigger
- Error message should appear: "Transaction reference is required for this payment mode"
- Payment should NOT be submitted
- Field label should show red asterisk (*)
- Hint text should appear: "Required for UPI, Bank Transfer, and Cheque payments"

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-002
**Title:** Validate Transaction ID is required for BANK_TRANSFER payment mode

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- At least one enquiry or student has outstanding fees

**Steps:**
1. Navigate to Finance → Fee Collection
2. Click "Collect Payment" for any entry
3. Fill in:
   - Amount: 10000
   - Payment Date: Today's date
   - Payment Mode: Select "BANK_TRANSFER"
   - Leave Transaction Reference empty
4. Click "Collect Payment"

**Expected Result:**
- Form validation should trigger
- Error message should appear: "Transaction reference is required for this payment mode"
- Payment should NOT be submitted
- Field shows required indicator (*)

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-003
**Title:** Validate Transaction ID is required for CHEQUE payment mode

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- At least one enquiry or student has outstanding fees

**Steps:**
1. Navigate to Finance → Fee Collection
2. Click "Collect Payment" for any entry
3. Fill in:
   - Amount: 5000
   - Payment Date: Today's date
   - Payment Mode: Select "CHEQUE"
   - Leave Transaction Reference empty
4. Click "Collect Payment"

**Expected Result:**
- Form validation should trigger
- Error message should appear
- Payment should NOT be submitted

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-004
**Title:** Validate Transaction ID is NOT required for CASH payment mode

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- At least one enquiry or student has outstanding fees

**Steps:**
1. Navigate to Finance → Fee Collection
2. Click "Collect Payment" for any entry
3. Fill in:
   - Amount: 2000
   - Payment Date: Today's date
   - Payment Mode: Select "CASH"
   - Leave Transaction Reference empty
4. Click "Collect Payment"

**Expected Result:**
- Form should submit successfully
- Receipt should be generated
- No error about transaction reference
- Field should NOT show required indicator (no *)

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-005
**Title:** Successfully collect payment with UPI and Transaction ID

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- At least one enquiry or student has outstanding fees

**Steps:**
1. Navigate to Finance → Fee Collection
2. Click "Collect Payment" for any entry
3. Fill in:
   - Amount: 15000
   - Payment Date: Today's date
   - Payment Mode: Select "UPI"
   - Transaction Reference: "UTR123456789012"
4. Click "Collect Payment"

**Expected Result:**
- Payment should be collected successfully
- Receipt should be displayed with:
  - Receipt number
  - Amount paid
  - Payment mode: UPI
  - Transaction reference: UTR123456789012
- Backend should store transaction reference in database

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-006
**Title:** Validate Transaction ID with whitespace only for UPI

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- At least one enquiry or student has outstanding fees

**Steps:**
1. Navigate to Finance → Fee Collection
2. Click "Collect Payment" for any entry
3. Fill in:
   - Amount: 5000
   - Payment Date: Today's date
   - Payment Mode: Select "UPI"
   - Transaction Reference: "   " (only spaces)
4. Click "Collect Payment"

**Expected Result:**
- Form validation should trigger
- Error message should appear (whitespace treated as empty)
- Payment should NOT be submitted

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-007
**Title:** Dynamic validation when changing payment mode

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- At least one enquiry or student has outstanding fees

**Steps:**
1. Navigate to Finance → Fee Collection
2. Click "Collect Payment" for any entry
3. Fill in:
   - Amount: 10000
   - Payment Date: Today's date
   - Payment Mode: Select "CASH"
   - Notice Transaction Reference field (no * indicator)
4. Change Payment Mode to "UPI"
5. Observe Transaction Reference field

**Expected Result:**
- When CASH is selected: no * indicator, not required
- When changed to UPI: * indicator appears, hint text shows
- Validation updates dynamically without touching the field

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-008
**Title:** Collect Payment Dialog validation (from Student Fee Details)

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- At least one student has semester fee allocation

**Steps:**
1. Navigate to Students → Select any student
2. Go to "Semester Fees" tab
3. Click "Collect Payment"
4. Fill in:
   - Amount: 8000
   - Payment Date: Today's date
   - Payment Mode: Select "BANK_TRANSFER"
   - Leave Transaction Reference empty
5. Click "Pay"

**Expected Result:**
- Form validation should trigger
- Error message should appear
- Payment should NOT be submitted
- Same validation rules apply in dialog as in fee collection page

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-009
**Title:** Term Fee Payment validation (from Academic Year)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- Academic year with term instances exists
- At least one fee demand exists

**Steps:**
1. Navigate to Academic Setup → Academic Years
2. Open an academic year detail view
3. Find a fee demand with outstanding amount
4. Click "Record Payment"
5. Fill in:
   - Amount: 7500
   - Payment Date: Today's date
   - Payment Mode: Select "CHEQUE"
   - Leave Transaction Reference empty
6. Click "Record Payment"

**Expected Result:**
- Form validation should trigger
- Error message should appear
- Payment should NOT be recorded
- Same validation rules apply in term fee payment dialog

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-010
**Title:** Backend validation for missing Transaction ID (API direct call)

**Preconditions:**
- Backend is running
- User has valid authentication token

**Steps:**
1. Make a POST request to `/api/v1/enquiries/{id}/collect-payment` with body:
```json
{
  "amountPaid": 5000,
  "paymentDate": "2026-05-05",
  "paymentMode": "UPI"
}
```
(Note: transactionReference is missing)

**Expected Result:**
- HTTP 400 Bad Request
- Response body should contain validation error message
- Message should indicate transaction reference is required for UPI

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-011
**Title:** Backend validation accepts Transaction ID for UPI

**Preconditions:**
- Backend is running
- User has valid authentication token

**Steps:**
1. Make a POST request to `/api/v1/enquiries/{id}/collect-payment` with body:
```json
{
  "amountPaid": 5000,
  "paymentDate": "2026-05-05",
  "paymentMode": "UPI",
  "transactionReference": "UTR987654321098"
}
```

**Expected Result:**
- HTTP 201 Created or HTTP 200 OK
- Payment is recorded successfully
- Response includes transaction reference in payment details

**Status:** NOT TESTED

---

## Test Case ID: TC-FEE-TX-012
**Title:** Database stores Transaction Reference correctly

**Preconditions:**
- Payment collected with UPI and transaction reference

**Steps:**
1. Collect a payment with:
   - Payment Mode: UPI
   - Transaction Reference: "TEST-UTR-12345"
2. Query database:
   - For fee_payments table: `SELECT * FROM fee_payments WHERE receipt_number = '{receiptNo}'`
   - For term_fee_payments table: `SELECT * FROM term_fee_payments WHERE receipt_number = '{receiptNo}'`

**Expected Result:**
- transaction_reference column should contain "TEST-UTR-12345"
- No data loss or truncation
- Column accepts up to 255 characters

**Status:** NOT TESTED

---

## Payment Mode Test Matrix

| Payment Mode | Transaction ID Required | Test Status |
|--------------|------------------------|-------------|
| CASH | No | NOT TESTED |
| UPI | **Yes** | NOT TESTED |
| BANK_TRANSFER | **Yes** | NOT TESTED |
| CHEQUE | **Yes** | NOT TESTED |
| CARD | No | NOT TESTED |
| NET_BANKING | No | NOT TESTED |
| DEMAND_DRAFT | No | NOT TESTED |
| SCHOLARSHIP | No | NOT TESTED |

---

## Notes for Testers

1. **Backend Validation**: The validation is enforced at both frontend (Angular) and backend (Spring Boot) levels
2. **Migration**: A database migration (V96) adds the `transaction_reference` column to `term_fee_payments` table
3. **Existing Data**: Existing payments without transaction references remain valid (column allows NULL)
4. **Error Messages**:
   - Frontend: "Transaction reference is required for this payment mode"
   - Backend: "Transaction reference is required for UPI, Bank Transfer, and Cheque payments"
5. **Affected Endpoints**:
   - `POST /api/v1/enquiries/{id}/collect-payment`
   - `POST /api/v1/finance/students/{id}/collect-payment`
   - `POST /api/term-fee-payments`

