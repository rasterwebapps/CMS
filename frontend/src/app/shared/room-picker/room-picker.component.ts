import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CampusInfrastructureService } from '../../features/hostel/campus-infrastructure/campus-infrastructure.service';
import { Room } from '../../features/hostel/campus-infrastructure/campus-infrastructure.model';

/**
 * Selectable, campus-wide Room search filtered by purpose (and optionally sub-type/min-capacity) —
 * spans every Branch, including a hospital Branch hosting clinical venues. Used both by venue
 * pickers (Cohort Room Allocation) and by the Classroom/Lab/ClinicalVenue master forms that link a
 * virtual venue to its physical Room. Plain two-way `[(selectedRoomId)]` binding rather than a full
 * ControlValueAccessor, matching this codebase's existing plain-select form patterns.
 *
 * Usage:
 *   <cms-room-picker [purposeCategoryId]="12" [minCapacity]="60" [(selectedRoomId)]="theoryRoomId" />
 */
@Component({
  selector: 'cms-room-picker',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './room-picker.component.html',
})
export class CmsRoomPickerComponent implements OnChanges {
  private readonly campusInfrastructureService = inject(CampusInfrastructureService);

  @Input() purposeCategoryId: number | null = null;
  @Input() subTypeId: number | null = null;
  @Input() minCapacity: number | null = null;
  /** The one Room this picker's own owning venue is already linked to, if editing — kept in the
   *  list even though it's "taken," so a venue never loses its own current selection. */
  @Input() keepRoomId: number | null = null;
  /** 'CLASSROOM' | 'LAB' | 'CLINICAL' — excludes a Room already linked to another active venue of
   *  this SAME type only (a Room used by a Classroom is still offered when picking for a new Lab;
   *  they're allowed to share a physical space). Omit to skip exclusion entirely. */
  @Input() venueType: string | null = null;
  @Input() selectedRoomId: number | null = null;
  @Input() label = 'Room';
  @Input() disabled = false;

  @Output() selectedRoomIdChange = new EventEmitter<number | null>();
  @Output() roomsLoaded = new EventEmitter<Room[]>();
  /** The full selected Room (or null), for a consumer that needs more than just the id — e.g. its
   *  capacity. Fires both when the user picks a room and once the room list finishes (re)loading,
   *  so a pre-set selectedRoomId (edit mode) still resolves to its Room as soon as it's known,
   *  even though nothing was "changed" by the user at that point. */
  @Output() selectedRoomChange = new EventEmitter<Room | null>();

  protected readonly rooms = signal<Room[]>([]);
  protected readonly loading = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['purposeCategoryId'] || changes['subTypeId'] || changes['minCapacity'] || changes['keepRoomId'] || changes['venueType']) {
      this.loadRooms();
    }
  }

  private loadRooms(): void {
    if (this.purposeCategoryId == null) {
      this.rooms.set([]);
      return;
    }
    this.loading.set(true);
    this.campusInfrastructureService
      .getRoomsByPurpose(this.purposeCategoryId, this.subTypeId, this.minCapacity, this.keepRoomId, this.venueType)
      .subscribe({
        next: (rooms) => {
          this.rooms.set(rooms);
          this.loading.set(false);
          this.roomsLoaded.emit(rooms);
          this.emitSelectedRoom();
        },
        error: () => {
          this.rooms.set([]);
          this.loading.set(false);
        },
      });
  }

  protected onSelectionChange(): void {
    this.selectedRoomIdChange.emit(this.selectedRoomId);
    this.emitSelectedRoom();
  }

  private emitSelectedRoom(): void {
    const room = this.selectedRoomId != null
      ? this.rooms().find((r) => r.id === this.selectedRoomId) ?? null
      : null;
    this.selectedRoomChange.emit(room);
  }

  protected roomLabel(room: Room): string {
    const subType = room.subTypeName ? ` [${room.subTypeName}]` : '';
    return `${room.zoneName} · ${room.roomNumber}${subType}${room.capacity != null ? ` (cap ${room.capacity})` : ''}`;
  }
}
