package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.SubjectRequest;
import com.cms.dto.SubjectResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Department;
import com.cms.model.Program;
import com.cms.model.Subject;
import com.cms.model.enums.ProgramLevel;
import com.cms.repository.CourseRepository;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    private SubjectService subjectService;

    private Program program;
    private Course course;
    private Department department;
    private Subject testSubject;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        subjectService = new SubjectService(subjectRepository, courseRepository, departmentRepository);

        department = new Department();
        department.setId(1L);
        department.setName("Medical-Surgical Nursing");
        department.setCode("MSN");
        department.setCreatedAt(now);
        department.setUpdatedAt(now);

        program = new Program();
        program.setId(1L);
        program.setName("B.Sc. Nursing");
        program.setCode("BSCN");
        program.setProgramLevel(ProgramLevel.UNDERGRADUATE);
        program.setDepartments(new HashSet<>(Set.of(department)));
        program.setCreatedAt(now);
        program.setUpdatedAt(now);

        course = new Course();
        course.setId(1L);
        course.setName("B.Sc. Nursing Course");
        course.setCode("BSN");
        course.setSpecialization("General");
        course.setProgram(program);
        course.setCreatedAt(now);
        course.setUpdatedAt(now);

        testSubject = new Subject("Anatomy", "ANAT101", 4, 3, 1, course, department, 1);
        testSubject.setId(1L);
        testSubject.setCreatedAt(now);
        testSubject.setUpdatedAt(now);
    }

    @Test
    void shouldCreateSubject() {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, 1L, 1L, 1);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);

        SubjectResponse response = subjectService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Anatomy");
        assertThat(response.code()).isEqualTo("ANAT101");
        assertThat(response.credits()).isEqualTo(4);
        assertThat(response.theoryCredits()).isEqualTo(3);
        assertThat(response.labCredits()).isEqualTo(1);
        assertThat(response.semester()).isEqualTo(1);
        assertThat(response.course().id()).isEqualTo(1L);
        assertThat(response.department().id()).isEqualTo(1L);

        ArgumentCaptor<Subject> captor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Anatomy");
    }

    @Test
    void shouldCreateSubjectWithoutDepartment() {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, 1L, null, 1);
        Subject subjectNoDept = new Subject("Anatomy", "ANAT101", 4, 3, 1, course, null, 1);
        subjectNoDept.setId(2L);
        subjectNoDept.setCreatedAt(now);
        subjectNoDept.setUpdatedAt(now);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(subjectRepository.save(any(Subject.class))).thenReturn(subjectNoDept);

        SubjectResponse response = subjectService.create(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.department()).isNull();
        verify(departmentRepository, never()).findById(any());
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnCreate() {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, 999L, null, 1);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldThrowWhenDepartmentNotFoundOnCreate() {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, 1L, 999L, 1);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");
    }

    @Test
    void shouldFindAllSubjects() {
        when(subjectRepository.findAll()).thenReturn(List.of(testSubject));

        List<SubjectResponse> results = subjectService.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Anatomy");
        verify(subjectRepository).findAll();
    }

    @Test
    void shouldFindSubjectById() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));

        SubjectResponse response = subjectService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Anatomy");
    }

    @Test
    void shouldThrowWhenSubjectNotFoundById() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");
    }

    @Test
    void shouldFindSubjectsByCourseId() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.findByCourseId(1L)).thenReturn(List.of(testSubject));

        List<SubjectResponse> results = subjectService.findByCourseId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Anatomy");
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnFindByCourseId() {
        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.findByCourseId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldFindSubjectsByDepartmentId() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.findByDepartmentId(1L)).thenReturn(List.of(testSubject));

        List<SubjectResponse> results = subjectService.findByDepartmentId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Anatomy");
    }

    @Test
    void shouldThrowWhenDepartmentNotFoundOnFindByDepartmentId() {
        when(departmentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.findByDepartmentId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");
    }

    @Test
    void shouldUpdateSubject() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, 1L, 1L, 2);

        Subject updatedSubject = new Subject("Physiology", "PHYS101", 5, 4, 1, course, department, 2);
        updatedSubject.setId(1L);
        updatedSubject.setCreatedAt(now);
        updatedSubject.setUpdatedAt(now);

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(subjectRepository.save(any(Subject.class))).thenReturn(updatedSubject);

        SubjectResponse response = subjectService.update(1L, request);

        assertThat(response.name()).isEqualTo("Physiology");
        assertThat(response.code()).isEqualTo("PHYS101");
        assertThat(response.credits()).isEqualTo(5);
        assertThat(response.semester()).isEqualTo(2);
    }

    @Test
    void shouldUpdateSubjectWithoutDepartment() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, 1L, null, 2);

        Subject updatedSubject = new Subject("Physiology", "PHYS101", 5, 4, 1, course, null, 2);
        updatedSubject.setId(1L);
        updatedSubject.setCreatedAt(now);
        updatedSubject.setUpdatedAt(now);

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(subjectRepository.save(any(Subject.class))).thenReturn(updatedSubject);

        SubjectResponse response = subjectService.update(1L, request);

        assertThat(response.department()).isNull();
        verify(departmentRepository, never()).findById(any());
    }

    @Test
    void shouldThrowWhenSubjectNotFoundOnUpdate() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, 1L, null, 2);
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnUpdate() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, 999L, null, 2);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldThrowWhenDepartmentNotFoundOnUpdate() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, 1L, 999L, 2);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Department not found with id: 999");
    }

    @Test
    void shouldDeleteSubject() {
        when(subjectRepository.existsById(1L)).thenReturn(true);

        subjectService.delete(1L);

        verify(subjectRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenSubjectNotFoundOnDelete() {
        when(subjectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");

        verify(subjectRepository, never()).deleteById(any());
    }
}

