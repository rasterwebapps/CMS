# Security Configuration — Manual Test Cases

## TC-SEC-001: Health endpoint is publicly accessible

**Preconditions:**
- Backend application is running (`./gradlew bootRun`)
- Keycloak is running on port 8180

**Steps:**
1. Send a GET request to `http://localhost:8080/api/v1/health` without any authentication headers
2. Verify the response status is 200 OK
3. Verify the response body contains `"status": "UP"` and a `"timestamp"` field

**Expected Result:**
- Health endpoint returns 200 OK with status "UP" and a valid timestamp

**Status:** NOT TESTED

---

## TC-SEC-002: Protected endpoints require authentication

**Preconditions:**
- Backend application is running
- Keycloak is running on port 8180

**Steps:**
1. Send a GET request to `http://localhost:8080/api/v1/departments` without any authentication headers
2. Verify the response status is 401 Unauthorized

**Expected Result:**
- Unauthenticated requests to protected endpoints return 401 Unauthorized

**Status:** NOT TESTED

---

## TC-SEC-003: CORS allows frontend origin

**Preconditions:**
- Backend application is running
- Keycloak is running on port 8180

**Steps:**
1. Send an OPTIONS preflight request to `http://localhost:8080/api/v1/health` with `Origin: http://localhost:4200`
2. Verify the response includes `Access-Control-Allow-Origin: http://localhost:4200`
3. Send an OPTIONS request with `Origin: http://evil.com`
4. Verify the CORS headers are not present or the request is rejected

**Expected Result:**
- CORS allows requests from `http://localhost:4200` and `http://localhost:4300` only

**Status:** NOT TESTED

---

## TC-SEC-004: JWT role extraction from Keycloak tokens

**Preconditions:**
- Backend and Keycloak are running
- A valid Keycloak access token is available for user `admin` (ROLE_ADMIN)

**Steps:**
1. Obtain an access token from Keycloak for the `admin` user
2. Send a GET request to a protected endpoint with `Authorization: Bearer <token>`
3. Verify the request is authenticated and the user has ROLE_ADMIN authority

**Expected Result:**
- Keycloak realm roles are correctly extracted from the JWT and mapped to Spring Security authorities

**Status:** NOT TESTED

---

## TC-SEC-005: GlobalExceptionHandler returns standardized error responses

**Preconditions:**
- Backend application is running

**Steps:**
1. Send a request that triggers a ResourceNotFoundException (e.g., GET non-existent resource)
2. Verify the response is 404 with body: `{"status": 404, "message": "...", "timestamp": "..."}`
3. Send a request with invalid validation data
4. Verify the response is 400 with field-level error messages
5. Send a request without authentication to a protected endpoint
6. Verify the response is 401

**Expected Result:**
- All errors return a consistent `ErrorResponse` format with `status`, `message`, and `timestamp`

**Status:** NOT TESTED

---

## TC-SEC-006: H2 Console accessible in local profile

**Preconditions:**
- Backend is running with `local` profile (default)

**Steps:**
1. Navigate to `http://localhost:8080/api/v1/h2-console` in a browser
2. Verify the H2 Console login page is displayed
3. Connect using JDBC URL: `jdbc:h2:mem:cmsdb`, username: `sa`, no password

**Expected Result:**
- H2 Console is accessible without authentication in the local profile

**Status:** NOT TESTED
