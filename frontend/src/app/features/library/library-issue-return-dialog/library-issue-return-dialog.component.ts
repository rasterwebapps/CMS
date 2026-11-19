import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { LibraryService } from '../library.service';
import { IssueResolutionStatus, LibraryIssue, LibraryReturnRequest } from '../library.model';

@Component({
  selector: 'app-library-issue-return-dialog',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsFlyoutPanelComponent,
  ],
  templateUrl: './library-issue-return-dialog.component.html',
  styleUrl: './library-issue-return-dialog.component.scss',
})
export class LibraryIssueReturnDialogComponent implements OnInit {
  readonly issue = input.required<LibraryIssue>();
  readonly closed = output<LibraryIssue | undefined>();

  private readonly libraryService = inject(LibraryService);
  private readonly toast = inject(ToastService);

  protected readonly resolutionStatus = signal<IssueResolutionStatus>('AVAILABLE');
  protected readonly penaltyFine = signal<number | null>(null);
  protected readonly remarks = signal('');
  protected readonly finePerDay = signal<number>(1);
  protected readonly submitting = signal(false);

  protected readonly itemLabel = computed(() =>
    this.issue().itemType === 'BOOK' ? 'Book' : 'Journal'
  );

  protected readonly memberLabel = computed(() => {
    const i = this.issue();
    return i.memberType === 'STUDENT'
      ? `${i.studentName ?? '—'} (${i.studentRollNumber ?? ''})`
      : `${i.facultyName ?? '—'} (${i.facultyEmployeeCode ?? ''})`;
  });

  protected readonly overdueDays = computed(() => {
    const dueStr = this.issue().dueDate;
    if (!dueStr) return 0;
    const now = new Date();
    const todayUtc = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
    const [y, m, d] = dueStr.split('-').map(Number);
    const dueUtc = Date.UTC(y, m - 1, d);
    const diffDays = Math.floor((todayUtc - dueUtc) / 86_400_000);
    return diffDays > 0 ? diffDays : 0;
  });

  protected readonly overdueFine = computed(() =>
    this.overdueDays() * this.finePerDay()
  );

  protected readonly effectivePenaltyFine = computed(() => {
    if (this.resolutionStatus() === 'AVAILABLE') return 0;
    const v = this.penaltyFine();
    return v != null && v > 0 ? Number(v) : 0;
  });

  protected readonly totalFine = computed(() =>
    this.overdueFine() + this.effectivePenaltyFine()
  );

  ngOnInit(): void {
    this.libraryService.getSettings().subscribe({
      next: settings => {
        const fineSetting = settings.find(s => s.settingKey === 'fine_per_day');
        if (fineSetting) {
          const parsed = parseFloat(fineSetting.settingValue);
          if (!Number.isNaN(parsed) && parsed >= 0) {
            this.finePerDay.set(parsed);
          }
        }
      },
      error: () => {
        // Default finePerDay = 1 is kept if settings lookup fails
      },
    });
  }

  protected selectResolution(status: IssueResolutionStatus): void {
    this.resolutionStatus.set(status);
    if (status === 'AVAILABLE') {
      this.penaltyFine.set(null);
    } else if (status === 'LOST' && this.penaltyFine() === null && this.issue().itemPriceRs != null) {
      this.penaltyFine.set(this.issue().itemPriceRs!);
    }
  }

  protected onPenaltyInput(value: string | number | null): void {
    if (value === null || value === '') {
      this.penaltyFine.set(null);
      return;
    }
    const num = Number(value);
    this.penaltyFine.set(!Number.isNaN(num) && num >= 0 ? num : 0);
  }

  protected submitReturn(): void {
    if (this.submitting()) return;
    this.submitting.set(true);

    const resolution = this.resolutionStatus();
    const penalty = this.effectivePenaltyFine();
    const request: LibraryReturnRequest = {
      resolutionStatus: resolution,
      penaltyFine: penalty > 0 ? penalty : undefined,
      remarks: this.remarks().trim() || undefined,
    };

    const label = this.itemLabel();
    this.libraryService.returnBook(this.issue().id, request).subscribe({
      next: returned => {
        this.submitting.set(false);
        const fineMsg = returned.fine
          ? ` Fine recorded: ₹${returned.fine.totalFine}.`
          : '';
        if (resolution === 'LOST') {
          this.toast.success(`${label} marked as lost.${fineMsg}`);
        } else if (resolution === 'DAMAGED') {
          this.toast.success(`${label} returned as damaged.${fineMsg}`);
        } else {
          this.toast.success(`${label} returned successfully.${fineMsg}`);
        }
        this.closed.emit(returned);
      },
      error: err => {
        this.submitting.set(false);
        this.toast.error(err?.error?.message ?? `Failed to process ${label.toLowerCase()} return`);
      },
    });
  }

  protected cancel(): void {
    this.closed.emit(undefined);
  }
}
