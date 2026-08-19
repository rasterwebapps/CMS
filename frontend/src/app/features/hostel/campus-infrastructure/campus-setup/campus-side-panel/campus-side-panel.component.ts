import { Component, Signal, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule, FormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { CampusInfrastructureService } from '../../campus-infrastructure.service';
import { Block, Branch, Floor, GenderRestriction, Organization, Room, Zone } from '../../campus-infrastructure.model';
import { HostelRoomTypeService } from '../../../hostel-room-type/hostel-room-type.service';
import { HostelRoomType } from '../../../hostel-room-type/hostel-room-type.model';
import { RoomPurposeCategoryService } from '../../../room-purpose-category/room-purpose-category.service';
import { RoomPurposeCategory } from '../../../room-purpose-category/room-purpose-category.model';
import { RoomSubTypeService } from '../../../room-sub-type/room-sub-type.service';
import { RoomSubType } from '../../../room-sub-type/room-sub-type.model';
import { FacultyService } from '../../../../faculty/faculty.service';
import { Faculty } from '../../../../faculty/faculty.model';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../../core/toast/toast.service';
import { environment } from '../../../../../../environments';
import { uniqueFieldValidator } from '../../../../../shared/validators/unique-field.validator';

export type CampusPanelLevel = 'organization' | 'branch' | 'block' | 'floor' | 'zone' | 'room';

interface AddOrganizationFormState {
  name: string;
  code: string;
  description: string;
  submitting: boolean;
  error: string | null;
}
const emptyAddOrganizationForm = (): AddOrganizationFormState => ({ name: '', code: '', description: '', submitting: false, error: null });

interface AddBranchFormState {
  name: string;
  code: string;
  description: string;
  submitting: boolean;
  error: string | null;
}
const emptyAddBranchForm = (): AddBranchFormState => ({ name: '', code: '', description: '', submitting: false, error: null });

interface AddBlockFormState {
  name: string;
  code: string;
  description: string;
  isHostel: boolean;
  genderRestriction: GenderRestriction | null;
  submitting: boolean;
  error: string | null;
}
const emptyAddBlockForm = (): AddBlockFormState =>
  ({ name: '', code: '', description: '', isHostel: false, genderRestriction: null, submitting: false, error: null });

interface AddFloorFormState {
  name: string;
  floorNumber: number;
  isHostel: boolean;
  genderRestriction: GenderRestriction | null;
  isBasement: boolean;
  submitting: boolean;
  error: string | null;
}
const emptyAddFloorForm = (floorNumber: number): AddFloorFormState =>
  ({ name: '', floorNumber, isHostel: false, genderRestriction: null, isBasement: false, submitting: false, error: null });

interface AddZoneFormState {
  name: string;
  isHostel: boolean;
  genderRestriction: GenderRestriction | null;
  wardenId: number | null;
  submitting: boolean;
  error: string | null;
}
const emptyAddZoneForm = (): AddZoneFormState =>
  ({ name: '', isHostel: false, genderRestriction: null, wardenId: null, submitting: false, error: null });

interface AddRoomFormState {
  roomNumber: string;
  capacity: string;
  description: string;
  purposeCategoryId: number | null;
  subTypeId: number | null;
  submitting: boolean;
  error: string | null;
}
const emptyAddRoomForm = (): AddRoomFormState =>
  ({ roomNumber: '', capacity: '', description: '', purposeCategoryId: null, subTypeId: null, submitting: false, error: null });

const codeSuggestionFrom = (name: string): string =>
  name.toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 20);

/**
 * Persistent right-side panel — a properties inspector, not a navigator. Selecting a node (Branch/
 * Block/Floor/Zone/Room) shows *that node's own fields* (inline-editable, with Save) plus a form to
 * add its direct children. There is deliberately no "jump to X" picker here: to add a Floor you
 * must be looking at its parent Block (select the block, then use the Floor form here) — adding a
 * sibling from inside the child's own view is not offered, by explicit design ask. Owns its own
 * service calls (update/status-toggle/hostel-room assignment/create-child) and reports back via
 * `created`/`saved` so the parent only has to refetch, not re-implement the mutation.
 */
