import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { SpatialService } from '../spatial.service';
import { DetectedShapeCandidate, DiagramLevel, FloorPlan, OriginAnchor, UnitSystem } from '../spatial.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PdfImportFlyoutComponent, PdfImportResult } from '../pdf-import-flyout/pdf-import-flyout.component';
import { DxfImportFlyoutComponent, DxfImportResult } from '../dxf-import-flyout/dxf-import-flyout.component';
import { DetectedShapesReviewFlyoutComponent } from '../detected-shapes-review-flyout/detected-shapes-review-flyout.component';

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ACCEPTED_TYPES = '.svg,.png,.jpg,.jpeg,.gif,.webp';
const METERS_PER_FOOT = 0.3048;

@Component({
  selector: 'app-floor-plan-form-flyout',
  standalone: true,
  imports: [FormsModule, CmsFlyoutPanelComponent, PdfImportFlyoutComponent, DxfImportFlyoutComponent, DetectedShapesReviewFlyoutComponent],
  templateUrl: './floor-plan-form-flyout.component.html',
  styleUrl: './floor-plan-form-flyout.component.scss',
})
export class FloorPlanFormFlyoutComponent implements OnInit {
  /** null = create mode; a FloorPlan = edit-metadata (+ optional replace-file) mode. */
  @Input() floorPlan: FloorPlan | null = null;
  /** Only used in create mode. */
  @Input({ required: true }) entityType!: string;
  @Input({ required: true }) entityId!: number;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly saved = new EventEmitter<void>();

  private readonly spatialService = inject(SpatialService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  protected readonly acceptedTypes = ACCEPTED_TYPES;
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);

  protected name = '';
  protected unitSystem: UnitSystem = 'METERS';
  protected originAnchor: OriginAnchor = 'TOP_LEFT';
  protected originX = 0;
  protected originY = 0;
  protected selectedFile: File | null = null;
  protected selectedFileSource: 'picked' | 'pdf' | 'dxf' | null = null;

  protected readonly showPdfImport = signal(false);
  protected readonly showDxfImport = signal(false);

  /** Real-world meters per one SVG unit of the imported DXF, when it carried reliable units —
   *  used to auto-calibrate right after this floor plan is created/replaced. */
  protected pendingAutoCalibrateMetersPerUnit: number | null = null;

  /** Candidate sub-components detected from the DXF import (BR-60 extension) — reviewed via
   *  `showShapeReview` right after this floor plan is created/replaced + auto-calibrated, instead
   *  of emitting `saved` immediately. `savedFloorPlanId` is only set once we actually know it
   *  (create returns a new id; edit mode already has `floorPlan.id`). */
  protected pendingDetectedShapes: DetectedShapeCandidate[] = [];
  protected readonly showShapeReview = signal(false);
  protected savedFloorPlanId: number | null = null;

  ngOnInit(): void {
    if (this.floorPlan) {
      this.isEditMode.set(true);
      this.name = this.floorPlan.name;
      this.unitSystem = this.floorPlan.unitSystem;
      this.originAnchor = this.floorPlan.originAnchor;
      this.originX = this.floorPlan.originX;
      this.originY = this.floorPlan.originY;
    }
  }

