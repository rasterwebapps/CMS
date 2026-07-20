import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PeriodService } from '../period.service';
import { PeriodRequest } from '../period.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-period-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './period-form.component.html',
  styleUrl: './period-form.component.scss',
})
export class PeriodFormComponent implements OnInit {
  private readonly fb            = inject(FormBuilder);
  private readonly route         = inject(ActivatedRoute);
  private readonly router        = inject(Router);
  private readonly periodService = inject(PeriodService);
  private readonly toast         = inject(ToastService);
  private readonly destroyRef    = inject(DestroyRef);
  private readonly http          = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Period');

  protected readonly previewName = signal('');
  protected readonly previewStart = signal('');
  protected readonly previewEnd = signal('');

  private periodId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(255), noConsecutiveSpaces()]],
    startTime:   ['', [Validators.required]],
    endTime:     ['', [Validators.required]],
    periodOrder: [null],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewStart.set(v.startTime ?? '');
        this.previewEnd.set(v.endTime ?? '');
      });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.periodId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Period');
      this.loadPeriod();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/periods/name-exists`, () => this.periodId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    if (this.form.value.endTime <= this.form.value.startTime) {
      this.toast.error('End time must be after start time');
      return;
    }

    const request: PeriodRequest = {
      name:        (this.form.value.name ?? '').trim(),
      startTime:   `${this.form.value.startTime}:00`,
      endTime:     `${this.form.value.endTime}:00`,
      periodOrder: this.form.value.periodOrder ?? undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.periodService.update(this.periodId!, request)
      : this.periodService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Period updated successfully' : 'Period created successfully');
        this.saving.set(false);
        void this.router.navigate(['/periods']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update period' : 'Failed to create period'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', startTime: 'Start Time', endTime: 'End Time', periodOrder: 'Order',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), PeriodFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadPeriod(): void {
    if (!this.periodId) return;
    this.loading.set(true);
    this.periodService.getById(this.periodId).subscribe({
      next: (p) => {
        this.form.patchValue({
          name: p.name,
          startTime: p.startTime?.slice(0, 5) ?? '',
          endTime: p.endTime?.slice(0, 5) ?? '',
          periodOrder: p.periodOrder ?? null,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load period');
        void this.router.navigate(['/periods']);
      },
    });
  }
}
