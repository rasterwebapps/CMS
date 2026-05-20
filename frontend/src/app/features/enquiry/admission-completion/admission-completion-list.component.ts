import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';

import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ADMISSION_COMPLETION_LIST_TOUR } from '../../../shared/tour/tours/enquiry.tours';

@Component({
  selector: 'app-admission-completion-list',
  standalone: true,
  imports: [
    InrPipe, AppDatePipe, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './admission-completion-list.component.html',
  styleUrl: './admission-completion-list.component.scss',
})
export class AdmissionCompletionListComponent implements OnInit {
  private readonly enquiryService = inject(EnquiryService);
  private readonly permissionService = inject(PermissionService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly tourService    = inject(TourService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly loading     = signal(true);
  protected readonly searchQuery = signal('');
  protected colMenuOpen          = false;

  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);
  private readonly _allData     = signal<Enquiry[]>([]);

  // ── Filters ────────────────────────────────────────────────────────────────
  protected readonly filterProgram     = signal<string>('ALL');
  protected readonly filterStudentType = signal<string>('ALL');

  protected readonly programs = computed(() =>
    [...new Set(this._allData().map(r => r.programName).filter(Boolean))].sort() as string[]
  );

  // ── Stats ──────────────────────────────────────────────────────────────────
  protected readonly totalCount    = computed(() => this._allData().length);
  protected readonly filteredCount = computed(() => this.dataSource.filteredData.length);

  // ── Column visibility ──────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['name', 'programName', 'courseName', 'status', 'totalPaidAmount', 'finalizedNetFee', 'finalizedAt', 'enquiryDate', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Student', programName: 'Program', courseName: 'Course',
    status: 'Status', totalPaidAmount: 'Paid (₹)', finalizedNetFee: 'Net Fee (₹)',
    finalizedAt: 'Fees Finalized', enquiryDate: 'Enquiry Date', actions: 'Actions',
  };
  private readonly COLS_KEY     = 'admission-completion-list-cols-v2';
  private readonly DEFAULT_COLS = new Set(['name', 'programName', 'courseName', 'status', 'totalPaidAmount', 'finalizedNetFee', 'finalizedAt', 'actions']);
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));

  protected readonly computeInitials = computeInitials;

  ngOnInit(): void {
    this.tourService.register('admission-completion-list', ADMISSION_COMPLETION_LIST_TOUR);
    this.dataSource.filterPredicate = (row: Enquiry, _filter: string) => {
      const program     = this.filterProgram();
      const studentType = this.filterStudentType();
      const q           = this.searchQuery().toLowerCase().trim();

      if (program     !== 'ALL' && (row.programName ?? '') !== program) return false;
      if (studentType !== 'ALL' && (row.studentType ?? '') !== studentType) return false;
      if (!q) return true;
      return row.name.toLowerCase().includes(q) ||
        (row.programName ?? '').toLowerCase().includes(q) ||
        (row.courseName  ?? '').toLowerCase().includes(q) ||
        (row.phone       ?? '').includes(q) ||
        (row.email       ?? '').toLowerCase().includes(q);
    };
    this.load();
  }

  private triggerFilter(): void {
    this.dataSource.filter = this.searchQuery() + '|' +
      this.filterProgram() + '|' + this.filterStudentType();
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
  protected onStudentTypeChange(val: string): void  { this.filterStudentType.set(val); this.triggerFilter(); }

  protected clearFilters(): void {
    this.filterProgram.set('ALL');
    this.filterStudentType.set('ALL');
    this.searchQuery.set('');
    this.triggerFilter();
  }

  protected hasActiveFilters(): boolean {
    return this.filterProgram() !== 'ALL' ||
           this.filterStudentType() !== 'ALL' ||
           this.searchQuery() !== '';
  }

  // ── Column prefs ───────────────────────────────────────────────────────────
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

  protected canCompleteAdmission(): boolean {
    return this.permissionService.hasAny('ADMISSION_CREATE', 'ADMISSION_EDIT');
  }

  private load(): void {
    this.loading.set(true);
    this.enquiryService.getAdmissionPending().subscribe({
      next:  enquiries => {
        this._allData.set(enquiries);
        this.dataSource.data = enquiries;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load enquiries'); this.loading.set(false); },
    });
  }

  protected viewEnquiry(item: Enquiry): void { void this.router.navigate(['/enquiries', item.id]); }

  protected completeAdmission(item: Enquiry): void {
    void this.router.navigate(['/enquiries', item.id, 'convert']);
  }
}
