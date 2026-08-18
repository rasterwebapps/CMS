import { TourDefinition, TourFlowMap } from '../tour.service';
import { DiagramLevel } from '../../../features/spatial/spatial.model';

// FloorPlanListComponent serves 4 routes (Branch/Floor/Zone/Room Diagrams) off one component,
// varying only by route `data.level` — so the tour/flow-map content is built per level rather
// than hard-coded, and each level still registers under its own distinct tourKey.

const TOUR_KEY_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'branch-diagrams',
  FLOOR: 'floor-plans',
  ZONE: 'zone-diagrams',
  ROOM: 'room-diagrams',
};

const TITLE_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'Branch Diagrams',
  FLOOR: 'Floor Plans',
  ZONE: 'Zone Diagrams',
  ROOM: 'Room Diagrams',
};

const SHOWS_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'its Blocks',
  FLOOR: 'its Zones, Rooms, Equipment, and Inventory Items',
  ZONE: 'its Rooms',
  ROOM: 'its Equipment and Inventory Items',
};

const PICKER_STEPS_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'Choose an Organization and Branch to view or add that branch\'s diagram.',
  FLOOR: 'Choose an Organization, Branch, Block, and Floor to view or add that floor\'s plans.',
  ZONE: 'Choose an Organization, Branch, Block, Floor, and Zone to view or add that zone\'s diagrams.',
  ROOM: 'Choose an Organization, Branch, Block, Floor, Zone, and Room to view or add that room\'s diagrams.',
};

export function tourKeyForDiagramLevel(level: DiagramLevel): string {
  return TOUR_KEY_BY_LEVEL[level];
}

export function buildFloorPlanListTour(level: DiagramLevel): TourDefinition {
  return {
    steps: [
      {
        popover: {
          title: `🗺️ ${TITLE_BY_LEVEL[level]}`,
          description: `Upload a diagram image or SVG for this level, calibrate it against a real-world distance, then place markers on it showing ${SHOWS_BY_LEVEL[level]}.`,
          side: 'over',
          align: 'center',
        },
      },
      {
        element: '#tour-fpl-picker',
        popover: {
          title: 'Drill Down',
          description: PICKER_STEPS_BY_LEVEL[level],
          side: 'bottom',
          align: 'start',
        },
      },
      {
        element: '#tour-fpl-cards',
        popover: {
          title: 'Diagrams',
          description: 'Each card is one uploaded diagram. Calibrate sets its real-world scale; Manage Locations opens the canvas to place markers on it.',
          side: 'top',
          align: 'start',
        },
      },
      {
        popover: {
          title: '✅ Calibrate before placing markers',
          description: 'A diagram must be calibrated (a known real-world distance mapped to pixels) before marker positions placed on it are meaningful.',
          side: 'over',
          align: 'center',
        },
      },
    ],
  };
}

export function buildFloorPlanListFlowMap(level: DiagramLevel): TourFlowMap {
  return {
    funnel: [{ label: TITLE_BY_LEVEL[level], description: `Upload, calibrate, and manage ${level.toLowerCase()}-level diagrams.` }],
    currentIndex: 0,
    steps: [
      { label: 'Drill Down', icon: 'search', detail: PICKER_STEPS_BY_LEVEL[level] },
      { label: 'Add a Diagram', icon: 'open', detail: 'Upload an image or SVG for the selected entity.' },
      { label: 'Calibrate', icon: 'checklist', detail: 'Map a known real-world distance to pixels so marker positions are meaningful.' },
      { label: 'Manage Locations', icon: 'send', detail: `Open the canvas and place markers for ${SHOWS_BY_LEVEL[level]}.` },
    ],
  };
}
