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
const AUTO_FIT_PADDING_PX = 16;
const BOUNDARY_HIT_TOLERANCE_PX = 5;

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

  private dragCleanup: (() => void) | null = null;
  /** Set for the duration of a resize gesture so the synthetic `click` that
   *  otherwise follows pointerup doesn't get interpreted as a sort-header click. */
  private suppressNextClick = false;

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
      this.renderer[wrapped ? 'addClass' : 'removeClass'](table, 'cms-wrap-active');
      // Defer past Angular's own re-render of the @for/matColumnDef rows.
      queueMicrotask(() => this.syncDom());
    });
  }

  ngOnDestroy(): void {
    this.styleEl?.remove();
    this.dragCleanup?.();
    this.tooltipRef?.dispose();
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

    table.addEventListener('pointerdown', onPointerDown, { capture: true });
    table.addEventListener('dblclick', onDblClick, { capture: true });
    table.addEventListener('click', onClickCapture, { capture: true });
    this.cleanupFns.push(() => {
      table.removeEventListener('pointerdown', onPointerDown, { capture: true });
      table.removeEventListener('dblclick', onDblClick, { capture: true });
      table.removeEventListener('click', onClickCapture, { capture: true });
    });
  }

  private startDrag(e: PointerEvent, th: HTMLElement, key: string): void {
    e.preventDefault();
    e.stopPropagation();
    const startX = e.clientX;
    const startWidth = th.getBoundingClientRect().width;
    document.body.style.userSelect = 'none';

    this.zone.runOutsideAngular(() => {
      const onMove = (ev: PointerEvent) => {
        const next = Math.max(MIN_WIDTH_PX, Math.round(startWidth + (ev.clientX - startX)));
        this.setColumnWidthLive(key, next);
      };
      const onUp = () => {
        document.removeEventListener('pointermove', onMove);
        document.removeEventListener('pointerup', onUp);
        document.body.style.userSelect = '';
        this.dragCleanup = null;
        this.zone.run(() => {
          const finalWidth = th.getBoundingClientRect().width;
          this.state.setWidth(key, finalWidth);
          this.matTable?.updateStickyColumnStyles();
        });
      };
      document.addEventListener('pointermove', onMove);
      document.addEventListener('pointerup', onUp, { once: true });
      this.dragCleanup = onUp;
    });
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

    ctx.font = getComputedStyle(th).font;
    let max = ctx.measureText(th.textContent?.trim() ?? '').width;

    const i = this.state.visibleColumns().indexOf(key);
    if (i >= 0) {
      const rows = this.el.nativeElement.querySelectorAll('tr.mat-row, tr.mat-mdc-row');
      rows.forEach(row => {
        const cell = row.children[i] as HTMLElement | undefined;
        if (!cell) return;
        ctx.font = getComputedStyle(cell).font;
        const w = ctx.measureText(cell.textContent?.trim() ?? '').width;
        if (w > max) max = w;
      });
    }

    const px = Math.max(MIN_WIDTH_PX, Math.round(max + AUTO_FIT_PADDING_PX * 2));
    this.state.setWidth(key, px);
    this.matTable?.updateStickyColumnStyles();
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
    this.tooltipRef.attach(new DomPortal(div));
  }

  private hideTooltip(): void {
    this.tooltipRef?.dispose();
    this.tooltipRef = null;
    this.tooltipTarget = null;
  }
}
