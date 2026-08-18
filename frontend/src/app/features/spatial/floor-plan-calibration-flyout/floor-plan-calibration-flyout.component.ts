import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { SpatialService } from '../spatial.service';
import { FloorPlan } from '../spatial.model';
import { ToastService } from '../../../core/toast/toast.service';

interface CalibrationPoint {
  natural: { x: number; y: number };
  display: { x: number; y: number };
}

/**
 * Calibration points are recorded in the image's *natural* pixel space (img.naturalWidth/Height),
 * not the space it happens to be displayed at in this flyout — this must match the coordinate
 * space the Virtual Location canvas uses for geometryJson, since both read/write against the same
 * calibration. See BR-60.
 */
@Component({
  selector: 'app-floor-plan-calibration-flyout',
  standalone: true,
  imports: [FormsModule, CmsFlyoutPanelComponent],
  templateUrl: './floor-plan-calibration-flyout.component.html',
  styleUrl: './floor-plan-calibration-flyout.component.scss',
})
export class FloorPlanCalibrationFlyoutComponent implements OnInit, OnDestroy {
  @Input({ required: true }) floorPlan!: FloorPlan;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly saved = new EventEmitter<void>();

  @ViewChild('planImage') planImageRef?: ElementRef<HTMLImageElement>;

  private readonly spatialService = inject(SpatialService);
  private readonly toast = inject(ToastService);

  protected readonly imageUrl = signal<string | null>(null);
  protected readonly imageLoading = signal(true);
  protected readonly saving = signal(false);

  protected point1: CalibrationPoint | null = null;
  protected point2: CalibrationPoint | null = null;
  protected physicalLength: number | null = null;

  private objectUrl: string | null = null;

  ngOnInit(): void {
    this.spatialService.downloadFloorPlanFile(this.floorPlan.id).subscribe({
      next: (response) => {
        this.objectUrl = URL.createObjectURL(response.body!);
        this.imageUrl.set(this.objectUrl);
        this.imageLoading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load the floor plan image');
        this.imageLoading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
  }

  protected onImageClick(event: MouseEvent): void {
    const img = this.planImageRef?.nativeElement;
    if (!img || !img.naturalWidth) return;

    const rect = img.getBoundingClientRect();
    const display = { x: event.clientX - rect.left, y: event.clientY - rect.top };
    const scaleX = img.naturalWidth / rect.width;
    const scaleY = img.naturalHeight / rect.height;
    const natural = { x: display.x * scaleX, y: display.y * scaleY };

    if (!this.point1) {
      this.point1 = { natural, display };
    } else if (!this.point2) {
      this.point2 = { natural, display };
    } else {
      this.point1 = { natural, display };
      this.point2 = null;
    }
  }

  protected resetPoints(): void {
    this.point1 = null;
    this.point2 = null;
  }

  protected canSubmit(): boolean {
    return !!this.point1 && !!this.point2 && !!this.physicalLength && this.physicalLength > 0;
  }

  protected onClose(): void {
    this.closed.emit();
  }

  protected onSubmit(): void {
    if (!this.canSubmit() || this.saving()) return;
    this.saving.set(true);
    this.spatialService.calibrateFloorPlan(this.floorPlan.id, {
      point1X: this.point1!.natural.x,
      point1Y: this.point1!.natural.y,
      point2X: this.point2!.natural.x,
      point2Y: this.point2!.natural.y,
      physicalLength: this.physicalLength!,
    }).subscribe({
      next: () => {
        this.toast.success('Floor plan calibrated successfully');
        this.saving.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to calibrate floor plan');
        this.saving.set(false);
      },
    });
  }
}
