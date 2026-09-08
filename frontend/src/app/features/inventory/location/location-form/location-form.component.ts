import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { InventoryLocationService } from '../inventory-location.service';
import { InventoryLocationRequest, LocationRole } from '../inventory-location.model';
import { CampusInfrastructureService } from '../../../hostel/campus-infrastructure/campus-infrastructure.service';
import { Zone, Room } from '../../../hostel/campus-infrastructure/campus-infrastructure.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-location-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
  ],
  templateUrl: './location-form.component.html',
  styleUrl: './location-form.component.scss',
})
export class LocationFormComponent implements OnInit {
  private readonly fb                 = inject(FormBuilder);
  private readonly route              = inject(ActivatedRoute);
  private readonly router             = inject(Router);
  private readonly locationService    = inject(InventoryLocationService);
  private readonly campusService      = inject(CampusInfrastructureService);
  private readonly toast              = inject(ToastService);
  private readonly destroyRef         = inject(DestroyRef);
  private readonly http               = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Inventory Location');
  protected readonly zones      = signal<Zone[]>([]);
  protected readonly rooms      = signal<Room[]>([]);
  protected readonly roomsLoading = signal(false);

  protected readonly previewName = signal('');
  protected readonly previewRoom = signal('');

  protected readonly roles: { value: LocationRole; label: string }[] = [
    { value: 'STORE', label: 'Store' },
    { value: 'REQUESTING_POINT', label: 'Requesting Point' },
    { value: 'BOTH', label: 'Both' },
  ];

  private locationId: number | null = null;
  // The Room already selected when editing — kept out of the "select a zone first" gate so
  // loading an existing location can pre-select its zone before the room list has even loaded.
  private pendingRoomId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    zoneId:       [null as number | null, [Validators.required]],
    roomId:       [null as number | null, [Validators.required]],
    virtualName:  ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(150), noConsecutiveSpaces()]],
    locationRole: ['STORE' as LocationRole, [Validators.required]],
    description:  ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.virtualName ?? '').trim());
        const room = this.rooms().find(r => r.id === v.roomId);
        this.previewRoom.set(room ? `${room.zoneName} · ${room.roomNumber}` : '');
      });

    this.form.get('zoneId')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((zoneId) => {
      this.loadRooms(zoneId);
    });
  }

  ngOnInit(): void {
    this.campusService.getAllActiveZones().subscribe({ next: (z) => this.zones.set(z) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.locationId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Inventory Location');
      this.loadLocation();
    }
    this.setupUniquenessValidator();
  }

  private setupUniquenessValidator(): void {
    const nameCtrl = this.form.get('virtualName');
    nameCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/locations/name-exists`, () => this.locationId),
    );
    nameCtrl?.updateValueAndValidity({ emitEvent: false });
  }

  private loadRooms(zoneId: number | null): void {
    this.rooms.set([]);
    if (zoneId == null) return;
    this.roomsLoading.set(true);
    this.campusService.getRoomsByZone(zoneId, true).subscribe({
      next: (rooms) => {
        this.rooms.set(rooms);
        this.roomsLoading.set(false);
        if (this.pendingRoomId != null && rooms.some(r => r.id === this.pendingRoomId)) {
          this.form.get('roomId')?.setValue(this.pendingRoomId, { emitEvent: true });
          this.pendingRoomId = null;
        }
      },
      error: () => { this.toast.error('Failed to load rooms for this zone'); this.roomsLoading.set(false); },
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: InventoryLocationRequest = {
      roomId:       this.form.value.roomId,
      virtualName:  (this.form.value.virtualName ?? '').trim(),
      locationRole: this.form.value.locationRole,
      description:  this.form.value.description?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.locationService.update(this.locationId!, request)
      : this.locationService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Inventory location updated successfully' : 'Inventory location created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/locations']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update inventory location' : 'Failed to create inventory location'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    zoneId: 'Zone', roomId: 'Room', virtualName: 'Virtual name', locationRole: 'Role', description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), LocationFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadLocation(): void {
    if (!this.locationId) return;
    this.loading.set(true);
    this.locationService.getById(this.locationId).subscribe({
      next: (loc) => {
        this.pendingRoomId = loc.roomId;
        this.form.patchValue({
          zoneId: loc.zoneId,
          virtualName: loc.virtualName,
          locationRole: loc.locationRole,
          description: loc.description || '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load inventory location');
        void this.router.navigate(['/inventory/locations']);
      },
    });
  }
}
