import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// CO-PO Mapping List
// ─────────────────────────────────────────────────────────────────────────────
export const CO_PO_MAPPING_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔗 CO-PO Mappings',
        description:
          'Map course outcomes (CO) and program outcomes (PO) to lab experiments, showing how each experiment contributes to the outcomes it\'s meant to build.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-copo-toolbar',
      popover: {
        title: 'Search',
        description: 'Search mappings by experiment, subject, or outcome.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-copo-table',
      popover: {
        title: 'Mapping Records',
        description:
          'Each row links one experiment to one outcome (CO or PO), with an outcome code, description, and mapping strength (Low / Medium / High).',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Add a mapping',
        description:
          'Use Add Mapping to link a new experiment to a course or program outcome — experiments come from the Experiments screen.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const CO_PO_MAPPING_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'CO/PO Mapping', description: 'Map course and program outcomes to the experiments that build them.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'Search mappings by experiment, subject, or outcome.' },
    { label: 'Mapping Records', icon: 'checklist', detail: 'Each row links an experiment to a course/program outcome with a mapping strength.' },
    { label: 'Add a Mapping', icon: 'open', detail: 'Pick an experiment and an outcome, and set how strongly the experiment maps to it.' },
    { label: 'Save', icon: 'send', detail: 'Save the mapping so it appears in outcome-attainment reporting.' },
  ],
};
