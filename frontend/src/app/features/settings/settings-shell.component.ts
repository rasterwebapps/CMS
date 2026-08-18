import { Component, inject, signal, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SystemConfigurationListComponent } from './system-configuration-list/system-configuration-list.component';
import { BrandingComponent } from './branding/branding.component';
import { IntegrationsSettingsComponent } from './integrations/integrations-settings.component';
import { TourService } from '../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../shared/tour/tour-button.component';
import { SETTINGS_SHELL_TOUR, SETTINGS_SHELL_FLOW_MAP } from '../../shared/tour/tours/settings.tours';

type SettingsTab = 'configuration' | 'branding' | 'integrations';

@Component({
  selector: 'app-settings-shell',
  standalone: true,
  imports: [RouterLink, SystemConfigurationListComponent, BrandingComponent, IntegrationsSettingsComponent, CmsTourButtonComponent],
  templateUrl: './settings-shell.component.html',
  styleUrl: './settings-shell.component.scss',
})
export class SettingsShellComponent {
  private readonly STORAGE_KEY = 'settings-active-tab';
  private readonly tourService = inject(TourService);

  @ViewChild(BrandingComponent) private brandingComp?: BrandingComponent;
  @ViewChild(IntegrationsSettingsComponent) private intComp?: IntegrationsSettingsComponent;

  protected readonly activeTab = signal<SettingsTab>(this.loadTab());

  constructor() {
    this.tourService.register('settings-shell', SETTINGS_SHELL_TOUR);
    this.tourService.registerFlowMap('settings-shell', SETTINGS_SHELL_FLOW_MAP);
  }

  protected setTab(tab: SettingsTab): void {
    this.activeTab.set(tab);
    localStorage.setItem(this.STORAGE_KEY, tab);
  }

  protected isSaving(): boolean {
    if (this.activeTab() === 'branding')      return this.brandingComp?.saving() ?? false;
    if (this.activeTab() === 'integrations')  return this.intComp?.saving() ?? false;
    return false;
  }

  protected isLoading(): boolean {
    if (this.activeTab() === 'branding')      return this.brandingComp?.loading() ?? true;
    if (this.activeTab() === 'integrations')  return this.intComp?.loading() ?? true;
    return false;
  }

  protected save(): void {
    if (this.activeTab() === 'branding')      this.brandingComp?.save();
    else if (this.activeTab() === 'integrations') this.intComp?.save();
  }

  private loadTab(): SettingsTab {
    const v = localStorage.getItem(this.STORAGE_KEY);
    return (v === 'branding' || v === 'integrations') ? v : 'configuration';
  }
}
