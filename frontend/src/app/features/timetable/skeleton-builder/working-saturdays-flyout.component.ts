import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { WeekOfMonth } from '../../academic-year/academic-year.model';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';

/** Which nth-Saturday-of-the-month occurrences count as real working days for this term — empty
 *  means the term hasn't opted in to Saturday scheduling at all (Mon-Fri only, hard-blocked
 *  otherwise). Institution-wide per term, not per cohort — every cohort in this term shares one
 *  pattern. */
@Component({
  selector: 'app-working-saturdays-flyout',
  standalone: true,
  imports: [CmsFlyoutPanelComponent],
  templateUrl: './working-saturdays-flyout.component.html',
  styleUrl: './working-saturdays-flyout.component.scss',
})
export class WorkingSaturdaysFlyoutComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);

  readonly termInstanceId = input.required<number>();

  readonly closed = output<void>();
  readonly saved = output<void>();

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly weeks: { value: WeekOfMonth; label: string; checked: boolean }[] = [
    { value: 'FIRST', label: '1st Saturday', checked: false },
    { value: 'SECOND', label: '2nd Saturday', checked: false },
    { value: 'THIRD', label: '3rd Saturday', checked: false },
    { value: 'FOURTH', label: '4th Saturday', checked: false },
    { value: 'LAST', label: 'Last Saturday of the month', checked: false },
  ];

  ngOnInit(): void {
    this.academicYearService.getWorkingSaturdays(this.termInstanceId()).subscribe({
      next: (configured) => {
        for (const week of this.weeks) {
          week.checked = configured.includes(week.value);
        }
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load working-Saturday settings');
        this.loading.set(false);
      },
    });
  }

  protected toggle(week: { checked: boolean }): void {
    week.checked = !week.checked;
  }

  protected save(): void {
    this.saving.set(true);
    const selected = this.weeks.filter((w) => w.checked).map((w) => w.value);
    this.academicYearService.updateWorkingSaturdays(this.termInstanceId(), selected).subscribe({
      next: () => {
        this.toast.success(selected.length > 0
          ? 'Working Saturdays updated — automation can now use those Saturdays'
          : 'Saturday scheduling turned off for this term');
        this.saving.set(false);
        this.saved.emit();
      },
      error: () => {
        this.toast.error('Failed to save working-Saturday settings');
        this.saving.set(false);
      },
    });
  }
}
