package com.cms.service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ClinicalShiftBatchLinkDto;
import com.cms.dto.ClinicalShiftGroupDto;
import com.cms.dto.ClinicalShiftGroupRequest;
import com.cms.dto.ClinicalShiftTheoryBlockDto;
import com.cms.dto.ClinicalShiftTheoryBlockRequest;
import com.cms.dto.ClinicalShiftWindow;
import com.cms.dto.CourseOfferingDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.Classroom;
import com.cms.model.ClinicalShiftGroup;
import com.cms.model.ClinicalShiftTheoryBlock;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.Subject;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalShiftGroupRepository;
import com.cms.repository.ClinicalShiftTheoryBlockRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.SubjectRepository;

/**
 * CRUD + read model for {@link ClinicalShiftGroup} (OC-175 Piece 2): a recurring off-campus
 * clinical shift window that several {@link Batch} rows (each its own venue) can link to, plus
 * the group's shared, reconvened theory block(s). Occurrence generation (the attendance-capable
 * per-date materialization) lives in {@link ClinicalShiftOccurrenceService}, kept separate since
 * it's a different concern (date-driven, not CRUD-driven).
 */
@Service
@Transactional(readOnly = true)
public class ClinicalShiftGroupService {

    private final ClinicalShiftGroupRepository shiftGroupRepository;
    private final ClinicalShiftTheoryBlockRepository theoryBlockRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final CohortRoomAllocationRepository cohortRoomAllocationRepository;
    private final BatchRepository batchRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final CourseOfferingService courseOfferingService;

    public ClinicalShiftGroupService(ClinicalShiftGroupRepository shiftGroupRepository,
                                      ClinicalShiftTheoryBlockRepository theoryBlockRepository,
                                      CourseOfferingRepository courseOfferingRepository,
                                      CohortSectionRepository cohortSectionRepository,
                                      CohortRoomAllocationRepository cohortRoomAllocationRepository,
                                      BatchRepository batchRepository,
                                      SubjectRepository subjectRepository,
                                      ClassroomRepository classroomRepository,
                                      CourseOfferingService courseOfferingService) {
        this.shiftGroupRepository = shiftGroupRepository;
        this.theoryBlockRepository = theoryBlockRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.cohortRoomAllocationRepository = cohortRoomAllocationRepository;
        this.batchRepository = batchRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.courseOfferingService = courseOfferingService;
    }

    @Transactional
    public ClinicalShiftGroupDto createGroup(ClinicalShiftGroupRequest request) {
        CourseOffering offering = courseOfferingRepository.findById(request.courseOfferingId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Course offering not found with id: " + request.courseOfferingId()));
        if (offering.getClinicalShiftDurationMinutes() == null) {
            throw new IllegalStateException(
                "Course offering " + offering.getId() + " has no clinical shift duration configured");
        }

        ClinicalShiftGroup group = new ClinicalShiftGroup();
        group.setCourseOffering(offering);
        group.setTermInstance(offering.getTermInstance());
        group.setCohortSection(resolveCohortSection(request.cohortSectionId()));
        group.setLabel(request.label());
        group.setDayOfWeek(request.dayOfWeek());
        group.setClinicalStartTime(request.clinicalStartTime());

        return toDto(shiftGroupRepository.save(group));
    }

    @Transactional
    public ClinicalShiftGroupDto updateGroup(Long id, ClinicalShiftGroupRequest request) {
        ClinicalShiftGroup group = getOrThrow(id);
        group.setCohortSection(resolveCohortSection(request.cohortSectionId()));
        group.setLabel(request.label());
        group.setDayOfWeek(request.dayOfWeek());
        group.setClinicalStartTime(request.clinicalStartTime());
        return toDto(shiftGroupRepository.save(group));
    }

    @Transactional
    public void deactivateGroup(Long id) {
        ClinicalShiftGroup group = getOrThrow(id);
        group.setIsActive(false);
        shiftGroupRepository.save(group);
    }

    public List<ClinicalShiftGroupDto> getGroupsForOffering(Long courseOfferingId) {
        return shiftGroupRepository.findByCourseOfferingId(courseOfferingId).stream()
            .map(this::toDto)
            .toList();
    }

    public ClinicalShiftGroupDto getGroup(Long id) {
        return toDto(getOrThrow(id));
    }

    /** Every active {@link ClinicalShiftGroup} bound to cohort X this term, across all days.
     *  Resolution rule (OC-187): the group's {@code courseOffering} must be one of cohort X's real
     *  offerings this term; when the group also names a specific {@link
     *  ClinicalShiftGroup#cohortSection}, that section must be one of cohort X's active (committed)
     *  sections too. No {@link Batch} link is required any more — grid-blocking used to require an
     *  active Batch to be manually linked to the group first, which meant a shift did nothing until
     *  Capacity Auto-Plan had committed room allocation AND an admin remembered to link it. Linking
     *  a Batch is now purely for Escort Rotation bookkeeping (per-batch bus rotation), not an
     *  eligibility gate — the shift blocks the grid as soon as the group itself exists. */
    public List<ClinicalShiftWindow> resolveActiveWindowsForCohort(Long cohortId, Long termInstanceId) {
        Set<Long> offeringIds = courseOfferingService.getOfferingsByTermInstanceAndCohort(termInstanceId, cohortId)
            .stream().map(CourseOfferingDto::id).collect(Collectors.toSet());
        if (offeringIds.isEmpty()) {
            return List.of();
        }
        Set<Long> activeSectionIds = cohortRoomAllocationRepository
            .findByCohortIdAndTermInstanceIdAndStatus(cohortId, termInstanceId, CohortRoomAllocationStatus.COMMITTED)
            .map(a -> cohortSectionRepository.findByCohortRoomAllocationIdAndIsActiveTrue(a.getId()))
            .orElse(List.of())
            .stream().map(CohortSection::getId).collect(Collectors.toSet());

        return shiftGroupRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .filter(g -> offeringIds.contains(g.getCourseOffering().getId()))
            .filter(g -> g.getCohortSection() == null || activeSectionIds.contains(g.getCohortSection().getId()))
            .map(ClinicalShiftWindow::from)
            .toList();
    }

    @Transactional
    public void linkBatch(Long shiftGroupId, Long batchId) {
        ClinicalShiftGroup group = getOrThrow(shiftGroupId);
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
        if (!batch.getCourseOffering().getId().equals(group.getCourseOffering().getId())) {
            throw new IllegalArgumentException(
                "Batch " + batchId + " does not belong to this shift group's course offering");
        }
        if (batch.getClinicalVenue() == null) {
            throw new IllegalStateException(
                "Batch " + batchId + " has no clinical venue assigned yet -- shift groups are for off-campus "
                    + "clinical batches only, assign a Clinical venue before linking it to a shift");
        }
        batch.setClinicalShiftGroup(group);
        batchRepository.save(batch);
    }

    @Transactional
    public void unlinkBatch(Long shiftGroupId, Long batchId) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
        if (batch.getClinicalShiftGroup() != null && batch.getClinicalShiftGroup().getId().equals(shiftGroupId)) {
            batch.setClinicalShiftGroup(null);
            batchRepository.save(batch);
        }
    }

