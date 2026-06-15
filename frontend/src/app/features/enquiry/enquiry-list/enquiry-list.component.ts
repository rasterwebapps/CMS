import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
import { PermissionService } from '../../../core/permissions/permission.service';
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
  DOCUMENTS_VERIFIED:   'Docs Verified',
  ADMITTED:             'Admitted',
  CLOSED:               'Closed',
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
  private readonly permissionService = inject(PermissionService);
  private readonly router         = inject(Router);
  private readonly route          = inject(ActivatedRoute);
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

  private readonly allEnquiries      = signal<Enquiry[]>([]);
  protected readonly dataSource      = new MatTableDataSource<Enquiry>([]);
  protected readonly loading         = signal(false);
  protected readonly searchValue     = signal('');
  protected readonly selectedStatuses      = signal<Set<string>>(new Set());
  protected readonly selectedProgramId     = signal<number | null>(null);
  protected readonly selectedCourseId      = signal<number | null>(null);
  protected readonly selectedStudentType   = signal<string | null>(null);
  protected readonly selectedReferralType  = signal<string | null>(null);
  protected readonly selectedAdmissionQuota = signal<string | null>(null);
  protected readonly selectedAgent         = signal<string | null>(null);
  protected readonly selectedAdmissionSource = signal<string | null>(null);
  protected readonly computeInitials  = computeInitials;
  protected readonly STATUS_LABELS    = STATUS_LABELS;
  protected statusMenuOpen    = false;
  protected colMenuOpen       = false;
  protected moreFiltersOpen   = false;

  protected get moreFiltersCount(): number {
    return (this.selectedStudentType()     !== null ? 1 : 0)
         + (this.selectedReferralType()    !== null ? 1 : 0)
         + (this.selectedAdmissionQuota()  !== null ? 1 : 0)
         + (this.selectedAgent()           !== null ? 1 : 0)
         + (this.selectedAdmissionSource() !== null ? 1 : 0);
  }

  // ── Unique program/course lists derived from loaded data ──────────────────
  protected readonly programOptions = computed(() => {
    const seen = new Map<number, string>();
    for (const e of this.allEnquiries()) {
      if (e.programId && e.programName && !seen.has(e.programId)) {
        seen.set(e.programId, e.programName);
      }
    }
    return [...seen.entries()].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
  });

  protected readonly courseOptions = computed(() => {
    const progId = this.selectedProgramId();
    const seen = new Map<number, string>();
    for (const e of this.allEnquiries()) {
      if (e.courseId && e.courseName && !seen.has(e.courseId)) {
        if (!progId || e.programId === progId) {
          seen.set(e.courseId, e.courseName);
        }
      }
    }
    return [...seen.entries()].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
  });

  protected readonly referralTypeOptions = computed(() => {
    const seen = new Set<string>();
    return this.allEnquiries()
      .map(e => e.referralTypeName)
      .filter((n): n is string => !!n && !seen.has(n) && !!seen.add(n))
      .sort();
  });

  protected readonly agentOptions = computed(() => {
    const seen = new Set<string>();
    return this.allEnquiries()
      .map(e => e.agentName)
      .filter((n): n is string => !!n && !seen.has(n) && !!seen.add(n))
      .sort();
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
    'FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED', 'DOCUMENTS_VERIFIED', 'ADMITTED', 'CLOSED',
  ];

  // ── Per-status counts from full loaded data ───────────────────────────────
  protected readonly statusCounts = computed<Record<string, number>>(() => {
    const map: Record<string, number> = {};
    for (const row of this.allEnquiries()) {
      map[row.status] = (map[row.status] ?? 0) + 1;
    }
    return map;
  });

  // ── Aggregate stats ───────────────────────────────────────────────────────
  protected readonly totalCount     = computed(() => this.allEnquiries().length);
  protected readonly filteredCount  = computed(() => this.dataSource.filteredData.length);
  protected readonly pipelineCount  = computed(() =>
    this.allEnquiries().filter(e => !['NOT_INTERESTED', 'CLOSED', 'ADMITTED'].includes(e.status)).length);
  protected readonly interestedCount = computed(() =>
    this.allEnquiries().filter(e => e.status === 'INTERESTED').length);
  protected readonly admittedCount  = computed(() =>
    this.allEnquiries().filter(e => e.status === 'ADMITTED').length);

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

    // filterPredicate reads signals directly; the filter string is just a trigger
    this.dataSource.filterPredicate = (row, _filter) => {
      const search          = this.searchValue().toLowerCase().trim();
      const statuses        = this.selectedStatuses();
      const progId          = this.selectedProgramId();
      const courseId        = this.selectedCourseId();
      const studentType     = this.selectedStudentType();
      const referralType    = this.selectedReferralType();
      const quota           = this.selectedAdmissionQuota();
      const agent           = this.selectedAgent();
      const source          = this.selectedAdmissionSource();

      const matchSearch    = !search        || row.name.toLowerCase().includes(search) || (row.phone ?? '').includes(search);
      const matchStatus    = statuses.size === 0 || statuses.has(row.status);
      const matchProgram   = !progId        || row.programId === progId;
      const matchCourse    = !courseId      || row.courseId  === courseId;
      const matchType      = !studentType   || row.studentType === studentType;
      const matchReferral  = !referralType  || row.referralTypeName === referralType;
      const matchQuota     = !quota         || row.admissionQuota === quota;
      const matchAgent     = !agent         || row.agentName === agent;
      const matchSource    = !source        || row.admissionSource === source;

      return matchSearch && matchStatus && matchProgram && matchCourse &&
             matchType && matchReferral && matchQuota && matchAgent && matchSource;
    };

    // Reactively update the Material filter trigger whenever any filter signal changes
    effect(() => {
      const parts = [
        this.searchValue().toLowerCase().trim(),
        [...this.selectedStatuses()].join(','),
        this.selectedProgramId() ?? '',
        this.selectedCourseId() ?? '',
        this.selectedStudentType() ?? '',
        this.selectedReferralType() ?? '',
        this.selectedAdmissionQuota() ?? '',
        this.selectedAgent() ?? '',
        this.selectedAdmissionSource() ?? '',
      ];
      this.dataSource.filter = parts.join('|') || '_';
      this.dataSource.paginator?.firstPage();
    });
  }

  ngOnInit(): void {
    this.tourService.register('enquiry-list', ENQUIRY_LIST_TOUR);

    // Restore filter state from URL query params (populated when the user
    // navigated away via a list row action — allows back-navigation to restore
    // exactly the same search/date/status/program that was active).
    const p = this.route.snapshot.queryParamMap;
    if (p.get('dateFrom')) this.dateFrom = p.get('dateFrom')!;
    if (p.get('dateTo'))   this.dateTo   = p.get('dateTo')!;
    if (p.get('search'))   this.searchValue.set(p.get('search')!);
    const rawStatuses = p.getAll('status').flatMap(s => s.split(',').filter(Boolean));
    if (rawStatuses.length) this.selectedStatuses.set(new Set(rawStatuses));
    if (p.get('programId'))       this.selectedProgramId.set(Number(p.get('programId')));
    if (p.get('courseId'))        this.selectedCourseId.set(Number(p.get('courseId')));
    if (p.get('studentType'))     this.selectedStudentType.set(p.get('studentType'));
    if (p.get('referralType'))    this.selectedReferralType.set(p.get('referralType'));
    if (p.get('admissionQuota'))  this.selectedAdmissionQuota.set(p.get('admissionQuota'));
    if (p.get('agent'))           this.selectedAgent.set(p.get('agent'));
    if (p.get('admissionSource')) this.selectedAdmissionSource.set(p.get('admissionSource'));
    if (this.moreFiltersCount > 0) this.moreFiltersOpen = true;

    this.load();
  }

  // ── URL filter sync ───────────────────────────────────────────────────────
  /**
   * Writes current filter values into the URL as query params using
   * replaceUrl:true so no extra history entries are created.  When the user
   * navigates to a detail and presses Back, Angular re-navigates to this URL,
   * the component reinitialises and reads the params back in ngOnInit.
   */
  private syncUrlFilters(): void {
    const statuses = [...this.selectedStatuses()];
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        dateFrom:        this.dateFrom  || null,
        dateTo:          this.dateTo    || null,
        search:          this.searchValue() || null,
        status:          statuses.length ? statuses.join(',') : null,
        programId:       this.selectedProgramId() ?? null,
        courseId:        this.selectedCourseId()  ?? null,
        studentType:     this.selectedStudentType()      ?? null,
        referralType:    this.selectedReferralType()     ?? null,
        admissionQuota:  this.selectedAdmissionQuota()   ?? null,
        agent:           this.selectedAgent()            ?? null,
        admissionSource: this.selectedAdmissionSource()  ?? null,
      },
      queryParamsHandling: 'replace',
      replaceUrl: true,
    });
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
    this.syncUrlFilters();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.syncUrlFilters();
  }

  // ── Status multiselect ────────────────────────────────────────────────────
  protected toggleStatus(s: string): void {
    this.selectedStatuses.update(set => {
      const next = new Set(set);
      if (next.has(s)) { next.delete(s); } else { next.add(s); }
      return next;
    });
    this.syncUrlFilters();
  }

  protected isStatusSelected(s: string): boolean { return this.selectedStatuses().has(s); }

  protected clearStatuses(): void {
    this.selectedStatuses.set(new Set());
    this.syncUrlFilters();
  }

  // ── Date range ────────────────────────────────────────────────────────────
  protected onDateRangeChange(): void {
    if (this.dateFrom && this.dateTo) {
      this.syncUrlFilters();
      this.load();
    }
  }

  // ── Filters ───────────────────────────────────────────────────────────────
  protected clearAllFilters(): void {
    const now = new Date();
    this.dateFrom = this.toDateString(new Date(now.getFullYear(), now.getMonth(), 1));
    this.dateTo   = this.toDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0));
    this.selectedStatuses.set(new Set());
    this.selectedProgramId.set(null);
    this.selectedCourseId.set(null);
    this.selectedStudentType.set(null);
    this.selectedReferralType.set(null);
    this.selectedAdmissionQuota.set(null);
    this.selectedAgent.set(null);
    this.selectedAdmissionSource.set(null);
    this.searchValue.set('');
    this.syncUrlFilters();
    this.load();
  }

  protected get hasActiveFilters(): boolean {
    return this.selectedStatuses().size > 0 || !!this.searchValue()
      || this.selectedProgramId()      !== null || this.selectedCourseId()       !== null
      || this.selectedStudentType()    !== null || this.selectedReferralType()   !== null
      || this.selectedAdmissionQuota() !== null || this.selectedAgent()          !== null
      || this.selectedAdmissionSource() !== null;
  }

  // ── Program / Course filter handlers ─────────────────────────────────────
  protected onProgramChange(value: string): void {
    const id = value ? Number(value) : null;
    this.selectedProgramId.set(id);
    this.selectedCourseId.set(null); // reset course when program changes
    this.syncUrlFilters();
  }

  protected onCourseChange(value: string): void {
    this.selectedCourseId.set(value ? Number(value) : null);
    this.syncUrlFilters();
  }

  protected onStudentTypeChange(value: string): void {
    this.selectedStudentType.set(value || null);
    this.syncUrlFilters();
  }

  protected onReferralTypeChange(value: string): void {
    this.selectedReferralType.set(value || null);
    this.syncUrlFilters();
  }

  protected onAdmissionQuotaChange(value: string): void {
    this.selectedAdmissionQuota.set(value || null);
    this.syncUrlFilters();
  }

  protected onAgentChange(value: string): void {
    this.selectedAgent.set(value || null);
    this.syncUrlFilters();
  }

  protected onAdmissionSourceChange(value: string): void {
    this.selectedAdmissionSource.set(value || null);
    this.syncUrlFilters();
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
        const data = this.allEnquiries();
        const idx  = data.findIndex(e => e.id === item.id);
        if (idx >= 0) {
          const nextData = [...data];
          nextData[idx] = { ...nextData[idx], status: updated.status };
          this.allEnquiries.set(nextData);
          this.dataSource.data = nextData;
        }
        this.toast.success(`Status → ${this.statusLabel(updated.status)}`);
      },
      error: () => this.toast.error('Failed to update status'),
    });
  }

  // ── Action guards ─────────────────────────────────────────────────────────
  protected canConvert(item: Enquiry): boolean { return item.status === 'DOCUMENTS_VERIFIED'; }

  protected canFinalizeFee(item: Enquiry): boolean {
    return item.status === 'INTERESTED' &&
      this.permissionService.has('FEE_FINALIZE');
  }

  protected canCollectPayment(item: Enquiry): boolean {
    const blockedStatuses = ['NOT_INTERESTED', 'CANCELLED', 'CLOSED', 'ADMITTED', 'CONVERTED'];
    return item.finalizedNetFee !== null && item.finalizedNetFee !== undefined &&
      !blockedStatuses.includes(item.status) &&
      this.permissionService.has('FEE_COLLECT');
  }

  protected canSubmitDocuments(item: Enquiry): boolean {
    return (item.status === 'FEES_PAID' || item.status === 'PARTIALLY_PAID') &&
      this.permissionService.has('DOCUMENT_SUBMISSION_MANAGE');
  }

  protected canDelete(item: Enquiry): boolean { return item.status === 'ENQUIRED'; }
  protected canEdit(item: Enquiry): boolean   { return !['ADMITTED', 'CLOSED'].includes(item.status); }

  // ── Actions ───────────────────────────────────────────────────────────────
  protected edit(item: Enquiry): void    { void this.router.navigate(['/enquiries', item.id, 'edit']); }
  protected view(item: Enquiry): void    { void this.router.navigate(['/enquiries', item.id]); }
  protected convert(item: Enquiry): void { void this.router.navigate(['/enquiries', item.id, 'convert']); }

  protected handleEmptyAction(): void {
    if (this.hasActiveFilters) {
      this.clearAllFilters();
      return;
    }

    void this.router.navigate(['/enquiries/new']);
  }

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
      next:  data => { this.allEnquiries.set(data); this.dataSource.data = data; this.loading.set(false); },
      error: ()   => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}
