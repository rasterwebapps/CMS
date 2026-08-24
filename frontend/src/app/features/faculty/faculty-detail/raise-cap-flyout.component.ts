import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { FacultyService } from '../faculty.service';

/**
 * In-page "Raise Cap" action — replaces navigating to the full Faculty edit form just to change
 * one field. Saves via FacultyService.updateDailyCap (a minimal single-field PATCH, not the full
 * update payload the edit form uses) and emits `saved` so the caller can refresh in place.
 */
@Component({
  selector: 'app-raise-cap-flyout',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, CmsFlyoutPanelComponent],
  templateUrl: './raise-cap-flyout.component.html',
  styleUrl: './raise-cap-flyout.component.scss',
})
export class RaiseCapFlyoutComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly toast = inject(ToastService);

  readonly facultyId = input.required<number>();
  readonly facultyName = input.required<string>();
  readonly currentDailyCap = input<number | null>(null);

  readonly closed = output<void>();
  readonly saved = output<void>();

  protected readonly saving = signal(false);
  protected newDailyCap: number | null = null;

  /** Signal inputs aren't guaranteed bound until ngOnInit — reading them any earlier throws NG0950. */
  ngOnInit(): void {
    this.newDailyCap = this.currentDailyCap();
  }

  protected onSave(): void {
    if (this.newDailyCap != null && this.newDailyCap < 0) {
      this.toast.error('Daily cap can’t be negative');
      return;
    }
    this.saving.set(true);
    this.facultyService.updateDailyCap(this.facultyId(), this.newDailyCap).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Daily cap updated');
        this.saved.emit();
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to update daily cap');
      },
    });
  }

  protected onClose(): void {
    this.closed.emit();
  }
}
