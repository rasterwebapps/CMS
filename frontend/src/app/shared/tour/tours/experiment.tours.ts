import { TourDefinition, TourFlowMap } from '../tour.service';

// Same curriculum-authoring funnel as curriculum-version.tours.ts (duplicated,
// not imported, so each tours.ts file stays self-contained).
const CURRICULUM_AUTHORING_FUNNEL = [
  { label: 'Curriculum Versions', description: 'Define versioned term/subject mappings for each program.' },
  { label: 'Syllabus', description: 'Author credit hours, objectives, content, books, and outcomes for each course.' },
  { label: 'Experiments', description: 'Define lab experiments with procedure, apparatus, and learning outcomes for each subject.' },
];

export const EXPERIMENT_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🧪 Welcome to Experiments',
        description:
          'This screen lets you define lab experiments — number them within a subject, capture procedure, apparatus, and learning outcomes. Let\'s walk through it.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-exp-add-btn',
      popover: {
        title: 'Add Experiment',
        description:
          'Click here to define a new lab experiment with subject, sequence number, and procedure.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-exp-search',
      popover: {
        title: 'Search Experiments',
        description: 'Quickly filter by name, subject, or apparatus.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-exp-columns',
      popover: {
        title: 'Toggle Columns',
        description: 'Show or hide table columns based on what you want to focus on. Your selection is remembered.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-exp-content',
      popover: {
        title: 'Experiments Table',
        description: 'Sort by any column header. Hover a row for <strong>Edit</strong> and <strong>Delete</strong> actions.',
        side: 'top',
        align: 'start',
      },
    },
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

export const EXPERIMENT_LIST_FLOW_MAP: TourFlowMap = {
  funnel: CURRICULUM_AUTHORING_FUNNEL,
  currentIndex: 2,
  steps: [
    { label: 'Search Experiments', icon: 'search', detail: 'Quickly filter by name, subject, or apparatus.' },
    { label: 'Experiments Table', icon: 'checklist', detail: 'Sort by any column; hover a row for Edit and Delete actions.' },
    { label: 'Add Experiment', icon: 'open', detail: 'Define a new lab experiment with subject, sequence number, and procedure.' },
    { label: 'Save', icon: 'send', detail: 'Save the aim, apparatus, procedure, and expected/learning outcomes.' },
  ],
};

export const EXPERIMENT_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Experiment Form',
        description: 'Create or edit a lab experiment — let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    { element: '#exp-subject', popover: { title: 'Subject', description: 'The subject this experiment belongs to.', side: 'bottom', align: 'start' } },
    { element: '#exp-number', popover: { title: 'Experiment Number', description: 'Sequence number within the subject (1, 2, 3, …).', side: 'bottom', align: 'start' } },
    { element: '#exp-name', popover: { title: 'Name', description: 'Short title of the experiment.', side: 'bottom', align: 'start' } },
    { element: '#exp-description', popover: { title: 'Description', description: 'High-level summary of what the experiment is about.', side: 'bottom', align: 'start' } },
    { element: '#exp-aim', popover: { title: 'Aim', description: 'Stated objective of the experiment.', side: 'bottom', align: 'start' } },
    { element: '#exp-apparatus', popover: { title: 'Apparatus', description: 'List of apparatus, instruments, or chemicals required.', side: 'bottom', align: 'start' } },
    { element: '#exp-procedure', popover: { title: 'Procedure', description: 'Step-by-step procedure students must follow.', side: 'bottom', align: 'start' } },
    { element: '#exp-outcome', popover: { title: 'Expected Outcome', description: 'What the result should look like when performed correctly.', side: 'bottom', align: 'start' } },
    { element: '#exp-learning', popover: { title: 'Learning Outcomes', description: 'Skills and concepts the student should master.', side: 'bottom', align: 'start' } },
    { element: '#exp-duration', popover: { title: 'Duration (Hours)', description: 'Expected duration of the lab session.', side: 'bottom', align: 'start' } },
    { element: '#tour-exp-submit', popover: { title: 'Save the Experiment', description: 'Click here to save. The button is disabled while saving.', side: 'top', align: 'end' } },
    {
      popover: {
        title: '✅ Ready to go!',
        description: 'Hit <strong>Save</strong> to create the experiment.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const EXPERIMENT_FORM_FLOW_MAP: TourFlowMap = {
  funnel: CURRICULUM_AUTHORING_FUNNEL,
  currentIndex: 2,
  steps: [
    { label: 'Subject & Sequence', icon: 'checklist', detail: 'Pick the subject and sequence number, then name and describe the experiment.' },
    { label: 'Aim, Apparatus & Procedure', icon: 'open', detail: 'Stated objective, required apparatus, and the step-by-step procedure students must follow.' },
    { label: 'Outcomes & Duration', icon: 'checklist', detail: 'Expected outcome, learning outcomes, and expected session duration.' },
    { label: 'Save', icon: 'send', detail: 'Save the experiment.' },
  ],
};

