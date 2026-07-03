export interface NumberSeriesDefinition {
  id: number;
  seriesCode: string;
  seriesName: string;
  scopeType: string;
  prefix: string | null;
  separator: string;
  sequencePadding: number;
  description: string | null;
  active: boolean;
  canEditScopeType: boolean;
  currentPeriodLabel: string;
  currentLastSequence: number;
  currentLastGenerated: string | null;
  currentNextPreview: string | null;
  createdAt: string;
  updatedAt: string;
}

export const SCOPE_TYPE_OPTIONS = [
  { value: 'NONE',                label: 'Never resets (global)' },
  { value: 'CALENDAR_DAY',        label: 'Daily (calendar day)' },
  { value: 'CALENDAR_MONTH',      label: 'Monthly (calendar month)' },
  { value: 'CALENDAR_YEAR',       label: 'Yearly (calendar year)' },
  { value: 'FINANCIAL_MONTH',     label: 'Monthly (financial month)' },
  { value: 'FINANCIAL_YEAR',      label: 'Yearly (financial year)' },
  { value: 'ACADEMIC_YEAR',       label: 'Per academic year' },
  { value: 'COURSE',              label: 'Per course (never resets)' },
  { value: 'ACADEMIC_YEAR_COURSE', label: 'Per course × academic year' },
];
