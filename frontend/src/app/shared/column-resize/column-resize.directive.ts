import {
  Directive,
  ElementRef,
  Input,
  NgZone,
  OnDestroy,
  Renderer2,
  effect,
  inject,
} from '@angular/core';
import { MatTable } from '@angular/material/table';
import { ConnectedPosition, Overlay, OverlayRef } from '@angular/cdk/overlay';
import { DomPortal } from '@angular/cdk/portal';
import { ColumnPickerState } from '../column-picker';

const MIN_WIDTH_PX = 60;
// Added per side (`* 2` at the call site). Actual cell padding is
// `12px 20px` (styles.scss) — 20px per side — plus a couple px of slack for
// canvas measureText() slightly underestimating actual rendered text width
// (subpixel/kerning rounding differences vs real DOM text layout). The old
// value of 16 undercounted the real 20px padding, so auto-fit consistently
// landed a few px too narrow and clipped trailing characters.
const AUTO_FIT_PADDING_PX = 22;
const BOUNDARY_HIT_TOLERANCE_PX = 8;
/** Minimum pointer movement before a pointerdown-on-boundary counts as an
 *  intentional drag, rather than jitter from an ordinary click. */
const DRAG_THRESHOLD_PX = 4;

const TOOLTIP_POSITIONS: ConnectedPosition[] = [
  { originX: 'center', originY: 'top', overlayX: 'center', overlayY: 'bottom', offsetY: -6 },
  { originX: 'center', originY: 'bottom', overlayX: 'center', overlayY: 'top', offsetY: 6 },
];

/**
 * Drag-to-resize, double-click auto-fit, and hover tooltips for a `mat-table`'s
 * columns — driven entirely by the same `ColumnPickerState` instance that
 * already owns column order/visibility/pin, so no per-column template edits
 * are needed anywhere. Column widths are applied via an injected `<style>`
 * block keyed off `nth-child` position, not per-cell bindings.
 */
@Directive({
  selector: 'table[cmsResizableColumns]',
  standalone: true,
})
export class ColumnResizeDirective implements OnDestroy {
  @Input({ required: true, alias: 'cmsResizableColumns' }) state!: ColumnPickerState;

  private readonly el = inject<ElementRef<HTMLTableElement>>(ElementRef);
  private readonly renderer = inject(Renderer2);
  private readonly zone = inject(NgZone);
  private readonly overlay = inject(Overlay);
  private readonly matTable = inject(MatTable, { self: true, optional: true });

  private readonly resizeId = `cms-rt-${Math.random().toString(36).slice(2, 9)}`;
  private styleEl: HTMLStyleElement | null = null;
  private readonly cleanupFns: Array<() => void> = [];

  private tooltipRef: OverlayRef | null = null;
  private tooltipTarget: HTMLElement | null = null;
  private tooltipEl: HTMLDivElement | null = null;

  private dragCleanup: (() => void) | null = null;
  /** Set for the duration of a resize gesture so the synthetic `click` that
   *  otherwise follows pointerup doesn't get interpreted as a sort-header click. */
  private suppressNextClick = false;

  /** The handle currently under the pointer, per `findBoundaryAt` — tracked so
   *  hover feedback can be driven by JS instead of a native `:hover`, since the
   *  handle element is `pointer-events: none` (see `attachHandles()`). */
  private hoveredHandle: HTMLElement | null = null;

  /** The `<th>` currently near a resize boundary — tracked separately from
   *  `hoveredHandle` because the cursor fix needs a class on the `<th>`
   *  itself (see `onMouseMove`'s comment on `cms-th-resize-hover`). */
  private hoveredTh: HTMLElement | null = null;

