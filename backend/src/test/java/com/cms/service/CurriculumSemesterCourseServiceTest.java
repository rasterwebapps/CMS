package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CurriculumFullViewDto;
import com.cms.dto.CurriculumSemesterCourseDto;
import com.cms.dto.CurriculumSemesterCourseRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Course;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Program;
import com.cms.model.Subject;
import com.cms.model.enums.ProgramStatus;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class CurriculumSemesterCourseServiceTest {

    @Mock
    private CurriculumSemesterCourseRepository courseRepository;

    @Mock
    private CurriculumVersionRepository curriculumVersionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    private CurriculumSemesterCourseService service;

    private Program testProgram;
    private AcademicYear testAcademicYear;
    private CurriculumVersion testCv;
    private Subject testSubject;

    @BeforeEach
    void setUp() {
        service = new CurriculumSemesterCourseService(
            courseRepository, curriculumVersionRepository, subjectRepository);

        testProgram = createProgram(1L, "BSc Nursing", "BSCN", 4);
        testAcademicYear = createAcademicYear(1L, "2026-2027");
        testCv = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear);
        testSubject = createSubject(1L, "Anatomy", "ANAT");
    }

    @Test
    void shouldAddCourseToSemester() {
        CurriculumSemesterCourseRequest request = new CurriculumSemesterCourseRequest(1L, 1, 1L, 1);
        CurriculumSemesterCourse saved = createCsc(1L, testCv, 1, testSubject, 1);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(testCv));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(courseRepository.save(any(CurriculumSemesterCourse.class))).thenReturn(saved);

        CurriculumSemesterCourseDto dto = service.addCourseToSemester(request);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.semesterNumber()).isEqualTo(1);
        assertThat(dto.subjectName()).isEqualTo("Anatomy");

        verify(courseRepository).save(any(CurriculumSemesterCourse.class));
    }

    @Test
    void shouldRejectInvalidSemesterNumber() {
        // 4-year program has 8 semesters max; semester 9 is invalid
        CurriculumSemesterCourseRequest request = new CurriculumSemesterCourseRequest(1L, 9, 1L, null);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(testCv));

        assertThatThrownBy(() -> service.addCourseToSemester(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("8");
    }

    @Test
    void shouldRejectSemesterNumberBelowOne() {
        CurriculumSemesterCourseRequest request = new CurriculumSemesterCourseRequest(1L, 0, 1L, null);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(testCv));

        assertThatThrownBy(() -> service.addCourseToSemester(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenCurriculumVersionNotFoundOnAdd() {
        CurriculumSemesterCourseRequest request = new CurriculumSemesterCourseRequest(999L, 1, 1L, null);

        when(curriculumVersionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addCourseToSemester(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldThrowWhenSubjectNotFoundOnAdd() {
        CurriculumSemesterCourseRequest request = new CurriculumSemesterCourseRequest(1L, 1, 999L, null);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(testCv));
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addCourseToSemester(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldRemoveCourseFromSemester() {
        when(courseRepository.existsById(1L)).thenReturn(true);

        service.removeCourseFromSemester(1L);

        verify(courseRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenRemovingNonExistentCourse() {
        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.removeCourseFromSemester(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetCoursesBySemester() {
        CurriculumSemesterCourse csc = createCsc(1L, testCv, 1, testSubject, 1);

        when(curriculumVersionRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByCurriculumVersionIdAndSemesterNumber(1L, 1))
            .thenReturn(List.of(csc));

        List<CurriculumSemesterCourseDto> dtos = service.getCoursesBySemester(1L, 1);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).semesterNumber()).isEqualTo(1);
    }

    @Test
    void shouldThrowWhenCvNotFoundForGetBySemester() {
        when(curriculumVersionRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getCoursesBySemester(999L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetAllByCurriculumVersion() {
        CurriculumSemesterCourse csc = createCsc(1L, testCv, 1, testSubject, 1);

        when(curriculumVersionRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByCurriculumVersionId(1L)).thenReturn(List.of(csc));

        List<CurriculumSemesterCourseDto> dtos = service.getAllByCurriculumVersion(1L);

        assertThat(dtos).hasSize(1);
    }

    @Test
    void shouldThrowWhenCvNotFoundForGetAll() {
        when(curriculumVersionRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getAllByCurriculumVersion(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetFullCurriculumGroupedBySemester() {
        Subject subject2 = createSubject(2L, "Physiology", "PHYS");
        CurriculumSemesterCourse csc1 = createCsc(1L, testCv, 1, testSubject, 1);
        CurriculumSemesterCourse csc2 = createCsc(2L, testCv, 2, subject2, 1);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(testCv));
        when(courseRepository.findByCurriculumVersionId(1L)).thenReturn(List.of(csc1, csc2));

        CurriculumFullViewDto fullView = service.getFullCurriculum(1L);

        assertThat(fullView.curriculumVersionId()).isEqualTo(1L);
        assertThat(fullView.totalSemesters()).isEqualTo(8);  // 4-year program
        assertThat(fullView.semesters()).hasSize(8);  // all 8 semesters present
        assertThat(fullView.semesters().get(0).courses()).hasSize(1);
        assertThat(fullView.semesters().get(1).courses()).hasSize(1);
        // Remaining semesters are empty
        assertThat(fullView.semesters().get(2).courses()).isEmpty();
    }

    @Test
    void shouldThrowWhenCvNotFoundForFullCurriculum() {
        when(curriculumVersionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFullCurriculum(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    private Program createProgram(Long id, String name, String code, Integer durationYears) {
        Program p = new Program(name, code, durationYears, ProgramStatus.ACTIVE);
        p.setId(id);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private AcademicYear createAcademicYear(Long id, String name) {
        AcademicYear ay = new AcademicYear(name,
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), false);
        ay.setId(id);
        ay.setCreatedAt(Instant.now());
        ay.setUpdatedAt(Instant.now());
        return ay;
    }

    private CurriculumVersion createCurriculumVersion(Long id, Program program, String versionName, AcademicYear ay) {
        CurriculumVersion cv = new CurriculumVersion(program, versionName, ay, true);
        cv.setId(id);
        cv.setCreatedAt(Instant.now());
        cv.setUpdatedAt(Instant.now());
        return cv;
    }

    private Subject createSubject(Long id, String name, String code) {
        Course course = new Course();
        course.setId(1L);
        Subject s = new Subject(name, code, 4, 3, 1, course, null, 1);
        s.setId(id);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }

    private CurriculumSemesterCourse createCsc(Long id, CurriculumVersion cv,
                                                Integer semesterNumber, Subject subject, Integer sortOrder) {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse(cv, semesterNumber, subject, sortOrder);
        csc.setId(id);
        csc.setCreatedAt(Instant.now());
        csc.setUpdatedAt(Instant.now());
        return csc;
    }
}
