import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FinanceService } from '../finance.service';
import { FeeState, FeeStructure, GroupedFeeStructure } from '../finance.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { environment } from '../../../../environments';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FEE_STRUCTURE_LIST_TOUR } from '../../../shared/tour/tours/fee-structure.tours';

interface Program { id: number; name: string; }
interface Course  { id: number; name: string; }
interface AcademicYear { id: number; name: string; }

type Quota  = 'MANAGEMENT' | 'COUNSELLING' | null;
type Gender = 'MALE' | 'FEMALE' | 'OTHER' | null;

@Component({
  selector: 'app-fee-structure-list',
  standalone: true,
  imports: [
    InrPipe,
    CmsEmptyStateComponent,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsTourButtonComponent,
  ],
  templateUrl: './fee-structure-list.component.html',
  styleUrl: './fee-structure-list.component.scss',
})
export class FeeStructureListComponent implements OnInit {
  private readonly financeService = inject(FinanceService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly http = inject(HttpClient);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'fee-structure-view-mode';

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = [
    'programName', 'courseName', 'academicYearName',
    'quota', 'feeStateName', 'gender',
    'feeCount', 'totalAmount', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<GroupedFeeStructure>([]);
  protected readonly loading    = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode   = signal<'card' | 'table'>(this.loadViewMode());

  protected readonly programs      = signal<Program[]>([]);
  protected readonly courses       = signal<Course[]>([]);
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly feeStates     = signal<FeeState[]>([]);

  protected readonly selectedAcademicYearId = signal<number | null>(null);
  protected readonly selectedProgramId      = signal<number | null>(null);
  protected readonly selectedCourseId       = signal<number | null>(null);
  protected readonly selectedQuota     = signal<Quota>(null);
  protected readonly selectedGender    = signal<Gender>(null);
  protected readonly selectedFeeStateId = signal<number | null>(null);

  private readonly allFeeStructures = signal<GroupedFeeStructure[]>([]);

  protected readonly filteredFeeStructures = computed(() => {
    const ayId        = this.selectedAcademicYearId();
    const progId      = this.selectedProgramId();
    const courseId    = this.selectedCourseId();
    const quota       = this.selectedQuota();
    const gender      = this.selectedGender();
    const feeStateId  = this.selectedFeeStateId();
    const search      = this.searchValue().trim().toLowerCase();

    return this.allFeeStructures().filter(fs => {
      if (ayId       !== null && fs.academicYearId !== ayId)  return false;
      if (progId     !== null && fs.programId !== progId)     return false;
      if (courseId   !== null && fs.courseId !== courseId)    return false;
      if (quota      !== null && fs.quota !== quota)          return false;
      if (gender     !== null && fs.gender !== gender)        return false;
      if (feeStateId !== null && fs.feeStateId !== feeStateId) return false;
      if (search) {
        const haystack = [
          fs.programName, fs.courseName, fs.academicYearName,
          fs.feeStateName, fs.quota, fs.gender,
        ].filter(Boolean).join(' ').toLowerCase();
        if (!haystack.includes(search)) return false;
      }
      return true;
    });
  });

  protected readonly totalFeeStructures = computed(() => this.allFeeStructures().length);
  protected readonly totalAmount = computed(() =>
    this.allFeeStructures().reduce((sum, fs) => sum + fs.totalAmount, 0)
  );

  protected readonly hasActiveFilters = computed(() =>
    !!this.searchValue() || !!this.selectedAcademicYearId() || !!this.selectedProgramId() ||
    !!this.selectedCourseId() || !!this.selectedQuota() || !!this.selectedGender() ||
    !!this.selectedFeeStateId()
  );

  // ── Fee-split helpers ─────────────────────────────────────────────────────

  private itemAmount(item: FeeStructure): number {
    if (item.yearAmounts?.length > 0) return item.yearAmounts.reduce((s, ya) => s + ya.amount, 0);
    return item.amount;
  }

  protected getDayScholarTotal(fs: GroupedFeeStructure): number {
    return fs.items.filter(i => i.isActive !== false && i.feeType !== 'HOSTEL_FEE')
      .reduce((s, i) => s + this.itemAmount(i), 0);
  }

  protected getHostelerTotal(fs: GroupedFeeStructure): number {
    // Transport fee applies to ALL students (same as form's courseFeeTypes which includes TRANSPORT_FEE).
    // Hosteler total = all fees (transport + course fees + hostel), matching form's hostelerTotal.
    return fs.items.filter(i => i.isActive !== false)
      .reduce((s, i) => s + this.itemAmount(i), 0);
  }

  protected hasAccommodationFees(fs: GroupedFeeStructure): boolean {
    // Only split the display when a HOSTEL_FEE exists — that is the fee that differs between
    // day scholars and hostelers. TRANSPORT_FEE is paid by all students (it is a courseFeeType
    // in the edit form) so it does not produce a meaningful split on its own.
    return fs.items.some(i => i.isActive !== false && i.feeType === 'HOSTEL_FEE');
  }

  protected quotaLabel(q: string): string {
    return q === 'MANAGEMENT' ? 'Management' : 'Counselling';
  }

  protected genderLabel(g: string): string {
    return g === 'FEMALE' ? 'Female' : g === 'MALE' ? 'Male' : 'Other';
  }

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredFeeStructures();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
  }

  ngOnInit(): void {
    this.tourService.register('fee-structure-list', FEE_STRUCTURE_LIST_TOUR);
    this.http.get<Program[]>(`${environment.apiUrl}/programs`).subscribe({ next: d => this.programs.set(d) });
    this.http.get<AcademicYear[]>(`${environment.apiUrl}/academic-years`).subscribe({ next: d => this.academicYears.set(d) });
    this.financeService.getFeeStates().subscribe({ next: d => this.feeStates.set(d) });
    this.load();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected applyFilter(event: Event): void { this.searchValue.set((event.target as HTMLInputElement).value); }
  protected clearFilter(): void             { this.searchValue.set(''); }

  protected onAcademicYearChange(id: number | null): void { this.selectedAcademicYearId.set(id); }

  protected onProgramFilterChange(id: number | null): void {
    this.selectedProgramId.set(id);
    this.selectedCourseId.set(null);
    this.courses.set([]);
    if (id !== null) {
      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${id}`)
        .subscribe({ next: d => this.courses.set(d) });
    }
  }

  protected onCourseChange(id: number | null): void     { this.selectedCourseId.set(id); }
  protected onQuotaChange(v: string | null): void     { this.selectedQuota.set((v || null) as Quota); }
  protected onGenderChange(v: string | null): void    { this.selectedGender.set((v || null) as Gender); }
  protected onFeeStateChange(id: number | null): void { this.selectedFeeStateId.set(id); }

  protected clearFilters(): void {
    this.selectedAcademicYearId.set(null);
    this.selectedProgramId.set(null);
    this.selectedCourseId.set(null);
    this.selectedQuota.set(null);
    this.selectedGender.set(null);
    this.selectedFeeStateId.set(null);
    this.searchValue.set('');
    this.courses.set([]);
  }

  protected handleEmptyAction(): void { void this.router.navigate(['/fee-structures/new']); }

  protected edit(item: GroupedFeeStructure): void {
    const params: Record<string, string> = {
      programId:      item.programId.toString(),
      academicYearId: item.academicYearId.toString(),
      quota:          item.quota,
      feeStateId:     item.feeStateId.toString(),
      gender:         item.gender,
    };
    if (item.courseId) params['courseId'] = item.courseId.toString();
    void this.router.navigate(['/fee-structures/new'], { queryParams: params });
  }

  protected delete(item: GroupedFeeStructure): void {
    const label = [
      item.programName,
      item.academicYearName,
      item.courseName,
      this.quotaLabel(item.quota),
      item.feeStateName,
      this.genderLabel(item.gender),
    ].filter(Boolean).join(' / ');

    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Fee Structure',
        message: `Delete all fee types for "${label}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.doDelete(item); });
  }

  private loadViewMode(): 'card' | 'table' {
    try { return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card'; }
    catch { return 'card'; }
  }

  private doDelete(item: GroupedFeeStructure): void {
    this.loading.set(true);
    this.financeService.deleteGroupedFeeStructures(
      item.programId, item.academicYearId, item.courseId ?? undefined,
      item.quota, item.feeStateId, item.gender,
    ).subscribe({
      next:  () => { this.toast.success('Deleted successfully'); this.load(); },
      error: () => { this.toast.error('Failed to delete'); this.loading.set(false); },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.financeService.getGroupedFeeStructures().subscribe({
      next:  data => { this.allFeeStructures.set(data); this.loading.set(false); },
      error: ()   => { this.toast.error('Failed to load fee structures'); this.loading.set(false); },
    });
  }
}
