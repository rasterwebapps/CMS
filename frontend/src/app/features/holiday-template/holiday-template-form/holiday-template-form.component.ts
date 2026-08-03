import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HolidayTemplateService } from '../holiday-template.service';
import { HolidayTemplateRequest, HolidayRecurrenceType, WeekOfMonth } from '../holiday-template.model';
import { AppDayOfWeek, HolidayCategory } from '../../academic-year/academic-year.model';
import { formatRecurrenceSummary } from '../holiday-template-summary.util';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-holiday-template-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './holiday-template-form.component.html',
  styleUrl: './holiday-template-form.component.scss',
})
export class HolidayTemplateFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly holidayTemplateService = inject(HolidayTemplateService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);

  protected readonly recurrenceTypes: HolidayRecurrenceType[] = ['YEARLY', 'MONTHLY'];
  protected readonly holidayCategories: HolidayCategory[] = ['GOVERNMENT', 'LOCAL', 'INSTITUTIONAL'];
  protected readonly weeksOfMonth: WeekOfMonth[] = ['FIRST', 'SECOND', 'THIRD', 'FOURTH', 'LAST'];
  protected readonly daysOfWeek: AppDayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  protected readonly months = Array.from({ length: 12 }, (_, i) => ({
    value: i + 1,
    label: new Intl.DateTimeFormat('en', { month: 'long' }).format(new Date(2000, i, 1)),
  }));

  protected readonly previewSummary = signal('');
  protected readonly previewName = signal('');

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(255), noConsecutiveSpaces()]],
    recurrenceType: ['YEARLY' as HolidayRecurrenceType, Validators.required],
    holidayCategory: [null as HolidayCategory | null],
    description: [''],
    durationDays: [1, [Validators.required, Validators.min(1)]],
    month: [null as number | null],
    dayOfMonth: [null as number | null],
    weekOfMonth: [null as WeekOfMonth | null],
    dayOfWeek: [null as AppDayOfWeek | null],
  });

  protected isYearly(): boolean {
    return this.form.get('recurrenceType')?.value === 'YEARLY';
  }

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      this.previewName.set((v.name ?? '').trim());
      this.previewSummary.set(
        v.recurrenceType
          ? formatRecurrenceSummary({
              recurrenceType: v.recurrenceType,
              month: v.month ?? null,
              dayOfMonth: v.dayOfMonth ?? null,
              weekOfMonth: v.weekOfMonth ?? null,
              dayOfWeek: v.dayOfWeek ?? null,
              durationDays: v.durationDays || 1,
            })
          : '',
      );
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.loadItem(this.itemId);
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/holiday-templates/name-exists`, () => this.itemId),
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    this.saving.set(true);
    const request = this.buildRequest();
    const op = this.isEditMode()
      ? this.holidayTemplateService.update(this.itemId!, request)
      : this.holidayTemplateService.create(request);
    op.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Holiday template updated' : 'Holiday template created');
        void this.router.navigate(['/holiday-templates']);
      },
      error: (err) => {
        const msg =
          err?.error?.message ??
          (this.isEditMode() ? 'Failed to update holiday template' : 'Failed to create holiday template');
        this.toast.error(msg);
        this.saving.set(false);
      },
    });
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.holidayTemplateService.getById(id).subscribe({
      next: (item) => {
        this.form.patchValue({
          name: item.name,
          recurrenceType: item.recurrenceType,
          holidayCategory: item.holidayCategory,
          description: item.description ?? '',
          durationDays: item.durationDays,
          month: item.month,
          dayOfMonth: item.dayOfMonth,
          weekOfMonth: item.weekOfMonth,
          dayOfWeek: item.dayOfWeek,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load holiday template');
        void this.router.navigate(['/holiday-templates']);
      },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = {
      name: 'Name', recurrenceType: 'Recurrence type', durationDays: 'Duration',
      month: 'Month', dayOfMonth: 'Day of month', weekOfMonth: 'Week of month', dayOfWeek: 'Day of week',
    };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }

  private buildRequest(): HolidayTemplateRequest {
    const v = this.form.value;
    const yearly = v.recurrenceType === 'YEARLY';
    return {
      name: (v.name ?? '').trim(),
      recurrenceType: v.recurrenceType,
      holidayCategory: v.holidayCategory ?? null,
      description: v.description?.trim() || undefined,
      durationDays: v.durationDays ?? 1,
      month: yearly ? v.month : null,
      dayOfMonth: yearly ? v.dayOfMonth : null,
      weekOfMonth: yearly ? null : v.weekOfMonth,
      dayOfWeek: yearly ? null : v.dayOfWeek,
    };
  }
}
