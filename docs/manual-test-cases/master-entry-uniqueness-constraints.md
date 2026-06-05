# Manual Test Cases — Master Entry Uniqueness Constraints (V109)

Module: Database Migration V109 — Master Entry Uniqueness Enforcement
Related: `backend/src/main/resources/db/migration/V109__enforce_master_entry_uniqueness.sql`

---

## Overview

Migration V109 enforces database-level uniqueness constraints on master entity names and codes using case-insensitive, whitespace-trimmed functional indexes. This prevents duplicate entries even if circumventing the service-layer validation.

**Test Environment Requirements:**
- PostgreSQL 17 database (not H2)
- Backend running with `prod` or non-`local` profile
- Flyway migrations enabled
- Migration V109 successfully applied

---

## TC-V109-001: Verify Agent name uniqueness constraint

**Preconditions:**
- Backend running with PostgreSQL
- Migration V109 applied
- At least one agent exists

**Steps:**
1. Create agent "John Agent" via API:
   ```bash
   POST /api/v1/agents
   {
     "name": "John Agent",
     "phone": "9876543210",
     "email": "john@agent.com",
     "area": "Chennai",
     "locality": "T Nagar",
     "isActive": true
   }
   ```
2. Attempt to create duplicate agent with same name:
   ```bash
   POST /api/v1/agents
   {
     "name": "John Agent",
     "phone": "9876543211",
     "email": "jane@agent.com",
     "area": "Salem",
     "locality": "Main Road",
     "isActive": true
   }
   ```
3. Verify service layer rejects with 400 Bad Request
4. Attempt database-level duplicate insertion via SQL:
   ```sql
   INSERT INTO agents (name, phone, email, area, locality, is_active, created_at, updated_at)
   VALUES ('John Agent', '9876543211', 'jane@agent.com', 'Salem', 'Main Road', true, NOW(), NOW());
   ```

**Expected Result:**
- API request is rejected by service layer validation (400 error)
- Direct SQL insertion is rejected by database constraint: `duplicate key value violates unique constraint "ux_agents_name_ci"`
- Error message indicates constraint violation on `lower(trim(name))`

**Status:** NOT TESTED

---

## TC-V109-002: Verify case-insensitive agent name uniqueness

**Preconditions:**
- Agent "John Agent" exists

**Steps:**
1. Attempt to create agent with different case:
   ```sql
   INSERT INTO agents (name, phone, email, area, locality, is_active, created_at, updated_at)
   VALUES ('JOHN AGENT', '9876543211', 'jane@agent.com', 'Salem', 'Main Road', true, NOW(), NOW());
   ```
2. Verify constraint violation

**Expected Result:**
- Database rejects insertion with unique constraint violation on `ux_agents_name_ci`

**Status:** NOT TESTED

---

## TC-V109-003: Verify whitespace-trimmed agent name uniqueness

**Preconditions:**
- Agent "John Agent" exists

**Steps:**
1. Attempt to create agent with extra whitespace:
   ```sql
   INSERT INTO agents (name, phone, email, area, locality, is_active, created_at, updated_at)
   VALUES ('  John Agent  ', '9876543211', 'jane@agent.com', 'Salem', 'Main Road', true, NOW(), NOW());
   ```

**Expected Result:**
- Database rejects insertion with unique constraint violation on `ux_agents_name_ci`
- Whitespace differences are ignored

**Status:** NOT TESTED

---

## TC-V109-004: Verify speciality name and code uniqueness

**Preconditions:**
- Speciality "Computer Science" with code "CS" exists

**Steps:**
1. Attempt duplicate speciality name via SQL:
   ```sql
   INSERT INTO specialities (name, code, description, hod_name, created_at, updated_at)
   VALUES ('Computer Science', 'MATH', 'Math Speciality', 'Dr. Smith', NOW(), NOW());
   ```
2. Verify constraint violation on `ux_specialities_name_ci`
3. Attempt duplicate speciality code via SQL:
   ```sql
   INSERT INTO specialities (name, code, description, hod_name, created_at, updated_at)
   VALUES ('Mathematics', 'CS', 'Math Speciality', 'Dr. Smith', NOW(), NOW());
   ```
4. Verify constraint violation on `ux_specialities_code_ci`

**Expected Result:**
- Both insertions are rejected by database constraints
- Speciality names are globally unique (case-insensitive)
- Speciality codes are globally unique (case-insensitive)

**Status:** NOT TESTED

---

