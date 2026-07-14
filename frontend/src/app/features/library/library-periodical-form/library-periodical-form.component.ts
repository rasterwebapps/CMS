import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AsyncValidatorFn, AbstractControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { map, switchMap, first } from 'rxjs/operators';
import { timer } from 'rxjs';
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
  imports: [RouterLink, ReactiveFormsModule],
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
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.itemId.set(+id);
      this.pageTitle.set('Edit Journal Entry');
    }

    this.buildForm();

    if (id) {
      this.loadItem(+id);
    }
  }

  private buildForm(): void {
    const excludeId = this.itemId();
    this.form = this.fb.group({
      accessionNumber:    ['', { validators: [Validators.required], asyncValidators: [this.accessionNumberValidator(excludeId)], updateOn: 'blur' }],
      barcode:            ['', { asyncValidators: [this.barcodeValidator(excludeId)], updateOn: 'blur' }],
      journalName:        ['', [Validators.required, Validators.maxLength(300)]],
      journalType:        ['NATIONAL'],
      organization:       ['', Validators.maxLength(200)],
      volumeNumber:       ['', Validators.maxLength(20)],
      issueNumber:        ['', Validators.maxLength(20)],
      monthRange:         [''],
      year:               [this.currentYear],
      subscriptionStatus: ['ACTIVE'],
      receivedDate:       [''],
      receivedBy:         ['', Validators.maxLength(100)],
      remarks:            ['', Validators.maxLength(500)],
    });
  }

  private accessionNumberValidator(excludeId: number | null): AsyncValidatorFn {
    return (control: AbstractControl) => {
      const value = control.value?.trim();
      if (!value) return Promise.resolve(null);
      return timer(350).pipe(
        switchMap(() => this.libraryService.checkPeriodicalAccessionNumberExists(value, excludeId ?? undefined)),
        map(res => res.exists ? { accessionNumberExists: true } : null),
        first(),
      );
    };
  }

  private barcodeValidator(excludeId: number | null): AsyncValidatorFn {
    return (control: AbstractControl) => {
      const value = control.value?.trim();
      if (!value) return Promise.resolve(null);
      return timer(350).pipe(
        switchMap(() => this.libraryService.checkPeriodicalBarcodeExists(value, excludeId ?? undefined)),
        map(res => res.exists ? { barcodeExists: true } : null),
        first(),
      );
    };
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.libraryService.getPeriodicalById(id).subscribe({
      next: p => {
        this.form.patchValue({
          accessionNumber:    p.accessionNumber,
          barcode:            p.barcode ?? '',
          journalName:        p.journalName,
          journalType:        p.journalType,
          organization:       p.organization ?? '',
          volumeNumber:       p.volumeNumber ?? '',
          issueNumber:        p.issueNumber  ?? '',
          monthRange:         p.monthRange   ?? '',
          year:               p.year ?? this.currentYear,
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

    const v = this.form.getRawValue();
    const request: LibraryPeriodicalRequest = {
      accessionNumber:    v.accessionNumber.trim(),
      barcode:            v.barcode?.trim() || undefined,
      journalName:        v.journalName.trim(),
      journalType:        v.journalType || undefined,
      organization:       v.organization?.trim() || undefined,
      volumeNumber:       v.volumeNumber?.trim() || undefined,
      issueNumber:        v.issueNumber?.trim()  || undefined,
      monthRange:         v.monthRange  || undefined,
      year:               v.year        || undefined,
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
