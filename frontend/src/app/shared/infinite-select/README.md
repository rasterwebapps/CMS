# `cms-infinite-select`

A searchable, infinite-scroll dropdown for any filter whose options come from the database,
replacing a plain `<select>` (or a bespoke checkbox panel) that used to eagerly load — or worse,
silently cap — its full option list.

## Why this exists

`fee-explorer` originally derived its filter dropdowns from whatever page of the main table
happened to be loaded (25 rows), so a value only present on page 2+ could never be picked. Once
fixed, an audit of the rest of the app found the same *shape* of risk one level removed:
`enquiry-list`'s Referral Type and Agent filters fetched up to 1,000 rows in one request as a
stand-in for "all of them" — fine today, but silently truncates the moment either list passes
1,000 rows, with no error or indication to the user. This component is the general fix: every
DB-backed filter dropdown should page its options 10 at a time via the backend's existing
`getPage({ search, page, size })` convention, so there is no size a master list can grow to that
this dropdown silently mishandles.

## Usage

Single-select, id-valued (the emitted value is a numeric/opaque id — needs `resolveLabel` to show
an already-selected value's name before its page has loaded, e.g. one restored from the URL):

```html
<cms-infinite-select
  label="All Programs" ariaLabel="Filter by program"
  [fetchPage]="programFetchPage" [resolveLabel]="programResolveLabel"
  [selectedValue]="selectedProgramId()" (selectedValueChange)="onProgramChange($event)"
/>
```
```ts
protected readonly programFetchPage = (search: string, page: number, size: number) =>
  this.programService.getPage({ search, page, size });
protected readonly programResolveLabel = (id: InfiniteSelectValue) =>
  this.programService.getById(Number(id)).pipe(map(p => p.name));
```

Single-select, name-valued (the value already IS the display name — omit `resolveLabel`, the
component falls back to showing the raw value):

```html
<cms-infinite-select
  label="All Agents" ariaLabel="Filter by agent" valueKey="name"
  [fetchPage]="agentFetchPage"
  [selectedValue]="selectedAgent()" (selectedValueChange)="onAgentChange($event)"
/>
```

Multi-select — the host owns its own "N selected" summary logic and passes it as `label` on every
change (this component only ever displays whatever `label` it's given in multi mode):

```html
<cms-infinite-select
  [multiple]="true" [label]="academicYearFilterLabel()" ariaLabel="Filter by academic year"
  [fetchPage]="academicYearFetchPage"
  [selectedValues]="selectedAcademicYearIds()" (selectedValuesChange)="onAcademicYearsChange($event)"
  [showClear]="selectedAcademicYearIds().size > 0" clearLabel="Show all years" (cleared)="clearAcademicYears()"
/>
```

Dependent filters (e.g. Course scoped to a selected Program) — bump `reloadKey` with whatever the
dependency's current value is; the picker drops its cached pages and, if currently open,
re-fetches immediately:

```html
<cms-infinite-select ... [fetchPage]="courseFetchPage" [reloadKey]="selectedProgramId()" ... />
```
```ts
protected readonly courseFetchPage = (search: string, page: number, size: number) =>
  this.courseService.getPage({ search, page, size, programId: this.selectedProgramId() ?? undefined });
```

## Mechanics

- Nothing is fetched until the dropdown is opened for the first time — matches a native
  `<select>`'s lazy behavior, unlike the `getAll()`/large-page calls this replaces.
- Typing in the built-in search box debounces 300ms, then restarts pagination from page 0 with
  the new term. Opening for the first time (or a `reloadKey` change while open) fetches
  immediately — no artificial delay when nothing was typed.
- Scrolling the option list past ~80% of its scroll height requests the next page and appends it,
  guarded against a second request firing while one is already in flight or after the last page
  has been reached (`totalElements` exhausted).
- `fetchPage`/`resolveLabel` must be stable function references (class fields, not inline
  `.bind()`/arrow expressions in the template) — a new function identity every change-detection
  cycle would be harmless here (the component doesn't diff them), but keeping them as fields
  matches this codebase's existing convention for passing callbacks into child components.
- Visual style is a self-contained copy of this app's existing filter-bar look (`enq-filter-select`
  / `status-drop-btn` / `status-drop-panel` in `enquiry-list.component.scss`) — Angular's emulated
  `ViewEncapsulation` means a shared component can't extend another component's local classes, so
  this component's `.scss` re-declares the same visual language against the same global
  `--cms-*` custom properties rather than depending on those other files.

## Known gaps

- No per-option decoration slot (e.g. the old Academic Year panel's "Current" badge next to the
  active year) — if a future adopter needs that, extend the template rather than re-forking a
  parallel dropdown implementation.
- Multi-select relies on the host to resolve names for values selected before their page has ever
  loaded (there's no per-value `resolveLabel` in multi mode, only single-select's). `enquiry-list`
  works around this by keeping its own small, already-existing full academic-year list just for
  computing the button's summary label and its "current + next" default selection — reasonable
  since a college has at most a few dozen academic years ever, unlike the genuinely unbounded
  Program/Course/Referral Type/Agent lists this component's pagination is actually protecting
  against.
