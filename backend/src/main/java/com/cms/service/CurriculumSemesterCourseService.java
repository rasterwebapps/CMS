package com.cms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CurriculumFullViewDto;
import com.cms.dto.CurriculumSemesterCourseDto;
import com.cms.dto.CurriculumSemesterCourseRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.CurriculumElectiveGroup;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Subject;
import com.cms.model.enums.AssessmentPattern;
import com.cms.repository.CourseRepository;
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
    private final CourseRepository courseMasterRepository;

    public CurriculumSemesterCourseService(CurriculumSemesterCourseRepository courseRepository,
                                            CurriculumVersionRepository curriculumVersionRepository,
                                            SubjectRepository subjectRepository,
                                            CurriculumElectiveGroupRepository electiveGroupRepository,
                                            CourseRepository courseMasterRepository) {
        this.courseRepository = courseRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.subjectRepository = subjectRepository;
        this.electiveGroupRepository = electiveGroupRepository;
        this.courseMasterRepository = courseMasterRepository;
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
        return toDto(courseRepository.save(entry));
    }

    @Transactional
    public CurriculumSemesterCourseDto updateCourseDetails(Long id, CurriculumSemesterCourseRequest request) {
        CurriculumSemesterCourse entry = courseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum semester course not found with id: " + id));

        applyDetails(entry, entry.getCurriculumVersion(), request);
        return toDto(courseRepository.save(entry));
    }

    private void applyDetails(CurriculumSemesterCourse entry, CurriculumVersion cv,
                               CurriculumSemesterCourseRequest request) {
        entry.setTheoryHours(request.theoryHours());
        entry.setLabHours(request.labHours());
        entry.setClinicalHours(request.clinicalHours());
        entry.setSubjectType(request.subjectType());
        entry.setIsElective(request.isElective());

        if (request.courseId() != null) {
            Course restrictedCourse = courseMasterRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Course not found with id: " + request.courseId()));
            if (!restrictedCourse.getProgram().getId().equals(cv.getProgram().getId())) {
                throw new IllegalArgumentException(
                    "Course must belong to the same program as the curriculum version");
            }
            entry.setCourse(restrictedCourse);
        } else {
            entry.setCourse(null);
        }

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
        courseRepository.deleteById(id);
    }

    public List<CurriculumSemesterCourseDto> getCoursesBySemester(Long curriculumVersionId,
                                                                    Integer termNumber) {
        if (!curriculumVersionRepository.existsById(curriculumVersionId)) {
            throw new ResourceNotFoundException(
                "Curriculum version not found with id: " + curriculumVersionId);
        }
        return courseRepository.findByCurriculumVersionIdAndSemesterNumber(curriculumVersionId, termNumber)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public List<CurriculumSemesterCourseDto> getAllByCurriculumVersion(Long curriculumVersionId) {
        if (!curriculumVersionRepository.existsById(curriculumVersionId)) {
            throw new ResourceNotFoundException(
                "Curriculum version not found with id: " + curriculumVersionId);
        }
        return courseRepository.findByCurriculumVersionId(curriculumVersionId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public CurriculumFullViewDto getFullCurriculum(Long curriculumVersionId) {
        CurriculumVersion cv = curriculumVersionRepository.findById(curriculumVersionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + curriculumVersionId));

        List<CurriculumSemesterCourse> allCourses = courseRepository.findByCurriculumVersionId(curriculumVersionId);

        Map<Integer, List<CurriculumSemesterCourseDto>> grouped = new LinkedHashMap<>();
        int totalTerms = cv.getProgram().getTotalTerms();
        for (int i = 1; i <= totalTerms; i++) {
            grouped.put(i, new ArrayList<>());
        }
        for (CurriculumSemesterCourse c : allCourses) {
            grouped.computeIfAbsent(c.getSemesterNumber(), k -> new ArrayList<>()).add(toDto(c));
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

    private CurriculumSemesterCourseDto toDto(CurriculumSemesterCourse c) {
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
            c.getCourse() != null ? c.getCourse().getId() : null,
            c.getCourse() != null ? c.getCourse().getName() : null,
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
