import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { UserRoleService } from '../../core/permissions/user-role.service';
import { PermissionService } from '../../core/permissions/permission.service';
import { groupPermissionsByNav, colorForNavGroup } from '../../core/permissions/menu-order.util';
import { AppRoleResponse, AllPermissionsResponse, PermissionGroup, WidgetConfigDto } from '../../core/permissions/permission.model';
import { ToastService } from '../../core/toast/toast.service';
import { WidgetPickerComponent } from '../../shared/widget-picker/widget-picker.component';
import { CmsEmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { TourService } from '../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../shared/tour/tour-button.component';
import { ROLE_MANAGEMENT_TOUR, ROLE_MANAGEMENT_FLOW_MAP } from '../../shared/tour/tours/user-management.tours';

type PanelMode = 'create' | 'edit' | 'widgets' | null;

interface PermSubGroup {
  screenLabel: string;
  permissions: AllPermissionsResponse[];
}

interface PermGroupWithSubs extends PermissionGroup {
  subGroups: PermSubGroup[];
}

const ACTION_SUFFIXES = new Set([
  'CREATE', 'DELETE', 'EDIT', 'UPDATE', 'VIEW', 'MANAGE',
  'ASSIGN', 'DEACTIVATE', 'GENERATE', 'DOWNLOAD', 'EXPORT',
  'IMPORT', 'DATA', 'CONFIGURE', 'UPLOAD', 'APPROVE', 'REJECT',
  'SUBMIT', 'REVIEW', 'COMPLETE', 'CANCEL', 'ARCHIVE', 'COLLECT',
  'FINALIZE', 'MARK', 'REPORT',
]);

@Component({
  selector: 'app-role-management',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, MatTooltipModule, MatIconModule, WidgetPickerComponent, CmsEmptyStateComponent, CmsTourButtonComponent],
  templateUrl: './role-management.component.html',
  styleUrl: './role-management.component.scss',
})
export class RoleManagementComponent implements OnInit {
  private readonly svc   = inject(UserRoleService);
  private readonly perm  = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly roles    = signal<AppRoleResponse[]>([]);
  protected readonly allPerms = signal<AllPermissionsResponse[]>([]);
  protected readonly loading  = signal(true);
  protected readonly saving   = signal(false);
  protected readonly panelMode  = signal<PanelMode>(null);
  protected readonly editTarget = signal<AppRoleResponse | null>(null);
  protected readonly searchTerm = signal('');

  protected createForm = { name: '', displayName: '', description: '' };
  protected readonly editPermCodes = signal<Set<string>>(new Set());

  /** Which accordion categories are collapsed in the editor */
  protected readonly collapsedGroups = signal<Set<string>>(new Set<string>());

  /** Which role cards are expanded in the left sidebar */
  protected readonly expandedRoleIds = signal<Set<number>>(new Set<number>());

  protected readonly canCreate = computed(() => this.perm.has('ROLE_CREATE'));
  protected readonly canEdit   = computed(() => this.perm.has('ROLE_EDIT'));

  // ── Widget picker state ───────────────────────────────────────
  protected widgetEditTarget: AppRoleResponse | null = null;

  /** Full permission list grouped by sidenav module → sub-grouped by sidenav item, same headers/order as the menu */
  protected readonly permGroups = computed<PermGroupWithSubs[]>(() => {
    const navGroups = groupPermissionsByNav(
      this.allPerms(),
      (p) => p.code,
      (p) => p.screenLabel ?? this.resourcePrefix(p.code),
    );
    return navGroups.map(({ groupLabel, itemGroups }) => ({
      category: groupLabel,
      permissions: itemGroups.flatMap(ig => ig.items),
      subGroups: itemGroups.map(ig => ({ screenLabel: ig.itemLabel, permissions: ig.items })),
    }));
  });

  protected groupColor(label: string): string {
    return colorForNavGroup(label);
  }

