import { Component, Input, computed, signal } from '@angular/core';
import { NgClass } from '@angular/common';

/**
 * Maps a string status value to the appropriate `.status-badge` CSS modifier
 * and renders it as a styled pill.
 *
 * Covers enquiry, admission, student, faculty, equipment, attendance,
 * maintenance, fee-payment, and syllabus status values.
 *
 * Usage:
 *   <cms-status-badge [status]="row.status"></cms-status-badge>
 */
@Component({
  selector: 'cms-status-badge',
  standalone: true,
  imports: [NgClass],
  templateUrl: './status-badge.component.html',
  styleUrl: './status-badge.component.scss',
})
export class CmsStatusBadgeComponent {
  private readonly _status = signal('');

  @Input()
  set status(value: string) {
    this._status.set(value ?? '');
  }

  /** Human-readable label derived from the raw status string. */
  protected readonly label = computed(() => this.formatLabel(this._status()));

  /** CSS modifier class for the badge. */
  protected readonly badgeClass = computed(() => this.resolveClass(this._status()));

  private formatLabel(status: string): string {
    if (!status) return '—';
    return status
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  private resolveClass(status: string): string {
    switch (status?.toUpperCase()) {
      // ── Terminal success states ──────────────────────────────────────────
      case 'ADMITTED':
      case 'FEES_PAID':
      case 'DOCUMENTS_SUBMITTED':
      case 'DOCUMENTS_VERIFIED':
      case 'ACTIVE':
      case 'PRESENT':
      case 'AVAILABLE':
      case 'PAID':
      case 'APPROVED':
      case 'ENROLLED':
      case 'VERIFIED':
      case 'YES':
      case 'EDITABLE':
      case 'PUBLISHED':
        return 'status-active';

      // ── In-progress / warning states ────────────────────────────────────
      case 'PARTIALLY_PAID':
      case 'IN_USE':
      case 'UNDER_REPAIR':
      case 'LATE':
      case 'IN_PROGRESS':
      case 'WARNING':
      case 'UNDER_REVIEW':
      case 'UPLOADED':
      case 'ON_LEAVE':
      case 'SABBATICAL':
      case 'ON_HOLD':
        return 'status-warning';

      // ── Neutral / pending states ─────────────────────────────────────────
      case 'ENQUIRED':
      case 'FEES_FINALIZED':
      case 'FINALIZED':
      case 'DRAFT':
      case 'INTERESTED':
      case 'PENDING':
      case 'SUBMITTED':
      case 'UPCOMING':
      case 'PLANNED':
        return 'status-pending';

      // ── Negative / terminal failure states ───────────────────────────────
      case 'INACTIVE':
      case 'CANCELLED':
      case 'ABSENT':
      case 'RETIRED':
      case 'RESIGNED':
      case 'TERMINATED':
      case 'NOT_INTERESTED':
      case 'CLOSED':
      case 'REJECTED':
      case 'WITHDRAWN':
      case 'NOT_UPLOADED':
      case 'NOT_ALLOCATED':
      case 'NO':
      case 'LOCKED':
      case 'EXPIRED':
      case 'SUSPENDED':
      case 'EXPELLED':
      case 'WITHHELD':
      case 'DAMAGED':
      case 'DISPOSED':
        return 'status-inactive';

      // ── Returned / handed-back documents ────────────────────────────────
      case 'RETURNED':
        return 'status-warning';

      // ── Completion states ────────────────────────────────────────────────
      case 'GRADUATED':
      case 'COMPLETED':
      case 'FULFILLED':
        return 'status-graduated';

      // ── Maintenance / info states ────────────────────────────────────────
      case 'MAINTENANCE':
      case 'UNDER_MAINTENANCE':
        return 'status-maintenance';

      // ── Current / primary states ─────────────────────────────────────────
      case 'CURRENT':
      case 'ONGOING':
      case 'OPEN':
        return 'status-current';

      // ── Past / closed-cycle states ────────────────────────────────────────
      case 'PAST':
        return 'status-neutral';

      // ── Dropped / archived states ────────────────────────────────────────
      case 'DROPPED':
        return 'status-dropped';

      default:
        return '';
    }
  }
}
