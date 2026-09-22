import {
  Component, ElementRef, EventEmitter, forwardRef, HostListener, Input, OnChanges, OnDestroy, OnInit,
  Output, SimpleChanges, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subject, merge, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import {
  InfiniteSelectFetchPage, InfiniteSelectOption, InfiniteSelectResolveLabel, InfiniteSelectValue,
} from './infinite-select.model';

/**
 * Searchable, infinite-scroll dropdown for any DB-backed filter — replaces a plain `<select>`
 * (or a bespoke checkbox panel) whose options/totals used to come from a bounded or fully-loaded
 * list. Loads {@link pageSize} options at a time via {@link fetchPage}; typing in the built-in
 * search box re-queries the backend (debounced), and scrolling near the bottom of the panel loads
 * the next page of whatever's currently searched — the same mechanism OnePharmacy's product/agent
 * pickers use (scroll-threshold directive feeding a paginated fetch), just built directly against
 * this panel's own scroll container instead of a MatAutocomplete/MatSelect overlay.
 *
 * Single-select (`multiple` false, default): emits the clicked option's `id` or `name` (per
 * {@link valueKey}) via `selectedValueChange`; the button shows that option's resolved name, or
 * `label` as the "nothing selected" placeholder.
 *
 * Multi-select (`multiple` true): emits the updated Set via `selectedValuesChange` on every
 * toggle; the button always shows whatever `label` the host passes (the host already owns its own
 * "N selected" summary logic — see enquiry-list's `academicYearFilterLabel`).
 *
 * Usage (single-select, id-valued):
 *   <cms-infinite-select label="All Programs" ariaLabel="Filter by program"
 *     [fetchPage]="programFetchPage" [resolveLabel]="programResolveLabel"
 *     [selectedValue]="selectedProgramId()" (selectedValueChange)="onProgramChange($event)" />
 *
 * Usage (single-select, name-valued — the value already IS the label, so resolveLabel is optional):
 *   <cms-infinite-select label="All Agents" ariaLabel="Filter by agent" valueKey="name"
 *     [fetchPage]="agentFetchPage"
 *     [selectedValue]="selectedAgent()" (selectedValueChange)="onAgentChange($event)" />
 *
 * Usage (multi-select):
 *   <cms-infinite-select multiple [label]="academicYearFilterLabel()" ariaLabel="Filter by academic year"
 *     [fetchPage]="academicYearFetchPage" [selectedValues]="selectedAcademicYearIds()"
 *     (selectedValuesChange)="onAcademicYearsChange($event)" clearLabel="Show all years"
 *     [showClear]="selectedAcademicYearIds().size > 0" (cleared)="clearAcademicYears()" />
 *
 * Usage (single-select, Reactive Forms — implements ControlValueAccessor, so `formControlName`/
 * `[formControl]` work directly instead of wiring [selectedValue]/(selectedValueChange) by hand;
 * `multiple` is not supported through a form control, since a Set isn't a typical control value):
 *   <cms-infinite-select label="All Programs" ariaLabel="Program" formControlName="programId"
 *     [fetchPage]="programFetchPage" [resolveLabel]="programResolveLabel" />
 */
@Component({
  selector: 'cms-infinite-select',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './infinite-select.component.html',
  styleUrl: './infinite-select.component.scss',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CmsInfiniteSelectComponent),
    multi: true,
  }],
})
export class CmsInfiniteSelectComponent implements OnInit, OnChanges, OnDestroy, ControlValueAccessor {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) ariaLabel!: string;
  @Input({ required: true }) fetchPage!: InfiniteSelectFetchPage;
  @Input() resolveLabel?: InfiniteSelectResolveLabel;
  @Input() valueKey: 'id' | 'name' = 'id';
  @Input() multiple = false;
  @Input() pageSize = 10;
  @Input() searchPlaceholder = 'Search…';
  /** Bump (any new value, e.g. the parent's selected Program id) whenever the option set this
   *  picker searches over should change — clears cached options/pages so the next open re-fetches
   *  fresh data instead of showing stale results for the old context. */
  @Input() reloadKey: unknown;

  @Input() selectedValue: InfiniteSelectValue | null = null;
  @Output() selectedValueChange = new EventEmitter<InfiniteSelectValue | null>();

  @Input() selectedValues: ReadonlySet<InfiniteSelectValue> = new Set();
  @Output() selectedValuesChange = new EventEmitter<Set<InfiniteSelectValue>>();
  @Input() showClear = false;
  @Input() clearLabel = 'Clear selection';
  @Output() cleared = new EventEmitter<void>();

  /** Also settable via ControlValueAccessor's setDisabledState when used with a form control. */
  @Input() disabled = false;
  private onChange: (value: InfiniteSelectValue | null) => void = () => {};
  private onTouched: () => void = () => {};

  protected readonly open = signal(false);
  protected readonly searchTerm = signal('');
  protected readonly options = signal<InfiniteSelectOption[]>([]);
  protected readonly loading = signal(false);
  protected readonly hasMore = signal(true);
  protected readonly selectedLabel = signal('');

  private pageIndex = 0;
  // Typed search goes through a debounce; opening for the first time or a reloadKey-triggered
  // refresh must fetch immediately — nothing was typed, so there's nothing to wait out.
  private readonly searchReset$ = new Subject<string>();
  private readonly immediateReset$ = new Subject<string>();
  private readonly loadMore$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly elementRef: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    const resetTo = (term: string) => {
      this.searchTerm.set(term);
      this.pageIndex = 0;
      this.options.set([]);
      this.hasMore.set(true);
    };

    merge(
      this.immediateReset$.pipe(tap(resetTo)),
      this.searchReset$.pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(resetTo),
      ),
      this.loadMore$.pipe(
        filter(() => !this.loading() && this.hasMore()),
        tap(() => this.pageIndex++),
      ),
    ).pipe(
      tap(() => this.loading.set(true)),
      switchMap(() =>
        this.fetchPage(this.searchTerm(), this.pageIndex, this.pageSize).pipe(
          catchError(() => of(null)),
        ),
      ),
      takeUntil(this.destroy$),
    ).subscribe(page => {
      this.loading.set(false);
      if (!page) { this.hasMore.set(false); return; }
      this.options.update(existing => (this.pageIndex === 0 ? page.content : existing.concat(page.content)));
      this.hasMore.set((this.pageIndex + 1) * this.pageSize < page.totalElements);
    });

    if (!this.multiple) this.updateSelectedLabel();

    // Capture phase (not bubble, which @HostListener('document:click') would use) so this still
    // fires even when some ancestor toolbar calls $event.stopPropagation() on the bubble phase —
    // several screens do that on their filter-bar wrapper, which silently broke sibling pickers'
    // outside-click-close (each stayed open when another was opened in the same toolbar).
    document.addEventListener('click', this.documentClickListener, true);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedValue'] && !this.multiple) this.updateSelectedLabel();

    if (changes['reloadKey'] && !changes['reloadKey'].firstChange) {
      if (this.open()) {
        this.immediateReset$.next(this.searchTerm());
      } else {
        // Closed -- just drop the stale cache so the next open re-fetches under the new context.
        this.pageIndex = 0;
        this.options.set([]);
        this.hasMore.set(true);
      }
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    document.removeEventListener('click', this.documentClickListener, true);
  }

  private readonly documentClickListener = (event: MouseEvent): void => {
    if (this.open() && !this.elementRef.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  };

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.open.set(false);
  }

  // ── ControlValueAccessor — lets formControlName/[formControl] drive this picker directly,
  // alongside the plain [selectedValue]/(selectedValueChange) API used by non-form filter bars.
  writeValue(value: InfiniteSelectValue | null): void {
    this.selectedValue = value;
    if (!this.multiple) this.updateSelectedLabel();
  }

  registerOnChange(fn: (value: InfiniteSelectValue | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  protected toggleOpen(): void {
    if (this.disabled) return;
    this.open.update(v => !v);
    if (this.open() && this.options().length === 0) this.immediateReset$.next(this.searchTerm());
    if (!this.open()) this.onTouched();
  }

  protected onSearchInput(term: string): void {
    this.searchReset$.next(term);
  }

  protected onScroll(event: Event): void {
    const el = event.target as HTMLElement;
    const threshold = 0.8 * el.scrollHeight;
    if (el.scrollTop + el.clientHeight >= threshold) this.loadMore$.next();
  }

  protected extractValue(option: InfiniteSelectOption): InfiniteSelectValue {
    return this.valueKey === 'name' ? option.name : option.id;
  }

  protected isSelected(option: InfiniteSelectOption): boolean {
    const val = this.extractValue(option);
    return this.multiple ? this.selectedValues.has(val) : this.selectedValue === val;
  }

  protected selectSingle(option: InfiniteSelectOption): void {
    const val = this.extractValue(option);
    this.selectedValue = val;
    this.selectedLabel.set(option.name);
    this.selectedValueChange.emit(val);
    this.onChange(val);
    this.onTouched();
    this.open.set(false);
  }

  protected selectAll(): void {
    this.selectedValue = null;
    this.selectedLabel.set('');
    this.selectedValueChange.emit(null);
    this.onChange(null);
    this.onTouched();
    this.open.set(false);
  }

  protected toggleMulti(option: InfiniteSelectOption): void {
    const val = this.extractValue(option);
    const next = new Set(this.selectedValues);
    if (next.has(val)) next.delete(val); else next.add(val);
    this.selectedValuesChange.emit(next);
  }

  protected onClear(): void {
    this.cleared.emit();
  }

  private updateSelectedLabel(): void {
    const val = this.selectedValue;
    if (val == null) { this.selectedLabel.set(''); return; }

    const found = this.options().find(o => this.extractValue(o) === val);
    if (found) { this.selectedLabel.set(found.name); return; }

    if (this.resolveLabel) {
      this.resolveLabel(val).pipe(take(1), takeUntil(this.destroy$)).subscribe({
        next: name => this.selectedLabel.set(name),
        error: () => this.selectedLabel.set(String(val)),
      });
    } else {
      this.selectedLabel.set(String(val));
    }
  }
}