@Component({
  selector: 'app-campus-side-panel',
  standalone: true,
  imports: [FormsModule, MatIconModule, CmsStatusBadgeComponent],
  templateUrl: './campus-side-panel.component.html',
  styleUrl: './campus-side-panel.component.scss',
})
export class CampusSidePanelComponent {
  private readonly service = inject(CampusInfrastructureService);
  private readonly roomTypeService = inject(HostelRoomTypeService);
  private readonly categoryService = inject(RoomPurposeCategoryService);
  private readonly subTypeService = inject(RoomSubTypeService);
  private readonly facultyService = inject(FacultyService);
  private readonly toast = inject(ToastService);
  private readonly http = inject(HttpClient);
  private readonly campusApiUrl = `${environment.apiUrl}/campus-infrastructure`;

  readonly canManage = input(false);
  readonly organizationId = input<number | null>(null);

  readonly branch = input<Branch | null>(null);
  readonly block = input<Block | null>(null);
  readonly floor = input<Floor | null>(null);
  readonly zone = input<Zone | null>(null);
  readonly room = input<Room | null>(null);

  /** The entity currently being edited via a card's edit pencil — independent of the `branch`/
   *  `block`/`floor`/`zone` drill-position inputs above (which now only decide which "Add child"
   *  form shows). Room has no equivalent since it's a leaf: selecting one already means viewing its
   *  properties, same as before this split. Organization is root-level (no drill-position input of
   *  its own), so it only ever appears via this editing path. */
  readonly editingOrganization = input<Organization | null>(null);
  readonly editingBranch = input<Branch | null>(null);
  readonly editingBlock = input<Block | null>(null);
  readonly editingFloor = input<Floor | null>(null);
  readonly editingZone = input<Zone | null>(null);

  /** Floors of the currently selected Block — used only to auto-compute the next floor number
   *  when adding a new Floor, so the add form doesn't need its own number field. */
  readonly blockFloors = input<Floor[]>([]);

  readonly created = output<{ level: CampusPanelLevel; id: number }>();
  readonly saved = output<{ level: CampusPanelLevel }>();
  /** Import Floor Plan action — Branch/Floor/Zone/Room only, never Block (Block has no diagram of
   *  its own, see BR-60). The panel doesn't own the spatial module's screens itself; it just reports
   *  which entity to open, mirroring `created`/`saved`. */
  readonly importFloorPlan = output<{ level: 'branch' | 'floor' | 'zone' | 'room'; id: number }>();

  protected readonly level = computed<CampusPanelLevel | 'none'>(() => {
    if (this.room()) return 'room';
    if (this.zone()) return 'zone';
    if (this.floor()) return 'floor';
    if (this.block()) return 'block';
    if (this.branch()) return 'branch';
    return 'none';
  });

  protected readonly roomTypes = signal<HostelRoomType[]>([]);
  protected readonly purposeCategories = signal<RoomPurposeCategory[]>([]);
  protected readonly subTypes = signal<RoomSubType[]>([]);
  /** Separate from `subTypes` above (Room's own edit/properties view) even though only one of the
   *  two is ever visible at once — keeps the Add-Room form's option list from ever flashing stale
   *  data carried over from whichever Room was last selected, or vice versa. */
  protected readonly addRoomSubTypes = signal<RoomSubType[]>([]);
  /** Zone's warden picker (Add and Edit both) — loaded once, not gated behind any drill position. */
  protected readonly faculties = signal<Faculty[]>([]);

  // ── Edit-in-place field state, one per level ────────────────────────────
  protected readonly organizationEdit = signal({ name: '', code: '', description: '' });
  protected readonly branchEdit = signal({ name: '', code: '', description: '' });
  protected readonly blockEdit = signal({ name: '', code: '', description: '', isHostel: false, genderRestriction: null as GenderRestriction | null });
  protected readonly floorEdit = signal({ name: '', floorNumber: 0, isHostel: false, genderRestriction: null as GenderRestriction | null, isBasement: false });
  protected readonly zoneEdit = signal({ name: '', isHostel: false, genderRestriction: null as GenderRestriction | null, wardenId: null as number | null });
  protected readonly roomEdit = signal({
    roomNumber: '', capacity: '' as number | string, description: '',
    purposeCategoryId: null as number | null, subTypeId: null as number | null,
  });
  protected readonly roomHostelTypeId = signal<number | null>(null);

