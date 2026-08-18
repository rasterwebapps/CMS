import { Component, inject, Input, OnInit } from '@angular/core';
import { TourService } from './tour.service';
import { CmsTourPanelComponent } from './tour-panel/tour-panel.component';

@Component({
  selector: 'cms-tour-button',
  standalone: true,
  imports: [CmsTourPanelComponent],
  template: `
    @if (panelOpen) {
      <cms-tour-panel
        [tourKey]="tourKey"
        (closed)="panelOpen = false"
        (guidedRequested)="onGuidedRequested()"
      />
    }
    @if (guidedActive) {
      <button type="button" class="cms-tour-swap-pill" (click)="swapToFlowMap()" title="Switch to Flow Map">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/>
        </svg>
        Flow Map
      </button>
    }
    @if (hasFlowMap) {
      <!-- Small icon-level switch — no popup needed for a two-way choice -->
      <div class="cms-tour-switch" role="group" aria-label="Take a Tour">
        <button type="button" class="cms-tour-switch-btn" (click)="startGuided()" title="Guided Tour" aria-label="Guided Tour">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10"/>
            <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/>
          </svg>
        </button>
        <span class="cms-tour-switch-divider"></span>
        <button type="button" class="cms-tour-switch-btn" (click)="openFlowMap()" title="Flow Map" aria-label="Flow Map">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="6" y1="3" x2="6" y2="15"/><circle cx="18" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M18 9a9 9 0 0 1-9 9"/>
          </svg>
        </button>
      </div>
    } @else if (iconOnly) {
      <!-- Icon-only variant -->
      <button
        class="cms-tour-icon-btn"
        [class.cms-tour-icon-btn--info-sq]="iconVariant === 'info-square'"
        type="button"
        (click)="start()"
        title="Take a Tour"
        aria-label="Take a Tour"
      >
        @if (iconVariant === 'info-circle') {
          <!-- Circle-i outline (Lucide info — Option A) -->
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 16v-4"/>
            <path d="M12 8h.01"/>
          </svg>
        } @else if (iconVariant === 'info-square') {
          <!-- Square-i badge (Option B) -->
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <rect x="3" y="3" width="18" height="18" rx="4"/>
            <line x1="12" y1="11" x2="12" y2="17"/>
            <circle cx="12" cy="8" r="0.5" fill="currentColor" stroke="currentColor" stroke-width="1.5"/>
          </svg>
        } @else {
          <!-- Compass (default) -->
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10"/>
            <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/>
          </svg>
        }
      </button>
    } @else {
      <!-- Full text variant -->
      <button
        class="cms-tour-btn"
        type="button"
        (click)="start()"
        [attr.aria-label]="buttonLabel"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24"
             fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="10"/>
          <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/>
        </svg>
        {{ buttonLabel }}
      </button>
    }
  `,
  styles: [`
    /* ── Full text button ── */
    .cms-tour-btn {
      position: relative;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 7px 14px;
      border: 1px solid var(--cms-border-default);
      border-radius: 8px;
      background: var(--cms-bg-card);
      color: var(--cms-text-secondary);
      font-size: 0.8125rem;
      font-weight: 500;
      font-family: var(--cms-font-ui);
      cursor: pointer;
      transition: border-color 0.15s, color 0.15s, box-shadow 0.15s;
      white-space: nowrap;

      svg { flex-shrink: 0; }

      &:hover {
        border-color: var(--cms-primary);
        color: var(--cms-primary);
        box-shadow: 0 0 0 3px var(--cms-primary-ring);
      }

      &--new {
        border-color: var(--cms-primary);
        color: var(--cms-primary);
      }

      /* Info-square tint: slightly more rounded to match dept-back-btn style */
      &--info-sq {
        border-radius: 10px;

        &:hover {
          background: rgba(var(--cms-primary-rgb), 0.08);
          border-color: rgba(var(--cms-primary-rgb), 0.35);
          color: var(--cms-primary);
          box-shadow: 0 0 0 3px var(--cms-primary-ring);
        }
      }
    }

    /* ── Icon-only button ── */
    .cms-tour-icon-btn {
      position: relative;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 34px;
      height: 34px;
      padding: 0;
      border: 1px solid var(--cms-border-default);
      border-radius: 8px;
      background: var(--cms-bg-card);
      color: var(--cms-text-secondary);
      cursor: pointer;
      transition: border-color 0.15s, color 0.15s, box-shadow 0.15s, background 0.15s;
      flex-shrink: 0;

      svg { flex-shrink: 0; }

      &:hover {
        border-color: var(--cms-primary);
        color: var(--cms-primary);
        background: var(--cms-primary-ring, rgba(99,102,241,0.08));
        box-shadow: 0 0 0 3px var(--cms-primary-ring);
      }
    }

    /* ── Small icon-level switch, replaces the single icon button on screens with a Flow Map ── */
    .cms-tour-switch {
      display: inline-flex;
      align-items: center;
      border: 1px solid var(--cms-border-default);
      border-radius: 8px;
      background: var(--cms-bg-card);
      overflow: hidden;
      flex-shrink: 0;
    }
    .cms-tour-switch-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 34px;
      padding: 0;
      border: none;
      background: transparent;
      color: var(--cms-text-secondary);
      cursor: pointer;
      transition: color 0.15s, background 0.15s;

      svg { flex-shrink: 0; }

      &:hover {
        color: var(--cms-primary);
        background: var(--cms-primary-ring, rgba(99,102,241,0.08));
      }
    }
    .cms-tour-switch-divider {
      width: 1px;
      align-self: stretch;
      margin: 7px 0;
      background: var(--cms-border-default);
    }

    /* ── Floating "swap back to Flow Map" pill, shown while a Guided Tour is running ── */
    .cms-tour-swap-pill {
      position: fixed;
      bottom: 22px;
      right: 22px;
      z-index: 2147483647;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 9px 16px;
      border: none;
      border-radius: 999px;
      background: var(--cms-primary);
      color: #fff;
      font-size: 0.8125rem;
      font-weight: 600;
      font-family: var(--cms-font-ui);
      cursor: pointer;
      box-shadow: var(--cms-shadow-lg);
      transition: transform 0.15s, box-shadow 0.15s;

      svg { flex-shrink: 0; }

      &:hover {
        transform: translateY(-2px);
        box-shadow: var(--cms-shadow-xl);
      }
    }
  `],
})
export class CmsTourButtonComponent implements OnInit {
  @Input({ required: true }) tourKey!: string;
  /** When true, renders a compact icon-only button instead of the full text button. */
  @Input() iconOnly = false;
  /**
   * Icon to use when iconOnly=true.
   * 'compass' (default) — the existing navigation compass icon.
   * 'info-circle' — Lucide circle-i outline (Option A).
   * 'info-square' — a rounded-square "i" badge icon (Option B).
   */
  @Input() iconVariant: 'compass' | 'info-circle' | 'info-square' = 'compass';
  /** Label text shown on the full-text button variant. Defaults to 'Take a Tour'. */
  @Input() buttonLabel = 'Take a Tour';

  protected panelOpen = false;
  protected guidedActive = false;
  protected hasFlowMap = false;

  private readonly tourService = inject(TourService);

  ngOnInit(): void {
    this.hasFlowMap = this.tourService.hasFlowMap(this.tourKey);
  }

  /** Used only on screens with no Flow Map — behaves exactly as before. */
  protected start(): void {
    this.tourService.start(this.tourKey);
  }

  protected startGuided(): void {
    this.guidedActive = true;
    this.tourService.start(this.tourKey, () => { this.guidedActive = false; });
  }

  protected openFlowMap(): void {
    this.panelOpen = true;
  }

  /** The Flow Map panel's own tab/footer asked to swap to Guided Tour. */
  protected onGuidedRequested(): void {
    this.panelOpen = false;
    this.startGuided();
  }

  /** Floating pill clicked mid-Guided-Tour — end it and reopen straight into the Flow Map. */
  protected swapToFlowMap(): void {
    this.tourService.stopActive();
    this.guidedActive = false;
    this.panelOpen = true;
  }
}
