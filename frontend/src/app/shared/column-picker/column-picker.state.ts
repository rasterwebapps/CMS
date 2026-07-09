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
}

export class ColumnPickerState {
  readonly columns: ColumnDef[];
  readonly orderedColumns: Signal<ColumnDef[]>;
  readonly visibleColumns: Signal<string[]>;
  readonly stickyColumns: Signal<Set<string>>;

  private readonly storageKey: string;
  private readonly mandatoryKeys: Set<string>;
  private readonly colMap: Map<string, ColumnDef>;
  private readonly _order:   WritableSignal<string[]>;
  private readonly _visible: WritableSignal<Set<string>>;
  private readonly _sticky:  WritableSignal<Set<string>>;

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

    this._order   = signal(order);
    this._visible = signal(visible);
    this._sticky  = signal(sticky);

    this.orderedColumns = computed(() =>
      this._order().map(k => this.colMap.get(k)!).filter(Boolean)
    );
    this.visibleColumns = computed(() =>
      this._order().filter(k => this._visible().has(k))
    );
    this.stickyColumns = computed(() => this._sticky());
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

  private _load(): StoredPrefs {
    try {
      const raw = JSON.parse(localStorage.getItem(this.storageKey)!);
      if (Array.isArray(raw)) return { visible: raw as string[] }; // migrate old string[] format
      return (raw as StoredPrefs) ?? {};
    } catch { return {}; }
  }

  private _save(): void {
    localStorage.setItem(this.storageKey, JSON.stringify({
      order:   this._order(),
      visible: [...this._visible()],
      sticky:  [...this._sticky()],
    }));
  }
}
