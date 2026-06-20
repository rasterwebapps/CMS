import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FacultyService } from '../faculty.service';
import {
  FACULTY_DOCUMENT_REVIEW_FILTER_OPTIONS,
  FACULTY_STATUS_OPTIONS,
  Faculty,
  FacultyDocumentReviewFilter,
  FacultyDocumentReviewSummary,
  FacultyStatus,
} from '../faculty.model';
import { SpecialityService } from '../../speciality/speciality.service';
import { Speciality } from '../../speciality/speciality.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FACULTY_LIST_TOUR } from '../../../shared/tour/tours/faculty.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';

@Component({
  selector: 'app-faculty-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsStatusBadgeComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    RouterLink,
    TitleCasePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './faculty-list.component.html',
  styleUrl: './faculty-list.component.scss',
})
export class FacultyListComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly specialityService = inject(SpecialityService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'faculty-view-mode';

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns: readonly string[] = ['employeeCode', 'fullName', 'phone', 'email', 'specialityName', 'designation', 'status', 'documentReview', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Faculty>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allFaculty = signal<Faculty[]>([]);
  protected readonly specialities = signal<Speciality[]>([]);
  protected readonly selectedSpecialityId = signal<number | null>(null);
  protected readonly selectedStatus = signal<FacultyStatus | null>(null);
  protected readonly selectedDocumentReview = signal<FacultyDocumentReviewFilter>('ALL');
  protected readonly statusOptions = FACULTY_STATUS_OPTIONS;
  protected readonly documentReviewOptions = FACULTY_DOCUMENT_REVIEW_FILTER_OPTIONS;

  protected readonly filteredFaculty = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    const deptId = this.selectedSpecialityId();
    const status = this.selectedStatus();
    const deptName = deptId
      ? (this.specialities().find(d => d.id === deptId)?.name ?? '')
      : '';

    return this.allFaculty().filter(item => {
      if (deptName && !item.specialityName.toLowerCase().includes(deptName.toLowerCase())) return false;
      if (status && item.status !== status) return false;
      if (!this.matchesDocumentReview(item)) return false;
      if (q && !(
        item.fullName.toLowerCase().includes(q) ||
        item.employeeCode.toLowerCase().includes(q) ||
        (item.email ?? '').toLowerCase().includes(q) ||
        item.specialityName.toLowerCase().includes(q)
      )) return false;
      return true;
    });
  });

  protected readonly totalCount = computed(() => this.allFaculty().length);
  protected readonly activeCount = computed(() => this.allFaculty().filter(f => f.status === 'ACTIVE').length);

  protected readonly hasActiveFilters = computed(() =>
    this.selectedSpecialityId() !== null ||
    this.selectedStatus() !== null ||
    this.selectedDocumentReview() !== 'ALL' ||
    this.searchValue().length > 0,
  );

  ngOnInit(): void {
    this.tourService.register('faculty-list', FACULTY_LIST_TOUR);
    this.dataSource.sortingDataAccessor = (item, property) => {
      if (property === 'documentReview') return this.documentReviewSortValue(item);
      return String((item as unknown as Record<string, unknown>)[property] ?? '').toLowerCase();
    };
    this.loadSpecialities();
    this.loadFaculty();
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.syncTableData();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.syncTableData();
  }

  protected onSpecialityChange(specialityId: number | null): void {
    this.selectedSpecialityId.set(specialityId);
    this.syncTableData();
  }

  protected onDocumentReviewChange(value: FacultyDocumentReviewFilter): void {
    this.selectedDocumentReview.set(value);
    this.syncTableData();
  }

  protected onStatusChange(status: FacultyStatus | null): void {
    this.selectedStatus.set(status);
    this.syncTableData();
  }

  protected clearFilters(): void {
    this.selectedSpecialityId.set(null);
    this.selectedStatus.set(null);
    this.selectedDocumentReview.set('ALL');
    this.clearFilter();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  protected handleEmptyAction(): void {
    if (this.hasActiveFilters()) {
      this.clearFilters();
    } else {
      void this.router.navigate(['/faculty/new']);
    }
  }

  protected viewFaculty(faculty: Faculty, openDocuments = false): void {
    void this.router.navigate(['/faculty', faculty.id], {
      fragment: openDocuments ? 'documents' : undefined,
    });
  }

  protected viewDocuments(faculty: Faculty, event?: Event): void {
    event?.stopPropagation();
    this.viewFaculty(faculty, true);
  }

  protected editFaculty(faculty: Faculty): void {
    void this.router.navigate(['/faculty', faculty.id, 'edit']);
  }

  protected deleteFaculty(faculty: Faculty): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Faculty',
        message: `Are you sure you want to delete "${faculty.fullName}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(faculty);
    });
  }

  protected documentReviewBadge(faculty: Faculty): { label: string; className: string; tooltip: string } {
    const review = this.documentReview(faculty);
    if (review.rejectedCount > 0) {
      return {
        label: `Rejected: ${review.rejectedCount}`,
        className: 'doc-review-badge--rejected',
        tooltip: `${review.rejectedCount} document(s) rejected. Faculty must re-upload corrected documents.`,
      };
    }
    if (review.pendingVerificationCount > 0) {
      return {
        label: `Needs Verification: ${review.pendingVerificationCount}`,
        className: 'doc-review-badge--pending',
        tooltip: `${review.pendingVerificationCount} uploaded document(s) awaiting verification.`,
      };
    }
    if (review.missingRequiredCount > 0) {
      return {
        label: `Missing Required: ${review.missingRequiredCount}`,
        className: 'doc-review-badge--missing',
        tooltip: `${review.missingRequiredCount} required document(s) not uploaded.`,
      };
    }
    if (this.isFullyVerified(review)) {
      return {
        label: 'All Verified',
        className: 'doc-review-badge--verified',
        tooltip: 'All required documents are verified.',
      };
    }
    if (!review.hasAnyDocuments) {
      return {
        label: 'No Documents',
        className: 'doc-review-badge--empty',
        tooltip: 'No faculty documents have been uploaded.',
      };
    }
    return {
      label: 'Has Documents',
      className: 'doc-review-badge--neutral',
      tooltip: 'Documents exist for this faculty member.',
    };
  }

  private performDelete(faculty: Faculty): void {
    this.loading.set(true);
    this.facultyService.delete(faculty.id).subscribe({
      next: () => {
        this.toast.success('Faculty deleted successfully');
        this.loadFaculty();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete faculty');
        this.loading.set(false);
      },
    });
  }

  private loadSpecialities(): void {
    this.specialityService.getAll().subscribe({
      next: (specialities) => { this.specialities.set(specialities); },
      error: () => { this.toast.error('Failed to load specialities'); },
    });
  }

  private loadFaculty(): void {
    this.loading.set(true);
    this.facultyService.getAll().subscribe({
      next: (facultyList) => {
        this.allFaculty.set(facultyList);
        this.syncTableData();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load faculty');
        this.loading.set(false);
      },
    });
  }

  private syncTableData(): void {
    this.dataSource.data = this.filteredFaculty();
    this.dataSource.paginator?.firstPage();
  }

  private matchesDocumentReview(faculty: Faculty): boolean {
    const review = this.documentReview(faculty);
    switch (this.selectedDocumentReview()) {
      case 'NEEDS_VERIFICATION': return review.pendingVerificationCount > 0;
      case 'REJECTED': return review.rejectedCount > 0;
      case 'MISSING_REQUIRED': return review.missingRequiredCount > 0;
      case 'FULLY_VERIFIED': return this.isFullyVerified(review);
      case 'NO_DOCUMENTS': return !review.hasAnyDocuments;
      case 'HAS_ANY_DOCUMENTS': return review.hasAnyDocuments;
      case 'ALL':
      default: return true;
    }
  }

  private documentReviewSortValue(faculty: Faculty): string {
    const review = this.documentReview(faculty);
    if (review.rejectedCount > 0) return `1-${review.rejectedCount}`;
    if (review.pendingVerificationCount > 0) return `2-${review.pendingVerificationCount}`;
    if (review.missingRequiredCount > 0) return `3-${review.missingRequiredCount}`;
    if (this.isFullyVerified(review)) return '4-verified';
    if (!review.hasAnyDocuments) return '5-empty';
    return '6-documents';
  }

  private isFullyVerified(review: FacultyDocumentReviewSummary): boolean {
    return review.allRequiredDocumentsVerified &&
      review.pendingVerificationCount === 0 &&
      review.rejectedCount === 0 &&
      review.missingRequiredCount === 0;
  }

  private documentReview(faculty: Faculty): FacultyDocumentReviewSummary {
    return faculty.documentReview ?? {
      totalDocumentCount: 0,
      requiredDocumentCount: 0,
      pendingVerificationCount: 0,
      rejectedCount: 0,
      missingRequiredCount: 0,
      verifiedRequiredCount: 0,
      hasAnyDocuments: false,
      hasPendingVerification: false,
      hasRejectedDocuments: false,
      allRequiredDocumentsVerified: false,
    };
  }
}
