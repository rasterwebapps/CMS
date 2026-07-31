import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { CampusInfrastructureService } from '../campus-infrastructure.service';
import { BlockRequest, GenderRestriction, Organization } from '../campus-infrastructure.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-block-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, FormsModule, MatCheckboxModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, CmsPreviewCardComponent,
  ],
  templateUrl: './block-form.component.html',
  styleUrl: './block-form.component.scss',
})
export class BlockFormComponent implements OnInit {
  private readonly fb      = inject(FormBuilder);
  private readonly route   = inject(ActivatedRoute);
  private readonly router  = inject(Router);
  private readonly service = inject(CampusInfrastructureService);
  private readonly toast   = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Block');
  protected readonly organizations = signal<Organization[]>([]);
  protected readonly branches      = signal<{ id: number; name: string; code: string }[]>([]);

  protected selectedOrganizationId: number | null = null;

  protected readonly previewName     = signal('');
  protected readonly previewCode     = signal('');
  protected readonly previewIsHostel = signal(false);
  protected readonly previewGender   = signal<GenderRestriction | null>(null);
  protected readonly codeCharCount   = signal(0);

  private blockId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:              ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    code:              ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    description:       ['', [Validators.maxLength(500)]],
    isHostel:          [false],
    genderRestriction: [null as GenderRestriction | null],
    branchId:          [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.service.getOrganizations(false).subscribe({ next: (o) => this.organizations.set(o) });

    this.form.valueChanges.subscribe(v => {
      this.previewName.set((v.name ?? '').trim());
      const code = (v.code ?? '').toUpperCase().trim();
      this.previewCode.set(code);
      this.codeCharCount.set(code.length);
      this.previewIsHostel.set(!!v.isHostel);
      this.previewGender.set(v.genderRestriction ?? null);
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.blockId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Block');
      this.loadBlock();
    } else {
      const branchId = Number(this.route.snapshot.queryParamMap.get('branchId')) || null;
      if (branchId) {
        this.service.getBranchById(branchId).subscribe({
          next: (b) => {
            this.selectedOrganizationId = b.organizationId;
            this.loadBranchesForOrganization(b.organizationId);
            this.form.patchValue({ branchId });
          },
        });
      }
    }
  }

  protected onOrganizationChange(organizationId: number | null): void {
    this.selectedOrganizationId = organizationId;
    this.branches.set([]);
    this.form.patchValue({ branchId: null });
    if (organizationId) this.loadBranchesForOrganization(organizationId);
  }

  private loadBranchesForOrganization(organizationId: number): void {
    this.service.getBranchesByOrganization(organizationId).subscribe({ next: (b) => this.branches.set(b) });
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

    const branchId = this.form.value.branchId;
    const request: BlockRequest = {
      name:              (this.form.value.name ?? '').trim(),
      code:              (this.form.value.code ?? '').trim().toUpperCase(),
      description:       this.form.value.description?.trim() || undefined,
      isHostel:          !!this.form.value.isHostel,
      genderRestriction: this.form.value.genderRestriction ?? null,
      branchId,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.service.updateBlock(this.blockId!, request)
      : this.service.createBlock(branchId, request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Block updated successfully' : 'Block created successfully');
        this.saving.set(false);
        void this.router.navigate(['/campus-infrastructure/table']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update block' : 'Failed to create block'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), BlockFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadBlock(): void {
    if (!this.blockId) return;
    this.loading.set(true);
    this.service.getBlockById(this.blockId).subscribe({
      next: (b) => {
        this.form.patchValue({
          name: b.name, code: b.code, description: b.description || '',
          isHostel: b.isHostel, genderRestriction: b.genderRestriction, branchId: b.branchId,
        });
        this.service.getBranchById(b.branchId).subscribe({
          next: (br) => {
            this.selectedOrganizationId = br.organizationId;
            this.loadBranchesForOrganization(br.organizationId);
          },
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load block');
        void this.router.navigate(['/campus-infrastructure/table']);
      },
    });
  }
}
