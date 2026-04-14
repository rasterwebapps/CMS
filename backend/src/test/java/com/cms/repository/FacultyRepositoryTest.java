package com.cms.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.cms.config.JpaConfig;
import com.cms.model.Department;
import com.cms.model.Faculty;
import com.cms.model.enums.Designation;
import com.cms.model.enums.FacultyStatus;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class FacultyRepositoryTest {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department testDepartment;

    @BeforeEach
    void setUp() {
        facultyRepository.deleteAll();
        departmentRepository.deleteAll();
        testDepartment = departmentRepository.save(
            new Department("Computer Science", "CS", "CS Dept", "Dr. HOD")
        );
    }

    @Test
    void shouldSaveFaculty() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");

        Faculty saved = facultyRepository.save(faculty);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmployeeCode()).isEqualTo("EMP001");
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getFullName()).isEqualTo("John Doe");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindFacultyById() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        Faculty saved = facultyRepository.save(faculty);

        Optional<Faculty> found = facultyRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmployeeCode()).isEqualTo("EMP001");
    }

    @Test
    void shouldFindFacultyByEmployeeCode() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        facultyRepository.save(faculty);

        Optional<Faculty> found = facultyRepository.findByEmployeeCode("EMP001");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldReturnEmptyWhenFindByEmployeeCodeNotExists() {
        Optional<Faculty> found = facultyRepository.findByEmployeeCode("NONEXISTENT");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindFacultyByEmail() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        facultyRepository.save(faculty);

        Optional<Faculty> found = facultyRepository.findByEmail("john@college.edu");

        assertThat(found).isPresent();
        assertThat(found.get().getEmployeeCode()).isEqualTo("EMP001");
    }

    @Test
    void shouldCheckIfFacultyExistsByEmployeeCode() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        facultyRepository.save(faculty);

        boolean exists = facultyRepository.existsByEmployeeCode("EMP001");
        boolean notExists = facultyRepository.existsByEmployeeCode("NONEXISTENT");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCheckIfFacultyExistsByEmail() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        facultyRepository.save(faculty);

        boolean exists = facultyRepository.existsByEmail("john@college.edu");
        boolean notExists = facultyRepository.existsByEmail("nonexistent@college.edu");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldFindFacultyByDepartmentId() {
        Faculty faculty1 = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        Faculty faculty2 = createFaculty("EMP002", "Jane", "Smith", "jane@college.edu");
        facultyRepository.save(faculty1);
        facultyRepository.save(faculty2);

        List<Faculty> facultyList = facultyRepository.findByDepartmentId(testDepartment.getId());

        assertThat(facultyList).hasSize(2);
    }

    @Test
    void shouldFindFacultyByStatus() {
        Faculty activeFaculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        activeFaculty.setStatus(FacultyStatus.ACTIVE);
        
        Faculty onLeaveFaculty = createFaculty("EMP002", "Jane", "Smith", "jane@college.edu");
        onLeaveFaculty.setStatus(FacultyStatus.ON_LEAVE);
        
        facultyRepository.save(activeFaculty);
        facultyRepository.save(onLeaveFaculty);

        List<Faculty> activeFacultyList = facultyRepository.findByStatus(FacultyStatus.ACTIVE);
        List<Faculty> onLeaveFacultyList = facultyRepository.findByStatus(FacultyStatus.ON_LEAVE);

        assertThat(activeFacultyList).hasSize(1);
        assertThat(activeFacultyList.get(0).getEmployeeCode()).isEqualTo("EMP001");
        assertThat(onLeaveFacultyList).hasSize(1);
        assertThat(onLeaveFacultyList.get(0).getEmployeeCode()).isEqualTo("EMP002");
    }

    @Test
    void shouldFindFacultyByDepartmentIdAndStatus() {
        Faculty activeFaculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        activeFaculty.setStatus(FacultyStatus.ACTIVE);
        
        Faculty onLeaveFaculty = createFaculty("EMP002", "Jane", "Smith", "jane@college.edu");
        onLeaveFaculty.setStatus(FacultyStatus.ON_LEAVE);
        
        facultyRepository.save(activeFaculty);
        facultyRepository.save(onLeaveFaculty);

        List<Faculty> facultyList = facultyRepository.findByDepartmentIdAndStatus(
            testDepartment.getId(), FacultyStatus.ACTIVE
        );

        assertThat(facultyList).hasSize(1);
        assertThat(facultyList.get(0).getEmployeeCode()).isEqualTo("EMP001");
    }

    @Test
    void shouldFindAllFaculty() {
        Faculty faculty1 = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        Faculty faculty2 = createFaculty("EMP002", "Jane", "Smith", "jane@college.edu");
        facultyRepository.save(faculty1);
        facultyRepository.save(faculty2);

        List<Faculty> facultyList = facultyRepository.findAll();

        assertThat(facultyList).hasSize(2);
    }

    @Test
    void shouldUpdateFaculty() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        Faculty saved = facultyRepository.save(faculty);

        saved.setFirstName("Johnny");
        saved.setDesignation(Designation.ASSOCIATE_PROFESSOR);
        Faculty updated = facultyRepository.save(saved);

        assertThat(updated.getFirstName()).isEqualTo("Johnny");
        assertThat(updated.getDesignation()).isEqualTo(Designation.ASSOCIATE_PROFESSOR);
    }

    @Test
    void shouldDeleteFaculty() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");
        Faculty saved = facultyRepository.save(faculty);
        Long id = saved.getId();

        facultyRepository.deleteById(id);

        Optional<Faculty> found = facultyRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnSave() {
        Faculty faculty = createFaculty("EMP001", "John", "Doe", "john@college.edu");

        Faculty saved = facultyRepository.save(faculty);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    private Faculty createFaculty(String employeeCode, String firstName, String lastName, String email) {
        return new Faculty(
            employeeCode,
            firstName,
            lastName,
            email,
            "1234567890",
            testDepartment,
            Designation.PROFESSOR,
            "Artificial Intelligence",
            "Machine Learning Lab",
            LocalDate.of(2020, 1, 15),
            FacultyStatus.ACTIVE
        );
    }
}
