import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import {
  CdkDragDrop, CdkDrag, CdkDropList, CdkDragHandle,
  CdkDragPlaceholder, moveItemInArray,
} from '@angular/cdk/drag-drop';
import { ColumnPickerState, ColumnDef } from './column-picker.state';

@Component({
  selector: 'cms-column-picker',
  standalone: true,
  imports: [CdkDrag, CdkDropList, CdkDragHandle, CdkDragPlaceholder],
  templateUrl: './column-picker.component.html',
  styleUrl: './column-picker.component.scss',
})
export class CmsColumnPickerComponent {
  @Input({ required: true }) state!: ColumnPickerState;
  @Output() readonly pinChange = new EventEmitter<void>();

  protected open = false;

  @HostListener('document:click')
  protected onDocClick(): void { this.open = false; }

  protected toggleCol(e: Event, key: string): void {
    e.stopPropagation();
    this.state.toggle(key);
  }

  protected pinCol(e: Event, key: string): void {
    e.stopPropagation();
    this.state.pin(key);
    this.pinChange.emit();
  }

  protected drop(event: CdkDragDrop<ColumnDef[]>): void {
    this.state.reorder(event.previousIndex, event.currentIndex);
  }
}
