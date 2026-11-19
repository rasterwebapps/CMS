import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AsyncValidatorFn, AbstractControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, map, switchMap, first } from 'rxjs/operators';
import { timer } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { LibraryService } from '../library.service';
import {
  BookStatus,
  LibraryBookRequest,
  BOOK_STATUS_OPTIONS,
  BOOK_SOURCE_OPTIONS,
  SUBJECT_CATEGORY_OPTIONS,
  LibraryRack,
  LibraryShelf,
  LibrarySubjectCategory,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-library-book-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatButtonModule],
  templateUrl: './library-book-form.component.html',
  styleUrl: './library-book-form.component.scss',
})
export class LibraryBookFormComponent implements OnInit {
  private readonly fb           = inject(FormBuilder);
  private readonly route        = inject(ActivatedRoute);
  private readonly router       = inject(Router);
  private readonly libraryService = inject(LibraryService);
  private readonly toast        = inject(ToastService);
  private readonly destroyRef   = inject(DestroyRef);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly isCurrentlyIssued = signal(false);
  protected readonly isLocationLocked = signal(false);
  protected readonly locationLockHint = signal('');
  protected readonly bookId     = signal<number | null>(null);
  protected readonly pageTitle  = signal('Add Book');

  protected readonly statusOptions   = computed(() =>
    this.isCurrentlyIssued()
      ? BOOK_STATUS_OPTIONS.filter(o => o.value === 'ISSUED')
      : BOOK_STATUS_OPTIONS.filter(o => o.value !== 'ISSUED')
  );
  protected readonly sourceOptions   = BOOK_SOURCE_OPTIONS;
  protected readonly categoryOptions = SUBJECT_CATEGORY_OPTIONS;
  protected readonly categories      = signal<LibrarySubjectCategory[]>([]);
  protected readonly racks           = signal<LibraryRack[]>([]);
  protected readonly shelves         = signal<LibraryShelf[]>([]);

  private libraryId: number | null = null;
  private savedRackId: number | string = '';
  private savedShelfId: number | string = '';

