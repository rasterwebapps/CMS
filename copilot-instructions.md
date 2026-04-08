# College Management System - GitHub Copilot Instructions

This document provides context and guidelines for GitHub Copilot when working on the College Management System (CMS) codebase.

## Project Overview

The College Management System is a full-stack web application for managing college operations including students, faculty, departments, courses, and lab resources. The system uses a modern tech stack with Angular on the frontend and Spring Boot on the backend.

## Technology Stack

### Frontend (Angular 21)
- **Framework**: Angular 21 with standalone components
- **UI Library**: Angular Material 21 with Material Design 3
- **Language**: TypeScript 5.9 with strict mode enabled
- **Styling**: SCSS
- **Build Tool**: Angular CLI with `@angular/build`
- **Testing**: Vitest for unit tests
- **Code Formatting**: Prettier (single quotes, 100 char width)
- **Authentication**: Keycloak JS SDK for OAuth2/OIDC

### Backend (Spring Boot 3.4)
- **Framework**: Spring Boot 3.4.5
- **Language**: Java 21 with virtual threads enabled
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: H2 (in-memory) for local development, PostgreSQL 16 for production/other environments
- **Database Migrations**: Flyway (enabled for PostgreSQL profiles, disabled for local/H2)
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security with OAuth2 Resource Server (JWT)
- **Authentication Provider**: Keycloak 26.0
- **Code Coverage**: JaCoCo with 95% minimum coverage enforcement
- **Testing**: JUnit 5 + Spring Boot Test (backend only — no frontend unit tests required)

### Infrastructure
- **Authentication**: Keycloak 26.0 (realm: `cms`)
- **Database (Production)**: PostgreSQL 16
- **Database (Local Development)**: H2 in-memory
- **Container Orchestration**: Docker Compose

## Project Structure

```
CollegeManagementSystem/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/com/cms/
│   │   ├── config/            # Configuration classes (Security, etc.)
│   │   ├── controller/        # REST controllers
│   │   ├── service/           # Business logic services
│   │   ├── repository/        # JPA repositories
│   │   ├── model/             # JPA entities
│   │   │   └── enums/         # Enum types
│   │   ├── dto/               # Data Transfer Objects (Java records)
│   │   └── exception/         # Custom exceptions
│   └── src/main/resources/
│       ├── application.yml          # Common configuration (default profile: local)
│       ├── application-local.yml    # H2 in-memory database for local development
│       └── db/migration/            # Flyway SQL migrations (PostgreSQL)
├── frontend/                   # Angular frontend
│   └── src/app/
│       ├── core/              # Core services, guards, interceptors
│       │   ├── auth/          # Authentication service
│       │   ├── guards/        # Route guards
│       │   └── interceptors/  # HTTP interceptors
│       ├── features/          # Feature modules (dashboard, etc.)
│       └── shared/            # Shared components
├── infrastructure/             # Infrastructure configs
│   └── keycloak/              # Keycloak realm configuration
└── docker-compose.yml          # Local development setup
```

## Coding Conventions

### Backend (Java/Spring Boot)

1. **DTOs**: Use Java records for all DTOs in `com.cms.dto`
   ```java
   public record StudentRequest(
       @NotBlank String firstName,
       @NotBlank String lastName,
       @Email String email
   ) {}
   ```

2. **Entities**: Place JPA entities in `com.cms.model` with proper annotations
   ```java
   @Entity
   @Table(name = "students")
   public class Student {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       // ...
   }
   ```

3. **Repositories**: Use JpaRepository interfaces in `com.cms.repository`
   ```java
   public interface StudentRepository extends JpaRepository<Student, Long> {
       Optional<Student> findByEmail(String email);
   }
   ```

4. **Controllers**: Follow REST conventions with `/api/v1` prefix
   ```java
   @RestController
   @RequestMapping("/api/v1/students")
   public class StudentController {
       // Use constructor injection
   }
   ```

5. **Security**: Use method-level security with `@PreAuthorize`
   ```java
   @PreAuthorize("hasRole('ROLE_ADMIN')")
   @PostMapping
   public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
       // ...
   }
   ```

6. **Database Migrations**: Create Flyway migrations in `src/main/resources/db/migration`
   - Naming: `V{version}__{description}.sql` (e.g., `V1__create_students_table.sql`)

### Frontend (Angular/TypeScript)

1. **Components**: Use standalone components with explicit imports
   ```typescript
   @Component({
     selector: 'app-student-list',
     standalone: true,
     imports: [MatTable, MatPaginator, ...],
     templateUrl: './student-list.html',
     styleUrl: './student-list.scss',
   })
   export class StudentListComponent {
     // Use inject() function over constructor injection
     private readonly studentService = inject(StudentService);
   }
   ```

2. **Services**: Use signals for reactive state management
   ```typescript
   @Injectable({ providedIn: 'root' })
   export class StudentService {
     private readonly _students = signal<Student[]>([]);
     readonly students = this._students.asReadonly();
   }
   ```

3. **HTTP Calls**: Use HttpClient with typed responses
   ```typescript
   getStudents(): Observable<Student[]> {
     return this.http.get<Student[]>(`${environment.apiUrl}/students`);
   }
   ```

4. **Templates**: Use separate `.html` template files (not inline templates)

5. **Styling**: Use SCSS with component-scoped styles

6. **Forms**: Use reactive forms with validators
   ```typescript
   this.form = this.fb.group({
     firstName: ['', [Validators.required]],
     email: ['', [Validators.required, Validators.email]],
   });
   ```

