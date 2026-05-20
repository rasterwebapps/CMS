import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { IndiaLocationService } from '../india-location.service';
import { CountryRequest } from '../india-location.model';
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

const COUNTRY_FORM_IMPORTS = [
  RouterLink,
  ReactiveFormsModule,
  MatProgressSpinnerModule,
  MatSlideToggleModule,
  CmsPreviewCardComponent,
  CmsTipsCardComponent,
];

@Component({
  selector: 'app-country-form',
  standalone: true,
  imports: COUNTRY_FORM_IMPORTS,
  templateUrl: './country-form.component.html',
  styleUrl: './country-form.component.scss',
})
export class CountryFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(IndiaLocationService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Country');
  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewActive = signal(true);

  protected readonly TIPS: CmsTip[] = [
    {
      icon: 'public',
      title: 'Country name',
      subtitle: 'Use the official country name as it should appear in admission and address forms.',
    },
    {
      icon: 'tag',
      title: 'ISO code',
      subtitle: 'Enter the 2–3 letter ISO code. Spaces are removed and letters are stored in uppercase.',
    },
    {
      icon: 'visibility',
      title: 'Active status',
      subtitle: 'Inactive countries stay in history but are hidden from new location selections.',
    },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: [
      '',
      [Validators.required, trimmedMinLength(1), Validators.maxLength(150), noConsecutiveSpaces()],
    ],
    isoCode: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(3), noInternalSpaces()]],
    isActive: [true],
  });

  constructor() {
    this.form
      .get('isoCode')!
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string) => {
        const cleaned = stripSpaces(v ?? '').toUpperCase();
        if (cleaned !== v) this.form.get('isoCode')!.setValue(cleaned, { emitEvent: false });
      });

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      this.previewName.set((v.name ?? '').trim());
      this.previewCode.set(stripSpaces(v.isoCode ?? '').toUpperCase());
      this.previewActive.set(!!v.isActive);
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Country');
      this.loadItem(this.itemId);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    this.saving.set(true);
    const request: CountryRequest = {
      name: this.form.value.name,
      isoCode: this.form.value.isoCode,
      isActive: this.form.value.isActive,
    };
    const op = this.isEditMode()
      ? this.service.updateCountry(this.itemId!, request)
      : this.service.createCountry(request);
    op.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Country updated' : 'Country created');
        void this.router.navigate(['/india-locations']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save country');
        this.saving.set(false);
      },
    });
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.service.getCountryById(id).subscribe({
      next: (item) => {
        this.form.patchValue({ name: item.name, isoCode: item.isoCode, isActive: item.isActive });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load country');
        this.loading.set(false);
      },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = { name: 'Name', isoCode: 'ISO Code' };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }
}

