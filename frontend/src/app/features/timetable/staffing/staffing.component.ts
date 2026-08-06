import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { ClassroomService } from '../../classroom/classroom.service';
import { Classroom } from '../../classroom/classroom.model';
import { StaffingService } from './staffing.service';
import { UnstaffedCell } from './staffing.model';
import { WEEK_GRID_DAY_LABELS } from '../../../shared/week-grid/week-grid.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments/environment';

interface StaffingRow extends UnstaffedCell {
  facultyId: number | null;
  roomId: number | null;
  saving: boolean;
}

@Component({
  selector: 'app-staffing',
  standalone: true,
  imports: [FormsModule, RouterLink, MatProgressSpinnerModule],
  templateUrl: './staffing.component.html',
  styleUrl: './staffing.component.scss',
})
export class StaffingComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly classroomService = inject(ClassroomService);
  private readonly staffingService = inject(StaffingService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly http = inject(HttpClient);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly rows = signal<StaffingRow[]>([]);

  protected readonly faculty = signal<{ id: number; name: string; specialityId: number | null }[]>([]);
  protected readonly classrooms = signal<Classroom[]>([]);

  /** Non-binding faculty-reuse tally, keyed by subjectCode|dayOfWeek|facultyId — incremented as
   *  assignments succeed in this session, used only to rank/hint the faculty dropdown toward
   *  reusing an instructor already teaching this subject that day, minimizing headcount. Never
   *  restricts the option list. */
  protected readonly facultyReuseCounts = signal<Map<string, number>>(new Map());

  protected readonly termsLoading = signal(false);
  protected readonly rowsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  protected canManage(): boolean {
    return this.permissionService.has('TIMETABLE_STAFFING_MANAGE');
  }

  ngOnInit(): void {
    this.http.get<{ id: number; fullName: string; specialityId: number | null }[]>(`${environment.apiUrl}/faculty`).subscribe({
      next: (data) => this.faculty.set(data.map((f) => ({ id: f.id, name: f.fullName, specialityId: f.specialityId }))),
      error: () => this.toast.error('Failed to load faculty'),
    });
    this.classroomService.getAll(true).subscribe({
      next: (data) => this.classrooms.set(data),
      error: () => this.toast.error('Failed to load classrooms'),
    });

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.rows.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    if (this.selectedTermInstanceId) this.loadRows(this.selectedTermInstanceId);
    else this.rows.set([]);
  }

  protected eligibleFacultyFor(row: StaffingRow): { id: number; name: string; specialityId: number | null }[] {
    const base = !row.subjectSpecialityId ? this.faculty()
      : this.faculty().filter((f) => f.specialityId === row.subjectSpecialityId);
    const counts = this.facultyReuseCounts();
    return [...base].sort((a, b) =>
      (counts.get(this.reuseKey(row, b.id)) ?? 0) - (counts.get(this.reuseKey(row, a.id)) ?? 0));
  }

  /** Appends a non-binding hint when this faculty is already teaching the same subject on the
   *  same day elsewhere in this staffing session — reusing them keeps total instructor headcount
   *  down instead of spreading the same subject across more staff than necessary. */
  protected facultyOptionLabel(row: StaffingRow, f: { name: string; id: number }): string {
    const count = this.facultyReuseCounts().get(this.reuseKey(row, f.id)) ?? 0;
    if (count === 0) return f.name;
    return `${f.name} — already teaching ${count} session${count > 1 ? 's' : ''} today`;
  }

  private reuseKey(row: StaffingRow, facultyId: number): string {
    return `${row.subjectCode}|${row.dayOfWeek}|${facultyId}`;
  }

  protected roomLabel(row: StaffingRow): string {
    if (row.sessionType === 'THEORY') return 'Classroom';
    if (row.sessionType === 'LAB') return 'Lab';
    return 'Clinical Venue';
  }

  /** Classroom-only now — LAB/CLINICAL rows no longer show a picker, they display the venue
   *  already committed to their batch in Capacity Planner. Best-fit-first: classrooms that seat
   *  the required strength sort first (tightest fit first), then unknown capacity, then
   *  undersized (closest to fitting first) — never removed from the list, just ranked, since the
   *  backend hard-blocks an actual over-capacity save regardless of what's shown here. */
  protected roomOptionsFor(row: StaffingRow): { id: number; name: string; capacity: number | null }[] {
    const base = this.classrooms().map((c) => ({ id: c.id, name: c.name, capacity: c.capacity ?? null }));

    const required = row.requiredStrength;
    if (required == null) return base;

    const fits = base.filter((o) => o.capacity != null && o.capacity >= required)
      .sort((a, b) => a.capacity! - b.capacity!);
    const unknown = base.filter((o) => o.capacity == null);
    const tooSmall = base.filter((o) => o.capacity != null && o.capacity < required)
      .sort((a, b) => b.capacity! - a.capacity!);
    return [...fits, ...unknown, ...tooSmall];
  }

  protected roomOptionLabel(row: StaffingRow, o: { name: string; capacity: number | null }): string {
    if (o.capacity == null) return o.name;
    const tooSmall = row.requiredStrength != null && o.capacity < row.requiredStrength;
    return tooSmall ? `${o.name} (Cap ${o.capacity} — too small)` : `${o.name} (Cap ${o.capacity})`;
  }

  /** Only an elective THEORY session still gets a free room pick here — everything else
   *  (LAB, CLINICAL, and non-elective THEORY) is hard-locked to whatever was committed in
   *  Cohort Room Allocation, so its room is shown as fixed text instead of a picker. */
  protected isFreelyPickedRoom(row: StaffingRow): boolean {
    return row.sessionType === 'THEORY' && row.isElective;
  }

  protected assign(row: StaffingRow): void {
    if (!row.facultyId) { this.toast.error('Pick a faculty member first'); return; }
    if (this.isFreelyPickedRoom(row) && !row.roomId) { this.toast.error('Pick a classroom first'); return; }
    if (!this.isFreelyPickedRoom(row) && !row.venueId) {
      this.toast.error(`No ${this.roomLabel(row).toLowerCase()} committed for this — commit it in Capacity Planner first`);
      return;
    }

    row.saving = true;
    this.staffingService.staffCell(row.id, {
      facultyId: row.facultyId,
      classroomId: this.isFreelyPickedRoom(row) ? row.roomId : null,
    }).subscribe({
      next: () => {
        this.toast.success(`Staffed ${row.subjectName}`);
        const key = this.reuseKey(row, row.facultyId!);
        this.facultyReuseCounts.update((m) => {
          const next = new Map(m);
          next.set(key, (next.get(key) ?? 0) + 1);
          return next;
        });
        this.rows.update((list) => list.filter((r) => r.id !== row.id));
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to assign faculty/room');
        row.saving = false;
      },
    });
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        if (this.selectedTermInstanceId) this.loadRows(this.selectedTermInstanceId);
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private loadRows(termInstanceId: number): void {
    this.rowsLoading.set(true);
    this.staffingService.getUnstaffedCells(termInstanceId).subscribe({
      next: (cells) => {
        this.rows.set(cells.map((c) => ({ ...c, facultyId: null, roomId: null, saving: false })));
        this.rowsLoading.set(false);
      },
      error: () => { this.toast.error('Failed to load unstaffed sessions'); this.rowsLoading.set(false); },
    });
  }
}
