import { Component, Input, computed, signal } from '@angular/core';
import { NgClass } from '@angular/common';

/**
 * `'urgency'` colors HIGH/CRITICAL red (something needs attention).
 * `'strength'` colors HIGH green (a strong/good mapping or rating).
 * Same literal values, opposite correct colors — must be picked explicitly,
 * never inferred from the field name.
 */
export type TypeBadgePalette = 'default' | 'urgency' | 'strength';

/**
 * Single source of truth for the soft-pastel `.cms-badge--soft-*` color used
 * by a categorical/type enum value in a list table — student type, referral
 * source, priority, mapping level, payment mode, etc.
 *
 * NOT for status fields (use `<cms-status-badge>`) or active/inactive flags.
 *
 * Usage:
 *   <cms-type-badge [value]="row.studentType"></cms-type-badge>
 *   <cms-type-badge [value]="row.priority" palette="urgency"></cms-type-badge>
 *   <cms-type-badge [value]="row.mappingLevel" palette="strength"></cms-type-badge>
 */
@Component({
  selector: 'cms-type-badge',
  standalone: true,
  imports: [NgClass],
  templateUrl: './type-badge.component.html',
})
export class CmsTypeBadgeComponent {
  private readonly _value = signal('');

  @Input()
  set value(value: string | null | undefined) {
    this._value.set(value ?? '');
  }

  /** Optional override for the displayed text; defaults to a formatted version of `value`. */
  @Input() label?: string;

  /** Disambiguates HIGH/MEDIUM/LOW-shaped values that need opposite color logic. */
  @Input() palette: TypeBadgePalette = 'default';

  protected readonly displayLabel = computed(() => this.label ?? this.formatLabel(this._value()));
  protected readonly badgeClass = computed(() => this.resolveClass(this._value(), this.palette));

  private formatLabel(value: string): string {
    if (!value) return '—';
    return value
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  private resolveClass(value: string, palette: TypeBadgePalette): string {
    const upper = value?.toUpperCase();

    if (palette === 'urgency') {
      switch (upper) {
        case 'CRITICAL':
        case 'HIGH':
          return 'cms-badge--soft-red';
        case 'MEDIUM':
          return 'cms-badge--soft-amber';
        case 'LOW':
          return 'cms-badge--soft-gray';
      }
    }

    if (palette === 'strength') {
      switch (upper) {
        case 'HIGH':
          return 'cms-badge--soft-green';
        case 'MEDIUM':
          return 'cms-badge--soft-amber';
        case 'LOW':
          return 'cms-badge--soft-gray';
      }
    }

    switch (upper) {
      // ── Student lifecycle / enrollment stage ───────────────────────────
      case 'STUDENT':
        return 'cms-badge--soft-blue';
      case 'ENQUIRY':
      case 'PRE_ENROLL':
      case 'PRE-ENROLL':
        return 'cms-badge--soft-gray';

      // ── Residency type ──────────────────────────────────────────────────
      case 'HOSTELER':
        return 'cms-badge--soft-green';
      case 'DAY_SCHOLAR':
        return 'cms-badge--soft-amber';

      // ── Person/member/referral type ──────────────────────────────────────
      case 'FACULTY':
      case 'FACULTY_REFERRER':
        return 'cms-badge--soft-purple';
      case 'AGENT':
      case 'STAFF_REFERRER':
        return 'cms-badge--soft-accent';
      case 'REFERRAL_TYPE':
      case 'NONE':
        return 'cms-badge--soft-gray';

      // ── Session / class type ─────────────────────────────────────────────
      case 'LECTURE':
        return 'cms-badge--soft-blue';
      case 'LAB':
        return 'cms-badge--soft-accent';

      // ── Fee quota ─────────────────────────────────────────────────────────
      case 'MANAGEMENT':
        return 'cms-badge--soft-purple';
      case 'COUNSELLING':
        return 'cms-badge--soft-blue';

      // ── Journal / periodical scope ─────────────────────────────────────────
      case 'NATIONAL':
        return 'cms-badge--soft-blue';
      case 'INTERNATIONAL':
        return 'cms-badge--soft-purple';

      default:
        return 'cms-badge--soft-gray';
    }
  }
}
