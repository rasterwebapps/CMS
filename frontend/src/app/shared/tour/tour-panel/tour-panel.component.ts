import { Component, EventEmitter, HostListener, inject, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { TourFlowMap, TourService } from '../tour.service';

/**
 * The Flow Map view for "Take a Tour" — opened directly by the small icon-level
 * switch on screens that have one registered (no chooser popup involved). The
 * footer link still lets you swap to the Guided Tour at any time — that hands
 * off to the existing driver.js walkthrough via `guidedRequested`, since it
 * needs to highlight real elements behind this panel.
 */
@Component({
  selector: 'cms-tour-panel',
  standalone: true,
  template: `
    <div class="ctp-backdrop" (click)="onBackdropClick($event)">
      <div class="ctp-panel" role="dialog" aria-modal="true" [attr.aria-label]="'Flow Map — ' + tourKey">
        <div class="ctp-hdr">
          <div class="ctp-hdr-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="6" y1="3" x2="6" y2="15"/><circle cx="18" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M18 9a9 9 0 0 1-9 9"/>
            </svg>
            Flow Map
          </div>
          <button class="ctp-close" type="button" (click)="close()" aria-label="Close">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        @if (flowMap) {
          <div class="ctp-rail-wrap">
            <div class="ctp-rail-caption">Where this screen sits in the journey</div>
            <div class="ctp-rail">
              @for (stage of flowMap.funnel; track $index; let i = $index) {
                @if (i > 0) { <span class="ctp-rail-line"></span> }
                <div class="ctp-rail-node" [class.ctp-rail-node--current]="i === flowMap.currentIndex" [title]="stage.description">
                  <span class="ctp-rail-dot">{{ pad(i + 1) }}</span>
                  <span class="ctp-rail-label">{{ stage.label }}</span>
                  <span class="ctp-rail-here">You are here</span>
                </div>
              }
            </div>
          </div>

          <div class="ctp-flow">
            <div class="ctp-flow-hdr">
              <span class="ctp-flow-caption">How this screen works</span>
              <button type="button" class="ctp-walk-btn" [class.ctp-walk-btn--on]="walking" (click)="toggleWalk()">
                {{ walking ? '⏸ Pause' : '▶ Walk the Steps' }}
              </button>
            </div>

            <div class="ctp-chart">
              @for (step of flowMap.steps; track $index; let i = $index) {
                @if (i > 0) {
                  <svg class="ctp-arrow" [class.ctp-arrow--active]="displayStep() === i || displayStep() === i - 1" width="30" height="18" viewBox="0 0 30 18" aria-hidden="true">
                    <line x1="0" y1="9" x2="20" y2="9" stroke="currentColor" stroke-width="2"/>
                    <path d="M17,3 L27,9 L17,15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                }
                <button
                  type="button"
                  class="ctp-node"
                  [class.ctp-node--active]="displayStep() === i"
                  (mouseenter)="hoverStep = i"
                  (mouseleave)="hoverStep = null"
                  (focus)="hoverStep = i"
                  (blur)="hoverStep = null"
                  (click)="setActiveStep(i)"
                >
                  <span class="ctp-node-step">{{ i + 1 }}</span>
                  <span class="ctp-node-icon">
                    @switch (step.icon) {
                      @case ('search') {
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                      }
                      @case ('open') {
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                      }
                      @case ('checklist') {
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 6h12"/><path d="M9 12h12"/><path d="M9 18h12"/><path d="M3 6l1.5 1.5L7 5"/><path d="M3 12l1.5 1.5L7 11"/><path d="M3 18l1.5 1.5L7 17"/></svg>
                      }
                      @case ('payment') {
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="1" y="5" width="22" height="14" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>
                      }
                      @case ('send') {
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                      }
                      @case ('receipt') {
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16l3-2 3 2 3-2 3 2V4a2 2 0 0 0-2-2z"/><line x1="8" y1="8" x2="14" y2="8"/><line x1="8" y1="12" x2="14" y2="12"/></svg>
                      }
                    }
                  </span>
                  <span class="ctp-node-label">{{ step.label }}</span>
                </button>
              }
            </div>

            <div class="ctp-caption">
              <span class="ctp-caption-n">{{ displayStep() + 1 }}</span>
              <span>{{ flowMap.steps[displayStep()]?.detail }}</span>
            </div>
          </div>

          <div class="ctp-ftr">
            <span>HOVER OR CLICK A STEP · WALK THE STEPS TO PLAY THROUGH THEM</span>
            <button type="button" class="ctp-switch" (click)="requestGuided()">
              Switch to Guided Tour for on-screen steps
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/>
              </svg>
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .ctp-backdrop {
      position: fixed; inset: 0; z-index: 1000;
      background: rgba(15, 23, 42, 0.42);
      display: flex; align-items: center; justify-content: center;
      padding: 20px;
    }
    @media (prefers-color-scheme: dark) {
      :host-context(html:not(.light-theme)) .ctp-backdrop { background: rgba(0, 0, 0, 0.6); }
    }
    :host-context(html.dark-theme) .ctp-backdrop { background: rgba(0, 0, 0, 0.6); }

    .ctp-panel {
      width: min(100%, 720px);
      background: var(--cms-bg-card);
      border-radius: var(--cms-radius-xl, 16px);
      box-shadow: var(--cms-shadow-xl);
      border: 1px solid var(--cms-border-default);
      overflow: hidden;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
    }

    .ctp-hdr { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px 0; flex: none; }
    .ctp-hdr-title { display: flex; align-items: center; gap: 8px; font-size: 13.5px; font-weight: 600; color: var(--cms-text-primary); font-family: var(--cms-font-ui); }
    .ctp-hdr-title svg { color: var(--cms-primary); flex: none; }
    .ctp-close {
      width: 26px; height: 26px; border-radius: 7px; border: none; background: transparent;
      color: var(--cms-text-secondary); display: flex; align-items: center; justify-content: center; cursor: pointer;
    }
    .ctp-close:hover { background: var(--cms-bg-hover); }

    /* ---- static journey rail: deliberately quiet — context, not the main event ---- */
    .ctp-rail-wrap { padding: 16px 18px 14px; border-bottom: 1px solid var(--cms-border-default); flex: none; background: var(--cms-bg-subtle, var(--cms-bg-hover)); }
    .ctp-rail-caption { font-size: 9.5px; font-weight: 700; letter-spacing: .06em; text-transform: uppercase; color: var(--cms-text-secondary); opacity: .75; margin-bottom: 9px; }
    .ctp-rail { display: flex; align-items: center; overflow-x: auto; }
    .ctp-rail-node { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: none; width: 88px; }
    .ctp-rail-dot {
      width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
      background: transparent; border: 1.5px solid var(--cms-border-default);
      color: var(--cms-text-secondary); font-size: 8.5px; font-weight: 700; font-family: var(--cms-font-mono, monospace);
    }
    .ctp-rail-node--current .ctp-rail-dot {
      border-color: var(--cms-primary); background: var(--cms-primary); color: #fff;
    }
    .ctp-rail-here { display: none; font-size: 8px; font-weight: 700; letter-spacing: .04em; text-transform: uppercase; color: var(--cms-primary); }
    .ctp-rail-node--current .ctp-rail-here { display: block; }
    .ctp-rail-label { font-size: 10px; font-weight: 500; color: var(--cms-text-secondary); text-align: center; line-height: 1.2; }
    .ctp-rail-node--current .ctp-rail-label { color: var(--cms-text-primary); font-weight: 700; }
    .ctp-rail-line { flex: 1; height: 1px; background: var(--cms-border-default); min-width: 8px; margin: 0 -2px; }

    /* ---- the flowchart: this screen's own steps — the visual centerpiece ---- */
    .ctp-flow { padding: 18px 18px 4px; overflow-y: auto; flex: 1 1 auto; min-height: 0; }
    .ctp-flow-hdr { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .ctp-flow-caption { font-size: 10px; font-weight: 700; letter-spacing: .06em; text-transform: uppercase; color: var(--cms-text-secondary); }
    .ctp-walk-btn {
      display: flex; align-items: center; gap: 5px;
      border: 1px solid var(--cms-border-default); background: var(--cms-bg-card); color: var(--cms-text-secondary);
      font-family: var(--cms-font-ui); font-size: 10.5px; font-weight: 600; padding: 5px 9px; border-radius: 7px; cursor: pointer;
    }
    .ctp-walk-btn:hover { border-color: var(--cms-primary); color: var(--cms-primary); }
    .ctp-walk-btn--on { border-color: var(--cms-primary); background: var(--cms-primary-light); color: var(--cms-primary); }

    .ctp-chart { display: flex; flex-wrap: wrap; align-items: center; gap: 0; row-gap: 16px; }
    .ctp-node {
      position: relative;
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      width: 108px; height: 112px; padding: 16px 8px 12px; border-radius: 14px; cursor: pointer;
      background: var(--cms-bg-card); border: 1px solid var(--cms-border-default);
      box-shadow: var(--cms-shadow-sm);
      font-family: var(--cms-font-ui); transition: border-color .15s, box-shadow .15s;
    }
    .ctp-node:hover, .ctp-node--active {
      border-color: var(--cms-primary); box-shadow: var(--cms-shadow-md), 0 0 0 3px var(--cms-primary-ring);
    }
    .ctp-node-step {
      position: absolute; top: -8px; right: -8px;
      width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
      background: var(--cms-bg-card); border: 1.5px solid var(--cms-border-default); color: var(--cms-text-secondary);
      font-size: 10px; font-weight: 700; font-family: var(--cms-font-mono, monospace);
      transition: border-color .15s, background .15s, color .15s;
    }
    .ctp-node:hover .ctp-node-step, .ctp-node--active .ctp-node-step { border-color: var(--cms-primary); background: var(--cms-primary); color: #fff; }
    .ctp-node-icon {
      width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
      background: var(--cms-primary-light); color: var(--cms-primary);
      transition: background .15s, color .15s;
    }
    .ctp-node:hover .ctp-node-icon, .ctp-node--active .ctp-node-icon { background: var(--cms-primary); color: #fff; }
    .ctp-node-label {
      font-size: 11px; font-weight: 600; color: var(--cms-text-primary); text-align: center; line-height: 1.3;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
      height: 28.6px; /* fixed — reserved for 2 lines always, so a 1-line label never changes the card's size */
    }
    .ctp-arrow { flex: none; color: var(--cms-border-default); margin: 0 -2px; transition: color .15s; align-self: center; }
    .ctp-arrow--active { color: var(--cms-primary); }

    .ctp-caption {
      display: flex; align-items: flex-start; gap: 10px; margin: 16px 0 4px; padding: 12px 14px;
      background: var(--cms-bg-subtle, var(--cms-bg-hover)); border-radius: 10px;
      font-size: 12.5px; line-height: 1.55; color: var(--cms-text-primary);
      min-height: 58px; /* reserves room for the longest step's detail (~3 lines) so switching steps never resizes the whole popup */
    }
    .ctp-caption-n {
      flex: none; width: 19px; height: 19px; border-radius: 50%; margin-top: 1px;
      background: var(--cms-primary-light); color: var(--cms-primary);
      font-size: 10px; font-weight: 700; font-family: var(--cms-font-mono, monospace);
      display: flex; align-items: center; justify-content: center;
    }

    .ctp-ftr {
      display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap;
      padding: 11px 18px; border-top: 1px solid var(--cms-border-default); background: var(--cms-bg-subtle, var(--cms-bg-card));
      font-size: 10px; color: var(--cms-text-secondary); flex: none;
    }
    .ctp-switch {
      border: none; background: none; color: var(--cms-primary); font-family: var(--cms-font-ui);
      font-size: 11px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 4px;
    }
    .ctp-switch:hover { color: var(--cms-primary-hover); }
  `],
})
export class CmsTourPanelComponent implements OnInit, OnDestroy {
  @Input({ required: true }) tourKey!: string;
  @Output() closed = new EventEmitter<void>();
  /** The tab/footer swap was clicked — the owner is responsible for closing this panel and starting the Guided Tour. */
  @Output() guidedRequested = new EventEmitter<void>();

