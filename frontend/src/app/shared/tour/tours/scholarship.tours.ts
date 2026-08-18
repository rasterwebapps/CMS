import { TourDefinition, TourFlowMap } from '../tour.service';

export const SCHOLARSHIP_TYPE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Scholarship Types',
        description:
          'This screen lets you manage scholarship master schemes — discount rules, renewal settings, and government scheme linkages. Let\'s take a quick walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-scholarship-header',
      popover: {
        title: 'Page Header',
        description:
          'The header shows the section title and a brief description. The <strong>Add Scholarship</strong> button opens the scholarship creation form.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-scholarship-search',
      popover: {
        title: 'Search Scholarships',
        description:
          'Type a scholarship name or code to filter the list instantly.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-scholarship-table',
      popover: {
        title: 'Scholarship Table',
        description:
          'Each row shows the scholarship code, name, discount type, discount value, renewal requirement, and active status. Use the action icons to edit or delete a scheme.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to navigate the Scholarship Types screen. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const SCHOLARSHIP_TYPE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Scholarship Types', description: 'Master list of scholarship types available for students to apply against, with their discount rules.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search Scholarships', icon: 'search', detail: 'Search scholarship types by code or name.' },
    { label: 'Scholarship Table', icon: 'checklist', detail: 'Each row shows the scholarship code, name, and discount type/value.' },
    { label: 'Add', icon: 'open', detail: 'Create a new scholarship type with a discount type (percentage or fixed) and value.' },
    { label: 'Save', icon: 'send', detail: 'Save with a real-time uniqueness check on the scholarship code.' },
  ],
};

export const SCHOLARSHIP_TYPE_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Scholarship Form',
        description:
          'This form lets you create or edit a scholarship scheme. Let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-scholarship-code',
      popover: {
        title: 'Scholarship Code',
        description:
          'A unique uppercase identifier — e.g., <strong>SC_GOVT</strong>, <strong>MERIT_25</strong>. Used in fee reports and student records.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-scholarship-name',
      popover: {
        title: 'Scholarship Name',
        description:
          'Full human-readable name of the scholarship — e.g., <em>SC Government Scholarship</em>. Required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-scholarship-discount-type',
      popover: {
        title: 'Discount Type',
        description:
          '<strong>PERCENTAGE</strong> — reduces fee by a percent.<br><strong>FIXED_AMOUNT</strong> — deducts a fixed rupee amount.<br><strong>FULL_WAIVER</strong> — waives the entire fee.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-scholarship-discount-value',
      popover: {
        title: 'Discount Value',
        description:
          'Enter the percentage (0–100) or fixed amount (₹) based on the discount type selected above.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-scholarship-submit',
      popover: {
        title: 'Save the Scholarship',
        description:
          'Click <strong>Save Scholarship</strong> to create (or update) the scheme. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the scholarship form. Fill in the details and hit <strong>Save Scholarship</strong> to register it.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