## TC-V109-005: Verify program name and code uniqueness

**Preconditions:**
- Program "Bachelor of Science" with code "BSC" exists

**Steps:**
1. Attempt duplicate program name:
   ```sql
   INSERT INTO programs (name, code, duration, created_at, updated_at)
   VALUES ('Bachelor of Science', 'BCA', 3, NOW(), NOW());
   ```
2. Verify constraint violation on `ux_programs_name_ci`
3. Attempt duplicate program code:
   ```sql
   INSERT INTO programs (name, code, duration, created_at, updated_at)
   VALUES ('Bachelor of Commerce', 'BSC', 3, NOW(), NOW());
   ```
4. Verify constraint violation on `ux_programs_code_ci`

**Expected Result:**
- Both insertions blocked by database constraints
- Program names and codes are globally unique

**Status:** NOT TESTED

---

## TC-V109-006: Verify course name and code uniqueness

**Preconditions:**
- Course "Data Structures" with code "CS101" exists

**Steps:**
1. Attempt duplicate course name:
   ```sql
   INSERT INTO courses (name, code, description, program_id, created_at, updated_at)
   VALUES ('Data Structures', 'CS102', 'Algorithms course', 1, NOW(), NOW());
   ```
2. Verify constraint violation on `ux_courses_name_ci`
3. Attempt duplicate course code:
   ```sql
   INSERT INTO courses (name, code, description, program_id, created_at, updated_at)
   VALUES ('Algorithms', 'CS101', 'Algorithms course', 1, NOW(), NOW());
   ```
4. Verify constraint violation on `ux_courses_code_ci`

**Expected Result:**
- Both insertions blocked by database constraints
- Course names and codes are globally unique

**Status:** NOT TESTED

---

## TC-V109-007: Verify academic year name uniqueness

**Preconditions:**
- Academic year "2024-2025" exists

**Steps:**
1. Attempt duplicate academic year:
   ```sql
   INSERT INTO academic_years (name, start_date, end_date, is_current, created_at, updated_at)
   VALUES ('2024-2025', '2024-06-01', '2025-05-31', false, NOW(), NOW());
   ```
2. Verify constraint violation on `ux_academic_years_name_ci`

**Expected Result:**
- Database rejects duplicate academic year name

**Status:** NOT TESTED

---

## TC-V109-008: Verify referral type name and code uniqueness

**Preconditions:**
- Referral type "Agent" with code "AGENT" exists

**Steps:**
1. Attempt duplicate referral type name:
   ```sql
   INSERT INTO referral_types (name, code, incentive_amount, is_percentage, incentive_percentage, is_active, created_at, updated_at)
   VALUES ('Agent', 'STAFF', 5000, false, NULL, true, NOW(), NOW());
   ```
2. Verify constraint violation on `ux_referral_types_name_ci`
3. Attempt duplicate referral type code:
   ```sql
   INSERT INTO referral_types (name, code, incentive_amount, is_percentage, incentive_percentage, is_active, created_at, updated_at)
   VALUES ('Staff', 'AGENT', 3000, false, NULL, true, NOW(), NOW());
   ```
4. Verify constraint violation on `ux_referral_types_code_ci`

**Expected Result:**
- Both insertions blocked by database constraints

**Status:** NOT TESTED

---

## TC-V109-009: Verify faculty employee code and email uniqueness

**Preconditions:**
- Faculty with employee code "EMP001" and email "john@college.edu" exists

**Steps:**
1. Attempt duplicate employee code:
   ```sql
   INSERT INTO faculty (employee_code, first_name, last_name, email, phone, speciality_id, designation, specialization, joining_date, status, created_at, updated_at)
   VALUES ('EMP001', 'Jane', 'Smith', 'jane@college.edu', '9876543211', 1, 'LECTURER', 'CS', '2024-01-01', 'ACTIVE', NOW(), NOW());
   ```
2. Verify constraint violation on `ux_faculty_employee_code_ci`
3. Attempt duplicate email:
   ```sql
   INSERT INTO faculty (employee_code, first_name, last_name, email, phone, speciality_id, designation, specialization, joining_date, status, created_at, updated_at)
   VALUES ('EMP002', 'Jane', 'Smith', 'john@college.edu', '9876543211', 1, 'LECTURER', 'CS', '2024-01-01', 'ACTIVE', NOW(), NOW());
   ```
4. Verify constraint violation on `ux_faculty_email_ci`

