import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { LibraryService } from '../library.service';
import { LibrarySetting } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { LIBRARY_SETTINGS_TOUR } from '../../../shared/tour/tours/library.tours';

@Component({
  selector: 'app-library-settings',
  standalone: true,
  imports: [ReactiveFormsModule, MatIconModule, CmsTourButtonComponent],
  templateUrl: './library-settings.component.html',
  styleUrl:    './library-settings.component.scss',
})
export class LibrarySettingsComponent implements OnInit {
  private readonly libraryService = inject(LibraryService);
  private readonly fb             = inject(FormBuilder);
  private readonly toast          = inject(ToastService);
  private readonly destroyRef     = inject(DestroyRef);
  private readonly tourService    = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly saving  = signal(false);
  protected form!: FormGroup;

  ngOnInit(): void {
    this.tourService.register('library-settings', LIBRARY_SETTINGS_TOUR);
    this.form = this.fb.group({
      student_loan_days:  [14, [Validators.required, Validators.min(1), Validators.max(365)]],
      faculty_loan_days:  [30, [Validators.required, Validators.min(1), Validators.max(365)]],
      student_max_books:  [2,  [Validators.required, Validators.min(1), Validators.max(20)]],
      faculty_max_books:  [3,  [Validators.required, Validators.min(1), Validators.max(20)]],
      fine_per_day:       [1,  [Validators.required, Validators.min(0)]],
      max_renewals:       [2,  [Validators.required, Validators.min(0), Validators.max(10)]],
      barcode_label_width_mm:  [50, [Validators.required, Validators.min(10), Validators.max(200)]],
      barcode_label_height_mm: [25, [Validators.required, Validators.min(10), Validators.max(200)]],
      barcode_printer_mode:    ['BROWSER', Validators.required],
      barcode_printer_ip:      [''],
      barcode_printer_port:    [9100, [Validators.required, Validators.min(1), Validators.max(65535)]],
      barcode_labels_per_row:  [1, Validators.required],
    });

    this.form.get('barcode_printer_mode')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(mode => this.updatePrinterIpValidators(mode));

    this.loadSettings();
  }

  private updatePrinterIpValidators(mode: string): void {
    const ipControl = this.form.get('barcode_printer_ip')!;
    ipControl.setValidators(mode === 'NETWORK' ? [Validators.required] : []);
    ipControl.updateValueAndValidity({ emitEvent: false });
  }

  private loadSettings(): void {
    this.loading.set(true);
    this.libraryService.getSettings().subscribe({
      next: settings => {
        const patch: Record<string, number | string> = {};
        for (const s of settings) {
          if (this.form.contains(s.settingKey)) {
            patch[s.settingKey] = s.dataType === 'STRING' ? s.settingValue : parseFloat(s.settingValue);
          }
        }
        this.form.patchValue(patch);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load library settings');
        this.loading.set(false);
      },
    });
  }

  protected save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);

    const v = this.form.value as Record<string, number>;
    const keys = Object.keys(v);
    let completed = 0;
    let failed = false;

    for (const key of keys) {
      this.libraryService.updateSetting(key, { settingValue: String(v[key]) })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            completed++;
            if (completed === keys.length && !failed) {
              this.toast.success('Library settings saved');
              this.saving.set(false);
            }
          },
          error: (err) => {
            if (!failed) {
              failed = true;
              this.toast.error(err?.error?.message ?? 'Failed to save one or more settings');
              this.saving.set(false);
            }
          },
        });
    }
  }

  protected get f() { return this.form.controls; }
}
