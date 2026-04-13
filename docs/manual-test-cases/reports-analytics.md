# Manual Test Cases — Reports & Analytics (R1-M5.2)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- Some data exists in the system (labs, schedules, students, attendance records)

---

### TC-RPT-001: Navigate to Reports Dashboard

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Reports" in the sidebar navigation        |
| **Expected**| Reports Dashboard page loads with title "Reports Dashboard" |

---

### TC-RPT-002: Lab Utilization Section Displayed

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the Reports Dashboard                       |
| **Expected**| "Lab Utilization" section displays cards for: Total Labs, Total Schedules, Avg Schedules per Lab |

---

### TC-RPT-003: Lab Utilization Data Values

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Verify lab utilization card values               |
| **Expected**| Cards show numeric values matching the backend data; if no data, values show "—" |

---

### TC-RPT-004: Attendance Analytics Section Displayed

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View the Reports Dashboard                       |
| **Expected**| "Attendance Analytics" section displays cards for: Total Students, Total Attendance Records |

---

### TC-RPT-005: Attendance Analytics Data Values

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Verify attendance analytics card values          |
| **Expected**| Cards show numeric values matching the backend data; if no data, values show "—" |

---

### TC-RPT-006: Loading State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to Reports Dashboard while API is loading |
| **Expected**| Loading spinner is displayed until data is fetched |

---

### TC-RPT-007: Error Handling — API Failure

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to Reports Dashboard when backend is unreachable |
| **Expected**| Snackbar shows error message (e.g., "Failed to load lab utilization" or "Failed to load attendance analytics") |

---

### TC-RPT-008: KPI Card Layout

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View Reports Dashboard on a wide screen          |
| **Expected**| KPI cards are displayed in a responsive grid layout; cards have icon, value, and label |

---

### TC-RPT-009: Responsive Layout

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Resize browser to mobile width (< 768px)         |
| **Expected**| KPI cards stack vertically in a single column    |
