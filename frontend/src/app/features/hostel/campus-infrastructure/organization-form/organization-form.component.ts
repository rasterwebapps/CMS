import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { CampusInfrastructureService } from '../campus-infrastructure.service';
import { OrganizationRequest } from '../campus-infrastructure.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-organization-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, CmsPreviewCardComponent,
  ],
  templateUrl: './organization-form.component.html',
  styleUrl: './organization-form.component.scss',
})
export class OrganizationFormComponent implements OnInit {
  private readonly fb         = inject(FormBuilder);
  private readonly route      = inject(ActivatedRoute);
  private readonly router     = inject(Router);
  private readonly service    = inject(CampusInfrastructureService);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http       = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Organization');

  protected readonly previewName   = signal('');
  protected readonly previewCode   = signal('');
  protected readonly codeCharCount = signal(0);

  private organizationId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    code:        ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    description: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        const code = (v.code ?? '').toUpperCase().trim();
        this.previewCode.set(code);
        this.codeCharCount.set(code.length);
      });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.organizationId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Organization');
      this.loadOrganization();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/campus-infrastructure/organizations/name-exists`, () => this.organizationId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/campus-infrastructure/organizations/code-exists`, () => this.organizationId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end   = input.selectionEnd ?? 0;
    const cleaned = stripSpaces(input.value).toUpperCase();
    if (cleaned !== input.value) {
      this.form.get('code')?.setValue(cleaned, { emitEvent: true });
      setTimeout(() => input.setSelectionRange(start, end), 0);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }

    const request: OrganizationRequest = {
      name:        (this.form.value.name ?? '').trim(),
      code:        (this.form.value.code ?? '').trim().toUpperCase(),
      description: this.form.value.description?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.service.updateOrganization(this.organizationId!, request)
      : this.service.createOrganization(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Organization updated successfully' : 'Organization created successfully');
        this.saving.set(false);
        void this.router.navigate(['/campus-infrastructure/table']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update organization' : 'Failed to create organization'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), OrganizationFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadOrganization(): void {
    if (!this.organizationId) return;
    this.loading.set(true);
    this.service.getOrganizationById(this.organizationId).subscribe({
      next: (o) => {
        this.form.patchValue({ name: o.name, code: o.code, description: o.description || '' });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load organization');
        void this.router.navigate(['/campus-infrastructure/table']);
      },
    });
  }
}
