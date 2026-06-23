import { AbstractControl, AsyncValidatorFn, ValidationErrors } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Observable, of, timer } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

/**
 * Async validator that checks uniqueness by calling a backend exists endpoint.
 *
 * The endpoint must accept:
 *   GET <checkUrl>?value=<fieldValue>[&excludeId=<id>][&<extraParams>]
 * and return a plain JSON boolean (true = taken, false = available).
 *
 * Usage (in ngOnInit, after the component knows its own ID for edit mode):
 *   this.form.get('name')?.setAsyncValidators(
 *     uniqueFieldValidator(this.http, `${env.apiUrl}/academic-years/name-exists`, () => this.entityId)
 *   );
 *
 * For uniqueness scoped to a parent entity (e.g. unique per institution),
 * pass getExtraParams returning the scope params. If it returns null, the
 * check is skipped until the scope value is available (e.g. before the
 * parent selector has a value):
 *   uniqueFieldValidator(this.http, url, () => this.itemId,
 *     () => this.form.value.institutionId ? { institutionId: this.form.value.institutionId } : null)
 *
 * The validator debounces 500 ms and returns { duplicate: true } when taken.
 */
export function uniqueFieldValidator(
  http: HttpClient,
  checkUrl: string,
  getExcludeId: () => number | null = () => null,
  getExtraParams: () => Record<string, string | number> | null = () => ({}),
): AsyncValidatorFn {
  return (control: AbstractControl): Observable<ValidationErrors | null> => {
    const value = (control.value ?? '').trim();
    if (!value || value.length < 1) return of(null);

    const extraParams = getExtraParams();
    if (extraParams === null) return of(null);

    return timer(500).pipe(
      switchMap(() => {
        const excludeId = getExcludeId();
        let url = `${checkUrl}?value=${encodeURIComponent(value)}`;
        if (excludeId != null) url += `&excludeId=${excludeId}`;
        for (const [key, val] of Object.entries(extraParams)) {
          url += `&${key}=${encodeURIComponent(val)}`;
        }
        return http.get<boolean>(url).pipe(
          map(exists => (exists ? { duplicate: true } : null)),
          catchError(() => of(null)),
        );
      }),
    );
  };
}
