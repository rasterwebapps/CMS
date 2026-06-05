# Manual Test Cases — Application Startup

## TC-STARTUP-001: Backend starts on default local profile using PostgreSQL

**Preconditions:**
- Java 21 is installed
- From the `backend/` directory, dependencies can be resolved by Gradle
- PostgreSQL is running and reachable at `jdbc:postgresql://localhost:5435/cmsdb` with credentials `cms` / `cms`, or `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are exported for another PostgreSQL instance
- Port `8080` is free before startup

**Steps:**
1. Run the backend with `./gradlew bootRun` from `backend/`
2. Wait for the application to finish starting
3. Verify the logs show the active profile is `local`
4. Verify the logs show PostgreSQL/Flyway startup and do not show H2 startup
5. Send a GET request to `http://localhost:8080/api/v1/health`
6. Verify the response status is `200 OK`
7. Verify the response body contains `{"status":"UP"}`

**Expected Result:**
- The backend starts successfully on port `8080` using the configured PostgreSQL database
- The local profile does not seed demo data unless `cms.seed.enabled=true` is explicitly provided
- The health endpoint responds with HTTP `200` and the JSON body `{"status":"UP"}`

**Actual Result:**

**Status:** NOT TESTED

## TC-STARTUP-003: Backend local profile can be selected explicitly and still uses PostgreSQL

**Preconditions:**
- Java 21 is installed
- PostgreSQL is running and reachable at `jdbc:postgresql://localhost:5435/cmsdb` with credentials `cms` / `cms`, or `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are exported for another PostgreSQL instance
- Port `8080` is free before startup

**Steps:**
1. From the `backend/` directory, run `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`
2. Wait for the application to finish starting
3. Verify the logs show Flyway connecting to PostgreSQL
4. Verify the logs do not show H2 startup or local demo data seeding
5. Send a GET request to `http://localhost:8080/api/v1/specialities` without an access token
6. Verify the response status is `401 Unauthorized`

**Expected Result:**
- The backend starts successfully using the explicit `local` profile with actual PostgreSQL data
- Flyway validates/migrates the PostgreSQL schema
- A protected API endpoint responds with HTTP `401`, confirming the web server is running and security is active

**Actual Result:**

**Status:** NOT TESTED

## TC-STARTUP-004: Backend 209 profile starts with 209 PostgreSQL and Keycloak settings

**Preconditions:**
- Java 21 is installed
- PostgreSQL for the 209 environment is reachable, defaulting to `jdbc:postgresql://localhost:5433/cms`
- `DB_USERNAME` and `DB_PASSWORD` are exported for the 209 database
- Port `8080` is free before startup

**Steps:**
1. From the `backend/` directory, run `SPRING_PROFILES_ACTIVE=209 ./gradlew bootRun`
2. Wait for the application to finish starting
3. Verify the logs show the active profile is `209`
4. Verify the logs show PostgreSQL/Flyway startup for the 209 database
5. Send a GET request to `http://localhost:8080/api/v1/health`
6. Verify the response status is `200 OK`
7. Send a GET request to `http://localhost:8080/api/v1/specialities` without an access token
8. Verify the response status is `401 Unauthorized`

**Expected Result:**
- The backend starts successfully using the 209 PostgreSQL profile
- Flyway validates/migrates the PostgreSQL schema
- The health endpoint is public and protected APIs still require authentication

**Actual Result:**

**Status:** NOT TESTED

## TC-STARTUP-005: Backend 243 profile starts with 243 PostgreSQL and Keycloak settings

**Preconditions:**
- Java 21 is installed
- PostgreSQL is reachable at `jdbc:postgresql://172.17.3.133:5435/cmsdb`, or `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are exported for another PostgreSQL instance
- Port `8080` is free before startup

**Steps:**
1. From the `backend/` directory, run `SPRING_PROFILES_ACTIVE=243 ./gradlew bootRun`
2. Wait for the application to finish starting
3. Verify the logs show the active profile is `243`
4. Verify the logs show PostgreSQL/Flyway startup for the 243 database
5. Send a GET request to `http://localhost:8080/api/v1/health`
6. Verify the response status is `200 OK`
7. Send a GET request to `http://localhost:8080/api/v1/specialities` without an access token
8. Verify the response status is `401 Unauthorized`

**Expected Result:**
- The backend starts successfully using the 243 PostgreSQL profile
- Flyway validates/migrates the PostgreSQL schema
- The health endpoint is public and protected APIs still require authentication

**Actual Result:**

**Status:** NOT TESTED

## TC-STARTUP-002: Frontend compiles and serves successfully

**Preconditions:**
- Node.js and npm are installed
- Frontend dependencies are installed in `frontend/`
- Port `4300` is free before startup

**Steps:**
1. Run `npm run build` from `frontend/`
2. Verify the build completes without errors
3. Run `npm run start -- --port 4300` from `frontend/`
4. Open `http://localhost:4300` in a browser
5. Verify the application shell HTML is returned and the page loads

**Expected Result:**
- The frontend build completes successfully
- The Angular development server starts successfully on port `4300`
- The application root URL responds with HTTP `200 OK`

**Actual Result:**

**Status:** NOT TESTED