**Expected Result:**
- Both insertions blocked by database constraints
- Employee codes and emails are globally unique

**Status:** NOT TESTED

---

## TC-V109-010: Verify lab name uniqueness within speciality

**Preconditions:**
- Lab "Physics Lab" exists in speciality ID 1

**Steps:**
1. Attempt duplicate lab name in same speciality:
   ```sql
   INSERT INTO labs (name, lab_type, speciality_id, building, room_number, capacity, status, created_at, updated_at)
   VALUES ('Physics Lab', 'PHYSICS', 1, 'Building B', '202', 25, 'ACTIVE', NOW(), NOW());
   ```
2. Verify constraint violation on `ux_labs_speciality_name_ci`
3. Verify same lab name allowed in different speciality:
   ```sql
   INSERT INTO labs (name, lab_type, speciality_id, building, room_number, capacity, status, created_at, updated_at)
   VALUES ('Physics Lab', 'PHYSICS', 2, 'Building C', '301', 30, 'ACTIVE', NOW(), NOW());
   ```

**Expected Result:**
- Insertion fails for duplicate name in same speciality
- Insertion succeeds for same name in different speciality

**Status:** NOT TESTED

---

## TC-V109-011: Verify community name and code uniqueness

**Preconditions:**
- Community "Backward Class" with code "BC" exists

**Steps:**
1. Attempt duplicate community name:
   ```sql
   INSERT INTO communities (name, code, description, is_active, created_at, updated_at)
   VALUES ('Backward Class', 'MBC', 'Description', true, NOW(), NOW());
   ```
2. Verify constraint violation on `ux_communities_name_ci`
3. Attempt duplicate community code:
   ```sql
   INSERT INTO communities (name, code, description, is_active, created_at, updated_at)
   VALUES ('Most Backward Class', 'BC', 'Description', true, NOW(), NOW());
   ```
4. Verify constraint violation on `ux_communities_code_ci`

**Expected Result:**
- Both insertions blocked by database constraints

**Status:** NOT TESTED

---

## TC-V109-012: Verify blood group name and code uniqueness

**Preconditions:**
- Blood group "A Positive" with code "A+" exists

**Steps:**
1. Attempt duplicate blood group name:
   ```sql
   INSERT INTO blood_groups (name, code, is_active, created_at, updated_at)
   VALUES ('A Positive', 'AP', true, NOW(), NOW());
   ```
2. Verify constraint violation on `ux_blood_groups_name_ci`
3. Attempt duplicate blood group code:
   ```sql
   INSERT INTO blood_groups (name, code, is_active, created_at, updated_at)
   VALUES ('A Plus', 'A+', true, NOW(), NOW());
   ```
4. Verify constraint violation on `ux_blood_groups_code_ci`

**Expected Result:**
- Both insertions blocked by database constraints

**Status:** NOT TESTED

---

## TC-V109-013: Verify equipment asset code uniqueness

**Preconditions:**
- Equipment with asset code "ASSET001" exists

**Steps:**
1. Attempt duplicate asset code:
   ```sql
   INSERT INTO equipment (name, asset_code, category, lab_id, status, created_at, updated_at)
   VALUES ('HP Computer', 'ASSET001', 'COMPUTER', 1, 'AVAILABLE', NOW(), NOW());
   ```
2. Verify constraint violation on `ux_equipment_asset_code_ci`
3. Verify NULL asset codes are allowed:
   ```sql
   INSERT INTO equipment (name, asset_code, category, lab_id, status, created_at, updated_at)
   VALUES ('Equipment 1', NULL, 'COMPUTER', 1, 'AVAILABLE', NOW(), NOW());
   
   INSERT INTO equipment (name, asset_code, category, lab_id, status, created_at, updated_at)
   VALUES ('Equipment 2', NULL, 'COMPUTER', 1, 'AVAILABLE', NOW(), NOW());
   ```

**Expected Result:**
- Duplicate asset code insertion is blocked
- Multiple NULL asset codes are allowed (partial unique index)

**Status:** NOT TESTED

---

## TC-V109-014: Verify semester name uniqueness within academic year

**Preconditions:**
- Semester "Fall 2024" exists in academic year ID 1

**Steps:**
1. Attempt duplicate semester name in same academic year:
   ```sql
   INSERT INTO semesters (name, academic_year_id, start_date, end_date, semester_number, created_at, updated_at)
   VALUES ('Fall 2024', 1, '2025-01-01', '2025-05-15', 2, NOW(), NOW());
   ```
