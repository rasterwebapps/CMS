# Take a Tour + Flow Map — how to add it to a screen

This is the recipe used to add "Take a Tour" (Guided Tour) and "Flow Map" to
Collect Payment (`fee-collection.component.*`, OC-136/137/138). Follow this
exact shape for every other screen so the pattern stays consistent — don't
invent per-screen variations.

## The shared layer (do not duplicate — reuse as-is)

- `tour.service.ts` — `TourService`. `register(key, TourDefinition)` for the
  driver.js Guided Tour; `registerFlowMap(key, TourFlowMap)` for the optional
  second Flow Map tab. A screen can have a Guided Tour with no Flow Map (the
  `<cms-tour-button>` falls back to a single "Take a Tour" icon), but never a
  Flow Map with no Guided Tour — the flow map's `steps[].detail` text is
  written to restate the guided tour's own step prose, so author the Guided
  Tour first.
- `tour-button.component.ts` — `CmsTourButtonComponent`, selector
  `cms-tour-button`. Fully generic, never edit per screen.
- `tour-panel/tour-panel.component.ts` — `CmsTourPanelComponent`, renders
  whatever `TourFlowMap` it's given. Fully generic, never edit per screen.

## Per-screen work (the only part that changes)

### 1. Guided Tour (if the screen doesn't already have one)

Add `id="tour-<screen>-<element>"` anchors to the key template elements
(toolbar, search, table/form, summary panel — whatever the screen actually
has), then a `TourDefinition` with driver.js `steps` targeting those anchors.
Put it in the module's shared tours file (e.g.
`frontend/src/app/shared/tour/tours/finance.tours.ts`,
`academics.tours.ts`, `library.tours.ts`, etc. — create one per module if it
doesn't exist yet, following `finance.tours.ts`'s shape). 64 of the app's 97
nav screens already have this — check `tourService.register(` in the target
component first; if present, skip straight to step 2.

### 2. Flow Map (`TourFlowMap`)

Add a `..._FLOW_MAP` constant next to the screen's `TourDefinition`, e.g.:

```ts
export const FEE_COLLECTION_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Enquiries', description: 'Track interest, follow up, and convert promising enquiries into admissions.' },
    { label: 'Finalize Fee', description: 'Set the final fee amount for each enquiry before payment can begin.' },
    { label: 'Collect Payment', description: 'Record payments from enquiries and students, installment by installment.' },
    { label: 'Submit Documents', description: 'Collect proof of identity, transcripts, and certificates once a candidate has paid.' },
    { label: 'Verify Documents', description: 'Review and approve submitted documents before admission can be completed.' },
    { label: 'Complete Admission', description: 'Finalize paid, verified candidates into enrolled students with a roll number.' },
  ],
  currentIndex: 2, // this screen's own position within `funnel`
  steps: [
    { label: 'Filter / Search', icon: 'search', detail: 'Filter by status (All / Overdue / Outstanding) or type (Enquiries / Students), or use Quick Search to find the person paying.' },
    { label: 'Open Record', icon: 'open', detail: 'Click their row to open the payment form for that person.' },
    { label: 'Review Installments', icon: 'checklist', detail: 'Check due date, fee amount, already paid, and outstanding balance — each installment shows a paid / partial / overdue / pending badge.' },
    { label: 'Enter Payment', icon: 'payment', detail: 'Enter the collection amount, payment date, and payment mode (cash, cheque, UPI, bank transfer). Cash can be broken down by denomination.' },
    { label: 'Submit', icon: 'send', detail: 'Add remarks if needed, then submit. The outstanding balance updates automatically.' },
    { label: 'Receipt', icon: 'receipt', detail: 'The payment is recorded and a receipt is ready to print or download immediately.' },
  ],
};
```

`funnel` = the multi-screen journey this screen sits inside (usually the same
nav group, in nav order — e.g. Collect Payment's funnel is the 6 screens of
Admission Management's own pipeline). If a screen has no natural multi-screen
journey (most Preferences/master screens), `funnel` can be a single-entry
array containing just that screen, or the Flow Map can be skipped entirely —
a Guided Tour alone is a fine, complete outcome for those.

`steps` = 4–6 flowchart blocks describing what's actually done **on this
screen**, using the existing `FlowMapIcon` union
(`search | open | checklist | payment | send | receipt`). Extend the union in
`tour.service.ts` only if a genuinely new icon shape is needed — check the
icon-rendering switch in `tour-panel.component.ts` before adding one, and
keep the set small/reusable rather than growing it per-screen.

### 3. Wiring (component + template)

In the component's `ngOnInit` (or wherever tours are already registered):

```ts
this.tourService.register('<tour-key>', SCREEN_TOUR);
this.tourService.registerFlowMap('<tour-key>', SCREEN_FLOW_MAP);
```

In the template, next to the screen's existing help icon (or add one to the
toolbar if none exists):

```html
<cms-tour-button tourKey="<tour-key>" [iconOnly]="true" iconVariant="info-circle" />
```

`tourKey` must match the string passed to both `register` and
`registerFlowMap`. If the screen already has a bare `<cms-tour-button>` for
its Guided Tour, adding `registerFlowMap` for that same key is enough — the
button automatically grows the Guided Tour / Flow Map icon-switch, no
template change needed.

## Verification

- `npx tsc -p tsconfig.app.json --noEmit` after each batch of screens.
- `ng build` if any new icon or shared-file change was made.
- Manual click-through (light + dark, all roles that see the screen) per the
  Component Touch Rule — flag anything not visually verified in an unattended
  session as "needs manual check" in the session log rather than skipping it
  silently.
