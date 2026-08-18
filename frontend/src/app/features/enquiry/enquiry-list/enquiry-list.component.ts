import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatTableModule, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear } from '../../academic-year/academic-year.model';
import { ProgramService } from '../../program/program.service';
import { CourseService } from '../../course/course.service';
import { Course } from '../../course/course.model';
import { ReferralTypeService } from '../../referral-type/referral-type.service';
import { AgentService } from '../../agent/agent.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ENQUIRY_LIST_TOUR, ENQUIRY_LIST_FLOW_MAP } from '../../../shared/tour/tours/enquiry.tours';
import { CmsIconDeleteComponent, CmsIconEditComponent, CmsIconViewComponent } from '../../../shared/icons';
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button';
import { CmsColumnPickerComponent, ColumnPickerState } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
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
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
    CmsIconViewComponent,
    ExportButtonComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
  ],
  templateUrl: './enquiry-list.component.html',
  styleUrl: './enquiry-list.component.scss',
})
export class EnquiryListComponent implements OnInit, OnDestroy {
  private readonly enquiryService      = inject(EnquiryService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly programService      = inject(ProgramService);
  private readonly courseService       = inject(CourseService);
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly agentService        = inject(AgentService);
  private readonly permissionService   = inject(PermissionService);
  private readonly router              = inject(Router);
  private readonly route               = inject(ActivatedRoute);
  private readonly toast               = inject(ToastService);
  private readonly dialog              = inject(MatDialog);
  private readonly tourService         = inject(TourService);

  private readonly destroy$     = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginatorSub?: Subscription;
  private _paginatorRef?: MatPaginator;

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(v: MatPaginator) {
    if (v) {
      this._paginatorRef = v;
      v.pageIndex = this.currentPage;
      v.pageSize  = this.currentPageSize;
      this._paginatorSub?.unsubscribe();
      this._paginatorSub = v.page.pipe(takeUntil(this.destroy$)).subscribe((ev: PageEvent) => {
        this.currentPage     = ev.pageIndex;
        this.currentPageSize = ev.pageSize;
        this.syncUrlFilters();
        this.loadPage();
      });
    }
  }

  // ── Table (server-side) ──────────────────────────────────────────────────
  protected readonly rows    = signal<Enquiry[]>([]);
  protected totalElements    = 0;
  protected readonly loading = signal(false);
  private currentPage        = 0;
  private currentPageSize    = 25;
  protected sortActive       = 'enquiryDate';
  protected sortDirection: 'asc' | 'desc' = 'desc';
  private readonly sortMap: Record<string, string> = {
    name: 'name', enquiryDate: 'enquiryDate', status: 'status', programName: 'program.name',
  };

  // ── Filter state ─────────────────────────────────────────────────────────
  protected readonly searchValue             = signal('');
  protected readonly selectedStatuses        = signal<Set<string>>(new Set(['ENQUIRED', 'INTERESTED']));
  protected readonly selectedProgramId       = signal<number | null>(null);
  protected readonly selectedCourseId        = signal<number | null>(null);
  protected readonly selectedStudentType     = signal<string | null>(null);
  protected readonly selectedReferralType    = signal<string | null>(null);
  protected readonly selectedAdmissionQuota  = signal<string | null>(null);
  protected readonly selectedAgent           = signal<string | null>(null);
  protected readonly selectedAdmissionSource = signal<string | null>(null);
  protected readonly selectedAcademicYearIds = signal<Set<number>>(new Set());
  private readonly allAcademicYears_         = signal<AcademicYear[]>([]);
  private academicYearsRestoredFromUrl       = false;

  protected readonly computeInitials  = computeInitials;
  protected readonly STATUS_LABELS    = STATUS_LABELS;
  protected statusMenuOpen       = false;
  protected academicYearMenuOpen = false;
  protected moreFiltersOpen      = false;
  protected readonly exporting   = signal(false);

  protected readonly canAdd    = computed(() => this.permissionService.has('ENQUIRY_CREATE'));
  protected readonly canExport = computed(() => this.permissionService.has('ENQUIRY_EXPORT'));

  // ── Date range ────────────────────────────────────────────────────────────
  protected dateFrom: string;
  protected dateTo:   string;

  // ── Filter dropdown options (loaded from master services on init) ─────────
  protected programs:   { id: number; name: string }[] = [];
  protected allCourses: Course[] = [];
  protected readonly courseOptions = computed(() => {
    const pid = this.selectedProgramId();
    return pid ? this.allCourses.filter(c => c.program?.id === pid) : this.allCourses;
  });
  protected referralTypeNames: string[] = [];
  protected agentNames:        string[] = [];

  // ── Academic year multiselect ─────────────────────────────────────────────
  protected readonly academicYearOptions = computed(() =>
    [...this.allAcademicYears_()].sort((a, b) => b.startDate.localeCompare(a.startDate))
  );

  private defaultAcademicYearIds(): Set<number> {
    const years = this.allAcademicYears_();
    const current = years.find(y => y.isCurrent);
    if (!current) return new Set();
    const ids = new Set([current.id]);
    const next = years
      .filter(y => y.startDate > current.startDate)
      .sort((a, b) => a.startDate.localeCompare(b.startDate))[0];
    if (next) ids.add(next.id);
    return ids;
  }

  protected readonly academicYearFilterLabel = computed(() => {
    const sel = this.selectedAcademicYearIds();
    if (sel.size === 0) return 'All Years';
    if (sel.size === 1) {
      const year = this.allAcademicYears_().find(y => sel.has(y.id));
      return year?.name ?? '1 year';
    }
    return `${sel.size} years`;
  });

  protected isAcademicYearSelected(id: number): boolean {
    return this.selectedAcademicYearIds().has(id);
  }

  protected toggleAcademicYear(id: number): void {
    this.selectedAcademicYearIds.update(s => {
      const next = new Set(s);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected clearAcademicYears(): void {
    this.selectedAcademicYearIds.set(new Set());
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected get moreFiltersCount(): number {
    return (this.selectedStudentType()      !== null ? 1 : 0)
         + (this.selectedReferralType()     !== null ? 1 : 0)
         + (this.selectedAdmissionQuota()   !== null ? 1 : 0)
         + (this.selectedAgent()            !== null ? 1 : 0)
         + (this.selectedAdmissionSource()  !== null ? 1 : 0);
  }

  // ── Column picker ─────────────────────────────────────────────────────────
  protected readonly colState = new ColumnPickerState({
    columns: [
      { key: 'name',            label: 'Name',     mandatory: true },
      { key: 'phone',           label: 'Phone' },
      { key: 'programName',     label: 'Course' },
      { key: 'studentType',     label: 'Type' },
      { key: 'enquiryDate',     label: 'Date' },
      { key: 'referralTypeName',label: 'Referral' },
      { key: 'status',          label: 'Status' },
      { key: 'agentName',       label: 'Agent' },
      { key: 'actions',         label: 'Actions',  mandatory: true, pinnable: false },
    ],
    storageKey: 'enquiry-list-cols',
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());

  protected readonly ALL_STATUSES = [
    'ENQUIRED', 'INTERESTED', 'NOT_INTERESTED', 'FEES_FINALIZED',
    'FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED', 'DOCUMENTS_VERIFIED', 'ADMITTED',
  ];

  protected readonly statusFilterLabel = computed(() => {
    const sel = this.selectedStatuses();
    if (sel.size === 0) return 'Status';
    if (sel.size === 1) return STATUS_LABELS[[...sel][0]] ?? [...sel][0];
    return `${sel.size} statuses`;
  });

  constructor() {
    const now = new Date();
    this.dateFrom = this.toDateString(new Date(now.getFullYear(), now.getMonth(), 1));
    this.dateTo   = this.toDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0));
  }


  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }
  ngOnInit(): void {
    this.tourService.register('enquiry-list', ENQUIRY_LIST_TOUR);
    this.tourService.registerFlowMap('enquiry-list', ENQUIRY_LIST_FLOW_MAP);

    // Restore filter state from URL query params
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
    const rawYears = p.getAll('academicYearIds').flatMap(s => s.split(',').filter(Boolean)).map(Number);
    if (rawYears.length) {
      this.selectedAcademicYearIds.set(new Set(rawYears));
      this.academicYearsRestoredFromUrl = true;
    }
    this.currentPage     = p.get('page') ? +p.get('page')! : 0;
    this.currentPageSize = p.get('size') ? +p.get('size')! : 25;
    this.sortActive      = p.get('sortField') ?? 'enquiryDate';
    this.sortDirection   = (p.get('sortDir') ?? 'desc') as 'asc' | 'desc';
    if (this.moreFiltersCount > 0) this.moreFiltersOpen = true;

    // Load filter dropdown options (non-critical — best effort)
    this.programService.getAll().subscribe({
      next: list => { this.programs = list; },
    });
    this.courseService.getAll().subscribe({
      next: list => { this.allCourses = list; },
    });
    this.referralTypeService.getPage({ size: 1000 }).subscribe({
      next: page => { this.referralTypeNames = page.content.map(r => r.name); },
    });
    this.agentService.getPage({ size: 1000 }).subscribe({
      next: page => { this.agentNames = page.content.map(a => a.name); },
    });

    // Debounced search
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(val => {
      this.searchValue.set(val);
      this.resetPage();
      this.syncUrlFilters();
      this.loadPage();
    });

    // Load academic years → apply defaults → first data load
    const dateWasRestoredFromUrl = !!p.get('dateFrom') || !!p.get('dateTo');
    this.academicYearService.getAllAcademicYears().subscribe({
      next: years => {
        this.allAcademicYears_.set(years);
        if (!this.academicYearsRestoredFromUrl && !dateWasRestoredFromUrl) {
          this.selectedAcademicYearIds.set(this.defaultAcademicYearIds());
        }
        this.syncUrlFilters();
        this.loadPage();
      },
      error: () => this.loadPage(),
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── URL filter sync (for back-navigation state restoration) ───────────────
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
        academicYearIds: [...this.selectedAcademicYearIds()].length
          ? [...this.selectedAcademicYearIds()].join(',') : null,
        page:      this.currentPage > 0 ? this.currentPage : null,
        size:      this.currentPageSize !== 25 ? this.currentPageSize : null,
        sortField: this.sortActive !== 'enquiryDate' ? this.sortActive : null,
        sortDir:   this.sortDirection !== 'desc' ? this.sortDirection : null,
      },
      queryParamsHandling: 'replace',
      replaceUrl: true,
    });
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || 'desc') as 'asc' | 'desc';
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  private resetPage(): void {
    this.currentPage = 0;
    if (this._paginatorRef) this._paginatorRef.pageIndex = 0;
  }


  // ── Search ────────────────────────────────────────────────────────────────
  protected applyFilter(event: Event): void {
    this.searchSubject.next((event.target as HTMLInputElement).value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  // ── Status multiselect ────────────────────────────────────────────────────
  protected toggleStatus(s: string): void {
    this.selectedStatuses.update(set => {
      const next = new Set(set);
      if (next.has(s)) { next.delete(s); } else { next.add(s); }
      return next;
    });
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected isStatusSelected(s: string): boolean { return this.selectedStatuses().has(s); }

  protected clearStatuses(): void {
    this.selectedStatuses.set(new Set());
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  // ── Date range ────────────────────────────────────────────────────────────
  protected onDateRangeChange(): void {
    if (this.dateFrom && this.dateTo) {
      this.resetPage();
      this.syncUrlFilters();
      this.loadPage();
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
    this.selectedAcademicYearIds.set(new Set());
    this.searchValue.set('');
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected get hasActiveFilters(): boolean {
    return this.selectedStatuses().size > 0 || !!this.searchValue()
      || this.selectedProgramId()      !== null || this.selectedCourseId()       !== null
      || this.selectedStudentType()    !== null || this.selectedReferralType()   !== null
      || this.selectedAdmissionQuota() !== null || this.selectedAgent()          !== null
      || this.selectedAdmissionSource() !== null || this.selectedAcademicYearIds().size > 0;
  }

  // ── Filter change handlers ────────────────────────────────────────────────
  protected onProgramChange(value: string): void {
    this.selectedProgramId.set(value ? Number(value) : null);
    this.selectedCourseId.set(null);
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected onCourseChange(value: string): void {
    this.selectedCourseId.set(value ? Number(value) : null);
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected onStudentTypeChange(value: string): void {
    this.selectedStudentType.set(value || null);
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected onReferralTypeChange(value: string): void {
    this.selectedReferralType.set(value || null);
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected onAdmissionQuotaChange(value: string): void {
    this.selectedAdmissionQuota.set(value || null);
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected onAgentChange(value: string): void {
    this.selectedAgent.set(value || null);
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  protected onAdmissionSourceChange(value: string): void {
    this.selectedAdmissionSource.set(value || null);
    this.resetPage();
    this.syncUrlFilters();
    this.loadPage();
  }

  // ── Status helpers ────────────────────────────────────────────────────────
  protected statusLabel(s: string): string { return STATUS_LABELS[s] ?? s; }

  protected getNextStatuses(currentStatus: string, item?: Enquiry): string[] {
    const hasProgramAndCourse = !!item && !!item.programId && !!item.courseId;
    switch (currentStatus) {
      case 'ENQUIRED':       return hasProgramAndCourse ? ['INTERESTED', 'NOT_INTERESTED'] : ['NOT_INTERESTED'];
      case 'NOT_INTERESTED': return hasProgramAndCourse ? ['INTERESTED'] : [];
      case 'FEES_FINALIZED': return ['NOT_INTERESTED'];
      default:               return [];
    }
  }

  protected canChangeStatus(item: Enquiry): boolean { return this.getNextStatuses(item.status, item).length > 0; }

  protected onStatusUpdate(item: Enquiry, newStatus: string): void {
    this.enquiryService.updateStatus(item.id, newStatus).subscribe({
      next: updated => {
        this.toast.success(`Status → ${this.statusLabel(updated.status)}`);
        this.loadPage();
      },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to update status'),
    });
  }

  // ── Action guards ─────────────────────────────────────────────────────────
  protected canConvert(item: Enquiry): boolean { return item.status === 'DOCUMENTS_VERIFIED'; }

  protected canFinalizeFee(item: Enquiry): boolean {
    return item.status === 'INTERESTED' &&
      this.permissionService.has('FEE_FINALIZE');
  }

  protected canCollectPayment(item: Enquiry): boolean {
    const blockedStatuses = ['NOT_INTERESTED', 'CANCELLED', 'ADMITTED'];
    return item.finalizedNetFee !== null && item.finalizedNetFee !== undefined &&
      !blockedStatuses.includes(item.status) &&
      this.permissionService.has('FEE_COLLECT');
  }

  protected canSubmitDocuments(item: Enquiry): boolean {
    return (item.status === 'FEES_PAID' || item.status === 'PARTIALLY_PAID') &&
      this.permissionService.has('DOCUMENT_SUBMISSION_MANAGE');
  }

  protected canDelete(item: Enquiry): boolean {
    return item.status === 'ENQUIRED' && this.permissionService.has('ENQUIRY_DELETE');
  }
  protected canEdit(item: Enquiry): boolean {
    return item.status !== 'ADMITTED' && this.permissionService.has('ENQUIRY_EDIT');
  }

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
      next:  () => { this.toast.success('Deleted'); this.loadPage(); },
      error: err => { this.toast.error(err?.error?.message ?? 'Failed to delete'); this.loading.set(false); },
    });
  }

  // ── Export ────────────────────────────────────────────────────────────────
  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    if (this.totalElements === 0) {
      this.toast.error('No data available to export.');
      return;
    }
    this.exporting.set(true);
    const statuses = [...this.selectedStatuses()];
    const yearIds  = [...this.selectedAcademicYearIds()];
    this.enquiryService.exportEnquiries(format, {
      search:           this.searchValue() || null,
      fromDate:         this.dateFrom || null,
      toDate:           this.dateTo   || null,
      statuses:         statuses.length ? statuses : undefined,
      programId:        this.selectedProgramId(),
      courseId:         this.selectedCourseId(),
      studentType:      this.selectedStudentType(),
      referralTypeName: this.selectedReferralType(),
      admissionQuota:   this.selectedAdmissionQuota(),
      agentName:        this.selectedAgent(),
      admissionSource:  this.selectedAdmissionSource(),
      academicYearIds:  yearIds.length ? yearIds : undefined,
      sort:             this.sortMap[this.sortActive] ?? this.sortActive,
      direction:        this.sortDirection,
    }).subscribe({
      next: blob => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const filename = `enquiries-${new Date().toISOString().slice(0, 10)}.${ext}`;
        const url = URL.createObjectURL(blob);
        const a = Object.assign(document.createElement('a'), { href: url, download: filename });
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.toast.error('Export failed. Please try again.');
        this.exporting.set(false);
      },
    });
  }

  // ── Data load ─────────────────────────────────────────────────────────────
  private toDateString(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private loadPage(): void {
    this.loading.set(true);
    const statuses = [...this.selectedStatuses()];
    const yearIds  = [...this.selectedAcademicYearIds()];
    this.enquiryService.getPage({
      search:           this.searchValue() || null,
      fromDate:         this.dateFrom || null,
      toDate:           this.dateTo   || null,
      statuses:         statuses.length ? statuses : undefined,
      programId:        this.selectedProgramId(),
      courseId:         this.selectedCourseId(),
      studentType:      this.selectedStudentType(),
      referralTypeName: this.selectedReferralType(),
      admissionQuota:   this.selectedAdmissionQuota(),
      agentName:        this.selectedAgent(),
      admissionSource:  this.selectedAdmissionSource(),
      academicYearIds:  yearIds.length ? yearIds : undefined,
      page:             this.currentPage,
      size:             this.currentPageSize,
      sort:             `${this.sortMap[this.sortActive] ?? this.sortActive},${this.sortDirection}`,
    }).subscribe({
      next: data => {
        this.rows.set(data.content);
        this.totalElements = data.totalElements;
        this.loading.set(false);
        if (this._paginatorRef) {
          this._paginatorRef.length    = data.totalElements;
          this._paginatorRef.pageIndex = this.currentPage;
        }
      },
      error: () => {
        this.toast.error('Failed to load enquiries');
        this.loading.set(false);
      },
    });
  }
}
