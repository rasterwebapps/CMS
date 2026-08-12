import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../core/toast/toast.service';
import { FacultyWorkloadRulesService } from './faculty-workload-rules.service';
import { FacultyWorkloadRules } from './faculty-workload-rules.model';

/** Scoped editor for the three global timetable.faculty_max_*_hours values -- per-designation and
 *  per-faculty overrides are intentionally NOT duplicated here; they stay editable on the
 *  Designation Master / Faculty Master forms this screen links out to. */
@Component({
  selector: 'app-faculty-workload-rules',
  standalone: true,
  imports: [FormsModule, RouterLink, MatProgressSpinnerModule],
  templateUrl: './faculty-workload-rules.component.html',
  styleUrl: './faculty-workload-rules.component.scss',
})
export class FacultyWorkloadRulesComponent implements OnInit {
  private readonly rulesService = inject(FacultyWorkloadRulesService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected maxDailyHours: number | null = null;
  protected maxWeeklyHours: number | null = null;
  protected maxContinuousHours: number | null = null;

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.rulesService.get().subscribe({
      next: (rules) => { this.applyRules(rules); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load workload rules'); this.loading.set(false); },
    });
  }

  private applyRules(rules: FacultyWorkloadRules): void {
    this.maxDailyHours = rules.maxDailyHours;
    this.maxWeeklyHours = rules.maxWeeklyHours;
    this.maxContinuousHours = rules.maxContinuousHours;
  }

  protected hasNegative(): boolean {
    return [this.maxDailyHours, this.maxWeeklyHours, this.maxContinuousHours].some((v) => v != null && v < 0);
  }

  protected save(): void {
    if (this.hasNegative()) return;
    this.saving.set(true);
    this.rulesService.update({
      maxDailyHours: this.maxDailyHours,
      maxWeeklyHours: this.maxWeeklyHours,
      maxContinuousHours: this.maxContinuousHours,
    }).subscribe({
      next: (rules) => {
        this.applyRules(rules);
        this.toast.success('Faculty workload rules saved');
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save workload rules');
        this.saving.set(false);
      },
    });
  }

  protected cancel(): void {
    this.load();
  }
}
