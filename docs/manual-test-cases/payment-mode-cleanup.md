# Payment Mode Cleanup — Manual Test Cases

## TC-PAY-MODE-001: Verify NET_BANKING is no longer available in Fee Collection dropdown

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN
- Application is running

**Steps:**
1. Navigate to Finance → Fee Collection
2. Select any student or enquiry with outstanding fees
3. Click to open the payment form / right panel
4. Click the "Payment Mode" dropdown

**Expected Result:**
- Dropdown contains: Cash, UPI (GPay / PhonePe / Paytm), Bank Transfer (NEFT / RTGS / IMPS), Card (Debit / Credit), Cheque, Demand Draft (DD), Scholarship / Fee Waiver
- "NET_BANKING" option is **not** present
- "BANK_TRANSFER" is represented as "Bank Transfer (NEFT / RTGS / IMPS)"

**Status:** NOT TESTED

---

## TC-PAY-MODE-002: Verify human-readable labels display correctly

**Preconditions:**
- User is logged in
- Application is running

**Steps:**
1. Navigate to Finance → Fee Collection
2. Open payment form
3. Observe the Payment Mode dropdown options

**Expected Result:**
- All labels are human-readable (e.g., "Bank Transfer (NEFT / RTGS / IMPS)" not "BANK_TRANSFER")
- Labels are consistent across all screens: Fee Collection, Enquiry Payment Collection, Student Fee Detail dialog

**Status:** NOT TESTED

---

## TC-PAY-MODE-003: Verify Scholarship option is available in payment modes

**Preconditions:**
- User is logged in with ROLE_CASHIER or ROLE_ADMIN

**Steps:**
1. Navigate to Finance → Fee Collection
2. Open payment form for any student
3. Click "Payment Mode" dropdown

**Expected Result:**
- "Scholarship / Fee Waiver" option is visible in the dropdown

**Status:** NOT TESTED

---

## TC-PAY-MODE-004: Verify Demand Draft requires transaction reference

**Preconditions:**
- User is logged in with ROLE_CASHIER

**Steps:**
1. Navigate to Finance → Fee Collection
2. Select a student with outstanding fees
3. Fill in Amount and Payment Date
4. Select "Demand Draft (DD)" as Payment Mode
5. Leave Transaction Reference blank
6. Click submit / Collect Payment

**Expected Result:**
- Validation error appears: "Transaction reference is required for this payment mode"
- Payment is not submitted

**Steps (success case):**
7. Enter a DD number in Transaction Reference
8. Click submit

**Expected Result:**
- Payment is accepted successfully

**Status:** NOT TESTED

---

## TC-PAY-MODE-005: Verify payment receipt displays human-readable mode label

**Preconditions:**
- At least one payment recorded via UPI or Bank Transfer

**Steps:**
1. Navigate to Finance → Student Fee Detail for a student with payments
2. Scroll to "Payment History" section
3. Observe the payment mode shown on each receipt

**Expected Result:**
- Receipt shows "UPI (GPay / PhonePe / Paytm)" instead of raw "UPI"
- Receipt shows "Bank Transfer (NEFT / RTGS / IMPS)" instead of raw "BANK_TRANSFER"

**Status:** NOT TESTED

---

## TC-PAY-MODE-006: Verify existing NET_BANKING records migrated in DB

**Preconditions:**
- PostgreSQL environment (prod profile)
- Access to database

**Steps:**
1. Run: `SELECT COUNT(*) FROM fee_payments WHERE payment_mode = 'NET_BANKING';`
2. Run: `SELECT COUNT(*) FROM enquiry_payments WHERE payment_mode = 'NET_BANKING';`
3. Run: `SELECT COUNT(*) FROM term_fee_payments WHERE payment_mode = 'NET_BANKING';`

**Expected Result:**
- All queries return 0 (all NET_BANKING records have been migrated to BANK_TRANSFER by migration V113)

**Status:** NOT TESTED

