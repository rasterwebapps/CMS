import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

export type CmsViewMode = 'card' | 'table';

/**
 * Segmented control for switching between card and table views on
 * explorer (list) screens. The current mode is persisted in
 * localStorage under the supplied storage key so a user's preference
 * is remembered per screen.
 *
 * The component never mutates its `mode` input — it emits `modeChange`
 * and lets the parent own the source of truth (one-way data flow).
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
    let stored: string | null = null;
    try {
      stored = localStorage.getItem(this.storageKey);
    } catch {
      /* localStorage may be unavailable — ignore */
    }
    if ((stored === 'card' || stored === 'table') && stored !== this.mode) {
      // Ask the parent to adopt the persisted preference.
      this.modeChange.emit(stored);
    }
  }

  protected select(next: CmsViewMode): void {
    if (this.mode === next) return;
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

