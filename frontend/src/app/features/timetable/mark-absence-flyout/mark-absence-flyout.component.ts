import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { FacultyService } from '../../faculty/faculty.service';
import { Faculty } from '../../faculty/faculty.model';
import { FacultyAbsenceService } from '../faculty-absence.service';
import { AffectedSession, FacultyAbsence, SubstituteCandidate } from '../faculty-absence.model';
import { ToastService } from '../../../core/toast/toast.service';

/**
 * Mark-absent + find-substitute flow, as a flyout over the Faculty Absence list — was previously
 * the whole /faculty-absence screen (FacultyAbsenceComponent) before that route became a list by
 * default; this is that same component's logic, just opened via the list's "+" button instead of
 * being the route target itself.
 */
@Component({
  selector: 'app-mark-absence-flyout',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatProgressSpinnerModule, CmsFlyoutPanelComponent],
  templateUrl: './mark-absence-flyout.component.html',
  styleUrl: './mark-absence-flyout.component.scss',
})
export class MarkAbsenceFlyoutComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly absenceService = inject(FacultyAbsenceService);
  private readonly toast = inject(ToastService);

  /** Set by the list's row action to reopen an already-marked absence straight into the
   *  affected-sessions/substitute view (skips the mark-absent form entirely) instead of the
   *  default "mark a new absence" mode. */
  readonly existingAbsenceId = input<number | null>(null);

  readonly closed = output<void>();
  /** Emitted once an absence has been recorded, so the list behind this flyout can refresh —
   *  fired right after markAbsent succeeds, not only when the flyout is closed, since the caller
   *  should reflect the new row even if the coordinator keeps this open to find substitutes. */
  readonly saved = output<void>();

  protected readonly facultyList = signal<Faculty[]>([]);
  protected selectedFacultyId: number | null = null;
  protected absenceDate: string = new Date().toISOString().slice(0, 10);
  protected reason = '';

  protected readonly marking = signal(false);
  protected readonly loadingAbsence = signal(false);
  protected readonly currentAbsence = signal<FacultyAbsence | null>(null);
  protected readonly affectedSessions = signal<AffectedSession[]>([]);
  protected readonly loadingSessions = signal(false);

  protected readonly findingCandidatesFor = signal<number | null>(null);
  protected readonly candidates = signal<SubstituteCandidate[]>([]);
  protected readonly loadingCandidates = signal(false);
  protected readonly applyingSubstituteFacultyId = signal<number | null>(null);

  protected readonly headerText = computed(() => {
    const absence = this.currentAbsence();
    return absence ? `Faculty Absence — ${absence.facultyName}` : 'Mark Faculty Absent';
  });

  ngOnInit(): void {
    this.facultyService.getAll().subscribe({
      next: (list) => this.facultyList.set(list),
      error: () => this.toast.error('Failed to load faculty list'),
    });

    const existingId = this.existingAbsenceId();
    if (existingId != null) {
      this.loadingAbsence.set(true);
      this.absenceService.getAbsence(existingId).subscribe({
        next: (absence) => {
          this.currentAbsence.set(absence);
          this.loadingAbsence.set(false);
          this.loadAffectedSessions(absence.id);
        },
        error: () => {
          this.toast.error('Failed to load absence');
          this.loadingAbsence.set(false);
        },
      });
    }
  }

  protected onClose(): void {
    this.closed.emit();
  }

  protected markAbsent(): void {
    if (!this.selectedFacultyId) return;
    this.marking.set(true);
    this.currentAbsence.set(null);
    this.affectedSessions.set([]);
    this.absenceService.markAbsent({
      facultyId: this.selectedFacultyId,
      absenceDate: this.absenceDate,
      reason: this.reason || null,
    }).subscribe({
      next: () => {
        this.toast.success('Absence recorded');
        this.marking.set(false);
        // Close straight back to the list instead of staying open on a "Done" button — the
        // affected-sessions/find-substitute flow is still reachable any time afterward via the
        // list row's View action (existingAbsenceId), so nothing is lost by not showing it inline
        // here too.
        this.saved.emit();
        this.closed.emit();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to record absence');
        this.marking.set(false);
      },
    });
  }

  private loadAffectedSessions(absenceId: number): void {
    this.loadingSessions.set(true);
    this.absenceService.getAffectedSessions(absenceId).subscribe({
      next: (sessions) => { this.affectedSessions.set(sessions); this.loadingSessions.set(false); },
      error: () => { this.toast.error('Failed to load affected sessions'); this.loadingSessions.set(false); },
    });
  }

  protected findSubstitute(session: AffectedSession): void {
    const absence = this.currentAbsence();
    if (!absence) return;
    this.findingCandidatesFor.set(session.classScheduleId);
    this.candidates.set([]);
    this.loadingCandidates.set(true);
    this.absenceService.getSubstituteCandidates(session.classScheduleId, absence.absenceDate).subscribe({
      next: (list) => { this.candidates.set(list); this.loadingCandidates.set(false); },
      error: () => { this.toast.error('Failed to load substitute candidates'); this.loadingCandidates.set(false); },
    });
  }

  protected cancelFindSubstitute(): void {
    this.findingCandidatesFor.set(null);
    this.candidates.set([]);
  }

  protected applySubstitute(classScheduleId: number, facultyId: number): void {
    const absence = this.currentAbsence();
    if (!absence) return;
    this.applyingSubstituteFacultyId.set(facultyId);
    this.absenceService.applySubstitute(absence.id, classScheduleId, facultyId).subscribe({
      next: () => {
        this.toast.success('Substitute applied');
        this.applyingSubstituteFacultyId.set(null);
        this.cancelFindSubstitute();
        this.saved.emit();
        this.loadAffectedSessions(absence.id);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to apply substitute');
        this.applyingSubstituteFacultyId.set(null);
      },
    });
  }
}
