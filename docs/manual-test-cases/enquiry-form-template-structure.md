## TC-ENQ-001: Enquiry form loads without template parse errors

**Preconditions:**
- Frontend dependencies are installed (`npm install` completed)
- Backend is running or API calls are mocked/stubbed

**Steps:**
1. Run `npm start` from `frontend/`.
2. Open the app and navigate to `/enquiries/new`.
3. Verify the Enquiry form page renders with header, sidebar stepper, and sticky footer.
4. Click Save with empty form fields and confirm validation messages appear (no blank page/crash).

**Expected Result:**
- Angular compilation completes with no `NG5002` template parse errors from `enquiry-form.component.html`.
- Enquiry form UI renders correctly and remains interactive.

**Status:** NOT TESTED

## TC-ENQ-002: Frontend build has no Angular template/import diagnostics for enquiry fix scope

**Preconditions:**
- Frontend dependencies are installed (`npm install` completed)

**Steps:**
1. Run `npm run build` from `frontend/`.
2. Verify build output does not include `NG5002`, `NG8102`, or `NG8113` diagnostics for the files touched in this fix.
3. Navigate to `/enquiries/new`, `/inventory`, `/maintenance`, and `/reports`.
4. Confirm pages load and primary actions (search/filter/edit navigation) are still usable.

**Expected Result:**
- Build completes successfully.
- No Angular template/import diagnostics appear for the updated files.
- Updated screens render and behave as before.

**Status:** NOT TESTED

