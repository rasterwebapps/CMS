import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export interface ColorSwatch {
  id: string;
  name: string;
  /** The 500-equivalent hex value shown in the picker */
  hex: string;
}

/**
 * Curated set of swatches — warm "warning" colours (orange, amber) are
 * intentionally excluded because they create cognitive dissonance when used
 * as a primary navigation/action colour (users associate them with alerts).
 */
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
];

const DEFAULT_SWATCH = COLOR_SWATCHES[0];
const STORAGE_KEY = 'cms_primary_theme';

/** Shade numbers for the primary palette */
const SHADES = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950] as const;
type Shade = (typeof SHADES)[number];

/**
 * Hand-crafted Tailwind v3 palette values for each approved swatch.
 * These are perceptually tuned and produce visually even ramps for all hues —
 * unlike HSL interpolation which looks "muddy" on warm/light hues.
 */
const TAILWIND_PALETTES: Record<string, Record<Shade, string>> = {
  indigo: {
    50: '#eef2ff', 100: '#e0e7ff', 200: '#c7d2fe', 300: '#a5b4fc',
    400: '#818cf8', 500: '#6366f1', 600: '#4f46e5', 700: '#4338ca',
    800: '#3730a3', 900: '#312e81', 950: '#1e1b4b',
  },
  violet: {
    50: '#f5f3ff', 100: '#ede9fe', 200: '#ddd6fe', 300: '#c4b5fd',
    400: '#a78bfa', 500: '#8b5cf6', 600: '#7c3aed', 700: '#6d28d9',
    800: '#5b21b6', 900: '#4c1d95', 950: '#2e1065',
  },
  purple: {
    50: '#faf5ff', 100: '#f3e8ff', 200: '#e9d5ff', 300: '#d8b4fe',
    400: '#c084fc', 500: '#a855f7', 600: '#9333ea', 700: '#7e22ce',
    800: '#6b21a8', 900: '#581c87', 950: '#3b0764',
  },
  blue: {
    50: '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe', 300: '#93c5fd',
    400: '#60a5fa', 500: '#3b82f6', 600: '#2563eb', 700: '#1d4ed8',
    800: '#1e40af', 900: '#1e3a8a', 950: '#172554',
  },
  sky: {
    50: '#f0f9ff', 100: '#e0f2fe', 200: '#bae6fd', 300: '#7dd3fc',
    400: '#38bdf8', 500: '#0ea5e9', 600: '#0284c7', 700: '#0369a1',
    800: '#075985', 900: '#0c4a6e', 950: '#082f49',
  },
  cyan: {
    50: '#ecfeff', 100: '#cffafe', 200: '#a5f3fc', 300: '#67e8f9',
    400: '#22d3ee', 500: '#06b6d4', 600: '#0891b2', 700: '#0e7490',
    800: '#155e75', 900: '#164e63', 950: '#083344',
  },
  teal: {
    50: '#f0fdfa', 100: '#ccfbf1', 200: '#99f6e4', 300: '#5eead4',
    400: '#2dd4bf', 500: '#14b8a6', 600: '#0d9488', 700: '#0f766e',
    800: '#115e59', 900: '#134e4a', 950: '#042f2e',
  },
  emerald: {
    50: '#ecfdf5', 100: '#d1fae5', 200: '#a7f3d0', 300: '#6ee7b7',
    400: '#34d399', 500: '#10b981', 600: '#059669', 700: '#047857',
    800: '#065f46', 900: '#064e3b', 950: '#022c22',
  },
  rose: {
    50: '#fff1f2', 100: '#ffe4e6', 200: '#fecdd3', 300: '#fda4af',
    400: '#fb7185', 500: '#f43f5e', 600: '#e11d48', 700: '#be123c',
    800: '#9f1239', 900: '#881337', 950: '#4c0519',
  },
  pink: {
    50: '#fdf2f8', 100: '#fce7f3', 200: '#fbcfe8', 300: '#f9a8d4',
    400: '#f472b6', 500: '#ec4899', 600: '#db2777', 700: '#be185d',
    800: '#9d174d', 900: '#831843', 950: '#500724',
  },
};

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
    const palette = this.generatePalette(swatch);
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
   * Returns a full 50–950 palette for the given swatch.
   * Uses the hand-crafted Tailwind v3 palette when the swatch ID is recognised
   * (gives perceptually even ramps), otherwise falls back to HSL interpolation.
   */
  generatePalette(swatch: ColorSwatch): Record<Shade, string> {
    const tailwind = TAILWIND_PALETTES[swatch.id];
    if (tailwind) {
      return tailwind;
    }
    // Fallback: HSL interpolation for any future custom swatches
    return this.generatePaletteFromHex(swatch.hex);
  }

  /**
   * Generate a full 50–950 palette from a base hex (treated as shade 500).
   * Uses HSL interpolation — kept as a fallback for custom / unknown swatches.
   *   Lighter shades: interpolate towards a desaturated near-white.
   *   Darker shades: interpolate towards a desaturated near-black.
   */
  private generatePaletteFromHex(baseHex: string): Record<Shade, string> {
    const [r, g, b] = this.hexToRgb(baseHex);
    const [h, s, l] = this.rgbToHsl(r, g, b);

    const LIGHT_S = Math.min(s * 0.25, 0.25);
    const LIGHT_L = 0.97;
    const DARK_S = s * 0.85;
    const DARK_L = 0.15;

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

  /**
   * Injects the full palette as `--color-primary-{shade}` variables AND
   * derives all semantic brand tokens (--cms-primary, gradients, alpha helpers)
   * so the entire design system responds to the chosen primary colour.
   *
   * Contrast safety: some palette[500] colours (e.g. teal, emerald, sky, cyan)
   * are too light for white text on a coloured background (fails WCAG AA).
   * For those we use palette[700] as the interactive --cms-primary so text/icons
   * always remain readable.
   *
   * ThemeService sets these as inline styles on <html>, which win over any
   * class-based CSS-variable rules in the stylesheets.
   */
  private injectPaletteVariables(palette: Record<Shade, string>): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const root = document.documentElement;

    // Raw palette shades (used by Tailwind utilities and component SCSS)
    (Object.entries(palette) as [string, string][]).forEach(([shade, hex]) => {
      root.style.setProperty(`--color-primary-${shade}`, hex);
    });

    // ─── Contrast-safe primary ────────────────────────────────────────────
    // Use palette[500] unless it is too light for WCAG AA white-text contrast,
    // in which case shift to palette[700].
    const primaryShade: Shade = this.failsWhiteTextContrast(palette[500]) ? 700 : 500;
    const primaryHoverShade: Shade = primaryShade === 500 ? 600 : 800;
    const primaryHex = palette[primaryShade];

    const [r, g, b] = this.hexToRgb(primaryHex);
    const rgba = (alpha: number): string => `rgba(${r}, ${g}, ${b}, ${alpha})`;

    // Core brand tokens
    root.style.setProperty('--cms-primary', primaryHex);
    root.style.setProperty('--cms-primary-hover', palette[primaryHoverShade]);
    root.style.setProperty('--cms-primary-light', palette[50]);
    root.style.setProperty('--cms-border-hover', palette[300]);
    root.style.setProperty('--cms-sidenav-active-text', palette[300]);

    // Opacity variants used for rings, hover backgrounds, shadows
    root.style.setProperty('--cms-primary-ring', rgba(0.3));
    root.style.setProperty('--cms-bg-hover', rgba(0.04));
    root.style.setProperty('--cms-sidenav-active-bg', rgba(0.15));

    // Composed shadow token (full box-shadow string)
    root.style.setProperty('--cms-shadow-colored', `0 4px 14px -3px ${rgba(0.25)}`);

    // Standalone alpha colour values for use inside gradient() and box-shadow()
    root.style.setProperty('--cms-primary-alpha-3', rgba(0.03));
    root.style.setProperty('--cms-primary-alpha-5', rgba(0.05));
    root.style.setProperty('--cms-primary-alpha-8', rgba(0.08));
    root.style.setProperty('--cms-primary-alpha-15', rgba(0.15));
    root.style.setProperty('--cms-primary-alpha-18', rgba(0.18));
    root.style.setProperty('--cms-primary-alpha-20', rgba(0.20));
    root.style.setProperty('--cms-primary-alpha-25', rgba(0.25));
    root.style.setProperty('--cms-primary-alpha-30', rgba(0.30));
    root.style.setProperty('--cms-primary-alpha-35', rgba(0.35));
    root.style.setProperty('--cms-primary-alpha-40', rgba(0.40));
    root.style.setProperty('--cms-primary-alpha-50', rgba(0.50));
    root.style.setProperty('--cms-primary-alpha-55', rgba(0.55));

    // ─── Gradient tokens ─────────────────────────────────────────────────
    // Brand gradient: dark → medium (gives depth; avoids "faded" look)
    root.style.setProperty(
      '--cms-gradient-brand',
      `linear-gradient(135deg, ${palette[400]} 0%, ${palette[700]} 100%)`,
    );
    root.style.setProperty(
      '--cms-gradient-brand-hover',
      `linear-gradient(135deg, ${palette[600]} 0%, ${palette[800]} 100%)`,
    );
    // Toolbar gradient: dark → medium so the header has natural depth, not a
    // faded candy-stripe (previously was 500→300 which looked washed-out).
    root.style.setProperty(
      '--cms-gradient-toolbar',
      `linear-gradient(135deg, ${palette[700]} 0%, ${palette[500]} 100%)`,
    );
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

  /**
   * Returns true when white text on this colour fails WCAG AA contrast (4.5:1).
   * Formula: white luminance = 1.0; contrast = (1 + 0.05) / (L + 0.05).
   * Fails when L > 0.183 (derived from 1.05 / 4.5 − 0.05).
   */
  private failsWhiteTextContrast(hex: string): boolean {
    const [r, g, b] = this.hexToRgb(hex);
    const toLinear = (c: number): number => {
      const n = c / 255;
      return n <= 0.03928 ? n / 12.92 : Math.pow((n + 0.055) / 1.055, 2.4);
    };
    const L = 0.2126 * toLinear(r) + 0.7152 * toLinear(g) + 0.0722 * toLinear(b);
    return L > 0.183;
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
