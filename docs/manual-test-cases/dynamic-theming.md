# Dynamic User Theming — Manual Test Cases

## TC-THEME-001: Default theme applied on first load

**Preconditions:**
- No `cms_primary_theme` key exists in localStorage (clear it if needed)
- Application is running

**Steps:**
1. Open the application in a browser
2. Inspect `document.documentElement.style` in DevTools
3. Verify CSS variables `--color-primary-50` through `--color-primary-950` are set to Indigo values

**Expected Result:**
- The toolbar palette button is visible
- Primary palette CSS variables default to Indigo (e.g. `--color-primary-500: #6366f1`)

**Status:** NOT TESTED

---

## TC-THEME-002: Open theme picker

**Preconditions:**
- Application is running and the user is authenticated

**Steps:**
1. Click the **palette** icon in the top toolbar
2. A dropdown menu should appear

**Expected Result:**
- A panel labelled "Accent Colour" appears with a 6-column grid of 12 colour swatches
- The currently active swatch shows a white checkmark (✓) overlay
- Indigo shows the checkmark on first load

**Status:** NOT TESTED

---

## TC-THEME-003: Select a different accent colour

**Preconditions:**
- Application is running and the theme picker menu is open

**Steps:**
1. Click the **Rose** swatch (#f43f5e)
2. Close the menu and observe the application

**Expected Result:**
- The `--color-primary-500` CSS variable on `<html>` is updated to a rose-family hex value
- The full palette (`--color-primary-50` through `--color-primary-950`) reflects rose tones
- The Rose swatch now shows the checkmark; Indigo does not
- Any element using `bg-primary-*` Tailwind utilities visually updates to the rose palette

**Status:** NOT TESTED

---

## TC-THEME-004: Theme persists across page reload

**Preconditions:**
- A non-default swatch (e.g. Teal) has been selected

**Steps:**
1. Select the **Teal** swatch
2. Reload the page (F5 / Cmd+R)
3. Inspect `document.documentElement.style` in DevTools

**Expected Result:**
- `--color-primary-500` is set to a teal-family value (e.g. `#14b8a6`)
- The Teal swatch shows the checkmark in the picker
- `localStorage.getItem('cms_primary_theme')` returns `"teal"`

**Status:** NOT TESTED

---

## TC-THEME-005: All 12 swatches selectable

**Preconditions:**
- Application is running

**Steps:**
1. Open the theme picker
2. Click each of the 12 swatches one by one (Indigo, Violet, Purple, Blue, Sky, Cyan, Teal, Emerald, Rose, Pink, Orange, Amber)
3. After each click, inspect `--color-primary-500` in DevTools

**Expected Result:**
- Each swatch updates `--color-primary-500` to a colour matching its hue
- Only the most-recently clicked swatch shows the checkmark
- No errors appear in the browser console

**Status:** NOT TESTED

---

## TC-THEME-006: Generated palette covers all shades

**Preconditions:**
- Application is running

**Steps:**
1. Open DevTools → Elements → `<html>` style attribute
2. Select any swatch (e.g. Blue)
3. Verify the following variables are all present and different:
   `--color-primary-50`, `--color-primary-100`, `--color-primary-200`,
   `--color-primary-300`, `--color-primary-400`, `--color-primary-500`,
   `--color-primary-600`, `--color-primary-700`, `--color-primary-800`,
   `--color-primary-900`, `--color-primary-950`

**Expected Result:**
- 11 distinct hex values are set for the primary palette
- Shades 50–400 are progressively lighter than the base
- Shades 600–950 are progressively darker than the base

**Status:** NOT TESTED

---

## TC-THEME-007: Opacity modifier compatibility

**Preconditions:**
- A Tailwind utility that uses a primary colour opacity modifier is visible (e.g. `bg-primary-500/10` hover state)

**Steps:**
1. Select the **Emerald** swatch
2. Hover over an element known to use `bg-primary-500/10` (e.g. sidebar hover state)
3. Observe the background colour

**Expected Result:**
- The hover tint shifts to an emerald tone (not indigo)
- Confirms `color-mix(in oklch, var(--color-primary-500) 10%, transparent)` resolves correctly

**Status:** NOT TESTED

---

## TC-THEME-008: Dark-mode + theme colour combination

**Preconditions:**
- Application is running

**Steps:**
1. Select the **Violet** swatch
2. Click the dark-mode toggle in the toolbar
3. Observe the application in dark mode

**Expected Result:**
- Dark-mode CMS design tokens are applied (dark background, light text)
- Primary palette CSS variables still reflect Violet tones
- No visual regression — primary-coloured elements adopt violet, not indigo

**Status:** NOT TESTED