  constructor() {
    const table = this.el.nativeElement;
    this.renderer.addClass(table, 'cms-resizable-table');
    this.renderer.setAttribute(table, 'data-resize-id', this.resizeId);

    this.setupHoverTooltips();
    this.setupBoundaryInteractions();

    effect(() => {
      // Track every signal that can change header/row DOM structure.
      this.state.orderedColumns();
      this.state.visibleColumns();
      this.state.widths();
      const wrapped = this.state.wrapText();

      // Wrap Text only changes whether overflowing content wraps within
      // whatever width a column already has (like Excel's Wrap Text) — it
      // never resizes anything. Width only ever changes via drag or
      // double-click auto-fit, both independent of this toggle.
      this.renderer[wrapped ? 'addClass' : 'removeClass'](table, 'cms-wrap-active');
      // Defer past Angular's own re-render of the @for/matColumnDef rows.
      queueMicrotask(() => this.syncDom());
    });
  }

  ngOnDestroy(): void {
    this.styleEl?.remove();
    this.dragCleanup?.();
    this.tooltipRef?.dispose();
    this.tooltipEl?.remove();
    this.cleanupFns.forEach(fn => fn());
  }

  private syncDom(): void {
    this.rebuildWidthStyles();
    this.attachHandles();
  }

  /**
   * Only columns the user has actually resized (drag or auto-fit) get a pinned
   * width + clipping. Every other column is left on the table's natural `auto`
   * layout, so it expands to fit its content and the `.table-wrapper`'s
   * `overflow-x: auto` handles the rest via horizontal scroll — not by
   * squeezing every column to fit inside the viewport.
   */
  private rebuildWidthStyles(): void {
    if (!this.styleEl) {
      this.styleEl = document.createElement('style');
      document.head.appendChild(this.styleEl);
    }
    const widths = this.state.widths();
    const order = this.state.visibleColumns();
    const sel = `table[data-resize-id="${this.resizeId}"]`;
    const rules = order
      .map((key, i) => {
        const px = widths[key];
        if (!px) return '';
        const n = i + 1;
        return `${sel} th:nth-child(${n}), ${sel} td:nth-child(${n}) { width:${px}px; min-width:${px}px; max-width:${px}px; overflow:hidden; text-overflow:ellipsis; }`;
      })
      .filter(Boolean)
      .join('\n');
    this.styleEl.textContent = rules;
  }

  /** Purely decorative — the cursor/hover affordance. Actual interaction is
   *  handled by `setupBoundaryInteractions()`'s coordinate-based hit test,
   *  since Angular Material's own sticky-column border decoration elements
   *  (`.mat-mdc-table-sticky-border-elem-*`) sit above this in paint order
   *  at exactly the column-boundary pixels and would otherwise swallow the
   *  pointer event before it ever reaches this element. */
  private attachHandles(): void {
    const headerRow = this.el.nativeElement.querySelector('tr.mat-header-row, tr.mat-mdc-header-row');
    if (!headerRow) return;
    const ths = Array.from(headerRow.querySelectorAll('th'));

    ths.forEach(th => {
      if (th.querySelector(':scope > .cms-col-resize-handle')) return;
      this.renderer.setStyle(th, 'position', 'relative');
      const handle = this.renderer.createElement('div') as HTMLDivElement;
      this.renderer.addClass(handle, 'cms-col-resize-handle');
      this.renderer.setAttribute(handle, 'aria-hidden', 'true');
      this.renderer.setStyle(handle, 'pointer-events', 'none');
      this.renderer.appendChild(th, handle);
    });
  }

  /** Finds the visible column whose right edge is under `clientX`/`clientY`, within tolerance. */
  private findBoundaryAt(clientX: number, clientY: number): { th: HTMLElement; key: string } | null {
    const headerRow = this.el.nativeElement.querySelector('tr.mat-header-row, tr.mat-mdc-header-row');
    if (!headerRow) return null;
    const ths = Array.from(headerRow.querySelectorAll('th')) as HTMLElement[];
    const keys = this.state.visibleColumns();

    for (let i = 0; i < ths.length; i++) {
      const rect = ths[i].getBoundingClientRect();
      if (clientY < rect.top || clientY > rect.bottom) continue;
      if (Math.abs(clientX - rect.right) <= BOUNDARY_HIT_TOLERANCE_PX) {
        const key = keys[i];
        return key ? { th: ths[i], key } : null;
      }
    }
    return null;
  }

