import { Component, ElementRef, EventEmitter, HostListener, Input, Output, ViewChild } from '@angular/core';
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

  @ViewChild('triggerBtn') private triggerBtn?: ElementRef<HTMLButtonElement>;

  protected open = false;
  protected dropdownTop = '0px';
  protected dropdownRight = '0px';

  @HostListener('document:click')
  protected onDocClick(): void { this.open = false; }

  // Close on scroll/resize so the panel doesn't drift from its anchor
  @HostListener('window:scroll', ['$event.target'])
  @HostListener('window:resize')
  protected onViewportChange(): void { this.open = false; }

  protected toggleOpen(): void {
    this.open = !this.open;
    if (this.open) this.positionDropdown();
  }

  private positionDropdown(): void {
    const rect = this.triggerBtn?.nativeElement.getBoundingClientRect();
    if (!rect) return;
    this.dropdownTop   = `${rect.bottom + 5}px`;
    this.dropdownRight = `${window.innerWidth - rect.right}px`;
  }

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
