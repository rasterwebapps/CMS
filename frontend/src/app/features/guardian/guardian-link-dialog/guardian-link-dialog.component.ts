import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GuardianService } from '../guardian.service';
import { GuardianResponse } from '../guardian.model';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
import { cmsFieldError } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { ToastService } from '../../../core/toast/toast.service';

export interface GuardianLinkDialogData {
  studentId: number;
  studentName: string;
  /** Guardian ids already linked to this student -- excluded from the "existing guardian" picker
   *  so the admin can't try to link the same guardian twice (the backend would 400 anyway, but
   *  filtering client-side avoids a confusing failed submit). */
  alreadyLinkedGuardianIds: number[];
}

/** Create-or-select-and-link dialog for the Student Detail Guardians tab. Short form -- Save/
 *  Cancel at the bottom only, no sticky footer, per this codebase's form-length convention. */
@Component({
  selector: 'app-guardian-link-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './guardian-link-dialog.component.html',
  styleUrl: './guardian-link-dialog.component.scss',
})
export class GuardianLinkDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly dialogRef = inject(MatDialogRef<GuardianLinkDialogComponent>);
  protected readonly data: GuardianLinkDialogData = inject(MAT_DIALOG_DATA);
  private readonly guardianService = inject(GuardianService);
  private readonly toast = inject(ToastService);

  protected readonly mode = signal<'existing' | 'new'>('existing');
  protected readonly saving = signal(false);
  protected readonly loadingGuardians = signal(false);
  protected readonly availableGuardians = signal<GuardianResponse[]>([]);

  protected readonly existingForm: FormGroup = this.fb.group({
    guardianId: [null, Validators.required],
    isPrimary: [false],
  });

  protected readonly newForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    relationshipHint: [''],
    isPrimary: [false],
  });

  ngOnInit(): void {
    this.loadingGuardians.set(true);
    this.guardianService.findAll().subscribe({
      next: (guardians) => {
        this.availableGuardians.set(
          guardians.filter((g) => !this.data.alreadyLinkedGuardianIds.includes(g.id)),
        );
        this.loadingGuardians.set(false);
      },
      error: () => {
        this.toast.error('Failed to load guardians');
        this.loadingGuardians.set(false);
      },
    });

    this.newForm.get('email')?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/guardians/email-exists`),
    );
  }

  protected setMode(mode: 'existing' | 'new'): void {
    this.mode.set(mode);
  }

  protected onSave(): void {
    if (this.mode() === 'existing') {
      this.saveExisting();
    } else {
      this.saveNew();
    }
  }

  private saveExisting(): void {
    if (this.existingForm.invalid) return;
    const { guardianId, isPrimary } = this.existingForm.value;
    this.saving.set(true);
    this.guardianService.linkWard(guardianId, this.data.studentId, isPrimary).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Guardian linked');
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to link guardian');
      },
    });
  }

  private saveNew(): void {
    if (this.newForm.invalid) return;
    const { firstName, lastName, email, phone, relationshipHint, isPrimary } = this.newForm.value;
    this.saving.set(true);
    this.guardianService.create({ firstName, lastName, email, phone, relationshipHint }).subscribe({
      next: (guardian) => {
        this.guardianService.linkWard(guardian.id, this.data.studentId, isPrimary).subscribe({
          next: () => {
            this.saving.set(false);
            this.toast.success('Guardian created and linked');
            this.dialogRef.close(true);
          },
          error: (err) => {
            this.saving.set(false);
            this.toast.error(err?.error?.message ?? 'Guardian created, but linking failed');
          },
        });
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to create guardian');
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }

  protected getNewFieldError(field: string): string {
    const labels: Record<string, string> = {
      firstName: 'First name', lastName: 'Last name', email: 'Email',
    };
    return cmsFieldError(this.newForm.get(field), labels[field] ?? field);
  }
}
