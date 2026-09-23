import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

// Local-time-only date math throughout -- .toISOString() converts to UTC, which silently shifts
// the date backward a day for any positive UTC offset (e.g. IST) when starting from local
// midnight, exactly the kind of off-by-one that's easy to miss without a same-day regression test.
function toIso(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function mondayOf(date: Date): Date {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const day = d.getDay(); // 0=Sunday..6=Saturday
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  return d;
}

function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  date.setDate(date.getDate() + days);
  return toIso(date);
}

/**
 * Prev/Next/Today week navigator for the Date-wise-weekly view — shared by the browse-all
 * timetable and both My Timetable (Student/Staff) screens so week navigation looks and behaves
 * identically everywhere it appears. Purely presentational: it only ever emits a candidate Monday
 * ISO date via {@link weekStartChange}; the parent owns loading and clamping to the selected
 * term's real bounds (same as the existing "Week of" picker on the Generic view already does).
 */
@Component({
  selector: 'cms-week-navigator',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './week-navigator.component.html',
  styleUrl: './week-navigator.component.scss',
})
export class CmsWeekNavigatorComponent {
  private readonly _weekStart = signal<string>(toIso(mondayOf(new Date())));
  @Input({ required: true }) set weekStart(value: string) {
    this._weekStart.set(value);
  }
  get weekStart(): string {
    return this._weekStart();
  }

  /** Term bounds -- prev/next disable rather than silently clamp once the adjacent week would
   *  fall outside the selected term, since a term the Date-wise-weekly view is scoped to never
   *  has real occurrences before/after these dates. Signal-backed (not plain fields) so the
   *  computed()s below actually re-evaluate when a parent updates them post-init -- a computed()
   *  only tracks other signals it reads, never a plain @Input() property. */
  private readonly _min = signal<string | null>(null);
  @Input() set min(value: string | null) {
    this._min.set(value);
  }
  get min(): string | null {
    return this._min();
  }

  private readonly _max = signal<string | null>(null);
  @Input() set max(value: string | null) {
    this._max.set(value);
  }
  get max(): string | null {
    return this._max();
  }

  @Output() weekStartChange = new EventEmitter<string>();

  protected readonly weekEnd = computed(() => addDays(this._weekStart(), 5));

  protected readonly rangeLabel = computed(() => {
    const start = new Date(`${this._weekStart()}T00:00:00`);
    const end = new Date(`${this.weekEnd()}T00:00:00`);
    const startLabel = start.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
    const endLabel = end.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
    return `${startLabel} – ${endLabel}`;
  });

  protected readonly canGoPrevious = computed(() => {
    const prev = addDays(this._weekStart(), -7);
    const min = this._min();
    return !min || prev >= min;
  });

  protected readonly canGoNext = computed(() => {
    const next = addDays(this._weekStart(), 7);
    const max = this._max();
    return !max || next <= max;
  });

  protected previousWeek(): void {
    if (!this.canGoPrevious()) return;
    this.weekStartChange.emit(addDays(this._weekStart(), -7));
  }

  protected nextWeek(): void {
    if (!this.canGoNext()) return;
    this.weekStartChange.emit(addDays(this._weekStart(), 7));
  }

  protected today(): void {
    let iso = toIso(mondayOf(new Date()));
    const min = this._min();
    const max = this._max();
    // Realigning to min/max's own Monday (rather than the raw boundary date) keeps the emitted
    // weekStart Monday-aligned even when the term starts/ends mid-week -- clamping straight to a
    // mid-week boundary produces a Mon-Sat window that never reaches one or more weekdays, which
    // then render as silently blank instead of their real sessions or a cancellation marker.
    if (min && iso < min) iso = toIso(mondayOf(new Date(`${min}T00:00:00`)));
    if (max && iso > max) iso = toIso(mondayOf(new Date(`${max}T00:00:00`)));
    this.weekStartChange.emit(iso);
  }
}
