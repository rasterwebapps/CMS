import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { LibraryService } from '../library.service';
import { LibrarySetting } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-library-settings',
  standalone: true,
  imports: [ReactiveFormsModule, MatIconModule],
  templateUrl: './library-settings.component.html',
  styleUrl:    './library-settings.component.scss',
})
export class LibrarySettingsComponent implements OnInit {
  private readonly libraryService = inject(LibraryService);
  private readonly fb             = inject(FormBuilder);
  private readonly toast          = inject(ToastService);
  private readonly destroyRef     = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving  = signal(false);
  protected form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      student_loan_days:  [14, [Validators.required, Validators.min(1), Validators.max(365)]],
      faculty_loan_days:  [30, [Validators.required, Validators.min(1), Validators.max(365)]],
      student_max_books:  [2,  [Validators.required, Validators.min(1), Validators.max(20)]],
      faculty_max_books:  [3,  [Validators.required, Validators.min(1), Validators.max(20)]],
      fine_per_day:       [1,  [Validators.required, Validators.min(0)]],
      max_renewals:       [2,  [Validators.required, Validators.min(0), Validators.max(10)]],
    });
    this.loadSettings();
  }

  private loadSettings(): void {
    this.loading.set(true);
    this.libraryService.getSettings().subscribe({
      next: settings => {
        const patch: Record<string, number> = {};
        for (const s of settings) {
          if (this.form.contains(s.settingKey)) {
            patch[s.settingKey] = parseFloat(s.settingValue);
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
          error: () => {
            if (!failed) {
              failed = true;
              this.toast.error('Failed to save one or more settings');
              this.saving.set(false);
            }
          },
        });
    }
  }

  protected get f() { return this.form.controls; }
}
