# Scholarship Workflow — Manual Test Cases

These test cases cover the full scholarship workflow:  
Step B (Update Eligibility) → Step C (Verify) → Step D (Apply) → Step E (Approve / Reject).

---

## TC-SCH-B-001: Open Eligibility Edit Dialog from Student Detail

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`
- A student record exists in the system

**Steps:**
1. Navigate to **Students → [Any Student] → Detail**
2. Click the **"Scholarships"** tab
3. Click the **"Edit Eligibility"** button (pen-and-notepad icon)

**Expected Result:**
- Dialog opens with title "Update Eligibility Profile — [Student Name]"
- Four checkbox flags visible: First Graduate, Merit Based, Sports Quota, EWS
- Annual Family Income number field
- Three certificate sections: Community Certificate, Income Certificate, First Graduate Certificate
- First Graduate Certificate section is hidden until the "First Graduate" checkbox is ticked

**Status:** NOT TESTED

---

## TC-SCH-B-002: Set First Graduate flag — certificate section appears

**Preconditions:**
- Eligibility Edit Dialog is open

**Steps:**
1. Tick the **"First Graduate in Family"** checkbox
2. Observe the form

**Expected Result:**
- A new section "First Graduate Certificate & Parents' Education" appears below the Income Certificate section
- Fields: Certificate Number, Issuing Authority, Issue Date, Father's Education, Mother's Education

**Status:** NOT TESTED

---

## TC-SCH-B-003: Annual Income < ₹3L triggers EWS hint

**Preconditions:**
- Eligibility Edit Dialog is open

**Steps:**
1. Enter `250000` in the **Annual Family Income** field

**Expected Result:**
- A blue hint message appears: "Income below ₹3,00,000 automatically qualifies as EWS."

**Status:** NOT TESTED

---

## TC-SCH-B-004: Save eligibility profile

**Preconditions:**
- Eligibility Edit Dialog is open

**Steps:**
1. Tick "First Graduate in Family"
2. Enter `200000` in Annual Family Income
3. Fill Community Certificate Number: `TN/2025/CC/001`, Authority: `Tahsildar`, Date: any past date
4. Fill Income Certificate Number: `TN/2025/IC/001`, Authority: `Tahsildar Salem`, Date: any past date
5. Fill First Graduate Certificate Number: `TN/2025/FG/001`, Authority: `VAO`, Date: any past date
6. Fill Father's Education: `Class 10`, Mother's Education: `Illiterate`
7. Click **"Save Eligibility"**

**Expected Result:**
- Dialog closes
- Toast: "Eligibility profile updated"
- Scholarships tab refreshes:
  - Eligibility Profile section now shows all filled certificate details
  - Eligible Scholarships section updates (FIRST_GRAD and/or EWS scholarships appear)

**Status:** NOT TESTED

---

## TC-SCH-C-001: Verify eligibility via dialog

**Preconditions:**
- Eligibility profile has been filled (TC-SCH-B-004 passed)
- User has `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`

**Steps:**
1. On the Scholarships tab, click **"Verify"** button
2. A dialog opens: "Verify Eligibility — [Student Name]"
3. Enter remarks: `Documents physically verified. Original certificates seen.`
4. Click **"Confirm Verification"**

**Expected Result:**
- Dialog closes
- Toast: "Eligibility verified successfully"
- A green verification banner appears on the Scholarships tab:  
  `✓ Verified by [admin username] on [today's date]`
- The Verify button now shows "Re-verify" label

**Status:** NOT TESTED

---

## TC-SCH-C-002: Re-verify shows previous remarks pre-filled

**Preconditions:**
- Eligibility already verified (TC-SCH-C-001 passed)

**Steps:**
1. Click **"Re-verify"** button

**Expected Result:**
- Dialog opens
- A grey "Already verified by [user]" banner is shown at the top of the dialog
- The remarks textarea is pre-filled with previous verification remarks

**Status:** NOT TESTED

---

## TC-SCH-D-001: Apply for a detected eligible scholarship

**Preconditions:**
- Eligibility profile is saved with isFirstGraduate = true
- FIRST_GRAD scholarship type is active in the system

**Steps:**
1. On the Scholarships tab, locate the **"Eligible Scholarships"** section
2. Verify FIRST_GRAD appears in the table
3. Click **"Apply"** next to FIRST_GRAD

**Expected Result:**
- Toast: "Scholarship application submitted"
- The "Applications" section below now shows a new row:
  - Academic Year: current year
  - Scholarship: FIRST_GRAD
  - Status: `PENDING` badge
  - Applied: today's date
- The "Apply" button for all eligible scholarships is now disabled (one per year rule)

**Status:** NOT TESTED

---

## TC-SCH-D-002: Duplicate application blocked

**Preconditions:**
- Student already has a PENDING/APPROVED application for the current academic year

**Steps:**
1. Try to click "Apply" for another eligible scholarship

**Expected Result:**
- "Apply" button is disabled (greyed out)
- No second application can be created

**Status:** NOT TESTED

---

## TC-SCH-E-001: Approve a scholarship application via proper dialog

**Preconditions:**
- A student has a PENDING scholarship application
- User has `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`

**Steps:**
1. Navigate to **Finance → Scholarship Applications**
2. Find the PENDING application row
3. Click the green **✓** (approve) button
4. Approve dialog opens: "Approve Scholarship"
5. Student details card shows: name, roll number, scholarship name, academic year
6. Enter Amount: `15000`
7. Disbursement Frequency: `Annual (once per year)`
8. Valid From: `2025-06-01`, Valid Till: `2026-05-31`
9. Remarks: `SC Govt scholarship approved for AY 2025-26`
10. Click **"Approve"**

**Expected Result:**
- Dialog closes
- Toast: "Scholarship approved successfully"
- The application row is removed from the pending list (or status changes to APPROVED)
- On the student's Scholarships tab, the application now shows status `APPROVED` with the approved amount

**Status:** NOT TESTED

---

## TC-SCH-E-002: Approve dialog validates amount

**Preconditions:**
- Approve dialog is open

**Steps:**
1. Clear the amount field or enter `0`
2. Click "Approve"

**Expected Result:**
- Validation error shown: "A valid amount is required (must be ≥ ₹1)"
- Dialog does not close
- No API call is made

**Status:** NOT TESTED

---

## TC-SCH-E-003: Reject a scholarship application via proper dialog

**Preconditions:**
- A student has a PENDING scholarship application

**Steps:**
1. Navigate to **Finance → Scholarship Applications**
2. Find the PENDING application row
3. Click the red **✗** (reject) button
4. Reject dialog opens: "Reject Scholarship Application"
5. Student details visible at the top
6. Enter reason: `Income certificate expired. Please resubmit with valid certificate.`
7. Click **"Reject Application"**

**Expected Result:**
- Dialog closes
- Toast: "Application rejected"
- The application status changes to `REJECTED`
- An info icon (ⓘ) appears on the rejected row with tooltip showing the rejection reason

**Status:** NOT TESTED

---

## TC-SCH-E-004: Reject dialog requires a reason

**Preconditions:**
- Reject dialog is open

**Steps:**
1. Leave the reason textarea blank
2. Click "Reject Application"

**Expected Result:**
- Validation error: "Rejection reason is required"
- Dialog does not close

**Status:** NOT TESTED

---

## TC-SCH-E-005: Student can see rejection reason on Scholarships tab

**Preconditions:**
- Student's scholarship application was rejected (TC-SCH-E-003 passed)

**Steps:**
1. Navigate to Student Detail → Scholarships tab
2. Look at the Applications section

**Expected Result:**
- The rejected row has status badge `REJECTED`
- An inline info chip shows the rejection reason text
- Full reason visible on hover (tooltip)

**Status:** NOT TESTED

---

## TC-SCH-RENEW-001: Renew an approved scholarship for the next year

**Preconditions:**
- Student has an APPROVED scholarship with `renewalRequired = true`
- A new academic year exists in the system

**Steps:**
1. Navigate to Student Detail → Scholarships tab → Applications section
2. Locate the APPROVED row with renewalRequired = "Yes"
3. Click **"Renew"**

**Expected Result:**
- Toast: "Scholarship renewal submitted"
- A new PENDING row appears for the next academic year
- Original APPROVED row remains unchanged

**Status:** NOT TESTED

