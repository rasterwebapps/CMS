package com.cms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.GenerateRollNumbersRequest;
import com.cms.dto.RollNumberAssignment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.RollNumberSequence;
import com.cms.model.Student;
import com.cms.repository.CourseRepository;
import com.cms.repository.RollNumberSequenceRepository;
import com.cms.repository.StudentRepository;

/**
 * Service for automatic roll number generation following a configurable pattern.
 *
 * <p><strong>Roll Number Format:</strong> {@code [CollegeCode][CourseCode][Year][Sequence]}
 *
 * <p><strong>Example:</strong> {@code 959652026004}
 * <ul>
 *   <li>College Code: {@code 959} (3 digits, configured via {@code COLLEGE_CODE})</li>
 *   <li>Course Code: {@code 65} (2 digits, stored in {@code courses.roll_number_code})</li>
 *   <li>Year: {@code 2026} (4 digits, academic year of admission)</li>
 *   <li>Sequence: {@code 004} (3 digits, sequential from 001 to 999)</li>
 * </ul>
 *
 * <p><strong>Configuration:</strong> Uses {@code SystemConfiguration} keys:
 * <ul>
 *   <li>{@code ROLL_NUMBER_COLLEGE_CODE} - Institution code (e.g., "959")</li>
 *   <li>{@code courses.roll_number_code} - Per-course 2-digit code</li>
 * </ul>
 *
 * <p><strong>Sequence Management:</strong> Sequences are tracked per course per year
 * in the {@code roll_number_sequences} table. Concurrent generation is handled via
 * pessimistic locking.
 */
@Service
@Transactional(readOnly = true)
public class RollNumberGeneratorService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final RollNumberSequenceRepository sequenceRepository;
    private final SystemConfigurationService systemConfigurationService;

    public RollNumberGeneratorService(
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            RollNumberSequenceRepository sequenceRepository,
            SystemConfigurationService systemConfigurationService) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.sequenceRepository = sequenceRepository;
        this.systemConfigurationService = systemConfigurationService;
    }

    /**
     * Generate and assign roll numbers to a batch of students.
     * Students are sorted alphabetically by name before assignment.
     *
     * @param request Request containing student IDs, course ID, and academic year
     * @return List of roll number assignments
     * @throws ResourceNotFoundException if course or students not found
     * @throws IllegalStateException if any student already has a roll number
     * @throws IllegalArgumentException if course has no roll number code configured
     */
    @Transactional
    public List<RollNumberAssignment> generateAndAssignRollNumbers(GenerateRollNumbersRequest request) {
        // Validate course exists and has roll number code
        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        if (course.getRollNumberCode() == null || course.getRollNumberCode().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Course '" + course.getName() + "' has no roll number code configured");
        }

        // Validate and fetch students
        List<Student> students = new ArrayList<>();
        for (Long studentId : request.studentIds()) {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

            if (student.getRollNumber() != null) {
                throw new IllegalStateException(
                    "Student '" + student.getFullName() + "' already has a roll number: " + student.getRollNumber());
            }

            students.add(student);
        }

        // Sort students alphabetically by full name
        students.sort(Comparator.comparing(Student::getFullName));

        // Generate and assign roll numbers
        List<RollNumberAssignment> assignments = new ArrayList<>();
        for (Student student : students) {
            String rollNumber = generateNextRollNumber(course, request.academicYear());
            student.setRollNumber(rollNumber);
            studentRepository.save(student);

            assignments.add(new RollNumberAssignment(
                rollNumber,
                student.getId(),
                student.getFullName()
            ));
        }

        return assignments;
    }

    /**
     * Preview roll numbers that would be generated without committing them.
     *
     * @param request Request containing student IDs, course ID, and academic year
     * @return List of roll number assignments (preview only, not saved)
     */
    public List<RollNumberAssignment> previewRollNumbers(GenerateRollNumbersRequest request) {
        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        if (course.getRollNumberCode() == null || course.getRollNumberCode().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Course '" + course.getName() + "' has no roll number code configured");
        }

        // Fetch students and sort alphabetically
        List<Student> students = new ArrayList<>();
        for (Long studentId : request.studentIds()) {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
            students.add(student);
        }
        students.sort(Comparator.comparing(Student::getFullName));

        // Get next sequence number (without incrementing)
        int nextSequence = getNextSequenceNumber(request.courseId(), request.academicYear());

        // Generate preview roll numbers
        String collegeCode = getCollegeCode();
        String courseCode = course.getRollNumberCode();
        List<RollNumberAssignment> assignments = new ArrayList<>();

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            int sequence = nextSequence + i;

            if (sequence > 999) {
                throw new IllegalStateException(
                    "Sequence number exceeded maximum (999) for course '" + course.getName() +
                    "' in year " + request.academicYear());
            }

            String rollNumber = formatRollNumber(collegeCode, courseCode, request.academicYear(), sequence);
            assignments.add(new RollNumberAssignment(rollNumber, student.getId(), student.getFullName()));
        }

        return assignments;
    }

    /**
     * Generate the next roll number for a course and year.
     * This method updates the sequence counter in the database.
     *
     * @param course Course entity
     * @param academicYear Academic year (e.g., 2026)
     * @return Generated roll number string (e.g., "959652026004")
     */
    private String generateNextRollNumber(Course course, Integer academicYear) {
        String collegeCode = getCollegeCode();
        String courseCode = course.getRollNumberCode();

        // Get or create sequence with pessimistic lock
        RollNumberSequence sequence = sequenceRepository
            .findByCourseIdAndAcademicYearForUpdate(course.getId(), academicYear)
            .orElseGet(() -> {
                RollNumberSequence newSeq = new RollNumberSequence(course.getId(), academicYear, 0);
                return sequenceRepository.save(newSeq);
            });

        // Increment sequence
        int nextSequence = sequence.getLastSequence() + 1;

        if (nextSequence > 999) {
            throw new IllegalStateException(
                "Sequence number exceeded maximum (999) for course '" + course.getName() +
                "' in year " + academicYear);
        }

        sequence.setLastSequence(nextSequence);
        sequenceRepository.save(sequence);

        return formatRollNumber(collegeCode, courseCode, academicYear, nextSequence);
    }

    /**
     * Get the next sequence number without incrementing it.
     */
    private int getNextSequenceNumber(Long courseId, Integer academicYear) {
        Optional<RollNumberSequence> sequence =
            sequenceRepository.findByCourseIdAndAcademicYear(courseId, academicYear);

        return sequence.map(s -> s.getLastSequence() + 1).orElse(1);
    }

    /**
     * Format a roll number string.
     *
     * @param collegeCode 3-digit college code
     * @param courseCode 2-digit course code
     * @param year 4-digit year
     * @param sequence 3-digit sequence number
     * @return Formatted roll number (e.g., "959652026004")
     */
    private String formatRollNumber(String collegeCode, String courseCode, Integer year, int sequence) {
        return String.format("%s%s%d%03d", collegeCode, courseCode, year, sequence);
    }

    /**
     * Get college code from system configuration.
     * Defaults to "000" if not configured.
     */
    private String getCollegeCode() {
        try {
            String code = systemConfigurationService.getValueByKey("ROLL_NUMBER_COLLEGE_CODE");
            if (code == null || code.trim().isEmpty()) {
                return "000";
            }
            // Ensure 3 digits
            return String.format("%03d", Integer.parseInt(code.trim()));
        } catch (Exception e) {
            return "000"; // Default if configuration not found
        }
    }
}

