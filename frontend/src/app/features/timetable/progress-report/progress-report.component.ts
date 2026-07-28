import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ProgressTrackingService } from '../progress-tracking.service';
import { OfferingProgress, TermProgressSummary } from '../progress-tracking.model';

@Component({
  selector: 'app-progress-report',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, DecimalPipe],
  templateUrl: './progress-report.component.html',
  styleUrl: './progress-report.component.scss',
})
export class ProgressReportComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly progressService = inject(ProgressTrackingService);
  private readonly toast = inject(ToastService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly summary = signal<TermProgressSummary | null>(null);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected readonly expandedOfferingId = signal<number | null>(null);
  protected readonly offeringDetail = signal<OfferingProgress | null>(null);
  protected readonly detailLoading = signal(false);

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
    this.summary.set(null);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.load();
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        this.load();
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private load(): void {
    if (!this.selectedTermInstanceId) { this.summary.set(null); return; }
    this.loading.set(true);
    this.expandedOfferingId.set(null);
    this.progressService.getTermProgressSummary(this.selectedTermInstanceId).subscribe({
      next: (data) => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load progress summary'); this.loading.set(false); },
    });
  }

  protected toggleExpand(courseOfferingId: number): void {
    if (this.expandedOfferingId() === courseOfferingId) {
      this.expandedOfferingId.set(null);
      this.offeringDetail.set(null);
      return;
    }
    this.expandedOfferingId.set(courseOfferingId);
    this.detailLoading.set(true);
    this.progressService.getOfferingProgress(courseOfferingId).subscribe({
      next: (detail) => {
        this.offeringDetail.set(detail);
        this.detailLoading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load subject detail');
        this.detailLoading.set(false);
      },
    });
  }
}
