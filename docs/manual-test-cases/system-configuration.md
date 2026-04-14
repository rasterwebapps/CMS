# System Configuration Management — Manual Test Cases

## TC-SYSCONF-001: List all system configurations

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Send a GET request to `/api/v1/system-configurations`
2. Verify the response status is 200 OK
3. Verify the response contains a list of configuration entries

**Expected Result:**
- A list of system configurations is returned, each containing id, configKey, configValue, category, description, dataType, isEditable, createdAt, and updatedAt

**Status:** NOT TESTED

---

## TC-SYSCONF-002: Get a system configuration by ID

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one system configuration exists

**Steps:**
1. Send a GET request to `/api/v1/system-configurations/{id}` with a valid configuration ID
2. Verify the response status is 200 OK

**Expected Result:**
- The system configuration details are returned

**Status:** NOT TESTED

---

## TC-SYSCONF-003: Get a system configuration by key

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A configuration with the given key exists (e.g., "college.name")

**Steps:**
1. Send a GET request to `/api/v1/system-configurations/key/college.name`
2. Verify the response status is 200 OK

**Expected Result:**
- The system configuration matching the key is returned

**Status:** NOT TESTED

---

## TC-SYSCONF-004: Create a new system configuration

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Send a POST request to `/api/v1/system-configurations` with body:
   ```json
   {
     "configKey": "app.custom.setting",
     "configValue": "enabled",
     "category": "Application",
     "description": "Custom application setting",
     "dataType": "STRING",
     "isEditable": true
   }
   ```
2. Verify the response status is 201 Created
3. Verify the returned object has the correct values

**Expected Result:**
- Configuration is created and returned with an ID

**Status:** NOT TESTED

---

## TC-SYSCONF-005: Update an existing system configuration

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A system configuration exists and is editable

**Steps:**
1. Send a PUT request to `/api/v1/system-configurations/{id}` with updated body
2. Verify the response status is 200 OK
3. Verify the configuration value is updated

**Expected Result:**
- Configuration is updated successfully

**Status:** NOT TESTED

---

## TC-SYSCONF-006: Delete a system configuration

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A system configuration exists

**Steps:**
1. Send a DELETE request to `/api/v1/system-configurations/{id}`
2. Verify the response status is 204 No Content
3. Send a GET request for the same ID
4. Verify the response status is 404 Not Found

**Expected Result:**
- Configuration is deleted and no longer accessible

**Status:** NOT TESTED

---

## TC-SYSCONF-007: Frontend — Settings list page

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Navigate to `/settings`
2. Verify the settings list page loads
3. Verify configurations are displayed in a table with search/filter

**Expected Result:**
- System configurations are displayed in a sortable, filterable table

**Status:** NOT TESTED

---

## TC-SYSCONF-008: Frontend — Create/Edit configuration form

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Navigate to `/settings/new`
2. Fill in configuration key, value, category, description
3. Click Save
4. Verify redirect to settings list
5. Verify the new configuration appears in the list

**Expected Result:**
- New configuration is created and shown in the list

**Status:** NOT TESTED

---

## TC-SYSCONF-009: Return 404 for non-existent configuration

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Send a GET request to `/api/v1/system-configurations/99999`
2. Verify the response status is 404 Not Found

**Expected Result:**
- A 404 error response is returned

**Status:** NOT TESTED
