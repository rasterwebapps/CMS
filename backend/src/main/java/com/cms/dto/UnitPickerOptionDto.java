package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.AttendanceType;

/** One selectable unit in the "Log Progress" dialog, with its aggregate state across every
 *  session logged so far (not just the date being edited) so the picker can default-select the
 *  actual current unit instead of showing all units with no sense of where things stand. */
public record UnitPickerOptionDto(
    Long id,
    Integer unitNumber,
    String title,
    AttendanceType componentType,
    Integer plannedHours,
    BigDecimal hoursLoggedSoFar,
    boolean markedComplete
) {}
