import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { LibraryService } from '../library.service';
import {
  LibraryBook,
  LibraryPeriodical,
  LibraryIssue,
  LibraryBookShelfTransfer,
  LibraryItemType,
  BOOK_STATUS_OPTIONS,
  ISSUE_STATUS_OPTIONS,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

export interface LibraryItemHistoryDialogData {
  itemType: LibraryItemType;
  item: LibraryBook | LibraryPeriodical;
}

interface SummaryField {
  label: string;
  value: string;
}

@Component({
  selector: 'app-library-item-history-dialog',
  standalone: true,
  imports: [DatePipe, NgTemplateOutlet, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTabsModule, CmsFlyoutPanelComponent],
  providers: [DatePipe],
  templateUrl: './library-item-history-dialog.component.html',
  styleUrl: './library-item-history-dialog.component.scss',
})
export class LibraryItemHistoryDialogComponent implements OnInit {
  readonly data = input.required<LibraryItemHistoryDialogData>();
  readonly closed = output<void>();

  private readonly libraryService = inject(LibraryService);
  private readonly toast = inject(ToastService);
  private readonly datePipe = inject(DatePipe);

  protected readonly issuesLoading = signal(true);
  protected readonly issues = signal<LibraryIssue[]>([]);

  protected readonly transfersLoading = signal(false);
  protected readonly transfers = signal<LibraryBookShelfTransfer[]>([]);

  protected get isBook(): boolean {
    return this.data().itemType === 'BOOK';
  }

  protected get itemIcon(): string {
    return this.isBook ? 'menu_book' : 'newspaper';
  }

  protected get title(): string {
    return this.isBook ? (this.data().item as LibraryBook).title : (this.data().item as LibraryPeriodical).journalName;
  }

  protected get accessionNumber(): string {
    return this.data().item.accessionNumber;
  }

  protected get currentStatusLabel(): string {
    const status = this.data().item.status;
    return BOOK_STATUS_OPTIONS.find(o => o.value === status)?.label ?? status;
  }

  protected get currentStatusClass(): string {
    const status = this.data().item.status;
    return BOOK_STATUS_OPTIONS.find(o => o.value === status)?.colorClass ?? '';
  }

  protected get summaryFields(): SummaryField[] {
    if (this.isBook) {
      const b = this.data().item as LibraryBook;
      return [
        { label: 'Author(s)', value: b.authors },
        { label: 'Publisher', value: b.publisher || '—' },
        { label: 'Category', value: b.subjectCategory || '—' },
        { label: 'Location', value: this.locationLabel(b.libraryName, b.rackName, b.shelfName) },
        { label: 'Added On', value: this.formatDate(b.entryDate) },
      ];
    }
    const p = this.data().item as LibraryPeriodical;
    return [
      { label: 'Type', value: p.journalType },
      { label: 'Organization', value: p.organization || '—' },
      { label: 'Volume / Issue', value: [p.volumeNumber, p.issueNumber].filter(Boolean).join(' / ') || '—' },
      { label: 'Received On', value: this.formatDate(p.receivedDate) },
      { label: 'Received By', value: p.receivedBy || '—' },
    ];
  }

  ngOnInit(): void {
    const item = this.data().item;
    const issueHistory$ = this.isBook
      ? this.libraryService.getBookIssueHistory(item.id)
      : this.libraryService.getPeriodicalIssueHistory(item.id);

    issueHistory$.subscribe({
      next: issues => {
        this.issues.set(issues);
        this.issuesLoading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load borrow/return history');
        this.issuesLoading.set(false);
      },
    });

    if (this.isBook) {
      this.transfersLoading.set(true);
      this.libraryService.getBookTransferHistory(item.id).subscribe({
        next: transfers => {
          this.transfers.set(transfers);
          this.transfersLoading.set(false);
        },
        error: () => {
          // Transfer history is supplementary — still let the borrow/return tab work if it fails.
          this.transfersLoading.set(false);
        },
      });
    }
  }

  protected close(): void {
    this.closed.emit();
  }

  protected borrowerName(issue: LibraryIssue): string {
    return issue.memberType === 'STUDENT' ? (issue.studentName ?? '—') : (issue.facultyName ?? '—');
  }

  /** Roll number (students) or employee code (faculty) — the identifier staff actually look up borrowers by. */
  protected borrowerCode(issue: LibraryIssue): string {
    return (issue.memberType === 'STUDENT' ? issue.studentRollNumber : issue.facultyEmployeeCode) ?? '—';
  }

  protected issueStatusLabel(status: string): string {
    return ISSUE_STATUS_OPTIONS.find(o => o.value === status)?.label ?? status;
  }

  protected issueStatusClass(status: string): string {
    return ISSUE_STATUS_OPTIONS.find(o => o.value === status)?.colorClass ?? '';
  }

  protected transferFrom(t: LibraryBookShelfTransfer): string {
    return this.locationLabel(t.oldLibraryName, t.oldRackName, t.oldShelfName);
  }

  protected transferTo(t: LibraryBookShelfTransfer): string {
    return this.locationLabel(t.newLibraryName, t.newRackName, t.newShelfName);
  }

  private locationLabel(library?: string, rack?: string, shelf?: string): string {
    if (!library) return 'Unassigned';
    const parts = [library];
    if (rack) parts.push(rack);
    if (shelf) parts.push(shelf);
    return parts.join(' / ');
  }

  private formatDate(value?: string): string {
    if (!value) return '—';
    return this.datePipe.transform(value, 'dd MMM yyyy') ?? value;
  }
}