  private setupBoundaryInteractions(): void {
    const table = this.el.nativeElement;

    const onPointerDown = (e: PointerEvent) => {
      const hit = this.findBoundaryAt(e.clientX, e.clientY);
      if (!hit) return;
      this.suppressNextClick = true;
      this.startDrag(e, hit.th, hit.key);
    };
    const onDblClick = (e: MouseEvent) => {
      const hit = this.findBoundaryAt(e.clientX, e.clientY);
      if (!hit) return;
      e.preventDefault();
      e.stopPropagation();
      this.autoFit(hit.th, hit.key);
    };
    const onClickCapture = (e: MouseEvent) => {
      if (!this.suppressNextClick) return;
      this.suppressNextClick = false;
      e.preventDefault();
      e.stopPropagation();
    };
    // Drives the resize-boundary cursor/highlight from JS rather than a native
    // `:hover` — the handle element is `pointer-events: none` (see
    // `attachHandles()`), so it can never itself register `:hover`, and there
    // was previously no visual cue at all for where the ±tolerance hit zone is.
    const onMouseMove = (e: MouseEvent) => {
      const hit = this.findBoundaryAt(e.clientX, e.clientY);
      const nextHandle = hit
        ? (hit.th.querySelector(':scope > .cms-col-resize-handle') as HTMLElement | null)
        : null;
      if (nextHandle !== this.hoveredHandle) {
        if (this.hoveredHandle) this.renderer.removeClass(this.hoveredHandle, 'cms-col-resize-handle--near');
        this.hoveredHandle = nextHandle;
        if (this.hoveredHandle) this.renderer.addClass(this.hoveredHandle, 'cms-col-resize-handle--near');
      }
      const nextTh = hit ? hit.th : null;
      if (nextTh !== this.hoveredTh) {
        // `sort-header-container` (a *descendant* of `<th>`) carries its own
        // `cursor: pointer` and spans the full header width
        // (`.mat-sort-header-container { width: 100% }`), so it wins over any
        // ancestor-level cursor style for that same pixel — a plain
        // `table.style.cursor` was losing to it almost everywhere except a
        // thin sliver. `!important` on a class targeting that exact
        // descendant is what actually overrides it.
        if (this.hoveredTh) this.renderer.removeClass(this.hoveredTh, 'cms-th-resize-hover');
        this.hoveredTh = nextTh;
        if (this.hoveredTh) this.renderer.addClass(this.hoveredTh, 'cms-th-resize-hover');
      }
    };
    const onMouseLeave = () => {
      if (this.hoveredHandle) this.renderer.removeClass(this.hoveredHandle, 'cms-col-resize-handle--near');
      this.hoveredHandle = null;
      if (this.hoveredTh) this.renderer.removeClass(this.hoveredTh, 'cms-th-resize-hover');
      this.hoveredTh = null;
    };

    table.addEventListener('pointerdown', onPointerDown, { capture: true });
    table.addEventListener('dblclick', onDblClick, { capture: true });
    table.addEventListener('click', onClickCapture, { capture: true });
    this.zone.runOutsideAngular(() => {
      table.addEventListener('mousemove', onMouseMove);
      table.addEventListener('mouseleave', onMouseLeave);
    });
    this.cleanupFns.push(() => {
      table.removeEventListener('pointerdown', onPointerDown, { capture: true });
      table.removeEventListener('dblclick', onDblClick, { capture: true });
      table.removeEventListener('click', onClickCapture, { capture: true });
      table.removeEventListener('mousemove', onMouseMove);
      table.removeEventListener('mouseleave', onMouseLeave);
    });
  }

