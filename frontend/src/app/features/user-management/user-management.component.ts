import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { UserRoleService } from '../../core/permissions/user-role.service';
import { PermissionService } from '../../core/permissions/permission.service';
import { AppUserResponse, AppRoleResponse, CreateUserRequest, UpdateUserRequest } from '../../core/permissions/permission.model';
import { ToastService } from '../../core/toast/toast.service';

type PanelMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './user-management.component.html',
  styleUrl:    './user-management.component.scss',
})
export class UserManagementComponent implements OnInit {
  private readonly svc    = inject(UserRoleService);
  private readonly perm   = inject(PermissionService);
  private readonly toast  = inject(ToastService);

  protected readonly users       = signal<AppUserResponse[]>([]);
  protected readonly roles       = signal<AppRoleResponse[]>([]);
  protected readonly loading     = signal(true);
  protected readonly saving      = signal(false);
  protected readonly panelMode   = signal<PanelMode>(null);
  protected readonly editTarget  = signal<AppUserResponse | null>(null);
  protected readonly searchTerm  = signal('');

  protected readonly filteredUsers = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    return this.users().filter(u =>
      !term ||
      u.fullName.toLowerCase().includes(term) ||
      u.email.toLowerCase().includes(term) ||
      u.keycloakUsername.toLowerCase().includes(term) ||
      u.roleDisplayName.toLowerCase().includes(term)
    );
  });

  // ── Create form ──────────────────────────────────────────────
  protected createForm: CreateUserRequest = this.emptyCreate();

  // ── Edit form ────────────────────────────────────────────────
  protected editForm: UpdateUserRequest = { fullName: '', email: '', roleName: '', isActive: true };

  protected readonly canCreate = computed(() => this.perm.has('USER_CREATE'));
  protected readonly canEdit   = computed(() => this.perm.has('USER_EDIT'));
  protected readonly canDeactivate = computed(() => this.perm.has('USER_DEACTIVATE'));

  ngOnInit(): void {
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.svc.getUsers().subscribe({
      next: (u) => { this.users.set(u); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load users'); this.loading.set(false); },
    });
    this.svc.getRoles().subscribe({ next: (r) => this.roles.set(r) });
  }

  protected openCreate(): void {
    this.createForm = this.emptyCreate();
    this.panelMode.set('create');
  }

  protected openEdit(user: AppUserResponse): void {
    this.editTarget.set(user);
    this.editForm = { fullName: user.fullName, email: user.email, roleName: user.roleName, isActive: user.isActive };
    this.panelMode.set('edit');
  }

  protected closePanel(): void {
    this.panelMode.set(null);
    this.editTarget.set(null);
  }

  protected submitCreate(): void {
    const f = this.createForm;
    if (!f.email || !f.fullName || !f.keycloakUsername || !f.password || !f.roleName) {
      this.toast.error('Please fill all required fields');
      return;
    }
    if (f.password.length < 8) {
      this.toast.error('Password must be at least 8 characters');
      return;
    }
    this.saving.set(true);
    this.svc.createUser(this.createForm).subscribe({
      next: (u) => {
        this.users.update(list => [u, ...list]);
        this.toast.success(`User "${u.fullName}" created`);
        this.closePanel();
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create user');
        this.saving.set(false);
      },
    });
  }

  protected submitEdit(): void {
    const target = this.editTarget();
    if (!target) return;
    this.saving.set(true);
    this.svc.updateUser(target.id, this.editForm).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.toast.success('User updated');
        this.closePanel();
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to update user');
        this.saving.set(false);
      },
    });
  }

  protected toggleActive(user: AppUserResponse): void {
    const call$ = user.isActive ? this.svc.deactivateUser(user.id) : this.svc.reactivateUser(user.id);
    call$.subscribe({
      next: () => {
        this.users.update(list =>
          list.map(u => u.id === user.id ? { ...u, isActive: !u.isActive } : u)
        );
        this.toast.success(user.isActive ? 'User deactivated' : 'User reactivated');
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Action failed'),
    });
  }

  private emptyCreate(): CreateUserRequest {
    return { email: '', fullName: '', keycloakUsername: '', password: '', roleName: '' };
  }
}
