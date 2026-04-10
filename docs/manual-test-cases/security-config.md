# Manual Test Cases — Security Configuration (R1-M1.1)

## Prerequisites

- Backend running (`./gradlew bootRun` with `local` profile)
- Keycloak running at `http://localhost:8180` with `cms` realm configured
- Test users available (admin, faculty1, student1, labincharge1, parent1)

---

## TC-SEC-001: Health Endpoint Accessible Without Authentication

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Send `GET http://localhost:8080/api/v1/health` without any authorization header |
| **Expected**| HTTP 200 with body `{"status": "UP"}`            |

---

## TC-SEC-002: Protected Endpoint Returns 401 Without Token

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Send `GET http://localhost:8080/api/v1/departments` without any authorization header |
| **Expected**| HTTP 401 Unauthorized                            |

---

## TC-SEC-003: Protected Endpoint Accessible With Valid JWT

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Obtain a token from Keycloak for `admin` user, then send `GET http://localhost:8080/api/v1/health` with `Authorization: Bearer <token>` |
| **Expected**| HTTP 200 with body `{"status": "UP"}`            |

---

## TC-SEC-004: CORS Headers Present for Allowed Origin

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Send `OPTIONS http://localhost:8080/api/v1/health` with `Origin: http://localhost:4200` |
| **Expected**| Response includes `Access-Control-Allow-Origin: http://localhost:4200` |

---

## TC-SEC-005: CORS Rejects Disallowed Origin

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Send `OPTIONS http://localhost:8080/api/v1/health` with `Origin: http://evil.com` |
| **Expected**| No `Access-Control-Allow-Origin` header in response |

---

## TC-SEC-006: JWT Role Mapping — Admin Role

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Obtain token for `admin` user and decode the JWT; verify `realm_access.roles` includes `ROLE_ADMIN` |
| **Expected**| Token contains `ROLE_ADMIN` in realm roles       |

---

## TC-SEC-007: Error Response Format — 404

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Send `GET http://localhost:8080/api/v1/nonexistent` with a valid JWT |
| **Expected**| Response body matches format: `{"status": 404, "message": "...", "timestamp": "..."}` |

---

## TC-SEC-008: H2 Console Accessible Without Authentication (Local Profile Only)

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to `http://localhost:8080/api/v1/h2-console/` in browser |
| **Expected**| H2 Console login page loads without requiring JWT |
