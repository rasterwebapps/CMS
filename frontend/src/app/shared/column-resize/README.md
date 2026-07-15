# Resizable / Wrap-Text Columns — Implementation Guide

This is the shared mechanism behind drag-to-resize columns, double-click
auto-fit, Excel-style Wrap Text, and hover tooltips on truncated cells,
already live on all `ColumnPickerState` list screens via `[cmsResizableColumns]`.

**The mechanism itself needs zero per-screen code.** Everything in this doc
past step 1 is either already true for every screen automatically, or is a
CSS requirement on how a screen authors its *own* cell markup — the second
category is the only place a new screen can still reintroduce a bug that was
already found and fixed once here. Read the whole "Per-screen requirements"
section before adding a multi-line or custom-font cell to any resizable table.

## 1. Wiring a new screen (the only per-screen code required)

```html
<cms-wrap-text-toggle [state]="colState" />
<cms-column-picker [state]="colState" />
...
<table mat-table [dataSource]="dataSource" matSort [cmsResizableColumns]="colState" ...>
```

`colState` is the screen's own `ColumnPickerState` instance (same one driving
column order/visibility/pin). That's it — resize, auto-fit, wrap, and
tooltips all come from this one binding. Do not add per-column resize
handles, width bindings, or tooltip directives manually.

## 2. What "Excel-like" means here (intentional design decisions)

- **Wrap Text is independent of column width — it never resizes anything.**
  Toggling it only flips `white-space`/`word-break`/`overflow` within
  whatever width a column currently has, exactly like Excel's own Wrap Text.
  If a column is already wide enough to show its content on one line,
  toggling wrap on does nothing visible — that's correct, not a bug.
  An earlier iteration of this feature made wrap auto-cap/auto-fit column
  widths on toggle; that was deliberately reverted because it doesn't match
  real Excel and surprised users by moving columns out from under them.
- **Width only ever changes via drag or double-click auto-fit**, both
  independent of the wrap toggle.
- **Double-click auto-fit** (`autoFit()`) snaps a column to fit its content
  on one line — same semantics as Excel's own double-click-the-border
  behavior. It is idempotent: clicking again recomputes the same value, it
  does not toggle or shrink back.
- **Unresized columns stay on natural CSS `auto` sizing** and grow to fit
  their content, with `.table-wrapper`'s `overflow-x: auto` handling
  anything wider than the viewport via horizontal scroll — columns are never
  force-squeezed to fit. Only a column the user has actually resized (drag
  or auto-fit) gets a pinned width.

## 3. Per-screen requirements for cell markup (read before adding a new cell type)

The directive's ellipsis-truncation CSS and its auto-fit text measurement
both work by finding **childless leaf `div`/`span` elements** inside a cell
(`:not(:has(*))` in CSS, `el.children.length === 0` in JS) and treating each
one as an independent single line of text. Two markup patterns break this if
you don't follow them:

### a) Flex containers wrapping multi-line text need `min-width: 0` on the text wrapper

If a cell's stacked title/subtitle content sits inside a `display: flex`
container (e.g. an avatar + name/email block), the flex child wrapping the
text **must** have `min-width: 0`:

```scss
.name-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  > div {
    min-width: 0; // required — see explanation below
  }
}
```

**Why:** flex items default to `min-width: auto`, which refuses to shrink a
flex child below its own content's natural (nowrap) width. Without this, the
text visually overflows past the `<td>`'s real, correctly-sized box —
completely bypassing the ellipsis/ellipsis-tooltip system even though the
column's actual measured width (what drag/resize math uses) stays correct.
This is silent and easy to miss: the column *looks* broken, but the bug is
in your cell's own SCSS, not the shared directive.

Found and fixed across every screen that had it (2026-07-15) — the class
holding the text is either applied directly as the flex item, or wraps it in
an inner `<div>`/named class first, depending on the screen:

| File | Flex container | Fix applied to |
|---|---|---|
| `enquiry-list.component.scss` | `.name-cell` | anonymous `> div` wrapper |
| `fee-explorer.component.scss` | `.student-cell` | `.cell-name` (direct child, no wrapper) |
| `admission-list.component.scss` | `.student-cell` | `.cell-name` (direct child, no wrapper) |
| `admission-completion-list.component.scss` | `.student-cell` | `.student-info` wrapper |
| `document-verification-list.component.scss` | `.student-cell` | `.student-info` wrapper |
| `document-submission-list.component.scss` | `.student-cell` | already had it — no change needed |
| `student-list.component.scss` | `.student-cell` | `.student-info` wrapper |
| `fee-refund-list.component.scss` | `.rfl-student-cell` | container itself, defensively (column-direction flex, not confirmed vulnerable to the same bug, but harmless) |

Swept every screen using `[cmsResizableColumns]` for this pattern — nothing
else matched. If a *new* screen adds a similar avatar+stacked-text cell,
apply the same fix at build time, not after a bug report.

### b) Every distinct text line needs its own leaf element

```html
<!-- Correct — two leaves, each gets its own ellipsis + its own auto-fit measurement -->
<td>
  <div class="cell-name">{{ row.name }}</div>
  <div class="cell-sub">{{ row.email }}</div>
</td>

<!-- Wrong — text-overflow: ellipsis never cascades into nested block children,
     and auto-fit would concatenate "NameEmail" into one string and measure
     that as if it were a single line, wildly overestimating the needed width -->
<td>
  <div>{{ row.name }}{{ row.email }}</div>
</td>
```

If a leaf element has its own distinct font (e.g. `.cell-mono` for a
monospace phone number), that's fine and expected — the measurement code
reads each leaf's *own* `getComputedStyle().font`, not the outer `<td>`'s.

