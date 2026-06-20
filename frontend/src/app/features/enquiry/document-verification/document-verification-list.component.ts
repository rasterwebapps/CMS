import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { TourService } from '../../../shared/tour/tour.service';
import { DOCUMENT_VERIFICATION_LIST_TOUR } from '../../../shared/tour/tours/enquiry.tours';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';

@Component({
  selector: 'app-document-verification-list',
  standalone: true,
  imports: [
    FormsModule, AppDatePipe,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
  ],
  templateUrl: './document-verification-list.component.html',
  styleUrl: './document-verification-list.component.scss',
})
export class DocumentVerificationListComponent implements OnInit {
  private readonly enquiryService   = inject(EnquiryService);
  private readonly permissionService = inject(PermissionService);
  private readonly router            = inject(Router);
  private readonly toast             = inject(ToastService);
  private readonly tourService       = inject(TourService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly loading     = signal(true);
  protected readonly searchQuery = signal('');
  protected colMenuOpen          = false;

  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);
  private readonly _allData     = signal<Enquiry[]>([]);

  protected readonly filterProgram     = signal<string>('ALL');
  protected readonly filterCourse      = signal<string>('ALL');
  protected readonly filterStudentType = signal<string>('ALL');
  protected readonly filterStatus      = signal<string>('ALL');

  protected readonly programs = computed(() =>
    [...new Set(this._allData().map(r => r.programName).filter(Boolean))].sort() as string[]
  );
  protected readonly courses = computed(() =>
    [...new Set(this._allData().map(r => r.courseName).filter(Boolean))].sort() as string[]
  );

  protected readonly VERIFICATION_STATUSES = [
    { value: 'DOCUMENTS_SUBMITTED', label: 'Submitted' },
    { value: 'DOCUMENTS_VERIFIED',  label: 'Verified'  },
  ];

  protected readonly totalCount    = computed(() => this._allData().length);
  protected readonly filteredCount = computed(() => this.dataSource.filteredData.length);

  protected readonly ALL_COLS = ['name', 'programName', 'courseName', 'studentType', 'enquiryDate', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Student', programName: 'Program', courseName: 'Course',
    studentType: 'Type', enquiryDate: 'Submitted', actions: 'Actions',
  };
  private readonly COLS_KEY     = 'doc-verification-list-cols-v1';
  private readonly DEFAULT_COLS = new Set(['name', 'programName', 'courseName', 'studentType', 'enquiryDate', 'actions']);
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));

  protected readonly computeInitials = computeInitials;

  ngOnInit(): void {
    this.tourService.register('document-verification-list', DOCUMENT_VERIFICATION_LIST_TOUR);
    this.dataSource.filterPredicate = (row: Enquiry, _filter: string) => {
      const program     = this.filterProgram();
      const course      = this.filterCourse();
      const studentType = this.filterStudentType();
      const q           = this.searchQuery().toLowerCase().trim();

      if (program     !== 'ALL' && (row.programName ?? '') !== program)     return false;
      if (course      !== 'ALL' && (row.courseName  ?? '') !== course)      return false;
      if (studentType !== 'ALL' && (row.studentType ?? '') !== studentType) return false;
      if (this.filterStatus() !== 'ALL' && row.status !== this.filterStatus()) return false;
      if (!q) return true;
      return row.name.toLowerCase().includes(q) ||
        (row.phone ?? '').includes(q) ||
        (row.email ?? '').toLowerCase().includes(q);
    };
    this.load();
  }

  private triggerFilter(): void {
    this.dataSource.filter = this.searchQuery() + '|' +
      this.filterProgram() + '|' + this.filterCourse() + '|' + this.filterStudentType() + '|' + this.filterStatus();
    this.dataSource.paginator?.firstPage();
  }

  protected onSearch(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
    this.triggerFilter();
  }

  protected clearSearch(): void {
    this.searchQuery.set('');
    this.triggerFilter();
  }

  protected onProgramChange(val: string): void     { this.filterProgram.set(val);     this.triggerFilter(); }
  protected onCourseChange(val: string): void       { this.filterCourse.set(val);      this.triggerFilter(); }
  protected onStudentTypeChange(val: string): void  { this.filterStudentType.set(val); this.triggerFilter(); }
  protected onStatusChange(val: string): void        { this.filterStatus.set(val);      this.triggerFilter(); }

  protected clearFilters(): void {
    this.filterProgram.set('ALL');
    this.filterCourse.set('ALL');
    this.filterStudentType.set('ALL');
    this.filterStatus.set('ALL');
    this.searchQuery.set('');
    this.triggerFilter();
  }

  protected hasActiveFilters(): boolean {
    return this.filterProgram()     !== 'ALL' ||
           this.filterCourse()      !== 'ALL' ||
           this.filterStudentType() !== 'ALL' ||
           this.filterStatus()      !== 'ALL' ||
           this.searchQuery() !== '';
  }

  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.DEFAULT_COLS);
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

  protected canVerifyDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFICATION_MANAGE');
  }

  private load(): void {
    this.loading.set(true);
    this.enquiryService.getDocumentVerificationPending().subscribe({
      next: enquiries => {
        this._allData.set(enquiries);
        this.dataSource.data = enquiries;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load enquiries'); this.loading.set(false); },
    });
  }

  protected viewEnquiry(item: Enquiry): void     { void this.router.navigate(['/enquiries', item.id]); }
  protected verifyDocuments(item: Enquiry): void  { void this.router.navigate(['/enquiries/document-verification', item.id]); }

  protected startTour(): void {
    this.tourService.start('document-verification-list');
  }
}
