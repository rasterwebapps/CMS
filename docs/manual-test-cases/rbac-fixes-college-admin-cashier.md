# Manual Test Cases - Role-Based Access Control (RBAC) Fixes
## College Admin, Front Office, and Cashier Permissions

**Date**: April 27, 2026  
**Version**: 1.0  
**Status**: To be executed

---

## Test Environment Setup

### Prerequisites
- Application running locally or on staging environment
- Keycloak with roles configured: `ROLE_ADMIN`, `ROLE_COLLEGE_ADMIN`, `ROLE_FRONT_OFFICE`, `ROLE_CASHIER`
- Test data with enquiries in various statuses (INTERESTED, FEES_FINALIZED, PARTIALLY_PAID, FEES_PAID, DOCUMENTS_SUBMITTED)

### Test Users
- **User 1**: Admin account with `ROLE_ADMIN`
- **User 2**: College Admin account with `ROLE_COLLEGE_ADMIN`
- **User 3**: Front Office account with `ROLE_FRONT_OFFICE`
- **User 4**: Cashier account with `ROLE_CASHIER`

---

## TC-RBAC-FIX-001: College Admin Collects Payment from Enquiry Shortcut

**Preconditions**:
- User is logged in as College Admin
- Application is running
- At least one enquiry exists with status `FEES_FINALIZED` or `PARTIALLY_PAID`

**Steps**:
1. Navigate to `/enquiries` (Enquiries list)
2. Find an enquiry with status `FEES_FINALIZED` or `PARTIALLY_PAID`
3. Verify that the `Collect Payment` button (success/green button) is visible in the actions column
4. Click the `Collect Payment` button
5. Verify navigation to `/student-fees/collect-payment?enquiryId={id}`
6. Verify the enquiry details are pre-filled on the payment collection form
7. Enter payment amount and payment date
8. Select payment method (e.g., CASH, ONLINE)
9. Click "Collect Payment" button
10. Verify HTTP 201 response and success message "Payment recorded"
11. Verify receipt is displayed with payment details
12. Verify new payment appears in enquiry's Payments tab

**Expected Result**:
- ✅ College Admin can see "Collect Payment" button for eligible enquiries
- ✅ College Admin can navigate to payment collection screen
- ✅ Payment collection form accepts and processes the payment
- ✅ Backend returns HTTP 201 and payment is saved
- ✅ Success toast notification appears
- ✅ Receipt details displayed correctly

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-002: College Admin Finalizes Fees from Enquiry Shortcut

**Preconditions**:
- User is logged in as College Admin
- Application is running
- At least one enquiry exists with status `INTERESTED`

**Steps**:
1. Navigate to `/enquiries` (Enquiries list)
2. Find an enquiry with status `INTERESTED`
3. Verify that the `Finalize Fee` button (action button) is visible in the actions column
4. Click the `Finalize Fee` button
5. Verify navigation to `/student-fees/finalize?enquiryId={id}`
6. Verify the enquiry pre-fills with its program, course, and student type
7. Review the fee breakdown by year
8. Optionally apply a global discount
9. Click "Finalize Fee" button
10. Verify HTTP 201 response and success message
11. Verify enquiry status changes from `INTERESTED` to `FEES_FINALIZED`
12. Verify fee allocation appears in student fee records

**Expected Result**:
- ✅ College Admin can see "Finalize Fee" button for INTERESTED enquiries
- ✅ College Admin can navigate to fee finalization screen
- ✅ Fee finalization form pre-fills correctly
- ✅ Backend returns HTTP 201
- ✅ Success message displayed
- ✅ Enquiry status transitions to FEES_FINALIZED
- ✅ Fee allocation saved to database

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-003: College Admin Submits Documents from Enquiry Shortcut

**Preconditions**:
- User is logged in as College Admin
- Application is running
- At least one enquiry exists with status `FEES_PAID` or `PARTIALLY_PAID`
- All mandatory documents have been uploaded for the enquiry

**Steps**:
1. Navigate to `/enquiries` (Enquiries list)
2. Find an enquiry with status `FEES_PAID` or `PARTIALLY_PAID`
3. Verify that the `Submit Documents` button is visible in the actions column
4. Click the `Submit Documents` button
5. Verify navigation to `/enquiries/document-submission/{id}`
6. Verify document collection screen shows all documents
7. Verify all mandatory documents are marked as "UPLOADED" or "VERIFIED"
8. Click "Submit Documents" button on the detail page
9. Verify HTTP 200 response and success message
10. Verify enquiry status changes to `DOCUMENTS_SUBMITTED`
11. Verify enquiry no longer shows in FEES_PAID tab

