package com.cms.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cms.model.RotationMemberAssignment;
import com.cms.model.RotationSlot;
import com.cms.model.SessionOccurrence;
import com.cms.repository.RotationGroupRepository;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.RotationMemberRepository;
import com.cms.repository.RotationSlotRepository;
import com.cms.repository.SessionOccurrenceRepository;

/** Shared "make it safe to hard-delete these class_schedules ids" cleanup, used by every place
 *  that permanently removes DRAFT cells -- originally {@link TimetableGlobalAutoScheduleService
 *  #purgeDraftCellsForRebuild}, and now also {@link TimetableGenerationService#clear} (Discard),
 *  which used to call {@code classScheduleRepository.deleteAll} directly with no cleanup at all
 *  and could throw a raw FK violation the moment a cleared cell still had session_occurrences or
 *  rotation rows attached (e.g. a cell {@link TimetableGenerationService#revertToDraft} handed
 *  back to DRAFT without itself cleaning those up). Extracted here rather than duplicated so both
 *  call sites stay in lockstep with the FK chain instead of silently drifting apart over time. */
@Service
public class ClassScheduleCleanupService {

    private final SessionOccurrenceRepository sessionOccurrenceRepository;
    private final RotationGroupRepository rotationGroupRepository;
    private final RotationSlotRepository rotationSlotRepository;
    private final RotationMemberRepository rotationMemberRepository;
    private final RotationMemberAssignmentRepository rotationMemberAssignmentRepository;

    public ClassScheduleCleanupService(SessionOccurrenceRepository sessionOccurrenceRepository,
                                        RotationGroupRepository rotationGroupRepository,
                                        RotationSlotRepository rotationSlotRepository,
                                        RotationMemberRepository rotationMemberRepository,
                                        RotationMemberAssignmentRepository rotationMemberAssignmentRepository) {
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
        this.rotationGroupRepository = rotationGroupRepository;
        this.rotationSlotRepository = rotationSlotRepository;
        this.rotationMemberRepository = rotationMemberRepository;
        this.rotationMemberAssignmentRepository = rotationMemberAssignmentRepository;
    }

    /** Cleans up every {@link SessionOccurrence} still attached to a cell about to be hard-deleted,
     *  so the DELETE never trips session_occurrences' {@code class_schedule_id} FK. Occurrences are
     *  normally only ever materialized against a PUBLISHED schedule (absence substitution, room
     *  relocation, staff swap, progress logging all gate on it) -- but {@link
     *  TimetableGenerationService#revertToDraft} can hand a cohort's cell back to DRAFT without
     *  cleaning up the occurrences already logged against it, so a cell reachable here can carry
     *  real ones. The only thing that can actually block a hard delete is the self-referencing
     *  {@code swap_partner_occurrence_id} FK (no {@code ON DELETE} clause): if some OTHER occurrence
     *  outside this purge batch still points at one of these as its swap partner, that pointer is
     *  cleared first. Two occurrences that are BOTH being purged together need no unswapping between
     *  them -- the single batch DELETE removes both sides in one statement, so there's no dangling
     *  reference left to trip the FK. Every other column on an occurrence (effective_faculty_id,
     *  faculty_absence_id) is an outward-pointing reference to faculty/faculty_absences, not
     *  something else pointing in, so it never blocks the delete; session_occurrence_units cascades
     *  away with its parent occurrence automatically (V324). */
    public void purgeOccurrencesForCells(Set<Long> classScheduleIds) {
        List<Long> ids = new ArrayList<>(classScheduleIds);
        List<SessionOccurrence> occurrences = sessionOccurrenceRepository.findByClassSchedule_IdIn(ids);
        if (occurrences.isEmpty()) {
            return;
        }
        List<Long> occurrenceIds = occurrences.stream().map(SessionOccurrence::getId).collect(Collectors.toList());
        List<SessionOccurrence> externalSwapReferrers = sessionOccurrenceRepository
            .findBySwapPartnerOccurrence_IdIn(occurrenceIds).stream()
            .filter(occ -> !occurrenceIds.contains(occ.getId()))
            .collect(Collectors.toList());
        for (SessionOccurrence referrer : externalSwapReferrers) {
            referrer.setSwapPartnerOccurrence(null);
        }
        sessionOccurrenceRepository.saveAll(externalSwapReferrers);
        sessionOccurrenceRepository.deleteAllInBatch(occurrences);
    }

    /** Global Auto-Schedule fully owns creating a cross-offering {@link RotationSlot}-based
     *  rotation fresh every run (see its pairing sub-phase) -- so a rotation tied to a cell about to
     *  be hard-deleted has no future life and must not be left pointing at a now-gone cell.
     *  Hard-deleted here, not soft-deactivated: a {@link RotationSlot}/{@link
     *  RotationMemberAssignment}/{@link com.cms.model.RotationGroup} row has no other consumer once
     *  orphaned ({@link RotationResolverService} is read-only, queried only by id from a live slot).
     *  Deletes bottom-up (assignments, then slots, then any now-empty group and its members) to
     *  respect the FK chain -- a manually-created rotation from the "Set up Rotation" flyout is
     *  cleaned up the exact same way if any of its cells happen to be in the ids passed in, since
     *  nothing here distinguishes who created it. */
    public void purgeRotationRowsForCells(Set<Long> classScheduleIds) {
        List<Long> ids = new ArrayList<>(classScheduleIds);
        List<RotationSlot> slots = rotationSlotRepository.findByClassScheduleIdIn(ids);
        if (slots.isEmpty()) {
            return;
        }
        Set<Long> affectedGroupIds = slots.stream().map(s -> s.getRotationGroup().getId()).collect(Collectors.toSet());

        List<RotationMemberAssignment> assignments = rotationMemberAssignmentRepository.findByRotationSlot_ClassSchedule_IdIn(ids);
        rotationMemberAssignmentRepository.deleteAllInBatch(assignments);
        rotationSlotRepository.deleteAllInBatch(slots);

        for (Long groupId : affectedGroupIds) {
            long remainingSlots = rotationSlotRepository.countByRotationGroupId(groupId);
            if (remainingSlots == 0) {
                rotationMemberRepository.deleteAllInBatch(rotationMemberRepository.findByRotationGroupIdOrderByMemberOrderAsc(groupId));
                rotationGroupRepository.deleteById(groupId);
            }
        }
    }
}
