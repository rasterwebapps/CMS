import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { SettingsService } from '../settings.service';
import { SystemConfigurationRequest } from '../settings.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-system-configuration-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    MatSlideToggleModule],
  templateUrl: './system-configuration-form.component.html',
  styleUrl: './system-configuration-form.component.scss',
})
export class SystemConfigurationFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly settingsService = inject(SettingsService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Configuration');

  protected readonly dataTypes = ['STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN'];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    configKey: ['', [Validators.required, Validators.maxLength(255)]],
    configValue: ['', [Validators.required, Validators.maxLength(1000)]],
    description: [''],
    dataType: ['STRING', Validators.required],
    category: ['', [Validators.required, Validators.maxLength(255)]],
    isEditable: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Configuration');
      this.loading.set(true);
      this.settingsService.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({ configKey: item.configKey, configValue: item.configValue, description: item.description, dataType: item.dataType, category: item.category, isEditable: item.isEditable });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/settings']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: SystemConfigurationRequest = { configKey: v.configKey.trim(), configValue: v.configValue.trim(), description: v.description?.trim() || undefined, dataType: v.dataType, category: v.category.trim(), isEditable: v.isEditable };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.settingsService.update(this.itemId!, request) : this.settingsService.create(request);
    op$.subscribe({
      next: () => { this.toast.success(this.isEditMode() ? 'Updated' : 'Created'); void this.router.navigate(['/settings']); },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
}
