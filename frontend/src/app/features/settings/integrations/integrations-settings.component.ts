import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { SettingsService } from '../settings.service';
import { SystemConfiguration, SystemConfigurationRequest } from '../settings.model';
import { ToastService } from '../../../core/toast/toast.service';

interface IntegrationField {
  key: string;
  label: string;
  description: string;
  value: string;
  dataType: 'STRING' | 'BOOLEAN';
  id: number | null;
  isSecret?: boolean;
  placeholder?: string;
}

@Component({
  selector: 'app-integrations-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './integrations-settings.component.html',
  styleUrl: './integrations-settings.component.scss',
})
export class IntegrationsSettingsComponent implements OnInit {
  private readonly settingsService = inject(SettingsService);
  private readonly toast           = inject(ToastService);

  readonly loading = signal(true);
  readonly saving  = signal(false);
  protected readonly isEnabled          = signal(false);
  protected readonly showPassword       = signal(false);
  protected readonly showWebhookSecret  = signal(false);

  protected readonly fields: IntegrationField[] = [
    {
      key: 'onebook.enabled',
      label: 'Enable OneBook Integration',
      description: 'When enabled, all online payments (commissions, refunds, scholarships) are routed to OneBook for processing. When disabled, all payments are managed entirely within OneCMS.',
      value: 'false',
      dataType: 'BOOLEAN',
      id: null,
    },
    {
      key: 'onebook.allow_cash_in_cms',
      label: 'Allow Cash Payments in OneCMS',
      description: 'When OneBook is enabled, cash payments can still be recorded directly in OneCMS. Online payments (UPI, bank transfer, etc.) always go through OneBook.',
      value: 'true',
      dataType: 'BOOLEAN',
      id: null,
    },
    {
      key: 'onebook.api_url',
      label: 'OneBook Server URL',
      description: 'Base URL of the OneBook server. OneCMS appends the payment register API path to this.',
      value: '',
      dataType: 'STRING',
      id: null,
      placeholder: 'https://172.16.x.x:8080',
    },
    {
      key: 'onebook.username',
      label: 'Username',
      description: 'OneBook login username for authenticating outbound requests.',
      value: '',
      dataType: 'STRING',
      id: null,
      placeholder: 'e.g. raster',
    },
    {
      key: 'onebook.password',
      label: 'Password',
      description: 'OneBook login password. Stored securely — keep this confidential.',
      value: '',
      dataType: 'STRING',
      id: null,
      isSecret: true,
      placeholder: '••••••••••••••••',
    },
    {
      key: 'onebook.org_id',
      label: 'Organisation ID',
      description: 'Organisation identifier assigned to this institution in OneBook.',
      value: '',
      dataType: 'STRING',
      id: null,
      placeholder: 'e.g. skscon',
    },
    {
      key: 'onebook.branch_id',
      label: 'Branch ID',
      description: 'Branch within OneBook that represents this college.',
      value: '',
      dataType: 'STRING',
      id: null,
      placeholder: 'e.g. 1',
    },
    {
      key: 'onebook.app_name',
      label: 'Application Name in OneBook',
      description: 'The application name registered in OneBook for this integration.',
      value: 'ONECMS',
      dataType: 'STRING',
      id: null,
      placeholder: 'ONECMS',
    },
    {
      key: 'onebook.paper_name',
      label: 'Paper Name in OneBook',
      description: 'Paper / entity name used in OneBook to identify the source of payments.',
      value: 'SKS College Of Nursing',
      dataType: 'STRING',
      id: null,
      placeholder: 'SKS College Of Nursing',
    },
    {
      key: 'onebook.zone_name',
      label: 'Zone Name',
      description: 'Timezone identifier sent on every OneBook authentication request.',
      value: 'Asia/Calcutta',
      dataType: 'STRING',
      id: null,
      placeholder: 'Asia/Calcutta',
    },
    {
      key: 'onebook.integration_date',
      label: 'Date of Integration',
      description: 'Date OneCMS was integrated with OneBook. Used for audit and tracking.',
      value: '',
      dataType: 'STRING',
      id: null,
      placeholder: 'YYYY-MM-DD',
    },
    {
      key: 'onebook.webhook_secret',
      label: 'Webhook Secret',
      description: 'OneBook sends this value in the X-OneBook-Secret header on every status callback. OneCMS verifies it to reject forged requests.',
      value: '',
      dataType: 'STRING',
      id: null,
      isSecret: true,
      placeholder: 'Shared secret from OneBook',
    },
  ];

  ngOnInit(): void {
    this.settingsService.getByCategory('INTEGRATION').subscribe({
      next: (configs) => {
        const byKey = new Map<string, SystemConfiguration>(configs.map(c => [c.configKey, c]));
        for (const field of this.fields) {
          const cfg = byKey.get(field.key);
          if (cfg) { field.value = cfg.configValue; field.id = cfg.id; }
        }
        this.isEnabled.set(this.getBooleanField('onebook.enabled'));
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load integration settings'); this.loading.set(false); },
    });
  }

  protected getBooleanField(key: string): boolean {
    return this.fields.find(f => f.key === key)?.value === 'true';
  }

  protected setBooleanField(key: string, value: boolean): void {
    const field = this.fields.find(f => f.key === key);
    if (field) {
      field.value = value ? 'true' : 'false';
      if (key === 'onebook.enabled') this.isEnabled.set(value);
    }
  }

  protected fieldValue(key: string): string {
    return this.fields.find(f => f.key === key)?.value ?? '';
  }

  protected setFieldValue(key: string, value: string): void {
    const field = this.fields.find(f => f.key === key);
    if (field) field.value = value;
  }

  save(): void {
    if (this.isEnabled()) {
      const url      = this.fieldValue('onebook.api_url').trim();
      const username = this.fieldValue('onebook.username').trim();
      const password = this.fieldValue('onebook.password').trim();
      const orgId    = this.fieldValue('onebook.org_id').trim();
      if (!url || !username || !password || !orgId) {
        this.toast.error('Server URL, Username, Password, and Organisation ID are required when OneBook is enabled.');
        return;
      }
    }

    this.saving.set(true);
    const ops = this.fields.map(f => this.settingsService.upsert(this.buildRequest(f)));

    forkJoin(ops).subscribe({
      next: (results) => {
        for (const cfg of results) {
          const field = this.fields.find(f => f.key === cfg.configKey);
          if (field) field.id = cfg.id;
        }
        this.toast.success('Integration settings saved');
        this.saving.set(false);
      },
      error: () => { this.toast.error('Failed to save integration settings'); this.saving.set(false); },
    });
  }

  private buildRequest(field: IntegrationField): SystemConfigurationRequest {
    return {
      configKey: field.key,
      configValue: field.value,
      dataType: field.dataType,
      category: 'INTEGRATION',
      isEditable: true,
    };
  }
}
