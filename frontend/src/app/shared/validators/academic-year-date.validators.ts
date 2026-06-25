import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Parses a date-only value (e.g. "YYYY-MM-DD" from <input type="date">) as a local-timezone
 * date — see date.validators.ts for why this matters (UTC parsing shifts the day in IST).
 */
function parseDateOnlyLocal(value: string | null | undefined): Date | null {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function subtractDays(date: Date, days: number): Date {
  return shiftDays(date, -days);
}

function shiftDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

/** Formats local date components directly — avoids the UTC-shift issue toISOString() has in IST. */
function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Merges/clears a single custom error key on a control without disturbing its other errors. */
function setControlError(control: AbstractControl | null, key: string, errorValue: unknown): void {
  if (!control) return;
  const errors = { ...(control.errors ?? {}) };
  if (errorValue) {
    errors[key] = errorValue;
  } else {
    delete errors[key];
  }
  control.setErrors(Object.keys(errors).length ? errors : null);
}

/**
 * Cross-field validator for the academic year form. Mirrors the backend checks so invalid dates
 * surface inline instead of only after a failed save:
 *  - TermInstanceService: term start/end must fall within the academic year's dates (inclusive)
 *    and must not overlap the sibling term (gaps are allowed).
 *  - TermBillingScheduleService: due date must be on/before the term's end date, and no earlier
 *    than `getAdvanceDays()` days before the term starts.
 *
 * Sets errors directly on the offending child controls (not the group) so the form's existing
 * per-field isInvalid()/getError() pattern picks them up without template changes.
 */
export function academicYearDateRangeValidator(getAdvanceDays: () => number): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const ayStartCtrl   = group.get('startDate');
    const ayEndCtrl      = group.get('endDate');
    const oddStartCtrl  = group.get('oddStartDate');
    const oddEndCtrl      = group.get('oddEndDate');
    const evenStartCtrl = group.get('evenStartDate');
    const evenEndCtrl     = group.get('evenEndDate');
    const oddDueCtrl     = group.get('oddDueDate');
    const evenDueCtrl    = group.get('evenDueDate');

    const ayStart    = parseDateOnlyLocal(ayStartCtrl?.value);
    const ayEnd       = parseDateOnlyLocal(ayEndCtrl?.value);
    const oddStart   = parseDateOnlyLocal(oddStartCtrl?.value);
    const oddEnd       = parseDateOnlyLocal(oddEndCtrl?.value);
    const evenStart  = parseDateOnlyLocal(evenStartCtrl?.value);
    const evenEnd      = parseDateOnlyLocal(evenEndCtrl?.value);
    const oddDue      = parseDateOnlyLocal(oddDueCtrl?.value);
    const evenDue     = parseDateOnlyLocal(evenDueCtrl?.value);

    // Each term's own end must be after its own start.
    setControlError(oddEndCtrl, 'dateOrder', !!(oddStart && oddEnd && oddEnd <= oddStart));
    setControlError(evenEndCtrl, 'dateOrder', !!(evenStart && evenEnd && evenEnd <= evenStart));

    // Term dates must fall within the academic year's own dates (inclusive of the boundary).
    setControlError(oddStartCtrl, 'outOfAcademicYear', !!(ayStart && oddStart && oddStart < ayStart));
    setControlError(oddEndCtrl, 'outOfAcademicYear', !!(ayEnd && oddEnd && oddEnd > ayEnd));
    setControlError(evenStartCtrl, 'outOfAcademicYear', !!(ayStart && evenStart && evenStart < ayStart));
    setControlError(evenEndCtrl, 'outOfAcademicYear', !!(ayEnd && evenEnd && evenEnd > ayEnd));

    // ODD and EVEN terms must not overlap each other (gaps between them are fine).
    const overlaps = !!(oddStart && oddEnd && evenStart && evenEnd &&
      oddStart <= evenEnd && evenStart <= oddEnd);
    setControlError(oddEndCtrl, 'termOverlap', overlaps);
    setControlError(evenStartCtrl, 'termOverlap', overlaps);

    // Billing due date must be on/before the term's end date, and no earlier than the
    // configurable advance window before the term starts.
    if (oddStart && oddEnd && oddDue) {
      const earliest = subtractDays(oddStart, getAdvanceDays());
      const outOfRange = oddDue < earliest || oddDue > oddEnd;
      setControlError(oddDueCtrl, 'dueDateRange',
        outOfRange ? { earliest: toIsoDate(earliest), latest: toIsoDate(oddEnd) } : null);
    } else {
      setControlError(oddDueCtrl, 'dueDateRange', null);
    }

    if (evenStart && evenEnd && evenDue) {
      const earliest = subtractDays(evenStart, getAdvanceDays());
      const outOfRange = evenDue < earliest || evenDue > evenEnd;
      setControlError(evenDueCtrl, 'dueDateRange',
        outOfRange ? { earliest: toIsoDate(earliest), latest: toIsoDate(evenEnd) } : null);
    } else {
      setControlError(evenDueCtrl, 'dueDateRange', null);
    }

    return null;
  };
}

export interface AcademicYearRange {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
}

