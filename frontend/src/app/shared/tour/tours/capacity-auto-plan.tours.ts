import { TourDefinition, TourFlowMap } from '../tour.service';
import { TIMETABLE_BUILD_FUNNEL } from './timetable.tours';

// ─────────────────────────────────────────────────────────────────────────────
// Capacity Auto-Plan (Academics nav — term-wide bulk version of Capacity Planner)
// ─────────────────────────────────────────────────────────────────────────────
export const CAPACITY_AUTO_PLAN_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '⚡ Term-Wide Capacity Auto-Plan',
        description:
          'Every cohort enrolled this term, the rooms available to plan into, and fewest-rooms Theory/Lab/Clinical suggestions — reviewed cohort by cohort. Nothing commits automatically.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cap-auto-toolbar',
      popover: {
        title: 'Term & Auto-Plan All',
        description:
          'Pick a term, then use Auto-Plan All to generate suggestions for every not-yet-planned cohort at once — or work through cohorts one at a time below.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cap-auto-summary',
      popover: {
        title: 'Term Summary',
        description:
          'How many cohorts are already committed, still not planned, and how far Theory sections and Lab/Clinical batches have progressed across the whole term.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cap-auto-cohorts',
      popover: {
        title: 'Review & Commit per Cohort',
        description:
          'Switch between cohort tabs to review that cohort\'s suggested Theory sections and Lab/Clinical batches, then commit — need finer manual control for one cohort? Open Capacity Planner instead.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const CAPACITY_AUTO_PLAN_FLOW_MAP: TourFlowMap = {
  funnel: TIMETABLE_BUILD_FUNNEL,
  currentIndex: 4,
  steps: [
    { label: 'Pick Term', icon: 'search', detail: 'Choose the term to plan capacity for.' },
    { label: 'Auto-Plan All', icon: 'open', detail: 'Generate fewest-rooms suggestions for every not-yet-planned cohort at once.' },
    { label: 'Review Summary', icon: 'checklist', detail: 'See committed vs. not-planned cohorts and section/batch progress term-wide.' },
    { label: 'Commit per Cohort', icon: 'send', detail: 'Review and commit each cohort\'s Theory sections and Lab/Clinical batches.' },
  ],
};
