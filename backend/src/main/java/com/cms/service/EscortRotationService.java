package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EscortCandidateDto;
import com.cms.dto.EscortRotationPoolDto;
import com.cms.dto.EscortRotationPoolDto.EscortRotationMemberDto;
import com.cms.dto.EscortRotationPoolRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.EscortRotationAssignment;
import com.cms.model.Faculty;
import com.cms.model.RotationGroup;
import com.cms.model.RotationMember;
import com.cms.repository.BatchRepository;
import com.cms.repository.EscortRotationAssignmentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.RotationGroupRepository;
import com.cms.repository.RotationMemberRepository;

/**
 * Admin-side setup for a clinical Batch's escort-duty rotation pool (OC-175 Piece 3). Reuses
 * {@link RotationGroup}/{@link RotationMember} as-is (see class javadocs — both already generic);
 * "whose turn is it" resolution lives separately in {@link EscortRotationResolverService}.
 */
@Service
@Transactional(readOnly = true)
public class EscortRotationService {

    private final RotationGroupRepository rotationGroupRepository;
    private final RotationMemberRepository rotationMemberRepository;
    private final EscortRotationAssignmentRepository escortAssignmentRepository;
    private final BatchRepository batchRepository;
    private final FacultyRepository facultyRepository;

    public EscortRotationService(RotationGroupRepository rotationGroupRepository,
                                  RotationMemberRepository rotationMemberRepository,
                                  EscortRotationAssignmentRepository escortAssignmentRepository,
                                  BatchRepository batchRepository,
                                  FacultyRepository facultyRepository) {
        this.rotationGroupRepository = rotationGroupRepository;
        this.rotationMemberRepository = rotationMemberRepository;
        this.escortAssignmentRepository = escortAssignmentRepository;
        this.batchRepository = batchRepository;
        this.facultyRepository = facultyRepository;
    }

    /** Eligible candidates for a batch's escort pool: active faculty sharing the batch's offering
     *  subject's speciality -- the same match key {@code FacultyEligibility} uses for teaching
     *  assignment, reused here rather than inventing new matching logic. */
    public List<EscortCandidateDto> eligibleCandidates(Long batchId) {
        Batch batch = getBatchOrThrow(batchId);
        Long specialityId = batch.getCourseOffering().getSubject().getSpeciality() != null
            ? batch.getCourseOffering().getSubject().getSpeciality().getId() : null;
        if (specialityId == null) {
            return List.of();
        }
        return facultyRepository.findBySpecialityIdAndStatus(specialityId, com.cms.model.enums.FacultyStatus.ACTIVE)
            .stream()
            .map(f -> new EscortCandidateDto(f.getId(), f.getFirstName() + " " + f.getLastName()))
            .toList();
    }

    @Transactional
    public EscortRotationPoolDto setupPool(EscortRotationPoolRequest request) {
        Batch batch = getBatchOrThrow(request.batchId());
        if (!escortAssignmentRepository.findByBatchIdOrderByRotationMember_MemberOrderAsc(batch.getId()).isEmpty()) {
            throw new IllegalStateException(
                "Batch " + batch.getId() + " already has an escort rotation pool -- deactivate it before creating another");
        }

        RotationGroup group = new RotationGroup(batch.getTermInstance(), "Escort Rotation - " + batch.getName(),
            request.facultyIds().size(), request.anchorOccurrenceDate(), null);
        rotationGroupRepository.save(group);

        List<Long> facultyIds = request.facultyIds();
        for (int i = 0; i < facultyIds.size(); i++) {
            Long facultyId = facultyIds.get(i);
            Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));
            RotationMember member = new RotationMember(group, i, faculty.getFirstName() + " " + faculty.getLastName());
            rotationMemberRepository.save(member);
            escortAssignmentRepository.save(new EscortRotationAssignment(member, batch, faculty));
        }

        return toDto(batch, group);
    }

    @Transactional
    public void deactivatePool(Long batchId) {
        List<EscortRotationAssignment> assignments =
            escortAssignmentRepository.findByBatchIdOrderByRotationMember_MemberOrderAsc(batchId);
        assignments.stream()
            .map(a -> a.getRotationMember().getRotationGroup())
            .distinct()
            .forEach(g -> {
                g.setIsActive(false);
                rotationGroupRepository.save(g);
            });
    }

    public EscortRotationPoolDto getPool(Long batchId) {
        Batch batch = getBatchOrThrow(batchId);
        List<EscortRotationAssignment> assignments =
            escortAssignmentRepository.findByBatchIdOrderByRotationMember_MemberOrderAsc(batchId);
        if (assignments.isEmpty()) {
            return null;
        }
        return toDto(batch, assignments.get(0).getRotationMember().getRotationGroup());
    }

    private EscortRotationPoolDto toDto(Batch batch, RotationGroup group) {
        List<EscortRotationMemberDto> members = escortAssignmentRepository
            .findByBatchIdOrderByRotationMember_MemberOrderAsc(batch.getId()).stream()
            .map(a -> new EscortRotationMemberDto(
                a.getRotationMember().getMemberOrder(), a.getFaculty().getId(),
                a.getFaculty().getFirstName() + " " + a.getFaculty().getLastName()))
            .toList();
        return new EscortRotationPoolDto(
            batch.getId(), batch.getName(), group.getId(), group.getCycleLength(),
            group.getAnchorOccurrenceDate(), members
        );
    }

    private Batch getBatchOrThrow(Long batchId) {
        return batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
    }
}
