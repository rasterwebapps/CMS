import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';

import { HostelRoomTypeService } from '../../hostel-room-type/hostel-room-type.service';
import { HostelRoomType } from '../../hostel-room-type/hostel-room-type.model';
import { CampusInfrastructureService } from '../../campus-infrastructure/campus-infrastructure.service';
import { Zone } from '../../campus-infrastructure/campus-infrastructure.model';
import { RoomPreferenceService } from '../room-preference.service';
import { RoomPreference, RoomPreferenceRequest } from '../room-preference.model';

/**
 * Non-binding room-preference picker (R2-4.1.3), embedded directly in the Enquiry and Admission
 * forms rather than living behind its own navigation — captures a preferred HostelRoomType and
 * optional Zone. Owns its own load/save calls against RoomPreferenceService so the much larger
 * host forms don't need their FormGroup/submit payload restructured; the host only needs to call
 * `persist()` after its own entity save succeeds (an enquiryId/studentId must exist first).
 */
@Component({
  selector: 'app-room-preference-picker',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './room-preference-picker.component.html',
  styleUrl: './room-preference-picker.component.scss',
})
export class RoomPreferencePickerComponent implements OnChanges {
  private readonly hostelRoomTypeService = inject(HostelRoomTypeService);
  private readonly campusInfrastructureService = inject(CampusInfrastructureService);
  private readonly roomPreferenceService = inject(RoomPreferenceService);

  @Input() enquiryId: number | null = null;
  @Input() studentId: number | null = null;
  /** Student's gender, if known yet — narrows the zone list to gender-compatible zones
   *  (a zone with no genderRestriction is unrestricted and shown regardless of gender). */
  @Input() set gender(value: 'MALE' | 'FEMALE' | 'OTHER' | null | undefined) {
    this.genderSignal.set(value ?? null);
  }
  get gender(): 'MALE' | 'FEMALE' | 'OTHER' | null {
    return this.genderSignal();
  }
  @Output() saved = new EventEmitter<RoomPreference>();

  private readonly genderSignal = signal<'MALE' | 'FEMALE' | 'OTHER' | null>(null);
  protected readonly roomTypes = signal<HostelRoomType[]>([]);
  protected readonly zones = signal<Zone[]>([]);
  protected readonly filteredZones = computed(() => {
    const gender = this.genderSignal();
    return this.zones().filter((z) => this.isZoneCompatible(z, gender));
  });
  protected readonly preferredRoomTypeId = signal<number | null>(null);
  protected readonly preferredZoneId = signal<number | null>(null);
  protected readonly remarks = signal<string>('');
  protected readonly loading = signal(false);

  private existingPreferenceId: number | null = null;

  constructor() {
    this.hostelRoomTypeService.getAll(true).subscribe((types) => this.roomTypes.set(types));
    this.campusInfrastructureService.getAllActiveZones().subscribe((zones) => this.zones.set(zones));

    // If gender becomes known (or changes) after a zone was already picked, and that zone is
    // no longer gender-compatible, clear the stale selection rather than silently keeping it.
    effect(() => {
      const compatible = this.filteredZones();
      const current = this.preferredZoneId();
      if (current !== null && !compatible.some((z) => z.id === current)) {
        this.preferredZoneId.set(null);
      }
    });
  }

  /** A zone with no genderRestriction is unrestricted (open to all genders, including OTHER).
   *  MALE only matches BOYS zones, FEMALE only matches GIRLS zones; OTHER has no matching
   *  restriction value, so it can only use unrestricted zones. */
  private isZoneCompatible(zone: Zone, gender: 'MALE' | 'FEMALE' | 'OTHER' | null): boolean {
    if (zone.genderRestriction === null) return true;
    if (gender === 'MALE') return zone.genderRestriction === 'BOYS';
    if (gender === 'FEMALE') return zone.genderRestriction === 'GIRLS';
    return false;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['enquiryId'] || changes['studentId']) {
      this.loadExisting();
    }
  }

  private loadExisting(): void {
    this.existingPreferenceId = null;
    this.preferredRoomTypeId.set(null);
    this.preferredZoneId.set(null);
    this.remarks.set('');

    const lookup$ = this.studentId
      ? this.roomPreferenceService.getByStudentId(this.studentId)
      : this.enquiryId
        ? this.roomPreferenceService.getByEnquiryId(this.enquiryId)
        : of(null);

    this.loading.set(true);
    lookup$.subscribe((preference) => {
      this.loading.set(false);
      if (!preference) return;
      this.existingPreferenceId = preference.id;
      this.preferredRoomTypeId.set(preference.preferredRoomTypeId);
      this.preferredZoneId.set(preference.preferredZoneId);
      this.remarks.set(preference.remarks ?? '');
    });
  }

  /** Called by the host form after its own entity (enquiry/student) has been saved. Skips
   *  silently if no room type was picked — the preference is optional and can be added later. */
  persist(enquiryId?: number, studentId?: number): Observable<RoomPreference | null> {
    const roomTypeId = this.preferredRoomTypeId();
    if (!roomTypeId) return of(null);

    const request: RoomPreferenceRequest = {
      enquiryId: enquiryId ?? this.enquiryId ?? undefined,
      studentId: studentId ?? this.studentId ?? undefined,
      preferredRoomTypeId: roomTypeId,
      preferredZoneId: this.preferredZoneId() ?? undefined,
      remarks: this.remarks().trim() || undefined,
    };
    const op$ = this.existingPreferenceId
      ? this.roomPreferenceService.update(this.existingPreferenceId, request)
      : this.roomPreferenceService.create(request);
    return op$;
  }

  protected onRoomTypeChange(value: string): void {
    this.preferredRoomTypeId.set(value ? Number(value) : null);
  }

  protected onZoneChange(value: string): void {
    this.preferredZoneId.set(value ? Number(value) : null);
  }
}
