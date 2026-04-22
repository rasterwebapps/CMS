# Phase 4 — Dashboard Redesign: End-to-End Instructions

## Current State Audit (read before starting)

The Admin and Front Office dashboards are already substantially built. Do not rebuild what exists.
Read each of the following files in full before writing a single line:

```
dashboard/admin/admin-dashboard.component.html / .ts / .scss
dashboard/front-office/front-office-dashboard.component.html / .ts / .scss
dashboard/shared/kpi-card/dashboard-kpi-card.component.html / .ts / .scss
dashboard/shared/tabs/dashboard-tabs.component.html / .ts
dashboard/_dashboard-tokens.scss
dashboard/dashboard.models.ts
dashboard/dashboard.component.html / .ts          ← the generic role-router shell
reports/reports-dashboard/reports-dashboard.component.html / .ts
```

### What already exists (do NOT recreate)

| Feature | Location |
|---|---|
| Hero banner with greeting | Admin + FO dashboards |
| KPI card row (5 cards) with skeleton loading | Both dashboards via `DashboardKpiCardComponent` |
| `DashboardKpiCardComponent` with `trend` input | `shared/kpi-card/` — input exists but is never passed by parents |
| Admission trend bar chart (CSS bars) | Admin dashboard |
| Enquiry funnel (CSS horizontal bars) | Admin + FO dashboards |
| Pending approvals panel | Admin dashboard |
| Today's enquiries list + pending actions | FO dashboard |
| Quick action CTA buttons | Both dashboards |
| Breakdown analytics (4 cards) | Admin dashboard |
| 6-month trends section (2 charts) | Admin dashboard |
| `_dashboard-tokens.scss` with role colour palettes | Shared |

### What is missing (Phase 4 delivers these)

| Feature | Scope |
|---|---|
| Recent activity feed (last 10 status changes) | Admin dashboard |
| KPI trend arrows wired to real data | Both dashboards |
| Academic calendar connected to live API | Admin dashboard |
| Walk-in registration as a visually dominant CTA | FO dashboard |
| Faculty Dashboard component | New — does not exist |
| Faculty: My classes today (timetable cards) | Faculty dashboard |
| Faculty: Pending attendance submissions | Faculty dashboard |
| Faculty: Lab schedule widget | Faculty dashboard |

---

## Step 0 — Backend APIs to confirm before writing frontend code

Phase 4 requires three API endpoints that may not exist yet. Confirm each with the backend team
or check the Spring Boot controllers before proceeding. If an endpoint is missing, add a TODO
note in the component and render an empty-state placeholder — do not block frontend work.

### 0.1 Recent Activity endpoint

```
GET /api/dashboard/activity?limit=10
```

Expected response shape (add to `dashboard.models.ts`):
```
ActivityItem {
  id: number
  entityType: 'ENQUIRY' | 'ADMISSION' | 'STUDENT' | 'PAYMENT' | 'DOCUMENT'
  entityId: number
  action: string          // e.g. "Status changed to FEES_PAID"
  actor: string           // username who triggered the action
  timestamp: string       // ISO-8601
  linkPath: string        // e.g. "/enquiries/42" — for the "View" link
}
```

If this endpoint doesn't exist, the backend needs to pull from the `enquiry_status_history`
table (which already records every status change) and potentially from a general audit log.
Do NOT use a polling approach — load once on `ngOnInit` and refresh only on manual trigger.

### 0.2 Academic Calendar endpoint

```
GET /api/academic-years/current/events
```

Expected response shape (add to `dashboard.models.ts`):
```
CalendarEvent {
  id: number
  label: string
  date: string            // ISO date string YYYY-MM-DD
  type: 'EXAM' | 'HOLIDAY' | 'EVENT' | 'DEADLINE' | 'OTHER'
}
```

The admin dashboard currently has a hardcoded `calendarPills` array with a `// TODO` comment.
This step replaces it with live data.

### 0.3 Faculty Dashboard endpoint

```
GET /api/dashboard/faculty
```

Expected response shape (add to `dashboard.models.ts`):
```
FacultyDashboard {
  todayClasses: FacultyClass[]
  pendingAttendanceCount: number
  pendingAttendanceSessions: AttendanceSession[]
  upcomingLabSlots: LabSlot[]
  assignedCourseCount: number
  totalStudentCount: number
}

FacultyClass {
  courseCode: string
  courseName: string
  batch: string           // e.g. "2023-24 Sem 2"
  startTime: string       // "HH:MM"
  endTime: string
  room: string
  attendanceMarked: boolean
}

AttendanceSession {
  id: number
  courseCode: string
  courseName: string
  date: string
  studentCount: number
  linkPath: string        // "/attendance/mark?courseId=X"
}

LabSlot {
  labName: string
  date: string
  startTime: string
  endTime: string
  batch: string
}
```

