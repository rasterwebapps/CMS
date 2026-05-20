import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { BloodGroupService } from '../blood-group.service';
import { BloodGroupRequest } from '../blood-group.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';

@Component({
  selector: 'app-blood-group-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatSlideToggleModule],
  templateUrl: './blood-group-form.component.html',
  styleUrl: './blood-group-form.component.scss',
})
export class BloodGroupFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly bloodGroupService = inject(BloodGroupService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Blood Group');

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, trimmedMinLength(1), Validators.maxLength(100), noConsecutiveSpaces()]],
    code: ['', [Validators.required, Validators.maxLength(20), noInternalSpaces()]],
    isActive: [true],
  });

  constructor() {
    // auto-uppercase and strip spaces from code field (BR-29 CODE rule)
    this.form.get('code')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string) => {
        const cleaned = stripSpaces(v ?? '').toUpperCase();
        if (cleaned !== v) {
          this.form.get('code')!.setValue(cleaned, { emitEvent: false });
        }
      });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Blood Group');
      this.loadItem(this.itemId);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
    this.saving.set(true);
    const request = this.buildRequest();
    const op = this.isEditMode()
      ? this.bloodGroupService.updateBloodGroup(this.itemId!, request)
      : this.bloodGroupService.createBloodGroup(request);
    op.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Blood group updated' : 'Blood group created');
        void this.router.navigate(['/blood-groups']);
      },
      error: (err) => {
        const msg = err?.error?.message ?? (this.isEditMode() ? 'Failed to update blood group' : 'Failed to create blood group');
        this.toast.error(msg);
        this.saving.set(false);
      },
    });
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.bloodGroupService.getBloodGroupById(id).subscribe({
      next: (item) => {
        this.form.patchValue({ name: item.name, code: item.code, isActive: item.isActive });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load blood group');
        this.loading.set(false);
      },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = { name: 'Name', code: 'Code' };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }

  private buildRequest(): BloodGroupRequest {
    const v = this.form.value as BloodGroupRequest & { isActive: boolean };
    return { name: v.name, code: v.code, isActive: v.isActive };
  }
}

