# Manual Test Cases — Reports & Analytics (R1-M5.2)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4300`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- Demo data seeded via `python3 scripts/seed_demo_data.py`

---

### TC-RPT-001: Navigate to Reports Dashboard

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Reports" in the sidebar navigation        |
| **Expected**| Reports & Analytics page loads with title "Reports & Analytics" |

---

### TC-RPT-002: Lab Utilization KPI Cards

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the Reports Dashboard                       |
| **Expected**| "Lab Utilization" section displays 4 KPI cards: Total Labs (10), Total Schedules (0), Avg Schedules / Lab (0.0), Total Equipment (10) |

---

### TC-RPT-003: Equipment by Status Breakdown

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the Equipment by Status card in Lab Utilization section |
| **Expected**| Shows breakdown: Available (6), Under Maintenance (3), In Use (1) |

---

### TC-RPT-004: Labs by Status Breakdown

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the Labs by Status card in Lab Utilization section |
| **Expected**| Shows breakdown: Active (9), Under Maintenance (1) |

---

### TC-RPT-005: Attendance Analytics KPI Cards

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the Attendance Analytics section            |
| **Expected**| Cards show: Total Students (10), Total Records (10) |

---

### TC-RPT-006: Attendance by Status Breakdown

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the Attendance by Status breakdown card     |
| **Expected**| Shows: Present (3), Absent (3), Late (2), Excused (2) |

---

### TC-RPT-007: Attendance by Type Breakdown

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the Attendance by Type breakdown card       |
| **Expected**| Shows: Theory (5), Lab (5) |

---

### TC-RPT-008: Loading State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to Reports Dashboard while API is loading |
| **Expected**| Loading spinner is displayed until data is fetched |

---

### TC-RPT-009: Error Handling — API Failure

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to Reports Dashboard when backend is unreachable |
| **Expected**| Snackbar shows error message (e.g., "Failed to load lab utilization" or "Failed to load attendance analytics") |

---

### TC-RPT-010: Dashboard KPI Cards with Real Data

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to the main Dashboard (home page)       |
| **Expected**| 11 KPI cards display actual counts: Students (10), Faculty (10), Departments (10), Courses (10), Programs (10), Labs (10), Equipment (10), Examinations (10), Fee Payments (10), Maintenance (10), Attendance Records (10) |

---

### TC-RPT-011: Dashboard Status Breakdown Cards

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the breakdown section below KPI cards on the Dashboard |
| **Expected**| Four cards show: Equipment by Status, Maintenance by Status, Students by Status (Active: 10), Attendance by Status |

---

### TC-RPT-012: Responsive Layout

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Resize browser to mobile width (< 768px)         |
| **Expected**| KPI cards and breakdown cards stack vertically   |