  protected readonly filteredRoles = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    return this.roles().filter(r =>
      !term ||
      r.displayName.toLowerCase().includes(term) ||
      r.name.toLowerCase().includes(term) ||
      (r.description ?? '').toLowerCase().includes(term)
    );
  });

  ngOnInit(): void {
    this.tourService.register('role-management', ROLE_MANAGEMENT_TOUR);
    this.tourService.registerFlowMap('role-management', ROLE_MANAGEMENT_FLOW_MAP);
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.svc.getRoles().subscribe({
      next: (r) => {
        this.roles.set(r);
        this.loading.set(false);
        // Auto-select the first role so permissions show immediately on load
        if (r.length > 0) { this.openEdit(r[0]); }
      },
      error: () => { this.toast.error('Failed to load roles'); this.loading.set(false); },
    });
    this.svc.getDelegatablePermissions().subscribe({ next: (p) => this.allPerms.set(p) });
  }

  // ── Role card expand / collapse ───────────────────────────────
  protected isRoleExpanded(id: number): boolean {
    return this.expandedRoleIds().has(id);
  }

  protected toggleRoleExpand(id: number): void {
    this.expandedRoleIds.update(set => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  /**
   * Returns the assigned permissions for a role, grouped by resource prefix.
   * Each inner array = one row (max 4 items visually enforced via CSS grid).
   */
  protected getRolePermRows(role: AppRoleResponse): AllPermissionsResponse[][] {
    const codes = new Set(role.permissionCodes);
    const map = new Map<string, AllPermissionsResponse[]>();
    for (const p of this.allPerms()) {
      if (!codes.has(p.code)) continue;
      const prefix = this.resourcePrefix(p.code);
      const arr = map.get(prefix) ?? [];
      arr.push(p);
      map.set(prefix, arr);
    }
    return Array.from(map.values());
  }

  /** Extract the short action label from a permission code (VIEW, CREATE → View, Create). */
  protected actionLabel(code: string): string {
    const parts = code.split('_');
    const last = parts[parts.length - 1];
    if (ACTION_SUFFIXES.has(last)) {
      return last.charAt(0) + last.slice(1).toLowerCase();
    }
    return last;
  }

  // ── Panel open / close ────────────────────────────────────────
  protected openCreate(): void {
    this.createForm = { name: '', displayName: '', description: '' };
    this.editTarget.set(null);
    this.panelMode.set('create');
  }

  protected handleEmptyAction(): void {
    if (this.searchTerm()) {
      this.searchTerm.set('');
      return;
    }

    if (this.canCreate()) {
      this.openCreate();
    }
  }

  protected openEdit(role: AppRoleResponse): void {
    this.editTarget.set(role);
    this.editPermCodes.set(new Set(role.permissionCodes));
    this.collapsedGroups.set(new Set<string>());
    this.panelMode.set('edit');
    // Auto-expand the card so the current permissions are visible for reference
    this.expandedRoleIds.update(set => { const n = new Set(set); n.add(role.id); return n; });
  }

  protected closePanel(): void {
    this.panelMode.set(null);
    this.editTarget.set(null);
    this.widgetEditTarget = null;
  }

  // ── Widget picker ─────────────────────────────────────────────
  protected openWidgetPicker(role: AppRoleResponse): void {
    this.widgetEditTarget = role;
    this.panelMode.set('widgets');
  }

  protected saveWidgets(configs: WidgetConfigDto[]): void {
    if (!this.widgetEditTarget) return;
    this.saving.set(true);
    this.svc.updateRoleDashboardWidgets(this.widgetEditTarget.id, configs).subscribe({
      next: (updated) => {
        this.roles.update(rs => rs.map(r => r.id === updated.id ? updated : r));
        this.toast.success('Dashboard widgets saved');
        this.closePanel();
        this.saving.set(false);
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to save widgets'); this.saving.set(false); },
    });
  }

  // ── Permission checkbox helpers ───────────────────────────────
  protected isPermChecked(code: string): boolean { return this.editPermCodes().has(code); }

  protected togglePerm(code: string): void {
    this.editPermCodes.update(set => {
      const next = new Set(set);
      if (next.has(code)) next.delete(code); else next.add(code);
      return next;
    });
  }

  protected allInGroupChecked(group: PermissionGroup): boolean {
    const set = this.editPermCodes();
    return group.permissions.every(p => set.has(p.code));
  }

  protected someInGroupChecked(group: PermissionGroup): boolean {
    const set = this.editPermCodes();
    return group.permissions.some(p => set.has(p.code)) && !this.allInGroupChecked(group);
  }

  protected toggleGroup(group: PermissionGroup): void {
    const all = this.allInGroupChecked(group);
    this.editPermCodes.update(set => {
      const next = new Set(set);
      for (const p of group.permissions) { if (all) next.delete(p.code); else next.add(p.code); }
      return next;
    });
  }

  protected groupSelectedCount(group: PermissionGroup): number {
    const set = this.editPermCodes();
    return group.permissions.filter(p => set.has(p.code)).length;
  }

  // ── Accordion expand / collapse ───────────────────────────────
  protected isGroupExpanded(cat: string): boolean { return !this.collapsedGroups().has(cat); }

  protected toggleGroupExpand(cat: string): void {
    this.collapsedGroups.update(set => {
      const next = new Set(set);
      if (next.has(cat)) next.delete(cat); else next.add(cat);
      return next;
    });
  }

  protected expandAll(): void { this.collapsedGroups.set(new Set<string>()); }

  protected collapseAll(): void {
    this.collapsedGroups.set(new Set(this.permGroups().map(g => g.category)));
  }

  // ── Submit handlers ───────────────────────────────────────────
  protected submitCreate(): void {
    if (!this.createForm.name || !this.createForm.displayName) {
      this.toast.error('Name and display name are required');
      return;
    }
    this.saving.set(true);
    this.svc.createRole({
      name: this.createForm.name.toUpperCase().replace(/\s+/g, '_'),
      displayName: this.createForm.displayName,
      description: this.createForm.description,
      permissionCodes: [],
    }).subscribe({
      next: (r) => {
        this.roles.update(list => [...list, r]);
        this.toast.success(`Role "${r.displayName}" created`);
        this.closePanel();
        this.saving.set(false);
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to create role'); this.saving.set(false); },
    });
  }

  protected submitEdit(): void {
    const target = this.editTarget();
    if (!target) return;
    this.saving.set(true);
    this.svc.updateRolePermissions(target.id, Array.from(this.editPermCodes())).subscribe({
      next: (updated) => {
        this.roles.update(list => list.map(r => r.id === updated.id ? updated : r));
        // Reload the current user's permissions so the admin's own session reflects
        // any changes immediately. Other logged-in users need to reload the page.
        this.perm.load();
        this.toast.success('Permissions saved. Users with this role must reload the page to see changes.');
        this.closePanel();
        this.saving.set(false);
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to update permissions'); this.saving.set(false); },
    });
  }

  // ── Utilities ─────────────────────────────────────────────────
  private resourcePrefix(code: string): string {
    const parts = code.split('_');
    if (parts.length > 1 && ACTION_SUFFIXES.has(parts[parts.length - 1])) {
      return parts.slice(0, -1).join('_');
    }
    return code;
  }

  protected levelLabel(level: number): string {
    const m: Record<number, string> = {
      1: 'Dev Admin', 2: 'Support Admin', 3: 'Admin', 4: 'College Admin', 5: 'Staff', 6: 'Other',
    };
    return m[level] ?? `Level ${level}`;
  }

  protected permCount(role: AppRoleResponse): number { return role.permissionCodes.length; }
}
