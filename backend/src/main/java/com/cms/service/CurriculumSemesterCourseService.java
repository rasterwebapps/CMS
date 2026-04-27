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
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Subject;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class CurriculumSemesterCourseService {

    private final CurriculumSemesterCourseRepository courseRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final SubjectRepository subjectRepository;

    public CurriculumSemesterCourseService(CurriculumSemesterCourseRepository courseRepository,
                                            CurriculumVersionRepository curriculumVersionRepository,
                                            SubjectRepository subjectRepository) {
        this.courseRepository = courseRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public CurriculumSemesterCourseDto addCourseToSemester(CurriculumSemesterCourseRequest request) {
        CurriculumVersion cv = curriculumVersionRepository.findById(request.curriculumVersionId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + request.curriculumVersionId()));

        int totalSemesters = cv.getProgram().getDurationYears() * 2;
        if (request.semesterNumber() < 1 || request.semesterNumber() > totalSemesters) {
            throw new IllegalArgumentException(
                "Semester number must be between 1 and " + totalSemesters +
                " for this program (duration " + cv.getProgram().getDurationYears() + " years)");
        }

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Subject not found with id: " + request.subjectId()));

        CurriculumSemesterCourse entry = new CurriculumSemesterCourse(
            cv, request.semesterNumber(), subject, request.sortOrder());
        return toDto(courseRepository.save(entry));
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
                                                                    Integer semesterNumber) {
        if (!curriculumVersionRepository.existsById(curriculumVersionId)) {
            throw new ResourceNotFoundException(
                "Curriculum version not found with id: " + curriculumVersionId);
        }
        return courseRepository.findByCurriculumVersionIdAndSemesterNumber(curriculumVersionId, semesterNumber)
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
        int totalSemesters = cv.getProgram().getDurationYears() * 2;
        for (int i = 1; i <= totalSemesters; i++) {
            grouped.put(i, new ArrayList<>());
        }
        for (CurriculumSemesterCourse c : allCourses) {
            grouped.computeIfAbsent(c.getSemesterNumber(), k -> new ArrayList<>()).add(toDto(c));
        }

        List<CurriculumFullViewDto.SemesterGroup> semesterGroups = grouped.entrySet().stream()
            .sorted(Comparator.comparingInt(Map.Entry::getKey))
            .map(e -> new CurriculumFullViewDto.SemesterGroup(e.getKey(), e.getValue()))
            .toList();

        return new CurriculumFullViewDto(
            cv.getId(),
            cv.getVersionName(),
            cv.getProgram().getId(),
            cv.getProgram().getName(),
            totalSemesters,
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
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