**Expected Result**:
- ✅ College Admin can see "Submit Documents" button for eligible enquiries
- ✅ Documents are pre-filled on the submission screen
- ✅ Backend processes submission and returns HTTP 200
- ✅ Success message "Documents submitted successfully" appears
- ✅ Enquiry status transitions to DOCUMENTS_SUBMITTED
- ✅ Enquiry removed from eligible enquiries list

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-004: College Admin Creates Admission from Enquiry Shortcut

**Preconditions**:
- User is logged in as College Admin
- Application is running
- At least one enquiry exists with status `DOCUMENTS_SUBMITTED`

**Steps**:
1. Navigate to `/enquiries` (Enquiries list)
2. Find an enquiry with status `DOCUMENTS_SUBMITTED`
3. Verify the enquiry can be clicked to view details
4. Click on the enquiry row to open enquiry detail view
5. On the detail page, verify "Create Admission" button is visible
6. Click "Create Admission" button
7. Verify navigation to `/enquiries/{id}/convert`
8. Verify enquiry details pre-fill in the conversion form
9. Fill in required student fields (semester, admission date, academic year, etc.)
10. Click "Create Admission" button
11. Verify HTTP 201 response
12. Verify success message "Admission created successfully"
13. Verify enquiry status changes to `ADMITTED`
14. Verify new student record created in Students list

**Expected Result**:
- ✅ College Admin can see "Create Admission" button for DOCUMENTS_SUBMITTED enquiries
- ✅ College Admin can navigate to admission creation form
- ✅ Form pre-fills with enquiry data correctly
- ✅ Backend returns HTTP 201 and creates admission + student
- ✅ Success message displayed
- ✅ Enquiry status transitions to ADMITTED
- ✅ Student appears in Students list

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-005: Cashier Collects Payment from Enquiry Shortcut

**Preconditions**:
- User is logged in as Cashier
- Application is running
- At least one enquiry exists with status `FEES_FINALIZED` or `PARTIALLY_PAID`

**Steps**:
1. Navigate to `/enquiries` (Enquiries list)
2. Find an enquiry with status `FEES_FINALIZED` or `PARTIALLY_PAID`
3. Verify that the `Collect Payment` button is visible in the actions column
4. Click the `Collect Payment` button
5. Verify navigation to `/student-fees/collect-payment?enquiryId={id}`
6. Verify enquiry details are pre-filled
7. Enter payment amount: ₹5000
8. Enter payment date
9. Select payment method: "CASH"
10. Click "Collect Payment" button
11. Verify HTTP 201 response
12. Verify success message and receipt details
13. Verify payment appears in payment history

**Expected Result**:
- ✅ Cashier can view Enquiries list
- ✅ Cashier can see "Collect Payment" button for eligible enquiries
- ✅ Cashier can navigate to payment collection screen
- ✅ Backend authorizes Cashier (returns 201, not 403)
- ✅ Payment is saved with Cashier's username in `collectedBy` field
- ✅ Receipt generated and displayed
- ✅ Payment appears in enquiry's payment history

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-006: Cashier Cannot Finalize Fees

**Preconditions**:
- User is logged in as Cashier
- Application is running
- At least one enquiry exists with status `INTERESTED`

**Steps**:
1. Navigate to `/enquiries` (Enquiries list)
2. Verify that NO "Finalize Fee" button is visible for any enquiry
3. Attempt direct navigation to `/student-fees/finalize?enquiryId={id}`
4. Attempt to send POST request to `/api/v1/enquiries/{id}/finalize-fees` with valid payload
5. Capture response details

**Expected Result**:
- ✅ Cashier cannot see "Finalize Fee" button in enquiry list (button is hidden)
- ✅ Cashier receives HTTP 403 Forbidden when attempting direct navigation
- ✅ Backend authorization check prevents Cashier from finalizing fees
- ✅ Error message indicates insufficient permissions

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-007: Front Office Cannot Finalize Fees