### c) Auto-fit padding assumes the shared cell padding

`AUTO_FIT_PADDING_PX` in the directive is tuned to the shared `td.mat-cell`
padding (`12px 20px` in `styles.scss`). If a screen overrides cell padding
for a specific column, auto-fit on that column may land a few px short
(minor truncation) — bump `AUTO_FIT_PADDING_PX` if this becomes common, or
give that column a fixed non-resizable width instead.

## 4. Bug catalog (context for anyone touching this file again)

Every one of these was found and fixed during the 2026-07-15 rollout of
drag-resize, double-click auto-fit, and Wrap Text. Documented so a future
change doesn't quietly reintroduce them.

| # | Symptom | Root cause | Fix |
|---|---|---|---|
| 1 | Stacked two-line cells (name+email) hard-clip with no "…" | `text-overflow: ellipsis` never cascades into nested block children — only truncates a container's own single-line content | Apply ellipsis/nowrap/overflow:hidden directly to childless leaf `div`/`span` (`:not(:has(*))`), not just the `<td>` |
| 2 | Short cells float mid-height next to a taller wrapped neighbor | Global `vertical-align: middle` on `td.mat-cell` never got a wrap-active override | `vertical-align: top !important` inside `.cms-wrap-active` |
| 3 | Whole table renders blank on load | `this.state` (an `@Input()`) read synchronously in the constructor — inputs aren't bound until after the constructor returns | Don't touch `@Input()`-bound values in the constructor body; only inside `effect()` |
| 4 | Double-click didn't reach the correct width in one click; short columns crept wider on repeated double-clicks | Each double-click's two pointerdown/pointerup pairs carry a px or two of hand jitter, which the (threshold-less) drag path committed as a tiny resize *before* `dblclick` fired — shifting the boundary and sometimes causing the real `dblclick` hit-test to miss entirely | 4px movement deadzone (`DRAG_THRESHOLD_PX`) before pointer movement counts as a drag |
| 5 | Auto-fit wildly over-sized name/email columns, under-sized monospace phone columns | Measured `cell.textContent` (concatenates every line with no separator) at the outer `<td>`'s font (wrong for inner elements with their own font, e.g. `.cell-mono`) | `measureCellWidth()` measures each childless leaf individually with its own computed font, takes the max |
| 6 | Resize-boundary cursor barely ever showed the resize arrow | The decorative handle is `pointer-events: none` (required so CDK's sticky-border elements don't swallow clicks), so its own `cursor`/`:hover` can never fire; Material's `.mat-sort-header-container` spans the full header width with its own `cursor: pointer`, which wins over any ancestor-level cursor style | Toggle a class on the hovered `<th>` from JS (`mousemove`), targeting `.mat-sort-header-container` directly with `cursor: col-resize !important` |
| 7 | Tooltip `<div>`s piled up in `<body>` forever, one per hover | CDK's `DomPortal`, on `dispose()`, restores the manually-created node to its original DOM location rather than destroying it (by design — `DomPortal` is meant for reusable existing elements) | Track the created element explicitly; call `.remove()` on hide, not just `dispose()` |
| 8 | Auto-fit still clipped a few trailing characters even after "fitting" | `AUTO_FIT_PADDING_PX` (16, ×2 = 32px total) undercounted the real cell padding (`12px 20px` = 40px total) | Corrected to 22 (×2 = 44px — the real 40px padding plus a small buffer for canvas `measureText()` vs. real DOM text-layout rounding) |
| 9 | Drag jumped a large, disorienting amount at the very start of the gesture | Starting width was measured from the `<th>`, which can have a different natural width than the `<td>`s (its own label + sort-icon content, both `white-space: nowrap`) | Measure the starting width from a real body `<td>` (`measureColumnWidth()`) |
| 10 | Drag jumped again immediately after a double-click auto-fit, right when you started dragging from the new boundary | Starting width still came from a fresh `getBoundingClientRect()` DOM measurement, which can diverge from the just-committed state value (`table-layout: auto` re-settling) | Prefer `state.getWidth(key)` (the authoritative committed value) as the drag's starting reference; only fall back to a DOM measurement for a never-resized column |
| 11 | Column snapped to a different width right at release, not matching what was shown throughout the drag | `onUp` re-measured `th.getBoundingClientRect().width` instead of using the value already live-applied during the drag | Commit the exact last value pushed during `onMove` (`lastWidth`), not a fresh re-measurement |
| 12 | Column width visually grew back after a drag, with zero clicks | Flexbox `min-width: auto` letting cell content overflow the `<td>`'s real (correctly-sized) box — see §3a. Not a resize-mechanism bug at all; it only *looked* like one | `min-width: 0` on the flex child wrapping the text |

## 5. If something looks broken on a new screen

1. Check the cursor near the boundary shows the resize arrow (↔) before
   assuming a hit-detection bug — if it does, the boundary math is correct
   and the bug is almost certainly a per-screen markup issue (§3), not the
   shared directive.
2. Check DevTools → Elements → `<head>` for the injected
   `<style data-resize-id="...">` tag — compare the `width` number actually
   written there against what you expect, and against the Computed-tab
   rendered width of the same `<td>`. A mismatch between "what's written"
   and "what's rendered" almost always traces back to a per-screen CSS issue
   (flex `min-width`, unusual padding, etc.), not the directive's math.
3. Check for stale widths in `localStorage` (`<screen>-cols` key,
   `widths` object) left over from earlier testing before concluding a fresh
   bug — `autoFit()` and drag both unconditionally overwrite on interaction,
   but if you're testing *without* interacting, an old value is likely why
   nothing looks like you expect.
