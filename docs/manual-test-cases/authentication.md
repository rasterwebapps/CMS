# Manual Test Cases — Authentication (R1-M1.2)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Keycloak running at `http://localhost:8180` with `cms` realm configured
- Backend running at `http://localhost:8080`
- Test users available (admin, faculty1, student1, labincharge1, parent1)

---

## TC-AUTH-001: Unauthenticated User Redirected to Keycloak Login

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open `http://localhost:4200` in a private browser window |
| **Expected**| User is redirected to Keycloak login page (`http://localhost:8180/realms/cms/protocol/openid-connect/auth?...`) |

---

## TC-AUTH-002: Successful Login as Admin

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | On the Keycloak login page, enter `admin` / `admin123` |
| **Expected**| User is redirected back to the CMS dashboard; username "admin" shown in toolbar user menu |

---

## TC-AUTH-003: Successful Login as Student

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | On the Keycloak login page, enter `student1` / `student123` |
| **Expected**| User is redirected back to the CMS dashboard; username "student1" shown in toolbar user menu |

---

## TC-AUTH-004: Logout Functionality

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | While logged in, click the user icon → Logout    |
| **Expected**| User is redirected to Keycloak login page; accessing `http://localhost:4200` again requires re-login |

---

## TC-AUTH-005: Bearer Token Attached to API Requests

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | While logged in, open browser DevTools → Network tab; observe any API call to the backend |
| **Expected**| Request headers include `Authorization: Bearer <jwt_token>` |

---

## TC-AUTH-006: Route Guard — Protected Route Requires Authentication

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Clear session/cookies and navigate directly to `http://localhost:4200/dashboard` |
| **Expected**| User is redirected to Keycloak login               |

---

## TC-AUTH-007: Token Refresh

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Log in and wait for the access token to expire (or manually trigger refresh) |
| **Expected**| Application continues to function; a new token is obtained silently |
