package com.cms.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.cms.model.Department;

@DataJpaTest
@ActiveProfiles("test")
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAll();
    }

    @Test
    void shouldSaveDepartment() {
        Department department = new Department(
            "Computer Science",
            "CS",
            "Department of Computer Science",
            "Dr. John Doe"
        );

        Department saved = departmentRepository.save(department);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Computer Science");
        assertThat(saved.getCode()).isEqualTo("CS");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindDepartmentById() {
        Department department = new Department(
            "Mathematics",
            "MATH",
            "Department of Mathematics",
            "Dr. Jane Smith"
        );
        Department saved = departmentRepository.save(department);

        Optional<Department> found = departmentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Mathematics");
    }

    @Test
    void shouldFindDepartmentByCode() {
        Department department = new Department(
            "Physics",
            "PHY",
            "Department of Physics",
            "Dr. Einstein"
        );
        departmentRepository.save(department);

        Optional<Department> found = departmentRepository.findByCode("PHY");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Physics");
    }

    @Test
    void shouldReturnEmptyWhenFindByCodeNotExists() {
        Optional<Department> found = departmentRepository.findByCode("NONEXISTENT");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckIfDepartmentExistsByCode() {
        Department department = new Department(
            "Chemistry",
            "CHEM",
            "Department of Chemistry",
            "Dr. Curie"
        );
        departmentRepository.save(department);

        boolean exists = departmentRepository.existsByCode("CHEM");
        boolean notExists = departmentRepository.existsByCode("NONEXISTENT");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldFindAllDepartments() {
        Department dept1 = new Department("CS", "CS", "CS Dept", "Dr. A");
        Department dept2 = new Department("Math", "MATH", "Math Dept", "Dr. B");
        departmentRepository.save(dept1);
        departmentRepository.save(dept2);

        List<Department> departments = departmentRepository.findAll();

        assertThat(departments).hasSize(2);
    }

    @Test
    void shouldUpdateDepartment() {
        Department department = new Department(
            "Biology",
            "BIO",
            "Department of Biology",
            "Dr. Darwin"
        );
        Department saved = departmentRepository.save(department);

        saved.setName("Life Sciences");
        saved.setHodName("Dr. Updated");
        Department updated = departmentRepository.save(saved);

        assertThat(updated.getName()).isEqualTo("Life Sciences");
        assertThat(updated.getHodName()).isEqualTo("Dr. Updated");
    }

    @Test
    void shouldDeleteDepartment() {
        Department department = new Department(
            "History",
            "HIST",
            "Department of History",
            "Dr. Historian"
        );
        Department saved = departmentRepository.save(department);
        Long id = saved.getId();

        departmentRepository.deleteById(id);

        Optional<Department> found = departmentRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenDepartmentExistsById() {
        Department department = new Department(
            "Geography",
            "GEO",
            "Department of Geography",
            "Dr. Geo"
        );
        Department saved = departmentRepository.save(department);

        boolean exists = departmentRepository.existsById(saved.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDepartmentNotExistsById() {
        boolean exists = departmentRepository.existsById(999L);

        assertThat(exists).isFalse();
    }

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnSave() {
        Department department = new Department(
            "Art",
            "ART",
            "Department of Art",
            "Dr. Artist"
        );

        Department saved = departmentRepository.save(department);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }
}
