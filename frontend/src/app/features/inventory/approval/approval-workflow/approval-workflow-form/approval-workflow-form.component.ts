import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApprovalWorkflowService } from '../approval-workflow.service';
import { ApprovalWorkflowRequest, ApprovalWorkflowStepRequest } from '../approval-workflow.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-approval-workflow-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './approval-workflow-form.component.html',
  styleUrl: './approval-workflow-form.component.scss',
})
export class ApprovalWorkflowFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly workflowService = inject(ApprovalWorkflowService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly locations  = signal<InventoryLocation[]>([]);

  private workflowId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:          ['', [Validators.required, Validators.maxLength(150)]],
    documentType:  ['PURCHASE_REQUISITION', [Validators.required]],
    locationId:    [null as number | null],
    minAmount:     [null as number | null, [Validators.min(0)]],
    steps:         this.fb.array([]),
  });

  protected get steps(): FormArray {
    return this.form.get('steps') as FormArray;
  }

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.workflowId = Number(idParam);
      this.isEditMode.set(true);
      this.loadWorkflow();
    } else {
      this.addStep();
    }
  }

  protected addStep(): void {
    const nextOrder = this.steps.length > 0 ? Math.max(...this.steps.controls.map((c) => c.get('stepOrder')?.value ?? 0)) + 1 : 1;
    this.steps.push(this.fb.group({
      stepOrder:      [nextOrder, [Validators.required, Validators.min(1)]],
      stepName:       ['', [Validators.required, Validators.maxLength(150)]],
      permissionCode: ['', [Validators.required]],
    }));
  }

  protected removeStep(index: number): void {
    this.steps.removeAt(index);
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { name: 'Name', documentType: 'Document type' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  private loadWorkflow(): void {
    this.loading.set(true);
    this.workflowService.getById(this.workflowId!).subscribe({
      next: (w) => {
        this.form.patchValue({
          name: w.name,
          documentType: w.documentType,
          locationId: w.locationId,
          minAmount: w.minAmount,
        });
        this.steps.clear();
        for (const step of w.steps) {
          this.steps.push(this.fb.group({
            stepOrder:      [step.stepOrder, [Validators.required, Validators.min(1)]],
            stepName:       [step.stepName, [Validators.required, Validators.maxLength(150)]],
            permissionCode: [step.permissionCode, [Validators.required]],
          }));
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load approval workflow'); this.loading.set(false); },
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid || this.steps.length === 0) {
      scrollToFirstInvalid(this.form);
      if (this.steps.length === 0) this.toast.error('Add at least one step');
      return;
    }

    const v = this.form.value;
    const steps: ApprovalWorkflowStepRequest[] = (v.steps ?? []).map((s: ApprovalWorkflowStepRequest) => ({
      stepOrder: s.stepOrder,
      stepName: s.stepName.trim(),
      permissionCode: s.permissionCode.trim(),
    }));

    const request: ApprovalWorkflowRequest = {
      name: v.name.trim(),
      documentType: v.documentType,
      locationId: v.locationId ?? undefined,
      minAmount: v.documentType === 'PURCHASE_ORDER' ? (v.minAmount ?? undefined) : undefined,
      steps,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.workflowService.update(this.workflowId!, request)
      : this.workflowService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Workflow updated' : 'Workflow created');
        this.saving.set(false);
        void this.router.navigate(['/inventory/approval/workflows']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save workflow');
        this.saving.set(false);
      },
    });
  }
}
