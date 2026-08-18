import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { UserRoleService } from '../../core/permissions/user-role.service';
import { AllPermissionsResponse, TierImpactEntry } from '../../core/permissions/permission.model';
import { groupPermissionsByNav, colorForNavGroup } from '../../core/permissions/menu-order.util';
import { ToastService } from '../../core/toast/toast.service';
import { forkJoin } from 'rxjs';
import { TourService } from '../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../shared/tour/tour-button.component';
import { PERMISSION_TIER_TOUR, PERMISSION_TIER_FLOW_MAP } from '../../shared/tour/tours/user-management.tours';

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
  itemLabel: string;
  rows: PermTierRow[];
}

interface CategoryGroup {
  groupLabel: string;
  groupIcon: string;
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


@Component({
  selector: 'app-permission-tier',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, MatTooltipModule, CmsTourButtonComponent],
  templateUrl: './permission-tier.component.html',
  styleUrl: './permission-tier.component.scss',
})
export class PermissionTierComponent implements OnInit {
  private readonly svc   = inject(UserRoleService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly loading  = signal(true);
  protected readonly saving   = signal(false);
  protected readonly groups   = signal<CategoryGroup[]>([]);
  protected readonly tierMeta = TIER_META;
  protected readonly tiers    = [1, 2, 3, 4];

  protected readonly dirtyCount = computed(() =>
    this.groups().reduce((n, g) => n + g.rows.filter(r => r.dirty).length, 0)
  );

  protected readonly showConfirm = signal(false);
  protected readonly loadingImpact = signal(false);
  protected readonly tierImpact = signal<TierImpactEntry[]>([]);

  ngOnInit(): void {
    this.tourService.register('permission-tier', PERMISSION_TIER_TOUR);
    this.tourService.registerFlowMap('permission-tier', PERMISSION_TIER_FLOW_MAP);

    this.svc.getAllPermissions().subscribe({
      next: (perms) => {
        const rows: PermTierRow[] = perms.map(p => ({
          id: p.id, code: p.code, displayName: p.displayName,
          category: p.category, screenLabel: p.screenLabel ?? '',
          currentTier: p.tier, pendingTier: p.tier,
          dirty: false, saving: false,
        }));
        const navGroups = groupPermissionsByNav(
          rows,
          (r) => r.code,
          (r) => r.screenLabel || 'General',
        );
        const groups: CategoryGroup[] = navGroups.map(({ groupLabel, groupIcon, itemGroups }) => ({
          groupLabel,
          groupIcon,
          rows: itemGroups.flatMap(ig => ig.items),
          screenGroups: itemGroups.map(ig => ({ itemLabel: ig.itemLabel, rows: ig.items })),
          expanded: true,
        }));
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

  protected catColorOf(label: string): string {
    return colorForNavGroup(label);
  }

  protected requestSave(): void {
    if (this.dirtyCount() === 0) return;
    const dirtyRows = this.groups().flatMap(g => g.rows).filter(r => r.dirty);

    this.loadingImpact.set(true);
    this.svc.previewTierImpact(dirtyRows.map(r => ({ id: r.id, tier: r.pendingTier }))).subscribe({
      next: (impact) => {
        this.tierImpact.set(impact);
        this.loadingImpact.set(false);
        this.showConfirm.set(true);
      },
      error: () => {
        this.tierImpact.set([]);
        this.loadingImpact.set(false);
        this.toast.error('Could not preview role impact — proceeding without it');
        this.showConfirm.set(true);
      },
    });
  }

  protected cancelConfirm(): void {
    this.showConfirm.set(false);
    this.tierImpact.set([]);
  }

  protected confirmSave(): void {
    this.showConfirm.set(false);
    const dirtyRows = this.groups()
      .flatMap(g => g.rows)
      .filter(r => r.dirty);
    const revokedRoleCount = new Set(
      this.tierImpact().flatMap(i => i.revokedFrom.map(r => r.roleId))
    ).size;
    this.tierImpact.set([]);

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
        const tierMsg = `Updated ${dirtyRows.length} permission tier${dirtyRows.length > 1 ? 's' : ''}`;
        this.toast.success(revokedRoleCount > 0
          ? `${tierMsg} — revoked from ${revokedRoleCount} role${revokedRoleCount > 1 ? 's' : ''} that no longer qualify`
          : tierMsg);
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
