# Manual Test Cases — Phase 5: Navigation & Shell Redesign

These test cases cover the Phase 5 redesign of the navigation shell:
hover-to-expand sidenav, breadcrumb trail, mobile responsiveness, and
keyboard shortcuts.

## Prerequisites

- Frontend running (`ng serve` or `npm run start`) at `http://localhost:4200`
- Backend reachable so menus and dashboard data load
- User logged in with `ROLE_ADMIN` (so all nav groups are visible)
- Test on three viewport widths: **desktop (1440 px)**, **tablet (900 px)**, and
  **mobile (390 px)**. Use the browser DevTools device toolbar.

---

## TC-NAV-001: Pin / Unpin sidenav rail (desktop)

**Preconditions:**
- Desktop viewport (≥ 1024 px), sidenav initially expanded.

**Steps:**
1. Click the `keyboard_tab` (unpin) icon in the sidenav header.
2. Move the mouse pointer **away** from the sidenav.
3. Move the mouse pointer over the icon rail and pause for ~0.5 s.
4. Move the mouse pointer **away** again.
5. Click the `push_pin` icon in the now-expanded overlay.

**Expected Result:**
- After step 1: sidenav collapses to a 68 px icon rail, page content stays in place.
- After step 2: the rail remains collapsed.
- After step 3: the sidenav expands as an overlay above the page content (page does **not** reflow); a soft drop shadow appears.
- After step 4: the overlay collapses back to the rail.
- After step 5: sidenav stays pinned-expanded; refreshing the page preserves the pinned state.

**Status:** NOT TESTED

---

## TC-NAV-002: Active item left-border accent

**Steps:**
1. Navigate to **Dashboard** (top-level entry).
2. Open the **Admission Management** group, then click **Enquiries**.
3. Unpin the sidenav (collapse to rail) and observe the rail icons.
4. With the rail collapsed, click the **Admission Management** group icon — its popup menu appears — hover an item to make it the active one.

**Expected Result:**
- In step 1: Dashboard row shows a **3 px solid left accent in the primary colour** + a faint primary tint background.
- In step 2: the **Enquiries** child item shows the same accent + tint and slightly bolder label weight.
- In step 3: the active rail icon shows a small left accent bar and a primary-tinted background.
- In step 4: the popup menu's active item shows the same accent treatment.

**Status:** NOT TESTED

---

## TC-NAV-003: `g` leader-key navigation

**Steps:**
1. With focus **outside** any text field, press **`g`** then **`e`**.
2. Click into the menu search input. Press **`g`** then **`e`**.
3. Wait > 2 seconds after pressing `g`, then press `e`.

**Expected Result:**
- Step 1: routes to `/enquiries`.
- Step 2: nothing happens — typing in input fields must not trigger nav shortcuts.
- Step 3: nothing happens — the `g` leader window expires after 1.5 s.

**Status:** NOT TESTED

---

## TC-NAV-004: `?` opens shortcuts cheat-sheet

**Steps:**
1. Press **`?`** anywhere outside an input field.
2. Press **Esc**.
3. Open the user-avatar menu in the toolbar and click **Keyboard shortcuts**.
4. From the sidenav footer, click the **Shortcuts** button.

**Expected Result:**
- Step 1: a Material dialog titled "Keyboard shortcuts" opens listing `g d`, `g e`, `g a`, `g s`, `g f`, `g p`, `Ctrl/⌘ K`, and `?`.
- Step 2: dialog closes.
- Steps 3 and 4: the same dialog opens.

**Status:** NOT TESTED

---

## TC-NAV-005: Breadcrumb trail visibility

**Steps:**
1. Open `/dashboard`.
2. Open `/enquiries`.
3. Open `/students/1` (any existing student profile).
4. Open `/admissions/new` (focus mode).

**Expected Result:**
- Step 1: no breadcrumb bar (only "Home" → "Dashboard" — single section, hidden).
- Step 2: breadcrumb bar shows `Enquiries` (single segment trail still visible only when > 1 crumb; otherwise hidden — verify this is consistent across the list pages).
- Step 3: breadcrumb bar shows `Students › 1` (or friendly label, fallback to ID).
- Step 4: breadcrumb bar is hidden — focus mode suppresses it.

**Status:** NOT TESTED

---

## TC-NAV-006: Mobile drawer (≤ 767 px)

**Preconditions:** Resize the browser to `≤ 767 px` (e.g. iPhone 12 Pro 390×844).

**Steps:**
1. Observe the toolbar.
2. Tap the **hamburger** icon on the left of the toolbar.
3. Tap any menu link (e.g. **Departments**).
4. Reopen the drawer and tap the **`close`** icon in its header.

**Expected Result:**
- Step 1: hamburger button is visible at the leftmost position; the brand name is hidden; live chip, theme picker, and notifications icon are hidden; only search icon, avatar, and hamburger remain.
- Step 2: drawer slides in over the page content (overlay mode).
- Step 3: app navigates **and** the drawer auto-closes.
- Step 4: drawer closes again.

**Status:** NOT TESTED

---

## TC-NAV-007: Command bar stacking on mobile

**Preconditions:** Mobile viewport, navigate to a list page (e.g. `/students`).

**Steps:**
1. Observe the command bar (search + filters + primary action button).

**Expected Result:**
- The command bar is laid out in a **vertical stack**:
  - Search input is full-width at the top.
  - Filter chips wrap.
  - The primary action button (`+ New Student` etc.) becomes full-width below.

**Status:** NOT TESTED

---

## TC-NAV-008: Tables render as card lists on mobile

**Preconditions:** Mobile viewport, navigate to a list page using `cms-data-table` (e.g. `/departments`).

**Steps:**
1. Observe how each row is rendered.
2. Resize the browser back to desktop (≥ 1024 px).

**Expected Result:**
- Step 1: rows are rendered as **vertical cards** (one per row), with the first column shown as the card title and the remaining columns rendered as `label / value` pairs. Row actions appear at the bottom of each card.
- Step 2: the same data switches back to a tabular layout without page reload.

**Status:** NOT TESTED

---

## TC-NAV-009: Toolbar hides non-essentials on mobile

**Preconditions:** Mobile viewport.

**Steps:**
1. Inspect the toolbar.
2. Tap the search icon.

**Expected Result:**
- Step 1: live chip, theme picker, and notifications button are **not** visible. Only the hamburger, brand icon, search icon, and avatar are visible.
- Step 2: the search pill expands inline to a wider input. Tapping the clear button or losing focus collapses it back to icon-only.

**Status:** NOT TESTED

---

## TC-NAV-010: Focus mode suppresses breadcrumbs and nav shortcuts

**Steps:**
1. Navigate to `/enquiries/new`.
2. Observe the area immediately under the toolbar.
3. Press **`g`** then **`e`** while the form is focused **outside** any input.
4. Press **`?`**.

**Expected Result:**
- Step 2: no breadcrumb bar is rendered.
- Step 3: no navigation occurs — focus-mode suppresses `g`-shortcuts.
- Step 4: the keyboard shortcuts dialog still opens (`?` works in all modes).

**Status:** NOT TESTED
