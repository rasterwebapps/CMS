## TC-ENQ-DET-001: Referral name field falls back to referral type in enquiry detail

**Preconditions:**
- User can open `Enquiries` detail screen
- Enquiry record exists with a referral type (e.g., `Online`) and no referred student/faculty/staff name

**Steps:**
1. Open `Enquiries` list and click the target enquiry.
2. In `Overview > Contact & Source`, check `Referral` value.
3. Check `Referral Name` value for the same enquiry.

**Expected Result:**
- `Referral` shows the referral type.
- `Referral Name` also shows a meaningful value by falling back to referral type when referred person name is absent.

**Status:** NOT TESTED

