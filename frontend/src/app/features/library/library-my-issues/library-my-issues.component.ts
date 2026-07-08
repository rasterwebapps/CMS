import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import {
  LibraryIssue,
  LibraryBook,
  ISSUE_STATUS_OPTIONS,
  SUBJECT_CATEGORY_OPTIONS,
  LibraryRack,
  LibraryShelf,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

type PortalTab = 'active' | 'history' | 'search';

@Component({
  selector: 'app-library-my-issues',
  standalone: true,
  imports: [DatePipe, MatIconModule, MatPaginatorModule, FormsModule],
  templateUrl: './library-my-issues.component.html',
  styleUrl:    './library-my-issues.component.scss',
})
export class LibraryMyIssuesComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize  = this.currentPageSize;
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadCatalogue();
    });
  }

  protected readonly activeTab    = signal<PortalTab>('active');
  protected readonly issuesLoading = signal(false);
  protected readonly allMyIssues  = signal<LibraryIssue[]>([]);

  protected readonly activeIssues  = computed(() =>
    this.allMyIssues().filter(i => i.status === 'ISSUED' || i.status === 'OVERDUE'));
  protected readonly historyIssues = computed(() =>
    this.allMyIssues().filter(i => i.status === 'RETURNED' || i.status === 'LOST'));
  protected readonly hasOverdue    = computed(() =>
    this.activeIssues().some(i => i.status === 'OVERDUE'));

  // Catalogue search
  protected readonly catalogueLoading = signal(false);
  protected readonly books            = signal<LibraryBook[]>([]);
  protected readonly catalogueSearch  = signal('');
  protected readonly categoryFilter   = signal('');
  protected readonly categoryOptions  = SUBJECT_CATEGORY_OPTIONS;
  protected readonly racks            = signal<LibraryRack[]>([]);
  protected readonly shelves          = signal<LibraryShelf[]>([]);
  protected readonly rackFilter       = signal<number | null>(null);
  protected readonly shelfFilter      = signal<number | null>(null);

  protected totalElements   = 0;
  protected currentPage     = 0;
  protected currentPageSize = 25;
  private catalogueLoaded   = false;

  readonly statusLabel = (s: string) =>
    ISSUE_STATUS_OPTIONS.find(o => o.value === s)?.label ?? s;

  ngOnInit(): void {
    this.loadMyIssues();
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.loadCatalogue(); });
  }

  ngOnDestroy(): void {
    this.destroy$.next(); this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected selectTab(tab: PortalTab): void {
    this.activeTab.set(tab);
    if (tab === 'search' && !this.catalogueLoaded) {
      this.catalogueLoaded = true;
      this.libraryService.getRacks(undefined, true).subscribe({ next: (racks) => this.racks.set(racks) });
      this.loadCatalogue();
    }
  }

  protected onCatalogueSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.catalogueSearch.set(value);
    this.searchSubject.next(value);
  }

  protected onCatalogueFilterChange(): void { this.currentPage = 0; this.loadCatalogue(); }

  protected onRackFilterChange(): void {
    this.shelfFilter.set(null);
    this.shelves.set([]);
    const rackId = this.rackFilter();
    if (rackId) {
      this.libraryService.getShelves(rackId, undefined, true).subscribe({ next: (shelves) => this.shelves.set(shelves) });
    }
    this.onCatalogueFilterChange();
  }

  private loadMyIssues(): void {
    this.issuesLoading.set(true);
    this.libraryService.getMyIssues().subscribe({
      next: issues => { this.allMyIssues.set(issues); this.issuesLoading.set(false); },
      error: () => { this.toast.error('Failed to load your issues'); this.issuesLoading.set(false); },
    });
  }

  private loadCatalogue(): void {
    this.catalogueLoading.set(true);
    this.libraryService.getBooksPage({
      status: 'AVAILABLE',
      search: this.catalogueSearch() || undefined,
      category: this.categoryFilter() || undefined,
      shelfId: this.shelfFilter(),
      page: this.currentPage, size: this.currentPageSize,
      sort: 'title', direction: 'asc',
    }).subscribe({
      next: page => {
        this.books.set(page.content);
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.catalogueLoading.set(false);
      },
      error: () => { this.toast.error('Failed to load catalogue'); this.catalogueLoading.set(false); },
    });
  }
}
