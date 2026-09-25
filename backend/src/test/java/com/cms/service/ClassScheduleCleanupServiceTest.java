package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.RotationGroup;
import com.cms.model.RotationMemberAssignment;
import com.cms.model.RotationSlot;
import com.cms.model.SessionOccurrence;
import com.cms.repository.RotationGroupRepository;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.RotationMemberRepository;
import com.cms.repository.RotationSlotRepository;
import com.cms.repository.SessionOccurrenceRepository;

@ExtendWith(MockitoExtension.class)
class ClassScheduleCleanupServiceTest {

    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;
    @Mock private RotationGroupRepository rotationGroupRepository;
    @Mock private RotationSlotRepository rotationSlotRepository;
    @Mock private RotationMemberRepository rotationMemberRepository;
    @Mock private RotationMemberAssignmentRepository rotationMemberAssignmentRepository;

    private ClassScheduleCleanupService service;

    @BeforeEach
    void setUp() {
        service = new ClassScheduleCleanupService(sessionOccurrenceRepository, rotationGroupRepository,
            rotationSlotRepository, rotationMemberRepository, rotationMemberAssignmentRepository);
    }

    /** A session_occurrences row tied to a cell about to be hard-deleted is itself hard-deleted,
     *  and any OTHER occurrence pointing at it as a Phase 7 swap partner is unlinked first -- the
     *  self-referencing swap_partner_occurrence_id FK (no ON DELETE clause) is the only thing that
     *  could otherwise block the class_schedules delete. */
    @Test
    void unswapsExternalPartnerThenPurgesOccurrencesTiedToTheGivenCells() {
        SessionOccurrence purgedOccurrence = new SessionOccurrence();
        purgedOccurrence.setId(5001L);
        when(sessionOccurrenceRepository.findByClassSchedule_IdIn(List.of(901L))).thenReturn(List.of(purgedOccurrence));

        // Some other, unrelated occurrence still calls this one its swap partner.
        SessionOccurrence externalSwapPartner = new SessionOccurrence();
        externalSwapPartner.setId(5002L);
        externalSwapPartner.setSwapPartnerOccurrence(purgedOccurrence);
        when(sessionOccurrenceRepository.findBySwapPartnerOccurrence_IdIn(List.of(5001L)))
            .thenReturn(List.of(externalSwapPartner));

        service.purgeOccurrencesForCells(Set.of(901L));

        assertThat(externalSwapPartner.getSwapPartnerOccurrence()).isNull();
        verify(sessionOccurrenceRepository).saveAll(List.of(externalSwapPartner));
        verify(sessionOccurrenceRepository).deleteAllInBatch(List.of(purgedOccurrence));
    }

    /** Two occurrences that are BOTH being purged together (e.g. a swap between two sessions that
     *  are both getting hard-deleted) need no unswapping between them -- neither one is "external"
     *  to the batch, so both go straight into the single deleteAllInBatch call with no save first. */
    @Test
    void doesNotUnswapTwoOccurrencesThatAreBothInTheSamePurgeBatch() {
        SessionOccurrence occA = new SessionOccurrence();
        occA.setId(5001L);
        SessionOccurrence occB = new SessionOccurrence();
        occB.setId(5002L);
        occA.setSwapPartnerOccurrence(occB);
        occB.setSwapPartnerOccurrence(occA);
        // Set<Long> -> new ArrayList<>(...) iteration order isn't guaranteed, so match on contents
        // instead of a fixed [901, 902] ordering.
        when(sessionOccurrenceRepository.findByClassSchedule_IdIn(anyList())).thenReturn(List.of(occA, occB));
        when(sessionOccurrenceRepository.findBySwapPartnerOccurrence_IdIn(anyList())).thenReturn(List.of(occA, occB));

        service.purgeOccurrencesForCells(Set.of(901L, 902L));

        // Both occurrences are inside the batch, so neither is an "external" referrer -- their
        // mutual swap link is left exactly as-is (saveAll runs with an empty list, a no-op).
        assertThat(occA.getSwapPartnerOccurrence()).isEqualTo(occB);
        assertThat(occB.getSwapPartnerOccurrence()).isEqualTo(occA);
        verify(sessionOccurrenceRepository).saveAll(List.of());
        verify(sessionOccurrenceRepository).deleteAllInBatch(List.of(occA, occB));
    }

