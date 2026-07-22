import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CampusInfrastructureService } from '../campus-infrastructure.service';
import { BranchRequest, Organization } from '../campus-infrastructure.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

@Component({
  selector: 'app-branch-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, CmsPreviewCardComponent,
  ],
  templateUrl: './branch-form.component.html',
  styleUrl: './branch-form.component.scss',
})
export class BranchFormComponent implements OnInit {
  private readonly fb      = inject(FormBuilder);
  private readonly route   = inject(ActivatedRoute);
  private readonly router  = inject(Router);
  private readonly service = inject(CampusInfrastructureService);
  private readonly toast   = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Branch');
  protected readonly organizations = signal<Organization[]>([]);

  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');

  private branchId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:           ['', [Validators.required, Validators.maxLength(100)]],
    code:           ['', [Validators.required, Validators.maxLength(50)]],
    description:    ['', [Validators.maxLength(500)]],
    organizationId: [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.service.getOrganizations(false).subscribe({ next: (o) => this.organizations.set(o) });

    this.form.valueChanges.subscribe(v => {
      this.previewName.set((v.name ?? '').trim());
      this.previewCode.set((v.code ?? '').toUpperCase().trim());
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.branchId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Branch');
      this.loadBranch();
    } else {
      const organizationId = Number(this.route.snapshot.queryParamMap.get('organizationId')) || null;
      if (organizationId) this.form.patchValue({ organizationId });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }

    const organizationId = this.form.value.organizationId;
    const request: BranchRequest = {
      name:        (this.form.value.name ?? '').trim(),
      code:        (this.form.value.code ?? '').trim().toUpperCase(),
      description: this.form.value.description?.trim() || undefined,
      organizationId,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.service.updateBranch(this.branchId!, request)
      : this.service.createBranch(organizationId, request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Branch updated successfully' : 'Branch created successfully');
        this.saving.set(false);
        void this.router.navigate(['/campus-infrastructure']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update branch' : 'Failed to create branch'));
        this.saving.set(false);
      },
    });
  }

  private loadBranch(): void {
    if (!this.branchId) return;
    this.loading.set(true);
    this.service.getBranchById(this.branchId).subscribe({
      next: (b) => {
        this.form.patchValue({
          name: b.name, code: b.code, description: b.description || '', organizationId: b.organizationId,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load branch');
        void this.router.navigate(['/campus-infrastructure']);
      },
    });
  }
}
