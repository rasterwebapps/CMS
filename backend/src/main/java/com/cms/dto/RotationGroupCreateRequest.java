package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** cycleLength is not sent by the client — it's derived server-side as {@code slots.size()} and
 *  cross-checked against {@code members.size()} (a rotation group is always a square rotation:
 *  N slots, N members). */
public record RotationGroupCreateRequest(
    @NotNull Long termInstanceId,
    @NotNull String label,
    @NotNull LocalDate anchorOccurrenceDate,
    @NotEmpty @Size(min = 2) List<RotationSlotInput> slots,
    @NotEmpty @Size(min = 2) List<RotationMemberInput> members
) {

    public record RotationSlotInput(@NotNull Long classScheduleId, @NotNull Integer slotOrder) {}

    public record RotationMemberInput(
        @NotNull Integer memberOrder,
        @NotNull String label,
        @NotEmpty List<RotationAssignmentInput> assignments
    ) {}

    public record RotationAssignmentInput(@NotNull Long classScheduleId, @NotNull Long batchId) {}
}
