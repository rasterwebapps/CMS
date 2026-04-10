# Manual Test Cases — Application Shell (R1-M1.3)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Keycloak running at `http://localhost:8180` with `cms` realm configured
- User logged in (e.g., `admin` / `admin123`)

---

## TC-SHELL-001: Sidenav Layout Displayed After Login

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Log in as any user                               |
| **Expected**| Application displays: left sidebar with navigation items, top toolbar with app title, menu toggle, theme toggle, and user menu |

---

## TC-SHELL-002: Sidenav Toggle

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the hamburger menu icon (☰) in the toolbar |
| **Expected**| Side navigation panel collapses/expands          |

---

## TC-SHELL-003: Dashboard Landing Page

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to `/` or `/dashboard`                  |
| **Expected**| Dashboard loads with: 4 metric cards (Total Students, Total Faculty, Departments, Active Courses) showing placeholder "—" values, Lab Utilization placeholder widget, Recent Activity placeholder widget |

---

## TC-SHELL-004: Dark Theme Toggle

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the theme toggle button (sun/moon icon) in the toolbar |
| **Expected**| Application switches to dark theme: dark background, light text, Material components update accordingly; icon changes from `dark_mode` to `light_mode` |

---

## TC-SHELL-005: Light Theme Toggle

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | While in dark theme, click the theme toggle button again |
| **Expected**| Application switches back to light theme          |

---

## TC-SHELL-006: User Menu — Username Display

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the user icon (account_circle) in the toolbar |
| **Expected**| Dropdown menu shows the logged-in username (e.g., "admin") and a Logout option |

---

## TC-SHELL-007: User Menu — Logout

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open user menu and click "Logout"                |
| **Expected**| User is logged out and redirected to Keycloak login page |

---

## TC-SHELL-008: Navigation — Dashboard Link Active State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to `/dashboard`                         |
| **Expected**| The "Dashboard" link in the sidenav has an active/highlighted appearance |

---

## TC-SHELL-009: Wildcard Route Redirect

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to a non-existent route (e.g., `/nonexistent`) |
| **Expected**| User is redirected to `/dashboard`               |

---

## TC-SHELL-010: Responsive Layout

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Resize browser window to mobile width (< 768px)  |
| **Expected**| Layout adjusts; sidenav can be toggled, content area fills available width |
