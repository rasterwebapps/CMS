import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { LibraryService } from '../library.service';
import {
  LibraryIssue,
  LibraryBook,
  IssueStatus,
  LibraryMemberType,
  ISSUE_STATUS_OPTIONS,
  SUBJECT_CATEGORY_OPTIONS,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

type ReportTab = 'overdue' | 'fines' | 'history' | 'accession';

@Component({
  selector: 'app-library-reports',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, MatIconModule],
  templateUrl: './library-reports.component.html',
  styleUrl:    './library-reports.component.scss',
})
export class LibraryReportsComponent implements OnInit {
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);

  protected readonly activeTab = signal<ReportTab>('overdue');

  // Overdue
  protected readonly overdueLoading = signal(false);
  protected readonly overdueIssues  = signal<LibraryIssue[]>([]);

  // Fines
  protected readonly finesLoading = signal(false);
  protected readonly fineIssues   = signal<LibraryIssue[]>([]);
  protected readonly fineTotal    = computed(() =>
    this.fineIssues().reduce((sum, i) => sum + (i.fine?.totalFine ?? 0), 0));
  protected readonly finePending  = computed(() =>
    this.fineIssues().filter(i => i.fine?.status === 'PENDING')
      .reduce((sum, i) => sum + (i.fine?.totalFine ?? 0), 0));

  // Issue History
  protected readonly historyLoading     = signal(false);
  protected readonly historyIssues      = signal<LibraryIssue[]>([]);
  protected readonly historyStatusFilter = signal<IssueStatus | ''>('');
  protected readonly historyMemberFilter = signal<LibraryMemberType | ''>('');
  protected readonly historySearchValue  = signal('');
  protected readonly statusOptions       = ISSUE_STATUS_OPTIONS;

  protected readonly filteredHistory = computed(() => {
    const q      = this.historySearchValue().toLowerCase().trim();
    const status = this.historyStatusFilter();
    const member = this.historyMemberFilter();
    return this.historyIssues().filter(i => {
      if (status && i.status     !== status) return false;
      if (member && i.memberType !== member) return false;
      if (q && !(
        i.accessionNumber.toLowerCase().includes(q) ||
        i.bookTitle.toLowerCase().includes(q) ||
        (i.studentName ?? '').toLowerCase().includes(q) ||
        (i.facultyName ?? '').toLowerCase().includes(q)
      )) return false;
      return true;
    });
  });

  // Accession Register
  protected readonly accessionLoading  = signal(false);
  protected readonly accessionBooks    = signal<LibraryBook[]>([]);
  protected readonly accessionCategory = signal('');
  protected readonly categoryOptions   = SUBJECT_CATEGORY_OPTIONS;

  ngOnInit(): void {
    this.loadOverdue();
  }

  protected selectTab(tab: ReportTab): void {
    this.activeTab.set(tab);
    if (tab === 'overdue'   && this.overdueIssues().length  === 0) this.loadOverdue();
    if (tab === 'fines'     && this.fineIssues().length     === 0) this.loadFines();
    if (tab === 'history'   && this.historyIssues().length  === 0) this.loadHistory();
    if (tab === 'accession' && this.accessionBooks().length === 0) this.loadAccession();
  }

  protected loadOverdue(): void {
    this.overdueLoading.set(true);
    this.libraryService.getOverdueReport().subscribe({
      next: data => { this.overdueIssues.set(data); this.overdueLoading.set(false); },
      error: () => { this.toast.error('Failed to load overdue report'); this.overdueLoading.set(false); },
    });
  }

  protected loadFines(): void {
    this.finesLoading.set(true);
    this.libraryService.getFineReport().subscribe({
      next: data => { this.fineIssues.set(data); this.finesLoading.set(false); },
      error: () => { this.toast.error('Failed to load fine report'); this.finesLoading.set(false); },
    });
  }

  protected loadHistory(): void {
    this.historyLoading.set(true);
    const status = this.historyStatusFilter() || undefined;
    const member = this.historyMemberFilter() || undefined;
    this.libraryService.getIssueHistoryReport(member as LibraryMemberType, status as IssueStatus).subscribe({
      next: data => { this.historyIssues.set(data); this.historyLoading.set(false); },
      error: () => { this.toast.error('Failed to load issue history'); this.historyLoading.set(false); },
    });
  }

  protected applyHistoryFilters(): void {
    this.historyIssues.set([]);
    this.loadHistory();
  }

  protected loadAccession(): void {
    this.accessionLoading.set(true);
    const cat = this.accessionCategory() || undefined;
    this.libraryService.getAccessionRegisterReport(cat).subscribe({
      next: data => { this.accessionBooks.set(data); this.accessionLoading.set(false); },
      error: () => { this.toast.error('Failed to load accession register'); this.accessionLoading.set(false); },
    });
  }

  protected applyAccessionFilter(): void {
    this.accessionBooks.set([]);
    this.loadAccession();
  }

  protected memberName(issue: LibraryIssue): string {
    return issue.memberType === 'STUDENT'
      ? `${issue.studentName ?? '—'} (${issue.studentRollNumber ?? ''})`
      : `${issue.facultyName ?? '—'} (${issue.facultyEmployeeCode ?? ''})`;
  }

  protected printAccessionRegister(): void {
    window.print();
  }

  protected today(): string {
    return new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'long', year: 'numeric' });
  }
}
