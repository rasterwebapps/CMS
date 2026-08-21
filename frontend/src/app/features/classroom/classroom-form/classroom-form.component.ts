import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClassroomService } from '../classroom.service';
import { ClassroomRequest } from '../classroom.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
import { CmsRoomPickerComponent } from '../../../shared/room-picker/room-picker.component';
import { RoomPurposeCategoryService } from '../../hostel/room-purpose-category/room-purpose-category.service';
import { Room } from '../../hostel/campus-infrastructure/campus-infrastructure.model';

@Component({
  selector: 'app-classroom-form',
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
  templateUrl: './classroom-form.component.html',
  styleUrl: './classroom-form.component.scss',
})
export class ClassroomFormComponent implements OnInit {
  private readonly fb               = inject(FormBuilder);
  private readonly route            = inject(ActivatedRoute);
  private readonly router           = inject(Router);
  private readonly classroomService = inject(ClassroomService);
  private readonly toast            = inject(ToastService);
  private readonly destroyRef       = inject(DestroyRef);
  private readonly http             = inject(HttpClient);
  private readonly roomPurposeCategoryService = inject(RoomPurposeCategoryService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Classroom');

  protected readonly previewName = signal('');
  protected readonly previewBuilding = signal('');
  protected readonly previewRoomNumber = signal('');

  /** Classrooms only ever link to an Academic-purpose Room — matched by the category's stable
   *  `code`, not name or id, so it keeps working even if an admin renames "Academic" in Room
   *  Purpose Categories. Mirrors Capacity Planner's own loadAcademicRooms(). No manual category
   *  picker here: a classroom linked to e.g. a Residential-purpose room doesn't mean anything. */
  protected readonly academicCategoryId = signal<number | null>(null);
  protected selectedRoomId: number | null = null;
  protected readonly currentRoomLabel = signal<string | null>(null);
  /** This classroom's own already-linked room, so its picker doesn't exclude it as "taken." */
  protected keepRoomId: number | null = null;

  private classroomId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:       ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(255), noConsecutiveSpaces()]],
    building:   ['', [Validators.maxLength(255)]],
    roomNumber: ['', [Validators.maxLength(255)]],
    capacity:   [null],
    allowsConcurrentSharing: [false],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewBuilding.set((v.building ?? '').trim());
        this.previewRoomNumber.set((v.roomNumber ?? '').trim());
      });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.classroomId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Classroom');
      this.loadClassroom();
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

  /** Once linked, this classroom's capacity is the physical room's capacity, full stop — the
   *  backend derives and enforces the same rule (ClassroomService.resolveCapacity), so locking it
   *  here is a UX mirror of a real server-side rule, not just a client-side nicety. Unlinking
   *  clears the field rather than leaving a stale auto-filled number sitting there looking
   *  manually entered. */
  protected onSelectedRoomChange(room: Room | null): void {
    const capacityCtrl = this.form.get('capacity');
    if (room) {
      capacityCtrl?.setValue(room.capacity ?? null);
      capacityCtrl?.disable();
    } else {
      capacityCtrl?.enable();
      capacityCtrl?.setValue(null);
    }
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/classrooms/name-exists`, () => this.classroomId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: ClassroomRequest = {
      name:       (this.form.value.name ?? '').trim(),
      building:   this.form.value.building?.trim() || undefined,
      roomNumber: this.form.value.roomNumber?.trim() || undefined,
      capacity:   this.form.get('capacity')?.value ?? undefined,
      roomId:     this.selectedRoomId ?? undefined,
      allowsConcurrentSharing: this.form.value.allowsConcurrentSharing ?? false,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.classroomService.update(this.classroomId!, request)
      : this.classroomService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Classroom updated successfully' : 'Classroom created successfully');
        this.saving.set(false);
        void this.router.navigate(['/classrooms']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update classroom' : 'Failed to create classroom'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', building: 'Building', roomNumber: 'Room Number', capacity: 'Capacity',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), ClassroomFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadClassroom(): void {
    if (!this.classroomId) return;
    this.loading.set(true);
    this.classroomService.getById(this.classroomId).subscribe({
      next: (c) => {
        this.form.patchValue({
          name: c.name, building: c.building || '', roomNumber: c.roomNumber || '', capacity: c.capacity ?? null,
          allowsConcurrentSharing: c.allowsConcurrentSharing ?? false,
        });
        this.selectedRoomId = c.roomId ?? null;
        this.keepRoomId = c.roomId ?? null;
        this.currentRoomLabel.set(c.roomLabel ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load classroom');
        void this.router.navigate(['/classrooms']);
      },
    });
  }
}
