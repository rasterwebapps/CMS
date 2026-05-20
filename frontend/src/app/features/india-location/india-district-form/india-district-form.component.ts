import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { IndiaLocationService } from '../india-location.service';
import { IndiaState, IndiaDistrictRequest } from '../india-location.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../shared/validators/cms-validators';

@Component({
  selector: 'app-india-district-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatSlideToggleModule],
  templateUrl: './india-district-form.component.html',
  styleUrl: './india-district-form.component.scss',
})
export class IndiaDistrictFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(IndiaLocationService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add District');
  protected readonly states = signal<IndiaState[]>([]);

  private itemId: number | null = null;
  private preselectedStateId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    stateId: [null, Validators.required],
    name: [
      '',
      [Validators.required, trimmedMinLength(1), Validators.maxLength(100), noConsecutiveSpaces()],
    ],
    isActive: [true],
  });

  ngOnInit(): void {
    // Load all states for the dropdown
    this.service.getStates(false).subscribe({
      next: (s) => this.states.set(s),
      error: () => this.toast.error('Failed to load states'),
    });

    // Check if coming from /states/:stateId/districts/new
    const stateIdParam = this.route.snapshot.paramMap.get('stateId');
    if (stateIdParam) {
      this.preselectedStateId = Number(stateIdParam);
      this.form.patchValue({ stateId: this.preselectedStateId });
    }

    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit District');
      this.loadItem(this.itemId);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    this.saving.set(true);
    const request: IndiaDistrictRequest = {
      stateId: this.form.value.stateId,
      name: this.form.value.name,
      isActive: this.form.value.isActive,
    };
    const stateId = Number(request.stateId);
    const op = this.isEditMode()
      ? this.service.updateDistrict(this.itemId!, request)
      : this.service.createDistrict(stateId, request);
    op.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'District updated' : 'District created');
        void this.router.navigate(['/india-locations']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save district');
        this.saving.set(false);
      },
    });
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.service.getDistrictById(id).subscribe({
      next: (item) => {
        this.form.patchValue({ stateId: item.stateId, name: item.name, isActive: item.isActive });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load district');
        this.loading.set(false);
      },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = { name: 'Name', stateId: 'State' };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }
}