**Preconditions**:
- User is logged in as Front Office
- Application is running
- At least one enquiry exists with status `INTERESTED`

**Steps**:
1. Navigate to `/enquiries` (Enquiries list)
2. Verify that NO "Finalize Fee" button is visible for any enquiry
3. Attempt direct API call: `POST /api/v1/enquiries/{id}/finalize-fees`
4. Send valid fee finalization request body
5. Capture HTTP response

**Expected Result**:
- ✅ Front Office cannot see "Finalize Fee" button (hidden by role check)
- ✅ Backend returns HTTP 403 Forbidden
- ✅ Error indicates Front Office lacks permission to finalize fees

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-008: API Authorization Enforcement

**Preconditions**:
- Valid JWT tokens available for each role
- API endpoint `/api/v1/enquiries/{id}/payments` accessible
- Test enquiry with existing payments

**Steps**:

### Sub-test 8A: College Admin Can Collect Payment via API
1. Obtain JWT token with `ROLE_COLLEGE_ADMIN`
2. Send POST request to `/api/v1/enquiries/{id}/payments`
3. Include valid `EnquiryPaymentRequest` body
4. Verify HTTP 201 response
5. Verify payment record created in database

### Sub-test 8B: Cashier Can Collect Payment via API
1. Obtain JWT token with `ROLE_CASHIER`
2. Send POST request to `/api/v1/enquiries/{id}/payments`
3. Include valid payload
4. Verify HTTP 201 response

### Sub-test 8C: Front Office Cannot Finalize Fees via API
1. Obtain JWT token with `ROLE_FRONT_OFFICE`
2. Send POST request to `/api/v1/enquiries/{id}/finalize-fees`
3. Include valid `FeeFinalizationRequest` body
4. Verify HTTP 403 Forbidden response

### Sub-test 8D: Cashier Cannot Finalize Fees via API
1. Obtain JWT token with `ROLE_CASHIER`
2. Send POST request to `/api/v1/enquiries/{id}/finalize-fees`
3. Verify HTTP 403 Forbidden response

**Expected Result**:
- ✅ 8A: College Admin receives 201, payment saved
- ✅ 8B: Cashier receives 201, payment saved
- ✅ 8C: Front Office receives 403 Forbidden
- ✅ 8D: Cashier receives 403 Forbidden

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-009: Navigation Menu Restrictions

**Preconditions**:
- Each role account logged in separately
- Application running
- All browser caches cleared

**Steps**:

### Sub-test 9A: College Admin Menu Items
1. Login as College Admin
2. Open side navigation menu
3. Verify visible menu items:
   - Dashboard ✅
   - Preferences (visible, expandable) ✅
   - Admission Management ✅
   - Finance ✅
   - Reports ✅
4. Verify NOT visible:
   - Curriculum & Academics ❌
   - Examinations ❌
   - Lab & Infrastructure ❌
   - (Settings hidden - only Admin) ❌

### Sub-test 9B: Cashier Menu Items
1. Login as Cashier
2. Open side navigation menu
3. Verify visible menu items:
   - Dashboard ✅
   - Admission Management (only Enquiries) ✅
   - Finance (only Fee Payments) ✅
   - Reports ✅
4. Verify NOT visible:
   - Preferences ❌
   - Student Fees ❌
   - Fee Finalization ❌
   - Curriculum & Academics ❌
   - Lab & Infrastructure ❌

### Sub-test 9C: Front Office Menu Items
1. Login as Front Office
2. Open side navigation menu
3. Verify visible menu items:
   - Dashboard ✅
   - Admission Management ✅
   - Reports ✅
4. Verify NOT visible:
   - Preferences ❌
   - Finance ❌
   - Curriculum & Academics ❌
   - Lab & Infrastructure ❌

**Expected Result**:
- ✅ College Admin sees all appropriate menu items
- ✅ Cashier sees only Finance (Fee Payments, NOT Fee Finalization) and Admission Management shortcuts
- ✅ Front Office sees only Admission Management and Reports
- ✅ Admin-only items hidden from other roles

**Actual Result**:
- Status: `NOT TESTED`

---

## TC-RBAC-FIX-010: End-to-End Workflow - College Admin

**Preconditions**:
- User is logged in as College Admin
- No enquiries exist or all are CLOSED
- Application is running with sample data loaded

