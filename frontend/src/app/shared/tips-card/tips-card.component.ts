import { Component, Input, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/** A single contextual hint shown inside a {@link CmsTipsCardComponent}. */
export interface CmsTip {
  /** Inline SVG markup for the icon (e.g. a 12×12 stroke icon). */
  iconSvg: string;
  /** Bold one-line heading for the tip. */
  title: string;
  /** Short supporting sentence. */
  subtitle: string;
}

/**
 * Right-rail card that lists short contextual tips next to entry forms.
 *
 * Tip `iconSvg` is bypassed through DomSanitizer so the SVG markup is
 * preserved. Only static, component-controlled SVG strings should ever
 * be supplied — never user input.
 *
 * Usage:
 *   <cms-tips-card [tips]="tips" />
 */
@Component({
  selector: 'cms-tips-card',
  standalone: true,
  templateUrl: './tips-card.component.html',
  styleUrl: './tips-card.component.scss',
})
export class CmsTipsCardComponent {
  private readonly sanitizer = inject(DomSanitizer);

  @Input() heading = 'Tips';

  private _tips: CmsTip[] = [];
  protected safeTips: { safeIcon: SafeHtml; title: string; subtitle: string }[] = [];

  @Input()
  set tips(value: CmsTip[]) {
    this._tips = value ?? [];
    this.safeTips = this._tips.map((t) => ({
      safeIcon: this.sanitizer.bypassSecurityTrustHtml(t.iconSvg),
      title: t.title,
      subtitle: t.subtitle,
    }));
  }
  get tips(): CmsTip[] {
    return this._tips;
  }
}