2. Verify constraint violation on `ux_semesters_academic_year_name_ci`
3. Verify same semester name allowed in different academic year:
   ```sql
   INSERT INTO semesters (name, academic_year_id, start_date, end_date, semester_number, created_at, updated_at)
   VALUES ('Fall 2024', 2, '2025-08-01', '2025-12-15', 1, NOW(), NOW());
   ```

**Expected Result:**
- Insertion fails for duplicate name in same academic year
- Insertion succeeds for same name in different academic year

**Status:** NOT TESTED

---

## TC-V109-015: Verify migration handles existing duplicates

**Preconditions:**
- Fresh PostgreSQL database
- Migrations up to V108 applied
- Manually insert duplicate data before applying V109

**Steps:**
1. Insert duplicate agents via SQL (before V109):
   ```sql
   INSERT INTO agents (name, phone, email, area, locality, is_active, created_at, updated_at)
   VALUES ('Test Agent', '9876543210', 'test1@agent.com', 'Area1', 'Locality1', true, NOW(), NOW()),
          ('Test Agent', '9876543211', 'test2@agent.com', 'Area2', 'Locality2', true, NOW(), NOW()),
          ('Test Agent', '9876543212', 'test3@agent.com', 'Area3', 'Locality3', true, NOW(), NOW');
   ```
2. Run migration V109:
   ```bash
   ./gradlew bootRun
   ```
3. Query agents table:
   ```sql
   SELECT id, name FROM agents WHERE name LIKE 'Test Agent%' ORDER BY id;
   ```

**Expected Result:**
- Migration completes successfully without errors
- First duplicate keeps original name: "Test Agent"
- Later duplicates are renamed with ID suffix: "Test Agent #2", "Test Agent #3"
- Unique index `ux_agents_name_ci` is created
- No further duplicates can be inserted

**Status:** NOT TESTED

---

## TC-V109-016: Verify all unique indexes are created

**Preconditions:**
- Migration V109 applied successfully

**Steps:**
1. Query PostgreSQL for all unique indexes created by V109:
   ```sql
   SELECT
       schemaname,
       tablename,
       indexname,
       indexdef
   FROM pg_indexes
   WHERE indexname LIKE 'ux_%_ci'
   ORDER BY tablename, indexname;
   ```

**Expected Result:**
- All the following indexes exist:
  - `ux_agents_name_ci` on `agents(lower(trim(name)))`
  - `ux_specialities_name_ci` on `specialities(lower(trim(name)))`
  - `ux_specialities_code_ci` on `specialities(lower(trim(code)))`
  - `ux_programs_name_ci` on `programs(lower(trim(name)))`
  - `ux_programs_code_ci` on `programs(lower(trim(code)))`
  - `ux_courses_name_ci` on `courses(lower(trim(name)))`
  - `ux_courses_code_ci` on `courses(lower(trim(code)))`
  - `ux_academic_years_name_ci` on `academic_years(lower(trim(name)))`
  - `ux_referral_types_name_ci` on `referral_types(lower(trim(name)))`
  - `ux_referral_types_code_ci` on `referral_types(lower(trim(code)))`
  - `ux_faculty_employee_code_ci` on `faculty(lower(trim(employee_code)))`
  - `ux_faculty_email_ci` on `faculty(lower(trim(email)))`
  - `ux_labs_speciality_name_ci` on `labs(speciality_id, lower(trim(name)))`
  - `ux_communities_name_ci` on `communities(lower(trim(name)))`
  - `ux_communities_code_ci` on `communities(lower(trim(code)))`
  - `ux_blood_groups_name_ci` on `blood_groups(lower(trim(name)))`
  - `ux_blood_groups_code_ci` on `blood_groups(lower(trim(code)))`
  - `ux_equipment_asset_code_ci` on `equipment(lower(trim(asset_code)))` WHERE NOT NULL
  - `ux_semesters_academic_year_name_ci` on `semesters(academic_year_id, lower(trim(name)))`

**Status:** NOT TESTED

---

## Notes

- These tests verify **database-level** constraints that cannot be tested in the H2 test environment
- Service-layer validation is tested separately in unit tests
- The migration automatically handles existing duplicates by appending IDs
- All constraints are case-insensitive and whitespace-trimmed
- Partial unique indexes (WHERE... IS NOT NULL) allow multiple NULL values
- Composite uniqueness (e.g., labs, semesters) enforces uniqueness within a scope

