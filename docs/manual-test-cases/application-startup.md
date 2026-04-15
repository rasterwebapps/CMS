# Manual Test Cases — Application Startup

## TC-STARTUP-001: Backend starts on the local profile and exposes the health endpoint

**Preconditions:**
- Java 21 is installed
- From the `backend/` directory, dependencies can be resolved by Gradle
- Port `8080` is free before startup

**Steps:**
1. Run the backend with `./gradlew bootRun` from `backend/`
2. Wait for the application to finish starting
3. Send a GET request to `http://localhost:8080/api/v1/health`
4. Verify the response status is `200 OK`
5. Verify the response body contains `{"status":"UP"}`

**Expected Result:**
- The backend starts successfully on port `8080`
- The health endpoint responds with HTTP `200` and the JSON body `{"status":"UP"}`

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

