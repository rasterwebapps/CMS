import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { TourService } from './tour.service';
import { TourStep } from './tour-step.model';

@Component({
  selector: 'cms-tour-tooltip',
  standalone: true,
  imports: [],
  templateUrl: './tour-tooltip.component.html',
  styleUrl: './tour-tooltip.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TourTooltipComponent implements OnInit, OnDestroy {
  protected readonly tourService = inject(TourService);

  protected readonly step = computed<TourStep | null>(() => this.tourService.currentStep());
  protected readonly stepIndex = computed(() => this.tourService.currentStepIndex());
  protected readonly totalSteps = computed(() => this.tourService.steps().length);
  protected readonly isLastStep = computed(
    () => this.tourService.currentStepIndex() === this.tourService.steps().length - 1,
  );
  protected readonly isFirstStep = computed(() => this.tourService.currentStepIndex() === 0);
  protected readonly isWaiting = computed(() => this.tourService.isWaiting());

  /** Zero-based index array for rendering progress dots. */
  protected readonly dots = computed(() =>
    Array.from({ length: this.tourService.steps().length }, (_, i) => i),
  );

  ngOnInit(): void {
    document.addEventListener('keydown', this.onKeyDown);
  }

  ngOnDestroy(): void {
    document.removeEventListener('keydown', this.onKeyDown);
  }

  protected onNext(): void {
    if (this.isWaiting()) return;
    this.tourService.advance();
  }

  protected onPrevious(): void {
    this.tourService.previous();
  }

  protected onDismiss(): void {
    this.tourService.end();
  }

  protected onHardClose(): void {
    this.tourService.hardClose();
  }

  private readonly onKeyDown = (event: KeyboardEvent): void => {
    switch (event.key) {
      case 'Escape':
        event.preventDefault();
        this.tourService.end();
        break;
      case 'ArrowRight':
      case 'Enter':
        if (!this.isWaiting()) {
          event.preventDefault();
          this.tourService.advance();
        }
        break;
      case 'ArrowLeft':
        if (!this.isFirstStep()) {
          event.preventDefault();
          this.tourService.previous();
        }
        break;
    }
  };
}
