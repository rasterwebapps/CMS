import { Component, inject, signal, computed } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ScholarshipEligibility, ScholarshipEligibilityRequest } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface EligibilityEditDialogData {
  studentId: number;
  studentName: string;
  eligibility: ScholarshipEligibility | null;
}

@Component({
  selector: 'app-eligibility-edit-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './eligibility-edit-dialog.component.html',
  styleUrl: './eligibility-edit-dialog.component.scss',
})
export class EligibilityEditDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<EligibilityEditDialogComponent>);
  protected readonly data: EligibilityEditDialogData = inject(MAT_DIALOG_DATA);
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly toast = inject(ToastService);

  protected saving = signal(false);

  protected readonly form: FormGroup = this.fb.group({
    // Eligibility flags
    isFirstGraduate: [this.data.eligibility?.isFirstGraduate ?? false],
    isMeritBased: [this.data.eligibility?.isMeritBased ?? false],
    isSportsQuota: [this.data.eligibility?.isSportsQuota ?? false],
    isEconomicallyWeaker: [this.data.eligibility?.isEconomicallyWeaker ?? false],
    annualFamilyIncome: [this.data.eligibility?.annualFamilyIncome ?? null],
    // Income certificate
    incomeCertificateNumber: [this.data.eligibility?.incomeCertificateNumber ?? ''],
    incomeCertIssuingAuthority: [this.data.eligibility?.incomeCertIssuingAuthority ?? ''],
    incomeCertIssueDate: [this.data.eligibility?.incomeCertIssueDate ?? ''],
    // Community certificate
    communityCertificateNumber: [this.data.eligibility?.communityCertificateNumber ?? ''],
    commCertIssuingAuthority: [this.data.eligibility?.commCertIssuingAuthority ?? ''],
    commCertIssueDate: [this.data.eligibility?.commCertIssueDate ?? ''],
    // First graduate certificate
    firstGraduateCertificateNumber: [this.data.eligibility?.firstGraduateCertificateNumber ?? ''],
    firstGradCertIssuingAuthority: [this.data.eligibility?.firstGradCertIssuingAuthority ?? ''],
    firstGradCertIssueDate: [this.data.eligibility?.firstGradCertIssueDate ?? ''],
    fatherEducation: [this.data.eligibility?.fatherEducation ?? ''],
    motherEducation: [this.data.eligibility?.motherEducation ?? ''],
  });

  /** Income < ₹3L automatically qualifies as EWS — show hint */
  protected readonly ewsAutoHint = computed(() => {
    const income = this.form.get('annualFamilyIncome')?.value as number | null;
    return income !== null && income > 0 && income < 300000;
  });

  protected isFirstGraduate(): boolean {
    return !!this.form.get('isFirstGraduate')?.value;
  }

  protected onSubmit(): void {
    const v = this.form.value;
    const request: ScholarshipEligibilityRequest = {
      isFirstGraduate: v.isFirstGraduate,
      isMeritBased: v.isMeritBased,
      isSportsQuota: v.isSportsQuota,
      isEconomicallyWeaker: v.isEconomicallyWeaker,
      annualFamilyIncome: v.annualFamilyIncome !== '' ? v.annualFamilyIncome : null,
      incomeCertificateNumber: v.incomeCertificateNumber?.trim() || null,
      incomeCertIssuingAuthority: v.incomeCertIssuingAuthority?.trim() || null,
      incomeCertIssueDate: v.incomeCertIssueDate || null,
      communityCertificateNumber: v.communityCertificateNumber?.trim() || null,
      commCertIssuingAuthority: v.commCertIssuingAuthority?.trim() || null,
      commCertIssueDate: v.commCertIssueDate || null,
      firstGraduateCertificateNumber: v.firstGraduateCertificateNumber?.trim() || null,
      firstGradCertIssuingAuthority: v.firstGradCertIssuingAuthority?.trim() || null,
      firstGradCertIssueDate: v.firstGradCertIssueDate || null,
      fatherEducation: v.fatherEducation?.trim() || null,
      motherEducation: v.motherEducation?.trim() || null,
    };

    this.saving.set(true);
    this.scholarshipService.updateEligibility(this.data.studentId, request).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.toast.success('Eligibility profile updated');
        this.dialogRef.close(updated);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to update eligibility');
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

