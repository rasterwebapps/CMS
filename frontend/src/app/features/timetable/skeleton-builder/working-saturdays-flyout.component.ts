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
  /** Mirrors the toolbar's own "Run Automation" gate (permission + a real single cohort selected,
   *  not "All Cohorts") -- controls whether the post-save follow-up offers that button at all. */
  readonly canRunAutomation = input<boolean>(false);

  readonly closed = output<void>();
  readonly saved = output<void>();
  /** Emitted alongside {@link saved} only when the admin picks "Run Automation now" on the
   *  post-save follow-up, so the parent can open the Global Auto-Schedule flyout immediately
   *  instead of leaving the admin to find the toolbar button themselves. */
  readonly runAutomation = output<void>();

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  /** True right after a successful save -- swaps the footer from Close/Save to a follow-up
   *  offering "Run Automation now" (when {@link canRunAutomation} allows it), since a changed
   *  Saturday pattern is the single most common reason to re-run automation right away. */
  protected readonly saveSucceeded = signal(false);
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
        this.saveSucceeded.set(true);
      },
      error: () => {
        this.toast.error('Failed to save working-Saturday settings');
        this.saving.set(false);
      },
    });
  }

  /** "Close" on the post-save follow-up -- the pattern changed, so the parent still needs to
   *  reload the skeleton even if the admin isn't running automation right this moment. */
  protected finish(): void {
    this.saved.emit();
  }

  protected runNow(): void {
    this.saved.emit();
    this.runAutomation.emit();
  }
}
