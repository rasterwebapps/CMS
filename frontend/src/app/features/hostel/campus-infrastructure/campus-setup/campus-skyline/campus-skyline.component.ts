import { Component, computed, effect, input, output, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { CdkDrag, CdkDragDrop, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Block, Floor, Room, Zone } from '../../campus-infrastructure.model';

export interface SkylineZone {
  zone: Zone;
  rooms: Room[];
}

export interface SkylineFloor {
  floor: Floor;
  zones: SkylineZone[];
}

export interface SkylineBlock {
  block: Block;
  floors: SkylineFloor[];
}

/** Every block uses the same single mild/light tint of the theme's own primary color (see
 *  `.building--primary` in the SCSS, a `color-mix()` of `--cms-primary`) — no more per-building
 *  variety, so a branch's row of buildings reads as one consistent, on-brand surface rather than
 *  distinct hues, and automatically follows a tenant's actual theme color rather than a hardcoded
 *  hex. Single-entry array (not a hardcoded string) so `gradientFor()` below needs no change if
 *  variety is ever reintroduced later. */
const BUILDING_GRADIENTS = ['primary'];

/** Zoomed single-Block view (`isSingleBlock`) has no sibling buildings competing for room, so it
 *  scales `densityScale` up rather than just leaving it at the un-shrunk 1 — a deliberate close-up,
 *  not just "alone at the same size." */
const SINGLE_BLOCK_BOOST = 1.15;

/**
 * Draws one or more Blocks as literal buildings: a colored roof label, floors stacked bottom-to-top
 * as decorative window rows (ground floor gets a door), a ground line splitting above-ground floors
 * (`isBasement === false`) from basement floors (rendered recessed below the line), and a caption
 * below each building ("Hostel · 3 floors · 8 rooms"). Purely structural — no Zone/Room detail is
 * legible in the diagram itself.
 *
 * Reused at two levels of the campus-setup drilldown, same component and same two outputs both
 * times: the "Blocks" level passes every Block in the branch (a full skyline, `floorsClickable`
 * false — a floor row there is decorative, clicking the building itself is what drills in); the
 * "Floors" level, once a Block is selected, passes just that one Block (a zoomed single-building
 * view of its floors, `floorsClickable` true) — see `CampusSetupComponent.selectedBlockSkyline`.
 * Where floor rows are clickable, clicking one "opens" it (the parent selects that Floor, which
 * switches the whole screen to the Grid view on that floor's Zones).
 */
@Component({
  selector: 'app-campus-skyline',
  standalone: true,
  imports: [MatIconModule, NgClass, CdkDropList, CdkDrag],
  templateUrl: './campus-skyline.component.html',
  styleUrl: './campus-skyline.component.scss',
})
export class CampusSkylineComponent {
  readonly blocks = input.required<SkylineBlock[]>();
  readonly loading = input(false);
  readonly emptyLabel = input('No blocks in this branch yet.');
  readonly heading = input('');
  readonly canManage = input(false);

  readonly selectedBlockId = input<number | null>(null);
  readonly selectedFloorId = input<number | null>(null);

  /** Floor rows only open a Floor (`floorOpened`) at the zoomed single-Block level — in the multi-
   *  Block view (`false`) a floor row's own click is a no-op (see the template), but it's never a
   *  disabled button: it still lets the click through to `.building-slot`'s own click handler,
   *  which is what makes "click anywhere on a hovered block" (including on top of a floor row)
   *  drill in, same as clicking the roof. Default `true` so the single-Block call site doesn't need
   *  to opt in. */
  readonly floorsClickable = input(true);

  readonly blockSelected = output<SkylineBlock>();
  readonly floorOpened = output<{ block: SkylineBlock; floor: SkylineFloor }>();

  /** Local copy of `blocks`, mutated immediately on drop for optimistic drag feedback (same pattern
   *  used for the earlier Zone/Room reorder — see V338's frontend notes) — re-synced from the input
   *  via this `effect()` whenever a fresh fetch comes in, which is also what snaps a failed reorder
   *  back to the server's real order (the parent refetches on error, the new `blocks` value flows
   *  back in here and overwrites the optimistic local mutation). */
  protected readonly localBlocks = signal<SkylineBlock[]>([]);
  readonly blocksReordered = output<number[]>();

  constructor() {
    effect(() => this.localBlocks.set(this.blocks()));
  }

  /** Reordering only ever applies at the multi-Block level (`blocks().length > 1` in practice) —
   *  the zoomed single-Block view passes a 0-or-1-item array, where a drop is a no-op regardless.
   *  Never reparents a Block to a different Branch, only reorders siblings within this one list. */
  protected onBlockDrop(event: CdkDragDrop<SkylineBlock[]>): void {
    if (event.previousIndex === event.currentIndex) return;
    const reordered = [...this.localBlocks()];
    moveItemInArray(reordered, event.previousIndex, event.currentIndex);
    this.localBlocks.set(reordered);
    this.blocksReordered.emit(reordered.map((sb) => sb.block.id));
  }

  /** Fired from the title's edit pencil — the label's own `blockSelected` (drill into Floors) must
   *  NOT also fire, so the handler stops propagation before emitting. */
  readonly blockEditRequested = output<SkylineBlock>();

  protected onEditClick(event: Event, sb: SkylineBlock): void {
    event.stopPropagation();
    this.blockEditRequested.emit(sb);
  }

  /** Where a row opens its own Floor (`floorsClickable`), the click must stop here — left to bubble,
   *  it also reaches `.building-slot`'s own `(click)="blockSelected.emit(sb)"` (see the template),
   *  which calls the parent's `selectBlock()`; that resets the floor selection back to `null` as
   *  part of re-selecting the (already-selected) Block, undoing `floorOpened` in the same tick so
   *  the screen never visibly leaves the Block view. Where a row is *not* individually clickable
   *  (the multi-Block skyline), propagation is deliberately left alone — that's what lets the click
   *  fall through to `.building-slot` and drill into the Block instead, same as clicking the roof. */
  protected onFloorRowClick(event: Event, sb: SkylineBlock, sf: SkylineFloor): void {
    if (!this.floorsClickable()) return;
    event.stopPropagation();
    this.floorOpened.emit({ block: sb, floor: sf });
  }

  /** Fired from a floor row's own edit pencil (only rendered where `floorsClickable`, the zoomed
   *  single-Block "Floors" view) — until this existed, the row's edit pencil was the *Block's* own
   *  (on the roof label), the only edit affordance visible while looking at a Block's Floors, so
   *  editing from here always edited the Block instead of the Floor being looked at. Stops
   *  propagation same as `onEditClick`/`onFloorRowClick` — must not also fire the row's own
   *  `floorOpened`. */
  readonly floorEditRequested = output<{ block: SkylineBlock; floor: SkylineFloor }>();

  protected onFloorEditClick(event: Event, sb: SkylineBlock, sf: SkylineFloor): void {
    event.stopPropagation();
    this.floorEditRequested.emit({ block: sb, floor: sf });
  }

  /** New, additive alongside `floorOpened` (which switches this same screen to the Grid view) —
   *  this instead navigates to that Floor's spatial diagram (BR-60 Phase 1). Only rendered where
   *  `floorsClickable` (the zoomed single-Block view), same gating as the edit pencil. */
  readonly viewFloorDiagram = output<{ block: SkylineBlock; floor: SkylineFloor }>();

  protected onViewFloorDiagramClick(event: Event, sb: SkylineBlock, sf: SkylineFloor): void {
    event.stopPropagation();
    this.viewFloorDiagram.emit({ block: sb, floor: sf });
  }

  protected readonly gradientFor = computed(() => {
    const map = new Map<number, string>();
    this.blocks().forEach((b, i) => map.set(b.block.id, `building--${BUILDING_GRADIENTS[i % BUILDING_GRADIENTS.length]}`));
    return map;
  });

  /** `.anim-rise` (styles.scss) uses `animation-fill-mode: backwards`, which never leaves a
   *  lingering transform behind once the animation ends — unlike `forwards`/`both`, that's what
   *  keeps this safe to use here (see the containing-block bug this screen already had once).
   *  Only 4 stagger steps are defined globally, so cycle rather than run out past the 4th building. */
  protected staggerClass(index: number): string {
    return `anim-rise--d${(index % 4) + 1}`;
  }

  /** Floors marked skyline (`isBasement === false`), highest `floorNumber` first — floorNumber is
   *  only an ordering key here, `isBasement` is the explicit flag that actually decides which side
   *  of the ground line a floor draws on (see `Floor.isBasement` on the backend for why: the old
   *  "floorNumber sign decides it" convention broke twice on real data numbered inconsistently). */
  protected aboveGround(sb: SkylineBlock): SkylineFloor[] {
    return sb.floors.filter((f) => !f.floor.isBasement).sort((a, b) => b.floor.floorNumber - a.floor.floorNumber);
  }

  /** Floors marked earthline (`isBasement === true`), highest `floorNumber` first (so a basement
   *  numbered -1 sits right under the ground line, -2 beneath that, and so on). */
  protected belowGround(sb: SkylineBlock): SkylineFloor[] {
    return sb.floors.filter((f) => f.floor.isBasement).sort((a, b) => b.floor.floorNumber - a.floor.floorNumber);
  }

  /** Deepest basement among all currently-rendered Blocks — `.building__below` hangs below the
   *  tower via `position: absolute` (see `.building-slot`'s doc comment in the SCSS) so it doesn't
   *  contribute to `.skyline-stage`'s own auto-height, and the stage previously reserved a flat
   *  guessed buffer for it, which visibly overlapped/clipped a Block with more basement floors than
   *  the guess accounted for. */
  protected readonly maxBasementRows = computed(() =>
    this.blocks().reduce((max, sb) => Math.max(max, this.belowGround(sb).length), 0)
  );

  /** Shrinks every building/floor-row/window dimension together as either the row (more Blocks) or
   *  a single building (more Floors) gets crowded — a Block never gets *individually* narrower just
   *  because a sibling has 8 floors, and a branch with 2 tall Blocks doesn't shrink just because
   *  it's not wide. Blocks still wrap to a new row when they don't fit (`.skyline`'s own
   *  `flex-wrap`, untouched) — this only reduces *how often* that has to happen, it doesn't replace
   *  it, since shrinking has a floor (`MIN_DENSITY`) rather than continuing indefinitely. Read by
   *  the template as a `--density` CSS custom property and consumed via `calc(<base> * var(--density))`
   *  throughout the SCSS, so every scaled dimension stays proportional to the same one number
   *  instead of drifting independently. */
  protected readonly densityScale = computed(() => {
    const blockCount = this.blocks().length;
    const maxFloors = this.blocks().reduce((max, sb) => Math.max(max, sb.floors.length), 0);
    const MIN_DENSITY = 0.6;
    const byBlockCount = blockCount > 4 ? Math.max(MIN_DENSITY, 1 - (blockCount - 4) * 0.06) : 1;
    const byFloorCount = maxFloors > 5 ? Math.max(MIN_DENSITY, 1 - (maxFloors - 5) * 0.05) : 1;
    const base = Math.min(byBlockCount, byFloorCount);
    return this.isSingleBlock() ? base * SINGLE_BLOCK_BOOST : base;
  });

  /** Exactly the zoomed "Floors" view's condition — one Block, drawn as a close-up rather than a
   *  row of several. Drives both the size boost above and the shortened ground line (see
   *  `.skyline--zoomed` in the SCSS) — kept as its own computed rather than inlined so both stay in
   *  sync with the same definition of "zoomed" if a future call site changes what gets passed in. */
  protected readonly isSingleBlock = computed(() => this.blocks().length === 1);

  /** The stage's actual bottom padding, computed to the pixel in TS rather than via a CSS
   *  `var()`/`calc()` chain (tried first — a `--basement-rows` custom property read inside
   *  `min()`/`calc()` — and the fit still came out short, so this replaces that guesswork with
   *  values that can be read directly off the numbers below rather than re-derived from CSS engine
   *  behavior). `.floor-row`'s own height (scaled by `densityScale`, same as the SCSS) per basement
   *  row; +1px per row is the divider border between rows; +2px is `.building__basement`'s own
   *  bottom border; +6px is `.building__below`'s `gap` before the caption (only relevant once a
   *  basement box actually exists); ~16px is the caption's own line height; the final +16px is the
   *  breathing room asked for. */
  protected readonly stageBottomPadding = computed(() => {
    const rows = this.maxBasementRows();
    const rowHeight = 76 * this.densityScale();
    const basementHeight = rows > 0 ? rows * rowHeight + (rows - 1) * 1 + 2 + 6 : 0;
    return basementHeight + 16 + 16;
  });

  /** The door goes on the bottom-most skyline floor — the one actually sitting on the ground line
   *  — not on whichever floor happens to be numbered 0, since numbering is just ordering now. */
  protected isGroundFloor(sb: SkylineBlock, sf: SkylineFloor): boolean {
    const above = this.aboveGround(sb);
    return above.length > 0 && above[above.length - 1].floor.id === sf.floor.id;
  }

  protected buildingCaption(sb: SkylineBlock): string {
    const floorCount = sb.floors.length;
    const zoneCount = sb.floors.reduce((n, f) => n + f.zones.length, 0);
    const roomCount = sb.floors.reduce((n, f) => n + f.zones.reduce((m, z) => m + z.rooms.length, 0), 0);
    return `${floorCount} Floor${floorCount === 1 ? '' : 's'} · ${zoneCount} Zone${zoneCount === 1 ? '' : 's'} · ${roomCount} Room${roomCount === 1 ? '' : 's'}`;
  }
}
