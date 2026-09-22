import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { ConflictInspectorService } from './conflict-inspector.service';
import { ConflictScanResponse } from './conflict-inspector.model';
import { violationText } from '../../../shared/util/violation-text';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CONFLICT_INSPECTOR_TOUR, CONFLICT_INSPECTOR_FLOW_MAP } from '../../../shared/tour/tours/conflict-inspector.tours';
import { CmsInfiniteSelectComponent } from '../../../shared/infinite-select/infinite-select.component';
import { InfiniteSelectValue } from '../../../shared/infinite-select/infinite-select.model';
import { staticOptionsFetchPage } from '../../../shared/infinite-select/infinite-select.utils';

@Component({
  selector: 'app-conflict-inspector',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, CmsEmptyStateComponent, CmsStatusBadgeComponent, CmsTourButtonComponent, CmsInfiniteSelectComponent],
  templateUrl: './conflict-inspector.component.html',
  styleUrl: './conflict-inspector.component.scss',
})
export class ConflictInspectorComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly conflictInspectorService = inject(ConflictInspectorService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly scan = signal<ConflictScanResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);
  protected readonly proceeding = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected canProceedToReview(): boolean {
    return this.permissionService.has('TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE');
  }

  ngOnInit(): void {
    this.tourService.register('conflict-inspector', CONFLICT_INSPECTOR_TOUR);
    this.tourService.registerFlowMap('conflict-inspector', CONFLICT_INSPECTOR_FLOW_MAP);

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

  protected readonly academicYearFetchPage = staticOptionsFetchPage(() =>
    this.academicYears().map(ay => ({ id: ay.id, name: ay.name })));
  protected readonly termFetchPage = staticOptionsFetchPage(() =>
    this.termInstances().map(t => ({ id: t.id, name: `${t.termType} · ${t.status}` })));

  protected onAcademicYearChange(value: InfiniteSelectValue | null): void {
    this.selectedAcademicYearId = value != null ? Number(value) : null;
    this.selectedTermInstanceId = null;
    this.scan.set(null);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(value: InfiniteSelectValue | null): void {
    this.selectedTermInstanceId = value != null ? Number(value) : null;
    if (this.selectedTermInstanceId) this.runScan();
    else this.scan.set(null);
  }

  protected onRescan(): void {
    if (this.selectedTermInstanceId) this.runScan();
  }

  /** Only reachable once the scan is clean (see the template's disabled binding), but the backend
   *  re-validates independently — the scan on screen could already be stale by the time this is
   *  clicked (e.g. another tab placed a conflicting cell in the meantime). */
  protected onProceedToReview(): void {
    if (!this.selectedTermInstanceId) return;
    this.proceeding.set(true);
    this.conflictInspectorService.acknowledge(this.selectedTermInstanceId).subscribe({
      next: () => {
        this.proceeding.set(false);
        // OC-260 retired the separate Timetable Draft Review screen into Timetable Builder -- Publish
        // now lives there, gated per-cohort via that screen's own "Check & Resolve Conflicts" row
        // action rather than this term-wide acknowledgment (kept only as a term-wide diagnostic).
        this.router.navigate(['/timetable/timetable-builder']);
      },
      error: (err) => {
        this.proceeding.set(false);
        this.toast.error(violationText(err) ?? 'This term is no longer clean — rescan before proceeding');
        this.runScan();
      },
    });
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
        if (this.selectedTermInstanceId) this.runScan();
        else this.scan.set(null);
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private runScan(): void {
    this.loading.set(true);
    this.conflictInspectorService.scan(this.selectedTermInstanceId!).subscribe({
      next: (response) => { this.scan.set(response); this.loading.set(false); },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to scan this term for conflicts');
        this.loading.set(false);
      },
    });
  }
}
