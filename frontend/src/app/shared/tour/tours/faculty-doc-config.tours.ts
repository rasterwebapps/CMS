import { TourDefinition, TourFlowMap } from '../tour.service';

export const FACULTY_DOC_CONFIG_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Faculty Document Requirements',
        description:
          'This screen lets admins define which documents are required from faculty based on their designation, speciality, or highest qualification. Let\'s take a quick walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fdoc-add-form',
      popover: {
        title: 'Add a Requirement Rule',
        description:
          'Select a <strong>Document Type</strong> and at least one matching criterion — Designation, Speciality, or Qualification. A document becomes required when <em>any</em> matching rule applies to a faculty member.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fdoc-table',
      popover: {
        title: 'Existing Rules Table',
        description:
          'All active requirement rules are listed here. Each row shows the document type and the conditions that trigger it. Use the <strong>Delete</strong> icon to remove a rule.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '📌 How Rules Work',
        description:
          'When a faculty profile is saved, the system checks all rules against the faculty\'s designation, speciality, and qualification. If any rule matches, that document is flagged as required on their profile.',
        side: 'over',
        align: 'center',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to configure faculty document requirements. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const FACULTY_DOC_CONFIG_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Faculty Doc Config', description: 'Define which documents are required from faculty based on designation, speciality, or qualification.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Add a Rule', icon: 'open', detail: 'Select a Document Type and at least one matching criterion — Designation, Speciality, or Qualification.' },
    { label: 'Existing Rules', icon: 'checklist', detail: 'All active requirement rules are listed, each showing the document type and its trigger conditions.' },
    { label: 'Auto-Apply', icon: 'send', detail: 'When a faculty profile is saved, all rules are checked — any match flags that document as required on their profile.' },
  ],
};

