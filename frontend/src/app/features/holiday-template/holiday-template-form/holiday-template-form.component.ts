import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HolidayTemplateService } from '../holiday-template.service';
import { HolidayTemplateRequest, HolidayRecurrenceType, WeekOfMonth } from '../holiday-template.model';
import { AppDayOfWeek, CalendarEventType, HolidayCategory } from '../../academic-year/academic-year.model';
import { formatRecurrenceSummary } from '../holiday-template-summary.util';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

type MonthlyPattern = 'DAY_OF_MONTH' | 'NTH_WEEKDAY';

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

  protected readonly recurrenceTypes: HolidayRecurrenceType[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'];
  protected readonly eventTypes: CalendarEventType[] = ['HOLIDAY', 'EXAM', 'CULTURAL', 'SPORTS', 'WORKSHOP', 'OTHER'];
  protected readonly holidayCategories: HolidayCategory[] = ['GOVERNMENT', 'LOCAL', 'INSTITUTIONAL'];
  protected readonly weeksOfMonth: WeekOfMonth[] = ['FIRST', 'SECOND', 'THIRD', 'FOURTH', 'LAST'];
  protected readonly daysOfWeek: AppDayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  protected readonly months = Array.from({ length: 12 }, (_, i) => ({
    value: i + 1,
    label: new Intl.DateTimeFormat('en', { month: 'long' }).format(new Date(2000, i, 1)),
  }));

  protected readonly previewSummary = signal('');
  protected readonly previewName = signal('');
  /** Only meaningful when recurrenceType === MONTHLY -- which of dayOfMonth vs
   *  weekOfMonth+dayOfWeek the form is currently collecting. Not part of the saved shape itself;
   *  derived back from whichever field is set when editing an existing template. */
  protected readonly monthlyPattern = signal<MonthlyPattern>('DAY_OF_MONTH');

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(255), noConsecutiveSpaces()]],
    recurrenceType: ['YEARLY' as HolidayRecurrenceType, Validators.required],
    eventType: ['HOLIDAY' as CalendarEventType, Validators.required],
    holidayCategory: [null as HolidayCategory | null],
    description: [''],
    durationDays: [1, [Validators.required, Validators.min(1)]],
    intervalCount: [1, [Validators.required, Validators.min(1)]],
    anchorDate: [null as string | null],
    endDate: [null as string | null],
    month: [null as number | null],
    dayOfMonth: [null as number | null],
    weekOfMonth: [null as WeekOfMonth | null],
    dayOfWeek: [null as AppDayOfWeek | null],
  });

  protected isYearly(): boolean {
    return this.form.get('recurrenceType')?.value === 'YEARLY';
  }

  protected isMonthly(): boolean {
    return this.form.get('recurrenceType')?.value === 'MONTHLY';
  }

  protected isWeekly(): boolean {
    return this.form.get('recurrenceType')?.value === 'WEEKLY';
  }

  protected showHolidayCategoryField(): boolean {
    return this.form.get('eventType')?.value === 'HOLIDAY';
  }

  /** DAILY always needs a start date to count intervals from; any other frequency only needs one
   *  once the interval is more than 1 (at interval 1, the pattern fields alone already determine
   *  every occurrence). */
  protected needsAnchorDate(): boolean {
    const intervalCount = this.form.get('intervalCount')?.value ?? 1;
    return this.form.get('recurrenceType')?.value === 'DAILY' || intervalCount > 1;
  }

  protected setMonthlyPattern(pattern: MonthlyPattern): void {
    this.monthlyPattern.set(pattern);
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
              intervalCount: v.intervalCount || 1,
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
    if (this.needsAnchorDate() && !this.form.get('anchorDate')?.value) {
      this.toast.error('A start date is required for a daily repeat, or when repeating every N > 1 units');
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
        this.monthlyPattern.set(item.dayOfMonth != null ? 'DAY_OF_MONTH' : 'NTH_WEEKDAY');
        this.form.patchValue({
          name: item.name,
          recurrenceType: item.recurrenceType,
          eventType: item.eventType,
          holidayCategory: item.holidayCategory,
          description: item.description ?? '',
          durationDays: item.durationDays,
          intervalCount: item.intervalCount,
          anchorDate: item.anchorDate,
          endDate: item.endDate,
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
      name: 'Name', recurrenceType: 'Recurrence type', durationDays: 'Duration', intervalCount: 'Interval',
      month: 'Month', dayOfMonth: 'Day of month', weekOfMonth: 'Week of month', dayOfWeek: 'Day of week',
    };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }

  private buildRequest(): HolidayTemplateRequest {
    const v = this.form.value;
    const yearly = v.recurrenceType === 'YEARLY';
    const monthly = v.recurrenceType === 'MONTHLY';
    const weekly = v.recurrenceType === 'WEEKLY';
    const monthlyFixedDay = monthly && this.monthlyPattern() === 'DAY_OF_MONTH';
    const monthlyNthWeekday = monthly && this.monthlyPattern() === 'NTH_WEEKDAY';

    return {
      name: (v.name ?? '').trim(),
      recurrenceType: v.recurrenceType,
      eventType: v.eventType,
      holidayCategory: v.eventType === 'HOLIDAY' ? (v.holidayCategory ?? null) : null,
      description: v.description?.trim() || undefined,
      durationDays: v.durationDays ?? 1,
      intervalCount: v.intervalCount ?? 1,
      anchorDate: this.needsAnchorDate() ? v.anchorDate : null,
      endDate: v.endDate || null,
      month: yearly ? v.month : null,
      dayOfMonth: yearly || monthlyFixedDay ? v.dayOfMonth : null,
      weekOfMonth: monthlyNthWeekday ? v.weekOfMonth : null,
      dayOfWeek: weekly || monthlyNthWeekday ? v.dayOfWeek : null,
    };
  }
}
