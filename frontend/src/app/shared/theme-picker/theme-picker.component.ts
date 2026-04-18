import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { COLOR_SWATCHES, ThemeService } from '../../core/theme/theme.service';

@Component({
  selector: 'app-theme-picker',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  templateUrl: './theme-picker.component.html',
  styleUrl: './theme-picker.component.scss',
})
export class ThemePickerComponent {
  protected readonly themeService = inject(ThemeService);
  protected readonly swatches = COLOR_SWATCHES;
}
