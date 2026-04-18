# Manual Test Cases — Fee Structure Classification & Enquiry Fee Flow

Covers: fee type classification (Generic / Additional) on the fee structure screen, course-filtered enquiry fee loading, read-only fee total, student-type fee rules, and fee-finalization discount-only constraint.

---

## TC-FSCLS-001: Fee structure screen splits fee types into Generic and Additional sections

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one program and academic year exist

**Steps:**
1. Navigate to Fee Structures → Add
2. Select a Program and an Academic Year

**Expected Result:**
- Fee items are displayed in two clearly labelled sections:
  - **Generic Fees (included in course total)**: Tuition Fee, Lab Fee, Library Fee, Examination Fee, Miscellaneous, Late Fee
  - **Additional Fees (hostel / transport)**: Hostel Fee, Transport Fee
- Additional fee items have a distinct visual style (e.g., different colour badge)

**Status:** NOT TESTED

---

## TC-FSCLS-002: Course Total (Generic) excludes hostel and transport fees

**Preconditions:**
- User is on the fee structure Add/Edit form with a program selected

**Steps:**
1. Enter ₹50,000 for Tuition Fee
2. Enter ₹5,000 for Lab Fee
3. Enter ₹20,000 for Hostel Fee
4. Enter ₹10,000 for Transport Fee
5. Observe the "Course Total (Generic)" and "Additional Fees Total" rows

**Expected Result:**
- "Course Total (Generic)" = ₹55,000 (Tuition + Lab only, NOT including Hostel or Transport)
- "Additional Fees Total" = ₹30,000 (Hostel + Transport)

**Status:** NOT TESTED

---

## TC-FSCLS-003: Course dropdown filters by selected program

**Preconditions:**
- Programs P1 and P2 exist
- Course C1 is under P1; Course C2 is under P2
- User is on the Enquiry Add form

**Steps:**
1. Select Program P1 from the Program dropdown
2. Observe the Course dropdown options

**Expected Result:**
- Only C1 appears in the Course dropdown (not C2)

**Status:** NOT TESTED

---

## TC-FSCLS-004: Fee total does NOT load on program selection alone

**Preconditions:**
- A fee structure exists for a program + course combination
- User is on the Enquiry Add form

**Steps:**
1. Select a Program from the dropdown
2. Observe the fee total area **before** selecting a course

**Expected Result:**
- No fee total is shown (fee panel / read-only fee field remains absent)
- No fee structure API call is made for just the program selection

**Status:** NOT TESTED

---

## TC-FSCLS-005: Fee total auto-loads (read-only) after course selection

**Preconditions:**
- A fee structure with Tuition ₹1,00,000 and Hostel ₹30,000 exists for Program P1 + Course C1
- User is on the Enquiry Add form

**Steps:**
1. Select Program P1
2. Select Course C1
3. Observe the fee display

**Expected Result:**
- A read-only fee amount box appears showing the total fee
- The total reflects Generic fees only (₹1,00,000) since no student type is selected yet
- The user cannot type into or edit this value

**Status:** NOT TESTED

---

## TC-FSCLS-006: Student type DAY_SCHOLAR adds transport fee to total

**Preconditions:**
- Fee structure for P1 + C1: Tuition ₹1,00,000, Transport ₹15,000, Hostel ₹30,000
- User is on Enquiry form with P1 + C1 already selected

**Steps:**
1. Select Student Type: Day Scholar
2. Observe the fee total

**Expected Result:**
- Total = ₹1,15,000 (Tuition ₹1,00,000 + Transport ₹15,000; Hostel excluded)
- Fee display label shows "(Day Scholar)"

**Status:** NOT TESTED

---

## TC-FSCLS-007: Student type HOSTELER adds hostel fee to total

**Preconditions:**
- Fee structure for P1 + C1: Tuition ₹1,00,000, Transport ₹15,000, Hostel ₹30,000
- User is on Enquiry form with P1 + C1 already selected

**Steps:**
1. Select Student Type: Hosteler
2. Observe the fee total

**Expected Result:**
- Total = ₹1,30,000 (Tuition ₹1,00,000 + Hostel ₹30,000; Transport excluded)
- Fee display label shows "(Hosteler)"

**Status:** NOT TESTED

---

## TC-FSCLS-008: Changing student type updates fee total dynamically

**Preconditions:**
- Fee structure for P1 + C1: Tuition ₹1,00,000, Transport ₹15,000, Hostel ₹30,000
- Enquiry form with P1 + C1 selected, Student Type = Day Scholar (total = ₹1,15,000)

**Steps:**
1. Change Student Type from Day Scholar to Hosteler
2. Observe the fee total

**Expected Result:**
- Fee total updates to ₹1,30,000 immediately (no page reload)

**Status:** NOT TESTED

---

## TC-FSCLS-009: Fee total is saved with enquiry record on create

**Preconditions:**
- Fee structure for P1 + C1: Tuition ₹80,000, Transport ₹12,000
- Filled enquiry form, Student Type = Day Scholar (total = ₹92,000)

**Steps:**
1. Fill all required fields and click Create
2. Open the newly created enquiry

**Expected Result:**
- Enquiry record has `feeGuidelineTotal` = 92000
- `finalCalculatedFee` = 92000 (+ commission if applicable)
- `studentType` = DAY_SCHOLAR

**Status:** NOT TESTED

---

## TC-FSCLS-010: Fee finalization total fee is pre-populated and read-only

**Preconditions:**
- An enquiry exists in INTERESTED status with `finalCalculatedFee` = ₹1,50,000

**Steps:**
1. Navigate to Fee Finalization
2. Click Finalize on the INTERESTED enquiry
3. Observe the Total Fee field

**Expected Result:**
- Total Fee field is pre-populated with ₹1,50,000
- The field is read-only (user cannot change the value)

**Status:** NOT TESTED

---

## TC-FSCLS-011: Fee finalization allows discount (fee cannot be increased)

**Preconditions:**
- Finalization form open with Total Fee = ₹1,50,000 (read-only)

**Steps:**
1. Enter Discount Amount = ₹10,000
2. Enter Discount Reason = "Merit scholarship"
3. Observe Net Fee

**Expected Result:**
- Net Fee = ₹1,40,000 (Total − Discount)
- Can click Finalize Fee successfully

**Status:** NOT TESTED

---

## TC-FSCLS-012: Fee finalization rejects discount exceeding total fee

**Preconditions:**
- Finalization form open with Total Fee = ₹1,50,000

**Steps:**
1. Enter Discount Amount = ₹2,00,000 (greater than total fee)
2. Click Finalize Fee

**Expected Result:**
- A validation error appears: "Discount cannot exceed the total fee"
- Form does not submit

**Status:** NOT TESTED

---

## TC-FSCLS-013: Fee finalization shows student type context

**Preconditions:**
- Enquiry has studentType = HOSTELER

**Steps:**
1. Open fee finalization for the above enquiry

**Expected Result:**
- Student Type row in the summary shows "Hosteler"

**Status:** NOT TESTED

---

## TC-FSCLS-014: Changing program in enquiry clears course and fee

**Preconditions:**
- Enquiry form with Program P1 and Course C1 selected (fee loaded)

**Steps:**
1. Change Program to P2

**Expected Result:**
- Course dropdown is reset (no course selected)
- Fee total display is cleared
- Course dropdown now shows only courses for P2

**Status:** NOT TESTED
