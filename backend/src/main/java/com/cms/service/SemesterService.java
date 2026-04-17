package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearResponse;
import com.cms.dto.SemesterRequest;
import com.cms.dto.SemesterResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Semester;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.SemesterRepository;

@Service
@Transactional(readOnly = true)
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;

    public SemesterService(SemesterRepository semesterRepository,
                           AcademicYearRepository academicYearRepository) {
        this.semesterRepository = semesterRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public SemesterResponse create(SemesterRequest request) {
        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));

        validateDateRange(request);
        validateDatesWithinAcademicYear(request, academicYear);

        Semester semester = new Semester(
            request.name(),
            academicYear,
            request.startDate(),
            request.endDate(),
            request.semesterNumber()
        );
        Semester saved = semesterRepository.save(semester);
        return toResponse(saved);
    }

    public List<SemesterResponse> findAll() {
        return semesterRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public SemesterResponse findById(Long id) {
        Semester semester = semesterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + id));
        return toResponse(semester);
    }

    public List<SemesterResponse> findByAcademicYearId(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return semesterRepository.findByAcademicYearIdOrderBySemesterNumber(academicYearId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SemesterResponse update(Long id, SemesterRequest request) {
        Semester semester = semesterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + id));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));

        validateDateRange(request);
        validateDatesWithinAcademicYear(request, academicYear);

        if (semesterRepository.existsByNameAndAcademicYearIdAndIdNot(
                request.name(), request.academicYearId(), id)) {
            throw new IllegalArgumentException(
                "A semester with the name '" + request.name()
                + "' already exists in this academic year");
        }

        semester.setName(request.name());
        semester.setAcademicYear(academicYear);
        semester.setStartDate(request.startDate());
        semester.setEndDate(request.endDate());
        semester.setSemesterNumber(request.semesterNumber());

        Semester updated = semesterRepository.save(semester);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!semesterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Semester not found with id: " + id);
        }
        semesterRepository.deleteById(id);
    }

    private void validateDateRange(SemesterRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    private void validateDatesWithinAcademicYear(SemesterRequest request, AcademicYear academicYear) {
        if (request.startDate().isBefore(academicYear.getStartDate())) {
            throw new IllegalArgumentException(
                "Semester start date must not be before academic year start date");
        }
        if (request.endDate().isAfter(academicYear.getEndDate())) {
            throw new IllegalArgumentException(
                "Semester end date must not be after academic year end date");
        }
    }

    private SemesterResponse toResponse(Semester semester) {
        AcademicYear academicYear = semester.getAcademicYear();
        AcademicYearResponse academicYearResponse = new AcademicYearResponse(
            academicYear.getId(),
            academicYear.getName(),
            academicYear.getStartDate(),
            academicYear.getEndDate(),
            academicYear.getIsCurrent(),
            academicYear.getCreatedAt(),
            academicYear.getUpdatedAt()
        );

        return new SemesterResponse(
            semester.getId(),
            semester.getName(),
            academicYearResponse,
            semester.getStartDate(),
            semester.getEndDate(),
            semester.getSemesterNumber(),
            semester.getCreatedAt(),
            semester.getUpdatedAt()
        );
    }
}
