import { Injectable } from '@angular/core';
import { driver, DriveStep, Config } from 'driver.js';

export interface TourDefinition {
  steps: DriveStep[];
  config?: Partial<Config>;
}

const SEEN_PREFIX = 'cms-tour-seen:';

@Injectable({ providedIn: 'root' })
export class TourService {

  private readonly tours = new Map<string, TourDefinition>();

  register(key: string, def: TourDefinition): void {
    this.tours.set(key, def);
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
