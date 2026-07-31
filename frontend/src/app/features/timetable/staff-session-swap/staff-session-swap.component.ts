import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassScheduleOccurrence, StaffSwapCandidate } from '../timetable.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';

@Component({
  selector: 'app-staff-session-swap',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatProgressSpinnerModule, CmsEmptyStateComponent],
  templateUrl: './staff-session-swap.component.html',
  styleUrl: './staff-session-swap.component.scss',
})
export class StaffSessionSwapComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly toast = inject(ToastService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);

  protected date = new Date().toISOString().slice(0, 10);
  protected readonly occurrences = signal<ClassScheduleOccurrence[]>([]);
  protected readonly loading = signal(false);

  protected readonly swapSourceId = signal<number | null>(null);
  protected readonly candidates = signal<StaffSwapCandidate[]>([]);
  protected readonly loadingCandidates = signal(false);
  protected readonly applyingCandidateId = signal<number | null>(null);

  ngOnInit(): void {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.occurrences.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    const term = this.selectedTerm();
    if (term) this.date = this.clampToTerm(this.date, term);
    this.loadDay();
  }

  protected onDateChange(): void {
    this.date = this.clampToTerm(this.date, this.selectedTerm());
    this.loadDay();
  }

  protected get dateMin(): string | null {
    return this.selectedTerm()?.startDate ?? null;
  }

  protected get dateMax(): string | null {
    return this.selectedTerm()?.endDate ?? null;
  }

  private clampToTerm(date: string, term: TermInstance | null): string {
    if (!term) return date;
    if (date < term.startDate) return term.startDate;
    if (date > term.endDate) return term.endDate;
    return date;
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        if (terms[0]) this.date = this.clampToTerm(this.date, terms[0]);
        this.loadDay();
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private loadDay(): void {
    if (!this.selectedTermInstanceId) { this.occurrences.set([]); return; }
    this.loading.set(true);
    this.swapSourceId.set(null);
    this.candidates.set([]);
    this.timetableService.getOccurrences(this.selectedTermInstanceId, this.date, this.date, 'browse').subscribe({
      next: (occs) => { this.occurrences.set(occs); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load sessions for this date'); this.loading.set(false); },
    });
  }

  protected findSwapPartners(classScheduleId: number): void {
    this.swapSourceId.set(classScheduleId);
    this.candidates.set([]);
    this.loadingCandidates.set(true);
    this.timetableService.getStaffSwapCandidates(classScheduleId, this.date).subscribe({
      next: (list) => { this.candidates.set(list); this.loadingCandidates.set(false); },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load swap candidates');
        this.loadingCandidates.set(false);
      },
    });
  }

  protected cancelSwap(): void {
    this.swapSourceId.set(null);
    this.candidates.set([]);
  }

  protected applySwap(targetClassScheduleId: number): void {
    const sourceId = this.swapSourceId();
    if (!sourceId) return;
    this.applyingCandidateId.set(targetClassScheduleId);
    this.timetableService.applyStaffSwap(sourceId, targetClassScheduleId, this.date).subscribe({
      next: () => {
        this.toast.success('Staff swapped for this date');
        this.applyingCandidateId.set(null);
        this.cancelSwap();
        this.loadDay();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to apply swap');
        this.applyingCandidateId.set(null);
      },
    });
  }
}
