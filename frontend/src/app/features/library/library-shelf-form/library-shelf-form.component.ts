import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { LibraryService } from '../library.service';
import { LibraryShelfRequest } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import {
  noConsecutiveSpaces,
  noInternalSpaces,
  trimmedMinLength,
  cmsFieldError,
  stripSpaces,
} from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

const SHELF_FORM_IMPORTS = [
  RouterLink,
  ReactiveFormsModule,
  MatProgressSpinnerModule,
  MatSlideToggleModule,
  CmsPreviewCardComponent,
  CmsTipsCardComponent,
];

@Component({
  selector: 'app-library-shelf-form',
  standalone: true,
  imports: SHELF_FORM_IMPORTS,
  templateUrl: './library-shelf-form.component.html',
  styleUrl: './library-shelf-form.component.scss',
})
export class LibraryShelfFormComponent implements OnInit {
  private readonly fb             = inject(FormBuilder);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);
  private readonly destroyRef     = inject(DestroyRef);
  private readonly http           = inject(HttpClient);

  protected readonly loading    = signal(true);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Shelf');
  protected readonly rackName   = signal('');
  protected readonly rackCode   = signal('');

  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewDesc = signal('');
  protected readonly previewActive = signal(true);

  protected readonly TIPS: CmsTip[] = [
    {
      icon: 'shelves',
      title: 'Shelf tiers',
      subtitle: 'A shelf represents a tier within the rack, e.g. Top, Middle or Bottom.',
    },
    {
      icon: 'tag',
      title: 'Shelf code',
      subtitle: 'A short, unique code within this rack, e.g. TOP, MID, BTM. Spaces are removed automatically.',
    },
    {
      icon: 'visibility',
      title: 'Active status',
      subtitle: 'Inactive shelves stay linked to existing books but are hidden from new book assignments.',
    },
  ];

  private shelfId: number | null = null;
  private rackId!: number;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    code:        ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    description: ['', [Validators.maxLength(500)]],
    isActive:    [true],
  });

  constructor() {
    this.form.get('code')!.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v: string) => {
      const cleaned = stripSpaces(v ?? '').toUpperCase();
      if (cleaned !== v) this.form.get('code')!.setValue(cleaned, { emitEvent: false });
    });

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(v => {
      this.previewName.set((v.name ?? '').trim());
      this.previewCode.set(stripSpaces(v.code ?? '').toUpperCase());
      this.previewDesc.set((v.description ?? '').trim());
      this.previewActive.set(!!v.isActive);
    });
  }

  ngOnInit(): void {
    const rackIdParam = this.route.snapshot.paramMap.get('rackId');
    if (!rackIdParam) {
      void this.router.navigate(['/library/racks']);
      return;
    }
    this.rackId = Number(rackIdParam);

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.shelfId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Shelf');
    }

    this.setupUniquenessValidators();

    this.libraryService.getRackById(this.rackId).subscribe({
      next: (rack) => {
        this.rackName.set(rack.name);
        this.rackCode.set(rack.code);
        if (this.shelfId) {
          this.loadShelf(this.shelfId);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.toast.error('Failed to load rack details');
        void this.router.navigate(['/library/racks']);
      },
    });
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/library/shelves/name-exists`,
          () => this.shelfId,
          () => ({ rackId: this.rackId }),
        ),
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/library/shelves/code-exists`,
          () => this.shelfId,
          () => ({ rackId: this.rackId }),
        ),
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: LibraryShelfRequest = {
      rackId:      this.rackId,
      name:        (this.form.value.name ?? '').trim(),
      code:        stripSpaces(this.form.value.code ?? '').toUpperCase(),
      description: this.form.value.description?.trim() || undefined,
      isActive:    this.form.value.isActive,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.libraryService.updateShelf(this.shelfId!, request)
      : this.libraryService.createShelf(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Shelf updated successfully' : 'Shelf created successfully');
        this.saving.set(false);
        void this.router.navigate(['/library/racks', this.rackId, 'shelves']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update shelf' : 'Failed to create shelf'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', description: 'Description',
  };

  protected getFieldError(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), LibraryShelfFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  protected get shelvesListLink(): unknown[] {
    return ['/library/racks', this.rackId, 'shelves'];
  }

  private loadShelf(id: number): void {
    this.libraryService.getShelfById(id).subscribe({
      next: (shelf) => {
        this.form.patchValue({
          name: shelf.name,
          code: shelf.code,
          description: shelf.description || '',
          isActive: shelf.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load shelf');
        void this.router.navigate(['/library/racks', this.rackId, 'shelves']);
      },
    });
  }
}
