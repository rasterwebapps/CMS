package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.ClinicalShiftGroup;
import com.cms.model.CourseOffering;
import com.cms.model.enums.DayOfWeek;

/**
 * A {@link ClinicalShiftGroup}'s wall-clock window, including bus travel buffer, computed the
 * same way {@code ClinicalShiftGroupService#toDto} derives {@code clinicalEndTime}/
 * {@code busDepartTime}/{@code busReturnTime}. Used by the Timetable engine to hard-block
 * on-campus Period placement for a Program that has opted into Clinical Shift scheduling.
 */
public record ClinicalShiftWindow(
    Long shiftGroupId,
    String label,
    DayOfWeek dayOfWeek,
    LocalTime busDepart,
    LocalTime clinicalStart,
    LocalTime clinicalEnd,
    LocalTime busReturn
) {
    public static ClinicalShiftWindow from(ClinicalShiftGroup group) {
        CourseOffering offering = group.getCourseOffering();
        LocalTime clinicalEnd = offering.getClinicalShiftDurationMinutes() == null ? null
            : group.getClinicalStartTime().plusMinutes(offering.getClinicalShiftDurationMinutes());
        Integer buffer = offering.getClinicalTravelBufferMinutes();
        LocalTime busDepart = buffer == null ? null : group.getClinicalStartTime().minusMinutes(buffer);
        LocalTime busReturn = (buffer == null || clinicalEnd == null) ? null : clinicalEnd.plusMinutes(buffer);
        return new ClinicalShiftWindow(group.getId(), group.getLabel(), group.getDayOfWeek(),
            busDepart, group.getClinicalStartTime(), clinicalEnd, busReturn);
    }

    /** True if [start,end) genuinely overlaps [busDepart,busReturn). A null busDepart/busReturn
     *  means the offering's shift duration/buffer isn't configured yet, so it never blocks. */
    public boolean overlaps(LocalTime start, LocalTime end) {
        return busDepart != null && busReturn != null && start.isBefore(busReturn) && busDepart.isBefore(end);
    }
}
