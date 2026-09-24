import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../core/toast/toast.service';
import { FacultyWorkloadRulesService } from './faculty-workload-rules.service';
import { FacultyWorkloadRules } from './faculty-workload-rules.model';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { FACULTY_WORKLOAD_RULES_TOUR, FACULTY_WORKLOAD_RULES_FLOW_MAP } from '../../../shared/tour/tours/faculty-workload-rules.tours';

/** Scoped editor for the four global timetable.faculty_*_sessions values -- per-designation and
 *  per-faculty overrides are intentionally NOT duplicated here; they stay editable on the
 *  Designation Master / Faculty Master forms this screen links out to. */
@Component({
  selector: 'app-faculty-workload-rules',
  standalone: true,
  imports: [FormsModule, RouterLink, MatProgressSpinnerModule, CmsTourButtonComponent],
  templateUrl: './faculty-workload-rules.component.html',
  styleUrl: './faculty-workload-rules.component.scss',
})
export class FacultyWorkloadRulesComponent implements OnInit {
  private readonly rulesService = inject(FacultyWorkloadRulesService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected maxDailySessions: number | null = null;
  protected maxWeeklySessions: number | null = null;
  protected maxContinuousSessions: number | null = null;
  protected minWeeklySessions: number | null = null;

  ngOnInit(): void {
    this.tourService.register('faculty-workload-rules', FACULTY_WORKLOAD_RULES_TOUR);
    this.tourService.registerFlowMap('faculty-workload-rules', FACULTY_WORKLOAD_RULES_FLOW_MAP);
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
    this.maxDailySessions = rules.maxDailySessions;
    this.maxWeeklySessions = rules.maxWeeklySessions;
    this.maxContinuousSessions = rules.maxContinuousSessions;
    this.minWeeklySessions = rules.minWeeklySessions;
  }

  protected hasNegative(): boolean {
    return [this.maxDailySessions, this.maxWeeklySessions, this.maxContinuousSessions, this.minWeeklySessions]
      .some((v) => v != null && v < 0);
  }

  protected save(): void {
    if (this.hasNegative()) return;
    this.saving.set(true);
    this.rulesService.update({
      maxDailySessions: this.maxDailySessions,
      maxWeeklySessions: this.maxWeeklySessions,
      maxContinuousSessions: this.maxContinuousSessions,
      minWeeklySessions: this.minWeeklySessions,
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
