## TC-RETRO-STEP-001: Stepper highlight progresses to Fee Structure and Payment History on scroll

**Preconditions:**
- User is logged in with access to `Admission Management > Retro Admit`
- Retro Admit page is open

**Steps:**
1. Open `Retro Admit` screen.
2. Select a program so all sections are available.
3. Scroll down slowly and observe left stepper highlight.
4. Continue scrolling to the bottom of the page.

**Expected Result:**
- Stepper highlight moves from `Referral` to `Fee Structure` and then `Payment History`.
- At the scroll bottom, `Payment History` is the active highlighted step.

**Status:** NOT TESTED

## TC-RETRO-STEP-002: Fee/Payment sections still render before program selection

**Preconditions:**
- User is logged in with access to `Admission Management > Retro Admit`
- Retro Admit page is opened fresh without selecting a program

**Steps:**
1. Open `Retro Admit`.
2. Without selecting a program, scroll down to Fee/Payment sections.
3. Verify `Fee Structure` section displays a guidance message.
4. Verify `Add Payment` button is disabled until program/year rows are available.

**Expected Result:**
- `Fee Structure` and `Payment History` sections are present in page flow.
- Guidance is visible and payment add action is disabled until program is selected.

**Status:** NOT TESTED