---

## Step 1 — Update `dashboard.models.ts`

Add the three new interfaces (`ActivityItem`, `CalendarEvent`, `FacultyDashboard`,
`FacultyClass`, `AttendanceSession`, `LabSlot`) confirmed from Step 0 to the existing
`dashboard.models.ts` file. Do not modify any existing interfaces.

Also add a `KpiTrendDirection` type if it doesn't already exist, and extend `DashboardSummary`
with any new fields the backend team confirms are available:
- `totalEnquiriesThisMonth: number`
- `admissionsThisMonth: number`
- `pendingDocumentsCount: number`
- Month-over-month delta fields for each KPI: `studentsDelta`, `feeDelta`, etc.

The delta fields power the trend arrows on KPI cards (the `trend` input of
`DashboardKpiCardComponent` is already implemented but never used).

---

## Step 2 — Wire KPI trend arrows (both dashboards)

### What to do

The `DashboardKpiCardComponent` already has a `trend` input of type `KpiTrend`:
```
KpiTrend { direction: 'up' | 'down' | 'neutral'; label: string }
```

It renders a `↑` or `↓` arrow with a label, but no parent currently passes this input.

### Admin Dashboard

In `admin-dashboard.component.ts`, update the `kpiCards` computed signal. For each card,
add a `trend` field computed from the delta fields in `DashboardSummary` (Step 1):

- Students KPI: `trend = { direction: delta > 0 ? 'up' : 'down', label: '+N this month' }`
- Fee Collected KPI: compare `feeCollectedThisMonth` to last month's value if available
- Fee Outstanding KPI: `direction: 'down'` is good (outstanding going down = positive)

In the template, pass `[trend]="card.trend"` wherever the trend field is non-null.

### Front Office Dashboard

Same approach in `front-office-dashboard.component.ts`. The conversion rate card is the most
meaningful one for trend — show whether this week's rate is above or below last week.

### Colour convention

- `direction: 'up'` on a positive metric (enrolments, collections) → green
- `direction: 'down'` on a cost/risk metric (outstanding, pending) → also green (downward is good)
- Always pair the arrow with a human label, never just the raw delta number

---

## Step 3 — Admin Dashboard: Recent Activity Feed

### 3.1 Add state and API call

In `admin-dashboard.component.ts`, add:
- `activityLoading = signal(true)`
- `activityFeed = signal<ActivityItem[]>([])`
- In `ngOnInit`, make a third HTTP GET to `/api/dashboard/activity?limit=10`

### 3.2 HTML layout

Place the activity feed as a **third column** in the existing `admin-two-col` layout.
Change the layout from a two-column section to a three-column section:
`admin-three-col` (proportions: `1fr 1fr 320px`) where the rightmost column is the feed.

If adding a third column breaks the responsive layout, keep it as a separate full-width
section below the two-column row instead.

The feed widget structure:
```
div.content-card.widget-card
  div.widget-header
    icon + "Recent Activity" title
    button.btn-toolbar (click)="refreshActivity()"  ← small "Refresh" text button
  div.widget-card__body
    div.activity-feed
      @for (item of activityFeed(); track item.id)
        div.activity-item
          div.activity-item__icon     ← entity-type icon (SVG, not mat-icon)
          div.activity-item__body
            p.activity-item__action   ← item.action text
            p.activity-item__meta     ← "by {actor} · {relative time}"
          a.activity-item__link       ← "View →" using item.linkPath
```

### 3.3 Relative timestamps

Do not use `DatePipe` for the activity feed timestamps. Write a `relativeTime(iso: string): string`
method in the component that returns human strings like "2 minutes ago", "1 hour ago",
"Yesterday". This is the only place in the app that needs relative time — do not create a shared
pipe unless a second screen requires it.

Formula:
```
const diff = Date.now() - Date.parse(iso);   // milliseconds
if diff < 60_000     → "Just now"
if diff < 3_600_000  → "N minutes ago"
if diff < 86_400_000 → "N hours ago"
else                 → DatePipe 'mediumDate'
```

### 3.4 Skeleton for activity feed

While `activityLoading()` is true, render 5 skeleton rows using the existing
`<app-skeleton [lines]="1" height="14px">` component inside the `activity-feed` container.
Each skeleton row should be approximately `40px` tall with a circle on the left (using the
`[circle]` input at `24px`).

