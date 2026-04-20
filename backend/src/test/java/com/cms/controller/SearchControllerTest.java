package com.cms.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.model.Department;
import com.cms.model.Enquiry;
import com.cms.model.Faculty;
import com.cms.model.Student;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentRepository;

import java.time.LocalDate;

@WebMvcTest(controllers = SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private FacultyRepository facultyRepository;

    @MockitoBean
    private EnquiryRepository enquiryRepository;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    @Test
    void shouldReturnEmptyResultsForBlankQuery() throws Exception {
        mockMvc.perform(get("/search").param("q", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    void shouldReturnEmptyResultsForShortQuery() throws Exception {
        mockMvc.perform(get("/search").param("q", "a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    void shouldSearchStudentsByName() throws Exception {
        Student student = new Student("S001", "John", "Doe", "john@test.com",
            null, 1, LocalDate.now(), StudentStatus.ACTIVE);
        student.setId(1L);

        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(facultyRepository.findAll()).thenReturn(List.of());
        when(enquiryRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/search").param("q", "john"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].type").value("STUDENT"))
            .andExpect(jsonPath("$.results[0].label").value("John Doe"))
            .andExpect(jsonPath("$.results[0].sublabel").value("S001"))
            .andExpect(jsonPath("$.results[0].route").value("/students/1"));
    }

    @Test
    void shouldSearchStudentsByRollNumber() throws Exception {
        Student student = new Student("S001", "Jane", "Smith", "jane@test.com",
            null, 1, LocalDate.now(), StudentStatus.ACTIVE);
        student.setId(2L);

        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(facultyRepository.findAll()).thenReturn(List.of());
        when(enquiryRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/search").param("q", "S001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].type").value("STUDENT"));
    }

    @Test
    void shouldSearchFacultyByName() throws Exception {
        Department dept = new Department("Engineering", "ENG", null, null);
        dept.setId(1L);

        Faculty faculty = new Faculty("E001", "Alice", "Brown", "alice@test.com",
            "9999", dept, com.cms.model.enums.Designation.PROFESSOR, null, null,
            LocalDate.now(), FacultyStatus.ACTIVE);
        faculty.setId(3L);

        when(studentRepository.findAll()).thenReturn(List.of());
        when(facultyRepository.findAll()).thenReturn(List.of(faculty));
        when(enquiryRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/search").param("q", "alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].type").value("FACULTY"))
            .andExpect(jsonPath("$.results[0].label").value("Alice Brown"))
            .andExpect(jsonPath("$.results[0].sublabel").value("Engineering"));
    }

    @Test
    void shouldSearchEnquiriesByName() throws Exception {
        Enquiry enquiry = new Enquiry("Bob Test", "bob@test.com", "1234567890",
            null, LocalDate.now(), null, EnquiryStatus.ENQUIRED);
        enquiry.setId(5L);

        when(studentRepository.findAll()).thenReturn(List.of());
        when(facultyRepository.findAll()).thenReturn(List.of());
        when(enquiryRepository.findAll()).thenReturn(List.of(enquiry));
        when(departmentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/search").param("q", "bob"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].type").value("ENQUIRY"))
            .andExpect(jsonPath("$.results[0].label").value("Bob Test"))
            .andExpect(jsonPath("$.results[0].route").value("/enquiries/5"));
    }

    @Test
    void shouldSearchDepartmentsByName() throws Exception {
        Department dept = new Department("Computer Science", "CS", null, null);
        dept.setId(7L);

        when(studentRepository.findAll()).thenReturn(List.of());
        when(facultyRepository.findAll()).thenReturn(List.of());
        when(enquiryRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAll()).thenReturn(List.of(dept));

        mockMvc.perform(get("/search").param("q", "computer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].type").value("DEPARTMENT"))
            .andExpect(jsonPath("$.results[0].label").value("Computer Science"))
            .andExpect(jsonPath("$.results[0].route").value("/departments/7"));
    }

    @Test
    void shouldRespectLimitParameter() throws Exception {
        List<Student> students = List.of(
            buildStudent(1L, "Alpha", "One", "r1"),
            buildStudent(2L, "Alpha", "Two", "r2"),
            buildStudent(3L, "Alpha", "Three", "r3")
        );

        when(studentRepository.findAll()).thenReturn(students);
        when(facultyRepository.findAll()).thenReturn(List.of());
        when(enquiryRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/search").param("q", "alpha").param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(2));
    }

    @Test
    void shouldCapLimitAtTwenty() throws Exception {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(facultyRepository.findAll()).thenReturn(List.of());
        when(enquiryRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/search").param("q", "xy").param("limit", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isEmpty());
    }

    private Student buildStudent(Long id, String firstName, String lastName, String roll) {
        Student s = new Student(roll, firstName, lastName, firstName + "@test.com",
            null, 1, LocalDate.now(), StudentStatus.ACTIVE);
        s.setId(id);
        return s;
    }
}
