# Country Location Master — Manual Test Cases

## Module: Country → State → District Hierarchy

---

## TC-LOC-001: View Location Master screen

**Preconditions:**
- User logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`
- Application running (V158 migration applied)

**Steps:**
1. Navigate to Masters → Location Master in the sidebar.
2. Observe the page header.
3. Check the meta stats row.

**Expected Result:**
- Title shows "Location Master".
- Stats show: Countries count, States/UTs count, Active count.
- India appears as the first country card (expanded by default).
- India has all 28 states + 8 UTs listed inside.

**Status:** NOT TESTED

---

## TC-LOC-002: India country is pre-seeded with all states

**Preconditions:**
- V158 migration has run on PostgreSQL.

**Steps:**
1. Open Location Master (card view).
2. Expand the India (IN) country card.
3. Count the states displayed.

**Expected Result:**
- India is listed with isoCode = `IN`.
- All 36 states/UTs from V150 seed are linked to India (e.g. Tamil Nadu, Maharashtra, Delhi, etc.).
- Clicking a state expands it to show its districts.

**Status:** NOT TESTED

---

## TC-LOC-003: Add a new country

**Preconditions:**
- User has `INDIA_LOCATION_MANAGE` permission.

**Steps:**
1. Click **Add Country** button in Location Master header.
2. Fill in: Name = `United States`, ISO Code = `US`.
3. Toggle Active = ON.
4. Click **Create Country**.

**Expected Result:**
- Country created successfully. Toast: "Country created".
- Redirected to Location Master.
- "United States (US)" appears as a new country card.
- Stats show 2 Countries.

**Status:** NOT TESTED

---

## TC-LOC-004: Duplicate ISO code is rejected

**Preconditions:**
- India (IN) already exists.

**Steps:**
1. Click Add Country.
2. Enter Name = `Bharat`, ISO Code = `IN`.
3. Click Create Country.

**Expected Result:**
- Error toast: "ISO code 'IN' already exists"
- Form stays open; country is not saved.

**Status:** NOT TESTED

---

## TC-LOC-005: Add a state under a non-India country

**Preconditions:**
- "United States (US)" country exists (created in TC-LOC-003).
- User has `INDIA_LOCATION_MANAGE` permission.

**Steps:**
1. In Location Master (card view), expand the United States country card.
2. Click the **+** (Add State) icon next to United States.
3. Verify the Country dropdown is pre-selected to "United States".
4. Fill in: Name = `California`, Code = `CA`.
5. Click **Create State**.

**Expected Result:**
- State "California" is created under United States.
- Toast: "State created".
- In Location Master, expanding United States shows "California (CA)".

**Status:** NOT TESTED

---

## TC-LOC-006: Add a state under India (default behaviour)

**Preconditions:**
- User has `INDIA_LOCATION_MANAGE` permission.

**Steps:**
1. Click **Add State / UT** button in Location Master header.
2. Verify Country dropdown defaults to "India (IN)".
3. Fill in: Name = `Ladakh`, Code = `LD`.
4. Click **Create State**.

**Expected Result:**
- State "Ladakh (LD)" created under India.
- Toast: "State created".
- Appears in India's accordion in Location Master.

**Status:** NOT TESTED

---

## TC-LOC-007: Duplicate state name within same country is rejected

**Preconditions:**
- "Tamil Nadu (TN)" already exists under India.

**Steps:**
1. Add State → Country = India, Name = `Tamil Nadu`, Code = `TN2`.
2. Click Create State.

**Expected Result:**
- Error: "State 'Tamil Nadu' already exists in India".
- Form stays open; state is not saved.

**Status:** NOT TESTED

---

## TC-LOC-008: Same state name allowed in different countries

**Preconditions:**
- "California (CA)" exists under United States.

**Steps:**
1. Add State → Country = India, Name = `California`, Code = `CL`.
2. Click Create State.

**Expected Result:**
- State created successfully (different country, no conflict).
- California appears in both India's and United States' state lists.

**Status:** NOT TESTED

---

## TC-LOC-009: Edit a country

**Preconditions:**
- United States country exists.

**Steps:**
1. Click Edit icon on the United States country card.
2. Change Name to `United States of America`.
3. Click Update Country.

**Expected Result:**
- Country updated. Toast: "Country updated".
- Card now shows "United States of America (US)".

**Status:** NOT TESTED

---

## TC-LOC-010: Delete a non-India country

**Preconditions:**
- A country "Test Country (TC)" exists with no states.

**Steps:**
1. Click Delete on the Test Country card.
2. Confirm in the dialog.

**Expected Result:**
- Country deleted. Toast: "Country deleted".
- Test Country no longer appears in the list.

**Status:** NOT TESTED

---

## TC-LOC-011: India country cannot be deleted (guard)

**Preconditions:**
- Logged in as ROLE_ADMIN.

**Steps:**
1. In Location Master, observe the India country card.
2. Check the action buttons.

**Expected Result:**
- No Delete button is shown for India (ISO code = IN).
- Edit button is present.

**Status:** NOT TESTED

---

## TC-LOC-012: Table view shows Country column

**Preconditions:**
- At least 2 countries with states exist.

**Steps:**
1. In Location Master, switch to Table view.
2. Observe the columns.

**Expected Result:**
- Columns: State/UT, Code, Country, Districts, Status, Actions.
- Country column shows ISO badge + country name for each state.
- States under India show "IN India"; states under USA show "US United States".

**Status:** NOT TESTED

---

## TC-LOC-013: Country → State cascade in shared selector

**Preconditions:**
- United States with state "California" exists.
- A form uses `<cms-country-state-district-selector>`.

**Steps:**
1. Open a form that uses the 3-level selector.
2. Observe default country is India.
3. Change country to United States.
4. Observe state dropdown.

**Expected Result:**
- State dropdown clears and reloads with USA states.
- District dropdown clears.
- After selecting California, district dropdown loads California's districts (if any).

**Status:** NOT TESTED

---

## TC-LOC-014: API endpoint — GET /api/v1/india/countries

**Steps:**
1. `GET /api/v1/india/countries` (no auth for read endpoints).

**Expected Result:**
- 200 OK, JSON array, first entry: `{"id":1,"name":"India","isoCode":"IN","isActive":true,...}`.

**Status:** NOT TESTED

---

## TC-LOC-015: API endpoint — GET /api/v1/india/countries/{id}/states

**Steps:**
1. `GET /api/v1/india/countries/1/states?activeOnly=true`.

**Expected Result:**
- 200 OK, array of 36 Indian states/UTs, each with `countryId=1`, `countryName="India"`, `countryIsoCode="IN"`.

**Status:** NOT TESTED