  protected readonly organizationSaving = signal(false);
  protected readonly branchSaving = signal(false);
  protected readonly blockSaving = signal(false);
  protected readonly floorSaving = signal(false);
  protected readonly zoneSaving = signal(false);
  protected readonly roomSaving = signal(false);
  protected readonly hostelAssigning = signal(false);

  // ── Add-child form state, one per level ─────────────────────────────────
  protected readonly addOrganizationForm = signal(emptyAddOrganizationForm());
  protected readonly addBranchForm = signal(emptyAddBranchForm());
  protected readonly addBlockForm = signal(emptyAddBlockForm());
  protected readonly addFloorForm = signal(emptyAddFloorForm(0));

  /** Next unused floorNumber in the selected block — seeds the Add Floor form's default, same as
   *  the auto-suggested Code field elsewhere in this panel, but still editable before submit. */
  protected readonly nextFloorNumber = computed(() => this.blockFloors().reduce((max, fl) => Math.max(max, fl.floorNumber), -1) + 1);
  protected readonly addZoneForm = signal(emptyAddZoneForm());
  protected readonly addRoomForm = signal(emptyAddRoomForm());

  /** Real-time async uniqueness (mandatory master-form pattern) — reuses the shared
   *  `uniqueFieldValidator` (the same one every Reactive-Forms master screen uses) even though this
   *  panel is signal/ngModel-driven, not FormGroup-driven: a throwaway `FormControl` exists purely
   *  as a vehicle to run the validator, its value driven from whichever signal (edit or add form)
   *  is currently live for that level — the two are mutually exclusive states, so one control per
   *  field covers both. Returns a signal of whether the current value is taken. */
  private createUniquenessCheck(
    checkUrl: string,
    getValue: () => string,
    getExcludeId: () => number | null,
    getExtraParams: () => Record<string, string | number> | null,
  ): Signal<boolean> {
    const control = new FormControl<string>('');
    control.setAsyncValidators(uniqueFieldValidator(this.http, checkUrl, getExcludeId, getExtraParams));
    const taken = signal(false);
    effect(() => control.setValue(getValue(), { emitEvent: true }));
    control.statusChanges.pipe(takeUntilDestroyed()).subscribe(() => taken.set(!!control.errors?.['duplicate']));
    return taken.asReadonly();
  }

