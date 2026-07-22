import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
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
  @Output() saved = new EventEmitter<RoomPreference>();

  protected readonly roomTypes = signal<HostelRoomType[]>([]);
  protected readonly zones = signal<Zone[]>([]);
  protected readonly preferredRoomTypeId = signal<number | null>(null);
  protected readonly preferredZoneId = signal<number | null>(null);
  protected readonly remarks = signal<string>('');
  protected readonly loading = signal(false);

  private existingPreferenceId: number | null = null;

  constructor() {
    this.hostelRoomTypeService.getAll(true).subscribe((types) => this.roomTypes.set(types));
    this.campusInfrastructureService.getAllActiveZones().subscribe((zones) => this.zones.set(zones));
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
