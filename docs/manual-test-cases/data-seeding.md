# Manual Test Cases — Demo Data Seeding

## TC-SEED-001: Verify automatic seed data loads on application startup (local profile)

**Preconditions:**
- Backend configured with default `local` Spring profile (H2 in-memory database)
- Keycloak is running at `http://localhost:8280`
- Local realm `cms` is imported
- Admin user exists with username `admin` and password `admin123`
- Frontend is available for visual verification

**Steps:**
1. Start the backend: `cd backend && ./gradlew bootRun`
2. Observe startup logs for the line: `Seeding initial data for local profile...`
3. Observe the final log line: `Seed data loaded successfully.`
4. Open the frontend and navigate through the screens in this order:
   - Departments → verify 5 departments (GN, MO, CHN, MSN, PN)
   - Programs → verify 3 programs (B.Sc Nursing, M.Sc Nursing, GNM)
   - Courses → verify 3 courses
   - Academic Years → verify 3 years (2023-24, 2024-25 current, 2025-26)
   - Semesters → verify 4 semesters under academic years
   - Faculty → verify 8 faculty members
   - Students → verify 10 students with detailed profiles
   - Labs → verify 4 labs with assignments
   - Equipment → verify 5 equipment items
   - Inventory → verify 5 inventory items
   - Maintenance → verify 3 maintenance requests
   - Fee Structures → verify 4 fee structures with year-wise amounts
   - Examinations → verify 5 examinations with results
   - Enquiries → verify 7 enquiries in various statuses
   - Agents → verify 3 agents
5. Restart the backend and confirm the log shows: `Seed data already present — skipping DataLoader.`

**Expected Result:**
- Backend starts and auto-seeds all entities in dependency-safe order using the `DataLoader` component
- All screens show the seeded nursing-college data
- Restarting the application skips seeding (idempotent)

**Actual Result:**

**Status:** NOT TESTED

## TC-SEED-002: Verify seed data for PostgreSQL (prod profile via Flyway migration V45)

**Preconditions:**
- Docker Compose is running (`docker compose up -d`)
- Backend configured with `prod` Spring profile: `SPRING_PROFILES_ACTIVE=prod`
- Flyway migrations V1–V44 have already been applied

**Steps:**
1. Start the backend: `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun`
2. Confirm Flyway applies `V45__insert_seed_data.sql` in startup logs
3. Obtain an admin access token from Keycloak for client `cms-frontend`
4. Send authenticated GET requests to verify:
   - `GET /api/v1/departments` → 5 records
   - `GET /api/v1/programs` → 3 records
   - `GET /api/v1/courses` → 3 records
   - `GET /api/v1/academic-years` → 3 records
   - `GET /api/v1/semesters` → 4 records
   - `GET /api/v1/faculty` → 8 records
   - `GET /api/v1/students` → 10 records
   - `GET /api/v1/labs` → 4 records
   - `GET /api/v1/equipment` → 5 records
   - `GET /api/v1/inventory` → 5 records
   - `GET /api/v1/maintenance` → 3 records
   - `GET /api/v1/fee-structures` → 4 records
   - `GET /api/v1/examinations` → 5 records
   - `GET /api/v1/enquiries` → 7 records
   - `GET /api/v1/agents` → 3 records
5. Re-run the backend and confirm V45 is not applied again (idempotent DO $$ block)

**Expected Result:**
- V45 migration inserts all seed data successfully in PostgreSQL
- All API endpoints return the expected counts
- Re-running the migration has no effect (skips due to existing departments)

**Actual Result:**

**Status:** NOT TESTED

## TC-SEED-003: Verify seeded data integrity and screen operations

**Preconditions:**
- Application is running with seed data loaded
- Logged in as admin

**Steps:**
1. Navigate to **Departments** → click Edit on "General Nursing" → change HOD name → Save → verify change persists
2. Navigate to **Faculty** → click on "Priya Sharma" detail → verify department, designation, joining date shown
3. Navigate to **Students** → click on "Aishwarya Rajput" → verify semester, program, personal details shown
4. Navigate to **Enquiries** → verify status badges show different colors (ENQUIRED, INTERESTED, FEES_FINALIZED, CONVERTED, NOT_INTERESTED, DOCUMENTS_SUBMITTED)
5. Navigate to **Fee Structures** → click on B.Sc Nursing tuition fee → verify 4-year breakdown (25000, 25000, 25000, 20000)
6. Navigate to **Equipment** → verify "Projector System" shows UNDER_MAINTENANCE status badge
7. Navigate to **Examinations** → click on "Anatomy Mid-Term" → verify exam results for 4 students
8. Navigate to **Lab Schedules** → verify Batch A on Monday, Batch B on Wednesday in Nursing Foundation Lab
9. Navigate to **Agents** → verify 3 agents with commission guidelines
10. Navigate to **Syllabus** → verify syllabi for Anatomy, Nursing Foundations Lab, Basic Nursing

**Expected Result:**
- All seeded records display correctly in their respective screens
- Edit operations work on seeded records
- Related data (dropdowns, detail pages) resolve correctly

**Actual Result:**

**Status:** NOT TESTED

