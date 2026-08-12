import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { ConflictInspectorService } from './conflict-inspector.service';
import { ConflictScanResponse } from './conflict-inspector.model';

@Component({
  selector: 'app-conflict-inspector',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, CmsEmptyStateComponent, CmsStatusBadgeComponent],
  templateUrl: './conflict-inspector.component.html',
  styleUrl: './conflict-inspector.component.scss',
})
export class ConflictInspectorComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly conflictInspectorService = inject(ConflictInspectorService);
  private readonly toast = inject(ToastService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly scan = signal<ConflictScanResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

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
    this.scan.set(null);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    if (this.selectedTermInstanceId) this.runScan();
    else this.scan.set(null);
  }

  protected onRescan(): void {
    if (this.selectedTermInstanceId) this.runScan();
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
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
