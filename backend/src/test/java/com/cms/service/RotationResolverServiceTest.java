package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.Batch;
import com.cms.model.RotationGroup;
import com.cms.model.RotationMember;
import com.cms.model.RotationMemberAssignment;
import com.cms.model.RotationSlot;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.RotationMemberRepository;
import com.cms.repository.RotationSlotRepository;

@ExtendWith(MockitoExtension.class)
class RotationResolverServiceTest {

    @Mock private RotationSlotRepository rotationSlotRepository;
    @Mock private RotationMemberRepository rotationMemberRepository;
    @Mock private RotationMemberAssignmentRepository rotationMemberAssignmentRepository;

    private RotationResolverService service;

    @BeforeEach
    void setUp() {
        service = new RotationResolverService(rotationSlotRepository, rotationMemberRepository, rotationMemberAssignmentRepository);
    }

    /** Batch 1 <-> Batch 2 swapping through a shared Wed P3-4 slot, week to week — the exact
     *  scenario the feature was built for. Anchor date is itself a Wednesday. */
    @Test
    void flipsEffectiveBatchEveryWeekForATwoWayRotation() {
        RotationGroup group = new RotationGroup();
        group.setId(1L);
        group.setCycleLength(2);
        group.setAnchorOccurrenceDate(LocalDate.of(2026, 8, 5));

        RotationSlot englishSlot = new RotationSlot();
        englishSlot.setId(10L);
        englishSlot.setRotationGroup(group);
        englishSlot.setSlotOrder(0);

        RotationMember batch1 = new RotationMember();
        batch1.setId(100L);
        batch1.setMemberOrder(0);
        batch1.setLabel("Batch 1");
        RotationMember batch2 = new RotationMember();
        batch2.setId(101L);
        batch2.setMemberOrder(1);
        batch2.setLabel("Batch 2");
        when(rotationMemberRepository.findByRotationGroupIdOrderByMemberOrderAsc(1L)).thenReturn(List.of(batch1, batch2));

        Batch englishBatch1 = new Batch();
        englishBatch1.setId(1000L);
        englishBatch1.setName("English Batch 1");
        Batch englishBatch2 = new Batch();
        englishBatch2.setId(1001L);
        englishBatch2.setName("English Batch 2");
        RotationMemberAssignment assignmentBatch1 = new RotationMemberAssignment(batch1, englishSlot, englishBatch1);
        RotationMemberAssignment assignmentBatch2 = new RotationMemberAssignment(batch2, englishSlot, englishBatch2);
        when(rotationMemberAssignmentRepository.findByRotationMemberIdAndRotationSlotId(100L, 10L))
            .thenReturn(Optional.of(assignmentBatch1));
        when(rotationMemberAssignmentRepository.findByRotationMemberIdAndRotationSlotId(101L, 10L))
            .thenReturn(Optional.of(assignmentBatch2));

        // Week 0 (the anchor date itself): Batch 1 is in the English slot.
        assertThat(service.resolveEffectiveAssignment(englishSlot, LocalDate.of(2026, 8, 5)))
            .contains(assignmentBatch1);

        // Week 1 (7 days later): flips to Batch 2.
        assertThat(service.resolveEffectiveAssignment(englishSlot, LocalDate.of(2026, 8, 12)))
            .contains(assignmentBatch2);

        // Week 2: back to Batch 1 (mod 2).
        assertThat(service.resolveEffectiveAssignment(englishSlot, LocalDate.of(2026, 8, 19)))
            .contains(assignmentBatch1);

        // Week 3: Batch 2 again.
        assertThat(service.resolveEffectiveAssignment(englishSlot, LocalDate.of(2026, 8, 26)))
            .contains(assignmentBatch2);
    }

    @Test
    void returnsEmptyWhenCellIsNotPartOfAnyRotationGroup() {
        when(rotationSlotRepository.findByClassScheduleId(999L)).thenReturn(Optional.empty());

        assertThat(service.resolveEffectiveAssignment(999L, LocalDate.of(2026, 8, 5))).isEmpty();
        assertThat(service.anyAssignmentForSlot(999L)).isEmpty();
        assertThat(service.allAssignmentsForSlot(999L)).isEmpty();
    }
}
