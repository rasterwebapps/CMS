# Live Data Walkthrough — Manual Test Cases

This document covers manual verification of the CMS Live Data Walkthrough tour system
built on Angular CDK Overlay (`TourService`, `TourSpotlightComponent`, `TourTooltipComponent`).

---

## TC-TOUR-001: Tour auto-starts for a first-time user

**Preconditions:**
- `cms_tour_show_onboarding` key is **not** present in `localStorage` (clear it manually if needed).
- User is logged in and on any page.

**Steps:**
1. Open the browser DevTools → Application → Local Storage and delete the `cms_tour_show_onboarding` key (or open a private/incognito window).
2. Reload the application and log in.
3. Observe the page after the app shell renders.

**Expected Result:**
- A semi-transparent dim overlay appears over the page.
- A floating tooltip is visible anchored below the toolbar with the title "Welcome to CMS".
- The step counter reads "Step 1 of 7".
- A "Next" button, a "Dismiss" link, and a "Don't show again" link are visible.

**Status:** NOT TESTED

---

## TC-TOUR-002: Tour does NOT auto-start when previously hard-closed

**Preconditions:**
- `cms_tour_show_onboarding` is set to `"false"` in `localStorage`.
- User is logged in.

**Steps:**
1. Set `localStorage.setItem('cms_tour_show_onboarding', 'false')` in DevTools console.
2. Reload the page.

**Expected Result:**
- No spotlight or tooltip appears on page load.

**Status:** NOT TESTED

---

## TC-TOUR-003: Navigate through all 7 steps using "Next"

**Preconditions:**
- Tour is active (auto-started or started via Help button).

**Steps:**
1. Confirm Step 1 is visible with title "Welcome to CMS".
2. Click "Next".
3. Confirm Step 2 shows "Navigation Sidebar" with the spotlight on the sidenav.
4. Click "Next".
5. Confirm Step 3 shows "Try the Menu Search" — Next button is **disabled** (greyed out).
6. Type any character in the sidenav search field.
7. Confirm the tour automatically advances to Step 4 after ~600 ms.
8. Click "Next" through Steps 4 ("Data Alignment — Currency"), 5 ("Data Alignment — Status Badges"), and 6 ("Data Alignment — Names & Text").
9. Confirm the alignment demo table is visible on Steps 4–6, with the appropriate row highlighted.
10. On Step 7, confirm the button reads "Done" instead of "Next".
11. Click "Done".

**Expected Result:**
- All 7 steps display in order with correct titles.
- Step counter increments correctly (e.g. "Step 3 of 7").
- After Step 7 "Done", the spotlight and tooltip are fully removed from the DOM (inspect with DevTools — no `.cms-tour-spotlight-panel` or `.cms-tour-tooltip-panel` elements remain).

**Status:** NOT TESTED

---

## TC-TOUR-004: Previous button navigates back

**Preconditions:**
- Tour is on Step 3 or later.

**Steps:**
1. Start the tour and advance to Step 3.
2. Click "Previous".

**Expected Result:**
- Tour returns to Step 2 ("Navigation Sidebar").
- Step counter reads "Step 2 of 7".
- "Previous" button is disabled on Step 1.

**Status:** NOT TESTED

---

## TC-TOUR-005: "Dismiss" soft-closes the tour

**Preconditions:**
- Tour is active on any step.

**Steps:**
1. Click the "Dismiss" link in the tooltip footer.

**Expected Result:**
- The spotlight and tooltip are removed from the DOM.
- `localStorage.getItem('cms_tour_show_onboarding')` is **not** `"false"` (preference is unchanged).
- Reloading the page causes the tour to auto-start again.

**Status:** NOT TESTED

---

## TC-TOUR-006: "Don't show again" hard-closes and persists preference

**Preconditions:**
- Tour is active on any step.

**Steps:**
1. Click the "Don't show again" link in the tooltip footer.
2. Inspect `localStorage` in DevTools.
3. Reload the page.

**Expected Result:**
- The spotlight and tooltip are removed from the DOM.
- `localStorage.getItem('cms_tour_show_onboarding')` equals `"false"`.
- After reload, the tour does NOT auto-start.

**Status:** NOT TESTED

---

## TC-TOUR-007: Close button (×) soft-closes the tour

**Preconditions:**
- Tour is active.

**Steps:**
1. Click the × button in the top-right corner of the tooltip.

**Expected Result:**
- Tour ends (overlay removed from DOM).
- Preference in `localStorage` is unchanged (same behaviour as "Dismiss").

**Status:** NOT TESTED

---

## TC-TOUR-008: Escape key closes the tour

**Preconditions:**
- Tour is active. Focus is anywhere on the page (not in an input with its own Escape handler).

