import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClinicalVenueService } from '../clinical-venue.service';
import { ClinicalVenueRequest } from '../clinical-venue.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
import { CmsRoomPickerComponent } from '../../../shared/room-picker/room-picker.component';
import { RoomPurposeCategoryService } from '../../hostel/room-purpose-category/room-purpose-category.service';
import { Room } from '../../hostel/campus-infrastructure/campus-infrastructure.model';
import { SubjectService } from '../../subject/subject.service';

@Component({
  selector: 'app-clinical-venue-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsRoomPickerComponent,
  ],
  templateUrl: './clinical-venue-form.component.html',
  styleUrl: './clinical-venue-form.component.scss',
})
export class ClinicalVenueFormComponent implements OnInit {
  private readonly fb                 = inject(FormBuilder);
  private readonly route              = inject(ActivatedRoute);
  private readonly router             = inject(Router);
  private readonly clinicalVenueService = inject(ClinicalVenueService);
  private readonly subjectService     = inject(SubjectService);
  private readonly toast              = inject(ToastService);
  private readonly destroyRef         = inject(DestroyRef);
  private readonly http               = inject(HttpClient);
  private readonly roomPurposeCategoryService = inject(RoomPurposeCategoryService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Clinical Venue');

  protected readonly previewName         = signal('');
  protected readonly previewHospitalName = signal('');
  protected readonly previewDepartment   = signal('');

  /** For an on-campus (internal) clinical/skills space, the physical Room must be Academic-purpose
   *  — matched by the category's stable `code`, not name or id, so it survives an admin renaming
   *  "Academic" in Room Purpose Categories. Mirrors the Classroom/Lab forms and Capacity Planner's
   *  own loadAcademicRooms(). Left null (no room link at all) for an off-campus/external hospital
   *  posting, which Hospital Name/Department already describe — the picker stays optional either
   *  way, this only fixes *which* rooms it's allowed to show when it is used. */
  protected readonly academicCategoryId = signal<number | null>(null);
  protected selectedRoomId: number | null = null;
  protected readonly currentRoomLabel = signal<string | null>(null);
  /** This venue's own already-linked room, so its picker doesn't exclude it as "taken." */
  protected keepRoomId: number | null = null;
  /** Whether a room is (or, before a genuine unlink, was) actually linked — see {@link
   *  onSelectedRoomChange}'s javadoc for why this exists. */
  private hadRoomLinked = false;

  private venueId: number | null = null;
  /** From the `linkSubjectIds` query param — set only when arriving via the Lab/Clinical
   *  venue-capacity checklist's "Add a second venue" remedy (see `GlobalAutoScheduleReportFlyoutComponent
   *  .venueNewQueryParams`). Empty on a normal "create a venue" visit. */
  private linkSubjectIds: number[] = [];

  protected readonly form: FormGroup = this.fb.group({
    name:         ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(255), noConsecutiveSpaces()]],
    hospitalName: ['', [Validators.maxLength(255)]],
    department:   ['', [Validators.maxLength(255)]],
    capacity:     [null, [Validators.min(1)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewHospitalName.set((v.hospitalName ?? '').trim());
        this.previewDepartment.set((v.department ?? '').trim());
      });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.venueId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Clinical Venue');
      this.loadVenue();
    } else {
      const raw = this.route.snapshot.queryParamMap.get('linkSubjectIds');
      this.linkSubjectIds = raw ? raw.split(',').map(Number).filter((n) => !isNaN(n)) : [];
    }
    this.setupUniquenessValidators();
    this.roomPurposeCategoryService.getAll(true).subscribe({
      next: (categories) => {
        const academic = categories.find((c) => c.code === 'ACADEMIC');
        this.academicCategoryId.set(academic?.id ?? null);
      },
      error: () => this.toast.error('Failed to load room purpose categories'),
    });
  }

  /** Only meaningful for an internal (on-campus) venue — once linked, this venue's capacity is
   *  the physical room's capacity, full stop, matching the backend's own derivation
   *  (ClinicalVenueService.resolveCapacity). Unlinking (or an external hospital posting that never
   *  links a room at all) leaves it a plain manual field, since there's no physical figure to
   *  derive from off-campus. */
  /** `CmsRoomPickerComponent.selectedRoomChange` fires both on a real user pick/unlink AND, purely
   *  as an echo, once the picker's own room list finishes (re)loading — including for an
   *  off-campus venue that never had a room to begin with, which fires with `room: null` on every
   *  edit-page load. Treating that echo the same as a genuine unlink used to blank this venue's
   *  real, server-loaded capacity the instant the room list resolved — invisibly, since the field
   *  just looks empty rather than obviously wrong — and Update would then persist the wipe. {@link
   *  hadRoomLinked} distinguishes "this is confirming there was never a room" (ignore) from "a room
   *  just got detached" (genuinely clear/re-enable). */
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

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/clinical-venues/name-exists`, () => this.venueId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: ClinicalVenueRequest = {
      name:         (this.form.value.name ?? '').trim(),
      hospitalName: this.form.value.hospitalName?.trim() || undefined,
      department:   this.form.value.department?.trim() || undefined,
      capacity:     this.form.get('capacity')?.value ?? undefined,
      roomId:       this.selectedRoomId ?? undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.clinicalVenueService.update(this.venueId!, request)
      : this.clinicalVenueService.create(request);

    op$.subscribe({
      next: (venue) => {
        this.toast.success(this.isEditMode() ? 'Clinical venue updated successfully' : 'Clinical venue created successfully');
        this.saving.set(false);
        this.linkToAffectedSubjects(venue.id);
        void this.router.navigate(['/clinical-venues']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update clinical venue' : 'Failed to create clinical venue'));
        this.saving.set(false);
      },
    });
  }

  /** Best-effort — a failure here doesn't undo the just-created venue, it only means the admin
   *  still has to go add it manually via Subjects (same as before this shortcut existed). */
  private linkToAffectedSubjects(venueId: number): void {
    if (this.linkSubjectIds.length === 0) return;
    this.subjectService.addEligibleVenue(this.linkSubjectIds, 'CLINICAL', venueId).subscribe({
      next: () => this.toast.success(`Also linked as an eligible venue for ${this.linkSubjectIds.length} subject(s)`),
      error: () => this.toast.error('Venue created, but linking it to the affected subject(s) failed — add it manually via Subjects'),
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', hospitalName: 'Hospital Name', department: 'Department', capacity: 'Capacity',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), ClinicalVenueFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadVenue(): void {
    if (!this.venueId) return;
    this.loading.set(true);
    this.clinicalVenueService.getById(this.venueId).subscribe({
      next: (v) => {
        this.form.patchValue({
          name: v.name,
          hospitalName: v.hospitalName || '',
          department: v.department || '',
          capacity: v.capacity ?? null,
        });
        this.selectedRoomId = v.roomId ?? null;
        this.keepRoomId = v.roomId ?? null;
        this.hadRoomLinked = v.roomId != null;
        this.currentRoomLabel.set(v.roomLabel ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load clinical venue');
        void this.router.navigate(['/clinical-venues']);
      },
    });
  }
}
