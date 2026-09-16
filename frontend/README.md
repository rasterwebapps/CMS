# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.7.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

Convention: co-locate `*.component.spec.ts`/`*.service.spec.ts` next to the file they cover.
Mock injected services directly (`{ provide: FooService, useValue: { method: vi.fn(...) } }`)
rather than going through `HttpClientTestingModule` for a component test — reserve
`provideHttpClient()` + `HttpTestingController` for testing a service itself, where the actual
request shape (URL, method, params) is what's under test. A component's `protected` members
(state signals, handler methods) are intentionally not part of its public API just for tests —
either drive them through the DOM (`fixture.debugElement.query(By.css(...))`,
`el.triggerEventHandler(...)`) or, where that reads worse than it's worth, cast
`component as unknown as { ... }` for the specific internals the test needs. Any route the
template's `routerLink` touches needs `provideRouter([])` in the test's providers even when no
navigation actually happens, or component creation throws `NG0201: No provider for
ActivatedRoute`.

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
