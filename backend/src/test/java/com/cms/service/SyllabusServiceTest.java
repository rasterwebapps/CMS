package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.SyllabusActivationRequest;
import com.cms.dto.SyllabusRequest;
import com.cms.dto.SyllabusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Course;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Program;
import com.cms.model.Subject;
import com.cms.model.Syllabus;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.SubjectType;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.SyllabusRepository;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceTest {

    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;

    private SyllabusService syllabusService;

    private CurriculumSemesterCourse testMapping;

    @BeforeEach
    void setUp() {
        syllabusService = new SyllabusService(syllabusRepository, curriculumSemesterCourseRepository);
        testMapping = createMapping(1L, 1, "Data Structures", "CS201", 30, 15, 10);
    }

    @Test
    void shouldCreateSyllabusWithAutoAssignedVersion() {
        SyllabusRequest request = new SyllabusRequest(
            1L,
            "Course objectives", "Course content",
            "Text books", "Reference books",
            "Course outcomes", false
        );

        when(curriculumSemesterCourseRepository.findById(1L)).thenReturn(Optional.of(testMapping));
        when(syllabusRepository.findMaxVersion(1L)).thenReturn(0);
        when(syllabusRepository.save(any(Syllabus.class))).thenAnswer(inv -> {
            Syllabus s = inv.getArgument(0);
            s.setId(1L);
            s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            return s;
        });

        SyllabusResponse response = syllabusService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.subjectId()).isEqualTo(1L);
        assertThat(response.curriculumTermCourseId()).isEqualTo(1L);
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.theoryHours()).isEqualTo(30);
        assertThat(response.labHours()).isEqualTo(15);
        assertThat(response.clinicalHours()).isEqualTo(10);

        verify(syllabusRepository, never()).clearActiveForMapping(any());
    }

    @Test
    void shouldAssignNextVersionAfterExistingOnes() {
        SyllabusRequest request = new SyllabusRequest(
            1L, "Objectives", "Content", "Text", "Ref", "CO", false
        );

        when(curriculumSemesterCourseRepository.findById(1L)).thenReturn(Optional.of(testMapping));
        when(syllabusRepository.findMaxVersion(1L)).thenReturn(3);
        when(syllabusRepository.save(any(Syllabus.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Syllabus> captor = ArgumentCaptor.forClass(Syllabus.class);
        syllabusService.create(request);
        verify(syllabusRepository).save(captor.capture());

        assertThat(captor.getValue().getVersion()).isEqualTo(4);
    }

    @Test
    void shouldClearOtherActiveVersionsWhenCreatingAnActiveOne() {
        SyllabusRequest request = new SyllabusRequest(
            1L, "Objectives", "Content", "Text", "Ref", "CO", true
        );

        when(curriculumSemesterCourseRepository.findById(1L)).thenReturn(Optional.of(testMapping));
        when(syllabusRepository.findMaxVersion(1L)).thenReturn(1);
        when(syllabusRepository.save(any(Syllabus.class))).thenAnswer(inv -> inv.getArgument(0));

        syllabusService.create(request);

        verify(syllabusRepository).clearActiveForMapping(1L);
    }

    @Test
    void shouldThrowExceptionWhenCreatingSyllabusWithNonExistentMapping() {
        SyllabusRequest request = new SyllabusRequest(
            999L, "Objectives", "Content", "Text", "Ref", "CO", true
        );

        when(curriculumSemesterCourseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syllabusService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curriculum mapping not found with id: 999");

        verify(syllabusRepository, never()).save(any(Syllabus.class));
    }

    @Test
    void shouldFindAllSyllabi() {
        Syllabus syllabus1 = createSyllabus(1L, testMapping, 1, true);
        Syllabus syllabus2 = createSyllabus(2L, testMapping, 2, false);

        when(syllabusRepository.findAll()).thenReturn(List.of(syllabus1, syllabus2));

        List<SyllabusResponse> responses = syllabusService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).version()).isEqualTo(1);
        assertThat(responses.get(1).version()).isEqualTo(2);
    }

    @Test
    void shouldFindSyllabusById() {
        Syllabus syllabus = createSyllabus(1L, testMapping, 1, true);

        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(syllabus));

        SyllabusResponse response = syllabusService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.subjectId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenSyllabusNotFoundById() {
        when(syllabusRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syllabusService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Syllabus not found with id: 999");
    }

    @Test
    void shouldFindSyllabusBySubjectId() {
        Syllabus syllabus = createSyllabus(1L, testMapping, 1, true);

        when(syllabusRepository.findByCurriculumSemesterCourse_Subject_Id(1L)).thenReturn(List.of(syllabus));

        List<SyllabusResponse> responses = syllabusService.findBySubjectId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).subjectId()).isEqualTo(1L);
    }

    @Test
    void shouldFindActiveSyllabusBySubjectId() {
        Syllabus syllabus = createSyllabus(1L, testMapping, 1, true);

        when(syllabusRepository.findByCurriculumSemesterCourse_Subject_IdAndIsActiveTrue(1L))
            .thenReturn(Optional.of(syllabus));

        SyllabusResponse response = syllabusService.findActiveBySubjectId(1L);

        assertThat(response.subjectId()).isEqualTo(1L);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenNoActiveSyllabusFound() {
        when(syllabusRepository.findByCurriculumSemesterCourse_Subject_IdAndIsActiveTrue(1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> syllabusService.findActiveBySubjectId(1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No active syllabus found for subject id: 1");
    }

    @Test
    void shouldActivateAndClearOtherActiveVersionsForSameMapping() {
        Syllabus existing = createSyllabus(1L, testMapping, 2, false);

        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(syllabusRepository.save(any(Syllabus.class))).thenAnswer(inv -> inv.getArgument(0));

        SyllabusResponse response = syllabusService.setActive(1L, new SyllabusActivationRequest(true));

        assertThat(response.isActive()).isTrue();
        verify(syllabusRepository).clearActiveForMapping(testMapping.getId());
    }

    @Test
    void shouldDeactivateWithoutClearingOthers() {
        Syllabus existing = createSyllabus(1L, testMapping, 2, true);

        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(syllabusRepository.save(any(Syllabus.class))).thenAnswer(inv -> inv.getArgument(0));

        SyllabusResponse response = syllabusService.setActive(1L, new SyllabusActivationRequest(false));

        assertThat(response.isActive()).isFalse();
        verify(syllabusRepository, never()).clearActiveForMapping(any());
    }

    @Test
    void shouldThrowExceptionWhenActivatingNonExistentSyllabus() {
        when(syllabusRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syllabusService.setActive(999L, new SyllabusActivationRequest(true)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Syllabus not found with id: 999");
    }

    private CurriculumSemesterCourse createMapping(Long id, Integer termNumber, String subjectName, String subjectCode,
                                                     Integer theoryHours, Integer labHours, Integer clinicalHours) {
        Program program = new Program("BSc Nursing", "BSCN", 4, ProgramStatus.ACTIVE);
        program.setId(1L);
        Course course = new Course("BSc Nursing", "BSCN-C", null, program);
        course.setId(1L);
        AcademicYear ay = new AcademicYear("2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), false);
        ay.setId(1L);
        CurriculumVersion cv = new CurriculumVersion(program, course, "BSCN-2026", ay, true);
        cv.setId(1L);
        Subject subject = new Subject(subjectName, subjectCode, 4, 3, 1, null, 1);
        subject.setId(1L);

        CurriculumSemesterCourse mapping = new CurriculumSemesterCourse(cv, termNumber, subject, 1);
        mapping.setId(id);
        mapping.setTheoryHours(theoryHours);
        mapping.setLabHours(labHours);
        mapping.setClinicalHours(clinicalHours);
        mapping.setSubjectType(SubjectType.CORE);
        mapping.setIsElective(false);
        return mapping;
    }

    private Syllabus createSyllabus(Long id, CurriculumSemesterCourse mapping, Integer version, Boolean isActive) {
        Syllabus syllabus = new Syllabus(
            mapping, version,
            "Objectives", "Content", "Text books",
            "Reference books", "Course outcomes", isActive
        );
        syllabus.setId(id);
        Instant now = Instant.now();
        syllabus.setCreatedAt(now);
        syllabus.setUpdatedAt(now);
        return syllabus;
    }
}
