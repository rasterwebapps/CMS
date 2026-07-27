import { NAV_ENTRIES, NavGroup, isNavGroup } from '../nav/nav-config';

export interface NavPermItemGroup<T> {
  itemLabel: string;
  items: T[];
}

export interface NavPermGroup<T> {
  groupLabel: string;
  groupIcon: string;
  itemGroups: NavPermItemGroup<T>[];
}

interface Placement {
  groupLabel: string;
  groupIcon: string;
  itemLabel: string;
}

/**
 * Permission code → { nav group label/icon, nav item label } for the first nav
 * entry that gates on it, walking NAV_ENTRIES top to bottom. This makes the
 * sidenav the single source of truth for how Permission Tiers / Roles &
 * Permissions group and order permissions — same headers, same item names,
 * same order as the menu, no deviations.
 */
const NAV_PLACEMENT: Map<string, Placement> = (() => {
  const map = new Map<string, Placement>();
  for (const entry of NAV_ENTRIES) {
    if (isNavGroup(entry)) {
      for (const item of entry.items) {
        for (const code of item.permissions ?? []) {
          if (!map.has(code)) map.set(code, { groupLabel: entry.label, groupIcon: entry.icon, itemLabel: item.label });
        }
      }
      for (const code of entry.permissions ?? []) {
        if (!map.has(code)) map.set(code, { groupLabel: entry.label, groupIcon: entry.icon, itemLabel: entry.label });
      }
    } else {
      for (const code of entry.permissions ?? []) {
        if (!map.has(code)) map.set(code, { groupLabel: entry.label, groupIcon: entry.icon, itemLabel: entry.label });
      }
    }
  }
  return map;
})();

/**
 * Nav item/group label (lowercased) → its placement. Secondary lookup for permission
 * codes that aren't in any nav item's `permissions` gate list (fine-grained, non-visibility
 * operations such as *_CREATE/_EDIT/_DEACTIVATE on an already-visible screen) but whose
 * backend `screenLabel` text happens to match a real nav item name exactly.
 */
const NAV_LABEL_PLACEMENT: Map<string, Placement> = (() => {
  const map = new Map<string, Placement>();
  for (const entry of NAV_ENTRIES) {
    if (isNavGroup(entry)) {
      map.set(entry.label.toLowerCase(), { groupLabel: entry.label, groupIcon: entry.icon, itemLabel: entry.label });
      for (const item of entry.items) {
        map.set(item.label.toLowerCase(), { groupLabel: entry.label, groupIcon: entry.icon, itemLabel: item.label });
      }
    } else {
      map.set(entry.label.toLowerCase(), { groupLabel: entry.label, groupIcon: entry.icon, itemLabel: entry.label });
    }
  }

  // Backend `screenLabel` text that has drifted from the current nav item wording,
  // for screens that unambiguously still exist in the nav under a different label.
  const RENAMED_SCREEN_ALIASES: Record<string, string> = {
    'book catalogue':  'book explorer',    // Library → Book Explorer
    'journals':        'journal explorer', // Library → Journal Explorer
    'library reports': 'overdue books',    // Library → Overdue Books
    'countries':       'location master',  // Preferences → Location Master (India Locations)
  };
  for (const [oldLabel, currentLabel] of Object.entries(RENAMED_SCREEN_ALIASES)) {
    const placement = map.get(currentLabel);
    if (placement) map.set(oldLabel, placement);
  }

  return map;
})();

/** Permissions with no nav-item match (fine-grained ops the nav doesn't gate on, or dead codes) land here. */
export const OTHER_GROUP_LABEL = 'Other';
const OTHER_GROUP_ICON = 'more_horiz';

/** Cycled by nav-group label so each sidenav module gets a stable, distinct accent color. */
const GROUP_COLOR_PALETTE = [
  '#6366f1', '#3b82f6', '#0ea5e9', '#14b8a6', '#10b981',
  '#f59e0b', '#f97316', '#ec4899', '#8b5cf6', '#a855f7', '#ef4444',
];

export function colorForNavGroup(groupLabel: string): string {
  let hash = 0;
  for (let i = 0; i < groupLabel.length; i++) hash = (hash * 31 + groupLabel.charCodeAt(i)) | 0;
  return GROUP_COLOR_PALETTE[Math.abs(hash) % GROUP_COLOR_PALETTE.length];
}

/**
 * Groups `items` (e.g. permission rows) into nav order: top-level group = the
 * sidenav module label that gates on the item's permission code, sub-group =
 * the sidenav item label. A code not referenced by any nav item's `permissions`
 * falls back to `fallbackItemLabelOf(item)` inside a trailing "Other" group.
 */
export function groupPermissionsByNav<T>(
  items: T[],
  codeOf: (item: T) => string,
  fallbackItemLabelOf: (item: T) => string,
): NavPermGroup<T>[] {
  const navGroupOrder = NAV_ENTRIES.filter(isNavGroup).map(g => g.label);
  const buckets = new Map<string, Map<string, T[]>>();

  for (const it of items) {
    const fallbackLabel = fallbackItemLabelOf(it);
    const placement = NAV_PLACEMENT.get(codeOf(it)) ?? NAV_LABEL_PLACEMENT.get(fallbackLabel.toLowerCase());
    const groupLabel = placement?.groupLabel ?? OTHER_GROUP_LABEL;
    const itemLabel = placement?.itemLabel ?? fallbackLabel;
    if (!buckets.has(groupLabel)) buckets.set(groupLabel, new Map());
    const itemMap = buckets.get(groupLabel)!;
    const arr = itemMap.get(itemLabel) ?? [];
    arr.push(it);
    itemMap.set(itemLabel, arr);
  }

  const orderedGroupLabels = [
    ...navGroupOrder.filter(l => buckets.has(l)),
    ...Array.from(buckets.keys()).filter(l => !navGroupOrder.includes(l)).sort(),
  ];

  return orderedGroupLabels.map(groupLabel => {
    const navGroup = NAV_ENTRIES.find((e): e is NavGroup => isNavGroup(e) && e.label === groupLabel);
    const groupIcon = navGroup?.icon ?? OTHER_GROUP_ICON;
    const itemMap = buckets.get(groupLabel)!;
    const itemOrder = navGroup ? navGroup.items.map(i => i.label) : [];
    const orderedItemLabels = [
      ...itemOrder.filter(l => itemMap.has(l)),
      ...Array.from(itemMap.keys()).filter(l => !itemOrder.includes(l)).sort(),
    ];
    return {
      groupLabel,
      groupIcon,
      itemGroups: orderedItemLabels.map(itemLabel => ({ itemLabel, items: itemMap.get(itemLabel)! })),
    };
  });
}
