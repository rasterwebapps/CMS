import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MaintenanceScheduleService } from '../maintenance-schedule.service';
import { AssetMaintenanceScheduleRequest, MaintenanceScheduleType } from '../maintenance-schedule.model';
import { AssetService } from '../../asset.service';
import { Asset } from '../../asset.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-maintenance-schedule-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './maintenance-schedule-form.component.html',
  styleUrl: './maintenance-schedule-form.component.scss',
})
export class MaintenanceScheduleFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly scheduleService = inject(MaintenanceScheduleService);
  private readonly assetService    = inject(AssetService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly assets     = signal<Asset[]>([]);

  private scheduleId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    assetId:                [null as number | null, [Validators.required]],
    scheduleType:           ['ONE_OFF' as MaintenanceScheduleType, [Validators.required]],
    recurrenceIntervalDays: [null as number | null],
    nextDueDate:            ['', [Validators.required]],
    notes:                  ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.assetService.getPage({ page: 0, size: 200 }).subscribe({ next: (page) => this.assets.set(page.content) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.scheduleId = Number(idParam);
      this.isEditMode.set(true);
      this.loadSchedule();
    }

    this.form.get('scheduleType')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((type) => {
      const intervalCtrl = this.form.get('recurrenceIntervalDays');
      if (type === 'RECURRING') {
        intervalCtrl?.setValidators([Validators.required, Validators.min(1)]);
      } else {
        intervalCtrl?.clearValidators();
        intervalCtrl?.setValue(null);
      }
      intervalCtrl?.updateValueAndValidity();
    });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { assetId: 'Asset', nextDueDate: 'Next due date', recurrenceIntervalDays: 'Recurrence interval' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  private loadSchedule(): void {
    this.loading.set(true);
    this.scheduleService.getById(this.scheduleId!).subscribe({
      next: (s) => {
        this.form.patchValue({
          assetId: s.assetId,
          scheduleType: s.scheduleType,
          recurrenceIntervalDays: s.recurrenceIntervalDays,
          nextDueDate: s.nextDueDate,
          notes: s.notes ?? '',
        });
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load maintenance schedule'); this.loading.set(false); },
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: AssetMaintenanceScheduleRequest = {
      assetId: v.assetId,
      scheduleType: v.scheduleType,
      recurrenceIntervalDays: v.scheduleType === 'RECURRING' ? v.recurrenceIntervalDays : undefined,
      nextDueDate: v.nextDueDate,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.scheduleService.update(this.scheduleId!, request)
      : this.scheduleService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Schedule updated' : 'Maintenance schedule created');
        this.saving.set(false);
        void this.router.navigate(['/inventory/asset/maintenance-schedules']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save maintenance schedule');
        this.saving.set(false);
      },
    });
  }
}
