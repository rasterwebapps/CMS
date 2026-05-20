import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../core/toast/toast.service';
import { ScholarshipTypeRequest } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';

@Component({
  selector: 'app-scholarship-type-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './scholarship-type-form.component.html',
  styleUrl: './scholarship-type-form.component.scss',
})
export class ScholarshipTypeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  private scholarshipId: number | null = null;

  protected readonly discountTypes = ['PERCENTAGE', 'FIXED_AMOUNT', 'FULL_WAIVER'];

  protected readonly form = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(20), noInternalSpaces()]],
    name: ['', [Validators.required, Validators.maxLength(100), trimmedMinLength(2), noConsecutiveSpaces()]],
    description: [''],
    govtScheme: [false],
    schemeCode: [''],
    discountType: ['FIXED_AMOUNT', Validators.required],
    discountValue: [0, [Validators.min(0)]],
    maxAmountPerYear: [null as number | null, [Validators.min(0)]],
    renewalRequired: [false],
    active: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.scholarshipId = Number(id);
      this.isEditMode.set(true);
      this.load();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const request: ScholarshipTypeRequest = {
      code: (v.code ?? '').trim().toUpperCase(),
      name: (v.name ?? '').trim(),
      description: v.description?.trim() || null,
      govtScheme: !!v.govtScheme,
      schemeCode: v.schemeCode?.trim() || null,
      discountType: v.discountType as ScholarshipTypeRequest['discountType'],
      discountValue: Number(v.discountValue ?? 0),
      maxAmountPerYear: v.maxAmountPerYear != null ? Number(v.maxAmountPerYear) : null,
      renewalRequired: !!v.renewalRequired,
      active: !!v.active,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.scholarshipService.updateScholarshipType(this.scholarshipId!, request)
      : this.scholarshipService.createScholarshipType(request);
    op$.subscribe({
      next: () => { this.toast.success('Scholarship saved'); void this.router.navigate(['/scholarships']); },
      error: err => { this.toast.error(err?.error?.message ?? 'Failed to save scholarship'); this.saving.set(false); },
    });
  }

  private load(): void {
    if (!this.scholarshipId) return;
    this.loading.set(true);
    this.scholarshipService.getScholarshipType(this.scholarshipId).subscribe({
      next: s => { this.form.patchValue(s); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load scholarship'); void this.router.navigate(['/scholarships']); },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = {
      code: 'Code',
      name: 'Name',
      schemeCode: 'Scheme Code',
      discountValue: 'Discount Value',
      maxAmountPerYear: 'Max Amount Per Year',
    };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }
}

