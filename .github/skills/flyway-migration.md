# Flyway Database Migration Skill

You are an expert at creating Flyway database migrations for the College Management System backend. This skill helps you create consistent, safe database migrations.

## Migration File Naming

Flyway migrations are located in `backend/src/main/resources/db/migration/`.

### Naming Convention

```
V{version}__{description}.sql
```

- `V` - Version prefix (capital V)
- `{version}` - Version number (e.g., `1`, `2`, `1.1`, `1.2.3`)
- `__` - Double underscore separator
- `{description}` - Snake_case description
- `.sql` - SQL file extension

### Examples

```
V1__create_departments_table.sql
V2__create_students_table.sql
V3__add_status_to_students.sql
V4__create_courses_table.sql
V5__add_indexes_to_students.sql
```

## Table Creation Pattern

### Basic Table

```sql
-- V1__create_departments_table.sql
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_departments_name UNIQUE (name),
    CONSTRAINT uq_departments_code UNIQUE (code)
);

-- Add comment for documentation
COMMENT ON TABLE departments IS 'Academic departments within the college';
COMMENT ON COLUMN departments.code IS 'Short code for the department (e.g., CS, MATH)';
```

### Table with Foreign Key

```sql
-- V2__create_students_table.sql
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    enrollment_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_students_email UNIQUE (email),
    CONSTRAINT fk_students_department 
        FOREIGN KEY (department_id) 
        REFERENCES departments(id)
        ON DELETE RESTRICT
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_students_department ON students(department_id);
CREATE INDEX idx_students_email ON students(email);
CREATE INDEX idx_students_status ON students(status);

COMMENT ON TABLE students IS 'Enrolled students in the college';
```

### Many-to-Many Relationship

```sql
-- V6__create_course_enrollments_table.sql
CREATE TABLE course_enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    grade VARCHAR(2),
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    
    CONSTRAINT fk_enrollments_student 
        FOREIGN KEY (student_id) 
        REFERENCES students(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_enrollments_course 
        FOREIGN KEY (course_id) 
        REFERENCES courses(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_enrollment UNIQUE (student_id, course_id)
);

CREATE INDEX idx_enrollments_student ON course_enrollments(student_id);
CREATE INDEX idx_enrollments_course ON course_enrollments(course_id);
```

## Column Modifications

### Add Column

```sql
-- V7__add_phone_to_students.sql
ALTER TABLE students
    ADD COLUMN phone VARCHAR(20);

-- With default value (for NOT NULL columns on existing data)
ALTER TABLE students
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;
```

### Modify Column

```sql
-- V8__expand_email_column.sql
ALTER TABLE students
    ALTER COLUMN email TYPE VARCHAR(150);
```

### Rename Column

```sql
-- V9__rename_enrollment_date.sql
ALTER TABLE students
    RENAME COLUMN enrollment_date TO enrolled_at;
```

### Drop Column

```sql
-- V10__remove_deprecated_field.sql
ALTER TABLE students
    DROP COLUMN IF EXISTS deprecated_field;
```

## Index Management

### Create Index

```sql
-- V11__add_performance_indexes.sql

-- Standard B-tree index
CREATE INDEX idx_students_last_name ON students(last_name);

-- Composite index
CREATE INDEX idx_students_name ON students(last_name, first_name);

-- Partial index (for active records only)
CREATE INDEX idx_active_students ON students(department_id)
    WHERE status = 'ACTIVE';

-- Index for text search
CREATE INDEX idx_students_email_pattern ON students(email varchar_pattern_ops);
```

### Drop Index

```sql
-- V12__remove_unused_index.sql
DROP INDEX IF EXISTS idx_old_unused_index;
```

## Enum Types

### Create Enum

```sql
-- V13__create_status_enum.sql
CREATE TYPE student_status AS ENUM ('ACTIVE', 'INACTIVE', 'GRADUATED', 'SUSPENDED');

-- Use in table
ALTER TABLE students
    ALTER COLUMN status TYPE student_status
    USING status::student_status;
```

### Add Value to Enum

