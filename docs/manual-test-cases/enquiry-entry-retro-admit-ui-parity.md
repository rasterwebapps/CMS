## TC-ENQ-UI-001: Enquiry Entry uses convert-style header, sidebar stepper, and sticky footer

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or a role that can open Enquiry Entry
- Frontend application is running

**Steps:**
1. Open `/enquiries/new`.
2. Verify the page shows the gradient header card with back button and title.
3. Verify the left sidebar stepper is visible on desktop widths (>900 px).
4. Click each step in the sidebar and verify the corresponding enquiry step content is shown.
5. Resize to mobile width (360 x 740) and verify sidebar hides while form remains usable.
6. Verify sticky footer remains reachable and save/cancel actions are visible.

**Expected Result:**
- Enquiry Entry visual layout and step navigation pattern match Enquiry Convert style.
- Mobile view remains usable without horizontal page scroll.

**Status:** NOT TESTED

## TC-ENQ-UI-002: Enquiry Entry business behavior remains unchanged after UI alignment

**Preconditions:**
- Same as TC-ENQ-UI-001

**Steps:**
1. Fill valid data across all enquiry steps.
2. Submit the form using the sticky footer action.
3. Repeat with one required field missing on each step and try to move next.

**Expected Result:**
- Valid payload saves successfully.
- Existing validation behavior remains unchanged and blocks invalid progression.

**Status:** NOT TESTED

## TC-RETRO-UI-001: Retro Admit uses convert-style header, sidebar stepper, and section scroll navigation

**Preconditions:**
- User is logged in with `ROLE_ADMIN` (or role allowed to retro admit)
- Frontend application is running

**Steps:**
1. Open `/students/retro-admit`.
2. Verify the gradient header card and convert-style layout are visible.
3. On desktop width (>900 px), verify left sidebar stepper is visible.
4. Click each step item and verify the page scrolls to the mapped section.
5. Scroll the page manually and verify active step updates in sidebar.
6. Resize to mobile width (360 x 740) and verify sidebar hides but form is fully accessible.

**Expected Result:**
- Retro Admit follows the same convert UI shell while preserving original form sections.
- Section navigation works from sidebar and remains mobile-compatible.

**Status:** NOT TESTED

## TC-RETRO-UI-002: Retro Admit submit flow remains unchanged after UI alignment

**Preconditions:**
- Same as TC-RETRO-UI-001

**Steps:**
1. Fill all required retro admit fields and submit.
2. Verify success panel renders with admission details.
3. Click `View Student` and `Admit Another` actions.

**Expected Result:**
- Existing submit API behavior and success actions remain intact.
- No regression in fee table/payment history sections.

**Status:** NOT TESTED

