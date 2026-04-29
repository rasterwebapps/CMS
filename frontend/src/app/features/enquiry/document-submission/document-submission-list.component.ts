import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/toast/toast.service';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { DOCUMENT_SUBMISSION_LIST_TOUR } from '../../../shared/tour/tours/enquiry.tours';

@Component({
  selector: 'app-document-submission-list',
  standalone: true,
  imports: [
    InrPipe, AppDatePipe, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatTooltipModule,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './document-submission-list.component.html',
  styleUrl: './document-submission-list.component.scss',
})
export class DocumentSubmissionListComponent implements OnInit {
  private readonly enquiryService = inject(EnquiryService);
  private readonly authService    = inject(AuthService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly tourService    = inject(TourService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly loading    = signal(true);
  protected readonly searchQuery = signal('');
  protected colMenuOpen         = false;

  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  // ── Stats ─────────────────────────────────────────────────────────────────
  protected readonly totalCount        = computed(() => this.dataSource.data.length);
  protected readonly feesPaidCount     = computed(() => this.dataSource.data.filter(e => e.status === 'FEES_PAID').length);
  protected readonly partiallyPaidCount = computed(() => this.dataSource.data.filter(e => e.status === 'PARTIALLY_PAID').length);
  protected readonly filteredCount     = computed(() => this.dataSource.filteredData.length);

  // ── Column visibility ─────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['name', 'programName', 'courseName', 'status', 'finalizedNetFee', 'enquiryDate', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Student', programName: 'Program', courseName: 'Course',
    status: 'Payment Status', finalizedNetFee: 'Net Fee', enquiryDate: 'Date', actions: 'Actions',
  };
  private readonly COLS_KEY     = 'document-submission-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));

  protected readonly computeInitials = computeInitials;

  ngOnInit(): void {
    this.tourService.register('document-submission-list', DOCUMENT_SUBMISSION_LIST_TOUR);
    this.dataSource.filterPredicate = (row, filter) => {
      const q = filter.toLowerCase();
      return row.name.toLowerCase().includes(q) ||
        (row.programName ?? '').toLowerCase().includes(q) ||
        (row.courseName  ?? '').toLowerCase().includes(q) ||
        (row.phone       ?? '').includes(q);
    };
    this.load();
  }

  protected onSearch(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchQuery.set(val);
    this.dataSource.filter = val.trim().toLowerCase();
    this.dataSource.paginator?.firstPage();
  }

  protected clearSearch(): void {
    this.searchQuery.set('');
    this.dataSource.filter = '';
  }

  // ── Column prefs ──────────────────────────────────────────────────────────
  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.ALL_COLS);
  }

  protected toggleColumn(col: string): void {
    this._visibleCols.update(s => {
      const next = new Set(s);
      if (next.size > 1 && next.has(col)) { next.delete(col); } else { next.add(col); }
      localStorage.setItem(this.COLS_KEY, JSON.stringify([...next]));
      return next;
    });
  }

  protected isColumnVisible(col: string): boolean { return this._visibleCols().has(col); }

  protected isAdminOrFrontOffice(): boolean {
    return this.authService.hasRole('ROLE_ADMIN') ||
           this.authService.hasRole('ROLE_COLLEGE_ADMIN') ||
           this.authService.hasRole('ROLE_FRONT_OFFICE');
  }

  private load(): void {
    this.loading.set(true);
    this.enquiryService.getDocumentPending().subscribe({
      next:  enquiries => { this.dataSource.data = enquiries; this.loading.set(false); },
      error: ()        => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }

  protected viewEnquiry(item: Enquiry): void       { void this.router.navigate(['/enquiries', item.id]); }
  protected collectDocuments(item: Enquiry): void   { void this.router.navigate(['/enquiries/document-submission', item.id]); }
}
