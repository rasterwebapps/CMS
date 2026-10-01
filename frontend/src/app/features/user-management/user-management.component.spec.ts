import { TestBed } from '@angular/core/testing';
import { describe, it, expect, vi } from 'vitest';

import { UserManagementComponent } from './user-management.component';
import { UserRoleService } from '../../core/permissions/user-role.service';
import { PermissionService } from '../../core/permissions/permission.service';
import { ToastService } from '../../core/toast/toast.service';
import { TourService } from '../../shared/tour/tour.service';
import { AppUserResponse } from '../../core/permissions/permission.model';

// Regression coverage for the USER_RENAME feature: a holder of USER_RENAME can see users at/above
// their own hierarchy level (e.g. a seeded DEV_ADMIN account), but fullyEditable() must still gate
// the regular Edit/Deactivate actions to rows strictly below the viewer's own level — only the
// narrower Rename action may reach a same-or-higher-level row.
describe('UserManagementComponent — fullyEditable gating', () => {
  function setup(myLevel: number): UserManagementComponent {
    TestBed.configureTestingModule({
      imports: [UserManagementComponent],
      providers: [
        { provide: UserRoleService, useValue: { getUsers: vi.fn(), getRoles: vi.fn() } },
        { provide: PermissionService, useValue: { has: vi.fn(() => true), level: vi.fn(() => myLevel) } },
        { provide: ToastService, useValue: { error: vi.fn(), success: vi.fn() } },
        { provide: TourService, useValue: { register: vi.fn(), registerFlowMap: vi.fn() } },
      ],
    });
    // ngOnInit's loadAll() is never triggered (no detectChanges()) — mocks only need to satisfy DI.
    return TestBed.createComponent(UserManagementComponent).componentInstance;
  }

  const user = (hierarchyLevel: number): AppUserResponse => ({
    id: 1, keycloakUsername: 'devadmin', email: 'dev@test.com', fullName: 'Developer Administrator',
    roleName: 'DEV_ADMIN', roleDisplayName: 'Developer Admin', hierarchyLevel,
    isActive: true, createdBy: 'system', createdAt: new Date().toISOString(),
  });

  const internal = (c: UserManagementComponent) =>
    c as unknown as { fullyEditable(u: AppUserResponse): boolean };

  it('treats a user at a strictly lower level (higher number) as fully editable', () => {
    const component = setup(3);
    expect(internal(component).fullyEditable(user(5))).toBe(true);
  });

  it('treats a user at the same level as NOT fully editable', () => {
    const component = setup(3);
    expect(internal(component).fullyEditable(user(3))).toBe(false);
  });

  it('treats a user at a higher level (lower number, e.g. DEV_ADMIN) as NOT fully editable', () => {
    const component = setup(3);
    expect(internal(component).fullyEditable(user(1))).toBe(false);
  });
});
