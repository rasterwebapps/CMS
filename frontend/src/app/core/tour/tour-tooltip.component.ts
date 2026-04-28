import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import { TourService } from './tour.service';
import { TourStep } from './tour-step.model';

/**
 * Floating tooltip rendered into a CDK `OverlayRef` by {@link TourService}.
 *
 * The component is intentionally thin — all state lives in `TourService`
 * (Angular signals) and this component just reflects it. This means the
 * service can dispose and re-create the overlay without losing state.
 */
@Component({
  selector: 'cms-tour-tooltip',
  standalone: true,
  imports: [A11yModule],
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
  protected readonly isWaiting = computed(() => this.tourService.isWaiting());

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

  protected onEscape(): void {
    this.tourService.end();
  }

  private readonly onKeyDown = (event: KeyboardEvent): void => {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.tourService.end();
    }
  };
}
