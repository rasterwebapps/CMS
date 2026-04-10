package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearRequest;
import com.cms.dto.AcademicYearResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.repository.AcademicYearRepository;

@Service
@Transactional(readOnly = true)
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;

    public AcademicYearService(AcademicYearRepository academicYearRepository) {
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public AcademicYearResponse create(AcademicYearRequest request) {
        validateDateRange(request);

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
        academicYearRepository.deleteById(id);
    }

    private void validateDateRange(AcademicYearRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
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