  protected onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (file && file.size > MAX_FILE_SIZE) {
      this.toast.error(`${file.name} exceeds the 10 MB limit`);
      (event.target as HTMLInputElement).value = '';
      return;
    }
    this.selectedFile = file;
    this.selectedFileSource = file ? 'picked' : null;
    this.pendingAutoCalibrateMetersPerUnit = null;
    this.pendingDetectedShapes = [];
  }

  protected openPdfImport(): void {
    this.showPdfImport.set(true);
  }

  protected closePdfImport(): void {
    this.showPdfImport.set(false);
  }

  protected onPdfImported(result: PdfImportResult): void {
    this.selectedFile = result.file;
    this.selectedFileSource = 'pdf';
    this.pendingAutoCalibrateMetersPerUnit = null;
    this.pendingDetectedShapes = result.detectedShapes;
    this.showPdfImport.set(false);
  }

  protected openDxfImport(): void {
    this.showDxfImport.set(true);
  }

  protected closeDxfImport(): void {
    this.showDxfImport.set(false);
  }

  protected onDxfImported(result: DxfImportResult): void {
    this.selectedFile = result.file;
    this.selectedFileSource = 'dxf';
    this.pendingAutoCalibrateMetersPerUnit = result.metersPerUnit;
    this.pendingDetectedShapes = result.detectedShapes;
    this.showDxfImport.set(false);
  }

  protected canSubmit(): boolean {
    if (!this.name.trim()) return false;
    if (!this.isEditMode() && !this.selectedFile) return false;
    return true;
  }

  protected onClose(): void {
    this.closed.emit();
  }

  protected onSubmit(): void {
    if (!this.canSubmit() || this.saving()) return;

    // Replacing the file re-derives the SVG viewBox, which can shift where already-placed markers
    // (in SVG-pixel space) land — warn first, but never block; the calibration flyout doesn't need
    // this same check since recalibrating never touches geometryJson or the viewBox.
    if (this.isEditMode() && this.selectedFile) {
      this.spatialService.getVirtualLocationsByFloorPlan(this.floorPlan!.id).subscribe({
        next: (locations) => {
          if (locations.length === 0) { this.proceedSubmit(); return; }
          this.dialog.open(ConfirmDialogComponent, {
            data: {
              title: 'Replace Floor Plan File',
              message: `This floor plan has ${locations.length} linked room/zone/marker${locations.length === 1 ? '' : 's'} placed on it — replacing the file may shift where they appear. Continue?`,
              confirmText: 'Replace',
              cancelText: 'Cancel',
            },
          }).afterClosed().subscribe((confirmed) => { if (confirmed) this.proceedSubmit(); });
        },
        error: () => this.proceedSubmit(),
      });
      return;
    }

    this.proceedSubmit();
  }

  private proceedSubmit(): void {
    this.saving.set(true);
    if (this.isEditMode()) {
      this.updateExisting();
    } else {
      this.createNew();
    }
  }

  private createNew(): void {
    this.spatialService.createFloorPlan({
      entityType: this.entityType,
      entityId: this.entityId,
      name: this.name.trim(),
      unitSystem: this.unitSystem,
      originAnchor: this.originAnchor,
      originX: this.originX,
      originY: this.originY,
    }, this.selectedFile!).subscribe({
      next: (plan) => this.finishWithAutoCalibrate(plan.id, 'Floor plan added successfully'),
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to add floor plan');
        this.saving.set(false);
      },
    });
  }

  private updateExisting(): void {
    const id = this.floorPlan!.id;
    this.spatialService.updateFloorPlan(id, {
      entityType: this.floorPlan!.entityType,
      entityId: this.floorPlan!.entityId,
      name: this.name.trim(),
      unitSystem: this.unitSystem,
      originAnchor: this.originAnchor,
      originX: this.originX,
      originY: this.originY,
      viewboxWidth: this.floorPlan!.viewboxWidth,
      viewboxHeight: this.floorPlan!.viewboxHeight,
    }).subscribe({
      next: () => {
        if (this.selectedFile) {
          this.replaceFile(id);
        } else {
          this.toast.success('Floor plan updated successfully');
          this.saving.set(false);
          this.saved.emit();
        }
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to update floor plan');
        this.saving.set(false);
      },
    });
  }

  private replaceFile(id: number): void {
    this.spatialService.replaceFloorPlanFile(id, this.selectedFile!).subscribe({
      next: () => this.finishWithAutoCalibrate(id, 'Floor plan updated successfully'),
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Metadata saved, but the file replace failed');
        this.saving.set(false);
      },
    });
  }

  /** After a create/replace that came from a DXF import with reliable units, calibrates the
   *  plan automatically instead of leaving it for the manual two-point workflow. Synthetic
   *  calibration points (0,0)→(1,0) are exactly 1 SVG unit apart, so physicalLength IS the
   *  real-world distance per SVG unit — matching the DXF's own scale. */
  private finishWithAutoCalibrate(id: number, successMessage: string): void {
    const metersPerUnit = this.pendingAutoCalibrateMetersPerUnit;
    if (metersPerUnit == null) {
      this.toast.success(successMessage);
      this.saving.set(false);
      this.finishSave(id);
      return;
    }

    const physicalLength = this.unitSystem === 'FEET' ? metersPerUnit / METERS_PER_FOOT : metersPerUnit;
    this.spatialService.calibrateFloorPlan(id, {
      point1X: 0, point1Y: 0, point2X: 1, point2Y: 0, physicalLength,
    }).subscribe({
      next: () => {
        this.toast.success(`${successMessage} and auto-calibrated from the DXF's units`);
        this.saving.set(false);
        this.finishSave(id);
      },
      error: () => {
        this.toast.success(successMessage);
        this.toast.error('Auto-calibration failed — calibrate it manually');
        this.saving.set(false);
        this.finishSave(id);
      },
    });
  }

  /** Detected candidates (DXF import) go through staged review before this form actually closes;
   *  everything else (manual pick, PDF import, or a DXF with nothing detected) closes immediately,
   *  same as before this feature existed. */
  private finishSave(id: number): void {
    if (this.pendingDetectedShapes.length === 0) {
      this.saved.emit();
      return;
    }
    this.savedFloorPlanId = id;
    this.showShapeReview.set(true);
  }

  protected onShapeReviewClosed(): void {
    this.showShapeReview.set(false);
    this.saved.emit();
  }

  /** `entityType`/`entityId` are always passed by the caller regardless of create/edit mode (see
   *  `floor-plan-list.component.ts`'s template — they come from the currently selected picker
   *  state, not conditionally omitted), and `entityType` is always one of the four DiagramLevel
   *  values in practice, since that's exactly what `floor-plan-list.component.ts` passes through
   *  from its own DiagramLevel-typed route data. */
  protected reviewDiagramLevel(): DiagramLevel {
    return this.entityType as DiagramLevel;
  }
}
