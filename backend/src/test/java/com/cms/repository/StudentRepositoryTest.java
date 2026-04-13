package com.cms.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.cms.model.Department;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.StudentStatus;

@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudentRepository studentRepository;

    private Program testProgram;

    @BeforeEach
    void setUp() {
        Department department = new Department("Computer Science", "CS", "CS Department", "Dr. Smith");
        entityManager.persist(department);

        testProgram = new Program("B.Tech Computer Science", "BTCS", com.cms.model.enums.DegreeType.BACHELOR, 4, department);
        entityManager.persist(testProgram);

        entityManager.flush();
    }

    @Test
    void shouldSaveAndFindStudent() {
        Student student = createStudent("CS2024001", "John", "Doe", "john@college.edu");
        studentRepository.save(student);

        Optional<Student> found = studentRepository.findById(student.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getRollNumber()).isEqualTo("CS2024001");
        assertThat(found.get().getFullName()).isEqualTo("John Doe");
    }

    @Test
    void shouldFindByRollNumber() {
        Student student = createStudent("CS2024002", "Jane", "Smith", "jane@college.edu");
        entityManager.persist(student);
        entityManager.flush();

        Optional<Student> found = studentRepository.findByRollNumber("CS2024002");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("jane@college.edu");
    }

    @Test
    void shouldReturnEmptyWhenRollNumberNotFound() {
        Optional<Student> found = studentRepository.findByRollNumber("NONEXISTENT");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindByEmail() {
        Student student = createStudent("CS2024003", "Bob", "Wilson", "bob@college.edu");
        entityManager.persist(student);
        entityManager.flush();

        Optional<Student> found = studentRepository.findByEmail("bob@college.edu");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Bob");
    }

    @Test
    void shouldCheckExistsByRollNumber() {
        Student student = createStudent("CS2024004", "Alice", "Brown", "alice@college.edu");
        entityManager.persist(student);
        entityManager.flush();

        assertThat(studentRepository.existsByRollNumber("CS2024004")).isTrue();
        assertThat(studentRepository.existsByRollNumber("NONEXISTENT")).isFalse();
    }

    @Test
    void shouldCheckExistsByEmail() {
        Student student = createStudent("CS2024005", "Charlie", "Davis", "charlie@college.edu");
        entityManager.persist(student);
        entityManager.flush();

        assertThat(studentRepository.existsByEmail("charlie@college.edu")).isTrue();
        assertThat(studentRepository.existsByEmail("nonexistent@college.edu")).isFalse();
    }

    @Test
    void shouldFindByProgramId() {
        Student student1 = createStudent("CS2024006", "Eve", "Fisher", "eve@college.edu");
        Student student2 = createStudent("CS2024007", "Frank", "Garcia", "frank@college.edu");
        entityManager.persist(student1);
        entityManager.persist(student2);
        entityManager.flush();

        List<Student> students = studentRepository.findByProgramId(testProgram.getId());

        assertThat(students).hasSize(2);
    }

    @Test
    void shouldFindByStatus() {
        Student activeStudent = createStudent("CS2024008", "Grace", "Hall", "grace@college.edu");
        Student graduatedStudent = createStudent("CS2024009", "Henry", "King", "henry@college.edu");
        graduatedStudent.setStatus(StudentStatus.GRADUATED);
        entityManager.persist(activeStudent);
        entityManager.persist(graduatedStudent);
        entityManager.flush();

        List<Student> activeStudents = studentRepository.findByStatus(StudentStatus.ACTIVE);
        List<Student> graduatedStudents = studentRepository.findByStatus(StudentStatus.GRADUATED);

        assertThat(activeStudents).hasSize(1);
        assertThat(activeStudents.get(0).getFirstName()).isEqualTo("Grace");
        assertThat(graduatedStudents).hasSize(1);
        assertThat(graduatedStudents.get(0).getFirstName()).isEqualTo("Henry");
    }

    @Test
    void shouldFindByLabBatch() {
        Student student1 = createStudent("CS2024010", "Ivy", "Lee", "ivy@college.edu");
        student1.setLabBatch("Batch-A");
        Student student2 = createStudent("CS2024011", "Jack", "Miller", "jack@college.edu");
        student2.setLabBatch("Batch-A");
        Student student3 = createStudent("CS2024012", "Kate", "Nelson", "kate@college.edu");
        student3.setLabBatch("Batch-B");
        entityManager.persist(student1);
        entityManager.persist(student2);
        entityManager.persist(student3);
        entityManager.flush();

        List<Student> batchA = studentRepository.findByLabBatch("Batch-A");
        List<Student> batchB = studentRepository.findByLabBatch("Batch-B");

        assertThat(batchA).hasSize(2);
        assertThat(batchB).hasSize(1);
    }

    @Test
    void shouldFindByProgramIdAndSemester() {
        Student student1 = createStudent("CS2024013", "Leo", "Owen", "leo@college.edu");
        student1.setSemester(1);
        Student student2 = createStudent("CS2024014", "Mia", "Parker", "mia@college.edu");
        student2.setSemester(2);
        entityManager.persist(student1);
        entityManager.persist(student2);
        entityManager.flush();

        List<Student> sem1Students = studentRepository.findByProgramIdAndSemester(testProgram.getId(), 1);
        List<Student> sem2Students = studentRepository.findByProgramIdAndSemester(testProgram.getId(), 2);

        assertThat(sem1Students).hasSize(1);
        assertThat(sem1Students.get(0).getFirstName()).isEqualTo("Leo");
        assertThat(sem2Students).hasSize(1);
        assertThat(sem2Students.get(0).getFirstName()).isEqualTo("Mia");
    }

    private Student createStudent(String rollNumber, String firstName, String lastName, String email) {
        return new Student(
            rollNumber, firstName, lastName, email,
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE
        );
    }
}