  private startDrag(e: PointerEvent, th: HTMLElement, key: string): void {
    e.preventDefault();
    e.stopPropagation();
    const startX = e.clientX;
    // Prefer the authoritative committed width from state over a fresh DOM
    // measurement — once a column has been drag-resized or auto-fit, that's
    // the one guaranteed-correct source of truth for "what it's currently
    // set to." A DOM `getBoundingClientRect()` can report something subtly
    // different (table-layout:auto re-settling, header vs. data-cell
    // discrepancies — see `measureColumnWidth()`), and since this is the
    // *starting* reference for the drag's relative math, any mismatch here
    // throws off every subsequent pixel of the drag from the very first
    // move, producing a large, disorienting jump. Only fall back to
    // measuring the DOM for a column that's never been touched (still on
    // natural auto width, nothing in state yet).
    const startWidth = this.state.getWidth(key) ?? this.measureColumnWidth(th, key);
    // A real double-click's two pointerdown/pointerup pairs almost always
    // carry a px or two of hand jitter between them — without a deadzone,
    // each one committed a tiny drag-resize *before* the `dblclick` event
    // could fire, which (a) grew any column a little on every double-click
    // attempt regardless of content, and (b) could shift the boundary enough
    // that the dblclick handler's own hit-test missed it, skipping autoFit
    // entirely. Sub-threshold movement now commits nothing.
    let dragStarted = false;
    // The exact value last pushed to the live stylesheet during the drag —
    // committed as-is on release. Previously `onUp` re-measured
    // `th.getBoundingClientRect().width` instead, which can differ from what
    // was actually shown throughout the drag: the header `<th>`'s own label
    // + sort-icon content (both `white-space: nowrap`) has its own natural
    // minimum width, and if that's bigger than the `<td>`s could visually
    // shrink to, the browser reports back a different number than what you
    // were watching live — causing a visible snap right at release.
    let lastWidth = startWidth;
    document.body.style.userSelect = 'none';

    this.zone.runOutsideAngular(() => {
      const onMove = (ev: PointerEvent) => {
        const delta = ev.clientX - startX;
        if (!dragStarted) {
          if (Math.abs(delta) < DRAG_THRESHOLD_PX) return;
          dragStarted = true;
        }
        const next = Math.max(MIN_WIDTH_PX, Math.round(startWidth + delta));
        lastWidth = next;
        this.setColumnWidthLive(key, next);
      };
      const onUp = () => {
        document.removeEventListener('pointermove', onMove);
        document.removeEventListener('pointerup', onUp);
        document.body.style.userSelect = '';
        this.dragCleanup = null;
        if (!dragStarted) return;
        this.zone.run(() => {
          this.state.setWidth(key, lastWidth);
          this.matTable?.updateStickyColumnStyles();
        });
      };
      document.addEventListener('pointermove', onMove);
      document.addEventListener('pointerup', onUp, { once: true });
      this.dragCleanup = onUp;
    });
  }

  /** The actual visible width of a column, read from a real data `<td>`
   *  rather than its `<th>` — see the comment in `startDrag()` for why. */
  private measureColumnWidth(th: HTMLElement, key: string): number {
    const i = this.state.visibleColumns().indexOf(key);
    if (i >= 0) {
      const firstRow = this.el.nativeElement.querySelector('tr.mat-row, tr.mat-mdc-row');
      const cell = firstRow?.children[i] as HTMLElement | undefined;
      if (cell) return cell.getBoundingClientRect().width;
    }
    return th.getBoundingClientRect().width;
  }

  /** Writes width directly to the injected stylesheet during drag — no Angular CD per pixel moved. */
  private setColumnWidthLive(key: string, px: number): void {
    if (!this.styleEl) return;
    const i = this.state.visibleColumns().indexOf(key);
    if (i < 0) return;
    const n = i + 1;
    const sel = `table[data-resize-id="${this.resizeId}"]`;
    const rule = `${sel} th:nth-child(${n}), ${sel} td:nth-child(${n}) { width:${px}px; min-width:${px}px; max-width:${px}px; overflow:hidden; text-overflow:ellipsis; }`;
    const marker = `/*live:${key}*/`;
    const base = (this.styleEl.textContent ?? '').replace(new RegExp(`${marker}[^\\n]*\\n?`, 'g'), '');
    this.styleEl.textContent = `${base}\n${marker}${rule}`;
  }

