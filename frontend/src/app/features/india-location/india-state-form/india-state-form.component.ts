import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { IndiaLocationService } from '../india-location.service';
import { IndiaStateRequest } from '../india-location.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import {
  noConsecutiveSpaces,
  noInternalSpaces,
  trimmedMinLength,
  cmsFieldError,
  stripSpaces,
} from '../../../shared/validators/cms-validators';

@Component({
  selector: 'app-india-state-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatSlideToggleModule],
  templateUrl: './india-state-form.component.html',
  styleUrl: './india-state-form.component.scss',
})
export class IndiaStateFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(IndiaLocationService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add State / UT');

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: [
      '',
      [Validators.required, trimmedMinLength(1), Validators.maxLength(100), noConsecutiveSpaces()],
    ],
    code: ['', [Validators.required, Validators.maxLength(10), noInternalSpaces()]],
    isActive: [true],
  });

  constructor() {
    this.form
      .get('code')!
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string) => {
        const cleaned = stripSpaces(v ?? '').toUpperCase();
        if (cleaned !== v) this.form.get('code')!.setValue(cleaned, { emitEvent: false });
      });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit State / UT');
      this.loadItem(this.itemId);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    this.saving.set(true);
    const request: IndiaStateRequest = {
      name: this.form.value.name,
      code: this.form.value.code,
      isActive: this.form.value.isActive,
    };
    const op = this.isEditMode()
      ? this.service.updateState(this.itemId!, request)
      : this.service.createState(request);
    op.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'State updated' : 'State created');
        void this.router.navigate(['/india-locations']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save state');
        this.saving.set(false);
      },
    });
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.service.getStateById(id).subscribe({
      next: (item) => {
        this.form.patchValue({ name: item.name, code: item.code, isActive: item.isActive });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load state');
        this.loading.set(false);
      },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = { name: 'Name', code: 'Code' };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }
}

