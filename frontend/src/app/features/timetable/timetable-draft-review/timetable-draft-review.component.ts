import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule } from '../timetable.model';
import { CmsWeekGridComponent } from '../../../shared/week-grid/week-grid.component';
import { WeekGridSession } from '../../../shared/week-grid/week-grid.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-timetable-draft-review',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatProgressSpinnerModule, CmsWeekGridComponent],
  templateUrl: './timetable-draft-review.component.html',
  styleUrl: './timetable-draft-review.component.scss',
})
export class TimetableDraftReviewComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly sessions = signal<ClassSchedule[]>([]);
  protected readonly unplaceable = signal<string[]>([]);
  protected readonly loading = signal(false);
  protected readonly generating = signal(false);
  protected readonly saving = signal(false);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected canGenerate(): boolean {
    return this.permissionService.has('TIMETABLE_GENERATE');
  }

  protected canManage(): boolean {
    return this.permissionService.has('TIMETABLE_MANAGE');
  }

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
    this.unplaceable.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.unplaceable.set([]);
    if (this.selectedTermInstanceId) this.loadDraft(this.selectedTermInstanceId);
    else this.sessions.set([]);
  }

  protected onGenerate(): void {
    if (!this.selectedTermInstanceId) return;
    this.generating.set(true);
    this.timetableService.generate(this.selectedTermInstanceId).subscribe({
      next: (response) => {
        this.unplaceable.set(response.unplaceable);
        this.toast.success(`Generated ${response.generatedCount} session(s)`);
        this.loadDraft(this.selectedTermInstanceId!);
        this.generating.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to generate timetable');
        this.generating.set(false);
      },
    });
  }

  protected onApprove(): void {
    if (!this.selectedTermInstanceId) return;
    this.saving.set(true);
    this.timetableService.approve(this.selectedTermInstanceId).subscribe({
      next: (response) => {
        this.toast.success(`Approved ${response.affectedCount} session(s) — timetable is now live`);
        this.sessions.set([]);
        this.unplaceable.set([]);
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to approve timetable');
        this.saving.set(false);
      },
    });
  }

  protected onDiscard(): void {
    if (!this.selectedTermInstanceId) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Discard Draft Timetable',
        message: 'This permanently deletes every draft session generated for this term. This cannot be undone.',
        confirmText: 'Discard',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doDiscard();
    });
  }

  private doDiscard(): void {
    this.saving.set(true);
    this.timetableService.clear(this.selectedTermInstanceId!).subscribe({
      next: () => {
        this.toast.success('Draft discarded');
        this.sessions.set([]);
        this.unplaceable.set([]);
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to discard draft');
        this.saving.set(false);
      },
    });
  }

  protected onSessionClick(session: WeekGridSession): void {
    void session; // manual fixup happens via the extended Lab Schedule form; no in-grid edit in v1
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
        if (preselect) this.loadDraft(preselect);
        else this.sessions.set([]);
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private loadDraft(termInstanceId: number): void {
    this.loading.set(true);
    this.timetableService.getDraft(termInstanceId).subscribe({
      next: (data) => { this.sessions.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load draft timetable'); this.loading.set(false); },
    });
  }
}
