import { Injectable, computed, inject, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { FacultyService } from '../../faculty/faculty.service';
import { AdmissionService } from '../../admission/admission.service';
import { ProgramService } from '../../program/program.service';

export interface DocSlotLite { status: string }

export interface DocStats {
  total: number;
  verified: number;
  pending: number;
  missing: number;
}

/**
 * Singleton signal store + loader for the current user's required-document slots.
 *
 * Both the Profile screen (which mounts <cms-profile-documents> for full management)
 * and the role-specific Dashboards (which only need the aggregate counts and the
 * completion ring) read from the same `slots` signal. The service can also fetch
 * the slots itself via {@link loadFaculty} / {@link loadStudent} — used by
 * dashboards that the user lands on directly without first visiting Profile.
 */
@Injectable({ providedIn: 'root' })
export class DocumentSlotsService {
  private readonly facultyService = inject(FacultyService);
  private readonly admissionService = inject(AdmissionService);
  private readonly programService = inject(ProgramService);

  private readonly _slots = signal<DocSlotLite[]>([]);
  readonly slots = this._slots.asReadonly();

  readonly docStats = computed<DocStats>(() => {
    const s = this._slots();
    return {
      total: s.length,
      verified: s.filter((x) => x.status === 'VERIFIED').length,
      pending: s.filter((x) => x.status === 'UPLOADED').length,
      missing: s.filter((x) => x.status !== 'VERIFIED' && x.status !== 'UPLOADED').length,
    };
  });

  readonly progressPct = computed(() => {
    const { total, verified } = this.docStats();
    return total === 0 ? 0 : Math.round((verified / total) * 100);
  });

  /** SVG ring offset for r=60 → C=2πr ≈ 376.99. */
  readonly bigRingOffset = computed(() => 376.99 - (376.99 * this.progressPct()) / 100);

  /** Replace the full slot list — called by ProfileDocumentsComponent's slotsChange. */
  setSlots(slots: DocSlotLite[]): void {
    this._slots.set(slots.map((s) => ({ status: s.status })));
  }

  /** Fetch faculty's required slots directly (used when dashboard loads first). */
  loadFaculty(facultyId: number): void {
    forkJoin({
      requiredTypes: this.facultyService.getRequiredDocumentTypesForFaculty(facultyId).pipe(
        catchError(() => of<string[]>([])),
      ),
      documents: this.facultyService.getDocuments(facultyId).pipe(catchError(() => of([]))),
    }).subscribe(({ requiredTypes, documents }) => {
      const byType = new Map(documents.map((d) => [d.documentType, d]));
      const required = new Set(requiredTypes);
      const slots: DocSlotLite[] = Array.from(required).map((type) => ({
        status: byType.get(type)?.status ?? 'NOT_UPLOADED',
      }));
      documents
        .filter((d) => !required.has(d.documentType))
        .forEach((d) => slots.push({ status: d.status }));
      this._slots.set(slots);
    });
  }

  /** Fetch student's required slots directly (used when dashboard loads first). */
  loadStudent(admissionId: number): void {
    forkJoin({
      checklist: this.admissionService.getDocumentChecklist(admissionId).pipe(
        catchError(() => of<Record<string, string>>({})),
      ),
      documents: this.admissionService.getDocuments(admissionId).pipe(catchError(() => of([]))),
    }).subscribe(({ checklist, documents }) => {
      const byType = new Map(documents.map((d) => [d.documentType, d]));
      const required = new Set(Object.keys(checklist));
      const slots: DocSlotLite[] = Array.from(required).map((type) => ({
        status: byType.get(type)?.verificationStatus ?? checklist[type] ?? 'NOT_UPLOADED',
      }));
      documents
        .filter((d) => !required.has(d.documentType))
        .forEach((d) => slots.push({ status: d.verificationStatus }));
      this._slots.set(slots);
    });
  }

  clear(): void {
    this._slots.set([]);
  }
}

