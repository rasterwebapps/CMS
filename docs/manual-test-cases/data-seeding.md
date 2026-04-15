# Manual Test Cases — Demo Data Seeding

## TC-SEED-001: Populate 10 records for each primary management screen

**Preconditions:**
- Backend is running at `http://localhost:8080`
- Keycloak is running at `http://localhost:8280`
- Local realm `cms` is imported
- Admin user exists with username `admin` and password `admin123`
- Frontend is available for visual verification

**Steps:**
1. Run the seed script from the project root: `python3 scripts/seed_demo_data.py`
2. Wait for the script to finish successfully
3. Verify the script prints a seed batch identifier and created IDs
4. Verify the script prints totals for each populated module
5. Open the frontend and navigate through the screens in this order:
   - Departments
   - Programs
   - Courses
   - Academic Years
   - Semesters
   - Faculty
   - Students
   - Attendance
   - Labs
   - Fee Structures
   - Fee Payments
   - Equipment
   - Inventory
   - Maintenance
   - Examinations
   - Exam Results
6. Confirm each listed screen shows at least 10 records

**Expected Result:**
- The seed script completes without errors
- Each primary management screen listed above contains at least 10 records
- Records appear in dependency-safe order and related dropdowns / references resolve correctly

**Actual Result:**

**Status:** NOT TESTED

## TC-SEED-002: Verify authenticated API counts after seeding

**Preconditions:**
- `TC-SEED-001` has been executed successfully
- Backend and Keycloak are running

**Steps:**
1. Obtain an admin access token from Keycloak for client `cms-frontend`
2. Send authenticated GET requests to the following endpoints:
   - `/api/v1/departments`
   - `/api/v1/programs`
   - `/api/v1/courses`
   - `/api/v1/academic-years`
   - `/api/v1/semesters`
   - `/api/v1/faculty`
   - `/api/v1/students`
   - `/api/v1/labs`
   - `/api/v1/fee-structures`
   - `/api/v1/fee-payments`
   - `/api/v1/equipment`
   - `/api/v1/inventory`
   - `/api/v1/maintenance`
   - `/api/v1/examinations`
3. Verify each endpoint returns at least 10 records
4. Verify attendance totals by querying `/api/v1/attendance?courseId={courseId}` for the seeded courses
5. Verify exam-result totals by querying `/api/v1/exam-results/examination/{examinationId}` for the seeded examinations

**Expected Result:**
- Each seeded module returns at least 10 records through authenticated API calls
- Attendance and exam result endpoints return records linked to the seeded entities

**Actual Result:**

**Status:** NOT TESTED

