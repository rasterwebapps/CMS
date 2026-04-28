package com.cms.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseOfferingDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class CourseOfferingServiceImpl implements CourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final CohortRepository cohortRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;

    public CourseOfferingServiceImpl(CourseOfferingRepository courseOfferingRepository,
                                      TermInstanceRepository termInstanceRepository,
                                      CohortRepository cohortRepository,
                                      CurriculumVersionRepository curriculumVersionRepository,
                                      CurriculumSemesterCourseRepository curriculumSemesterCourseRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.cohortRepository = cohortRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
    }

    @Override
    @Transactional
    public int generateOfferingsForTermInstance(Long termInstanceId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));

        List<Cohort> activeCohorts = cohortRepository.findByStatus(CohortStatus.ACTIVE);
        int count = 0;

        for (Cohort cohort : activeCohorts) {
            List<CurriculumVersion> activeCVs =
                curriculumVersionRepository.findByProgramIdAndIsActiveTrue(cohort.getProgram().getId());
            if (activeCVs.isEmpty()) {
                continue;
            }
            // Use the most recently created active CV (max by createdAt)
            CurriculumVersion cv = activeCVs.stream()
                .max(java.util.Comparator.comparing(CurriculumVersion::getCreatedAt))
                .orElseThrow();

            Integer totalSemesters = cohort.getProgram().getTotalSemesters();
            if (totalSemesters == null) {
                continue;
            }

            // Determine relevant semester numbers for this term type
            Set<Integer> relevantSemesters = buildRelevantSemesters(termInstance.getTermType(), totalSemesters);

            // Load CurriculumSemesterCourses for this CV where semesterNumber is relevant
            List<CurriculumSemesterCourse> courses =
                curriculumSemesterCourseRepository.findByCurriculumVersionId(cv.getId());

            for (CurriculumSemesterCourse csc : courses) {
                if (!relevantSemesters.contains(csc.getSemesterNumber())) {
                    continue;
                }
                // Idempotent check: create only if not already existing
                Optional<CourseOffering> existing =
                    courseOfferingRepository.findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(
                        termInstanceId, cv.getId(), csc.getSubject().getId(), csc.getSemesterNumber());
                if (existing.isEmpty()) {
                    CourseOffering offering = new CourseOffering();
                    offering.setTermInstance(termInstance);
                    offering.setCurriculumVersion(cv);
                    offering.setSubject(csc.getSubject());
                    offering.setSemesterNumber(csc.getSemesterNumber());
                    offering.setIsActive(true);
                    courseOfferingRepository.save(offering);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public List<CourseOfferingDto> getOfferingsByTermInstance(Long termInstanceId) {
        return courseOfferingRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<CourseOfferingDto> getOfferingsByTermInstanceAndSemester(Long termInstanceId,
                                                                          Integer semesterNumber) {
        return courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(termInstanceId, semesterNumber)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public CourseOfferingDto getById(Long id) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        return toDto(offering);
    }

    @Override
    @Transactional
    public CourseOfferingDto updateOffering(Long id, Long facultyId, String sectionLabel) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        offering.setFacultyId(facultyId);
        offering.setSectionLabel(sectionLabel);
        return toDto(courseOfferingRepository.save(offering));
    }

    @Override
    @Transactional
    public void deactivateOffering(Long id) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        offering.setIsActive(false);
        courseOfferingRepository.save(offering);
    }

    @Override
    @Transactional
    public void deactivateAllOfferingsForTermInstance(Long termInstanceId) {
        List<CourseOffering> offerings = courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId);
        for (CourseOffering o : offerings) {
            o.setIsActive(false);
            courseOfferingRepository.save(o);
        }
    }

    private Set<Integer> buildRelevantSemesters(TermType termType, int totalSemesters) {
        return IntStream.rangeClosed(1, totalSemesters)
            .filter(s -> termType == TermType.ODD ? s % 2 != 0 : s % 2 == 0)
            .boxed()
            .collect(Collectors.toSet());
    }

    private CourseOfferingDto toDto(CourseOffering o) {
        String termInstanceLabel = o.getTermInstance().getAcademicYear().getName()
            + " " + o.getTermInstance().getTermType();
        return new CourseOfferingDto(
            o.getId(),
            o.getTermInstance().getId(),
            termInstanceLabel,
            o.getCurriculumVersion().getId(),
            o.getCurriculumVersion().getVersionName(),
            o.getSubject().getId(),
            o.getSubject().getName(),
            o.getSubject().getCode(),
            o.getSemesterNumber(),
            o.getFacultyId(),
            o.getSectionLabel(),
            o.getIsActive(),
            o.getCreatedAt(),
            o.getUpdatedAt()
        );
    }
}
