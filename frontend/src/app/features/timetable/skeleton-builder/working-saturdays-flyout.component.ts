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
  /** The term's real date range, used purely to show how many actual Saturdays each pattern yields
   *  (see {@link saturdayDatesByWeek}). Null-tolerant: the counts are simply hidden if the parent
   *  hasn't loaded the term yet, never guessed at. */
  readonly termStartDate = input<string | null>(null);
  readonly termEndDate = input<string | null>(null);
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

  /** Every Saturday falling inside the term, or an empty list when the parent hasn't supplied the
   *  date range. Parsed field-by-field rather than via `new Date(iso)` so a 'YYYY-MM-DD' string is
   *  read as a local calendar date — `Date.parse` treats the bare form as UTC, which shifts the day
   *  by one for anyone east of Greenwich and would mis-bucket Saturdays near a month boundary. */
  private allSaturdays(): Date[] {
    const start = this.parseLocalDate(this.termStartDate());
    const end = this.parseLocalDate(this.termEndDate());
    if (!start || !end || start > end) {
      return [];
    }
    const cursor = new Date(start);
    cursor.setDate(cursor.getDate() + ((6 - cursor.getDay() + 7) % 7)); // first Saturday on/after start
    const saturdays: Date[] = [];
    while (cursor <= end) {
      saturdays.push(new Date(cursor));
      cursor.setDate(cursor.getDate() + 7);
    }
    return saturdays;
  }

  private parseLocalDate(iso: string | null): Date | null {
    if (!iso) {
      return null;
    }
    const [year, month, day] = iso.split('-').map(Number);
    return Number.isFinite(year) && Number.isFinite(month) && Number.isFinite(day)
      ? new Date(year, month - 1, day)
      : null;
  }

  /** Mirrors the backend's `WorkingSaturdayCalculator.matches` exactly — same ordinal arithmetic and
   *  the same rule that LAST also covers a rare 5th Saturday, so the count shown here can never
   *  disagree with what automation will actually treat as a working day. */
  private matchesWeek(date: Date, week: WeekOfMonth): boolean {
    const ordinal = Math.floor((date.getDate() - 1) / 7) + 1;
    const lengthOfMonth = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
    if (week === 'LAST') {
      return date.getDate() + 7 > lengthOfMonth;
    }
    const byOrdinal: Record<number, WeekOfMonth> = { 1: 'FIRST', 2: 'SECOND', 3: 'THIRD', 4: 'FOURTH' };
    return byOrdinal[ordinal] === week;
  }

  protected totalSaturdays(): number {
    return this.allSaturdays().length;
  }

  /** How many real Saturdays this one pattern yields across the term — the number an admin needs in
   *  order to read "1st Saturday" as "about 6 days", not "every Saturday". */
  protected countForWeek(week: WeekOfMonth): number {
    return this.allSaturdays().filter((d) => this.matchesWeek(d, week)).length;
  }

  /** Distinct Saturdays covered by the current tick-state — deliberately de-duplicated, since LAST
   *  overlaps FOURTH (or FIFTH) in most months and naively summing the per-pattern counts would
   *  overstate the total. */
  protected selectedSaturdayCount(): number {
    const selected = this.weeks.filter((w) => w.checked).map((w) => w.value);
    if (selected.length === 0) {
      return 0;
    }
    return this.allSaturdays().filter((d) => selected.some((w) => this.matchesWeek(d, w))).length;
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
