/** Minimal shape this component needs from an option — every master-data model in this app
 *  (Program, Course, AcademicYear, ReferralType, Agent, ...) already has both fields, so no
 *  adapter is needed; extra fields on the real model are simply ignored. */
export interface InfiniteSelectOption {
  id: number | string;
  name: string;
}

/** Structurally matches every feature module's own local `Page<T>` (they're independently
 *  declared per module but identically shaped) — duck-typed on purpose so this component never
 *  has to import a specific feature's Page type. */
export interface InfiniteSelectPage<T extends InfiniteSelectOption = InfiniteSelectOption> {
  content: T[];
  totalElements: number;
}

export type InfiniteSelectValue = number | string;

/** A page-fetcher the host screen wires to its own service's `getPage()` — e.g.
 *  `(search, page, size) => this.agentService.getPage({ search, page, size }).pipe(map(p => p as InfiniteSelectPage))`. */
export type InfiniteSelectFetchPage = (
  search: string,
  page: number,
  size: number,
) => import('rxjs').Observable<InfiniteSelectPage>;

/** Resolves the display name for an already-selected value the option list hasn't loaded yet
 *  (e.g. a filter restored from the URL before the dropdown was ever opened). Omit it when the
 *  selected value already IS the display name (valueKey: 'name') — the component falls back to
 *  showing the raw value in that case. */
export type InfiniteSelectResolveLabel = (value: InfiniteSelectValue) => import('rxjs').Observable<string>;
