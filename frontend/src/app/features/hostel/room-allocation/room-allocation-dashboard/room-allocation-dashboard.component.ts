import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged, Subject, switchMap } from 'rxjs';

import { RoomAllocationService } from '../room-allocation.service';
import { HostelRoomOccupancy, RoomAllocationRequest } from '../room-allocation.model';
import { HostelRoomTypeService } from '../../hostel-room-type/hostel-room-type.service';
import { HostelRoomType } from '../../hostel-room-type/hostel-room-type.model';
import { CampusInfrastructureService } from '../../campus-infrastructure/campus-infrastructure.service';
import { Zone } from '../../campus-infrastructure/campus-infrastructure.model';
import { StudentService } from '../../../student/student.service';
import { Student } from '../../../student/student.model';
import { CmsFlyoutPanelComponent } from '../../../../shared/flyout-panel/flyout-panel.component';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsCapacityMeterComponent } from '../../../../shared/capacity-meter/capacity-meter.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { TourService } from '../../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../../shared/tour/tour-button.component';
import { ROOM_ALLOCATION_DASHBOARD_TOUR, ROOM_ALLOCATION_DASHBOARD_FLOW_MAP } from '../../../../shared/tour/tours/hostel-management.tours';

@Component({
  selector: 'app-room-allocation-dashboard',
  standalone: true,
  imports: [FormsModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule, CmsFlyoutPanelComponent, CmsEmptyStateComponent, CmsCapacityMeterComponent, CmsTourButtonComponent],
  templateUrl: './room-allocation-dashboard.component.html',
  styleUrl: './room-allocation-dashboard.component.scss',
})
export class RoomAllocationDashboardComponent implements OnInit {
  private readonly roomAllocationService = inject(RoomAllocationService);
  private readonly hostelRoomTypeService = inject(HostelRoomTypeService);
  private readonly campusInfrastructureService = inject(CampusInfrastructureService);
  private readonly studentService = inject(StudentService);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly rooms = signal<HostelRoomOccupancy[]>([]);
  protected readonly roomTypes = signal<HostelRoomType[]>([]);
  protected readonly zones = signal<Zone[]>([]);

  protected readonly search = signal('');
  protected readonly roomTypeFilter = signal<number | null>(null);
  protected readonly zoneFilter = signal<number | null>(null);
  // 'MALE'/'FEMALE' narrow to BOYS/GIRLS zones respectively; unrestricted (null) zones match
  // every gender filter since they're open to anyone. 'ALL' applies no gender filter.
  protected readonly genderFilter = signal<'ALL' | 'MALE' | 'FEMALE'>('ALL');

  protected readonly filteredRooms = computed(() => {
    const term = this.search().trim().toLowerCase();
    const roomTypeId = this.roomTypeFilter();
    const zoneId = this.zoneFilter();
    const gender = this.genderFilter();
    return this.rooms().filter((r) => {
      if (roomTypeId && r.roomTypeId !== roomTypeId) return false;
      if (zoneId && r.zoneId !== zoneId) return false;
      if (gender === 'MALE' && r.genderRestriction !== null && r.genderRestriction !== 'BOYS') return false;
      if (gender === 'FEMALE' && r.genderRestriction !== null && r.genderRestriction !== 'GIRLS') return false;
      if (!term) return true;
      return r.roomNumber.toLowerCase().includes(term)
        || r.occupants.some((o) => o.studentName.toLowerCase().includes(term));
    });
  });

  // ── Allocate flyout ────────────────────────────────────────────────────
  protected readonly targetRoom = signal<HostelRoomOccupancy | null>(null);
  protected readonly saving = signal(false);
  protected readonly studentQuery = signal('');
  protected readonly studentResults = signal<Student[]>([]);
  protected readonly selectedStudent = signal<Student | null>(null);
  protected readonly startDate = signal<string>(new Date().toISOString().split('T')[0]);
  protected readonly endDate = signal<string>('');
  protected readonly remarks = signal<string>('');
  private readonly studentQuery$ = new Subject<string>();

  ngOnInit(): void {
    this.tourService.register('room-allocation-dashboard', ROOM_ALLOCATION_DASHBOARD_TOUR);
    this.tourService.registerFlowMap('room-allocation-dashboard', ROOM_ALLOCATION_DASHBOARD_FLOW_MAP);

    this.refresh();
    this.hostelRoomTypeService.getAll(true).subscribe((types) => this.roomTypes.set(types));
    this.campusInfrastructureService.getAllActiveZones().subscribe((zones) => this.zones.set(zones));

    this.studentQuery$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((q) => q.length >= 3
        ? this.studentService.getExplorer({ studentType: 'HOSTELER', search: q, size: 10 })
        : []),
    ).subscribe({
      next: (page) => this.studentResults.set(page.content),
    });
  }

  protected refresh(): void {
    this.loading.set(true);
    this.roomAllocationService.getOccupancy().subscribe({
      next: (rooms) => { this.rooms.set(rooms); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load room occupancy'); this.loading.set(false); },
    });
  }

  protected openAllocate(room: HostelRoomOccupancy): void {
    this.targetRoom.set(room);
    this.selectedStudent.set(null);
    this.studentQuery.set('');
    this.studentResults.set([]);
    this.startDate.set(new Date().toISOString().split('T')[0]);
    this.endDate.set('');
    this.remarks.set('');
  }

  protected closeAllocate(): void {
    this.targetRoom.set(null);
  }

  protected onStudentQueryChange(value: string): void {
    this.studentQuery.set(value);
    this.selectedStudent.set(null);
    this.studentQuery$.next(value);
  }

  protected selectStudent(student: Student): void {
    this.selectedStudent.set(student);
    this.studentQuery.set(student.fullName);
    this.studentResults.set([]);
  }

  protected submitAllocation(): void {
    const room = this.targetRoom();
    const student = this.selectedStudent();
    if (!room || !student || !this.startDate()) return;

    const request: RoomAllocationRequest = {
      studentId: student.id,
      hostelRoomId: room.hostelRoomId,
      startDate: this.startDate(),
      endDate: this.endDate() || undefined,
      remarks: this.remarks().trim() || undefined,
    };
    this.saving.set(true);
    this.roomAllocationService.create(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Room allocated');
        this.closeAllocate();
        this.refresh();
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to allocate room');
      },
    });
  }

  protected cancelOccupant(room: HostelRoomOccupancy, allocationId: number, studentName: string): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Allocation',
        message: `Cancel ${studentName}'s allocation to room ${room.roomNumber}? This frees up the bed immediately.`,
        confirmText: 'Cancel Allocation',
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.roomAllocationService.updateStatus(allocationId, 'CANCELLED').subscribe({
        next: () => { this.toast.success('Allocation cancelled'); this.refresh(); },
        error: () => this.toast.error('Failed to cancel allocation'),
      });
    });
  }

}