**Steps:**
1. Press the `Escape` key.

**Expected Result:**
- Tour ends (overlay removed from DOM).
- Preference unchanged.

**Status:** NOT TESTED

---

## TC-TOUR-009: Help button restarts the tour after hard-close

**Preconditions:**
- Tour has been hard-closed (`cms_tour_show_onboarding = "false"`).

**Steps:**
1. Locate the `help_outline` icon button in the sidenav footer (expanded state) or rail (collapsed state).
2. Click it.

**Expected Result:**
- Tour starts from Step 1 regardless of the stored preference.
- `localStorage` preference is NOT reset to `true` — it remains `"false"`.

**Status:** NOT TESTED

---

## TC-TOUR-010: `h` keyboard shortcut starts the tour

**Preconditions:**
- Focus is not inside any text input, textarea, or select field.
- Tour is not already active.

**Steps:**
1. Press `h` (lowercase) on the keyboard from any page.

**Expected Result:**
- Tour starts from Step 1.

**Status:** NOT TESTED

---

## TC-TOUR-011: Clicking the dim backdrop hard-closes the tour

**Preconditions:**
- Tour is active with a visible spotlight and tooltip.

**Steps:**
1. Click anywhere on the semi-transparent dim area (not on the highlighted element and not on the tooltip).

**Expected Result:**
- Tour ends.
- `localStorage.getItem('cms_tour_show_onboarding')` equals `"false"` (clicking the backdrop is a hard-close).

**Status:** NOT TESTED

---

## TC-TOUR-012: Alignment demo table renders correctly per step

**Preconditions:**
- Tour is on Step 4, 5, or 6.

**Steps:**
1. Advance to Step 4 ("Data Alignment — Currency").
2. Verify the alignment demo table is visible. The **currency row** is highlighted and labelled "Right-aligned".
3. Advance to Step 5 ("Data Alignment — Status Badges").
4. Verify the **status badge row** is highlighted, showing a green "Active" badge, labelled "Centered".
5. Advance to Step 6 ("Data Alignment — Names & Text").
6. Verify the **name row** is highlighted, showing "Dr. Priya Sharma", labelled "Left-aligned".

**Expected Result:**
- Each step highlights exactly one row.
- `.cell-currency` class is applied to the amount (monospace, right-aligned).
- Status badge uses `.status-badge--active` (green pill).
- Name uses `.cell-name` (medium weight, text-primary colour).

**Status:** NOT TESTED

---

## TC-TOUR-013: Event-driven step (Step 3) — "Next" is disabled until interaction

**Preconditions:**
- Tour is on Step 3 ("Try the Menu Search").

**Steps:**
1. Observe the "Next" button.
2. Attempt to click "Next" without typing anything.
3. Type at least one character in the sidenav search input.
4. Observe the tour.

**Expected Result:**
- "Next" button is visually disabled (opaque, cursor: not-allowed) before typing.
- Clicking "Next" while disabled does nothing.
- After typing ≥1 character, the tour advances automatically to Step 4 after ~600 ms.
- The waiting-hint ("Waiting for your interaction…") pulsing dot is visible before the interaction.

**Status:** NOT TESTED

---

## TC-TOUR-014: Tooltip does not overlap the target element or go off-screen

**Preconditions:**
- Tour is active on any step that has a target element.

**Steps:**
1. Test on desktop (1440 px wide).
2. Resize the browser to 375 px (mobile width).
3. Step through several steps.

**Expected Result:**
- Tooltip stays within the viewport at all widths (no horizontal scroll needed).
- `max-width: calc(100vw - 32px)` prevents overflow on narrow screens.
- Tooltip flips sides when near the viewport edge (e.g. if anchored to the right sidenav on a narrow screen, it moves below the target).

**Status:** NOT TESTED

---

## TC-TOUR-015: DOM is clean after the tour ends

**Preconditions:**
- Tour has been started and ended (via any method).

**Steps:**
1. Open DevTools → Elements.
2. Search for `cms-tour-spotlight` and `cms-tour-tooltip`.

**Expected Result:**
- Neither element exists in the DOM after the tour ends.
- No CDK overlay panel with class `cms-tour-spotlight-panel` or `cms-tour-tooltip-panel` remains.

**Status:** NOT TESTED

---

## TC-TOUR-016: Tour keyboard shortcut `h` is listed in the shortcuts dialog

**Preconditions:**
- Application is running.

**Steps:**
1. Press `?` to open the keyboard shortcuts dialog.
2. Look for the "h" shortcut entry.

**Expected Result:**
- Dialog shows `h` → "Start Help tour".

**Status:** NOT TESTED
