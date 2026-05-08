import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../pipes/inr.pipe';

const NOTES = [2000, 500, 200, 100, 50, 20, 10];
const COINS = [5, 2, 1];
const ALL = [...NOTES, ...COINS];

@Component({
  selector: 'app-cash-denomination',
  standalone: true,
  imports: [FormsModule, MatIconModule, InrPipe],
  templateUrl: './cash-denomination.component.html',
  styleUrl: './cash-denomination.component.scss',
})
export class CashDenominationComponent implements OnChanges {
  @Input({ required: true }) expectedAmount!: number;
  @Output() readonly validChange = new EventEmitter<boolean>();

  protected readonly notes = NOTES;
  protected readonly coins = COINS;
  protected readonly counts: Record<number, number> = {};

  protected get total(): number {
    return ALL.reduce((sum, d) => sum + d * Math.max(0, Math.floor(this.counts[d] || 0)), 0);
  }

  protected get isValid(): boolean {
    return this.expectedAmount > 0 && this.total === this.expectedAmount;
  }

  ngOnChanges(): void {
    this.validChange.emit(this.isValid);
  }

  protected onCountChange(): void {
    this.validChange.emit(this.isValid);
  }
}
