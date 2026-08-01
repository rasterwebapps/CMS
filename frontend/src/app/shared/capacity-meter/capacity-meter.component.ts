import { Component, Input, computed, signal } from '@angular/core';

/**
 * Occupied-vs-capacity meter — a small bar + label, red when at/over capacity.
 * Extracted from the Hostel Room Allocation dashboard's `.rad-occupancy` markup, which was the
 * only prior instance of this pattern; this is its second use.
 *
 * Usage:
 *   <cms-capacity-meter [occupied]="18" [capacity]="30"></cms-capacity-meter>
 */
@Component({
  selector: 'cms-capacity-meter',
  standalone: true,
  templateUrl: './capacity-meter.component.html',
  styleUrl: './capacity-meter.component.scss',
})
export class CmsCapacityMeterComponent {
  private readonly _occupied = signal(0);
  private readonly _capacity = signal<number | null>(null);

  @Input()
  set occupied(value: number) {
    this._occupied.set(value ?? 0);
  }

  @Input()
  set capacity(value: number | null | undefined) {
    this._capacity.set(value ?? null);
  }

  /** Optional override for the label text; defaults to "occupied / capacity". */
  @Input() label = '';

  protected readonly percent = computed(() => {
    const capacity = this._capacity();
    if (!capacity || capacity <= 0) return 0;
    return Math.min(100, (this._occupied() / capacity) * 100);
  });

  protected readonly isFull = computed(() => {
    const capacity = this._capacity();
    return capacity != null && capacity > 0 && this._occupied() >= capacity;
  });

  protected readonly displayLabel = computed(() => {
    if (this.label) return this.label;
    const capacity = this._capacity();
    return `${this._occupied()} / ${capacity ?? '—'}`;
  });
}