```sql
-- V14__add_transferred_status.sql
ALTER TYPE student_status ADD VALUE 'TRANSFERRED';
```

## Data Migrations

### Insert Reference Data

```sql
-- V15__seed_departments.sql
INSERT INTO departments (name, code, description) VALUES
    ('Computer Science', 'CS', 'Computer Science and Engineering'),
    ('Mathematics', 'MATH', 'Mathematics Department'),
    ('Physics', 'PHY', 'Physics Department'),
    ('Chemistry', 'CHEM', 'Chemistry Department'),
    ('Biology', 'BIO', 'Biology Department')
ON CONFLICT (code) DO NOTHING;
```

### Update Existing Data

```sql
-- V16__normalize_email_addresses.sql
UPDATE students
SET email = LOWER(email)
WHERE email <> LOWER(email);
```

### Data Transformation

```sql
-- V17__split_full_name.sql
-- First add new columns
ALTER TABLE faculty
    ADD COLUMN first_name VARCHAR(50),
    ADD COLUMN last_name VARCHAR(50);

-- Then migrate data
UPDATE faculty
SET 
    first_name = SPLIT_PART(full_name, ' ', 1),
    last_name = SUBSTRING(full_name FROM POSITION(' ' IN full_name) + 1);

-- Make columns NOT NULL after data is populated
ALTER TABLE faculty
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;

-- Finally drop old column
ALTER TABLE faculty
    DROP COLUMN full_name;
```

## Constraints

### Add Constraint

```sql
-- V18__add_constraints.sql

-- Check constraint
ALTER TABLE students
    ADD CONSTRAINT chk_student_email_format
    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Foreign key constraint
ALTER TABLE courses
    ADD CONSTRAINT fk_courses_department
    FOREIGN KEY (department_id)
    REFERENCES departments(id);
```

### Drop Constraint

```sql
-- V19__remove_constraint.sql
ALTER TABLE students
    DROP CONSTRAINT IF EXISTS chk_old_constraint;
```

## Views

### Create View

```sql
-- V20__create_student_summary_view.sql
CREATE OR REPLACE VIEW student_summary AS
SELECT 
    s.id,
    s.first_name,
    s.last_name,
    s.email,
    d.name AS department_name,
    d.code AS department_code,
    s.status,
    s.enrollment_date,
    COUNT(ce.id) AS enrolled_courses
FROM students s
JOIN departments d ON s.department_id = d.id
LEFT JOIN course_enrollments ce ON s.id = ce.student_id
GROUP BY s.id, d.id;

COMMENT ON VIEW student_summary IS 'Summary view of students with department and enrollment count';
```

## Stored Functions

```sql
-- V21__create_audit_function.sql
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to tables
CREATE TRIGGER trg_students_updated_at
    BEFORE UPDATE ON students
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_departments_updated_at
    BEFORE UPDATE ON departments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

## Rollback Migrations (Undo)

Create undo scripts in `db/migration/undo/` (if needed):

```sql
-- U20__drop_student_summary_view.sql
DROP VIEW IF EXISTS student_summary;
```

## Best Practices

1. **Never modify existing migrations** - Create new migrations for changes
2. **Test migrations locally** before committing
3. **Include rollback scripts** for critical changes
4. **Use transactions** (Flyway wraps each migration in a transaction by default)
5. **Add comments** to complex schemas
6. **Use IF EXISTS/IF NOT EXISTS** for idempotent operations
7. **Consider data size** when modifying large tables
8. **Create indexes concurrently** for large tables in production:
   ```sql
   CREATE INDEX CONCURRENTLY idx_name ON table_name(column);
   ```
9. **Validate constraints** before making columns NOT NULL
10. **Document breaking changes** in migration comments

## Running Migrations

```bash
cd backend

# Run migrations (happens automatically on app start)
./gradlew bootRun

# Or use Flyway CLI directly (if installed)
flyway -url=jdbc:postgresql://localhost:5432/cms_db \
       -user=cms_user \
       -password=cms_password \
       migrate

# Validate migrations
./gradlew flywayValidate

# View migration info
./gradlew flywayInfo
```
