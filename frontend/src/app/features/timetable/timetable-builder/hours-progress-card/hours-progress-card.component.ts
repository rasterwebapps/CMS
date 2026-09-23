import { Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export type HoursProgressCardVariant = 'term' | 'theory' | 'lab' | 'clinical';

/** A single "hours progress" stat card — icon + label, an optional unassigned-hours badge, a big
 *  value against its total with a progress bar, and up to two caption lines below (a status line
 *  and a bold "beyond curriculum" line). Purpose-built for Timetable Builder's term/Theory/Lab/
 *  Clinical hour summary (2026-09-21 redesign, replacing the old plain stat-grid boxes) — no
 *  existing card combines a progress bar with this badge+caption shape, so this is a new component
 *  rather than a reuse of {@code DashboardKpiCardComponent} (simpler icon+value+subtitle layout, no
 *  progress bar, and its color tokens live in the dashboard module only). */
@Component({
  selector: 'app-hours-progress-card',
  standalone: true,
  imports: [DecimalPipe, MatIconModule],
  templateUrl: './hours-progress-card.component.html',
  styleUrl: './hours-progress-card.component.scss',
})
export class HoursProgressCardComponent {
  readonly label = input.required<string>();
  readonly icon = input.required<string>();
  readonly variant = input.required<HoursProgressCardVariant>();
  readonly value = input.required<number>();
  readonly total = input.required<number>();
  /** "available" for the term-load card, "total" for Theory/Lab/Clinical — the word right after
   *  "of {{ total }}h". */
  readonly totalSuffix = input<string>('total');
  readonly unassigned = input<number>(0);
  readonly extra = input<number>(0);
  /** Only the term-load card passes this (e.g. "820h required · fully scheduled") — its presence
   *  is also what gates showing the green "0h unassigned" badge when nothing's outstanding, so a
   *  Theory/Lab/Clinical card with a clean 0 stays badge-free, matching the reference design. */
  readonly requiredCaption = input<string | null>(null);

  protected readonly progressPercent = computed(() => {
    const total = this.total();
    if (total <= 0) return 0;
    return Math.min(100, Math.max(0, (this.value() / total) * 100));
  });

  protected readonly hasUnassigned = computed(() => this.unassigned() > 0.05);
  protected readonly hasExtra = computed(() => this.extra() > 0.05);
  protected readonly hasCaption = computed(() => this.requiredCaption() !== null || this.hasExtra());
}
