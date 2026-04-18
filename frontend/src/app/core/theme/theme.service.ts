import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export interface ColorSwatch {
  id: string;
  name: string;
  /** The 500-equivalent hex value shown in the picker */
  hex: string;
}

/** All pre-approved base-500 swatches users can pick from */
export const COLOR_SWATCHES: ColorSwatch[] = [
  { id: 'indigo', name: 'Indigo', hex: '#6366f1' },
  { id: 'violet', name: 'Violet', hex: '#8b5cf6' },
  { id: 'purple', name: 'Purple', hex: '#a855f7' },
  { id: 'blue', name: 'Blue', hex: '#3b82f6' },
  { id: 'sky', name: 'Sky', hex: '#0ea5e9' },
  { id: 'cyan', name: 'Cyan', hex: '#06b6d4' },
  { id: 'teal', name: 'Teal', hex: '#14b8a6' },
  { id: 'emerald', name: 'Emerald', hex: '#10b981' },
  { id: 'rose', name: 'Rose', hex: '#f43f5e' },
  { id: 'pink', name: 'Pink', hex: '#ec4899' },
  { id: 'orange', name: 'Orange', hex: '#f97316' },
  { id: 'amber', name: 'Amber', hex: '#f59e0b' },
];

const DEFAULT_SWATCH = COLOR_SWATCHES[0];
const STORAGE_KEY = 'cms_primary_theme';

/** Shade numbers for the primary palette */
const SHADES = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950] as const;
type Shade = (typeof SHADES)[number];

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);

  private readonly _activeSwatch = signal<ColorSwatch>(DEFAULT_SWATCH);
  readonly activeSwatch = this._activeSwatch.asReadonly();

  /** Called once on app startup to restore the persisted theme choice. */
  init(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const saved = this.loadPersistedSwatch();
    this.applyTheme(saved ?? DEFAULT_SWATCH);
  }

  /** Apply the given swatch, inject CSS variables, and persist the choice. */
  applyTheme(swatch: ColorSwatch): void {
    this._activeSwatch.set(swatch);
    const palette = this.generatePalette(swatch.hex);
    this.injectPaletteVariables(palette);
    if (isPlatformBrowser(this.platformId)) {
      try {
        localStorage.setItem(STORAGE_KEY, swatch.id);
      } catch {
        // Ignore storage errors
      }
    }
  }

  /**
   * Generate a full 50–950 palette from a base color (treated as shade 500).
   * Uses HSL interpolation:
   *   - Lighter shades: interpolate towards a desaturated near-white.
   *   - Darker shades: interpolate towards a desaturated near-black.
   */
  generatePalette(baseHex: string): Record<Shade, string> {
    const [r, g, b] = this.hexToRgb(baseHex);
    const [h, s, l] = this.rgbToHsl(r, g, b);

    // Interpolation endpoints:
    //   LIGHT — very desaturated near-white (shade 50 target)
    //     saturation capped at 25% so the palest tints don't look washed-out
    //   DARK  — slightly desaturated near-black (shade 950 target)
    //     lightness 15% keeps the darkest shade legible on a dark background
    const LIGHT_S = Math.min(s * 0.25, 0.25);
    const LIGHT_L = 0.97;  // ~97% → almost white for shade 50
    const DARK_S = s * 0.85; // reduce saturation slightly so darks look natural
    const DARK_L = 0.15;   // ~15% → near-black for shade 950

    // SHADES array has 11 entries; shade 500 is at index 5 (the midpoint).
    // Lighter shades (indices 0–4) interpolate toward LIGHT.
    // Darker shades (indices 6–10) interpolate toward DARK.
    const BASE_POS = 5;
    const MAX_POS = 10;

    const palette = {} as Record<Shade, string>;

    SHADES.forEach((shade, idx) => {
      let outS: number;
      let outL: number;

      if (idx <= BASE_POS) {
        const t = idx / BASE_POS;
        outS = LIGHT_S + (s - LIGHT_S) * t;
        outL = LIGHT_L + (l - LIGHT_L) * t;
      } else {
        const t = (idx - BASE_POS) / (MAX_POS - BASE_POS);
        outS = s + (DARK_S - s) * t;
        outL = l + (DARK_L - l) * t;
      }

      outS = Math.max(0, Math.min(1, outS));
      outL = Math.max(0, Math.min(1, outL));
      palette[shade] = this.hslToHex(h, outS, outL);
    });

    return palette;
  }

  // ─── Colour Math Utilities ────────────────────────────────────────────────

  private hexToRgb(hex: string): [number, number, number] {
    const n = parseInt(hex.replace('#', ''), 16);
    return [(n >> 16) & 0xff, (n >> 8) & 0xff, n & 0xff];
  }

  /** Returns [hue(0–360), saturation(0–1), lightness(0–1)] */
  private rgbToHsl(r: number, g: number, b: number): [number, number, number] {
    const rn = r / 255;
    const gn = g / 255;
    const bn = b / 255;
    const max = Math.max(rn, gn, bn);
    const min = Math.min(rn, gn, bn);
    const l = (max + min) / 2;

    if (max === min) {
      return [0, 0, l];
    }

    const d = max - min;
    const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    let h = 0;
    if (max === rn) {
      h = ((gn - bn) / d + (gn < bn ? 6 : 0)) / 6;
    } else if (max === gn) {
      h = ((bn - rn) / d + 2) / 6;
    } else {
      h = ((rn - gn) / d + 4) / 6;
    }

    return [h * 360, s, l];
  }

  private hslToHex(h: number, s: number, l: number): string {
    const hNorm = h / 360;
    const hue2rgb = (p: number, q: number, t: number): number => {
      let tt = t;
      if (tt < 0) tt += 1;
      if (tt > 1) tt -= 1;
      if (tt < 1 / 6) return p + (q - p) * 6 * tt;
      if (tt < 1 / 2) return q;
      if (tt < 2 / 3) return p + (q - p) * (2 / 3 - tt) * 6;
      return p;
    };
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
    const p = 2 * l - q;
    const r = Math.round(hue2rgb(p, q, hNorm + 1 / 3) * 255);
    const g = Math.round(hue2rgb(p, q, hNorm) * 255);
    const b = Math.round(hue2rgb(p, q, hNorm - 1 / 3) * 255);
    return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
  }

  // ─── CSS Variable Injection ────────────────────────────────────────────────

  private injectPaletteVariables(palette: Record<Shade, string>): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const root = document.documentElement;
    (Object.entries(palette) as [string, string][]).forEach(([shade, hex]) => {
      root.style.setProperty(`--color-primary-${shade}`, hex);
    });
  }

  // ─── Persistence ─────────────────────────────────────────────────────────

  private loadPersistedSwatch(): ColorSwatch | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    try {
      const id = localStorage.getItem(STORAGE_KEY);
      if (id) {
        return COLOR_SWATCHES.find((s) => s.id === id) ?? null;
      }
    } catch {
      // Ignore storage errors
    }
    return null;
  }
}
