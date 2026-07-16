package com.cms.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CurriculumVersionDto;
import com.cms.dto.CurriculumVersionRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.AttendanceThreshold;
import com.cms.model.Course;
import com.cms.model.CurriculumElectiveGroup;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Program;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AttendanceThresholdRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.CurriculumElectiveGroupRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class CurriculumVersionService {

    private final CurriculumVersionRepository curriculumVersionRepository;
    private final ProgramRepository programRepository;
    private final AcademicYearRepository academicYearRepository;
    private final CourseRepository courseRepository;
    private final CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;
    private final CurriculumElectiveGroupRepository curriculumElectiveGroupRepository;
    private final AttendanceThresholdRepository attendanceThresholdRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public CurriculumVersionService(CurriculumVersionRepository curriculumVersionRepository,
                                     ProgramRepository programRepository,
                                     AcademicYearRepository academicYearRepository,
                                     CourseRepository courseRepository,
                                     CurriculumSemesterCourseRepository curriculumSemesterCourseRepository,
                                     CurriculumElectiveGroupRepository curriculumElectiveGroupRepository,
                                     AttendanceThresholdRepository attendanceThresholdRepository,
                                     CourseOfferingRepository courseOfferingRepository) {
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
        this.courseRepository = courseRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
        this.curriculumElectiveGroupRepository = curriculumElectiveGroupRepository;
        this.attendanceThresholdRepository = attendanceThresholdRepository;
        this.courseOfferingRepository = courseOfferingRepository;
    }

    @Transactional
    public CurriculumVersionDto createCurriculumVersion(CurriculumVersionRequest request) {
        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.effectiveFromAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.effectiveFromAcademicYearId()));

        Course course = resolveCourse(request.courseId(), program);

        if (curriculumVersionRepository.existsByProgramAndCourseAndVersionName(
                request.programId(), request.courseId(), request.versionName(), null)) {
            throw new IllegalArgumentException(
                "A curriculum version named '" + request.versionName() + "' already exists for this program/course");
        }

        Boolean isActive = request.isActive() != null ? request.isActive() : true;
        CurriculumVersion cv = new CurriculumVersion(program, course, request.versionName(), academicYear, isActive);
        return toDto(curriculumVersionRepository.save(cv));
    }

    public List<CurriculumVersionDto> getCurriculumVersionsByProgram(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        return curriculumVersionRepository.findByProgramId(programId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public Page<CurriculumVersionDto> findPage(String search, Long programId, Boolean isActive, Pageable pageable) {
        Specification<CurriculumVersion> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("versionName")), pattern),
                cb.like(cb.lower(root.get("program").get("name")), pattern),
                cb.like(cb.lower(root.get("course").get("name")), pattern)
            ));
        }
        if (programId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("program").get("id"), programId));
        }
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }
        return curriculumVersionRepository.findAll(spec, pageable).map(this::toDto);
    }

    public boolean nameExists(Long programId, Long courseId, String versionName, Long excludeId) {
        return curriculumVersionRepository.existsByProgramAndCourseAndVersionName(
            programId, courseId, versionName == null ? "" : versionName.trim(), excludeId);
    }

    public CurriculumVersionDto getById(Long id) {
        CurriculumVersion cv = curriculumVersionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + id));
        return toDto(cv);
    }

    @Transactional
    public CurriculumVersionDto update(Long id, CurriculumVersionRequest request) {
        CurriculumVersion cv = curriculumVersionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + id));

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.effectiveFromAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.effectiveFromAcademicYearId()));

        Course course = resolveCourse(request.courseId(), program);

        if (curriculumVersionRepository.existsByProgramAndCourseAndVersionName(
                request.programId(), request.courseId(), request.versionName(), id)) {
            throw new IllegalArgumentException(
                "A curriculum version named '" + request.versionName() + "' already exists for this program/course");
        }

        cv.setProgram(program);
        cv.setCourse(course);
        cv.setVersionName(request.versionName());
        cv.setEffectiveFromAcademicYear(academicYear);
        if (request.isActive() != null) {
            cv.setIsActive(request.isActive());
        }

        return toDto(curriculumVersionRepository.save(cv));
    }

    @Transactional
    public CurriculumVersionDto deactivateCurriculumVersion(Long id) {
        CurriculumVersion cv = curriculumVersionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + id));
        // TODO: check if cohorts are attached (Phase 2)
        cv.setIsActive(false);
        return toDto(curriculumVersionRepository.save(cv));
    }

    @Transactional
    public void delete(Long id) {
        if (!curriculumVersionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Curriculum version not found with id: " + id);
        }
        if (courseOfferingRepository.existsByCurriculumVersionId(id)) {
            throw new IllegalStateException(
                "Cannot delete curriculum version because course offerings have been created against it.");
        }
        if (curriculumSemesterCourseRepository.existsByCurriculumVersionId(id)) {
            throw new IllegalStateException(
                "Cannot delete curriculum version because subjects are mapped into it. "
                    + "Remove the term/subject mapping on the Curriculum Map screen first.");
        }
        // No subjects mapped in — only empty elective groups (if any) can still be hanging around.
        curriculumElectiveGroupRepository.deleteByCurriculumVersionId(id);
        curriculumVersionRepository.deleteById(id);
    }

    @Transactional
    public CurriculumVersionDto cloneCurriculumVersion(Long sourceId, String newVersionName,
                                                        Long newEffectiveAcademicYearId) {
        CurriculumVersion source = curriculumVersionRepository.findById(sourceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + sourceId));

        AcademicYear newAcademicYear = academicYearRepository.findById(newEffectiveAcademicYearId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + newEffectiveAcademicYearId));

        if (curriculumVersionRepository.existsByProgramAndCourseAndVersionName(
                source.getProgram().getId(), source.getCourse().getId(), newVersionName, null)) {
            throw new IllegalArgumentException(
                "A curriculum version named '" + newVersionName + "' already exists for this program/course");
        }

        CurriculumVersion clone = new CurriculumVersion(
            source.getProgram(), source.getCourse(), newVersionName, newAcademicYear, true);
        clone = curriculumVersionRepository.save(clone);

        // Deep-copy the source's own curriculum content (term/subject mapping, elective groups,
        // attendance thresholds) — Clone is meant to duplicate a full curriculum, not just the
        // version stub, so a cloned version starts pre-populated instead of empty.
        Map<Long, CurriculumElectiveGroup> electiveGroupMap = new HashMap<>();
        for (CurriculumElectiveGroup sourceGroup : curriculumElectiveGroupRepository.findByCurriculumVersionId(source.getId())) {
            CurriculumElectiveGroup newGroup = new CurriculumElectiveGroup(
                clone, sourceGroup.getTermNumber(), sourceGroup.getGroupName(), sourceGroup.getGroupCode());
            electiveGroupMap.put(sourceGroup.getId(), curriculumElectiveGroupRepository.save(newGroup));
        }

        for (CurriculumSemesterCourse sourceCourse : curriculumSemesterCourseRepository.findByCurriculumVersionId(source.getId())) {
            CurriculumSemesterCourse newCourse = new CurriculumSemesterCourse(
                clone, sourceCourse.getSemesterNumber(), sourceCourse.getSubject(), sourceCourse.getSortOrder());
            newCourse.setTheoryHours(sourceCourse.getTheoryHours());
            newCourse.setLabHours(sourceCourse.getLabHours());
            newCourse.setClinicalHours(sourceCourse.getClinicalHours());
            newCourse.setSubjectType(sourceCourse.getSubjectType());
            newCourse.setIsElective(sourceCourse.getIsElective());
            if (sourceCourse.getElectiveGroup() != null) {
                newCourse.setElectiveGroup(electiveGroupMap.get(sourceCourse.getElectiveGroup().getId()));
            }
            newCourse = curriculumSemesterCourseRepository.save(newCourse);

            for (AttendanceThreshold sourceThreshold
                    : attendanceThresholdRepository.findByCurriculumSemesterCourseId(sourceCourse.getId())) {
                attendanceThresholdRepository.save(new AttendanceThreshold(
                    newCourse, sourceThreshold.getAttendanceType(), sourceThreshold.getMinPercentage()));
            }
        }

        return toDto(curriculumVersionRepository.findById(clone.getId()).orElseThrow());
    }

    /** Resolves the mandatory course, verifying it actually belongs to the given program. */
    private Course resolveCourse(Long courseId, Program program) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (!course.getProgram().getId().equals(program.getId())) {
            throw new IllegalArgumentException(
                "Course " + course.getName() + " does not belong to program " + program.getName());
        }
        return course;
    }

    private CurriculumVersionDto toDto(CurriculumVersion cv) {
        Course course = cv.getCourse();
        Long id = cv.getId();
        int termCount = (int) curriculumSemesterCourseRepository.countDistinctTermsByCurriculumVersionId(id);
        int subjectCount = (int) curriculumSemesterCourseRepository.countDistinctSubjectsByCurriculumVersionId(id);
        boolean deletable = !courseOfferingRepository.existsByCurriculumVersionId(id)
            && !curriculumSemesterCourseRepository.existsByCurriculumVersionId(id);
        return new CurriculumVersionDto(
            id,
            cv.getProgram().getId(),
            cv.getProgram().getName(),
            course.getId(),
            course.getName(),
            cv.getVersionName(),
            cv.getEffectiveFromAcademicYear().getId(),
            cv.getEffectiveFromAcademicYear().getName(),
            cv.getIsActive(),
            termCount,
            subjectCount,
            deletable,
            cv.getCreatedAt(),
            cv.getUpdatedAt()
        );
    }
}
