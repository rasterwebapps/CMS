import {
  Directive,
  ElementRef,
  HostBinding,
  Input,
  Renderer2,
  inject,
} from '@angular/core';

/**
 * Toggles the standard `.is-loading` modifier and `disabled` attribute on a
 * submit button while an async operation is in flight. Pairs with the
 * `.btn-submit.is-loading` styling defined in `styles.scss`.
 *
 * @example
 * ```html
 * <button class="btn-submit" [appLoadingButton]="saving()">Save</button>
 * ```
 */
@Directive({
  selector: '[appLoadingButton]',
  standalone: true,
})
export class LoadingButtonDirective {
  private readonly el = inject<ElementRef<HTMLButtonElement>>(ElementRef);
  private readonly renderer = inject(Renderer2);

  private _loading = false;

  /** When `true`, the button shows a spinner and is non-interactive. */
  @Input({ alias: 'appLoadingButton' })
  set loading(value: boolean) {
    this._loading = !!value;
    const node = this.el.nativeElement;
    if (this._loading) {
      this.renderer.setAttribute(node, 'disabled', 'true');
      this.renderer.setAttribute(node, 'aria-busy', 'true');
    } else {
      this.renderer.removeAttribute(node, 'disabled');
      this.renderer.removeAttribute(node, 'aria-busy');
    }
  }
  get loading(): boolean {
    return this._loading;
  }

  @HostBinding('class.is-loading')
  get isLoadingClass(): boolean {
    return this._loading;
  }
}
