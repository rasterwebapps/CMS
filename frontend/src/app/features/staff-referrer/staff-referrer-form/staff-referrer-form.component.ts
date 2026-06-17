import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { StaffReferrerService } from '../staff-referrer.service';
import { StaffReferrerRequest, STAFF_REFERRER_BANK_ACCOUNT_TYPE_OPTIONS } from '../staff-referrer.model';
import { ToastService } from '../../../core/toast/toast.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-staff-referrer-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSlideToggleModule,
    CmsPreviewCardComponent, CmsTipsCardComponent,
  ],
  templateUrl: './staff-referrer-form.component.html',
  styleUrl: './staff-referrer-form.component.scss',
})
export class StaffReferrerFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(StaffReferrerService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Staff Referrer');

  protected readonly bankAccountTypeOptions = STAFF_REFERRER_BANK_ACCOUNT_TYPE_OPTIONS;

  protected readonly previewName = signal('');
  protected readonly previewPhone = signal('');
  protected readonly previewInstitution = signal('');
  protected readonly previewComm = signal<number | null>(null);
  protected readonly previewActive = signal(true);
  protected readonly previewInitials = computed(() => {
    const n = this.previewName();
    if (!n) return '';
    const parts = n.split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase();
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'person',         title: 'Identity',     subtitle: "Use the staff member's legal name as it will appear on payout records." },
    { icon: 'location_on',    title: 'Institution',  subtitle: 'Enter the name of the sister concern college this staff belongs to.' },
    { icon: 'currency_rupee', title: 'Commission',   subtitle: 'Override the STAFF referral type commission only if this person has a special rate.' },
    { icon: 'account_balance', title: 'Bank Details', subtitle: 'Required to disburse commission payouts. PAN + bank info should match.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    phone: [''],
    email: [''],
    institution: [''],
    commissionAmount: [null as number | null, [Validators.min(0)]],
    isActive: [true],
    panNumber: ['', [Validators.maxLength(20)]],
    aadhaarNumber: ['', [Validators.maxLength(20)]],
    bankAccountHolder: [''],
    bankAccountNumber: [''],
    bankIfscCode: [''],
    bankName: [''],
    bankBranch: [''],
    bankAccountType: [null as string | null],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewPhone.set((v.phone ?? '').trim());
        this.previewInstitution.set((v.institution ?? '').trim());
        this.previewComm.set(v.commissionAmount != null && v.commissionAmount !== '' ? Number(v.commissionAmount) : null);
        this.previewActive.set(!!v.isActive);
      });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Staff Referrer');
      this.loading.set(true);
      this.service.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            name: item.name, phone: item.phone, email: item.email,
            institution: item.institution,
            commissionAmount: item.commissionAmount,
            isActive: item.isActive,
            panNumber: item.panNumber || '',
            aadhaarNumber: item.aadhaarNumber || '',
            bankAccountHolder: item.bankAccountHolder || '',
            bankAccountNumber: item.bankAccountNumber || '',
            bankIfscCode: item.bankIfscCode || '',
            bankName: item.bankName || '',
            bankBranch: item.bankBranch || '',
            bankAccountType: item.bankAccountType ?? null,
          });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/staff-referrers']); },
      });
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/staff-referrers/name-exists`, () => this.itemId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
    const v = this.form.value;
    const request: StaffReferrerRequest = {
      name: v.name.trim(),
      phone: v.phone || undefined,
      email: v.email || undefined,
      institution: v.institution?.trim() || undefined,
      commissionAmount: v.commissionAmount ?? undefined,
      isActive: v.isActive,
      panNumber: v.panNumber?.trim()?.toUpperCase() || undefined,
      aadhaarNumber: v.aadhaarNumber?.trim() || undefined,
      bankAccountHolder: v.bankAccountHolder?.trim() || undefined,
      bankAccountNumber: v.bankAccountNumber?.trim() || undefined,
      bankIfscCode: v.bankIfscCode?.trim()?.toUpperCase() || undefined,
      bankName: v.bankName?.trim() || undefined,
      bankBranch: v.bankBranch?.trim() || undefined,
      bankAccountType: v.bankAccountType || undefined,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.service.update(this.itemId!, request)
      : this.service.create(request);
    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Updated' : 'Created');
        void this.router.navigate(['/staff-referrers']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save');
        this.saving.set(false);
      },
    });
  }
}
