import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FacultyService } from '../../faculty/faculty.service';
import { Faculty } from '../../faculty/faculty.model';
import { FacultyAbsenceService } from '../faculty-absence.service';
import { AffectedSession, FacultyAbsence, SubstituteCandidate } from '../faculty-absence.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-faculty-absence',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './faculty-absence.component.html',
  styleUrl: './faculty-absence.component.scss',
})
export class FacultyAbsenceComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly absenceService = inject(FacultyAbsenceService);
  private readonly toast = inject(ToastService);

  protected readonly facultyList = signal<Faculty[]>([]);
  protected selectedFacultyId: number | null = null;
  protected absenceDate: string = new Date().toISOString().slice(0, 10);
  protected reason = '';

  protected readonly marking = signal(false);
  protected readonly currentAbsence = signal<FacultyAbsence | null>(null);
  protected readonly affectedSessions = signal<AffectedSession[]>([]);
  protected readonly loadingSessions = signal(false);

  protected readonly findingCandidatesFor = signal<number | null>(null);
  protected readonly candidates = signal<SubstituteCandidate[]>([]);
  protected readonly loadingCandidates = signal(false);
  protected readonly applyingSubstituteFacultyId = signal<number | null>(null);

  ngOnInit(): void {
    this.facultyService.getAll().subscribe({
      next: (list) => this.facultyList.set(list),
      error: () => this.toast.error('Failed to load faculty list'),
    });
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
      next: (absence) => {
        this.currentAbsence.set(absence);
        this.toast.success('Absence recorded');
        this.marking.set(false);
        this.loadAffectedSessions(absence.id);
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
        this.loadAffectedSessions(absence.id);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to apply substitute');
        this.applyingSubstituteFacultyId.set(null);
      },
    });
  }
}
