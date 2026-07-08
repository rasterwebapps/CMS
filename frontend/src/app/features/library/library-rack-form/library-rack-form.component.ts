import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { LibraryService } from '../library.service';
import { LibraryRackRequest } from '../library.model';
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

const RACK_FORM_IMPORTS = [
  RouterLink,
  ReactiveFormsModule,
  MatProgressSpinnerModule,
  MatSlideToggleModule,
  CmsPreviewCardComponent,
  CmsTipsCardComponent,
];

@Component({
  selector: 'app-library-rack-form',
  standalone: true,
  imports: RACK_FORM_IMPORTS,
  templateUrl: './library-rack-form.component.html',
  styleUrl: './library-rack-form.component.scss',
})
export class LibraryRackFormComponent implements OnInit {
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
  protected readonly pageTitle  = signal('Add Rack');
  protected readonly libraryName = signal('');

  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewDesc = signal('');
  protected readonly previewActive = signal(true);

  protected readonly TIPS: CmsTip[] = [
    {
      icon: 'inventory_2',
      title: 'Rack code',
      subtitle: 'A short, unique code used across the catalogue, e.g. R-01. Spaces are removed automatically.',
    },
    {
      icon: 'shelves',
      title: 'Add shelves next',
      subtitle: 'After saving this rack, add its shelf tiers (Top / Middle / Bottom) from the rack list.',
    },
    {
      icon: 'visibility',
      title: 'Active status',
      subtitle: 'Inactive racks stay linked to existing books but are hidden from new shelf and book assignments.',
    },
  ];

  private rackId: number | null = null;
  private libraryId: number | null = null;

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
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.rackId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Rack');
    }

    this.libraryService.getLibraries().subscribe({
      next: (libraries) => {
        const library = libraries[0];
        if (!library) {
          this.toast.error('No library is configured yet. Please contact an administrator.');
          void this.router.navigate(['/library/racks']);
          return;
        }
        this.libraryId = library.id;
        this.libraryName.set(library.name);
        this.setupUniquenessValidators();

        if (this.rackId) {
          this.loadRack(this.rackId);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.toast.error('Failed to load library details');
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
          `${environment.apiUrl}/library/racks/name-exists`,
          () => this.rackId,
          () => (this.libraryId != null ? { libraryId: this.libraryId } : null),
        ),
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/library/racks/code-exists`,
          () => this.rackId,
          () => (this.libraryId != null ? { libraryId: this.libraryId } : null),
        ),
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid || this.libraryId == null) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: LibraryRackRequest = {
      libraryId:   this.libraryId,
      name:        (this.form.value.name ?? '').trim(),
      code:        stripSpaces(this.form.value.code ?? '').toUpperCase(),
      description: this.form.value.description?.trim() || undefined,
      isActive:    this.form.value.isActive,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.libraryService.updateRack(this.rackId!, request)
      : this.libraryService.createRack(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Rack updated successfully' : 'Rack created successfully');
        this.saving.set(false);
        void this.router.navigate(['/library/racks']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update rack' : 'Failed to create rack'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', description: 'Description',
  };

  protected getFieldError(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), LibraryRackFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadRack(id: number): void {
    this.libraryService.getRackById(id).subscribe({
      next: (rack) => {
        this.form.patchValue({
          name: rack.name,
          code: rack.code,
          description: rack.description || '',
          isActive: rack.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load rack');
        void this.router.navigate(['/library/racks']);
      },
    });
  }
}
