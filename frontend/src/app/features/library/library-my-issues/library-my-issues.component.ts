import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { LibraryService } from '../library.service';
import {
  LibraryIssue,
  LibraryBook,
  ISSUE_STATUS_OPTIONS,
  SUBJECT_CATEGORY_OPTIONS,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

type PortalTab = 'active' | 'history' | 'search';

@Component({
  selector: 'app-library-my-issues',
  standalone: true,
  imports: [DatePipe, MatIconModule, FormsModule],
  templateUrl: './library-my-issues.component.html',
  styleUrl:    './library-my-issues.component.scss',
})
export class LibraryMyIssuesComponent implements OnInit {
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);

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
  protected readonly allBooks         = signal<LibraryBook[]>([]);
  protected readonly catalogueSearch  = signal('');
  protected readonly categoryFilter   = signal('');
  protected readonly categoryOptions  = SUBJECT_CATEGORY_OPTIONS;

  protected readonly filteredBooks = computed(() => {
    const q   = this.catalogueSearch().toLowerCase().trim();
    const cat = this.categoryFilter();
    return this.allBooks().filter(b => {
      if (cat && b.subjectCategory !== cat) return false;
      if (q && !(
        b.title.toLowerCase().includes(q) ||
        b.authors.toLowerCase().includes(q) ||
        b.accessionNumber.toLowerCase().includes(q) ||
        (b.callNumber ?? '').toLowerCase().includes(q)
      )) return false;
      return true;
    });
  });

  readonly statusLabel = (s: string) =>
    ISSUE_STATUS_OPTIONS.find(o => o.value === s)?.label ?? s;

  ngOnInit(): void {
    this.loadMyIssues();
  }

  protected selectTab(tab: PortalTab): void {
    this.activeTab.set(tab);
    if (tab === 'search' && this.allBooks().length === 0) this.loadCatalogue();
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
    this.libraryService.getAll('AVAILABLE').subscribe({
      next: books => { this.allBooks.set(books); this.catalogueLoading.set(false); },
      error: () => { this.toast.error('Failed to load catalogue'); this.catalogueLoading.set(false); },
    });
  }
}
