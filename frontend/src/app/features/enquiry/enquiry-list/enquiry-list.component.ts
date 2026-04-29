import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ENQUIRY_LIST_TOUR } from '../../../shared/tour/tours/enquiry.tours';

export const STATUS_LABELS: Record<string, string> = {
  ENQUIRED:             'Enquired',
  INTERESTED:           'Interested',
  NOT_INTERESTED:       'Not Interested',
  FEES_FINALIZED:       'Fees Finalized',
  FEES_PAID:            'Fees Paid',
  PARTIALLY_PAID:       'Partially Paid',
  DOCUMENTS_SUBMITTED:  'Docs Submitted',
  ADMITTED:             'Admitted',
  CLOSED:               'Closed',
};

const STATUS_COLOURS: Record<string, string> = {
  ENQUIRED:             '#3b82f6',
  INTERESTED:           'var(--cms-primary)',
  NOT_INTERESTED:       '#ef4444',
  FEES_FINALIZED:       '#d97706',
  FEES_PAID:            '#16a34a',
  PARTIALLY_PAID:       '#d97706',
  DOCUMENTS_SUBMITTED:  '#0284c7',
  ADMITTED:             '#15803d',
  CLOSED:               'var(--cms-text-muted)',
};

@Component({
  selector: 'app-enquiry-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule, AppDatePipe,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatDialogModule, MatTooltipModule, MatMenuModule,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './enquiry-list.component.html',
  styleUrl: './enquiry-list.component.scss',
})
export class EnquiryListComponent implements OnInit {
  private readonly enquiryService = inject(EnquiryService);
  private readonly authService    = inject(AuthService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);
  private readonly tourService    = inject(TourService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort) set sort(v: MatSort) {
    if (v) {
      this.dataSource.sort = v;
      if (!v.active) { v.active = 'enquiryDate'; v.direction = 'asc'; v.sortChange.emit({ active: 'enquiryDate', direction: 'asc' }); }
    }
  }

  protected readonly dataSource      = new MatTableDataSource<Enquiry>([]);
  protected readonly loading         = signal(false);
  protected readonly searchValue     = signal('');
  protected readonly selectedStatuses = signal<Set<string>>(new Set());
  protected readonly selectedProgramId = signal<number | null>(null);
  protected readonly selectedCourseId  = signal<number | null>(null);
  protected readonly computeInitials  = computeInitials;
  protected readonly STATUS_LABELS    = STATUS_LABELS;
  protected readonly STATUS_COLOURS   = STATUS_COLOURS;
  protected statusMenuOpen  = false;
  protected colMenuOpen     = false;

  // ── Unique program/course lists derived from loaded data ──────────────────
  protected readonly programOptions = computed(() => {
    const seen = new Map<number, string>();
    for (const e of this.dataSource.data) {
      if (e.programId && e.programName && !seen.has(e.programId)) {
        seen.set(e.programId, e.programName);
      }
    }
    return [...seen.entries()].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
  });

  protected readonly courseOptions = computed(() => {
    const progId = this.selectedProgramId();
    const seen = new Map<number, string>();
    for (const e of this.dataSource.data) {
      if (e.courseId && e.courseName && !seen.has(e.courseId)) {
        if (!progId || e.programId === progId) {
          seen.set(e.courseId, e.courseName);
        }
      }
    }
    return [...seen.entries()].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
  });

