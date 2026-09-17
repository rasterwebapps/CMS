import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { GuardianService } from '../../features/guardian/guardian.service';
import { WardSummaryResponse } from '../../features/guardian/guardian.model';
import { PermissionService } from '../permissions/permission.service';

/**
 * Holds the guardian's set of wards and which one is currently "active" for every ward-scoped
 * screen (parent dashboard, ward fees, the ward switcher in the top bar). Signal-based, matching
 * this codebase's established reactive style (PermissionService/AuthService).
 *
 * Loads automatically, once, as soon as PermissionService reports the caller holds a ward-scoped
 * permission -- never calls GET /guardian/wards for a non-guardian user. Selection is in-memory
 * only (resets on reload), which is fine for a single-session "who am I looking at" choice; there
 * is no existing localStorage-persisted-selector precedent in this codebase to justify persisting
 * it further.
 *
 * Public surface for consumers (WardSwitcherComponent, ParentDashboardComponent, WardFeesComponent):
 *   - wards(): the guardian's wards, empty until loaded (or if the caller isn't a guardian at all)
 *   - selectedWardId(): the active ward's studentId, or null before anything is selected/loaded
 *   - selectedWard(): the full WardSummaryResponse for the active ward, or null
 *   - selectWard(studentId): switch the active ward
 */
@Injectable({ providedIn: 'root' })
export class WardContextService {
  private readonly guardianService = inject(GuardianService);
  private readonly permissionService = inject(PermissionService);

  private readonly _wards = signal<WardSummaryResponse[]>([]);
  private readonly _selectedWardId = signal<number | null>(null);
  private loadRequested = false;

  readonly wards = this._wards.asReadonly();
  readonly selectedWardId = this._selectedWardId.asReadonly();
  readonly selectedWard = computed(() =>
    this._wards().find((w) => w.studentId === this._selectedWardId()) ?? null,
  );
  readonly hasMultipleWards = computed(() => this._wards().length > 1);

  constructor() {
    // Load once permissions have arrived and the caller actually has ward-scoped access --
    // effect() re-runs whenever permissionService's underlying signal changes, so this fires
    // correctly whether permissions were already loaded or arrive shortly after app bootstrap.
    effect(() => {
      if (this.loadRequested || !this.permissionService.loaded()) return;
      if (!this.permissionService.hasAny('MY_WARD_ATTENDANCE_VIEW', 'MY_WARD_EXAM_RESULT_VIEW')) return;
      this.loadRequested = true;
      this.guardianService.myWards().subscribe({
        next: (wards) => {
          this._wards.set(wards);
          const primary = wards.find((w) => w.isPrimary) ?? wards[0];
          if (primary) this._selectedWardId.set(primary.studentId);
        },
        error: () => this._wards.set([]),
      });
    });
  }

  selectWard(studentId: number): void {
    if (this._wards().some((w) => w.studentId === studentId)) {
      this._selectedWardId.set(studentId);
    }
  }
}
