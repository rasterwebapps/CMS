package com.cms.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearRequest;
import com.cms.dto.AcademicYearResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.FeeStructureRepository;

@Service
@Transactional(readOnly = true)
public class AcademicYearService {

    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("^\\d{4}-\\d{4}$");

    private final AcademicYearRepository academicYearRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final TermInstanceService termInstanceService;

    public AcademicYearService(AcademicYearRepository academicYearRepository,
                               FeeStructureRepository feeStructureRepository,
                               TermInstanceService termInstanceService) {
        this.academicYearRepository = academicYearRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.termInstanceService = termInstanceService;
    }

    @Transactional
    public AcademicYearResponse create(AcademicYearRequest request) {
        validateDateRange(request);
        validateNameFormat(request.name());

        Boolean isCurrent = request.isCurrent() != null ? request.isCurrent() : false;

        if (Boolean.TRUE.equals(isCurrent)) {
            academicYearRepository.clearCurrentAcademicYear();
        }

        AcademicYear academicYear = new AcademicYear(
            request.name(),
            request.startDate(),
            request.endDate(),
            isCurrent
        );
        AcademicYear saved = academicYearRepository.save(academicYear);
        termInstanceService.createTermInstancesForAcademicYear(saved);
        return toResponse(saved);
    }

    public List<AcademicYearResponse> findAll() {
        return academicYearRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public AcademicYearResponse findById(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
        return toResponse(academicYear);
    }

    public AcademicYearResponse findCurrent() {
        AcademicYear academicYear = academicYearRepository.findByIsCurrentTrue()
            .orElseThrow(() -> new ResourceNotFoundException("No current academic year found"));
        return toResponse(academicYear);
    }

    @Transactional
    public AcademicYearResponse update(Long id, AcademicYearRequest request) {
        AcademicYear academicYear = academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));

        validateDateRange(request);
        validateNameFormat(request.name());

        if (academicYearRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException(
                "An academic year with the name '" + request.name() + "' already exists");
        }

        Boolean isCurrent = request.isCurrent() != null ? request.isCurrent() : false;

        if (Boolean.TRUE.equals(isCurrent) && !Boolean.TRUE.equals(academicYear.getIsCurrent())) {
            academicYearRepository.clearCurrentAcademicYear();
        }

        academicYear.setName(request.name());
        academicYear.setStartDate(request.startDate());
        academicYear.setEndDate(request.endDate());
        academicYear.setIsCurrent(isCurrent);

        AcademicYear updated = academicYearRepository.save(academicYear);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!academicYearRepository.existsById(id)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + id);
        }
        if (feeStructureRepository.existsByAcademicYearId(id)) {
            throw new IllegalStateException(
                "Cannot delete academic year because fee structures are associated with it.");
        }
        academicYearRepository.deleteById(id);
    }

    private void validateDateRange(AcademicYearRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    private void validateNameFormat(String name) {
        if (name == null) return;
        if (!YEAR_RANGE_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                "Academic year name must be in YYYY-YYYY format (e.g. 2026-2027)");
        }
        String[] parts = name.split("-");
        int startYear = Integer.parseInt(parts[0]);
        int endYear = Integer.parseInt(parts[1]);
        if (endYear != startYear + 1) {
            throw new IllegalArgumentException(
                "Academic year end year must be exactly one year after start year (e.g. 2026-2027)");
        }
    }

    private AcademicYearResponse toResponse(AcademicYear academicYear) {
        return new AcademicYearResponse(
            academicYear.getId(),
            academicYear.getName(),
            academicYear.getStartDate(),
            academicYear.getEndDate(),
            academicYear.getIsCurrent(),
            academicYear.getCreatedAt(),
            academicYear.getUpdatedAt()
        );
    }
}
