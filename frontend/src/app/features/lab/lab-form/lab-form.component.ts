import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LabService } from '../lab.service';
import { LabRequest, LAB_TYPES, LAB_STATUSES } from '../lab.model';
import { SpecialityService } from '../../speciality/speciality.service';
import { Speciality } from '../../speciality/speciality.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';
import { CmsRoomPickerComponent } from '../../../shared/room-picker/room-picker.component';
import { RoomPurposeCategoryService } from '../../hostel/room-purpose-category/room-purpose-category.service';
import { Room } from '../../hostel/campus-infrastructure/campus-infrastructure.model';
import { SubjectService } from '../../subject/subject.service';

@Component({
  selector: 'app-lab-form',
  standalone: true,
  imports: [
    RouterLink, CmsTourButtonComponent,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
    CmsRoomPickerComponent,
  ],
  templateUrl: './lab-form.component.html',
  styleUrl: './lab-form.component.scss',
})
export class LabFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labService = inject(LabService);
  private readonly subjectService = inject(SubjectService);
  private readonly specialityService = inject(SpecialityService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly roomPurposeCategoryService = inject(RoomPurposeCategoryService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Lab');
  protected readonly specialities = signal<Speciality[]>([]);

  /** Labs only ever link to an Academic-purpose Room — matched by the category's stable `code`,
   *  not name or id, so it survives an admin renaming "Academic" in Room Purpose Categories.
   *  Mirrors the Classroom form and Capacity Planner's own loadAcademicRooms(). No manual category
   *  picker: a lab linked to e.g. a Residential-purpose room doesn't mean anything. */
  protected readonly academicCategoryId = signal<number | null>(null);
  protected selectedRoomId: number | null = null;
  protected readonly currentRoomLabel = signal<string | null>(null);
  /** This lab's own already-linked room, so its picker doesn't exclude it as "taken." */
  protected keepRoomId: number | null = null;
  /** See `ClinicalVenueFormComponent.hadRoomLinked` — same picker-echo-vs-genuine-unlink guard. */
  private hadRoomLinked = false;

  protected readonly labTypes = LAB_TYPES;
  protected readonly labStatuses = LAB_STATUSES;

  // Preview signals
  protected readonly previewName     = signal('');
  protected readonly previewType     = signal<string>('');
  protected readonly previewSpecialityId = signal<number | null>(null);
  protected readonly previewBuilding = signal('');
  protected readonly previewRoom     = signal('');
  protected readonly previewCapacity = signal<number | null>(null);
  protected readonly previewStatus   = signal<string>('ACTIVE');
  protected readonly previewLocation = computed(() => {
    const b = this.previewBuilding();
    const r = this.previewRoom();
    if (b && r) return `${b}, Room ${r}`;
    return b || (r ? `Room ${r}` : '');
  });
  protected readonly previewTypeLabel = computed(() => LAB_TYPES.find(t => t.value === this.previewType())?.label ?? '');
  protected readonly previewSpecialityName = computed(() => {
    const id = this.previewSpecialityId();
    if (!id) return '';
    return this.specialities().find(d => d.id === id)?.name ?? '';
  });
  protected readonly previewStatusLabel = computed(() => LAB_STATUSES.find(s => s.value === this.previewStatus())?.label ?? '');

  protected readonly TIPS: CmsTip[] = [
    { icon: 'category',   title: 'Lab Type',  subtitle: 'Choose the closest matching type so equipment requests are routed correctly.' },
    { icon: 'place',      title: 'Location',  subtitle: 'Building + Room helps faculty and students find the lab quickly.' },
    { icon: 'event_seat', title: 'Capacity',  subtitle: 'Used for scheduling — the lab cannot be booked beyond this seat count.' },
  ];

  private labId: number | null = null;
  /** See `ClinicalVenueFormComponent.linkSubjectIds` — same `linkSubjectIds` query-param mechanism. */
  private linkSubjectIds: number[] = [];

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100), trimmedMinLength(2), noConsecutiveSpaces()]],
    labType: ['', [Validators.required]],
    specialityId: ['', [Validators.required]],
    building: ['', [Validators.maxLength(100)]],
    roomNumber: ['', [Validators.maxLength(50)]],
    capacity: [1, [Validators.required, Validators.min(1), Validators.max(500)]],
    status: ['ACTIVE', [Validators.required]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewType.set(v.labType ?? '');
        this.previewSpecialityId.set(v.specialityId ? Number(v.specialityId) : null);
        this.previewBuilding.set((v.building ?? '').trim());
        this.previewRoom.set((v.roomNumber ?? '').trim());
        // .get('capacity')?.value directly, not the destructured v -- a disabled control (locked
        // capacity, room linked) is excluded from the FormGroup's own aggregate value.
        const capacityValue = this.form.get('capacity')?.value;
        this.previewCapacity.set(capacityValue ? Number(capacityValue) : null);
        this.previewStatus.set(v.status ?? 'ACTIVE');
      });
  }

  ngOnInit(): void {
    this.loadSpecialities();
    this.roomPurposeCategoryService.getAll(true).subscribe({
      next: (categories) => {
        const academic = categories.find((c) => c.code === 'ACADEMIC');
        this.academicCategoryId.set(academic?.id ?? null);
      },
      error: () => this.toast.error('Failed to load room purpose categories'),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.labId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Lab');
      this.loadLab();
    } else {
      const raw = this.route.snapshot.queryParamMap.get('linkSubjectIds');
      this.linkSubjectIds = raw ? raw.split(',').map(Number).filter((n) => !isNaN(n)) : [];
    }
  }

  /** Once linked, this lab's capacity is the physical room's capacity, full stop — the backend
   *  derives and enforces the same rule (LabService.resolveCapacity). Unlinking clears the field
   *  rather than leaving a stale auto-filled number sitting there looking manually entered. */
  /** See `ClinicalVenueFormComponent.onSelectedRoomChange`'s javadoc — the picker's own
   *  load-completion echo (fires with `room: null` on every edit-page load for a lab with no
   *  linked room) must not be treated as a genuine unlink, or it silently blanks the real,
   *  server-loaded capacity. */
  protected onSelectedRoomChange(room: Room | null): void {
    const capacityCtrl = this.form.get('capacity');
    if (room) {
      capacityCtrl?.setValue(room.capacity ?? null);
      capacityCtrl?.disable();
      this.hadRoomLinked = true;
    } else if (this.hadRoomLinked) {
      capacityCtrl?.enable();
      capacityCtrl?.setValue(null);
      this.hadRoomLinked = false;
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: LabRequest = {
      name: (this.form.value.name ?? '').trim(),
      labType: this.form.value.labType,
      specialityId: Number(this.form.value.specialityId),
      building: this.form.value.building?.trim() || undefined,
      roomNumber: this.form.value.roomNumber?.trim() || undefined,
      capacity: Number(this.form.get('capacity')?.value),
      status: this.form.value.status,
      roomId: this.selectedRoomId ?? undefined,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.labService.update(this.labId!, request)
      : this.labService.create(request);

    operation$.subscribe({
      next: (lab) => {
        const message = this.isEditMode() ? 'Lab updated successfully' : 'Lab created successfully';
        this.toast.success(message);
        this.linkToAffectedSubjects(lab.id);
        void this.router.navigate(['/labs']);
      },
      error: (err) => {
        const message = this.isEditMode() ? 'Failed to update lab' : 'Failed to create lab';
        this.toast.error(err?.error?.message ?? message);
        this.saving.set(false);
      },
    });
  }

  /** Best-effort — see `ClinicalVenueFormComponent.linkToAffectedSubjects`. */
  private linkToAffectedSubjects(labId: number): void {
    if (this.linkSubjectIds.length === 0) return;
    this.subjectService.addEligibleVenue(this.linkSubjectIds, 'LAB', labId).subscribe({
      next: () => this.toast.success(`Also linked as an eligible venue for ${this.linkSubjectIds.length} subject(s)`),
      error: () => this.toast.error('Lab created, but linking it to the affected subject(s) failed — add it manually via Subjects'),
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Lab Name',
    building: 'Building',
    roomNumber: 'Room Number',
    capacity: 'Capacity',
    labType: 'Lab Type',
    specialityId: 'Speciality',
    status: 'Status',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), LabFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadSpecialities(): void {
    this.specialityService.getAll().subscribe({
      next: (specialities) => {
        this.specialities.set(specialities);
      },
      error: () => {
        this.toast.error('Failed to load specialities');
      },
    });
  }

  private loadLab(): void {
    if (!this.labId) return;

    this.loading.set(true);
    this.labService.getById(this.labId).subscribe({
      next: (lab) => {
        this.form.patchValue({
          name: lab.name,
          labType: lab.labType,
          specialityId: lab.speciality.id,
          building: lab.building || '',
          roomNumber: lab.roomNumber || '',
          capacity: lab.capacity,
          status: lab.status,
        });
        this.selectedRoomId = lab.roomId ?? null;
        this.keepRoomId = lab.roomId ?? null;
        this.hadRoomLinked = lab.roomId != null;
        this.currentRoomLabel.set(lab.roomLabel ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load lab');
        void this.router.navigate(['/labs']);
      },
    });
  }
}
