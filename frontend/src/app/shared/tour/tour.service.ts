import { Injectable } from '@angular/core';
import { driver, DriveStep, Config } from 'driver.js';

export interface TourDefinition {
  steps: DriveStep[];
  config?: Partial<Config>;
}

/** One other screen in the same journey, shown for context in the flow-map rail. */
export interface FlowMapFunnelStage {
  label: string;
  /** One-line summary shown on hover — not a click target, the rail itself is static. */
  description: string;
}

/** Icon shown on a flowchart block — keep this set small and reusable across screens. */
export type FlowMapIcon = 'search' | 'open' | 'checklist' | 'payment' | 'send' | 'receipt';

/** One step of the work actually done on this screen, shown as a flowchart block. */
export interface FlowMapStep {
  label: string;
  detail: string;
  icon: FlowMapIcon;
}

export interface TourFlowMap {
  /** The multi-screen journey this screen belongs to, for context only — not itself navigable. */
  funnel: FlowMapFunnelStage[];
  /** Index of the current screen within `funnel`. */
  currentIndex: number;
  /** The steps performed on this screen itself, rendered as a flowchart. */
  steps: FlowMapStep[];
}

const SEEN_PREFIX = 'cms-tour-seen:';

@Injectable({ providedIn: 'root' })
export class TourService {

  private readonly tours = new Map<string, TourDefinition>();
  private readonly flowMaps = new Map<string, TourFlowMap>();

  register(key: string, def: TourDefinition): void {
    this.tours.set(key, def);
  }

  /** Registers a Flow Map view for this tour key, shown as a second tab alongside the Guided Tour. */
  registerFlowMap(key: string, def: TourFlowMap): void {
    this.flowMaps.set(key, def);
  }

  hasFlowMap(key: string): boolean {
    return this.flowMaps.has(key);
  }

  getFlowMap(key: string): TourFlowMap | undefined {
    return this.flowMaps.get(key);
  }

  start(key: string): void {
    const def = this.tours.get(key);
    if (!def) return;

    const d = driver({
      showProgress: true,
      animate: true,
      smoothScroll: true,
      allowClose: true,
      overlayOpacity: 0.4,
      stagePadding: 6,
      stageRadius: 10,
      popoverClass: 'cms-tour-popover',
      progressText: '{{current}} of {{total}}',
      nextBtnText: 'Next →',
      prevBtnText: '← Back',
      doneBtnText: 'Done',
      onDestroyStarted: (el, step, { driver: drv }) => {
        drv.destroy();
        this.markSeen(key);
      },
      ...def.config,
      steps: def.steps,
    });

    d.drive();
  }

  hasSeen(key: string): boolean {
    try {
      return localStorage.getItem(SEEN_PREFIX + key) === '1';
    } catch {
      return false;
    }
  }

  markSeen(key: string): void {
    try {
      localStorage.setItem(SEEN_PREFIX + key, '1');
    } catch { /* ignore */ }
  }

  reset(key: string): void {
    try {
      localStorage.removeItem(SEEN_PREFIX + key);
    } catch { /* ignore */ }
  }
}
