import { TourDefinition } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Capacity Planner
// ─────────────────────────────────────────────────────────────────────────────
export const CAPACITY_PLANNER_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📐 Plan a Term\'s Timetable Capacity',
        description:
          'Work out how many classrooms and lab/clinical batches a cohort actually needs — before building its timetable — and commit the physical rooms Skeleton Builder and Staffing will build against.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cap-toolbar',
      popover: {
        title: 'Pick a Term and Cohort',
        description:
          'Choose the term and cohort, and whether to plan against enrolled headcount or sanctioned intake (useful in a first term where enrollment is still rolling in), then Calculate.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cap-allocation',
      popover: {
        title: 'Theory Sections',
        description:
          'If no single classroom fits the whole cohort, you\'ll be walked through splitting it into sections, each with its own classroom.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-cap-allocation',
      popover: {
        title: 'Lab & Clinical Batches',
        description:
          'Pick a subject with Lab/Clinical hours and a default venue — one row auto-generates per section; split a row if a venue can\'t seat the whole section — then Commit.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Next: build the timetable',
        description:
          'Open Skeleton Builder to place Theory/Lab/Clinical sessions into periods for each subject, then Staffing to assign faculty — Theory rooms are picked there, but Lab/Clinical rooms are already fixed from here and can\'t be changed in Staffing.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};
