import { TourDefinition, TourFlowMap } from '../tour.service';

// Same curriculum-authoring funnel as curriculum-version.tours.ts (duplicated,
// not imported, so each tours.ts file stays self-contained).
const CURRICULUM_AUTHORING_FUNNEL = [
  { label: 'Curriculum Versions', description: 'Define versioned term/subject mappings for each program.' },
  { label: 'Syllabus', description: 'Author credit hours, objectives, content, books, and outcomes for each course.' },
  { label: 'Experiments', description: 'Define lab experiments with procedure, apparatus, and learning outcomes for each subject.' },
];

export const SYLLABUS_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📖 Welcome to Syllabus',
        description:
          'This screen lets you manage course syllabi — credit hours, objectives, content, books, and outcomes. Let\'s walk through it.',
        side: 'over',
        align: 'center',
      },
    },
    { element: '#tour-syl-add-btn', popover: { title: 'Add Syllabus', description: 'Define a new syllabus for a course version.', side: 'bottom', align: 'end' } },
    { element: '#tour-syl-search', popover: { title: 'Search Syllabus', description: 'Quickly filter by course name or version.', side: 'bottom', align: 'start' } },
    { element: '#tour-syl-columns', popover: { title: 'Toggle Columns', description: 'Show or hide table columns. Your selection is remembered.', side: 'bottom', align: 'end' } },
    { element: '#tour-syl-content', popover: { title: 'Syllabus Table', description: 'Sortable list of all syllabi grouped by course and version. Hover a row for actions.', side: 'top', align: 'start' } },
    {
      popover: {
        title: '✅ You\'re all set!',
        description: 'Replay this tour any time via <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const SYLLABUS_LIST_FLOW_MAP: TourFlowMap = {
  funnel: CURRICULUM_AUTHORING_FUNNEL,
  currentIndex: 1,
  steps: [
    { label: 'Search Syllabus', icon: 'search', detail: 'Quickly filter by course name or version.' },
    { label: 'Syllabus Table', icon: 'checklist', detail: 'Sortable list of all syllabi grouped by course and version.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Define a new syllabus for a course version, or edit an existing one — credit hours, objectives, content, books, and outcomes.' },
    { label: 'Save', icon: 'send', detail: 'Save to make the syllabus available for that course version.' },
  ],
};

export const SYLLABUS_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Syllabus Form',
        description: 'Create or edit a course syllabus — let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    { element: '#syl-course', popover: { title: 'Course', description: 'The course this syllabus applies to.', side: 'bottom', align: 'start' } },
    { element: '#syl-version', popover: { title: 'Version', description: 'Bump the version number when revising the syllabus — old versions are preserved for audit.', side: 'bottom', align: 'start' } },
    { element: '#syl-theory', popover: { title: 'Theory Hours', description: 'Weekly theory contact hours for this course.', side: 'bottom', align: 'start' } },
    { element: '#syl-lab', popover: { title: 'Lab Hours', description: 'Weekly lab/practical contact hours.', side: 'bottom', align: 'start' } },
    { element: '#syl-tutorial', popover: { title: 'Tutorial Hours', description: 'Weekly tutorial/discussion contact hours.', side: 'bottom', align: 'start' } },
    { element: '#syl-objectives', popover: { title: 'Course Objectives', description: 'High-level objectives the course intends to achieve.', side: 'bottom', align: 'start' } },
    { element: '#syl-content', popover: { title: 'Syllabus Content', description: 'Detailed unit-wise topics covered in the syllabus.', side: 'bottom', align: 'start' } },
    { element: '#syl-textbooks', popover: { title: 'Text Books', description: 'Recommended text books — one per line.', side: 'bottom', align: 'start' } },
    { element: '#syl-refbooks', popover: { title: 'Reference Books', description: 'Supplementary reference books — one per line.', side: 'bottom', align: 'start' } },
    { element: '#syl-outcomes', popover: { title: 'Course Outcomes', description: 'Measurable outcomes students achieve on completing the course.', side: 'bottom', align: 'start' } },
    { element: '#tour-syl-submit', popover: { title: 'Save the Syllabus', description: 'Click here to save. The button is disabled while saving.', side: 'top', align: 'end' } },
    {
      popover: {
        title: '✅ Ready to go!',
        description: 'Hit <strong>Save</strong> to create the syllabus.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

