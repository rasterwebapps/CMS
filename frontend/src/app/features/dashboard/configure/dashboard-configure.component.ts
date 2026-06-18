import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule }             from '@angular/material/icon';
import { MatProgressSpinnerModule }  from '@angular/material/progress-spinner';
import { MatSnackBar }               from '@angular/material/snack-bar';

import { DashboardConfigService }             from '../services/dashboard-config.service';
import { PermissionService, WidgetConfigDto } from '../../../core/permissions/permission.service';
import { WidgetPickerComponent }              from '../../../shared/widget-picker/widget-picker.component';

@Component({
  selector:    'app-dashboard-configure',
  standalone:  true,
  imports: [MatIconModule, MatProgressSpinnerModule, WidgetPickerComponent],
  templateUrl: './dashboard-configure.component.html',
  styleUrl:    './dashboard-configure.component.scss',
})
export class DashboardConfigureComponent implements OnInit {
  private readonly router        = inject(Router);
  private readonly configService = inject(DashboardConfigService);
  private readonly permService   = inject(PermissionService);
  private readonly snackBar      = inject(MatSnackBar);

  protected readonly loading        = signal(true);
  protected readonly saving         = signal(false);
  protected          initialWidgets: WidgetConfigDto[] = [];

  ngOnInit(): void {
    if (!this.permService.has('DASHBOARD_CUSTOMIZE')) {
      void this.router.navigate(['/dashboard']);
      return;
    }
    this.configService.getMyConfig().subscribe({
      next:  configs => { this.initialWidgets = configs; this.loading.set(false); },
      error: ()      => this.loading.set(false),
    });
  }

  protected onSave(configs: WidgetConfigDto[]): void {
    this.saving.set(true);
    this.configService.saveConfig(configs).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Dashboard saved', 'OK', { duration: 2500 });
        void this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(err?.error?.message ?? 'Save failed — please try again', 'Dismiss', { duration: 3500 });
      },
    });
  }

  protected onReset(): void {
    this.saving.set(true);
    this.configService.resetConfig().subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Reset to role default', 'OK', { duration: 2500 });
        void this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Reset failed — please try again', 'Dismiss', { duration: 3500 });
      },
    });
  }

  protected onCancel(): void { void this.router.navigate(['/dashboard']); }
}
