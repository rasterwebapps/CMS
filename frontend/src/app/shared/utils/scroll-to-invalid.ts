import { AbstractControl } from '@angular/forms';

/**
 * Marks all controls as touched (to trigger validation messages) then scrolls
 * the viewport to the first visible invalid input/select/textarea and focuses it.
 * Call this instead of form.markAllAsTouched() on every submit guard.
 */
export function scrollToFirstInvalid(form: AbstractControl): void {
  form.markAllAsTouched();

  // Let Angular flush the DOM updates before querying ng-invalid
  setTimeout(() => {
    const el = document.querySelector<HTMLElement>(
      'input.ng-invalid:not([type="hidden"]), select.ng-invalid, textarea.ng-invalid',
    );
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.focus({ preventScroll: true });
    }
  }, 0);
}
