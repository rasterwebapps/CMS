package com.cms.service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.RotationCandidateSlotResponse;
import com.cms.dto.RotationEffectiveResponse;
import com.cms.dto.RotationGroupCreateRequest;
import com.cms.dto.RotationGroupCreateRequest.RotationAssignmentInput;
import com.cms.dto.RotationGroupCreateRequest.RotationMemberInput;
import com.cms.dto.RotationGroupCreateRequest.RotationSlotInput;
import com.cms.dto.RotationGroupResponse;
import com.cms.dto.RotationGroupResponse.RotationAssignmentResponse;
import com.cms.dto.RotationGroupResponse.RotationMemberResponse;
import com.cms.dto.RotationGroupResponse.RotationSlotResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.RotationGroup;
import com.cms.model.RotationMember;
import com.cms.model.RotationMemberAssignment;
import com.cms.model.RotationSlot;
import com.cms.model.Student;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.RotationGroupRepository;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.RotationMemberRepository;
import com.cms.repository.RotationSlotRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Sets up a {@link RotationGroup}: N already-placed {@link ClassSchedule} cells sharing the same
 * day+period (e.g. "English Lab, Wed P3-4" + "Tamil Lab, Wed P3-4") linked to N physical groups
 * of students that alternate through them week to week, each represented at each slot by an
 * existing per-subject {@link Batch} (created the normal way, via Capacity Planner). Creating a
 * rotation group nulls out {@link ClassSchedule#getBatch()} on every linked cell — its occupant
 * is resolved per-date by {@link RotationResolverService} from then on instead of being fixed.
 */
@Service
@Transactional(readOnly = true)
public class RotationGroupService {

    private final RotationGroupRepository rotationGroupRepository;
    private final RotationSlotRepository rotationSlotRepository;
    private final RotationMemberRepository rotationMemberRepository;
    private final RotationMemberAssignmentRepository rotationMemberAssignmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final BatchRepository batchRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final RotationResolverService rotationResolverService;

    public RotationGroupService(RotationGroupRepository rotationGroupRepository,
                                 RotationSlotRepository rotationSlotRepository,
                                 RotationMemberRepository rotationMemberRepository,
                                 RotationMemberAssignmentRepository rotationMemberAssignmentRepository,
                                 ClassScheduleRepository classScheduleRepository,
                                 BatchRepository batchRepository,
                                 TermInstanceRepository termInstanceRepository,
                                 RotationResolverService rotationResolverService) {
        this.rotationGroupRepository = rotationGroupRepository;
        this.rotationSlotRepository = rotationSlotRepository;
        this.rotationMemberRepository = rotationMemberRepository;
        this.rotationMemberAssignmentRepository = rotationMemberAssignmentRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.batchRepository = batchRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.rotationResolverService = rotationResolverService;
    }

    public RotationEffectiveResponse effective(Long rotationGroupId, Long classScheduleId, java.time.LocalDate date) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        RotationSlot slot = rotationSlotRepository.findByClassScheduleId(classScheduleId)
            .filter(s -> s.getRotationGroup().getId().equals(rotationGroupId))
            .orElseThrow(() -> new LifecycleConflictException(
                "This cell is not part of rotation group " + rotationGroupId + ".",
                "ROTATION_SLOT_NOT_FOUND", "ClassSchedule", classScheduleId, null));
        RotationMemberAssignment assignment = rotationResolverService.resolveEffectiveAssignment(slot, date)
            .orElseThrow(() -> new LifecycleConflictException(
                "No rotation assignment resolved for this date.",
                "ROTATION_EFFECTIVE_NOT_FOUND", "ClassSchedule", classScheduleId, null));
        return new RotationEffectiveResponse(
            slot.getRotationGroup().getId(), cs.getId(), date,
            assignment.getRotationMember().getId(), assignment.getRotationMember().getLabel(),
            assignment.getBatch().getId(), assignment.getBatch().getName());
    }

    public List<RotationGroupResponse> list(Long termInstanceId) {
        return rotationGroupRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .map(g -> toResponse(g, List.of()))
            .toList();
    }

    /** Already-placed LAB/CLINICAL cells at a given day+period with no rotation group yet — the
     *  Rotation Setup UI's first step. Only cells with a batch already set are offered, since a
     *  candidate must have a real subject+venue to compare for the shared-venue check in
     *  {@link #create}. */
    public List<RotationCandidateSlotResponse> candidateSlots(Long termInstanceId, com.cms.model.enums.DayOfWeek dayOfWeek, Long periodId) {
        List<ClassSchedule> drafts = classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(
            termInstanceId, ClassScheduleStatus.DRAFT, dayOfWeek);
        List<ClassSchedule> published = classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(
            termInstanceId, ClassScheduleStatus.PUBLISHED, dayOfWeek);

        List<ClassSchedule> combined = new ArrayList<>(drafts);
        combined.addAll(published);

        return combined.stream()
            .filter(cs -> cs.getSessionType() != ClassSessionType.THEORY)
            .filter(cs -> cs.getPeriod() != null && cs.getPeriod().getId().equals(periodId))
            .filter(cs -> cs.getBatch() != null)
            .filter(cs -> !rotationSlotRepository.existsByClassScheduleId(cs.getId()))
            .map(this::toCandidateResponse)
            .toList();
    }

    @Transactional
    public RotationGroupResponse create(RotationGroupCreateRequest request, String createdBy) {
        TermInstance term = termInstanceRepository.findById(request.termInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + request.termInstanceId()));

        int cycleLength = request.slots().size();
        if (request.members().size() != cycleLength) {
            throw new IllegalArgumentException(
                "A rotation group needs exactly as many physical groups as slots (" + cycleLength
                    + " slots, " + request.members().size() + " members given).");
        }

        Map<Long, ClassSchedule> cellsByScheduleId = new HashMap<>();
        for (RotationSlotInput slotInput : request.slots()) {
            ClassSchedule cs = classScheduleRepository.findById(slotInput.classScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + slotInput.classScheduleId()));
            if (rotationSlotRepository.existsByClassScheduleId(cs.getId())) {
                throw new LifecycleConflictException(
                    "This cell already belongs to a rotation group.",
                    "ROTATION_SLOT_ALREADY_LINKED", "ClassSchedule", cs.getId(), null);
            }
            cellsByScheduleId.put(slotInput.classScheduleId(), cs);
        }
        requireSharedDayAndPeriod(cellsByScheduleId.values());
        requireDistinctOrdered(request.slots().stream().map(RotationSlotInput::slotOrder).toList(), cycleLength, "slotOrder");
        requireDistinctOrdered(request.members().stream().map(RotationMemberInput::memberOrder).toList(), cycleLength, "memberOrder");

        com.cms.model.enums.DayOfWeek sharedDay = cellsByScheduleId.values().iterator().next().getDayOfWeek();
        DayOfWeek anchorDay = request.anchorOccurrenceDate().getDayOfWeek();
        if (!anchorDay.name().equals(sharedDay.name())) {
            throw new IllegalArgumentException(
                "Anchor date must fall on the same day of week as the rotation's slots (" + sharedDay + ").");
        }

        RotationGroup group = new RotationGroup(term, request.label(), cycleLength, request.anchorOccurrenceDate(), createdBy);
        group = rotationGroupRepository.save(group);

        Map<Long, RotationSlot> slotsByScheduleId = new HashMap<>();
        for (RotationSlotInput slotInput : request.slots()) {
            RotationSlot slot = new RotationSlot(group, cellsByScheduleId.get(slotInput.classScheduleId()), slotInput.slotOrder());
            slot = rotationSlotRepository.save(slot);
            slotsByScheduleId.put(slotInput.classScheduleId(), slot);
        }

        List<String> warnings = new ArrayList<>();
        for (RotationMemberInput memberInput : request.members()) {
            if (memberInput.assignments().size() != cycleLength
                || !memberInput.assignments().stream().map(RotationAssignmentInput::classScheduleId)
                    .collect(java.util.stream.Collectors.toSet()).equals(cellsByScheduleId.keySet())) {
                throw new IllegalArgumentException(
                    "Member '" + memberInput.label() + "' must have exactly one batch assignment for each of the " + cycleLength + " slots.");
            }
            RotationMember member = new RotationMember(group, memberInput.memberOrder(), memberInput.label());
            member = rotationMemberRepository.save(member);

            List<Batch> memberBatches = new ArrayList<>();
            for (RotationAssignmentInput assignmentInput : memberInput.assignments()) {
                ClassSchedule cs = cellsByScheduleId.get(assignmentInput.classScheduleId());
                Batch batch = batchRepository.findById(assignmentInput.batchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + assignmentInput.batchId()));
                requireBatchMatchesSlot(cs, batch);
                rotationMemberAssignmentRepository.save(
                    new RotationMemberAssignment(member, slotsByScheduleId.get(assignmentInput.classScheduleId()), batch));
                memberBatches.add(batch);
            }
            warnings.addAll(rosterConsistencyWarnings(memberInput.label(), memberBatches));
        }

        requireConsistentVenuePerSlot(request, cellsByScheduleId);

        for (ClassSchedule cs : cellsByScheduleId.values()) {
            cs.setBatch(null);
            cs.setBatchName(null);
            classScheduleRepository.save(cs);
        }

        return toResponse(group, warnings);
    }

    @Transactional
    public void delete(Long rotationGroupId) {
        RotationGroup group = rotationGroupRepository.findById(rotationGroupId)
            .orElseThrow(() -> new ResourceNotFoundException("Rotation group not found with id: " + rotationGroupId));
        group.setIsActive(false);
        rotationGroupRepository.save(group);
    }

    private void requireSharedDayAndPeriod(java.util.Collection<ClassSchedule> cells) {
        com.cms.model.enums.DayOfWeek day = null;
        Long periodId = null;
        for (ClassSchedule cs : cells) {
            if (day == null) {
                day = cs.getDayOfWeek();
                periodId = cs.getPeriod() != null ? cs.getPeriod().getId() : null;
                continue;
            }
            Long thisPeriodId = cs.getPeriod() != null ? cs.getPeriod().getId() : null;
            if (day != cs.getDayOfWeek() || !java.util.Objects.equals(periodId, thisPeriodId)) {
                throw new IllegalArgumentException(
                    "All slots in a rotation group must share the exact same day and period.");
            }
        }
    }

    private void requireDistinctOrdered(List<Integer> values, int cycleLength, String fieldName) {
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < cycleLength; i++) {
            expected.add(i);
        }
        if (!new HashSet<>(values).equals(expected)) {
            throw new IllegalArgumentException(
                fieldName + " values must be exactly 0.." + (cycleLength - 1) + " with no gaps or duplicates.");
        }
    }

    /** Each assigned batch must belong to the same subject the cell was placed against — only
     *  the roster differs by member, the subject/venue for a slot never does. */
    private void requireBatchMatchesSlot(ClassSchedule cs, Batch batch) {
        if (cs.getCourseOffering() == null || batch.getCourseOffering() == null
            || !batch.getCourseOffering().getId().equals(cs.getCourseOffering().getId())) {
            throw new IllegalArgumentException(
                "Batch '" + batch.getName() + "' does not belong to the same subject as the slot it's assigned to.");
        }
    }

    /** All members' batches at the same slot must share the exact same physical venue — the
     *  room for a slot is fixed, only the occupying batch varies by week. */
    private void requireConsistentVenuePerSlot(RotationGroupCreateRequest request, Map<Long, ClassSchedule> cellsByScheduleId) {
        Map<Long, Long> venueByScheduleId = new HashMap<>();
        for (RotationMemberInput memberInput : request.members()) {
            for (RotationAssignmentInput assignmentInput : memberInput.assignments()) {
                Batch batch = batchRepository.findById(assignmentInput.batchId()).orElseThrow();
                ClassSchedule cs = cellsByScheduleId.get(assignmentInput.classScheduleId());
                Long venueId = venueIdOf(cs, batch);
                Long existing = venueByScheduleId.putIfAbsent(assignmentInput.classScheduleId(), venueId);
                if (existing != null && !existing.equals(venueId)) {
                    throw new LifecycleConflictException(
                        "All batches rotating through one slot must share the exact same venue.",
                        "ROTATION_SLOT_VENUE_MISMATCH", "ClassSchedule", assignmentInput.classScheduleId(), null);
                }
            }
        }
    }

    private Long venueIdOf(ClassSchedule cs, Batch batch) {
        return switch (cs.getSessionType()) {
            case LAB -> batch.getLab() != null ? batch.getLab().getId() : null;
            case CLINICAL -> batch.getClinicalVenue() != null ? batch.getClinicalVenue().getId() : null;
            case THEORY, LIBRARY -> null;
        };
    }

    private List<String> rosterConsistencyWarnings(String memberLabel, List<Batch> batches) {
        if (batches.size() < 2) {
            return List.of();
        }
        Set<Long> firstRoster = studentIds(batches.get(0));
        for (int i = 1; i < batches.size(); i++) {
            if (!studentIds(batches.get(i)).equals(firstRoster)) {
                return List.of("'" + memberLabel + "' has a different roster across its rotating batches — "
                    + "confirm this is intentional, since rotation assumes the same physical students each time.");
            }
        }
        return List.of();
    }

    private Set<Long> studentIds(Batch batch) {
        return batch.getStudents().stream().map(Student::getId).collect(java.util.stream.Collectors.toSet());
    }

    private RotationCandidateSlotResponse toCandidateResponse(ClassSchedule cs) {
        Batch batch = cs.getBatch();
        var period = cs.getPeriod();
        return new RotationCandidateSlotResponse(
            cs.getId(),
            cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null,
            cs.getSubject().getName(),
            batch != null ? batch.getId() : null,
            batch != null ? batch.getName() : null,
            cs.getSessionType(),
            cs.getDayOfWeek(),
            period != null ? period.getId() : null,
            period != null ? period.getName() : null
        );
    }

    private RotationGroupResponse toResponse(RotationGroup group, List<String> warnings) {
        List<RotationSlot> slots = rotationSlotRepository.findByRotationGroupIdOrderBySlotOrderAsc(group.getId());
        List<RotationMember> members = rotationMemberRepository.findByRotationGroupIdOrderByMemberOrderAsc(group.getId());

        List<RotationSlotResponse> slotResponses = slots.stream().map(slot -> {
            ClassSchedule cs = slot.getClassSchedule();
            var period = cs.getPeriod();
            return new RotationSlotResponse(
                slot.getId(), cs.getId(), slot.getSlotOrder(), cs.getSubject().getName(),
                cs.getSessionType(), cs.getDayOfWeek(), period != null ? period.getName() : null);
        }).toList();

        List<RotationMemberResponse> memberResponses = members.stream()
            .map(member -> new RotationMemberResponse(member.getId(), member.getMemberOrder(), member.getLabel(),
                assignmentsForMember(slots, member)))
            .toList();

        return new RotationGroupResponse(
            group.getId(), group.getTermInstance().getId(), group.getLabel(), group.getCycleLength(),
            group.getAnchorOccurrenceDate(), slotResponses, memberResponses, warnings);
    }

    private List<RotationAssignmentResponse> assignmentsForMember(List<RotationSlot> slots, RotationMember member) {
        List<RotationAssignmentResponse> result = new ArrayList<>();
        for (RotationSlot slot : slots) {
            rotationMemberAssignmentRepository.findByRotationMemberIdAndRotationSlotId(member.getId(), slot.getId())
                .ifPresent(a -> result.add(new RotationAssignmentResponse(
                    slot.getId(), slot.getClassSchedule().getId(), a.getBatch().getId(), a.getBatch().getName())));
        }
        return result;
    }
}
