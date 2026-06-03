import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { LibraryService } from '../library.service';
import {
  LibraryPeriodicalRequest,
  JOURNAL_TYPE_OPTIONS,
  SUBSCRIPTION_STATUS_OPTIONS,
  MONTH_RANGE_OPTIONS,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-library-periodical-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatIconModule],
  templateUrl: './library-periodical-form.component.html',
  styleUrl:    './library-periodical-form.component.scss',
})
export class LibraryPeriodicalFormComponent implements OnInit {
  private readonly fb             = inject(FormBuilder);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);
  private readonly destroyRef     = inject(DestroyRef);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly itemId     = signal<number | null>(null);
  protected readonly pageTitle  = signal('Add Journal Entry');

  protected readonly journalTypeOptions   = JOURNAL_TYPE_OPTIONS;
  protected readonly statusOptions        = SUBSCRIPTION_STATUS_OPTIONS;
  protected readonly monthRangeOptions    = MONTH_RANGE_OPTIONS;
  protected readonly currentYear          = new Date().getFullYear();
  protected readonly yearOptions          = Array.from({ length: 20 }, (_, i) => this.currentYear - i + 2);

  protected form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      journalName:        ['', [Validators.required, Validators.maxLength(300)]],
      journalType:        ['NATIONAL'],
      organization:       ['', Validators.maxLength(200)],
      volumeNumber:       ['', Validators.maxLength(20)],
      issueNumber:        ['', Validators.maxLength(20)],
      monthRange:         [''],
      year:               [this.currentYear],
      copiesCount:        [1, [Validators.required, Validators.min(1)]],
      subscriptionStatus: ['ACTIVE'],
      receivedDate:       [''],
      receivedBy:         ['', Validators.maxLength(100)],
      remarks:            ['', Validators.maxLength(500)],
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.itemId.set(+id);
      this.pageTitle.set('Edit Journal Entry');
      this.loadItem(+id);
    }
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.libraryService.getPeriodicalById(id).subscribe({
      next: p => {
        this.form.patchValue({
          journalName:        p.journalName,
          journalType:        p.journalType,
          organization:       p.organization ?? '',
          volumeNumber:       p.volumeNumber ?? '',
          issueNumber:        p.issueNumber  ?? '',
          monthRange:         p.monthRange   ?? '',
          year:               p.year ?? this.currentYear,
          copiesCount:        p.copiesCount,
          subscriptionStatus: p.subscriptionStatus,
          receivedDate:       p.receivedDate ?? '',
          receivedBy:         p.receivedBy   ?? '',
          remarks:            p.remarks      ?? '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load journal entry');
        this.loading.set(false);
      },
    });
  }

  protected save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);

    const v = this.form.value;
    const request: LibraryPeriodicalRequest = {
      journalName:        v.journalName.trim(),
      journalType:        v.journalType || undefined,
      organization:       v.organization?.trim() || undefined,
      volumeNumber:       v.volumeNumber?.trim() || undefined,
      issueNumber:        v.issueNumber?.trim()  || undefined,
      monthRange:         v.monthRange  || undefined,
      year:               v.year        || undefined,
      copiesCount:        v.copiesCount,
      subscriptionStatus: v.subscriptionStatus || undefined,
      receivedDate:       v.receivedDate || undefined,
      receivedBy:         v.receivedBy?.trim() || undefined,
      remarks:            v.remarks?.trim()    || undefined,
    };

    const op = this.isEditMode()
      ? this.libraryService.updatePeriodical(this.itemId()!, request)
      : this.libraryService.createPeriodical(request);

    op.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Journal entry updated' : 'Journal entry added');
        void this.router.navigate(['/library/periodicals']);
      },
      error: err => {
        this.toast.error(err?.error?.message ?? 'Failed to save');
        this.saving.set(false);
      },
    });
  }

  protected cancel(): void {
    void this.router.navigate(['/library/periodicals']);
  }

  protected get f() { return this.form.controls; }
}
