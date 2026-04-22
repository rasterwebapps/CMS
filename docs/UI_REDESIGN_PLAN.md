# SKSCMS UI/UX Redesign Plan

## Vision

Transform SKSCMS from a functional but dated Material Design 2 application into a modern, premium-feeling nursing college management system. The aesthetic direction: **clean editorial + structured data** — inspired by Linear, Vercel Dashboard, and Notion. Every screen should feel purposeful, fast, and polished.

---

## Design Principles

1. **Information density without clutter** — show more on screen but never feel cramped
2. **Typography as hierarchy** — Sora for UI labels, Instrument Serif for display headings, JetBrains Mono for codes/IDs
3. **Purposeful motion** — micro-animations that confirm state, not decorate
4. **Skeleton-first loading** — never show a spinner where a layout shape would suffice
5. **Zero visual noise** — no unnecessary borders, gradients, or shadows; use space and weight instead

---

## Token System (already defined in `styles.scss`)

All design decisions must use the existing CSS custom property tokens:

| Category | Tokens |
|----------|--------|
| Colors | `--cms-primary`, `--cms-secondary`, `--cms-surface`, `--cms-border`, `--cms-text-*` |
| Typography | `--cms-font-ui` (Sora), `--cms-font-display` (Instrument Serif), `--cms-font-mono` (JetBrains Mono) |
| Spacing | `--cms-space-*` (xs through 3xl) |
| Radius | `--cms-radius-*` (sm through pill) |
| Shadow | `--cms-shadow-*` (sm through xl) |
| Animation | `--cms-duration-*`, `--cms-ease-*` |

---

## Phase 1 — Design System Foundation (Current Sprint)

**Goal:** Establish the baseline so every subsequent phase builds on solid ground.

### 1.1 Fix body font token
- **File:** `frontend/src/styles.scss` line 442
- **Change:** `font-family: 'Inter', system-ui...` → `font-family: var(--cms-font-ui)`
- **Why:** The token `--cms-font-ui` is defined as `'Sora', 'Inter', system-ui` but the body overrides it with a raw value

### 1.2 Convert all legacy forms from `mat-form-field` to custom field system
26 HTML files contain `mat-form-field`. Target conversion pattern (see `department-form.component.html` as the model):

| Legacy | New |
|--------|-----|
| `form-container` wrapper | `entry-form-page` wrapper |
| `mat-card appearance="outlined"` | `div.entry-form-card` |
| `mat-form-field` + `input matInput` | `div.field-group` + `label.field-label` + `input.field-input` |
| `mat-form-field` + `mat-select` | `div.field-group` + `label.field-label` + `select.field-select` + `option` |
| `mat-form-field` + `textarea matInput` | `div.field-group` + `label.field-label` + `textarea.field-textarea` |
| `mat-error` | `p.field-error` |
| `mat-stroked-button` | `button.btn-cancel` |
| `mat-flat-button color="primary"` | `button.btn-submit` |

TypeScript: remove `MatFormFieldModule`, `MatInputModule`, `MatSelectModule`, `MatCardModule` from imports arrays.

**Fully-legacy forms (no entry-form-page at all):**
- `syllabus-form.component.html`
- `maintenance-form.component.html`
- `inventory-form.component.html`
- `lab-schedule-form.component.html`
- `examination-form.component.html`
- `co-po-mapping-form.component.html`
- `experiment-form.component.html`

**Mixed forms (entry-form-page but mat-form-field inside):**
- `admission-form.component.html`
- `enquiry-form.component.html`
- `enquiry-convert.component.html`
- `fee-finalization-form.component.html`
- `fee-payment-form.component.html`
- `collect-payment-dialog.component.html`
- Plus others in finance, faculty, department areas

### 1.3 Build `<cms-data-table>` shared component
A typed wrapper around `mat-table` that provides:
- Built-in skeleton loading state (shimmer rows while loading)
- Empty state slot (icon + message when no data)
- Sticky header
- Responsive: scrollable horizontally on small viewports
- Accepts `columns: ColumnDef[]`, `data: T[]`, `loading: boolean`

### 1.4 Build shared utility components

**`CmsSkeletonComponent`** (`shared/skeleton/`)
- Input: `lines?: number`, `height?: string`, `circle?: boolean`
- CSS shimmer animation using `--cms-primary` tint

**`CmsEmptyStateComponent`** (`shared/empty-state/`)
- Inputs: `icon`, `title`, `subtitle`, `actionLabel`, `actionClick` event
- Used on every list page when result set is empty

**`CmsStatusBadgeComponent`** (`shared/status-badge/`)
- Maps `EnquiryStatus` / `AdmissionStatus` etc. to color tokens
- Replaces all inline `<span class="status-*">` usages

---

## Phase 2 — List Screens Redesign

**Goal:** Every list/table screen gets a consistent command-bar layout.

### Command-bar pattern (`.command-bar`)
Already defined in `styles.scss`. Contains:
- Left: page title + record count badge
- Center: search input (`.search-bar`) — debounced, highlights matches
- Right: filter chips + primary action button