    @Transactional
    public List<ClinicalShiftTheoryBlockDto> replaceTheoryBlocks(Long shiftGroupId,
                                                                   List<ClinicalShiftTheoryBlockRequest> requests) {
        ClinicalShiftGroup group = getOrThrow(shiftGroupId);
        theoryBlockRepository.deleteByShiftGroupId(shiftGroupId);

        List<ClinicalShiftTheoryBlock> blocks = requests.stream()
            .map(r -> toEntity(group, r))
            .sorted(Comparator.comparing(ClinicalShiftTheoryBlock::getSequenceOrder))
            .toList();
        theoryBlockRepository.saveAll(blocks);
        return blocks.stream().map(this::toDto).toList();
    }

    private ClinicalShiftTheoryBlock toEntity(ClinicalShiftGroup group, ClinicalShiftTheoryBlockRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("Theory block end time must be after start time");
        }
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));
        Classroom classroom = request.classroomId() == null ? null
            : classroomRepository.findById(request.classroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + request.classroomId()));

        ClinicalShiftTheoryBlock block = new ClinicalShiftTheoryBlock();
        block.setShiftGroup(group);
        block.setSequenceOrder(request.sequenceOrder());
        block.setStartTime(request.startTime());
        block.setEndTime(request.endTime());
        block.setSubject(subject);
        block.setClassroom(classroom);
        return block;
    }

    private CohortSection resolveCohortSection(Long cohortSectionId) {
        if (cohortSectionId == null) {
            return null;
        }
        return cohortSectionRepository.findById(cohortSectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort section not found with id: " + cohortSectionId));
    }

    private ClinicalShiftGroup getOrThrow(Long id) {
        return shiftGroupRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Clinical shift group not found with id: " + id));
    }

    private ClinicalShiftGroupDto toDto(ClinicalShiftGroup group) {
        CourseOffering offering = group.getCourseOffering();
        ClinicalShiftWindow window = ClinicalShiftWindow.from(group);
        LocalTime clinicalEnd = window.clinicalEnd();
        LocalTime busDepart = window.busDepart();
        LocalTime busReturn = window.busReturn();

        List<ClinicalShiftBatchLinkDto> batches = batchRepository.findByClinicalShiftGroupId(group.getId()).stream()
            .map(b -> new ClinicalShiftBatchLinkDto(
                b.getId(),
                b.getName(),
                b.getCapacity(),
                b.getLab() != null ? b.getLab().getName()
                    : b.getClinicalVenue() != null ? b.getClinicalVenue().getName() : null))
            .toList();

        List<ClinicalShiftTheoryBlockDto> blocks = theoryBlockRepository
            .findByShiftGroupIdOrderBySequenceOrderAsc(group.getId()).stream()
            .map(this::toDto)
            .toList();

        return new ClinicalShiftGroupDto(
            group.getId(),
            offering.getId(),
            offering.getSubject().getName(),
            group.getCohortSection() != null ? group.getCohortSection().getId() : null,
            group.getCohortSection() != null ? group.getCohortSection().getSectionLabel() : null,
            group.getTermInstance().getId(),
            group.getLabel(),
            group.getDayOfWeek(),
            group.getClinicalStartTime(),
            clinicalEnd,
            busDepart,
            busReturn,
            group.getIsActive(),
            batches,
            blocks,
            group.getCreatedAt(),
            group.getUpdatedAt()
        );
    }

    private ClinicalShiftTheoryBlockDto toDto(ClinicalShiftTheoryBlock block) {
        return new ClinicalShiftTheoryBlockDto(
            block.getId(),
            block.getSequenceOrder(),
            block.getStartTime(),
            block.getEndTime(),
            block.getSubject().getId(),
            block.getSubject().getName(),
            block.getClassroom() != null ? block.getClassroom().getId() : null,
            block.getClassroom() != null ? block.getClassroom().getName() : null
        );
    }
}
