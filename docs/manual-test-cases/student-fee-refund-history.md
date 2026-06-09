## TC-STU-FEE-HIST-001: Approved refund voucher appears in student payment history

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or any role with student fee view permissions
- A student has at least one paid receipt
- A refund for that receipt is approved with `refundNumber`, `paymentDate`, and `paymentMode`

**Steps:**
1. Open the student fee detail page for that student.
2. Scroll to the **Payment History** section.
3. Locate the approved refund voucher row.

**Expected Result:**
- Refund voucher is shown as a separate history entry using the refund number.
- Entry displays `Refund Voucher` chip and `Against: <originalReceiptNumber>` metadata.
- Refund amount is displayed as a negative value.

**Status:** NOT TESTED

## TC-STU-FEE-HIST-002: Refund voucher does not expose payment receipt print/download buttons

**Preconditions:**
- Preconditions from `TC-STU-FEE-HIST-001`

**Steps:**
1. Open the student fee detail page.
2. In Payment History, find a normal payment entry and a refund voucher entry.
3. Compare action controls on both entries.

**Expected Result:**
- Normal payment entry shows print/download actions.
- Refund voucher entry does not show payment receipt print/download actions.

**Status:** NOT TESTED

## TC-STU-FEE-HIST-003: Receipt timeline keeps latest event first across payments and refunds

**Preconditions:**
- Student has at least one payment receipt and one approved refund voucher with different dates

**Steps:**
1. Open student fee detail page.
2. Review order of entries under Payment History.

**Expected Result:**
- Entries are sorted by latest payment/refund date first.
- Latest approved refund appears above older payment entries when applicable.

**Status:** NOT TESTED

