import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { WardContextService } from './ward-context.service';
import { GuardianService } from '../../features/guardian/guardian.service';
import { PermissionService } from '../permissions/permission.service';
import { WardSummaryResponse } from '../../features/guardian/guardian.model';

describe('WardContextService', () => {
  let guardianService: { myWards: ReturnType<typeof vi.fn> };
  let permissionService: { loaded: ReturnType<typeof vi.fn>; hasAny: ReturnType<typeof vi.fn> };

  const wards: WardSummaryResponse[] = [
    { studentId: 45, fullName: 'Oviya Thangam', rollNumber: 'GNM4-015', isPrimary: true },
    { studentId: 46, fullName: 'Pavithra Umapathy', rollNumber: 'GNM4-016', isPrimary: false },
  ];

  function setup(): WardContextService {
    TestBed.configureTestingModule({
      providers: [
        { provide: GuardianService, useValue: guardianService },
        { provide: PermissionService, useValue: permissionService },
      ],
    });
    const service = TestBed.inject(WardContextService);
    TestBed.tick();
    return service;
  }

  beforeEach(() => {
    guardianService = { myWards: vi.fn(() => of(wards)) };
  });

  it('does not call GET /guardian/wards for a non-guardian user', () => {
    permissionService = { loaded: vi.fn(() => true), hasAny: vi.fn(() => false) };
    setup();

    expect(guardianService.myWards).not.toHaveBeenCalled();
  });

  it('waits for permissions to load before requesting wards', () => {
    // A real signal here (not a plain vi.fn) so the service's effect() actually tracks it as a
    // reactive dependency and re-runs when it flips, the same way the real PermissionService's
    // computed() `loaded` signal would.
    const loaded = signal(false);
    permissionService = { loaded: loaded as unknown as typeof permissionService.loaded, hasAny: vi.fn(() => true) };
    setup();

    expect(guardianService.myWards).not.toHaveBeenCalled();

    loaded.set(true);
    TestBed.tick();

    expect(guardianService.myWards).toHaveBeenCalled();
  });

  it('loads wards and defaults the selection to the primary ward', () => {
    permissionService = { loaded: vi.fn(() => true), hasAny: vi.fn(() => true) };
    const service = setup();

    expect(service.wards()).toEqual(wards);
    expect(service.selectedWardId()).toBe(45);
    expect(service.selectedWard()?.fullName).toBe('Oviya Thangam');
    expect(service.hasMultipleWards()).toBe(true);
  });

  it('selectWard switches the active ward when it is one of the caller\'s own wards', () => {
    permissionService = { loaded: vi.fn(() => true), hasAny: vi.fn(() => true) };
    const service = setup();

    service.selectWard(46);

    expect(service.selectedWardId()).toBe(46);
    expect(service.selectedWard()?.fullName).toBe('Pavithra Umapathy');
  });

  it('selectWard ignores a studentId that is not one of the caller\'s own wards', () => {
    permissionService = { loaded: vi.fn(() => true), hasAny: vi.fn(() => true) };
    const service = setup();

    service.selectWard(999);

    expect(service.selectedWardId()).toBe(45);
  });

  it('only calls GET /guardian/wards once even if re-checked after loading', () => {
    permissionService = { loaded: vi.fn(() => true), hasAny: vi.fn(() => true) };
    setup();
    TestBed.tick();
    TestBed.tick();

    expect(guardianService.myWards).toHaveBeenCalledTimes(1);
  });
});