  protected readonly organizationNameTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/organizations/name-exists`,
    () => (this.editingOrganization() ? this.organizationEdit().name : this.addOrganizationForm().name),
    () => this.editingOrganization()?.id ?? null,
    () => ({}),
  );
  protected readonly organizationCodeTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/organizations/code-exists`,
    () => (this.editingOrganization() ? this.organizationEdit().code : this.addOrganizationForm().code),
    () => this.editingOrganization()?.id ?? null,
    () => ({}),
  );
  protected readonly branchNameTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/branches/name-exists`,
    () => (this.editingBranch() ? this.branchEdit().name : this.addBranchForm().name),
    () => this.editingBranch()?.id ?? null,
    () => (this.organizationId() != null ? { organizationId: this.organizationId()! } : null),
  );
  protected readonly branchCodeTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/branches/code-exists`,
    () => (this.editingBranch() ? this.branchEdit().code : this.addBranchForm().code),
    () => this.editingBranch()?.id ?? null,
    () => (this.organizationId() != null ? { organizationId: this.organizationId()! } : null),
  );
  protected readonly blockNameTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/blocks/name-exists`,
    () => (this.editingBlock() ? this.blockEdit().name : this.addBlockForm().name),
    () => this.editingBlock()?.id ?? null,
    () => (this.branch()?.id != null ? { branchId: this.branch()!.id } : null),
  );
  protected readonly blockCodeTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/blocks/code-exists`,
    () => (this.editingBlock() ? this.blockEdit().code : this.addBlockForm().code),
    () => this.editingBlock()?.id ?? null,
    () => (this.branch()?.id != null ? { branchId: this.branch()!.id } : null),
  );
  protected readonly floorNameTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/floors/name-exists`,
    () => (this.editingFloor() ? this.floorEdit().name : this.addFloorForm().name),
    () => this.editingFloor()?.id ?? null,
    () => (this.block()?.id != null ? { blockId: this.block()!.id } : null),
  );
  protected readonly zoneNameTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/zones/name-exists`,
    () => (this.editingZone() ? this.zoneEdit().name : this.addZoneForm().name),
    () => this.editingZone()?.id ?? null,
    () => (this.floor()?.id != null ? { floorId: this.floor()!.id } : null),
  );
  protected readonly roomNumberTaken = this.createUniquenessCheck(
    `${this.campusApiUrl}/rooms/number-exists`,
    () => (this.room() ? this.roomEdit().roomNumber : this.addRoomForm().roomNumber),
    () => this.room()?.id ?? null,
    () => (this.zone()?.id != null ? { zoneId: this.zone()!.id } : null),
  );

  constructor() {
    this.roomTypeService.getAll(true).subscribe({ next: (types) => this.roomTypes.set(types) });
    this.categoryService.getAll(true).subscribe({ next: (categories) => this.purposeCategories.set(categories) });
    this.facultyService.getAll().subscribe({ next: (faculty) => this.faculties.set(faculty) });

    // ── Add-child forms reset off the *drill position* inputs — unaffected by the editing split
    // below, since "which entity's children am I adding to" is about where you've navigated, not
    // which sibling you're inspecting via its edit pencil.
    effect(() => {
      this.branch();
      this.addBlockForm.set(emptyAddBlockForm());
    });
    effect(() => {
      this.block();
      this.addFloorForm.set(emptyAddFloorForm(this.nextFloorNumber()));
    });
    effect(() => {
      this.floor();
      this.addZoneForm.set(emptyAddZoneForm());
    });
    effect(() => {
      this.zone();
      this.addRoomForm.set(emptyAddRoomForm());
      this.addRoomSubTypes.set([]);
    });

    // ── Edit-in-place field state resets off the *editing* inputs instead — set only when a card's
    // edit pencil was clicked, independent of the drill position above.
    effect(() => {
      const o = this.editingOrganization();
      this.organizationEdit.set(o ? { name: o.name, code: o.code, description: o.description ?? '' } : { name: '', code: '', description: '' });
    });
    effect(() => {
      const b = this.editingBranch();
      this.branchEdit.set(b ? { name: b.name, code: b.code, description: b.description ?? '' } : { name: '', code: '', description: '' });
    });
    effect(() => {
      const b = this.editingBlock();
      this.blockEdit.set(
        b
          ? { name: b.name, code: b.code, description: b.description ?? '', isHostel: b.isHostel, genderRestriction: b.genderRestriction }
          : { name: '', code: '', description: '', isHostel: false, genderRestriction: null }
      );
    });
    effect(() => {
      const f = this.editingFloor();
      this.floorEdit.set(
        f
          ? { name: f.name, floorNumber: f.floorNumber, isHostel: f.isHostel, genderRestriction: f.genderRestriction, isBasement: f.isBasement }
          : { name: '', floorNumber: 0, isHostel: false, genderRestriction: null, isBasement: false }
      );
    });
    effect(() => {
      const z = this.editingZone();
      this.zoneEdit.set(
        z
          ? { name: z.name, isHostel: z.isHostel, genderRestriction: z.genderRestriction, wardenId: z.wardenId }
          : { name: '', isHostel: false, genderRestriction: null, wardenId: null }
      );
    });
    effect(() => {
      const r = this.room();
      this.roomEdit.set(
        r
          ? { roomNumber: r.roomNumber, capacity: r.capacity ?? '', description: r.description ?? '', purposeCategoryId: r.purposeCategoryId, subTypeId: r.subTypeId }
          : { roomNumber: '', capacity: '', description: '', purposeCategoryId: null, subTypeId: null }
      );
      this.roomHostelTypeId.set(r?.hostelRoomTypeId ?? null);
      this.subTypes.set([]);
      if (r?.purposeCategoryId) this.loadSubTypesForCategory(r.purposeCategoryId, this.subTypes);
    });
  }

  /** User picked a different Purpose Category in the Room panel — reload its Sub-Type options and
   *  clear the current sub-type selection (it belonged to the previous category). */
  protected onRoomPurposeCategoryChange(purposeCategoryId: number | null): void {
    this.roomEdit.set({ ...this.roomEdit(), purposeCategoryId, subTypeId: null });
    this.subTypes.set([]);
    if (purposeCategoryId) this.loadSubTypesForCategory(purposeCategoryId, this.subTypes);
  }

  /** Same as `onRoomPurposeCategoryChange` above but for the Add-Room form's own category select. */
  protected onAddRoomPurposeCategoryChange(purposeCategoryId: number | null): void {
    this.addRoomForm.set({ ...this.addRoomForm(), purposeCategoryId, subTypeId: null });
    this.addRoomSubTypes.set([]);
    if (purposeCategoryId) this.loadSubTypesForCategory(purposeCategoryId, this.addRoomSubTypes);
  }

  private loadSubTypesForCategory(purposeCategoryId: number, target: typeof this.subTypes): void {
    this.subTypeService.getAll(purposeCategoryId, true).subscribe({ next: (s) => target.set(s) });
  }

  // ── Save current entity's fields ────────────────────────────────────────
  protected saveOrganization(): void {
    const o = this.editingOrganization();
    const f = this.organizationEdit();
    if (!o || !f.name.trim() || !f.code.trim() || this.organizationSaving()) return;
    this.organizationSaving.set(true);
    this.service
      .updateOrganization(o.id, { name: f.name.trim(), code: f.code.trim().toUpperCase(), description: f.description.trim() || undefined, isActive: o.isActive })
      .subscribe({
        next: () => {
          this.organizationSaving.set(false);
          this.toast.success('Organization updated');
          this.saved.emit({ level: 'organization' });
        },
        error: (err) => {
          this.organizationSaving.set(false);
          this.toast.error(err?.error?.message ?? 'Failed to update organization');
        },
      });
  }

  protected saveBranch(): void {
    const b = this.branch();
    const f = this.branchEdit();
    if (!b || !f.name.trim() || !f.code.trim() || this.branchSaving()) return;
    this.branchSaving.set(true);
    this.service
      .updateBranch(b.id, { name: f.name.trim(), code: f.code.trim().toUpperCase(), description: f.description.trim() || undefined, isActive: b.isActive })
      .subscribe({
        next: () => {
          this.branchSaving.set(false);
          this.toast.success('Branch updated');
          this.saved.emit({ level: 'branch' });
        },
        error: (err) => {
          this.branchSaving.set(false);
          this.toast.error(err?.error?.message ?? 'Failed to update branch');
        },
      });
  }

  protected saveBlock(): void {
    const b = this.block();
    const f = this.blockEdit();
    if (!b || !f.name.trim() || !f.code.trim() || this.blockSaving()) return;
    this.blockSaving.set(true);
    this.service
      .updateBlock(b.id, {
        name: f.name.trim(),
        code: f.code.trim().toUpperCase(),
        description: f.description.trim() || undefined,
        isHostel: f.isHostel,
        genderRestriction: f.genderRestriction,
        isActive: b.isActive,
        branchId: b.branchId,
      })
      .subscribe({
        next: () => {
          this.blockSaving.set(false);
          this.toast.success('Block updated');
          this.saved.emit({ level: 'block' });
        },
        error: (err) => {
          this.blockSaving.set(false);
          this.toast.error(err?.error?.message ?? 'Failed to update block');
        },
      });
  }

  protected saveFloor(): void {
    const fl = this.floor();
    const f = this.floorEdit();
    if (!fl || !f.name.trim() || this.floorSaving()) return;
    this.floorSaving.set(true);
    this.service
      .updateFloor(fl.id, {
        name: f.name.trim(),
        floorNumber: f.floorNumber,
        isHostel: f.isHostel,
        genderRestriction: f.genderRestriction,
        isBasement: f.isBasement,
        isActive: fl.isActive,
        blockId: fl.blockId,
      })
      .subscribe({
        next: () => {
          this.floorSaving.set(false);
          this.toast.success('Floor updated');
          this.saved.emit({ level: 'floor' });
        },
        error: (err) => {
          this.floorSaving.set(false);
          this.toast.error(err?.error?.message ?? 'Failed to update floor');
        },
      });
  }

  protected saveZone(): void {
    const z = this.zone();
    const f = this.zoneEdit();
    if (!z || !f.name.trim() || this.zoneSaving()) return;
    this.zoneSaving.set(true);
    this.service
      .updateZone(z.id, {
        name: f.name.trim(),
        isHostel: f.isHostel,
        genderRestriction: f.genderRestriction,
        wardenId: f.wardenId,
        isActive: z.isActive,
        floorId: z.floorId,
      })
      .subscribe({
        next: () => {
          this.zoneSaving.set(false);
          this.toast.success('Zone updated');
          this.saved.emit({ level: 'zone' });
        },
        error: (err) => {
          this.zoneSaving.set(false);
          this.toast.error(err?.error?.message ?? 'Failed to update zone');
        },
      });
  }

  protected saveRoom(): void {
    const r = this.room();
    const f = this.roomEdit();
    if (!r || !f.roomNumber.trim() || !f.purposeCategoryId || !f.subTypeId || this.roomSaving()) return;
    const capacity = f.capacity === '' ? null : Number(f.capacity);
    this.roomSaving.set(true);
    this.service
      .updateRoom(r.id, {
        roomNumber: f.roomNumber.trim(), capacity, description: f.description.trim() || undefined,
        isActive: r.isActive, zoneId: r.zoneId,
        purposeCategoryId: f.purposeCategoryId, subTypeId: f.subTypeId,
      })
      .subscribe({
        next: () => {
          this.roomSaving.set(false);
          this.toast.success('Room updated');
          this.saved.emit({ level: 'room' });
        },
        error: (err) => {
          this.roomSaving.set(false);
          this.toast.error(err?.error?.message ?? 'Failed to update room');
        },
      });
  }

  // ── Active/Inactive toggle ───────────────────────────────────────────────
  protected toggleActive(level: CampusPanelLevel): void {
    const entity =
      level === 'organization' ? this.editingOrganization() :
      level === 'branch' ? this.branch() :
      level === 'block' ? this.block() :
      level === 'floor' ? this.floor() :
      level === 'zone' ? this.zone() : this.room();
    if (!entity) return;
    const nextActive = !entity.isActive;

    const statusCall$ =
      level === 'organization' ? this.service.updateOrganizationStatus(entity.id, { isActive: nextActive }) :
      level === 'branch' ? this.service.updateBranchStatus(entity.id, { isActive: nextActive }) :
      level === 'block' ? this.service.updateBlockStatus(entity.id, { isActive: nextActive }) :
      level === 'floor' ? this.service.updateFloorStatus(entity.id, { isActive: nextActive }) :
      level === 'zone' ? this.service.updateZoneStatus(entity.id, { isActive: nextActive }) :
      this.service.updateRoomStatus(entity.id, { isActive: nextActive });

    statusCall$.subscribe({
      next: () => {
        this.toast.success(nextActive ? 'Marked active' : 'Marked inactive');
        this.saved.emit({ level });
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update status'),
    });
  }

  // ── Hostel Room Type assignment (Room leaf only) ────────────────────────
  protected assignHostelType(): void {
    const r = this.room();
    const roomTypeId = this.roomHostelTypeId();
    if (!r || !roomTypeId || this.hostelAssigning()) return;
    this.hostelAssigning.set(true);
    this.service.assignHostelRoom(r.id, { roomTypeId, isActive: true }).subscribe({
      next: () => {
        this.hostelAssigning.set(false);
        this.toast.success('Hostel room type assigned');
        this.saved.emit({ level: 'room' });
      },
      error: (err) => {
        this.hostelAssigning.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to assign hostel room type');
      },
    });
  }

  protected unassignHostelType(): void {
    const r = this.room();
    if (!r || !r.hostelRoomId || this.hostelAssigning()) return;
    this.hostelAssigning.set(true);
    this.service.unassignHostelRoom(r.id).subscribe({
      next: () => {
        this.hostelAssigning.set(false);
        this.roomHostelTypeId.set(null);
        this.toast.success('Hostel room type removed');
        this.saved.emit({ level: 'room' });
      },
      error: (err) => {
        this.hostelAssigning.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to remove hostel room type');
      },
    });
  }

  // ── Add child ─────────────────────────────────────────────────────────
  protected submitAddOrganization(): void {
    const f = this.addOrganizationForm();
    const name = f.name.trim();
    if (!name || f.submitting) return;
    const code = f.code.trim() || codeSuggestionFrom(name);
    this.addOrganizationForm.set({ ...f, submitting: true, error: null });
    this.service.createOrganization({ name, code, description: f.description.trim() || undefined }).subscribe({
      next: (created) => {
        this.addOrganizationForm.set(emptyAddOrganizationForm());
        this.created.emit({ level: 'organization', id: created.id });
      },
      error: (err) => this.addOrganizationForm.set({ ...f, submitting: false, error: err?.error?.message ?? 'Failed to add organization.' }),
    });
  }

  protected submitAddBranch(): void {
    const orgId = this.organizationId();
    const f = this.addBranchForm();
    const name = f.name.trim();
    if (!orgId || !name || f.submitting) return;
    const code = f.code.trim() || codeSuggestionFrom(name);
    this.addBranchForm.set({ ...f, submitting: true, error: null });
    this.service.createBranch(orgId, { name, code, description: f.description.trim() || undefined }).subscribe({
      next: (created) => {
        this.addBranchForm.set(emptyAddBranchForm());
        this.created.emit({ level: 'branch', id: created.id });
      },
      error: (err) => this.addBranchForm.set({ ...f, submitting: false, error: err?.error?.message ?? 'Failed to add branch.' }),
    });
  }

  protected submitAddBlock(): void {
    const branch = this.branch();
    const f = this.addBlockForm();
    const name = f.name.trim();
    if (!branch || !name || f.submitting) return;
    const code = f.code.trim() || codeSuggestionFrom(name);
    this.addBlockForm.set({ ...f, submitting: true, error: null });
    this.service
      .createBlock(branch.id, {
        name,
        code,
        description: f.description.trim() || undefined,
        isHostel: f.isHostel,
        genderRestriction: f.genderRestriction,
      })
      .subscribe({
        next: (created) => {
          this.addBlockForm.set(emptyAddBlockForm());
          this.created.emit({ level: 'block', id: created.id });
        },
        error: (err) => this.addBlockForm.set({ ...f, submitting: false, error: err?.error?.message ?? 'Failed to add block.' }),
      });
  }

  protected submitAddFloor(): void {
    const block = this.block();
    const f = this.addFloorForm();
    const name = f.name.trim();
    if (!block || !name || f.submitting) return;
    this.addFloorForm.set({ ...f, submitting: true, error: null });
    this.service
      .createFloor(block.id, {
        name,
        floorNumber: f.floorNumber,
        isHostel: f.isHostel,
        genderRestriction: f.genderRestriction,
        isBasement: f.isBasement,
      })
      .subscribe({
        next: (created) => {
          this.addFloorForm.set(emptyAddFloorForm(f.floorNumber + 1));
          this.created.emit({ level: 'floor', id: created.id });
        },
        error: (err) => this.addFloorForm.set({ ...f, submitting: false, error: err?.error?.message ?? 'Failed to add floor.' }),
      });
  }

  protected submitAddZone(): void {
    const floor = this.floor();
    const f = this.addZoneForm();
    const name = f.name.trim();
    if (!floor || !name || f.submitting) return;
    this.addZoneForm.set({ ...f, submitting: true, error: null });
    this.service
      .createZone(floor.id, { name, isHostel: f.isHostel, genderRestriction: f.genderRestriction, wardenId: f.wardenId })
      .subscribe({
        next: (created) => {
          this.addZoneForm.set(emptyAddZoneForm());
          this.created.emit({ level: 'zone', id: created.id });
        },
        error: (err) => this.addZoneForm.set({ ...f, submitting: false, error: err?.error?.message ?? 'Failed to add zone.' }),
      });
  }

  protected submitAddRoom(): void {
    const zone = this.zone();
    const f = this.addRoomForm();
    const roomNumber = f.roomNumber.trim();
    if (!zone || !roomNumber || !f.purposeCategoryId || !f.subTypeId || f.submitting) return;
    const capacity = f.capacity.trim() ? Number(f.capacity.trim()) : null;
    this.addRoomForm.set({ ...f, submitting: true, error: null });
    this.service
      .createRoom(zone.id, {
        roomNumber,
        capacity,
        description: f.description.trim() || undefined,
        purposeCategoryId: f.purposeCategoryId,
        subTypeId: f.subTypeId,
      })
      .subscribe({
        next: (created) => {
          this.addRoomForm.set(emptyAddRoomForm());
          this.addRoomSubTypes.set([]);
          this.created.emit({ level: 'room', id: created.id });
        },
        error: (err) => this.addRoomForm.set({ ...f, submitting: false, error: err?.error?.message ?? 'Failed to add room.' }),
      });
  }
}
