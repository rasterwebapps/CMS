import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { ClassroomService } from '../../classroom/classroom.service';
import { Classroom } from '../../classroom/classroom.model';
import { ClinicalVenueService } from '../../clinical-venue/clinical-venue.service';
import { ClinicalVenue } from '../../clinical-venue/clinical-venue.model';
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
  imports: [FormsModule, MatProgressSpinnerModule],
  templateUrl: './staffing.component.html',
  styleUrl: './staffing.component.scss',
})
export class StaffingComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly classroomService = inject(ClassroomService);
  private readonly clinicalVenueService = inject(ClinicalVenueService);
  private readonly staffingService = inject(StaffingService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly http = inject(HttpClient);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly rows = signal<StaffingRow[]>([]);

  protected readonly faculty = signal<{ id: number; name: string; specialityId: number | null }[]>([]);
  protected readonly classrooms = signal<Classroom[]>([]);
  protected readonly labs = signal<{ id: number; name: string }[]>([]);
  protected readonly clinicalVenues = signal<ClinicalVenue[]>([]);

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
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/labs`).subscribe({
      next: (data) => this.labs.set(data),
      error: () => this.toast.error('Failed to load labs'),
    });
    this.clinicalVenueService.getAll(true).subscribe({
      next: (data) => this.clinicalVenues.set(data),
      error: () => this.toast.error('Failed to load clinical venues'),
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
    if (!row.subjectSpecialityId) return this.faculty();
    return this.faculty().filter((f) => f.specialityId === row.subjectSpecialityId);
  }

  protected roomLabel(row: StaffingRow): string {
    if (row.sessionType === 'THEORY') return 'Classroom';
    if (row.sessionType === 'LAB') return 'Lab';
    return 'Clinical Venue';
  }

  protected roomOptionsFor(row: StaffingRow): { id: number; name: string }[] {
    if (row.sessionType === 'THEORY') return this.classrooms();
    if (row.sessionType === 'LAB') return this.labs();
    return this.clinicalVenues();
  }

  protected assign(row: StaffingRow): void {
    if (!row.facultyId) { this.toast.error('Pick a faculty member first'); return; }
    if (!row.roomId) { this.toast.error(`Pick a ${this.roomLabel(row).toLowerCase()} first`); return; }

    row.saving = true;
    this.staffingService.staffCell(row.id, {
      facultyId: row.facultyId,
      classroomId: row.sessionType === 'THEORY' ? row.roomId : null,
      labId: row.sessionType === 'LAB' ? row.roomId : null,
      clinicalVenueId: row.sessionType === 'CLINICAL' ? row.roomId : null,
    }).subscribe({
      next: () => {
        this.toast.success(`Staffed ${row.subjectName}`);
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
