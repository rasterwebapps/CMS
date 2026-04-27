package com.cms.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseRegistrationDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.RegistrationStatus;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class CourseRegistrationServiceImpl implements CourseRegistrationService {

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudentTermEnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final TermInstanceRepository termInstanceRepository;

    public CourseRegistrationServiceImpl(CourseRegistrationRepository courseRegistrationRepository,
                                          StudentTermEnrollmentRepository enrollmentRepository,
                                          CourseOfferingRepository courseOfferingRepository,
                                          TermInstanceRepository termInstanceRepository) {
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.termInstanceRepository = termInstanceRepository;
    }

    @Override
    @Transactional
    public int generateRegistrationsForTermInstance(Long termInstanceId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }

        // All enrolled StudentTermEnrollments for this term
        List<StudentTermEnrollment> enrollments = enrollmentRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
            .toList();

        int count = 0;
        for (StudentTermEnrollment enrollment : enrollments) {
            // Find active course offerings for this term, matching the enrollment's semester number
            // and belonging to the cohort's curriculum version
            List<CourseOffering> offerings = courseOfferingRepository
                .findByTermInstanceIdAndSemesterNumber(termInstanceId, enrollment.getSemesterNumber())
                .stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .filter(o -> o.getCurriculumVersion().getProgram().getId()
                    .equals(enrollment.getCohort().getProgram().getId()))
                .toList();

            for (CourseOffering offering : offerings) {
                Optional<CourseRegistration> existing =
                    courseRegistrationRepository.findByStudentTermEnrollmentIdAndCourseOfferingId(
                        enrollment.getId(), offering.getId());
                if (existing.isEmpty()) {
                    CourseRegistration registration = new CourseRegistration();
                    registration.setStudentTermEnrollment(enrollment);
                    registration.setCourseOffering(offering);
                    registration.setStatus(RegistrationStatus.REGISTERED);
                    courseRegistrationRepository.save(registration);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public List<CourseRegistrationDto> getRegistrationsByEnrollment(Long enrollmentId) {
        return courseRegistrationRepository.findByStudentTermEnrollmentId(enrollmentId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<CourseRegistrationDto> getRegistrationsByCourseOffering(Long courseOfferingId) {
        return courseRegistrationRepository.findByCourseOfferingId(courseOfferingId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public CourseRegistrationDto getById(Long id) {
        CourseRegistration registration = courseRegistrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course registration not found with id: " + id));
        return toDto(registration);
    }

    @Override
    @Transactional
    public CourseRegistrationDto dropRegistration(Long id) {
        CourseRegistration registration = courseRegistrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course registration not found with id: " + id));
        registration.setStatus(RegistrationStatus.DROPPED);
        return toDto(courseRegistrationRepository.save(registration));
    }

    private CourseRegistrationDto toDto(CourseRegistration r) {
        StudentTermEnrollment enrollment = r.getStudentTermEnrollment();
        CourseOffering offering = r.getCourseOffering();
        return new CourseRegistrationDto(
            r.getId(),
            enrollment.getId(),
            enrollment.getStudent().getId(),
            enrollment.getStudent().getFullName(),
            enrollment.getCohort().getCohortCode(),
            offering.getId(),
            offering.getSubject().getName(),
            offering.getSubject().getCode(),
            offering.getSemesterNumber(),
            r.getStatus(),
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }
}
