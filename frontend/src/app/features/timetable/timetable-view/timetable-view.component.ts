import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule } from '../timetable.model';
import { CmsWeekGridComponent } from '../../../shared/week-grid/week-grid.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-timetable-view',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, CmsWeekGridComponent],
  templateUrl: './timetable-view.component.html',
  styleUrl: './timetable-view.component.scss',
})
export class TimetableViewComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly sessions = signal<ClassSchedule[]>([]);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly selectedFaculty = signal<string | null>(null);
  protected readonly selectedRoom = signal<string | null>(null);
  protected readonly selectedBatch = signal<string | null>(null);

  /** Filter option lists are derived from the term's own loaded sessions rather than fetched
   *  from the Faculty/Classroom/Lab/Batch masters — keeps the dropdowns scoped to only what's
   *  actually scheduled this term instead of every faculty/room in the college. */
  protected readonly facultyOptions = computed(() =>
    Array.from(new Set(this.sessions().map((s) => s.facultyName))).sort());

  protected readonly roomOptions = computed(() =>
    Array.from(new Set(this.sessions().map((s) => s.roomName))).sort());

  protected readonly batchOptions = computed(() =>
    Array.from(new Set(this.sessions().flatMap((s) => s.batchName ? [s.batchName] : []))).sort());

  protected readonly filteredSessions = computed(() => {
    const faculty = this.selectedFaculty();
    const room = this.selectedRoom();
    const batch = this.selectedBatch();
    return this.sessions().filter((s) =>
      (!faculty || s.facultyName === faculty) &&
      (!room || s.roomName === room) &&
      (!batch || s.batchName === batch));
  });

  ngOnInit(): void {
    const qpAcademicYearId = Number(this.route.snapshot.queryParamMap.get('academicYearId')) || null;
    const qpTermInstanceId = Number(this.route.snapshot.queryParamMap.get('termInstanceId')) || null;

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = qpAcademicYearId
          ?? years.find((y) => y.isCurrent)?.id
          ?? years[0]?.id
          ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId, qpTermInstanceId ?? undefined);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.sessions.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    if (this.selectedTermInstanceId) this.loadPublished(this.selectedTermInstanceId);
    else this.sessions.set([]);
  }

  private loadTermInstances(academicYearId: number, preselectTermInstanceId?: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        const preselect = preselectTermInstanceId && terms.some((t) => t.id === preselectTermInstanceId)
          ? preselectTermInstanceId
          : terms[0]?.id ?? null;
        this.selectedTermInstanceId = preselect;
        if (preselect) this.loadPublished(preselect);
        else this.sessions.set([]);
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private loadPublished(termInstanceId: number): void {
    this.loading.set(true);
    this.resetFilters();
    this.timetableService.getPublished(termInstanceId).subscribe({
      next: (data) => { this.sessions.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load timetable'); this.loading.set(false); },
    });
  }

  private resetFilters(): void {
    this.selectedFaculty.set(null);
    this.selectedRoom.set(null);
    this.selectedBatch.set(null);
  }
}
