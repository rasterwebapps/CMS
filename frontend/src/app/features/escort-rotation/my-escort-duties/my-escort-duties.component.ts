import { Component, OnInit, inject, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { EscortRotationService } from '../escort-rotation.service';
import { EscortDuty } from '../escort-rotation.model';

/** OC-175 Piece 3 — a faculty member's own upcoming clinical escort duties, fully computed via
 *  round-robin (no self-claim workflow, same shape as the existing student rotation feature). */
@Component({
  selector: 'app-my-escort-duties',
  standalone: true,
  imports: [MatProgressSpinnerModule, CmsEmptyStateComponent],
  templateUrl: './my-escort-duties.component.html',
  styleUrl: './my-escort-duties.component.scss',
})
export class MyEscortDutiesComponent implements OnInit {
  private readonly service = inject(EscortRotationService);

  protected readonly loading = signal(true);
  protected readonly duties = signal<EscortDuty[]>([]);

  ngOnInit(): void {
    this.service.myUpcomingDuties().subscribe({
      next: (duties) => { this.duties.set(duties); this.loading.set(false); },
      error: () => { this.duties.set([]); this.loading.set(false); },
    });
  }
}
