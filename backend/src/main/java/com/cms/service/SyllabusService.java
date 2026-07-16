package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SyllabusActivationRequest;
import com.cms.dto.SyllabusRequest;
import com.cms.dto.SyllabusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Syllabus;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.SyllabusRepository;

@Service
@Transactional(readOnly = true)
public class SyllabusService {

    private final SyllabusRepository syllabusRepository;
    private final CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;

    public SyllabusService(SyllabusRepository syllabusRepository,
                            CurriculumSemesterCourseRepository curriculumSemesterCourseRepository) {
        this.syllabusRepository = syllabusRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
    }

    @Transactional
    public SyllabusResponse create(SyllabusRequest request) {
        CurriculumSemesterCourse mapping = curriculumSemesterCourseRepository.findById(request.curriculumTermCourseId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum mapping not found with id: " + request.curriculumTermCourseId()));

        Boolean isActive = request.isActive() != null ? request.isActive() : false;
        if (Boolean.TRUE.equals(isActive)) {
            syllabusRepository.clearActiveForMapping(mapping.getId());
        }

        int nextVersion = syllabusRepository.findMaxVersion(mapping.getId()) + 1;

        Syllabus syllabus = new Syllabus(
            mapping,
            nextVersion,
            request.objectives(),
            request.content(),
            request.textBooks(),
            request.referenceBooks(),
            request.courseOutcomes(),
            isActive
        );

        Syllabus saved = syllabusRepository.save(syllabus);
        return toResponse(saved);
    }

    public List<SyllabusResponse> findAll() {
        return syllabusRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public SyllabusResponse findById(Long id) {
        Syllabus syllabus = syllabusRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found with id: " + id));
        return toResponse(syllabus);
    }

    public List<SyllabusResponse> findBySubjectId(Long subjectId) {
        return syllabusRepository.findByCurriculumSemesterCourse_Subject_Id(subjectId).stream()
            .map(this::toResponse)
            .toList();
    }

    public SyllabusResponse findActiveBySubjectId(Long subjectId) {
        return syllabusRepository.findByCurriculumSemesterCourse_Subject_IdAndIsActiveTrue(subjectId)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No active syllabus found for subject id: " + subjectId));
    }

    /** A syllabus version is immutable once created — activating/deactivating is the only
     *  permitted change. Activating clears every other active version for the same mapping
     *  (mirrors AcademicYearService's "only one current at a time" pattern); deactivating has
     *  no such guard — a subject can legitimately have zero active syllabus versions between
     *  revisions. Content changes go through create() as a new version instead. */
    @Transactional
    public SyllabusResponse setActive(Long id, SyllabusActivationRequest request) {
        Syllabus syllabus = syllabusRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found with id: " + id));

        if (Boolean.TRUE.equals(request.isActive())) {
            syllabusRepository.clearActiveForMapping(syllabus.getCurriculumSemesterCourse().getId());
        }
        syllabus.setIsActive(request.isActive());

        Syllabus updated = syllabusRepository.save(syllabus);
        return toResponse(updated);
    }

    private SyllabusResponse toResponse(Syllabus syllabus) {
        CurriculumSemesterCourse mapping = syllabus.getCurriculumSemesterCourse();
        return new SyllabusResponse(
            syllabus.getId(),
            mapping.getId(),
            mapping.getCurriculumVersion().getId(),
            mapping.getCurriculumVersion().getVersionName(),
            mapping.getSemesterNumber(),
            mapping.getSubject().getId(),
            mapping.getSubject().getName(),
            mapping.getSubject().getCode(),
            syllabus.getVersion(),
            mapping.getTheoryHours(),
            mapping.getLabHours(),
            mapping.getClinicalHours(),
            syllabus.getObjectives(),
            syllabus.getContent(),
            syllabus.getTextBooks(),
            syllabus.getReferenceBooks(),
            syllabus.getCourseOutcomes(),
            syllabus.getIsActive(),
            syllabus.getCreatedAt(),
            syllabus.getUpdatedAt()
        );
    }
}
