import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Campus Infrastructure (Setup view)
// ─────────────────────────────────────────────────────────────────────────────
export const CAMPUS_SETUP_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🏫 Campus Setup',
        description: 'Build out the physical campus hierarchy — Organization → Branch → Block → Floor → Zone → Room — by drilling down and adding structure from the panel on the right.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cs-orgtabs',
      popover: {
        title: 'Organizations',
        description: 'Switch between organizations with the tabs, edit one with its pencil icon, or add a new one with the + button.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-cs-crumbs',
      popover: {
        title: 'Breadcrumbs',
        description: 'Shows exactly where you are in the drill-down — click any level to jump back to it. A search box appears once you\'re inside a branch.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cs-viewport',
      popover: {
        title: 'The Drill-Down View',
        description: 'Branches show as cards; a Block\'s Floors render as a Skyline (visual, not to scale). Click a card to go deeper, use the edit pencil to update it in place, or View Diagram to jump to that level\'s spatial diagram.',
        side: 'right',
        align: 'start',
      },
    },
    {
      element: '#tour-cs-panel',
      popover: {
        title: 'Add / Edit Panel',
        description: 'Whatever you\'ve selected (or are editing) shows its form here — add the next level down, edit the currently selected/editing entity\'s own properties, or (Branch/Floor/Zone/Room) import a DXF/PDF floor plan for it.',
        side: 'left',
        align: 'start',
      },
    },
  ],
};

export const CAMPUS_SETUP_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Campus Infrastructure', description: 'Build the physical campus hierarchy: Organization → Branch → Block → Floor → Zone → Room.' }],
  currentIndex: 0,
  steps: [
    { label: 'Pick Organization', icon: 'search', detail: 'Switch organizations with the tabs, or add a new one.' },
    { label: 'Drill Down', icon: 'checklist', detail: 'Branch → Block (Skyline) → Floor → Zone → Room, following the breadcrumbs.' },
    { label: 'Add from the Panel', icon: 'open', detail: 'The right-hand panel adds the next level down for whatever\'s selected.' },
    { label: 'Edit in Place', icon: 'send', detail: 'Use a card/block\'s edit pencil to update its properties without losing your drill position.' },
  ],
};
