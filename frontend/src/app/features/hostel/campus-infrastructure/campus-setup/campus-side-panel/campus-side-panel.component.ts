import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { CampusInfrastructureService } from '../../campus-infrastructure.service';
import { Block, Branch, Floor, GenderRestriction, Room, Zone } from '../../campus-infrastructure.model';
import { HostelRoomTypeService } from '../../../hostel-room-type/hostel-room-type.service';
import { HostelRoomType } from '../../../hostel-room-type/hostel-room-type.model';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../../core/toast/toast.service';

export type CampusPanelLevel = 'branch' | 'block' | 'floor' | 'zone' | 'room';

interface AddFormState {
  name: string;
  code: string;
  capacity: string;
  submitting: boolean;
  error: string | null;
}
const emptyAddForm = (): AddFormState => ({ name: '', code: '', capacity: '', submitting: false, error: null });

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
  private readonly toast = inject(ToastService);

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
   *  properties, same as before this split. */
  readonly editingBranch = input<Branch | null>(null);
  readonly editingBlock = input<Block | null>(null);
  readonly editingFloor = input<Floor | null>(null);
  readonly editingZone = input<Zone | null>(null);

  /** Floors of the currently selected Block — used only to auto-compute the next floor number
   *  when adding a new Floor, so the add form doesn't need its own number field. */
  readonly blockFloors = input<Floor[]>([]);

  readonly created = output<{ level: CampusPanelLevel; id: number }>();
  readonly saved = output<{ level: CampusPanelLevel }>();

  protected readonly level = computed<CampusPanelLevel | 'none'>(() => {
    if (this.room()) return 'room';
    if (this.zone()) return 'zone';
    if (this.floor()) return 'floor';
    if (this.block()) return 'block';
    if (this.branch()) return 'branch';
    return 'none';
  });

  protected readonly roomTypes = signal<HostelRoomType[]>([]);

  // ── Edit-in-place field state, one per level ────────────────────────────
  protected readonly branchEdit = signal({ name: '', code: '', description: '' });
  protected readonly blockEdit = signal({ name: '', code: '', description: '', isHostel: false, genderRestriction: null as GenderRestriction | null });
  protected readonly floorEdit = signal({ name: '', floorNumber: 0, isHostel: false, genderRestriction: null as GenderRestriction | null, isBasement: false });
  protected readonly zoneEdit = signal({ name: '', isHostel: false, genderRestriction: null as GenderRestriction | null });
  protected readonly roomEdit = signal({ roomNumber: '', capacity: '' as number | string, description: '' });
  protected readonly roomHostelTypeId = signal<number | null>(null);

  protected readonly branchSaving = signal(false);
  protected readonly blockSaving = signal(false);
  protected readonly floorSaving = signal(false);
  protected readonly zoneSaving = signal(false);
  protected readonly roomSaving = signal(false);
  protected readonly hostelAssigning = signal(false);

  // ── Add-child form state, one per level ─────────────────────────────────
  protected readonly addBranchForm = signal(emptyAddForm());
  protected readonly addBlockForm = signal(emptyAddForm());
  protected readonly addFloorForm = signal(emptyAddFloorForm(0));

  /** Next unused floorNumber in the selected block — seeds the Add Floor form's default, same as
   *  the auto-suggested Code field elsewhere in this panel, but still editable before submit. */
  protected readonly nextFloorNumber = computed(() => this.blockFloors().reduce((max, fl) => Math.max(max, fl.floorNumber), -1) + 1);
  protected readonly addZoneForm = signal(emptyAddForm());
  protected readonly addRoomForm = signal(emptyAddForm());

  constructor() {
    this.roomTypeService.getAll(true).subscribe({ next: (types) => this.roomTypes.set(types) });

    // ── Add-child forms reset off the *drill position* inputs — unaffected by the editing split
    // below, since "which entity's children am I adding to" is about where you've navigated, not
    // which sibling you're inspecting via its edit pencil.
    effect(() => {
      this.branch();
      this.addBlockForm.set(emptyAddForm());
    });
    effect(() => {
      this.block();
      this.addFloorForm.set(emptyAddFloorForm(this.nextFloorNumber()));
    });
    effect(() => {
      this.floor();
      this.addZoneForm.set(emptyAddForm());
    });
    effect(() => {
      this.zone();
      this.addRoomForm.set(emptyAddForm());
    });

    // ── Edit-in-place field state resets off the *editing* inputs instead — set only when a card's
    // edit pencil was clicked, independent of the drill position above.
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
      this.zoneEdit.set(z ? { name: z.name, isHostel: z.isHostel, genderRestriction: z.genderRestriction } : { name: '', isHostel: false, genderRestriction: null });
    });
    effect(() => {
      const r = this.room();
      this.roomEdit.set(r ? { roomNumber: r.roomNumber, capacity: r.capacity ?? '', description: r.description ?? '' } : { roomNumber: '', capacity: '', description: '' });
      this.roomHostelTypeId.set(r?.hostelRoomTypeId ?? null);
    });
  }

  // ── Save current entity's fields ────────────────────────────────────────
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
        wardenId: z.wardenId,
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
    if (!r || !f.roomNumber.trim() || this.roomSaving()) return;
    const capacity = f.capacity === '' ? null : Number(f.capacity);
    this.roomSaving.set(true);
    this.service
      .updateRoom(r.id, { roomNumber: f.roomNumber.trim(), capacity, description: f.description.trim() || undefined, isActive: r.isActive, zoneId: r.zoneId })
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
      level === 'branch' ? this.branch() :
      level === 'block' ? this.block() :
      level === 'floor' ? this.floor() :
      level === 'zone' ? this.zone() : this.room();
    if (!entity) return;
    const nextActive = !entity.isActive;

    const statusCall$ =
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
  protected submitAddBranch(): void {
    const orgId = this.organizationId();
    const f = this.addBranchForm();
    const name = f.name.trim();
    if (!orgId || !name || f.submitting) return;
    const code = f.code.trim() || codeSuggestionFrom(name);
    this.addBranchForm.set({ ...f, submitting: true, error: null });
    this.service.createBranch(orgId, { name, code }).subscribe({
      next: (created) => {
        this.addBranchForm.set(emptyAddForm());
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
    this.service.createBlock(branch.id, { name, code }).subscribe({
      next: (created) => {
        this.addBlockForm.set(emptyAddForm());
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
    this.service.createZone(floor.id, { name }).subscribe({
      next: (created) => {
        this.addZoneForm.set(emptyAddForm());
        this.created.emit({ level: 'zone', id: created.id });
      },
      error: (err) => this.addZoneForm.set({ ...f, submitting: false, error: err?.error?.message ?? 'Failed to add zone.' }),
    });
  }

  protected submitAddRoom(): void {
    const zone = this.zone();
    const f = this.addRoomForm();
    const roomNumber = f.name.trim();
    if (!zone || !roomNumber || f.submitting) return;
    const capacity = f.capacity.trim() ? Number(f.capacity.trim()) : null;
    this.addRoomForm.set({ ...f, submitting: true, error: null });
    this.service.createRoom(zone.id, { roomNumber, capacity }).subscribe({
      next: (created) => {
        this.addRoomForm.set(emptyAddForm());
        this.created.emit({ level: 'room', id: created.id });
      },
      error: (err) => this.addRoomForm.set({ ...f, submitting: false, error: err?.error?.message ?? 'Failed to add room.' }),
    });
  }
}