## Authentication & Authorization

### User Roles
- `ROLE_ADMIN` - Full system access
- `ROLE_FACULTY` - Faculty-specific operations
- `ROLE_STUDENT` - Student-specific access
- `ROLE_LAB_INCHARGE` - Lab management access
- `ROLE_TECHNICIAN` - Technical support access

### Frontend Authentication
- Keycloak JS handles authentication flow
- `AuthService` provides user profile and role information
- `authInterceptor` attaches JWT tokens to API requests
- Route guards protect authenticated routes

### Backend Security
- JWT tokens validated against Keycloak issuer
- Roles extracted from `realm_access.roles` claim
- Method-level security with `@PreAuthorize`

## Build & Test Commands

### Backend
```bash
cd backend

# Build
./gradlew compileJava

# Run tests
./gradlew test

# Run tests with coverage report (HTML report at build/reports/jacoco/test/html/)
./gradlew test jacocoTestReport

# Run full check (tests + 95% coverage verification)
./gradlew check

# Run application (defaults to 'local' profile with H2)
./gradlew bootRun

# Run application with PostgreSQL (production profile)
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

### Frontend
```bash
cd frontend

# Install dependencies
npm install

# Build
npm run build
# or: npx ng build

# Development server
npm run start
```

> **Note:** Frontend unit tests are not required for this project. No `npm run test` is expected.

### Docker Compose (Local Development)
```bash
# Start all services (PostgreSQL, Keycloak) — needed only for non-local profiles
docker compose up -d

# Stop services
docker compose down
```

## Database Profiles

The backend supports multiple Spring profiles for database configuration:

| Profile | Database | Flyway | Use Case |
|---------|----------|--------|----------|
| `local` (default) | H2 in-memory | Disabled | Local development — no external dependencies needed |
| `prod` / others | PostgreSQL 16 | Enabled | Production, staging, CI — requires Docker Compose or external PostgreSQL |

- **Local development**: Run `./gradlew bootRun` — uses H2 in-memory database with `ddl-auto: create-drop`. The H2 console is available at `http://localhost:8080/h2-console`.
- **Production/other environments**: Set `SPRING_PROFILES_ACTIVE=prod` (or omit the `local` profile) — uses PostgreSQL with Flyway migrations.

## Testing & Code Coverage

### Backend Testing Requirements
- **Minimum code coverage: 95%** — enforced by JaCoCo via `./gradlew check`
- All new services, controllers, and repositories **must** have corresponding unit tests
- Use `@SpringBootTest` with `@ActiveProfiles("test")` for integration tests
- Use `@WebMvcTest` for controller-layer unit tests
- Use `@DataJpaTest` for repository-layer tests
- Coverage reports are generated at `backend/build/reports/jacoco/test/html/`

### Frontend Testing
- **No frontend unit tests are required** for this project
- Frontend testing tooling (Vitest) is available but not enforced

## Manual Test Cases

**Every completed task in this project must include manual test cases.** When a task is finished, the developer must create or update manual test case documentation to verify the feature works as expected.

### Manual Test Case Guidelines

1. **When to create**: After completing any backend or frontend task from the milestone tracker
2. **Where to document**: Create a markdown file in `docs/manual-test-cases/` named after the module or feature (e.g., `docs/manual-test-cases/department-management.md`)
3. **Format**: Each test case must include:
   - **Test Case ID**: Unique identifier (e.g., `TC-DEPT-001`)
   - **Title**: Short description of what is being tested
   - **Preconditions**: Setup required before testing
   - **Steps**: Numbered step-by-step instructions
   - **Expected Result**: What the correct behavior should be
   - **Actual Result**: To be filled during testing (leave blank in documentation)
   - **Status**: PASS / FAIL / NOT TESTED

### Manual Test Case Template

```markdown
## TC-{MODULE}-{NUMBER}: {Title}

**Preconditions:**
- {List any required setup}

**Steps:**
1. {Step 1}
2. {Step 2}
3. {Step 3}

**Expected Result:**
- {What should happen}

**Status:** NOT TESTED
```

### Example

```markdown
## TC-DEPT-001: Create a new department

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Send a POST request to `/api/v1/departments` with body: `{"name": "Computer Science", "code": "CS"}`
2. Verify the response status is 201 Created
3. Send a GET request to `/api/v1/departments`
4. Verify the new department appears in the list

**Expected Result:**
- Department is created successfully and returned in the list

**Status:** NOT TESTED
```

## API Conventions

- Base path: `/api/v1`
- Use standard HTTP methods: GET, POST, PUT, DELETE
- Return appropriate HTTP status codes
- Use pagination for list endpoints
- Validate request bodies with Bean Validation (`@Valid`)

## Best Practices

1. **Prefer composition over inheritance**
2. **Use constructor injection for dependencies (backend)** and `inject()` function (frontend)
3. **Write unit tests for all backend services and controllers** — maintain 95% code coverage
4. **No frontend unit tests** — frontend testing is not required
5. **Create manual test cases** for every completed task (see [Manual Test Cases](#manual-test-cases))
6. **Keep controllers thin** - business logic belongs in services
7. **Use meaningful variable and method names**
8. **Document public APIs with Javadoc (backend)** and JSDoc (frontend)
9. **Handle errors gracefully** with appropriate error responses
10. **Follow existing patterns** in the codebase when adding new features
11. **Use the `local` profile** for development (H2) and `prod` for production (PostgreSQL)
