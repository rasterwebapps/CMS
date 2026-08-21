package com.cms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CurriculumFullViewDto;
import com.cms.dto.CurriculumSemesterCourseDto;
import com.cms.dto.CurriculumSemesterCourseRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CurriculumElectiveGroup;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Subject;
import com.cms.model.enums.AssessmentPattern;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CurriculumElectiveGroupRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class CurriculumSemesterCourseService {

    private final CurriculumSemesterCourseRepository courseRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final SubjectRepository subjectRepository;
    private final CurriculumElectiveGroupRepository electiveGroupRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public CurriculumSemesterCourseService(CurriculumSemesterCourseRepository courseRepository,
                                            CurriculumVersionRepository curriculumVersionRepository,
                                            SubjectRepository subjectRepository,
                                            CurriculumElectiveGroupRepository electiveGroupRepository,
                                            CourseOfferingRepository courseOfferingRepository) {
        this.courseRepository = courseRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.subjectRepository = subjectRepository;
        this.electiveGroupRepository = electiveGroupRepository;
        this.courseOfferingRepository = courseOfferingRepository;
    }

    @Transactional
    public CurriculumSemesterCourseDto addCourseToSemester(CurriculumSemesterCourseRequest request) {
        CurriculumVersion cv = curriculumVersionRepository.findById(request.curriculumVersionId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + request.curriculumVersionId()));

        int totalTerms = cv.getProgram().getTotalTerms();
        if (request.termNumber() < 1 || request.termNumber() > totalTerms) {
            AssessmentPattern pattern = cv.getProgram().getAssessmentPattern();
            String termLabel = pattern == AssessmentPattern.YEARLY ? "Year" : "Term";
            throw new IllegalArgumentException(
                termLabel + " number must be between 1 and " + totalTerms +
                " for this program (duration " + cv.getProgram().getDurationYears() + " years)");
        }

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Subject not found with id: " + request.subjectId()));

        CurriculumSemesterCourse entry = new CurriculumSemesterCourse(
            cv, request.termNumber(), subject, request.sortOrder());
        applyDetails(entry, cv, request);
        return toDto(courseRepository.save(entry), false);
    }

    @Transactional
    public CurriculumSemesterCourseDto updateCourseDetails(Long id, CurriculumSemesterCourseRequest request) {
        CurriculumSemesterCourse entry = courseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum semester course not found with id: " + id));

        if (courseOfferingRepository.existsByCurriculumSemesterCourseId(id)) {
            throw new IllegalStateException(
                "Cannot edit a subject mapping that already has course offerings against it");
        }

        applyDetails(entry, entry.getCurriculumVersion(), request);
        return toDto(courseRepository.save(entry), false);
    }

    private void applyDetails(CurriculumSemesterCourse entry, CurriculumVersion cv,
                               CurriculumSemesterCourseRequest request) {
        // Hard block: a subject can't be declared as needing Lab/Clinical hours in a curriculum
        // unless it already has at least one eligible Lab/Clinical Venue configured (Subject master
        // > Eligible Labs/Clinical Venues) -- otherwise the auto-suggest algorithm and every manual
        // picker would have nothing appropriate to offer once this curriculum entry starts producing
        // real CourseOfferings. Unlike the soft-prefer-with-fallback behavior those pickers use once
        // a subject already has SOME eligible venue configured, this is the earlier gate that forces
        // the mapping to exist in the first place.
        if (request.labHours() != null && request.labHours() > 0 && entry.getSubject().getEligibleLabs().isEmpty()) {
            throw new IllegalArgumentException(
                "Subject '" + entry.getSubject().getName() + "' needs " + request.labHours() + " lab hour(s) but has "
                    + "no eligible Lab configured — link at least one Lab to this subject first (Subjects > Eligible Labs).");
        }
        if (request.clinicalHours() != null && request.clinicalHours() > 0 && entry.getSubject().getEligibleClinicalVenues().isEmpty()) {
            throw new IllegalArgumentException(
                "Subject '" + entry.getSubject().getName() + "' needs " + request.clinicalHours() + " clinical hour(s) "
                    + "but has no eligible Clinical Venue configured — link at least one Clinical Venue to this "
                    + "subject first (Subjects > Eligible Clinical Venues).");
        }

        entry.setTheoryHours(request.theoryHours());
        entry.setLabHours(request.labHours());
        entry.setClinicalHours(request.clinicalHours());
        entry.setSubjectType(request.subjectType());
        entry.setIsElective(request.isElective());

        if (Boolean.TRUE.equals(request.isElective())) {
            if (request.electiveGroupId() == null) {
                throw new IllegalArgumentException(
                    "An elective group is required when a subject is marked as elective");
            }
            CurriculumElectiveGroup group = electiveGroupRepository.findById(request.electiveGroupId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Curriculum elective group not found with id: " + request.electiveGroupId()));
            if (!group.getCurriculumVersion().getId().equals(cv.getId())
                    || !group.getTermNumber().equals(entry.getSemesterNumber())) {
                throw new IllegalArgumentException(
                    "Elective group must belong to the same curriculum version and term as the subject");
            }
            entry.setElectiveGroup(group);
        } else {
            entry.setElectiveGroup(null);
        }
    }

    @Transactional
    public void removeCourseFromSemester(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "Curriculum semester course not found with id: " + id);
        }
        if (courseOfferingRepository.existsByCurriculumSemesterCourseId(id)) {
            throw new IllegalStateException(
                "Cannot remove a subject mapping that already has course offerings against it");
        }
        courseRepository.deleteById(id);
    }

    public List<CurriculumSemesterCourseDto> getCoursesBySemester(Long curriculumVersionId,
                                                                    Integer termNumber) {
        if (!curriculumVersionRepository.existsById(curriculumVersionId)) {
            throw new ResourceNotFoundException(
                "Curriculum version not found with id: " + curriculumVersionId);
        }
        Set<Long> lockedIds = courseOfferingRepository.findLockedCurriculumSemesterCourseIds(curriculumVersionId);
        return courseRepository.findByCurriculumVersionIdAndSemesterNumber(curriculumVersionId, termNumber)
            .stream()
            .map(c -> toDto(c, lockedIds.contains(c.getId())))
            .toList();
    }

    public List<CurriculumSemesterCourseDto> getAllByCurriculumVersion(Long curriculumVersionId) {
        if (!curriculumVersionRepository.existsById(curriculumVersionId)) {
            throw new ResourceNotFoundException(
                "Curriculum version not found with id: " + curriculumVersionId);
        }
        Set<Long> lockedIds = courseOfferingRepository.findLockedCurriculumSemesterCourseIds(curriculumVersionId);
        return courseRepository.findByCurriculumVersionId(curriculumVersionId)
            .stream()
            .map(c -> toDto(c, lockedIds.contains(c.getId())))
            .toList();
    }

    public CurriculumFullViewDto getFullCurriculum(Long curriculumVersionId) {
        CurriculumVersion cv = curriculumVersionRepository.findById(curriculumVersionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + curriculumVersionId));

        List<CurriculumSemesterCourse> allCourses = courseRepository.findByCurriculumVersionId(curriculumVersionId);
        Set<Long> lockedIds = courseOfferingRepository.findLockedCurriculumSemesterCourseIds(curriculumVersionId);

        Map<Integer, List<CurriculumSemesterCourseDto>> grouped = new LinkedHashMap<>();
        int totalTerms = cv.getProgram().getTotalTerms();
        for (int i = 1; i <= totalTerms; i++) {
            grouped.put(i, new ArrayList<>());
        }
        for (CurriculumSemesterCourse c : allCourses) {
            grouped.computeIfAbsent(c.getSemesterNumber(), k -> new ArrayList<>())
                .add(toDto(c, lockedIds.contains(c.getId())));
        }

        List<CurriculumFullViewDto.TermGroup> semesterGroups = grouped.entrySet().stream()
            .sorted(Comparator.comparingInt(Map.Entry::getKey))
            .map(e -> new CurriculumFullViewDto.TermGroup(e.getKey(), e.getValue()))
            .toList();

        return new CurriculumFullViewDto(
            cv.getId(),
            cv.getVersionName(),
            cv.getProgram().getId(),
            cv.getProgram().getName(),
            cv.getProgram().getAssessmentPattern(),
            totalTerms,
            semesterGroups
        );
    }

    private CurriculumSemesterCourseDto toDto(CurriculumSemesterCourse c, boolean isLocked) {
        return new CurriculumSemesterCourseDto(
            c.getId(),
            c.getCurriculumVersion().getId(),
            c.getCurriculumVersion().getVersionName(),
            c.getSemesterNumber(),
            c.getSubject().getId(),
            c.getSubject().getName(),
            c.getSubject().getCode(),
            c.getSortOrder(),
            c.getTheoryHours(),
            c.getLabHours(),
            c.getClinicalHours(),
            c.getSubjectType(),
            c.getIsElective(),
            c.getElectiveGroup() != null ? c.getElectiveGroup().getId() : null,
            c.getElectiveGroup() != null ? c.getElectiveGroup().getGroupName() : null,
            isLocked,
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
