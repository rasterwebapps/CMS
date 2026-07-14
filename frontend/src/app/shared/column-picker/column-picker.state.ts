import { computed, signal, Signal, WritableSignal } from '@angular/core';

export interface ColumnDef {
  key: string;
  label: string;
  mandatory?: boolean;  // always visible; checkbox disabled
  pinnable?: boolean;   // defaults true; set false for 'actions' etc.
}

interface StoredPrefs {
  order?: string[];
  visible?: string[];
  sticky?: string[];
  widths?: Record<string, number>;
  wrapText?: boolean;
}

export class ColumnPickerState {
  readonly columns: ColumnDef[];
  readonly orderedColumns: Signal<ColumnDef[]>;
  readonly visibleColumns: Signal<string[]>;
  readonly stickyColumns: Signal<Set<string>>;
  readonly widths: Signal<Record<string, number>>;
  readonly wrapText: Signal<boolean>;

  private readonly storageKey: string;
  private readonly mandatoryKeys: Set<string>;
  private readonly colMap: Map<string, ColumnDef>;
  private readonly _order:    WritableSignal<string[]>;
  private readonly _visible:  WritableSignal<Set<string>>;
  private readonly _sticky:   WritableSignal<Set<string>>;
  private readonly _widths:   WritableSignal<Record<string, number>>;
  private readonly _wrapText: WritableSignal<boolean>;

  constructor(config: {
    columns: ColumnDef[];
    storageKey: string;
    defaults?: string[];
    defaultSticky?: string[];
  }) {
    this.columns = config.columns;
    this.storageKey = config.storageKey;
    this.mandatoryKeys = new Set(config.columns.filter(c => c.mandatory).map(c => c.key));
    this.colMap = new Map(config.columns.map(c => [c.key, c]));

    const prefs   = this._load();
    const allKeys = config.columns.map(c => c.key);

    // Order: merge saved order with any new columns appended at end
    const savedOrder  = (prefs.order ?? []).filter(k => this.colMap.has(k));
    const unsavedKeys = allKeys.filter(k => !savedOrder.includes(k));
    const order = savedOrder.length ? [...savedOrder, ...unsavedKeys] : allKeys;

    // Visible: saved or defaults or all; mandatory always visible
    const defaultSet = new Set(config.defaults ?? allKeys);
    const visible = prefs.visible
      ? new Set([...prefs.visible.filter(k => this.colMap.has(k)), ...this.mandatoryKeys])
      : defaultSet;
    this.mandatoryKeys.forEach(k => visible.add(k));

    // Sticky: saved or per-screen defaults
    const sticky = new Set(
      (prefs.sticky ?? config.defaultSticky ?? []).filter(k => this.colMap.has(k))
    );

    this._order    = signal(order);
    this._visible  = signal(visible);
    this._sticky   = signal(sticky);
    this._widths   = signal(prefs.widths ?? {});
    this._wrapText = signal(prefs.wrapText ?? false);

    this.orderedColumns = computed(() =>
      this._order().map(k => this.colMap.get(k)!).filter(Boolean)
    );
    this.visibleColumns = computed(() =>
      this._order().filter(k => this._visible().has(k))
    );
    this.stickyColumns = computed(() => this._sticky());
    this.widths = computed(() => this._widths());
    this.wrapText = computed(() => this._wrapText());
  }

  toggle(key: string): void {
    if (this.mandatoryKeys.has(key)) return;
    this._visible.update(s => {
      const next = new Set(s);
      const nonMandatoryVisible = [...next].filter(k => !this.mandatoryKeys.has(k));
      if (next.has(key) && nonMandatoryVisible.length > 1) {
        next.delete(key);
      } else if (!next.has(key)) {
        next.add(key);
      }
      return next;
    });
    this._save();
  }

  pin(key: string): void {
    if (this.colMap.get(key)?.pinnable === false) return;
    this._sticky.update(s => {
      const next = new Set(s);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
    this._save();
  }

  reorder(from: number, to: number): void {
    this._order.update(order => {
      const next = [...order];
      const [moved] = next.splice(from, 1);
      next.splice(to, 0, moved);
      return next;
    });
    this._save();
  }

  isVisible(key: string): boolean   { return this._visible().has(key); }
  isSticky(key: string): boolean    { return this._sticky().has(key); }
  isMandatory(key: string): boolean { return this.mandatoryKeys.has(key); }
  isPinnable(key: string): boolean  { return this.colMap.get(key)?.pinnable !== false; }

  getWidth(key: string): number | undefined { return this._widths()[key]; }

  setWidth(key: string, px: number): void {
    this._widths.update(w => ({ ...w, [key]: Math.round(px) }));
    this._save();
  }

  toggleWrap(): void {
    this._wrapText.update(w => !w);
    this._save();
  }

  private _load(): StoredPrefs {
    try {
      const raw = JSON.parse(localStorage.getItem(this.storageKey)!);
      if (Array.isArray(raw)) return { visible: raw as string[] }; // migrate old string[] format
      return (raw as StoredPrefs) ?? {};
    } catch { return {}; }
  }

  private _save(): void {
    localStorage.setItem(this.storageKey, JSON.stringify({
      order:    this._order(),
      visible:  [...this._visible()],
      sticky:   [...this._sticky()],
      widths:   this._widths(),
      wrapText: this._wrapText(),
    }));
  }
}
