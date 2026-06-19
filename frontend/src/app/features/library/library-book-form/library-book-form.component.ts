import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AsyncValidatorFn, AbstractControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, map, switchMap, first } from 'rxjs/operators';
import { timer } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { LibraryService } from '../library.service';
import {
  LibraryBookRequest,
  BOOK_STATUS_OPTIONS,
  BOOK_SOURCE_OPTIONS,
  SUBJECT_CATEGORY_OPTIONS,
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
  protected readonly bookId     = signal<number | null>(null);
  protected readonly pageTitle  = signal('Add Book');

  protected readonly statusOptions   = BOOK_STATUS_OPTIONS;
  protected readonly sourceOptions   = BOOK_SOURCE_OPTIONS;
  protected readonly categoryOptions = SUBJECT_CATEGORY_OPTIONS;

  protected form!: FormGroup;

  ngOnInit(): void {
    this.buildForm();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.bookId.set(+id);
      this.pageTitle.set('Edit Book');
      this.loadBook(+id);
    }
  }

  private buildForm(): void {
    const excludeId = this.bookId();
    this.form = this.fb.group({
      accessionNumber:    ['', { asyncValidators: [this.accessionNumberValidator(excludeId)], updateOn: 'blur' }],
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
      shelfLocation:      ['', Validators.maxLength(20)],
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

  private loadBook(id: number): void {
    this.loading.set(true);
    this.libraryService.getById(id).subscribe({
      next: book => {
        this.form.patchValue({
          accessionNumber:   book.accessionNumber,
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
          shelfLocation:     book.shelfLocation ?? '',
          subjectCategory:   book.subjectCategory ?? '',
          sourceOfSupply:    book.sourceOfSupply ?? '',
          vendorDonorName:   book.vendorDonorName ?? '',
          billNumber:        book.billNumber ?? '',
          billDate:          book.billDate ?? '',
          priceRs:           book.priceRs ?? null,
          status:            book.status,
          remarks:           book.remarks ?? '',
        });
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
    const v = this.form.value;
    return {
      accessionNumber:   v.accessionNumber?.trim() || undefined,
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
      shelfLocation:     v.shelfLocation?.trim() || undefined,
      subjectCategory:   v.subjectCategory || undefined,
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
