import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { UserRoleService } from '../../core/permissions/user-role.service';
import { AllPermissionsResponse } from '../../core/permissions/permission.model';
import { ToastService } from '../../core/toast/toast.service';
import { forkJoin } from 'rxjs';

interface PermTierRow {
  id: number;
  code: string;
  displayName: string;
  category: string;
  screenLabel: string;
  currentTier: number;
  pendingTier: number;
  dirty: boolean;
  saving: boolean;
}

interface ScreenGroup {
  screenLabel: string;
  rows: PermTierRow[];
}

interface CategoryGroup {
  category: string;
  rows: PermTierRow[];        // flat — used for dirty count / save / discard
  screenGroups: ScreenGroup[];
  expanded: boolean;
}

const TIER_META: Record<number, { label: string; desc: string; color: string }> = {
  1: { label: 'Dev Only',    desc: 'DEV_ADMIN can hold and assign. No other role has access.',                                    color: '#ef4444' },
  2: { label: 'Support+',   desc: 'DEV_ADMIN and SUPPORT_ADMIN can hold and assign.',                                            color: '#f59e0b' },
  3: { label: 'Hold Only',  desc: 'Senior admins can hold this permission but cannot delegate it to sub-roles they create.',      color: '#8b5cf6' },
  4: { label: 'Open',       desc: 'Any role can hold this permission, and any role that holds it can assign it to sub-roles.',   color: '#10b981' },
};

const CAT_COLOR: Record<string, string> = {
  ADMISSION:      '#6366f1',
  CURRICULUM:     '#3b82f6',
  DOCUMENT:       '#0284c7',
  EXAMINATION:    '#0ea5e9',
  FINANCE:        '#10b981',
  INFRASTRUCTURE: '#f59e0b',
  LIBRARY:        '#14b8a6',
  MASTER:         '#8b5cf6',
  REPORTS:        '#ec4899',
  SCHOLARSHIP:    '#f97316',
  SYSTEM:         '#ef4444',
};

@Component({
  selector: 'app-permission-tier',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './permission-tier.component.html',
  styleUrl: './permission-tier.component.scss',
})
export class PermissionTierComponent implements OnInit {
  private readonly svc   = inject(UserRoleService);
  private readonly toast = inject(ToastService);

  protected readonly loading  = signal(true);
  protected readonly saving   = signal(false);
  protected readonly groups   = signal<CategoryGroup[]>([]);
  protected readonly tierMeta = TIER_META;
  protected readonly catColor = CAT_COLOR;
  protected readonly tiers    = [1, 2, 3, 4];

  protected readonly dirtyCount = computed(() =>
    this.groups().reduce((n, g) => n + g.rows.filter(r => r.dirty).length, 0)
  );

  protected readonly showConfirm = signal(false);

  ngOnInit(): void {
    this.svc.getAllPermissions().subscribe({
      next: (perms) => {
        const groupMap = new Map<string, PermTierRow[]>();
        for (const p of perms) {
          const row: PermTierRow = {
            id: p.id, code: p.code, displayName: p.displayName,
            category: p.category, screenLabel: p.screenLabel ?? '',
            currentTier: p.tier, pendingTier: p.tier,
            dirty: false, saving: false,
          };
          const arr = groupMap.get(p.category) ?? [];
          arr.push(row);
          groupMap.set(p.category, arr);
        }
        const groups: CategoryGroup[] = Array.from(groupMap.entries())
          .sort(([a], [b]) => a.localeCompare(b))
          .map(([category, rows]) => {
            const screenMap = new Map<string, PermTierRow[]>();
            for (const row of rows) {
              const label = row.screenLabel || 'General';
              const arr = screenMap.get(label) ?? [];
              arr.push(row);
              screenMap.set(label, arr);
            }
            const screenGroups: ScreenGroup[] = Array.from(screenMap.entries())
              .sort(([a], [b]) => a.localeCompare(b))
              .map(([screenLabel, srows]) => ({ screenLabel, rows: srows }));
            return { category, rows, screenGroups, expanded: true };
          });
        this.groups.set(groups);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load permissions'); this.loading.set(false); },
    });
  }

  protected toggleGroup(g: CategoryGroup): void {
    this.groups.update(gs => gs.map(x => x === g ? { ...x, expanded: !x.expanded } : x));
  }

  protected markDirty(row: PermTierRow): void {
    row.dirty = row.pendingTier !== row.currentTier;
    this.groups.update(gs => [...gs]);
  }

  protected catColorOf(cat: string): string {
    return CAT_COLOR[cat] ?? '#6b7280';
  }

  protected requestSave(): void {
    if (this.dirtyCount() === 0) return;
    this.showConfirm.set(true);
  }

  protected cancelConfirm(): void {
    this.showConfirm.set(false);
  }

  protected confirmSave(): void {
    this.showConfirm.set(false);
    const dirtyRows = this.groups()
      .flatMap(g => g.rows)
      .filter(r => r.dirty);

    this.saving.set(true);
    const calls = dirtyRows.map(r =>
      this.svc.updatePermissionTier(r.id, r.pendingTier)
    );

    forkJoin(calls).subscribe({
      next: () => {
        for (const r of dirtyRows) {
          r.currentTier = r.pendingTier;
          r.dirty = false;
        }
        this.groups.update(gs => [...gs]);
        this.saving.set(false);
        this.toast.success(`Updated ${dirtyRows.length} permission tier${dirtyRows.length > 1 ? 's' : ''}`);
      },
      error: () => { this.toast.error('Some tier updates failed. Please refresh and retry.'); this.saving.set(false); },
    });
  }

  protected discardAll(): void {
    this.groups.update(gs =>
      gs.map(g => ({
        ...g,
        rows: g.rows.map(r => ({ ...r, pendingTier: r.currentTier, dirty: false })),
      }))
    );
  }
}