**Steps**:
1. Create new enquiry for student "John Doe" with program "B.Tech"
2. Verify enquiry status is `ENQUIRED`, then mark as `INTERESTED`
3. Navigate to enquiry list and find newly created enquiry
4. Click "Finalize Fee" button
5. Verify navigation to fee finalization screen
6. Apply ₹5000 discount
7. Click "Finalize Fee"
8. Verify enquiry status changed to `FEES_FINALIZED`
9. Go back to enquiry list
10. Find enquiry and click "Collect Payment"
11. Record payment of ₹10000 via CASH
12. Verify receipt and success message
13. Navigate back to Admission Management > Enquiries
14. Find same enquiry and click to open detail
15. Verify Documents tab is accessible
16. Verify Submit Documents button visible (status is now FEES_PAID)
17. Click "Submit Documents" button
18. Verify status transitions to `DOCUMENTS_SUBMITTED`
19. Verify "Create Admission" button now visible
20. Click "Create Admission"
21. Fill in student form with required data
22. Submit to create admission
23. Verify student record created and visible in Students list

**Expected Result**:
- ✅ All steps complete successfully
- ✅ Status transitions occur correctly (INTERESTED → FEES_FINALIZED → FEES_PAID → DOCUMENTS_SUBMITTED → ADMITTED)
- ✅ All shortcuts (Finalize, Collect Payment, Submit Documents, Create Admission) are visible and functional
- ✅ No 403 Forbidden errors occur
- ✅ Final student appears in Students list

**Actual Result**:
- Status: `NOT TESTED`

---

## Summary Table

| Test Case | College Admin | Front Office | Cashier | Admin | Status |
|-----------|:---:|:---:|:---:|:---:|:---:|
| TC-FIX-001: Payment Collection | ✅ | ✅ | ✅ | ✅ | NOT TESTED |
| TC-FIX-002: Fee Finalization | ✅ | ❌ | ❌ | ✅ | NOT TESTED |
| TC-FIX-003: Submit Documents | ✅ | ✅ | ❌ | ✅ | NOT TESTED |
| TC-FIX-004: Create Admission | ✅ | ✅ | ❌ | ✅ | NOT TESTED |
| TC-FIX-005: Cashier Payments | - | - | ✅ | ✅ | NOT TESTED |
| TC-FIX-006: Cashier Fee Block | - | - | ✅ | ✅ | NOT TESTED |
| TC-FIX-007: Front Office Fee Block | - | ✅ | - | ✅ | NOT TESTED |
| TC-FIX-008: API Authorization | ✅ | ✅ | ✅ | ✅ | NOT TESTED |
| TC-FIX-009: Navigation Menu | ✅ | ✅ | ✅ | ✅ | NOT TESTED |
| TC-FIX-010: End-to-End Workflow | ✅ | - | - | ✅ | NOT TESTED |

---

## Notes for Testers

### Browser Developer Tools
- Press F12 to open Developer Tools
- Go to "Network" tab to monitor API requests
- Check "Console" tab for any errors
- Verify HTTP response codes (201 = Success, 403 = Forbidden)

### JWT Token Inspection
- Install JWT.io Browser Extension
- Click extension after logging in to view JWT payload
- Verify `realm_access.roles` claim contains correct roles
- Example: `"realm_access": { "roles": ["ROLE_COLLEGE_ADMIN", "..."] }`

### Database Verification
- After successful payment: Check `fee_payments` and `enquiry_payments` tables
- After successful fee finalization: Check `student_fee_allocations` table
- After successful admission: Check `admissions` and `students` tables
- Verify `collectedBy`/`finalizedBy`/`createdBy` fields contain correct username

### API Testing with cURL
```bash
# Collect Payment (College Admin)
curl -X POST \
  http://localhost:8080/api/v1/enquiries/1/payments \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000,
    "paymentDate": "2026-04-27",
    "paymentMode": "CASH"
  }'

# Expected Response: HTTP 201
```

---

## Sign-off

| Role | Tester Name | Date | Status |
|------|---|---|---|
| College Admin | _____________ | __________ | [ ] PASS [ ] FAIL |
| Front Office | _____________ | __________ | [ ] PASS [ ] FAIL |
| Cashier | _____________ | __________ | [ ] PASS [ ] FAIL |
| Admin | _____________ | __________ | [ ] PASS [ ] FAIL |