  private readonly tourService = inject(TourService);

  protected hasGuided = false;
  /** The committed step — set by a click, or advanced automatically while walking. */
  protected activeStep = 0;
  /** Purely visual hover/focus preview — never touches the walk timer, so pointing at a step doesn't stop it. */
  protected hoverStep: number | null = null;
  protected walking = false;
  protected flowMap: TourFlowMap | undefined;

  private walkTimer: ReturnType<typeof setInterval> | undefined;

  ngOnInit(): void {
    this.hasGuided = this.tourService.hasTour(this.tourKey);
    this.flowMap = this.tourService.getFlowMap(this.tourKey);
  }

  ngOnDestroy(): void {
    this.stopWalk();
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.close();
  }

  protected pad(n: number): string {
    return n < 10 ? '0' + n : String(n);
  }

  /**
   * What the chart/caption should show right now. While walking, this always follows the
   * timer — a stray hover must never freeze the display on whatever card the mouse happens
   * to be resting on. Only when not walking does the hover preview take over.
   */
  protected displayStep(): number {
    return this.walking ? this.activeStep : (this.hoverStep ?? this.activeStep);
  }

  /** Explicit click — stop auto-play (if any) and commit to this step. */
  protected setActiveStep(i: number): void {
    this.stopWalk();
    this.activeStep = i;
  }

  protected toggleWalk(): void {
    if (this.walking) { this.stopWalk(); return; }
    this.walking = true;
    const total = this.flowMap?.steps.length ?? 0;
    this.walkTimer = setInterval(() => {
      this.activeStep = (this.activeStep + 1) % total;
    }, 1400);
  }

  private stopWalk(): void {
    this.walking = false;
    if (this.walkTimer) { clearInterval(this.walkTimer); this.walkTimer = undefined; }
  }

  protected requestGuided(): void {
    if (!this.hasGuided) return;
    this.stopWalk();
    this.guidedRequested.emit();
  }

  protected onBackdropClick(e: MouseEvent): void {
    if (e.target === e.currentTarget) { this.close(); }
  }

  protected close(): void {
    this.stopWalk();
    this.closed.emit();
  }
}