  // ── Column visibility ─────────────────────────────────────────────────────
  protected readonly ALL_COLS = [
    'name', 'phone', 'programName', 'studentType',
    'enquiryDate', 'referralTypeName', 'status', 'agentName', 'actions',
  ];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Name', phone: 'Phone', programName: 'Course', studentType: 'Type',
    enquiryDate: 'Date', referralTypeName: 'Referral', status: 'Status',
    agentName: 'Agent', actions: 'Actions',
  };
  private readonly COLS_KEY = 'enquiry-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));

  protected readonly ALL_STATUSES = [
    'ENQUIRED', 'INTERESTED', 'NOT_INTERESTED', 'FEES_FINALIZED',
    'FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED', 'ADMITTED', 'CLOSED',
  ];

  // ── Per-status counts from full loaded data ───────────────────────────────
  protected readonly statusCounts = computed<Record<string, number>>(() => {
    const map: Record<string, number> = {};
    for (const row of this.dataSource.data) {
      map[row.status] = (map[row.status] ?? 0) + 1;
    }
    return map;
  });

  // ── Aggregate stats ───────────────────────────────────────────────────────
  protected readonly totalCount     = computed(() => this.dataSource.data.length);
  protected readonly filteredCount  = computed(() => this.dataSource.filteredData.length);
  protected readonly pipelineCount  = computed(() =>
    this.dataSource.data.filter(e => !['NOT_INTERESTED', 'CLOSED', 'ADMITTED'].includes(e.status)).length);
  protected readonly interestedCount = computed(() =>
    this.dataSource.data.filter(e => e.status === 'INTERESTED').length);
  protected readonly admittedCount  = computed(() =>
    this.dataSource.data.filter(e => e.status === 'ADMITTED').length);

  // ── Status dropdown label ─────────────────────────────────────────────────
  protected readonly statusFilterLabel = computed(() => {
    const sel = this.selectedStatuses();
    if (sel.size === 0) return 'Status';
    if (sel.size === 1) return STATUS_LABELS[[...sel][0]] ?? [...sel][0];
    return `${sel.size} statuses`;
  });

  // ── Date range ────────────────────────────────────────────────────────────
  protected dateFrom: string;
  protected dateTo:   string;

  constructor() {
    const now = new Date();
    this.dateFrom = this.toDateString(new Date(now.getFullYear(), now.getMonth(), 1));
    this.dateTo   = this.toDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0));

    // Combined filter: "<search>|<STATUS1,STATUS2>|<programId>|<courseId>"
    this.dataSource.filterPredicate = (row, filter) => {
      const parts    = filter.split('|');
      const search   = parts[0] ?? '';
      const statuses = new Set((parts[1] ?? '').split(',').filter(Boolean));
      const progId   = parts[2] ? Number(parts[2]) : null;
      const courseId = parts[3] ? Number(parts[3]) : null;

      const matchSearch = !search ||
        row.name.toLowerCase().includes(search) ||
        (row.phone ?? '').includes(search) ||
        (row.courseName ?? '').toLowerCase().includes(search) ||
        (row.agentName ?? '').toLowerCase().includes(search);

      const matchStatus  = statuses.size === 0 || statuses.has(row.status);
      const matchProgram = !progId   || row.programId === progId;
      const matchCourse  = !courseId || row.courseId  === courseId;
      return matchSearch && matchStatus && matchProgram && matchCourse;
    };

    // Reactively update the Material filter whenever any filter changes
    effect(() => {
      const search   = this.searchValue().toLowerCase().trim();
      const statuses = [...this.selectedStatuses()].join(',');
      const progId   = this.selectedProgramId() ?? '';
      const courseId = this.selectedCourseId() ?? '';
      this.dataSource.filter = `${search}|${statuses}|${progId}|${courseId}`;
      this.dataSource.paginator?.firstPage();
    });
  }

  ngOnInit(): void {
    this.tourService.register('enquiry-list', ENQUIRY_LIST_TOUR);
    this.load();
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

  // ── Search ────────────────────────────────────────────────────────────────
  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
  }

  protected clearFilter(): void { this.searchValue.set(''); }

  // ── Status multiselect ────────────────────────────────────────────────────
  protected toggleStatus(s: string): void {
    this.selectedStatuses.update(set => {
      const next = new Set(set);
      if (next.has(s)) { next.delete(s); } else { next.add(s); }
      return next;
    });
  }

  protected isStatusSelected(s: string): boolean { return this.selectedStatuses().has(s); }

  protected clearStatuses(): void { this.selectedStatuses.set(new Set()); }

  // ── Date range ────────────────────────────────────────────────────────────
  protected onDateRangeChange(): void { if (this.dateFrom && this.dateTo) this.load(); }

  // ── Filters ───────────────────────────────────────────────────────────────
  protected clearAllFilters(): void {
    const now = new Date();
    this.dateFrom = this.toDateString(new Date(now.getFullYear(), now.getMonth(), 1));
    this.dateTo   = this.toDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0));
    this.selectedStatuses.set(new Set());
    this.selectedProgramId.set(null);
    this.selectedCourseId.set(null);
    this.searchValue.set('');
    this.load();
  }

  protected get hasActiveFilters(): boolean {
    return this.selectedStatuses().size > 0 || !!this.searchValue()
      || this.selectedProgramId() !== null || this.selectedCourseId() !== null;
  }

  // ── Program / Course filter handlers ─────────────────────────────────────
  protected onProgramChange(value: string): void {
    const id = value ? Number(value) : null;
    this.selectedProgramId.set(id);
    this.selectedCourseId.set(null); // reset course when program changes
  }

  protected onCourseChange(value: string): void {
    this.selectedCourseId.set(value ? Number(value) : null);
  }

  // ── Status helpers ────────────────────────────────────────────────────────
  protected statusLabel(s: string): string { return STATUS_LABELS[s] ?? s; }

  protected getNextStatuses(currentStatus: string): string[] {
    switch (currentStatus) {
      case 'ENQUIRED':       return ['INTERESTED', 'NOT_INTERESTED'];
      case 'NOT_INTERESTED': return ['INTERESTED'];
      case 'FEES_FINALIZED': return ['NOT_INTERESTED'];
      case 'CLOSED':         return ['ENQUIRED'];
      default:               return [];
    }
  }

  protected canChangeStatus(item: Enquiry): boolean { return this.getNextStatuses(item.status).length > 0; }

  protected onStatusUpdate(item: Enquiry, newStatus: string): void {
    this.enquiryService.updateStatus(item.id, newStatus).subscribe({
      next: updated => {
        const data = this.dataSource.data;
        const idx  = data.findIndex(e => e.id === item.id);
        if (idx >= 0) { data[idx] = { ...data[idx], status: updated.status }; this.dataSource.data = [...data]; }
        this.toast.success(`Status → ${this.statusLabel(updated.status)}`);
      },
      error: () => this.toast.error('Failed to update status'),
    });
  }

  // ── Action guards ─────────────────────────────────────────────────────────
  protected canConvert(item: Enquiry): boolean { return item.status === 'DOCUMENTS_SUBMITTED'; }

  protected canFinalizeFee(item: Enquiry): boolean {
    return item.status === 'INTERESTED' &&
      (this.authService.isAdmin() || this.authService.isCollegeAdmin());
  }

  protected canCollectPayment(item: Enquiry): boolean {
    return (item.status === 'FEES_FINALIZED' || item.status === 'PARTIALLY_PAID') &&
      (this.authService.isAdmin() || this.authService.isCollegeAdmin() || this.authService.isCashier());
  }

  protected canSubmitDocuments(item: Enquiry): boolean {
    return (item.status === 'FEES_PAID' || item.status === 'PARTIALLY_PAID') &&
      (this.authService.isAdmin() || this.authService.isCollegeAdmin() || this.authService.isFrontOffice());
  }

  protected canDelete(item: Enquiry): boolean { return item.status === 'ENQUIRED'; }

  // ── Actions ───────────────────────────────────────────────────────────────
  protected edit(item: Enquiry): void    { void this.router.navigate(['/enquiries', item.id, 'edit']); }
  protected view(item: Enquiry): void    { void this.router.navigate(['/enquiries', item.id]); }
  protected convert(item: Enquiry): void { void this.router.navigate(['/enquiries', item.id, 'convert']); }

  protected finalizeFee(item: Enquiry): void {
    void this.router.navigate(['/student-fees/finalize'], { queryParams: { enquiryId: item.id } });
  }

  protected collectPayment(item: Enquiry): void {
    void this.router.navigate(['/student-fees/collect-payment'], { queryParams: { enquiryId: item.id } });
  }

  protected submitDocuments(item: Enquiry): void {
    void this.router.navigate(['/enquiries/document-submission', item.id]);
  }

  protected delete(item: Enquiry): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Enquiry', message: `Delete "${item.name}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe(ok => { if (ok) this.doDelete(item); });
  }

  private doDelete(item: Enquiry): void {
    this.loading.set(true);
    this.enquiryService.deleteEnquiry(item.id).subscribe({
      next:  () => { this.toast.success('Deleted'); this.load(); },
      error: () => { this.toast.error('Failed to delete'); this.loading.set(false); },
    });
  }

  // ── Export ────────────────────────────────────────────────────────────────
  protected exportCsv(): void {
    const rows    = this.dataSource.filteredData;
    const headers = ['Name', 'Phone', 'Course', 'Type', 'Date', 'Referral', 'Status', 'Agent'];
    const cells   = rows.map(e => [
      e.name, e.phone ?? '', e.courseName ?? '', e.studentType ?? '',
      e.enquiryDate, e.referralTypeName ?? '', this.statusLabel(e.status), e.agentName ?? '',
    ]);
    const csv = [headers, ...cells].map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }));
    const a   = Object.assign(document.createElement('a'), { href: url, download: `enquiries-${new Date().toISOString().slice(0, 10)}.csv` });
    document.body.appendChild(a); a.click(); document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // ── Load ──────────────────────────────────────────────────────────────────
  private toDateString(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private load(): void {
    this.loading.set(true);
    // Load all data for the date range; status filtering is done client-side
    this.enquiryService.getEnquiriesByDateRange(this.dateFrom, this.dateTo).subscribe({
      next:  data => { this.dataSource.data = data; this.loading.set(false); },
      error: ()   => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}
