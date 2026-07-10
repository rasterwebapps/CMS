import { Component, ElementRef, EventEmitter, inject, Input, OnDestroy, Output, TemplateRef, ViewChild, ViewContainerRef } from '@angular/core';
import {
  CdkDragDrop, CdkDrag, CdkDropList, CdkDragHandle,
  CdkDragPlaceholder, moveItemInArray,
} from '@angular/cdk/drag-drop';
import { ConnectedPosition, Overlay, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { ColumnPickerState, ColumnDef } from './column-picker.state';

// Prefer below the trigger, right-aligned; flip above if there's no room below.
// CDK measures the actual rendered panel and viewport, so no hardcoded height
// estimate is needed (unlike the old manual getBoundingClientRect() math).
const DROPDOWN_POSITIONS: ConnectedPosition[] = [
  { originX: 'end', originY: 'bottom', overlayX: 'end', overlayY: 'top', offsetY: 5 },
  { originX: 'end', originY: 'top', overlayX: 'end', overlayY: 'bottom', offsetY: -5 },
];

@Component({
  selector: 'cms-column-picker',
  standalone: true,
  imports: [CdkDrag, CdkDropList, CdkDragHandle, CdkDragPlaceholder],
  templateUrl: './column-picker.component.html',
  styleUrl: './column-picker.component.scss',
})
export class CmsColumnPickerComponent implements OnDestroy {
  @Input({ required: true }) state!: ColumnPickerState;
  @Output() readonly pinChange = new EventEmitter<void>();

  @ViewChild('triggerBtn') private triggerBtn?: ElementRef<HTMLButtonElement>;
  @ViewChild('dropdownTpl') private dropdownTpl?: TemplateRef<unknown>;

  private readonly overlay = inject(Overlay);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private overlayRef: OverlayRef | null = null;

  protected open = false;

  protected toggleOpen(): void {
    if (this.open) {
      this.closeDropdown();
    } else {
      this.openDropdown();
    }
  }

  // Rendered via CDK Overlay (portaled to the app's overlay container at the
  // document root) instead of a local position:fixed div. A plain fixed div
  // here gets trapped inside whatever ancestor stacking context the screen
  // happens to have — e.g. the sticky global app-toolbar (z-index:100) always
  // painted over it because .mlp-toolbar's own promoted context (also
  // z-index:100) is nested inside mat-sidenav-container, which is capped
  // below the toolbar regardless of the dropdown's own z-index. The overlay
  // escapes that entirely.
  private openDropdown(): void {
    if (!this.triggerBtn || !this.dropdownTpl) return;

    const positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.triggerBtn.nativeElement)
      .withPositions(DROPDOWN_POSITIONS)
      .withViewportMargin(8)
      .withPush(true);

    this.overlayRef = this.overlay.create({
      positionStrategy,
      scrollStrategy: this.overlay.scrollStrategies.reposition(),
      hasBackdrop: true,
      backdropClass: 'cdk-overlay-transparent-backdrop',
      maxHeight: 'calc(100vh - 16px)',
    });

    this.overlayRef.backdropClick().subscribe(() => this.closeDropdown());
    this.overlayRef.attach(new TemplatePortal(this.dropdownTpl, this.viewContainerRef));
    this.open = true;
  }

  private closeDropdown(): void {
    this.overlayRef?.dispose();
    this.overlayRef = null;
    this.open = false;
  }

  ngOnDestroy(): void {
    this.overlayRef?.dispose();
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
