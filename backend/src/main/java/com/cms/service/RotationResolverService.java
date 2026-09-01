package com.cms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.RotationMember;
import com.cms.model.RotationMemberAssignment;
import com.cms.model.RotationSlot;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.RotationMemberRepository;
import com.cms.repository.RotationSlotRepository;
import com.cms.util.RotationParity;

/**
 * Resolves which physical group (and therefore which existing per-subject {@link com.cms.model.Batch})
 * actually occupies a rotating {@link com.cms.model.ClassSchedule} cell on a given calendar date.
 * Counts whole weeks elapsed since {@link com.cms.model.RotationGroup#getAnchorOccurrenceDate()}
 * (not raw ISO week numbers, to stay predictable across year boundaries) and shifts the slot's
 * fixed {@code slotOrder} by that count, modulo the cycle length — a simple cyclic (Latin-square)
 * rotation that generalizes from a 2-way swap to any N-way rotation.
 */
@Service
@Transactional(readOnly = true)
public class RotationResolverService {

    private final RotationSlotRepository rotationSlotRepository;
    private final RotationMemberRepository rotationMemberRepository;
    private final RotationMemberAssignmentRepository rotationMemberAssignmentRepository;

    public RotationResolverService(RotationSlotRepository rotationSlotRepository,
                                    RotationMemberRepository rotationMemberRepository,
                                    RotationMemberAssignmentRepository rotationMemberAssignmentRepository) {
        this.rotationSlotRepository = rotationSlotRepository;
        this.rotationMemberRepository = rotationMemberRepository;
        this.rotationMemberAssignmentRepository = rotationMemberAssignmentRepository;
    }

    /** Empty when the given cell isn't part of any rotation group. */
    public Optional<RotationMemberAssignment> resolveEffectiveAssignment(Long classScheduleId, LocalDate date) {
        return rotationSlotRepository.findByClassScheduleId(classScheduleId)
            .flatMap(slot -> resolveEffectiveAssignment(slot, date));
    }

    public Optional<RotationMemberAssignment> resolveEffectiveAssignment(RotationSlot slot, LocalDate date) {
        var group = slot.getRotationGroup();
        int cycleLength = group.getCycleLength();
        int memberOrder = RotationParity.resolveMemberOrder(
            group.getAnchorOccurrenceDate(), cycleLength, date, slot.getSlotOrder());

        List<RotationMember> members = rotationMemberRepository.findByRotationGroupIdOrderByMemberOrderAsc(group.getId());
        return members.stream()
            .filter(m -> m.getMemberOrder() == memberOrder)
            .findFirst()
            .flatMap(member -> rotationMemberAssignmentRepository.findByRotationMemberIdAndRotationSlotId(member.getId(), slot.getId()));
    }

    /** Any one member's assignment for a slot is enough to resolve the shared venue (all members
     *  at a slot are validated to share the same venue at rotation-group creation time), even
     *  without a specific date — used by staffing, which needs venue but not "whose turn". */
    public Optional<RotationMemberAssignment> anyAssignmentForSlot(Long classScheduleId) {
        return allAssignmentsForSlot(classScheduleId).stream().findFirst();
    }

    /** Every member's assignment for a slot, ordered by member order — used to list all
     *  rotating batch names on a cell, and to size a venue against the largest rotating roster. */
    public List<RotationMemberAssignment> allAssignmentsForSlot(Long classScheduleId) {
        return rotationSlotRepository.findByClassScheduleId(classScheduleId)
            .map(slot -> rotationMemberAssignmentRepository.findByRotationSlotIdOrderByRotationMember_MemberOrderAsc(slot.getId()))
            .orElse(List.of());
    }
}