  protected form!: FormGroup;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.bookId.set(+id);
      this.pageTitle.set('Edit Book');
    }
    this.buildForm();
    this.form.get('status')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.syncLocationControlsState(true));
    this.syncLocationControlsState(false);

    this.libraryService.getSubjectCategories(true).subscribe({
      next: (cats) => this.categories.set(cats),
    });
    this.libraryService.getLibraries().subscribe({
      next: (libraries) => {
        this.libraryId = libraries[0]?.id ?? null;
        if (this.libraryId) {
          this.libraryService.getRacks(this.libraryId, true).subscribe({ next: (racks) => this.racks.set(racks) });
        }
      },
    });
    if (id) {
      this.loadBook(+id);
    }
  }

  protected onRackChange(): void {
    this.form.patchValue({ shelfId: '' });
    const rackId = this.form.getRawValue().rackId;
    if (!rackId) {
      this.shelves.set([]);
      this.form.get('shelfId')?.disable({ emitEvent: false });
      return;
    }
    if (!this.isLocationLocked()) {
      this.form.get('shelfId')?.enable({ emitEvent: false });
    }
    this.libraryService.getShelves(+rackId, undefined, true).subscribe({ next: (shelves) => this.shelves.set(shelves) });
  }

  private syncLocationControlsState(restoreSavedOnLock: boolean): void {
    const status = (this.form.getRawValue().status || 'AVAILABLE') as BookStatus;
    const locked = status === 'ISSUED' || status === 'LOST' || status === 'WITHDRAWN';
    this.isLocationLocked.set(locked);

    if (locked) {
      this.locationLockHint.set(status === 'ISSUED' ? 'Locked while Issued' : 'Last known location');
      if (restoreSavedOnLock && this.isEditMode()) {
        const currentRackId = this.form.getRawValue().rackId;
        this.form.patchValue(
          { rackId: this.savedRackId, shelfId: this.savedShelfId },
          { emitEvent: false },
        );
        if (this.savedRackId && String(currentRackId) !== String(this.savedRackId)) {
          this.libraryService.getShelves(+this.savedRackId, undefined, true).subscribe({
            next: (shelves) => this.shelves.set(shelves),
          });
        } else if (!this.savedRackId) {
          this.shelves.set([]);
        }
      }
      this.form.get('rackId')?.disable({ emitEvent: false });
      this.form.get('shelfId')?.disable({ emitEvent: false });
    } else {
      this.locationLockHint.set('');
      this.form.get('rackId')?.enable({ emitEvent: false });
      if (this.form.getRawValue().rackId) {
        this.form.get('shelfId')?.enable({ emitEvent: false });
      } else {
        this.form.get('shelfId')?.disable({ emitEvent: false });
      }
    }
  }

  private buildForm(): void {
    const excludeId = this.bookId();
    this.form = this.fb.group({
      accessionNumber:    ['', { asyncValidators: [this.accessionNumberValidator(excludeId)], updateOn: 'blur' }],
      barcode:            ['', { asyncValidators: [this.barcodeValidator(excludeId)], updateOn: 'blur' }],
      entryDate:          [''],
      title:              ['', [Validators.required, Validators.maxLength(500)]],
      authors:            ['', [Validators.required, Validators.maxLength(500)]],
      publisher:          ['', Validators.maxLength(300)],
      yearOfPublication:  ['', Validators.maxLength(20)],
      edition:            ['', Validators.maxLength(100)],
      isbn:               ['', Validators.maxLength(30)],
      collation:          ['', Validators.maxLength(200)],
      series:             ['', Validators.maxLength(200)],
      callNumber:         ['', Validators.maxLength(50)],
      rackId:             [''],
      shelfId:            [''],
      subjectCategoryId:  [''],
      subjectCategory:    [''],
      sourceOfSupply:     [''],
      vendorDonorName:    ['', Validators.maxLength(200)],
      billNumber:         ['', Validators.maxLength(50)],
      billDate:           [''],
      priceRs:            [null],
      status:             ['AVAILABLE'],
      remarks:            [''],
    });
  }

  private accessionNumberValidator(excludeId: number | null): AsyncValidatorFn {
    return (control: AbstractControl) => {
      const value = control.value?.trim();
      if (!value) return Promise.resolve(null);
      return timer(350).pipe(
        switchMap(() => this.libraryService.checkAccessionNumberExists(value, excludeId ?? undefined)),
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
        switchMap(() => this.libraryService.checkBarcodeExists(value, excludeId ?? undefined)),
        map(res => res.exists ? { barcodeExists: true } : null),
        first(),
      );
    };
  }

  private loadBook(id: number): void {
    this.loading.set(true);
    this.libraryService.getById(id).subscribe({
      next: book => {
        const issued = book.status === 'ISSUED';
        this.isCurrentlyIssued.set(issued);
        this.savedRackId = book.rackId ?? '';
        this.savedShelfId = book.shelfId ?? '';
        this.form.patchValue({
          accessionNumber:   book.accessionNumber,
          barcode:           book.barcode ?? '',
          entryDate:         book.entryDate ?? '',
          title:             book.title,
          authors:           book.authors,
          publisher:         book.publisher ?? '',
          yearOfPublication: book.yearOfPublication ?? '',
          edition:           book.edition ?? '',
          isbn:              book.isbn ?? '',
          collation:         book.collation ?? '',
          series:            book.series ?? '',
          callNumber:        book.callNumber ?? '',
          rackId:            book.rackId ?? '',
          shelfId:           book.shelfId ?? '',
          subjectCategoryId: book.subjectCategoryId ?? '',
          subjectCategory:   book.subjectCategory ?? '',
          sourceOfSupply:    book.sourceOfSupply ?? '',
          vendorDonorName:   book.vendorDonorName ?? '',
          billNumber:        book.billNumber ?? '',
          billDate:          book.billDate ?? '',
          priceRs:           book.priceRs ?? null,
          status:            book.status,
          remarks:           book.remarks ?? '',
        }, { emitEvent: false });
        if (issued) {
          this.form.get('status')?.disable({ emitEvent: false });
        } else {
          this.form.get('status')?.enable({ emitEvent: false });
        }
        this.syncLocationControlsState(false);
        if (book.rackId) {
          this.libraryService.getShelves(book.rackId, undefined, true).subscribe({ next: (shelves) => this.shelves.set(shelves) });
        }
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load book');
        this.loading.set(false);
      },
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const request = this.buildRequest();
    const op = this.isEditMode()
      ? this.libraryService.update(this.bookId()!, request)
      : this.libraryService.create(request);

    op.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Book updated successfully' : 'Book added successfully');
        void this.router.navigate(['/library/books']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save book');
        this.saving.set(false);
      },
    });
  }

  protected cancel(): void {
    void this.router.navigate(['/library/books']);
  }

  protected get f() { return this.form.controls; }

  private buildRequest(): LibraryBookRequest {
    const v = this.form.getRawValue();
    return {
      accessionNumber:   v.accessionNumber?.trim() || undefined,
      barcode:           v.barcode?.trim() || undefined,
      entryDate:         v.entryDate || undefined,
      title:             v.title.trim(),
      authors:           v.authors.trim(),
      publisher:         v.publisher?.trim() || undefined,
      yearOfPublication: v.yearOfPublication?.trim() || undefined,
      edition:           v.edition?.trim() || undefined,
      isbn:              v.isbn?.trim() || undefined,
      collation:         v.collation?.trim() || undefined,
      series:            v.series?.trim() || undefined,
      callNumber:        v.callNumber?.trim() || undefined,
      libraryId:         this.libraryId!,
      shelfId:           v.shelfId || undefined,
      subjectCategoryId: v.subjectCategoryId ? +v.subjectCategoryId : undefined,
      subjectCategory:   this.categories().find(c => c.id === +v.subjectCategoryId)?.name || v.subjectCategory || undefined,
      sourceOfSupply:    v.sourceOfSupply || undefined,
      vendorDonorName:   v.vendorDonorName?.trim() || undefined,
      billNumber:        v.billNumber?.trim() || undefined,
      billDate:          v.billDate || undefined,
      priceRs:           v.priceRs ?? undefined,
      status:            v.status || 'AVAILABLE',
      remarks:           v.remarks?.trim() || undefined,
    };
  }
}