    /** No occurrences at all for the given cells (the ordinary case -- occurrences only ever exist
     *  against a PUBLISHED schedule, so a fresh, never-published DRAFT cell has none) is a no-op:
     *  no lookups for swap partners, no save, no delete. */
    @Test
    void doesNothingWhenNoCellsHaveAnyOccurrences() {
        when(sessionOccurrenceRepository.findByClassSchedule_IdIn(List.of(901L))).thenReturn(List.of());

        service.purgeOccurrencesForCells(Set.of(901L));

        verify(sessionOccurrenceRepository, never()).findBySwapPartnerOccurrence_IdIn(anyList());
        verify(sessionOccurrenceRepository, never()).saveAll(anyList());
        verify(sessionOccurrenceRepository, never()).deleteAllInBatch(anyList());
    }

    /** Deletes bottom-up (assignments, then slots, then any now-empty group and its members) to
     *  respect the FK chain -- a rotation group with another slot still standing (a different cell,
     *  not in this purge) must itself survive. */
    @Test
    void purgesRotationRowsBottomUpAndDeletesAGroupOnlyOnceItsLastSlotIsGone() {
        RotationGroup group = new RotationGroup();
        group.setId(77L);

        RotationSlot slot = new RotationSlot();
        slot.setId(10L);
        slot.setRotationGroup(group);
        when(rotationSlotRepository.findByClassScheduleIdIn(List.of(901L))).thenReturn(List.of(slot));

        RotationMemberAssignment assignment = new RotationMemberAssignment();
        assignment.setId(20L);
        when(rotationMemberAssignmentRepository.findByRotationSlot_ClassSchedule_IdIn(List.of(901L)))
            .thenReturn(List.of(assignment));
        when(rotationSlotRepository.countByRotationGroupId(77L)).thenReturn(0L);
        when(rotationMemberRepository.findByRotationGroupIdOrderByMemberOrderAsc(77L)).thenReturn(List.of());

        service.purgeRotationRowsForCells(Set.of(901L));

        verify(rotationMemberAssignmentRepository).deleteAllInBatch(List.of(assignment));
        verify(rotationSlotRepository).deleteAllInBatch(List.of(slot));
        verify(rotationGroupRepository).deleteById(77L);
    }

    /** A rotation group that still has another slot standing after this purge (a sibling cell not
     *  in this batch) must not be deleted -- only its purged slot/assignment rows go. */
    @Test
    void leavesARotationGroupStandingWhenItStillHasOtherSlots() {
        RotationGroup group = new RotationGroup();
        group.setId(77L);

        RotationSlot slot = new RotationSlot();
        slot.setId(10L);
        slot.setRotationGroup(group);
        when(rotationSlotRepository.findByClassScheduleIdIn(List.of(901L))).thenReturn(List.of(slot));
        when(rotationMemberAssignmentRepository.findByRotationSlot_ClassSchedule_IdIn(List.of(901L))).thenReturn(List.of());
        when(rotationSlotRepository.countByRotationGroupId(77L)).thenReturn(1L);

        service.purgeRotationRowsForCells(Set.of(901L));

        verify(rotationSlotRepository).deleteAllInBatch(List.of(slot));
        verify(rotationGroupRepository, never()).deleteById(77L);
        verify(rotationMemberRepository, never()).deleteAllInBatch(anyList());
    }

    /** No rotation slots at all for the given cells is a no-op -- no assignment lookup, no group
     *  cleanup pass. */
    @Test
    void doesNothingWhenNoCellsHaveAnyRotationSlots() {
        when(rotationSlotRepository.findByClassScheduleIdIn(List.of(901L))).thenReturn(List.of());

        service.purgeRotationRowsForCells(Set.of(901L));

        verify(rotationMemberAssignmentRepository, never()).findByRotationSlot_ClassSchedule_IdIn(anyList());
        verify(rotationSlotRepository, never()).deleteAllInBatch(anyList());
    }
}
