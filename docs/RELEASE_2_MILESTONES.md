# 🚀 Release 2 — Milestone Tracker

> **College Management System — Release 2** covers the extended modules: lab safety, communication portals, library, hostel, transport, research, placement, events, LMS, feedback, security/compliance, and mobile/integration.
>
> This corresponds to **Phase 6** from the [Master Development Plan](DEVELOPMENT_PLAN.md).
>
> **Prerequisite:** [Release 1](RELEASE_1_MILESTONES.md) must be complete before starting Release 2.

---

## 📋 Table of Contents

- [Release 2 Scope](#-release-2-scope)
- [R2-M1: Lab Safety & Compliance](#-r2-m1-lab-safety--compliance)
- [R2-M2: Communication & Portals](#-r2-m2-communication--portals)
- [R2-M3: Library Management](#-r2-m3-library-management)
- [R2-M4: Hostel Management](#-r2-m4-hostel-management)
- [R2-M5: Transport Management](#-r2-m5-transport-management)
- [R2-M6: Research & Publication](#-r2-m6-research--publication)
- [R2-M7: Placement & Career](#-r2-m7-placement--career)
- [R2-M8: Event & Activity Management](#-r2-m8-event--activity-management)
- [R2-M9: Online Learning / LMS](#-r2-m9-online-learning--lms)
- [R2-M10: Feedback & Survey](#-r2-m10-feedback--survey)
- [R2-M11: Security & Compliance](#-r2-m11-security--compliance)
- [R2-M12: Mobile & Integration](#-r2-m12-mobile--integration)
- [Release 2 Definition of Done](#-release-2-definition-of-done)
- [Release 2 Progress Tracking](#-release-2-progress-tracking)

---

## 🎯 Release 2 Scope

| Milestone | Phase Origin | Module | Key Outcome |
|-----------|-------------|--------|-------------|
| **R2-M1** | Phase 6.1 | Module 7.8 | Lab safety guidelines, incident reporting, PPE tracking |
| **R2-M2** | Phase 6.2 | Module 12 | Notice board, messaging, student/faculty/parent portals |
| **R2-M3** | Phase 6.3 | Module 9 | Book cataloging, issue/return, digital library |
| **R2-M4** | Phase 6.4 | Module 10 | Room allocation, hostel fees, mess management |
| **R2-M5** | Phase 6.5 | Module 11 | Routes, vehicles, transport fees, driver management |
| **R2-M6** | Phase 6.6 | Module 14 | Research projects, publications, patents, grants |
| **R2-M7** | Phase 6.7 | Module 15 | Placement drives, job portal, resume builder |
| **R2-M8** | Phase 6.8 | Module 17 | Events, clubs, certificate generation |
| **R2-M9** | Phase 6.9 | Module 19 | LMS content delivery, assignments, virtual labs |
| **R2-M10** | Phase 6.10 | Module 21 | Student feedback, 360° feedback, surveys, grievances |
| **R2-M11** | Phase 6.11 | Module 18 | Audit logs, GDPR compliance, document management |
| **R2-M12** | Phase 6.12 | Module 20 | Mobile API optimization, biometric/RFID, IoT integration |

---

## 🛡️ R2-M1: Lab Safety & Compliance

> **Module 7.8** — Safety guidelines, incident reporting, and PPE tracking.

- [ ] **R2-1.1** Backend: Create safety guideline entities and APIs
  - Safety guidelines management (per lab, per department)
  - PPE (Personal Protective Equipment) tracking per lab
- [ ] **R2-1.2** Backend: Create incident reporting entities and APIs
  - Incident report entity (`id`, `lab`, `reportedBy`, `incidentDate`, `description`, `severity`, `status`, `actionTaken`)
  - Incident workflow (Report → Investigate → Resolve → Close)
- [ ] **R2-1.3** Backend: Create safety training and audit entities and APIs
  - Safety training records (student/faculty training completion)
  - Safety audit scheduling and results
- [ ] **R2-1.4** Frontend: Create safety management components
  - Safety guidelines list and editor
  - Incident reporting form and tracker
  - PPE inventory dashboard
  - Training records view
- [ ] **R2-1.5** Write unit + controller tests (95% coverage)
- [ ] **R2-1.6** Create manual test cases: `docs/manual-test-cases/lab-safety.md`

---

## 📢 R2-M2: Communication & Portals

> **Module 12** — Notice board, internal messaging, and role-specific portals.

- [ ] **R2-2.1** Backend: Create notice board / announcements API
  - Announcement entity (`id`, `title`, `content`, `targetAudience`, `department`, `publishDate`, `expiryDate`, `createdBy`)
  - Filter announcements by role, department, date
- [ ] **R2-2.2** Backend: Create internal messaging system
  - Message entity with sender, recipients, subject, body, read status
  - Inbox, sent, and archived message endpoints
- [ ] **R2-2.3** Frontend: Create student portal dashboard
  - Personalized view: enrolled courses, attendance, lab schedule, fee status, results
- [ ] **R2-2.4** Frontend: Create faculty portal dashboard
  - Personalized view: assigned courses, lab schedules, attendance marking, student lists
- [ ] **R2-2.5** Frontend: Create parent portal (read-only access to ward's progress)
  - Ward's attendance, results, fee payment status, announcements
  - Restricted by `ROLE_PARENT`
- [ ] **R2-2.6** Frontend: Create announcement and messaging components
  - Announcement list, detail, and creation form
  - Messaging inbox, compose, and thread views
- [ ] **R2-2.7** Write unit + controller tests (95% coverage)
- [ ] **R2-2.8** Create manual test cases: `docs/manual-test-cases/communication-portals.md`

---

## 📚 R2-M3: Library Management

> **Module 9** — Book cataloging, issue/return workflows, and digital library.

- [ ] **R2-3.1** Backend: Create library entities and APIs
  - `Book` entity (`id`, `title`, `author`, `isbn`, `publisher`, `category`, `totalCopies`, `availableCopies`)
  - `BookIssue` entity (`id`, `book`, `student`, `issueDate`, `dueDate`, `returnDate`, `fineAmount` [BigDecimal], `status`)
  - Book cataloging, search, and availability endpoints
  - Issue/return/renew workflow endpoints
  - Fine calculation logic
- [ ] **R2-3.2** Backend: Create digital library APIs
  - Digital resource management (e-books, journals, papers)
  - Access control by student/faculty role
- [ ] **R2-3.3** Frontend: Create library management components
  - Book catalog with search, filters, and availability indicators
  - Issue/return form and history
  - Fine payment tracker
  - Digital library browser
- [ ] **R2-3.4** Write unit + controller tests (95% coverage)
- [ ] **R2-3.5** Create manual test cases: `docs/manual-test-cases/library-management.md`

---

## 🏠 R2-M4: Hostel Management

> **Module 10** — Room allocation, hostel fees, mess management, and hostel attendance.

- [ ] **R2-4.1** Backend: Create hostel entities and APIs
  - `Hostel` entity (`id`, `name`, `type` [BOYS/GIRLS], `totalRooms`, `warden`)
  - `Room` entity (`id`, `hostel`, `roomNumber`, `capacity`, `occupants`)
  - `RoomAllocation` entity (student ↔ room mapping with dates)
  - Room allocation and vacancy tracking endpoints
- [ ] **R2-4.2** Backend: Create hostel fee and mess management APIs
  - Hostel fee structure and payment tracking
  - Mess menu management and meal tracking
- [ ] **R2-4.3** Backend: Create hostel attendance APIs
  - In/out tracking, leave requests
- [ ] **R2-4.4** Frontend: Create hostel management components
  - Room allocation dashboard with occupancy map
  - Hostel fee management
  - Mess menu and meal schedule
  - Hostel attendance tracker
- [ ] **R2-4.5** Write unit + controller tests (95% coverage)
- [ ] **R2-4.6** Create manual test cases: `docs/manual-test-cases/hostel-management.md`

---

## 🚌 R2-M5: Transport Management

> **Module 11** — Routes, vehicles, transport fees, and driver management.

- [ ] **R2-5.1** Backend: Create transport entities and APIs
  - `Route` entity (`id`, `name`, `startPoint`, `endPoint`, `stops`, `distance`)
  - `Vehicle` entity (`id`, `registrationNumber`, `type`, `capacity`, `route`, `driver`)
  - `TransportAllocation` entity (student ↔ route mapping)
  - Transport fee structure and payment tracking
  - Driver management and assignment
- [ ] **R2-5.2** Frontend: Create transport management components
  - Route management with stop configuration
  - Vehicle fleet dashboard
  - Student transport allocation
  - Transport fee management
- [ ] **R2-5.3** Write unit + controller tests (95% coverage)
- [ ] **R2-5.4** Create manual test cases: `docs/manual-test-cases/transport-management.md`

---

## 🔬 R2-M6: Research & Publication

> **Module 14** — Research projects, publications, patents, and grant management.

- [ ] **R2-6.1** Backend: Create research entities and APIs
  - `ResearchProject` entity (`id`, `title`, `principalInvestigator`, `coPIs`, `department`, `fundingSource`, `amount` [BigDecimal], `startDate`, `endDate`, `status`)
  - `Publication` entity (`id`, `title`, `authors`, `journal`, `year`, `doi`, `type` [JOURNAL/CONFERENCE/BOOK_CHAPTER])
  - `Patent` entity (`id`, `title`, `inventors`, `filingDate`, `status`, `patentNumber`)
  - Grant tracking and expenditure management
- [ ] **R2-6.2** Frontend: Create research management components
  - Research project dashboard with status tracking
  - Publication list with filters and citation metrics
  - Patent tracker
  - Grant expenditure reports
- [ ] **R2-6.3** Write unit + controller tests (95% coverage)
- [ ] **R2-6.4** Create manual test cases: `docs/manual-test-cases/research-publication.md`

---

## 💼 R2-M7: Placement & Career

> **Module 15** — Placement drives, job portal, and resume builder.

- [ ] **R2-7.1** Backend: Create placement entities and APIs
  - `PlacementDrive` entity (`id`, `company`, `date`, `eligiblePrograms`, `minCGPA`, `packageOffered` [BigDecimal], `status`)
  - `PlacementApplication` entity (student ↔ drive mapping with status tracking)
  - `PlacementOffer` entity (offer details, acceptance status)
  - Job portal with company profiles
  - Resume/CV builder data endpoints
- [ ] **R2-7.2** Frontend: Create placement management components
  - Placement drive calendar and details
  - Student application and tracking portal
  - Company profile management
  - Placement statistics dashboard
  - Resume builder interface
- [ ] **R2-7.3** Write unit + controller tests (95% coverage)
- [ ] **R2-7.4** Create manual test cases: `docs/manual-test-cases/placement-career.md`

---

## 🎉 R2-M8: Event & Activity Management

> **Module 17** — Events, clubs, and certificate generation.

- [ ] **R2-8.1** Backend: Create event entities and APIs
  - `Event` entity (`id`, `name`, `type`, `date`, `venue`, `organizer`, `department`, `budget` [BigDecimal], `status`)
  - `Club` entity (`id`, `name`, `type`, `faculty_advisor`, `president`, `department`)
  - `EventRegistration` entity (student ↔ event mapping)
  - Certificate generation endpoints (participation, achievement)
- [ ] **R2-8.2** Frontend: Create event management components
  - Event calendar and registration
  - Club management dashboard
  - Certificate template manager and generation
  - Event budget tracker
- [ ] **R2-8.3** Write unit + controller tests (95% coverage)
- [ ] **R2-8.4** Create manual test cases: `docs/manual-test-cases/event-management.md`

---

## 🎓 R2-M9: Online Learning / LMS

> **Module 19** — LMS content delivery, assignment submission, and virtual lab integration.

- [ ] **R2-9.1** Backend: Create LMS entities and APIs
  - `LMSCourse` entity (course content structure with modules/topics)
  - `LMSContent` entity (`id`, `course`, `module`, `title`, `contentType` [VIDEO/DOCUMENT/QUIZ], `contentUrl`, `orderIndex`)
  - `Assignment` entity (`id`, `course`, `title`, `description`, `dueDate`, `maxMarks`)
  - `AssignmentSubmission` entity (student ↔ assignment with file upload, grade)
  - Virtual lab integration endpoints
  - Content progress tracking per student
- [ ] **R2-9.2** Frontend: Create LMS components
  - Course content browser with module navigation
  - Video/document viewer
  - Assignment submission portal
  - Student progress tracker
  - Virtual lab launcher
- [ ] **R2-9.3** Write unit + controller tests (95% coverage)
- [ ] **R2-9.4** Create manual test cases: `docs/manual-test-cases/lms.md`

---

## 📋 R2-M10: Feedback & Survey

> **Module 21** — Student feedback, 360° feedback, surveys, and grievance system.

- [ ] **R2-10.1** Backend: Create feedback entities and APIs
  - `FeedbackForm` entity (`id`, `title`, `targetType` [COURSE/FACULTY/LAB/INSTITUTION], `semester`, `startDate`, `endDate`, `isAnonymous`)
  - `FeedbackQuestion` entity (questions with rating/text/MCQ types)
  - `FeedbackResponse` entity (student responses with anonymity support)
  - 360° feedback for faculty (from students, peers, HOD)
  - Survey creation and response collection
  - Grievance submission and tracking workflow
- [ ] **R2-10.2** Frontend: Create feedback and survey components
  - Feedback form builder (admin)
  - Feedback submission interface (students)
  - Feedback analytics dashboard (charts, averages, trends)
  - Survey creation and response viewer
  - Grievance submission form and status tracker
- [ ] **R2-10.3** Write unit + controller tests (95% coverage)
- [ ] **R2-10.4** Create manual test cases: `docs/manual-test-cases/feedback-survey.md`

---

## 🔒 R2-M11: Security & Compliance

> **Module 18** — Audit logs, GDPR compliance, and document management.

- [ ] **R2-11.1** Backend: Create security and compliance entities and APIs
  - `AuditLog` entity (`id`, `userId`, `action`, `entityType`, `entityId`, `timestamp`, `ipAddress`, `details`)
  - Automatic audit logging via AOP/interceptor for all write operations
  - GDPR data export and deletion endpoints (student data portability)
  - `Document` entity for institutional document management
  - Data retention policies and automated cleanup
- [ ] **R2-11.2** Frontend: Create security and compliance components
  - Audit log viewer with filters (user, action, date range, entity)
  - GDPR data request management interface
  - Document management portal (upload, version, share)
  - Compliance dashboard
- [ ] **R2-11.3** Write unit + controller tests (95% coverage)
- [ ] **R2-11.4** Create manual test cases: `docs/manual-test-cases/security-compliance.md`

---

## 📱 R2-M12: Mobile & Integration

> **Module 20** — Mobile API optimization, biometric/RFID integration, third-party APIs, and IoT.

- [ ] **R2-12.1** Backend: API optimization for mobile clients
  - Lightweight response DTOs for mobile consumption
  - Pagination and field selection support
  - Push notification integration hooks
- [ ] **R2-12.2** Backend: Biometric/RFID integration hooks
  - Attendance integration via biometric devices
  - RFID-based lab access control endpoints
  - Device registration and management APIs
- [ ] **R2-12.3** Backend: Third-party API integration endpoints
  - ERP system integration (data sync endpoints)
  - Government portal integration (regulatory reporting)
  - Payment gateway integration hooks
- [ ] **R2-12.4** Backend: IoT integration for lab sensors
  - Lab environment monitoring endpoints (temperature, humidity, power)
  - Sensor data ingestion and alerting
  - Equipment usage tracking via IoT
- [ ] **R2-12.5** Write unit + controller tests (95% coverage)
- [ ] **R2-12.6** Create manual test cases: `docs/manual-test-cases/mobile-integration.md`

---

## ✅ Release 2 Definition of Done

Every task/milestone is considered **complete** only when ALL of the following are met:

| Criteria | Description |
|----------|-------------|
| **Code Complete** | All backend and frontend code is written and functional |
| **Backend Tests** | Unit + controller tests pass with ≥95% code coverage (JaCoCo) |
| **Build Passes** | `./gradlew check` (backend) and `ng build` (frontend) succeed |
| **Flyway Migration** | Database migration script created (for PostgreSQL profile) |
| **DTOs** | All data transfer objects are Java records with Jakarta validation |
| **Role-Based Access** | `@PreAuthorize` annotations applied to all controller methods |
| **Error Handling** | All errors return standardized `ErrorResponse` via GlobalExceptionHandler |
| **Manual Test Cases** | Manual test case document created in `docs/manual-test-cases/` |
| **Business Documentation** | Any business/workflow changes documented in `docs/BUSINESS_REQUIREMENTS.md` |
| **Code Review** | Pull request reviewed and approved |
| **CHANGELOG** | `CHANGELOG.md` updated with the new feature |
| **Release 1 Stable** | All Release 1 features remain functional (regression-free) |

---

## 📊 Release 2 Progress Tracking

| Milestone | Status | Progress |
|-----------|--------|----------|
| R2-M1: Lab Safety & Compliance | ⬜ Not Started | 0% |
| R2-M2: Communication & Portals | ⬜ Not Started | 0% |
| R2-M3: Library Management | ⬜ Not Started | 0% |
| R2-M4: Hostel Management | ⬜ Not Started | 0% |
| R2-M5: Transport Management | ⬜ Not Started | 0% |
| R2-M6: Research & Publication | ⬜ Not Started | 0% |
| R2-M7: Placement & Career | ⬜ Not Started | 0% |
| R2-M8: Event & Activity Management | ⬜ Not Started | 0% |
| R2-M9: Online Learning / LMS | ⬜ Not Started | 0% |
| R2-M10: Feedback & Survey | ⬜ Not Started | 0% |
| R2-M11: Security & Compliance | ⬜ Not Started | 0% |
| R2-M12: Mobile & Integration | ⬜ Not Started | 0% |

---

> **Note:** This release tracker is aligned with the [Master Development Plan](DEVELOPMENT_PLAN.md) Phase 6. Milestones can be prioritized and reordered based on institutional needs. All Release 2 work assumes [Release 1](RELEASE_1_MILESTONES.md) is complete and stable.
