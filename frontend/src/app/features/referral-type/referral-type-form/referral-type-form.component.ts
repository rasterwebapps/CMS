import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ReferralTypeService } from '../referral-type.service';
import { ReferralTypeRequest } from '../referral-type.model';

@Component({
  selector: 'app-referral-type-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatSlideToggleModule,
  ],
  templateUrl: './referral-type-form.component.html',
  styleUrl: './referral-type-form.component.scss',
})
export class ReferralTypeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Referral Type');

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    code: ['', [Validators.required, Validators.maxLength(50)]],
    hasCommission: [false],
    commissionAmount: [0, [Validators.required, Validators.min(0)]],
    description: [''],
    isActive: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Referral Type');
      this.loading.set(true);
      this.referralTypeService.getReferralTypeById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            name: item.name,
            code: item.code,
            hasCommission: item.hasCommission,
            commissionAmount: item.commissionAmount,
            description: item.description,
            isActive: item.isActive,
          });
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load', 'Close', { duration: 3000 });
          void this.router.navigate(['/referral-types']);
        },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: ReferralTypeRequest = {
      name: v.name.trim(),
      code: v.code.trim(),
      hasCommission: v.hasCommission,
      commissionAmount: v.commissionAmount,
      description: v.description || undefined,
      isActive: v.isActive,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.referralTypeService.updateReferralType(this.itemId!, request)
      : this.referralTypeService.createReferralType(request);
    op$.subscribe({
      next: () => {
        this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 });
        void this.router.navigate(['/referral-types']);
      },
      error: (err) => {
        const message = err?.error?.message ?? (this.isEditMode() ? 'Failed to update' : 'Failed to save');
        this.snackBar.open(message, 'Close', { duration: 4000 });
        this.saving.set(false);
      },
    });
  }
}
