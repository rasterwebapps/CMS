## TC-STU-FIN-001: Student Explorer shows paid fee from all successful receipts

**Preconditions:**
- User is logged in with a role that can view `Student Explorer` and `Receipts`
- A student exists with at least one successful receipt in `Receipts`
- That student has no fee-demand ledger entries (or an empty Finance ledger)

**Steps:**
1. Open `Finance > Receipts` and note the student and total of successful receipts (`PAYMENT` and `ENQUIRY_PAYMENT`).
2. Open `Student Management > Student Explorer` and open the same student profile.
3. Go to the `Finance` tab.
4. Verify `Total Paid` shows the same aggregated amount as the successful receipts total.
5. Verify the empty-state message is shown only for fee demands and does not hide the `Total Paid` summary.

**Expected Result:**
- `Total Paid` in Student Explorer Finance tab equals sum of all successful receipts for the student.
- Finance summary remains visible even when no fee-demand ledger entries exist.

**Status:** NOT TESTED

## TC-STU-FIN-002: Refund entries do not inflate paid fee total

**Preconditions:**
- User is logged in with finance/student view permissions
- A student has at least one `REFUND` receipt and one successful payment receipt

**Steps:**
1. Open `Finance > Receipts` and filter/identify the student.
2. Calculate sum of successful receipts only (`PAYMENT` + `ENQUIRY_PAYMENT`), excluding `REFUND` rows.
3. Open the same student in `Student Explorer` and navigate to `Finance` tab.
4. Compare displayed `Total Paid` with the successful-only sum.

**Expected Result:**
- `Total Paid` excludes refund voucher rows and matches only successful receipt totals.

**Status:** NOT TESTED

