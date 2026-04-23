import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

export type CmsViewMode = 'card' | 'table';

/**
 * Segmented control for switching between card and table views on
 * explorer (list) screens. The current mode is persisted in
 * localStorage under the supplied storage key so a user's preference
 * is remembered per screen.
 *
 * Usage:
 *   <cms-view-toggle
 *     [mode]="viewMode()"
 *     storageKey="department-view"
 *     (modeChange)="viewMode.set($event)" />
 */
@Component({
  selector: 'cms-view-toggle',
  standalone: true,
  templateUrl: './view-toggle.component.html',
  styleUrl: './view-toggle.component.scss',
})
export class CmsViewToggleComponent implements OnInit {
  @Input() mode: CmsViewMode = 'card';
  @Input() storageKey: string | null = null;
  @Output() modeChange = new EventEmitter<CmsViewMode>();

  ngOnInit(): void {
    if (!this.storageKey) return;
    try {
      const stored = localStorage.getItem(this.storageKey);
      if (stored === 'card' || stored === 'table') {
        if (stored !== this.mode) {
          this.mode = stored;
          this.modeChange.emit(stored);
        }
      }
    } catch {
      /* localStorage may be unavailable — ignore */
    }
  }

  protected select(next: CmsViewMode): void {
    if (this.mode === next) return;
    this.mode = next;
    if (this.storageKey) {
      try {
        localStorage.setItem(this.storageKey, next);
      } catch {
        /* ignore */
      }
    }
    this.modeChange.emit(next);
  }
}