### Table upgrade
- Replace `<mat-table>` with `<cms-data-table>` across all list screens
- Add column visibility toggle (dropdown, persisted in `localStorage`)
- Row hover: subtle `--cms-primary-tint` background
- Clickable rows navigate to detail (remove separate action icon columns)
- Batch selection for bulk operations (delete, export)

### Screens to convert:
- Enquiry List, Admission List, Student List
- Faculty List, Department List, Course List, Program List
- Lab List, Lab Schedule List
- Inventory List, Equipment List
- Examination List, Exam Result List
- Maintenance List, Fee Structure List
- Syllabus List, Co-PO Mapping List

---

## Phase 3 — Detail / Profile Screens

**Goal:** Rich context panels that feel like a product, not a CRUD form.

### Enquiry Detail (flagship screen)
- Split-pane layout: left narrow sidebar (status timeline, quick stats) + right main content tabs
- Tabs: Overview | Payments | Documents | Status History
- Status timeline: vertical stepper with `Instrument Serif` labels
- Payment cards: receipt-style layout with subtle border-left accent color
- Document checklist: check/cross icons with upload affordance inline

### Student Profile
- Hero section: avatar (initials fallback), name in `Instrument Serif`, program + batch badges
- Stats row: Attendance %, Fee Outstanding, Pending Documents, Exam Results
- Tabbed sections: Academic | Finance | Documents | Contact

### Faculty Profile
- Similar hero + stats pattern
- Assigned courses, lab slots, timetable preview

---

## Phase 4 — Dashboard Redesign

**Goal:** Role-aware dashboards that surface actionable data at a glance.

### Admin Dashboard
- Metric cards row: Total Enquiries, Admissions This Month, Fee Collection, Pending Documents
- Enquiry funnel chart (Recharts or ng2-charts): ENQUIRED → FEES_FINALIZED → FEES_PAID → ADMITTED
- Recent activity feed (right sidebar): last 10 status changes with timestamps
- Quick actions: New Enquiry, Collect Payment, Upload Document

### Front Office Dashboard
- Today's tasks: enquiries needing follow-up, documents pending
- Walk-in registration shortcut (prominent CTA)

### Faculty Dashboard  
- My classes today (timetable-style cards)
- Pending attendance submissions
- Lab schedule

---

## Phase 5 — Navigation & Shell Redesign

**Goal:** Faster navigation, better information architecture.

### Sidenav upgrades
- Collapsible to icon-only rail (64px) — expands on hover or pin toggle
- Section headers with subtle dividers, not bold text blocks
- Active item: primary color left border + background tint
- Keyboard navigation: `k` to open global search, `g` + letter for nav shortcuts

### Breadcrumb
- Add breadcrumb trail to all nested pages
- Format: `Home / Enquiries / Ravi Kumar / Payments`

### Mobile responsiveness
- Sidenav becomes bottom sheet on `< 768px`
- Command bars stack vertically
- Tables become card lists on mobile

---

## Phase 6 — Polish & Micro-interactions

**Goal:** The details that make it feel premium.

### Transitions
- Route transitions: fade-slide using Angular animations
- Dialog open/close: scale + fade (already partially in `styles.scss`)
- Tab switches: slide indicator animation

### Toast/Snackbar redesign
- Replace `MatSnackBar` with custom positioned toasts (top-right)
- Types: success (green left border), error (red), warning (amber), info (blue)
- Auto-dismiss with progress bar
- Stack multiple toasts

### Form feedback
- Inline validation with animated error messages (slide down)
- Success state on submit: checkmark animation before navigate-away
- Loading state: button shows spinner + disables

### Print/export
- Receipt print styles (already referenced in code)
- CSV export button on all list screens
- PDF export for fee receipts using browser print API

---

## Implementation Priority Order

```
Phase 1.1  → Fix font token                          [30 min]
Phase 1.2  → Convert 7 fully-legacy forms            [4 hours]
Phase 1.2  → Convert mixed forms                     [4 hours]  
Phase 1.3  → cms-data-table component                [3 hours]
Phase 1.4  → skeleton, empty-state, status-badge     [2 hours]
Phase 2    → List screens with new table component   [6 hours]
Phase 3    → Enquiry detail + Student profile        [8 hours]
Phase 4    → Dashboard cards + charts                [6 hours]
Phase 5    → Nav rail + breadcrumbs                  [4 hours]
Phase 6    → Polish pass                             [4 hours]
```

Total estimated: ~41 hours of focused frontend work.

---

## Reference Components

- `department-form.component.html` — canonical form pattern (field-group/field-label/field-input)
- `enquiry-detail.component.html` — tab pattern, payment section
- `department-list.component.html` — list + modern-table pattern
- `page-header.component.ts` — existing shared header component
- `styles.scss` — all tokens, utilities, component classes

---

## Non-Goals

- Do NOT change the Angular routing structure or lazy-load boundaries
- Do NOT replace Angular Material completely — keep `mat-dialog`, `mat-snackbar`, `mat-checkbox`, `mat-icon`, `mat-button` for interactive behavior; only remove form field wrappers
- Do NOT introduce new third-party UI libraries — use the token system
- Do NOT change backend APIs