package com.cms.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.TermInstanceDto;
import com.cms.dto.TermInstanceUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.TermInstance;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class TermInstanceService {

    private final TermInstanceRepository termInstanceRepository;
    private final AcademicYearRepository academicYearRepository;

    // Field injection with @Lazy breaks the circular dependency:
    // TermInstanceService -> StudentTermEnrollmentService -> TermInstanceRepository
    @Autowired
    @Lazy
    private StudentTermEnrollmentService studentTermEnrollmentService;

    public TermInstanceService(TermInstanceRepository termInstanceRepository,
                                AcademicYearRepository academicYearRepository) {
        this.termInstanceRepository = termInstanceRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public void createTermInstancesForAcademicYear(AcademicYear academicYear) {
        int startYear = academicYear.getStartDate().getYear();

        TermInstance odd = new TermInstance(
            academicYear,
            TermType.ODD,
            LocalDate.of(startYear, 6, 1),
            LocalDate.of(startYear, 11, 30),
            TermInstanceStatus.PLANNED
        );

        TermInstance even = new TermInstance(
            academicYear,
            TermType.EVEN,
            LocalDate.of(startYear, 12, 1),
            LocalDate.of(startYear + 1, 5, 31),
            TermInstanceStatus.PLANNED
        );

        termInstanceRepository.save(odd);
        termInstanceRepository.save(even);
    }

    public List<TermInstanceDto> getTermInstancesByAcademicYear(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return termInstanceRepository.findByAcademicYearId(academicYearId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public TermInstanceDto getById(Long id) {
        TermInstance instance = termInstanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + id));
        return toDto(instance);
    }

    @Transactional
    public TermInstanceDto updateTermInstance(Long id, TermInstanceUpdateRequest request) {
        TermInstance instance = termInstanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + id));

        if (request.startDate() != null) {
            instance.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            instance.setEndDate(request.endDate());
        }
        if (request.status() != null) {
            validateStatusTransition(instance.getStatus(), request.status());
            instance.setStatus(request.status());
        }

        TermInstance saved = termInstanceRepository.save(instance);
        if (request.status() != null && request.status() == TermInstanceStatus.OPEN) {
            studentTermEnrollmentService.generateEnrollmentsForTermInstance(id);
        }
        return toDto(saved);
    }

    private void validateStatusTransition(TermInstanceStatus current, TermInstanceStatus next) {
        boolean valid = switch (current) {
            case PLANNED -> next == TermInstanceStatus.OPEN;
            case OPEN -> next == TermInstanceStatus.LOCKED;
            case LOCKED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                "Invalid status transition from " + current + " to " + next +
                ". Allowed: PLANNED → OPEN → LOCKED (no backward transitions)");
        }
    }

    private TermInstanceDto toDto(TermInstance ti) {
        return new TermInstanceDto(
            ti.getId(),
            ti.getAcademicYear().getId(),
            ti.getAcademicYear().getName(),
            ti.getTermType(),
            ti.getStartDate(),
            ti.getEndDate(),
            ti.getStatus(),
            ti.getCreatedAt(),
            ti.getUpdatedAt()
        );
    }
}
