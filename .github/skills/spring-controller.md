# Spring Boot REST Controller Skill

You are an expert at creating Spring Boot REST controllers for the College Management System backend. This skill helps you create well-structured, secure REST APIs following project conventions.

## Controller Structure

### Basic REST Controller

```java
package com.cms.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    // Constructor injection (preferred over @Autowired)
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_FACULTY')")
    public ResponseEntity<Page<StudentResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(studentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_STUDENT')")
    public ResponseEntity<StudentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentResponse created = studentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<StudentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## DTO Patterns

### Request DTO (Java Record)

```java
package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentRequest(
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotNull(message = "Department ID is required")
    Long departmentId
) {}
```

### Response DTO (Java Record)

```java
package com.cms.dto;

import java.time.LocalDate;

public record StudentResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    Long departmentId,
    String departmentName,
    LocalDate enrollmentDate
) {
    // Factory method to create from entity
    public static StudentResponse fromEntity(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getFirstName(),
            student.getLastName(),
            student.getEmail(),
            student.getDepartment().getId(),
            student.getDepartment().getName(),
            student.getEnrollmentDate()
        );
    }
}
```

## Role-Based Security

Available roles in the CMS system:

| Role | Description |
|------|-------------|
| `ROLE_ADMIN` | Full system access |
| `ROLE_FACULTY` | Faculty operations |
| `ROLE_STUDENT` | Student access |
| `ROLE_LAB_INCHARGE` | Lab management |
| `ROLE_TECHNICIAN` | Technical support |

### Security Annotations

```java
// Single role required
@PreAuthorize("hasRole('ROLE_ADMIN')")

// Multiple roles (any one)
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_FACULTY')")

// All roles required
@PreAuthorize("hasRole('ROLE_ADMIN') and hasRole('ROLE_FACULTY')")

// Custom SpEL expressions
@PreAuthorize("#id == principal.username or hasRole('ROLE_ADMIN')")

// Access current user
@PreAuthorize("@securityService.isOwner(#id, authentication)")
```

## Error Handling

### Global Exception Handler

```java
package com.cms.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Access denied",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
```

### Custom Exception

```java
package com.cms.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### Error Response Records

```java
package com.cms.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    int status,
    String message,
    LocalDateTime timestamp
) {}

public record ValidationErrorResponse(
    int status,
    String message,
    Map<String, String> errors,
    LocalDateTime timestamp
) {}
```

## Pagination and Sorting

```java
// Controller method with pagination
@GetMapping
public ResponseEntity<Page<StudentResponse>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "lastName") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir) {
    
    Sort sort = sortDir.equalsIgnoreCase("desc")
        ? Sort.by(sortBy).descending()
        : Sort.by(sortBy).ascending();
    
    Pageable pageable = PageRequest.of(page, size, sort);
    return ResponseEntity.ok(studentService.findAll(pageable));
}

// Or use Pageable directly (Spring resolves from query params)
@GetMapping
public ResponseEntity<Page<StudentResponse>> getAll(Pageable pageable) {
    return ResponseEntity.ok(studentService.findAll(pageable));
}
```

## API Documentation (Javadoc)

```java
/**
 * REST controller for managing students.
 * 
 * <p>Provides CRUD operations for student resources. All endpoints require
 * authentication and appropriate role-based authorization.</p>
 */
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    /**
     * Retrieves all students with pagination.
     *
     * @param pageable pagination information (page, size, sort)
     * @return paginated list of students
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_FACULTY')")
    public ResponseEntity<Page<StudentResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(studentService.findAll(pageable));
    }
}
```

## Testing Controllers

```java
package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentService studentService;

    @Test
    void shouldGetStudentById() throws Exception {
        StudentResponse student = new StudentResponse(
            1L, "John", "Doe", "john@example.com", 1L, "Computer Science", null
        );
        when(studentService.findById(1L)).thenReturn(student);

        mockMvc.perform(get("/api/v1/students/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void shouldCreateStudent() throws Exception {
        StudentRequest request = new StudentRequest("Jane", "Doe", "jane@example.com", 1L);
        StudentResponse response = new StudentResponse(
            2L, "Jane", "Doe", "jane@example.com", 1L, "Computer Science", null
        );
        when(studentService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/students")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void shouldReturn403WhenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/students")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
```

## Best Practices

1. **Use constructor injection** for dependencies
2. **Keep controllers thin** - business logic belongs in services
3. **Validate all input** with Bean Validation (`@Valid`)
4. **Use DTOs** - never expose entities directly
5. **Apply security at method level** with `@PreAuthorize`
6. **Return appropriate HTTP status codes**
7. **Handle errors consistently** with `@RestControllerAdvice`
8. **Document APIs** with Javadoc
9. **Write comprehensive tests** for all endpoints