  private autoFit(th: HTMLElement, key: string): void {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let max = this.measureCellWidth(ctx, th);

    const i = this.state.visibleColumns().indexOf(key);
    if (i >= 0) {
      const rows = this.el.nativeElement.querySelectorAll('tr.mat-row, tr.mat-mdc-row');
      rows.forEach(row => {
        const cell = row.children[i] as HTMLElement | undefined;
        if (!cell) return;
        const w = this.measureCellWidth(ctx, cell);
        if (w > max) max = w;
      });
    }

    const px = Math.max(MIN_WIDTH_PX, Math.round(max + AUTO_FIT_PADDING_PX * 2));
    this.state.setWidth(key, px);
    this.matTable?.updateStickyColumnStyles();
  }

  /**
   * Measures the widest *single rendered line* a cell needs, not its whole
   * `textContent` at the cell's own font. Two things break a naive
   * `ctx.measureText(cell.textContent)` at `getComputedStyle(cell).font`:
   * stacked multi-line cells (e.g. name + email) concatenate every line's
   * text into one string with no separators, wildly overestimating since
   * those lines never render on one line together; and any inner element
   * with its own font (e.g. a `<span class="cell-mono">` phone number)
   * renders at a different size/family than the outer `<td>`, so measuring
   * with the cell's own font under/overestimates what's actually on screen.
   * Measuring each childless leaf with its *own* computed font and taking
   * the max fixes both.
   */
  private measureCellWidth(ctx: CanvasRenderingContext2D, cell: HTMLElement): number {
    const leaves = Array.from(cell.querySelectorAll('div, span')).filter(
      el => el.children.length === 0 && (el.textContent ?? '').trim().length > 0
    ) as HTMLElement[];
    const targets = leaves.length ? leaves : [cell];

    let max = 0;
    targets.forEach(el => {
      ctx.font = getComputedStyle(el).font;
      const w = ctx.measureText(el.textContent?.trim() ?? '').width;
      if (w > max) max = w;
    });
    return max;
  }

  // ── Truncation tooltips (delegated, driven by textContent — works for any column's markup) ──
  private setupHoverTooltips(): void {
    const table = this.el.nativeElement;
    const onOver = (e: Event) => {
      if (this.state.wrapText()) return;
      const td = (e.target as HTMLElement).closest('td');
      if (!td || td === this.tooltipTarget || !table.contains(td)) return;
      if (td.scrollWidth <= td.clientWidth) return;
      this.showTooltip(td);
    };
    const onOut = (e: Event) => {
      const related = (e as MouseEvent).relatedTarget as HTMLElement | null;
      if (this.tooltipTarget && related && this.tooltipTarget.contains(related)) return;
      this.hideTooltip();
    };
    table.addEventListener('mouseover', onOver);
    table.addEventListener('mouseout', onOut);
    this.cleanupFns.push(() => {
      table.removeEventListener('mouseover', onOver);
      table.removeEventListener('mouseout', onOut);
    });
  }

  private showTooltip(td: HTMLElement): void {
    this.hideTooltip();
    const text = td.textContent?.trim();
    if (!text) return;

    this.tooltipTarget = td;
    const positionStrategy = this.overlay.position()
      .flexibleConnectedTo(td)
      .withPositions(TOOLTIP_POSITIONS)
      .withViewportMargin(4);

    this.tooltipRef = this.overlay.create({ positionStrategy, panelClass: 'cms-cell-tooltip-panel' });
    // DomPortal moves an *existing* DOM node into the overlay pane — it
    // doesn't insert a detached one, so this must be appended somewhere
    // first (hidden) or attach() throws "must be attached to a parent node".
    const div = document.createElement('div');
    div.className = 'cms-cell-tooltip';
    div.textContent = text;
    document.body.appendChild(div);
    this.tooltipEl = div;
    this.tooltipRef.attach(new DomPortal(div));
  }

  private hideTooltip(): void {
    this.tooltipRef?.dispose();
    this.tooltipRef = null;
    this.tooltipTarget = null;
    // DomPortal's detach (triggered by dispose() above) restores the node to
    // its original spot in <body> rather than destroying it — CDK leaves a
    // placeholder comment there specifically to support reusing the same
    // element across multiple attach/detach cycles. Since we create a fresh
    // div per tooltip and never reuse it, dispose() alone left every single
    // tooltip ever shown sitting in <body> forever as an orphaned element.
    this.tooltipEl?.remove();
    this.tooltipEl = null;
  }
}
