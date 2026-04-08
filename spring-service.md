# Spring Boot Service Skill

You are an expert at creating Spring Boot services for the College Management System backend. This skill helps you create well-structured service classes following project conventions.

## Service Structure

### Basic Service Class

```java
package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Student;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;

    // Constructor injection
    public StudentService(
            StudentRepository studentRepository,
            DepartmentRepository departmentRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
    }

    public Page<StudentResponse> findAll(Pageable pageable) {
        return studentRepository.findAll(pageable)
            .map(StudentResponse::fromEntity);
    }

    public StudentResponse findById(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        return StudentResponse.fromEntity(student);
    }

    public List<StudentResponse> findByDepartment(Long departmentId) {
        return studentRepository.findByDepartmentId(departmentId).stream()
            .map(StudentResponse::fromEntity)
            .toList();
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        // Validate department exists
        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));

        // Check for duplicate email
        if (studentRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Student with email already exists: " + request.email());
        }

        Student student = new Student();
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setDepartment(department);
        student.setEnrollmentDate(LocalDate.now());

        Student saved = studentRepository.save(student);
        return StudentResponse.fromEntity(saved);
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        // Validate department if changed
        if (!student.getDepartment().getId().equals(request.departmentId())) {
            Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
            student.setDepartment(department);
        }

        // Check for duplicate email (excluding current student)
        if (!student.getEmail().equals(request.email()) 
                && studentRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Student with email already exists: " + request.email());
        }

        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());

        Student saved = studentRepository.save(student);
        return StudentResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student", "id", id);
        }
        studentRepository.deleteById(id);
    }
}
```

## Repository Patterns

### Basic JPA Repository

```java
package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // Query methods using method naming
    List<Student> findByDepartmentId(Long departmentId);
    
    Optional<Student> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<Student> findByLastNameContainingIgnoreCase(String lastName);

    // Custom JPQL query
    @Query("SELECT s FROM Student s WHERE s.department.name = :departmentName")
    List<Student> findByDepartmentName(@Param("departmentName") String departmentName);

    // Native SQL query
    @Query(value = "SELECT * FROM students WHERE enrollment_date >= :date", nativeQuery = true)
    List<Student> findRecentEnrollments(@Param("date") LocalDate date);

    // Query with projection
    @Query("SELECT s.email FROM Student s WHERE s.department.id = :deptId")
    List<String> findEmailsByDepartment(@Param("deptId") Long departmentId);
}
```

## Entity Patterns

### JPA Entity

```java
package com.cms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### Entity with Enum

```java
package com.cms.model;

import com.cms.model.enums.StudentStatus;

@Entity
@Table(name = "students")
public class Student {
    // ... other fields

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    // getter/setter
}
```

```java
package com.cms.model.enums;

public enum StudentStatus {
    ACTIVE,
    INACTIVE,
    GRADUATED,
    SUSPENDED
}
```

## Flyway Migration Pattern

Create migrations in `src/main/resources/db/migration/`:

```sql
-- V1__create_departments_table.sql
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- V2__create_students_table.sql
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department_id BIGINT NOT NULL REFERENCES departments(id),
    enrollment_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_students_department ON students(department_id);
CREATE INDEX idx_students_email ON students(email);
```

## Transaction Management

```java
@Service
@Transactional(readOnly = true)  // Default read-only for all methods
public class StudentService {

    // Read operations use class-level annotation (readOnly = true)
    public StudentResponse findById(Long id) { ... }

    // Write operations override with @Transactional
    @Transactional
    public StudentResponse create(StudentRequest request) { ... }

    // Complex transaction with rollback rules
    @Transactional(rollbackFor = Exception.class)
    public void complexOperation() { ... }

    // Multiple operations in single transaction
    @Transactional
    public void batchEnroll(List<StudentRequest> requests) {
        for (StudentRequest request : requests) {
            create(request);  // All succeed or all fail
        }
    }
}
```

## Testing Services

```java
package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Student;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private StudentService studentService;

    private Department department;
    private Student student;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        student = new Student();
        student.setId(1L);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEmail("john@example.com");
        student.setDepartment(department);
    }

    @Test
    void shouldFindStudentById() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        StudentResponse response = studentService.findById(1L);

        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.departmentName()).isEqualTo("Computer Science");
    }

    @Test
    void shouldThrowWhenStudentNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Student not found");
    }

    @Test
    void shouldCreateStudent() {
        StudentRequest request = new StudentRequest("Jane", "Doe", "jane@example.com", 1L);
        
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(studentRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(studentRepository.save(any())).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            s.setId(2L);
            return s;
        });

        StudentResponse response = studentService.create(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.firstName()).isEqualTo("Jane");
        verify(studentRepository).save(any(Student.class));
    }
}
```

## Integration Testing

```java
package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;

@SpringBootTest
@ActiveProfiles("test")
@Transactional  // Rollback after each test
class StudentServiceIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Test
    void shouldCreateAndRetrieveStudent() {
        StudentRequest request = new StudentRequest("Test", "User", "test@example.com", 1L);
        
        StudentResponse created = studentService.create(request);
        StudentResponse retrieved = studentService.findById(created.id());

        assertThat(retrieved.email()).isEqualTo("test@example.com");
    }
}
```

## Best Practices

1. **Use constructor injection** for all dependencies
2. **Apply `@Transactional(readOnly = true)` at class level** for services
3. **Override with `@Transactional`** for write operations
4. **Validate business rules in services**, not controllers
5. **Use DTOs** - never expose entities
6. **Handle exceptions consistently** with custom exceptions
7. **Use `Optional` returns** from repositories properly
8. **Write unit tests** with mocks for all service methods
9. **Write integration tests** for complex business logic
10. **Keep services focused** on single responsibility
