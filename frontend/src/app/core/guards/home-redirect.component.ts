import { Component } from '@angular/core';

/** Route target for the `''`/`'**'` entries in app.routes.ts. Never actually renders in
 *  practice -- `homeRedirectGuard` always returns a UrlTree, so the router redirects away
 *  before this ever mounts. Exists only because Angular requires a route to have a component
 *  (or `redirectTo`, which cannot be combined with `canActivate` -- NG04014) even when a guard
 *  is guaranteed to redirect every time. */
@Component({
  selector: 'app-home-redirect',
  standalone: true,
  template: '',
})
export class HomeRedirectComponent {}
