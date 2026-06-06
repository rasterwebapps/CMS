## TC-PRF-001: Profile page loads correctly after SCSS cleanup

**Preconditions:**
- Frontend dependencies are installed (`npm install` completed)
- User can log in and access `/profile`

**Steps:**
1. Run `npm start` from `frontend/`.
2. Open `/profile` in light mode and verify hero, About, Personal Info, Contact, Preferences, Notifications, and Edit drawer UI.
3. Switch to dark mode and repeat visual verification.
4. Click `Edit Profile`, update a field, and confirm the drawer opens/closes and buttons remain aligned.

**Expected Result:**
- Profile page renders without missing styles in both light and dark modes.
- Existing profile interactions work normally after style cleanup.

**Status:** NOT TESTED

## TC-PRF-003: Dead style cleanup does not affect visible profile sections

**Preconditions:**
- Frontend is running (`npm start`)
- User is logged in with access to `/profile`

**Steps:**
1. Open `/profile` and verify `About`, `Role & Access`, `Preferences`, and `Notifications` cards are styled correctly.
2. Open the theme picker and confirm dropdown styling remains intact.
3. Open and close `Edit Profile` drawer and verify layout/spacing of fields and footer buttons.
4. Repeat checks in both dark and light modes.

**Expected Result:**
- No missing or broken UI blocks after removing unused SCSS selectors.
- Theme picker, cards, and drawer continue to render and behave correctly.

**Status:** NOT TESTED

## TC-PRF-002: Frontend build has no profile component style budget warning

**Preconditions:**
- Frontend dependencies are installed (`npm install` completed)

**Steps:**
1. Run `npm run build` from `frontend/`.
2. Check build output for warnings related to `src/app/features/profile/profile.component.scss`.

**Expected Result:**
- Build completes successfully.
- No `anyComponentStyle` budget warning is reported for `profile.component.scss`.

**Status:** NOT TESTED

