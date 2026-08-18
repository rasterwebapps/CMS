import { Component, EventEmitter, Output, computed, inject, signal } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { buildSvg, computeBounds, extractLayerNames, insunitsToMetersPerUnit, supportedEntities, Bounds } from './dxf-render.util';
import { detectShapes } from './dxf-shape-detect.util';
import { DetectedShapeCandidate } from '../spatial.model';
import type { IDxf, IEntity } from 'dxf-parser';

const MAX_FILE_SIZE = 30 * 1024 * 1024;

export interface DxfImportResult {
  file: File;
  /** Real-world meters per one drawing unit, from the DXF's own $INSUNITS header — null when the
   *  file carries no reliable unit info, in which case the caller falls back to manual calibration. */
  metersPerUnit: number | null;
  /** Candidate sub-component boundaries detected from the DXF's own closed-polyline geometry —
   *  empty when none were found, never auto-committed by this component. The caller (see
   *  `floor-plan-form-flyout.component.ts`) opens a staged-review flyout when this is non-empty. */
  detectedShapes: DetectedShapeCandidate[];
}

/**
 * Parses a DXF entirely in the browser (dxf-parser) and renders the common architectural subset
 * (LINE/LWPOLYLINE/POLYLINE/CIRCLE/ARC/TEXT/MTEXT) to a plain SVG the admin can preview and toggle
 * layers on before importing. Unlike a raster/PDF import, a DXF carries real-world units, so the
 * generated SVG can drive automatic calibration instead of the manual two-point workflow.
 */
@Component({
  selector: 'app-dxf-import-flyout',
  standalone: true,
  imports: [CmsFlyoutPanelComponent],
  templateUrl: './dxf-import-flyout.component.html',
  styleUrl: './dxf-import-flyout.component.scss',
})
export class DxfImportFlyoutComponent {
  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly imported = new EventEmitter<DxfImportResult>();

  private readonly toast = inject(ToastService);
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly loading = signal(false);
  protected readonly sourceFileName = signal('');
  protected readonly layers = signal<string[]>([]);
  protected readonly visibleLayers = signal<Set<string>>(new Set());
  protected readonly entityCount = signal(0);
  protected readonly metersPerUnit = signal<number | null>(null);
  protected readonly hasReliableUnits = computed(() => this.metersPerUnit() !== null);

  protected readonly previewSvg = computed(() => {
    const bounds = this.bounds();
    if (!bounds || this.entities.length === 0) return null;
    const svg = buildSvg(this.entities, this.visibleLayers(), bounds);
    return this.sanitizer.bypassSecurityTrustHtml(svg);
  });

  /** Live count so the admin knows before clicking import whether this drawing has anything to
   *  review — the actual detection re-runs on import itself (see `onImport`), this is just a
   *  preview count kept in sync with the layer toggles. */
  protected readonly detectedShapeCount = computed(() => {
    const bounds = this.bounds();
    if (!bounds || this.entities.length === 0) return 0;
    const visibleEntities = this.entities.filter((e) => this.visibleLayers().has(e.layer || '0'));
    return detectShapes(visibleEntities, bounds).length;
  });

  private readonly bounds = signal<Bounds | null>(null);
  private entities: IEntity[] = [];

  protected async onFileSelected(event: Event): Promise<void> {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) return;
    if (file.size > MAX_FILE_SIZE) {
      this.toast.error(`${file.name} exceeds the 30 MB limit`);
      (event.target as HTMLInputElement).value = '';
      return;
    }

    this.loading.set(true);
    this.sourceFileName.set(file.name);

    try {
      const text = await file.text();
      const { default: DxfParser } = await import('dxf-parser');
      const dxf: IDxf | null = new DxfParser().parseSync(text);
      if (!dxf || !dxf.entities?.length) {
        this.toast.error('No drawable entities found in this DXF');
        this.loading.set(false);
        return;
      }

      this.entities = supportedEntities(dxf.entities);
      if (this.entities.length === 0) {
        this.toast.error('This DXF only contains entity types that aren’t supported yet (e.g. hatches, splines, dimensions)');
        this.loading.set(false);
        return;
      }

      const bounds = computeBounds(this.entities);
      if (!bounds) {
        this.toast.error('Could not determine the drawing’s extents');
        this.loading.set(false);
        return;
      }
      this.bounds.set(bounds);

      const layerNames = extractLayerNames(this.entities);
      this.layers.set(layerNames);
      this.visibleLayers.set(new Set(layerNames));
      this.entityCount.set(this.entities.length);

      const insunits = dxf.header?.['$INSUNITS'];
      this.metersPerUnit.set(insunitsToMetersPerUnit(typeof insunits === 'number' ? insunits : undefined));
    } catch {
      this.toast.error('Failed to read this DXF file');
    } finally {
      this.loading.set(false);
    }
  }

  protected toggleLayer(layer: string): void {
    const next = new Set(this.visibleLayers());
    if (next.has(layer)) next.delete(layer); else next.add(layer);
    this.visibleLayers.set(next);
  }

  protected onClose(): void {
    this.closed.emit();
  }

  protected onImport(): void {
    const bounds = this.bounds();
    if (!bounds || this.entities.length === 0) return;

    const svgString = buildSvg(this.entities, this.visibleLayers(), bounds);
    const baseName = this.sourceFileName().replace(/\.dxf$/i, '') || 'floor-plan';
    const blob = new Blob([svgString], { type: 'image/svg+xml' });
    const file = new File([blob], `${baseName}.svg`, { type: 'image/svg+xml' });

    // Only detect within entities on visible layers — a layer the admin toggled off is, from
    // their point of view, not part of this drawing (same filter `buildSvg` already applies).
    const visibleEntities = this.entities.filter((e) => this.visibleLayers().has(e.layer || '0'));
    const detectedShapes = detectShapes(visibleEntities, bounds);

    this.imported.emit({ file, metersPerUnit: this.metersPerUnit(), detectedShapes });
  }
}
