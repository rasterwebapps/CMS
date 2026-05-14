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
  { id: 'electric-indigo',    name: 'Electric Indigo',    hex: '#6366f1' },
  { id: 'cosmic-cobalt',      name: 'Cosmic Cobalt',      hex: '#2563eb' },
  { id: 'aurora-sky',         name: 'Aurora Sky',         hex: '#0ea5e9' },
  { id: 'transformative-teal',name: 'Transformative Teal',hex: '#00d4c1' },
  { id: 'radiant-emerald',    name: 'Radiant Emerald',    hex: '#059669' },
  { id: 'hyper-violet',       name: 'Hyper Violet',       hex: '#7c3aed' },
  { id: 'digital-fuchsia',    name: 'Digital Fuchsia',    hex: '#d946ef' },
  { id: 'crimson-spark',      name: 'Crimson Spark',      hex: '#e11d48' },
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
  'electric-indigo': {
    50: '#eef2ff', 100: '#e0e7ff', 200: '#c7d2fe', 300: '#a5b4fc',
    400: '#818cf8', 500: '#6366f1', 600: '#4f46e5', 700: '#4338ca',
    800: '#3730a3', 900: '#312e81', 950: '#1e1b4b',
  },
  'cosmic-cobalt': {
    50: '#eff6ff', 100: '#dbeafe', 200: '#b8d1fc', 300: '#85b0f9',
    400: '#5289f5', 500: '#2563eb', 600: '#1a4fc8', 700: '#133ca0',
    800: '#0e2e7c', 900: '#0a2260', 950: '#05123a',
  },
  'aurora-sky': {
    50: '#f0f9ff', 100: '#e0f2fe', 200: '#bae6fd', 300: '#7dd3fc',
    400: '#38bdf8', 500: '#0ea5e9', 600: '#0284c7', 700: '#0369a1',
    800: '#075985', 900: '#0c4a6e', 950: '#082f49',
  },
  'transformative-teal': {
    50: '#f0fffe', 100: '#ccfff9', 200: '#99fff2', 300: '#60f5e8',
    400: '#2ae5d5', 500: '#00d4c1', 600: '#00a89a', 700: '#007c72',
    800: '#005c55', 900: '#003d39', 950: '#002220',
  },
  'radiant-emerald': {
    50: '#ecfdf5', 100: '#d1fae8', 200: '#a3f4d0', 300: '#67e5b3',
    400: '#2dcf90', 500: '#059669', 600: '#047a54', 700: '#035c3f',
    800: '#024430', 900: '#012e21', 950: '#001811',
  },
  'hyper-violet': {
    50: '#f3f1ff', 100: '#e8e4fe', 200: '#d4ccfd', 300: '#b8abfc',
    400: '#9a78f7', 500: '#7c3aed', 600: '#6424d0', 700: '#4e18a8',
    800: '#3c1284', 900: '#2d0d66', 950: '#1a0740',
  },
  'digital-fuchsia': {
    50: '#fdf4ff', 100: '#fae8ff', 200: '#f5d0fe', 300: '#f0abfc',
    400: '#e879f9', 500: '#d946ef', 600: '#c026d3', 700: '#a21caf',
    800: '#86198f', 900: '#701a75', 950: '#4a044e',
  },
  'crimson-spark': {
    50: '#fff0f2', 100: '#ffe1e5', 200: '#ffc2cc', 300: '#ff94a6',
    400: '#fc5c76', 500: '#e11d48', 600: '#be1239', 700: '#98102e',
    800: '#7a0e25', 900: '#630c1e', 950: '#380610',
  },
};

/** Angle used in all brand linear-gradients — ensures a consistent direction. */
const GRADIENT_ANGLE = 135;

/**
 * The two interactive shade pairs used when applying --cms-primary.
 *  - NORMAL pair: palette[500] as primary, palette[600] as hover.
 *  - LIGHT pair: palette[700] as primary (contrast-safe), palette[800] as hover.
 * "Light" means the 500 is too luminous for white text to meet WCAG AA.
 */
const PRIMARY_SHADE_NORMAL: { primary: Shade; hover: Shade } = { primary: 500, hover: 600 };
const PRIMARY_SHADE_LIGHT: { primary: Shade; hover: Shade } = { primary: 700, hover: 800 };

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
    const shades = this.failsWhiteTextContrast(palette[500])
      ? PRIMARY_SHADE_LIGHT
      : PRIMARY_SHADE_NORMAL;
    const primaryHex = palette[shades.primary];

    const [r, g, b] = this.hexToRgb(primaryHex);
    const rgba = (alpha: number): string => `rgba(${r}, ${g}, ${b}, ${alpha})`;

    // Core brand tokens
    root.style.setProperty('--cms-primary', primaryHex);
    // Foreground colour safe for text/icons placed ON a --primary-theme surface.
    // Black when the 500-shade is too bright for white text (WCAG AA).
    root.style.setProperty('--primary-fg', this.failsWhiteTextContrast(palette[500]) ? '#000000' : '#ffffff');
    // Update --cms-primary-rgb so rgba() usages in SCSS work correctly
    root.style.setProperty('--cms-primary-rgb', `${r}, ${g}, ${b}`);
    root.style.setProperty('--cms-primary-hover', palette[shades.hover]);
    root.style.setProperty('--cms-primary-light', palette[50]);
    root.style.setProperty('--cms-border-hover', palette[300]);
    root.style.setProperty('--cms-sidenav-active-text', primaryHex);

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
      `linear-gradient(${GRADIENT_ANGLE}deg, ${palette[400]} 0%, ${palette[700]} 100%)`,
    );
    root.style.setProperty(
      '--cms-gradient-brand-hover',
      `linear-gradient(${GRADIENT_ANGLE}deg, ${palette[600]} 0%, ${palette[800]} 100%)`,
    );
    // Toolbar gradient: dark → medium so the header has natural depth, not a
    // faded candy-stripe (previously was 500→300 which looked washed-out).
    root.style.setProperty(
      '--cms-gradient-toolbar',
      `linear-gradient(${GRADIENT_ANGLE}deg, ${palette[700]} 0%, ${palette[500]} 100%)`,
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
