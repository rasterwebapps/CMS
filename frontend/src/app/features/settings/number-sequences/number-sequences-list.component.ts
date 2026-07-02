import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { NUMBER_SEQUENCES_TOUR } from '../../../shared/tour/tours/number-sequences.tours';
import { NumberSequence } from './number-sequence.model';
import { NumberSequenceService } from './number-sequence.service';

@Component({
  selector: 'app-number-sequences-list',
  standalone: true,
  imports: [
    FormsModule,
    AppDatePipe,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
  ],
  templateUrl: './number-sequences-list.component.html',
  styleUrl: './number-sequences-list.component.scss',
})
export class NumberSequencesListComponent implements OnInit, OnDestroy {
  private readonly numberSequenceService = inject(NumberSequenceService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize = this.currentPageSize;
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly displayedColumns = [
    'seriesName',
    'scopeKey',
    'lastGeneratedNumber',
    'nextPreviewNumber',
    'lastSequence',
    'updatedAt',
  ];
  protected readonly dataSource = new MatTableDataSource<NumberSequence>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'seriesName';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  private readonly sortMap: Record<string, string> = {
    seriesName: 'seriesName',
    scopeKey: 'scopeKey',
    lastSequence: 'lastSequence',
    updatedAt: 'updatedAt',
  };

  ngOnInit(): void {
    this.tourService.register('number-sequences-list', NUMBER_SEQUENCES_TOUR);
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.loadPage(); });
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected clearSearch(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected onSortChange(sort: Sort): void {
    if (!this.sortMap[sort.active]) return;
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0;
    this.loadPage();
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.numberSequenceService.getPage({
      search,
      page: this.currentPage,
      size: this.currentPageSize,
      sort: this.sortMap[this.sortActive] ?? this.sortActive,
      direction: this.sortDirection,
    }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load number sequences');
        this.loading.set(false);
      },
    });
  }
}
