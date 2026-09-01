package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EscortDutyDto;
import com.cms.model.Batch;
import com.cms.model.ClinicalShiftGroup;
import com.cms.model.EscortRotationAssignment;
import com.cms.model.RotationGroup;
import com.cms.repository.EscortRotationAssignmentRepository;
import com.cms.util.RotationParity;

/**
 * Resolves "whose turn is it" for a clinical Batch's escort-duty rotation on a given date (OC-175
 * Piece 3) — the escort-duty analog of {@link RotationResolverService}, sharing its parity math
 * via {@link RotationParity} but resolving a {@link com.cms.model.Faculty} instead of a
 * {@link Batch}, and with no slot dimension (escort duty has {@code slotOrder = 0} — a single
 * recurring duty, not interleaved subject slots).
 */
@Service
@Transactional(readOnly = true)
public class EscortRotationResolverService {

    private static final int ESCORT_SLOT_ORDER = 0;
    private static final int LOOKAHEAD_WEEKS = 12;

    private final EscortRotationAssignmentRepository escortAssignmentRepository;

    public EscortRotationResolverService(EscortRotationAssignmentRepository escortAssignmentRepository) {
        this.escortAssignmentRepository = escortAssignmentRepository;
    }

    /** Empty when the batch has no escort rotation pool configured. */
    public Optional<EscortDutyDto> resolveForDate(Long batchId, LocalDate date) {
        List<EscortRotationAssignment> assignments =
            escortAssignmentRepository.findByBatchIdOrderByRotationMember_MemberOrderAsc(batchId);
        if (assignments.isEmpty()) {
            return Optional.empty();
        }
        RotationGroup group = assignments.get(0).getRotationMember().getRotationGroup();
        int memberOrder = RotationParity.resolveMemberOrder(
            group.getAnchorOccurrenceDate(), group.getCycleLength(), date, ESCORT_SLOT_ORDER);

        return assignments.stream()
            .filter(a -> a.getRotationMember().getMemberOrder() == memberOrder)
            .findFirst()
            .map(a -> toDto(date, a));
    }

    /** This faculty's upcoming escort duties across every pool they belong to, looking ahead
     *  {@link #LOOKAHEAD_WEEKS} occurrences of each pool's batch's clinical shift day-of-week.
     *  Empty for a batch not linked to a {@link ClinicalShiftGroup} yet -- there's no recurring
     *  day to project from. */
    public List<EscortDutyDto> myUpcomingDuties(Long facultyId) {
        List<EscortDutyDto> duties = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (EscortRotationAssignment assignment : escortAssignmentRepository.findByFaculty_Id(facultyId)) {
            Batch batch = assignment.getBatch();
            ClinicalShiftGroup shiftGroup = batch.getClinicalShiftGroup();
            if (shiftGroup == null) {
                continue;
            }
            LocalDate candidate = nextOrSame(today, shiftGroup.getDayOfWeek());
            for (int i = 0; i < LOOKAHEAD_WEEKS; i++) {
                resolveForDate(batch.getId(), candidate)
                    .filter(duty -> duty.facultyId().equals(facultyId))
                    .ifPresent(duties::add);
                candidate = candidate.plusWeeks(1);
            }
        }
        return duties;
    }

    private LocalDate nextOrSame(LocalDate from, com.cms.model.enums.DayOfWeek target) {
        java.time.DayOfWeek javaTarget = java.time.DayOfWeek.valueOf(target.name());
        LocalDate date = from;
        while (date.getDayOfWeek() != javaTarget) {
            date = date.plusDays(1);
        }
        return date;
    }

    private EscortDutyDto toDto(LocalDate date, EscortRotationAssignment assignment) {
        Batch batch = assignment.getBatch();
        return new EscortDutyDto(
            date,
            batch.getId(),
            batch.getName(),
            assignment.getFaculty().getId(),
            assignment.getFaculty().getFirstName() + " " + assignment.getFaculty().getLastName(),
            batch.getClinicalVenue() != null ? batch.getClinicalVenue().getName() : null
        );
    }
}
