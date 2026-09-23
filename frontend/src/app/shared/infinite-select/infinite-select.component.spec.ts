import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { CmsInfiniteSelectComponent } from './infinite-select.component';
import { InfiniteSelectOption, InfiniteSelectPage } from './infinite-select.model';

// Targets the exact bug this component replaces (Fee Explorer / enquiry-list Referral Type &
// Agent filters): a dropdown's options and totals silently scoped to whatever page/bound-list
// happened to be loaded, instead of spanning the real backend dataset. These tests drive the
// component's own scroll/search/select mechanics directly rather than through a host screen.
describe('CmsInfiniteSelectComponent', () => {
  let fixture: ComponentFixture<CmsInfiniteSelectComponent>;
  let component: CmsInfiniteSelectComponent;
  let fetchPage: ReturnType<typeof vi.fn>;

  const page = (items: InfiniteSelectOption[], total: number): InfiniteSelectPage => ({ content: items, totalElements: total });

  beforeEach(async () => {
    vi.useFakeTimers();
    fetchPage = vi.fn(() => of(page([{ id: 1, name: 'Alpha' }, { id: 2, name: 'Beta' }], 25)));

    await TestBed.configureTestingModule({
      imports: [CmsInfiniteSelectComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CmsInfiniteSelectComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('label', 'All Agents');
    fixture.componentRef.setInput('ariaLabel', 'Filter by agent');
    fixture.componentRef.setInput('fetchPage', fetchPage);
    fixture.detectChanges();
  });

  const internal = () => component as unknown as {
    open: { (): boolean };
    options: { (): InfiniteSelectOption[] };
    loading: { (): boolean };
    hasMore: { (): boolean };
    selectedLabel: { (): string };
    disabled: boolean;
    toggleOpen(): void;
    onSearchInput(term: string): void;
    onScroll(event: Event): void;
    selectSingle(option: InfiniteSelectOption): void;
    selectAll(): void;
    toggleMulti(option: InfiniteSelectOption): void;
  };

  it('does not fetch anything until first opened (matches a native select — no eager full load)', () => {
    expect(fetchPage).not.toHaveBeenCalled();
  });

  it('fetches page 0 with an empty search term the first time it is opened', () => {
    internal().toggleOpen();
    expect(fetchPage).toHaveBeenCalledWith('', 0, 10);
    expect(internal().options()).toEqual([{ id: 1, name: 'Alpha' }, { id: 2, name: 'Beta' }]);
  });

  it('scrolling near the bottom of the panel with more pages available fetches and appends the next page', () => {
    internal().toggleOpen(); // page 0: 2 of 25
    fetchPage.mockReturnValue(of(page([{ id: 3, name: 'Gamma' }], 25)));

    const scrollEvent = {
      target: { scrollTop: 90, clientHeight: 20, scrollHeight: 100 }, // 110 >= 80% of 100
    } as unknown as Event;
    internal().onScroll(scrollEvent);

    expect(fetchPage).toHaveBeenLastCalledWith('', 1, 10);
    expect(internal().options()).toEqual([
      { id: 1, name: 'Alpha' }, { id: 2, name: 'Beta' }, { id: 3, name: 'Gamma' },
    ]);
  });

  it('stops requesting further pages once totalElements is exhausted', () => {
    fetchPage.mockReturnValue(of(page([{ id: 1, name: 'Alpha' }], 1))); // only 1 of 1 total
    internal().toggleOpen();
    expect(internal().hasMore()).toBe(false);

    const scrollEvent = { target: { scrollTop: 90, clientHeight: 20, scrollHeight: 100 } } as unknown as Event;
    internal().onScroll(scrollEvent);
    expect(fetchPage).toHaveBeenCalledTimes(1); // the scroll did not trigger a second call
  });

  it('debounces typed search input, then resets to page 0 with the new term', () => {
    internal().toggleOpen();
    fetchPage.mockClear();
    fetchPage.mockReturnValue(of(page([{ id: 9, name: 'Zeta' }], 1)));

    internal().onSearchInput('ze');
    expect(fetchPage).not.toHaveBeenCalled(); // still within the debounce window

    vi.advanceTimersByTime(300);
    expect(fetchPage).toHaveBeenCalledWith('ze', 0, 10);
    expect(internal().options()).toEqual([{ id: 9, name: 'Zeta' }]); // page 0 replaces, not appends
  });

  it('single-select: clicking an option emits its id, shows its name, and closes the panel', () => {
    const emitted: (string | number | null)[] = [];
    component.selectedValueChange.subscribe(v => emitted.push(v));
    internal().toggleOpen();

    internal().selectSingle({ id: 2, name: 'Beta' });

    expect(emitted).toEqual([2]);
    expect(internal().selectedLabel()).toBe('Beta');
    expect(internal().open()).toBe(false);
  });

  it('single-select: clicking the pinned "All" row clears the selection', () => {
    const emitted: (string | number | null)[] = [];
    component.selectedValueChange.subscribe(v => emitted.push(v));

    internal().selectAll();

    expect(emitted).toEqual([null]);
    expect(internal().selectedLabel()).toBe('');
  });

  it('name-valued single-select (valueKey: "name") emits the name string, not an id', () => {
    fixture.componentRef.setInput('valueKey', 'name');
    let emitted: unknown;
    component.selectedValueChange.subscribe(v => (emitted = v));

    internal().selectSingle({ id: 7, name: 'Field Agent Co.' });

    expect(emitted).toBe('Field Agent Co.');
  });

  it('multi-select: toggling an option emits an updated Set without closing the panel', () => {
    fixture.componentRef.setInput('multiple', true);
    let emitted: Set<string | number> | undefined;
    component.selectedValuesChange.subscribe(v => (emitted = v));
    internal().toggleOpen();

    internal().toggleMulti({ id: 1, name: 'Alpha' });

    expect(emitted).toEqual(new Set([1]));
    expect(internal().open()).toBe(true); // multi-select stays open for further picks
  });

  it('resolves the label of an already-selected value that has not been loaded into any page yet', async () => {
    const resolveLabel = vi.fn(() => of('Pre-selected Program'));
    fixture.componentRef.setInput('resolveLabel', resolveLabel);
    fixture.componentRef.setInput('selectedValue', 42);
    fixture.detectChanges();

    expect(resolveLabel).toHaveBeenCalledWith(42);
    expect(internal().selectedLabel()).toBe('Pre-selected Program');
  });

  it('clears cached options when reloadKey changes (e.g. a dependent Course picker after Program changes)', () => {
    internal().toggleOpen(); // loads page 0 under the old context
    expect(internal().options().length).toBeGreaterThan(0);
    internal().toggleOpen(); // close it -- picker is closed at the time the dependency changes

    fixture.componentRef.setInput('reloadKey', 5);
    fixture.detectChanges();

    // Cache is cleared immediately but not eagerly re-fetched while closed.
    expect(internal().options()).toEqual([]);
  });

  // ControlValueAccessor — lets formControlName/[formControl] drive this picker directly
  // (course-form, subject-form, etc.) instead of every Reactive-Forms host hand-wiring
  // [selectedValue]/(selectedValueChange) itself.
  it('writeValue sets the selected value and resolves its label, like an external [selectedValue] change', () => {
    const resolveLabel = vi.fn(() => of('Bachelor of Science'));
    fixture.componentRef.setInput('resolveLabel', resolveLabel);

    component.writeValue(9);

    expect(resolveLabel).toHaveBeenCalledWith(9);
    expect(internal().selectedLabel()).toBe('Bachelor of Science');
  });

  it('registerOnChange/registerOnTouched callbacks fire on selection, the way Angular forms expects', () => {
    const onChange = vi.fn();
    const onTouched = vi.fn();
    component.registerOnChange(onChange);
    component.registerOnTouched(onTouched);

    internal().selectSingle({ id: 2, name: 'Beta' });

    expect(onChange).toHaveBeenCalledWith(2);
    expect(onTouched).toHaveBeenCalled();
  });

  it('setDisabledState(true) disables the button and blocks toggleOpen from opening the panel', () => {
    component.setDisabledState(true);

    internal().toggleOpen();

    expect(internal().disabled).toBe(true);
    expect(internal().open()).toBe(false);
    expect(fetchPage).not.toHaveBeenCalled();
  });
});
