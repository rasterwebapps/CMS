import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { SettingsService } from '../settings.service';
import { SystemConfiguration, SystemConfigurationRequest } from '../settings.model';
import { ToastService } from '../../../core/toast/toast.service';

interface BrandingField {
  key: string;
  label: string;
  value: string;
  id: number | null;
}

@Component({
  selector: 'app-branding',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './branding.component.html',
  styleUrl: './branding.component.scss',
})
export class BrandingComponent implements OnInit {
  private readonly settingsService = inject(SettingsService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly logoPreview = signal<string | null>(null);
  protected readonly logoId = signal<number | null>(null);

  protected readonly fields: BrandingField[] = [
    { key: 'college.name',                label: 'College Name',        value: '', id: null },
    { key: 'college.trust_name',          label: 'Trust Name',          value: '', id: null },
    { key: 'college.registration_number', label: 'Registration Number', value: '', id: null },
    { key: 'college.address',             label: 'Address',             value: '', id: null },
    { key: 'college.phone',               label: 'Phone',               value: '', id: null },
    { key: 'college.email',               label: 'Email',               value: '', id: null },
  ];

  ngOnInit(): void {
    this.settingsService.getAll().subscribe({
      next: (configs) => {
        const byKey = new Map<string, SystemConfiguration>(configs.map(c => [c.configKey, c]));
        for (const field of this.fields) {
          const cfg = byKey.get(field.key);
          if (cfg) { field.value = cfg.configValue; field.id = cfg.id; }
        }
        const logoCfg = byKey.get('college.logo_data');
        if (logoCfg) {
          this.logoId.set(logoCfg.id);
          if (logoCfg.configValue) this.logoPreview.set(logoCfg.configValue);
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load branding settings'); this.loading.set(false); },
    });
  }

  protected onLogoSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) { this.toast.error('Please select an image file'); return; }
    const reader = new FileReader();
    reader.onload = () => this.logoPreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  protected clearLogo(): void {
    this.logoPreview.set(null);
    const el = document.getElementById('logo-input') as HTMLInputElement | null;
    if (el) el.value = '';
  }

  protected save(): void {
    this.saving.set(true);
    const ops = this.fields.map(f =>
      f.id !== null
        ? this.settingsService.update(f.id, this.buildRequest(f.key, f.value, 'BRANDING'))
        : this.settingsService.create(this.buildRequest(f.key, f.value, 'BRANDING'))
    );

    const logoId = this.logoId();
    if (logoId !== null) {
      ops.push(this.settingsService.update(logoId,
        this.buildRequest('college.logo_data', this.logoPreview() ?? '', 'BRANDING')));
    } else if (this.logoPreview()) {
      ops.push(this.settingsService.create(
        this.buildRequest('college.logo_data', this.logoPreview()!, 'BRANDING')));
    }

    if (ops.length === 0) { this.saving.set(false); return; }

    forkJoin(ops).subscribe({
      next: (results) => {
        // Update local ids so subsequent saves use update, not create
        const created = results.filter(r => r.id);
        for (const cfg of created) {
          const field = this.fields.find(f => f.key === cfg.configKey);
          if (field && field.id === null) field.id = cfg.id;
          if (cfg.configKey === 'college.logo_data' && !this.logoId()) this.logoId.set(cfg.id);
        }
        this.toast.success('Branding settings saved');
        this.saving.set(false);
      },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }

  private buildRequest(key: string, value: string, category: string): SystemConfigurationRequest {
    return { configKey: key, configValue: value, dataType: 'STRING', category, isEditable: true };
  }
}
