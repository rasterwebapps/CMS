import { of } from 'rxjs';
import { InfiniteSelectFetchPage, InfiniteSelectOption } from './infinite-select.model';

/**
 * Wraps a small, already-fully-known option list as a one-page `fetchPage` for
 * cms-infinite-select — for filters whose values are a fixed enum, or an array the host
 * screen already loaded in full (e.g. a signal populated once from a small master list).
 * No backend paging needed, but cms-infinite-select is still the right fit over a native
 * `<select>`: it keeps the filter visually/behaviourally uniform with every other filter in
 * the toolbar (same pill, same panel, same outside-click-closes-siblings handling) instead of
 * mixing in browser-native `<select>` chrome that can't be restyled.
 *
 * Pass a function (not the array itself) so a live signal/array is read fresh on every open
 * rather than frozen at construction time.
 */
export function staticOptionsFetchPage(getOptions: () => InfiniteSelectOption[]): InfiniteSelectFetchPage {
  return () => {
    const content = getOptions();
    return of({ content, totalElements: content.length });
  };
}