---

## Step 4 — Admin Dashboard: Live Academic Calendar

### 4.1 Replace the hardcoded calendarPills

Remove the `protected readonly calendarPills = [...]` array from `admin-dashboard.component.ts`
and replace it with:
- `calendarLoading = signal(false)`
- `calendarEvents = signal<CalendarEvent[]>([])`
- HTTP GET to `/api/academic-years/current/events` in `ngOnInit`

### 4.2 Calendar pill rendering

The existing `.admin-calendar-strip` / `.admin-calendar-pill` CSS classes should remain.
Map `CalendarEvent.type` to a `data-color` attribute value:
```
EXAM     → 'amber'
HOLIDAY  → 'blue'
DEADLINE → 'red'
EVENT    → 'violet'
OTHER    → 'gray'
```

Format the `date` field using `DatePipe` with `'d MMM'` format (e.g. "14 Mar").
Show the date relative to today: if the event is today, show "Today" instead of the date.

### 4.3 Empty state

If `calendarEvents()` is empty (API returned no events, or endpoint doesn't exist yet),
render a single pill with text "No upcoming events" in gray. Do not show an error.

---

## Step 5 — Front Office Dashboard: Walk-in Registration CTA

### 5.1 Problem with current state

The current FO dashboard has three equal-weight CTA buttons: New Enquiry, New Admission,
Collect Payment. The "walk-in registration" concept (student walks in, staff creates an enquiry
immediately) maps to "New Enquiry" — but it's visually indistinguishable from the other two.

### 5.2 What to change

Make the "New Enquiry" CTA the **primary dominant action**:
- Full width on mobile, at least `2x` the visual weight of the others
- Add a short subtitle text: "Walk-in · Quick registration"
- Change its class from `fo-cta fo-cta--sky` to `fo-cta fo-cta--primary fo-cta--hero`
- The other two CTAs (New Admission, Collect Payment) become secondary: smaller, border-only style

In the SCSS, add `.fo-cta--hero`:
- `padding: 20px 28px` (vs the current uniform padding)
- `font-size: 1rem` (vs current smaller size)
- A subtitle line below the label: `font-size: 0.75rem; opacity: 0.75`

No TypeScript changes needed — this is purely HTML and SCSS.

### 5.3 Add a "Today's Summary" strip above the CTAs

Insert a one-liner strip immediately below the FO hero and above the CTAs. It is already
partially rendered inside the hero's `fo-hero__stats` paragraph. Extract it into its own
`.fo-today-strip` element so it can be styled independently:

```
div.fo-today-strip
  span.fo-today-stat  "{{ foData()?.todayEnquiryCount }} enquiries today"
  span.fo-today-divider
  span.fo-today-stat  "₹{{ foData()?.feeCollectedToday }} collected"
  span.fo-today-stat  "{{ foData()?.pendingActionItems.length }} pending"
```

This data is already loaded — no new API call needed.

---

## Step 6 — Faculty Dashboard (New Component)

The faculty dashboard does not exist yet. This is a new standalone Angular component.

### 6.1 Create the component

Create the following files:
```
dashboard/faculty/faculty-dashboard.component.ts
dashboard/faculty/faculty-dashboard.component.html
dashboard/faculty/faculty-dashboard.component.scss
```

Register it in the Angular router under the faculty role guard. Check the existing
`dashboard.component.ts` to understand how the role-based routing works — the generic dashboard
shell routes to `admin-dashboard` or `front-office-dashboard` based on the authenticated role.
Add the faculty case in the same switch and register the route in `app.routes.ts`.

### 6.2 Component TypeScript

The component should:
- Inject `HttpClient`, `AuthService`
- Declare signals: `loading`, `facultyData` (`FacultyDashboard | null`), `today` (formatted date string)
- On `ngOnInit`: GET `/api/dashboard/faculty`, set `facultyData`, set `loading(false)` on both success and error
- No computed signals needed at first — derive everything in the template

### 6.3 Hero strip (no hero component — inline HTML)

Do not use `app-page-header`. Render a compact strip at the top:
```
div.faculty-hero
  div.faculty-hero__greeting
    p  "Good morning, {username}"  (use AuthService.username())
    p  "{ today }"                 (e.g. "Wednesday, 16 April 2025")
  div.faculty-hero__stats
    span  "{ assignedCourseCount } courses assigned"
    span  "{ totalStudentCount } students"
```

### 6.4 Today's Classes widget

Render as a `content-card widget-card` with the title "My Classes Today".

If `todayClasses` is empty, show an empty state: "No classes scheduled for today."

For each class in `todayClasses`, render a `.class-card`:
```
div.class-card  [class.class-card--done]="item.attendanceMarked"
  div.class-card__time     "{ startTime } – { endTime }"   (mono font)
  div.class-card__info
    p.class-card__name     item.courseName
    p.class-card__meta     "{ courseCode }  ·  { batch }  ·  { room }"
  div.class-card__status
    If attendanceMarked:
      span.cms-badge.cms-badge--blue  "Marked"
    Else:
      a.btn-sm-primary  [routerLink]="['/attendance/mark']" [queryParams]="{courseId: ...}"
                        "Mark Attendance"
```

The `.class-card--done` modifier uses opacity: 0.6 and a lighter border to visually de-emphasise
already-completed classes.

### 6.5 Pending Attendance widget

Render as a `content-card widget-card` with the title "Pending Attendance
({{ facultyData()?.pendingAttendanceCount }})".

If `pendingAttendanceSessions` is empty, show an all-clear empty state with a checkmark SVG.

For each session, render a compact `.pending-row`:
```
div.pending-row
  div.pending-row__info
    span.pending-row__course    item.courseName
    span.pending-row__date      item.date (DatePipe 'mediumDate')
    span.pending-row__students  "{ studentCount } students"
  a.btn-sm-secondary  [routerLink]="item.linkPath"   "Mark →"
```

### 6.6 Upcoming Lab Slots widget

Render as a `content-card widget-card` with the title "Lab Schedule".

If `upcomingLabSlots` is empty: empty state "No lab slots scheduled."

For each slot, a simple row:
```
div.lab-slot-row
  span.lab-slot-row__date   item.date (DatePipe 'EEE d MMM')
  span.lab-slot-row__time   "{ startTime }–{ endTime }"  (mono font)
  span.lab-slot-row__lab    item.labName
  span.lab-slot-row__batch  item.batch
```

### 6.7 Layout

Use a two-column grid for the faculty dashboard:
- Left (narrow, `340px`): Pending Attendance + Lab Schedule stacked
- Right (wide, `1fr`): Today's Classes (full height)

On screens narrower than `800px`: single column, Today's Classes first.

---

## Step 7 — SCSS additions

All new CSS for Phase 4 goes in the **component-level SCSS files**, not `styles.scss`.
The global token system is already defined. New classes are:

### Admin dashboard SCSS additions (`admin-dashboard.component.scss`)
- `.activity-feed`, `.activity-item`, `.activity-item__icon`, `.activity-item__body`
- `.activity-item__action`, `.activity-item__meta`, `.activity-item__link`
- `.admin-three-col` (if using a three-column layout for activity feed)
- Tweak `.admin-two-col--60-40` → `admin-two-col--40-40-20` if activity goes in a third column

### Front Office dashboard SCSS additions (`front-office-dashboard.component.scss`)
- `.fo-cta--hero` (dominant walk-in CTA)
- `.fo-cta--secondary` (smaller CTAs)
- `.fo-today-strip`, `.fo-today-stat`, `.fo-today-divider`

### Faculty dashboard SCSS (new file: `faculty-dashboard.component.scss`)
- `.faculty-hero`, `.faculty-hero__greeting`, `.faculty-hero__stats`
- `.class-card`, `.class-card--done`, `.class-card__time`, `.class-card__info`
- `.class-card__name`, `.class-card__meta`, `.class-card__status`
- `.pending-row`, `.pending-row__info`, `.pending-row__course`, `.pending-row__date`
- `.lab-slot-row` and its child elements
- Two-column grid for the faculty dashboard layout

---

## Step 8 — Router wiring for Faculty Dashboard

### 8.1 Check the existing role router

Open `dashboard/dashboard.component.ts`. It uses `AuthService` to determine the current role
and renders either `<app-admin-dashboard>` or `<app-front-office-dashboard>`. Add the faculty
case here:
```
@if (authService.isFaculty()) {
  <app-faculty-dashboard />
} @else if (authService.isAdmin()) {
  <app-admin-dashboard />
} @else {
  <app-front-office-dashboard />
}
```

Check whether `isFaculty()` already exists on `AuthService`. If not, add it: it should check
the `role` from the JWT claim, exactly like `isAdmin()` and `isFrontOffice()` do.

### 8.2 Import the new component

In `dashboard.component.ts`, add `FacultyDashboardComponent` to the `imports` array of the
standalone component decorator.

---

## Step 9 — Implementation Order

Execute in this sequence to avoid blocked states:

```
9.1  Confirm all three backend API endpoints (Step 0) — block nothing, use empty-state stubs
9.2  Update dashboard.models.ts (Step 1) — foundation for all subsequent steps
9.3  Wire KPI trend arrows — Admin dashboard first, then FO (Step 2)
9.4  Add recent activity feed to Admin dashboard (Step 3)
9.5  Replace hardcoded calendar with live API (Step 4)
9.6  Build — verify zero errors
9.7  FO walk-in CTA visual dominance (Step 5) — pure HTML/SCSS, no TS changes
9.8  Create Faculty Dashboard component shell + router wiring (Steps 6 + 8)
9.9  Faculty: Today's Classes widget (Step 6.4)
9.10 Faculty: Pending Attendance widget (Step 6.5)
9.11 Faculty: Lab Slots widget (Step 6.6)
9.12 All SCSS additions (Step 7) — do alongside each feature, not all at the end
9.13 Final build + visual QA on all three dashboards
```

---

## Skills Required

| Area | What you need to know |
|---|---|
| **Angular Signals** | `signal()` + `computed()` for reactive state. All async data loaded via HTTP goes into a signal. All derived values (KPI trends, funnel percentages) use `computed()`. |
| **Angular `input()` signal API** | `DashboardKpiCardComponent` uses the new `input.required<T>()` / `input<T>()` API, not `@Input()`. When passing trend data, use `[trend]="card.trend"` — the type must be `KpiTrend \| undefined`, not null. |
| **Angular `HttpClient` with signals** | Use `.subscribe({ next, error })` pattern already used throughout. Do not use `toSignal(httpClient.get(...))` unless you are comfortable with the `takeUntilDestroyed` lifecycle requirement. |
| **CSS Grid responsive layout** | The faculty dashboard needs a two-column grid that collapses to one column. Use `grid-template-columns: 340px 1fr` with a `@media (max-width: 800px)` override to `1fr`. |
| **Relative time without a library** | Write `relativeTime(iso: string): string` in the admin dashboard component. Pure arithmetic on `Date.now() - Date.parse(iso)`. No `moment.js` or `date-fns` — they are not in the project. |
| **Angular router with query params** | The "Mark Attendance" button in the faculty dashboard links to `/attendance/mark` with `[queryParams]="{ courseId: X }"`. Confirm the attendance-mark route accepts this query param. |
| **`AuthService.isFaculty()`** | May need to be added. Pattern to follow: look at how `isAdmin()` and `isFrontOffice()` are implemented — they read the role claim from the stored JWT. |
| **Angular standalone component imports** | The faculty dashboard is a new standalone component. Its `imports` array needs: `RouterLink`, `DecimalPipe`, `DatePipe`, `MatIconModule`, `DashboardKpiCardComponent`. Add only what the template actually uses. |
| **`DatePipe` in template** | The class cards and lab slots use `DatePipe`. Import `DatePipe` in the component's `imports` array (not in a module). Use `date:'mediumDate'` for standard dates and `date:'EEE d MMM'` for the short weekday format in lab slots. |
| **SCSS `data-*` attribute selectors** | The existing dashboard uses `[attr.data-status]` and `[attr.data-color]` patterns for dynamic colour application via CSS `[data-status="ENQUIRED"] { ... }`. Reuse this pattern for the activity feed entity-type icon colouring. |
| **`@empty` block in `@for`** | Angular 17+ `@for` supports `@empty` as the empty-state block. Use it wherever a list may be empty — do not use a separate `@if (items.length === 0)`. |

---

## Non-Goals for Phase 4

- Do NOT add a charting library (Recharts, ng2-charts, Chart.js). The custom CSS bar charts
  already work and are lightweight. A charting library adds bundle weight and is unnecessary
  unless the product explicitly asks for interactive tooltips or pie charts.
- Do NOT add WebSocket / real-time updates. Load once on `ngOnInit` and add a manual refresh
  button on the activity feed. Polling timers are banned — they cause unnecessary load.
- Do NOT redesign the `DashboardKpiCardComponent` internals. It is already well-structured.
  Only pass new inputs that already exist (`trend`, `danger`).
- Do NOT build a Faculty Dashboard if the backend `/api/dashboard/faculty` endpoint does not
  exist. Create the component shell with a full-page empty state:
  "Faculty dashboard data not yet available." and wire the route — that is enough for Phase 4.
  The widgets are additive once the API exists.
- Do NOT touch the Reports Dashboard (`reports-dashboard.component`). It belongs to a separate
  analytics sprint.
