import { Component, Input } from '@angular/core';
import { ColumnPickerState } from '../column-picker';

@Component({
  selector: 'cms-wrap-text-toggle',
  standalone: true,
  templateUrl: './wrap-text-toggle.component.html',
})
export class CmsWrapTextToggleComponent {
  @Input({ required: true }) state!: ColumnPickerState;
}