/**
 * Validates the academic year's own start/end dates against every other existing academic year
 * (the list passed in should already exclude the year being edited). Mirrors the backend's
 * AcademicYearRepository.existsOverlapping inclusive-overlap check.
 */
export function academicYearOverlapValidator(getOthers: () => AcademicYearRange[]): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const startCtrl = group.get('startDate');
    const endCtrl = group.get('endDate');
    const start = parseDateOnlyLocal(startCtrl?.value);
    const end = parseDateOnlyLocal(endCtrl?.value);

    if (!start || !end) {
      setControlError(startCtrl, 'academicYearOverlap', null);
      setControlError(endCtrl, 'academicYearOverlap', null);
      return null;
    }

    const conflict = getOthers().find((other) => {
      const otherStart = parseDateOnlyLocal(other.startDate);
      const otherEnd = parseDateOnlyLocal(other.endDate);
      return !!(otherStart && otherEnd && start <= otherEnd && otherStart <= end);
    });

    const errorValue = conflict ? { name: conflict.name } : null;
    setControlError(startCtrl, 'academicYearOverlap', errorValue);
    setControlError(endCtrl, 'academicYearOverlap', errorValue);
    return null;
  };
}

export interface AcademicYearDateBounds {
  ayStartMin: string | null;   ayStartMax: string | null;
  ayEndMin: string | null;     ayEndMax: string | null;
  oddStartMin: string | null;  oddStartMax: string | null;
  oddEndMin: string | null;    oddEndMax: string | null;
  evenStartMin: string | null; evenStartMax: string | null;
  evenEndMin: string | null;   evenEndMax: string | null;
  oddDueMin: string | null;    oddDueMax: string | null;
  evenDueMin: string | null;   evenDueMax: string | null;
}

/**
 * Computes <input type="date"> min/max attribute values so the picker itself can't land on a
 * date that the form's validators would reject — locking the selection instead of only flagging
 * it after the fact. `others` should already exclude the academic year being edited.
 */
export function computeAcademicYearDateBounds(
  formValue: {
    startDate?: string | null; endDate?: string | null;
    oddStartDate?: string | null; oddEndDate?: string | null;
    evenStartDate?: string | null; evenEndDate?: string | null;
  },
  others: AcademicYearRange[],
  advanceDays: number,
): AcademicYearDateBounds {
  const start    = parseDateOnlyLocal(formValue.startDate);
  const end       = parseDateOnlyLocal(formValue.endDate);
  const oddStart = parseDateOnlyLocal(formValue.oddStartDate);
  const oddEnd     = parseDateOnlyLocal(formValue.oddEndDate);
  const evenStart= parseDateOnlyLocal(formValue.evenStartDate);
  const evenEnd    = parseDateOnlyLocal(formValue.evenEndDate);

  // Nearest other academic year ending before, and starting after, wherever this year currently
  // sits (anchored on whichever of start/end has been picked first).
  const anchor = start ?? end;
  let prevEnd: Date | null = null;
  let nextStart: Date | null = null;
  if (anchor) {
    for (const other of others) {
      const otherStart = parseDateOnlyLocal(other.startDate);
      const otherEnd = parseDateOnlyLocal(other.endDate);
      if (otherEnd && otherEnd < anchor && (!prevEnd || otherEnd > prevEnd)) prevEnd = otherEnd;
      if (otherStart && otherStart > anchor && (!nextStart || otherStart < nextStart)) nextStart = otherStart;
    }
  }

  return {
    ayStartMin: prevEnd ? toIsoDate(shiftDays(prevEnd, 1)) : null,
    ayStartMax: end ? toIsoDate(subtractDays(end, 1)) : (nextStart ? toIsoDate(subtractDays(nextStart, 1)) : null),
    ayEndMin: start ? toIsoDate(shiftDays(start, 1)) : (prevEnd ? toIsoDate(shiftDays(prevEnd, 1)) : null),
    ayEndMax: nextStart ? toIsoDate(subtractDays(nextStart, 1)) : null,

    oddStartMin: start ? toIsoDate(start) : null,
    oddStartMax: oddEnd ? toIsoDate(oddEnd) : (end ? toIsoDate(end) : null),
    oddEndMin: oddStart ? toIsoDate(oddStart) : (start ? toIsoDate(start) : null),
    oddEndMax: evenStart ? toIsoDate(subtractDays(evenStart, 1)) : (end ? toIsoDate(end) : null),

    evenStartMin: oddEnd ? toIsoDate(shiftDays(oddEnd, 1)) : (start ? toIsoDate(start) : null),
    evenStartMax: evenEnd ? toIsoDate(evenEnd) : (end ? toIsoDate(end) : null),
    evenEndMin: evenStart ? toIsoDate(evenStart) : (start ? toIsoDate(start) : null),
    evenEndMax: end ? toIsoDate(end) : null,

    oddDueMin: oddStart ? toIsoDate(subtractDays(oddStart, advanceDays)) : null,
    oddDueMax: oddEnd ? toIsoDate(oddEnd) : null,
    evenDueMin: evenStart ? toIsoDate(subtractDays(evenStart, advanceDays)) : null,
    evenDueMax: evenEnd ? toIsoDate(evenEnd) : null,
  };
}
