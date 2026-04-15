# Referral Type Management — Manual Test Cases

## TC-RT-001: List all referral types

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Send a GET request to `/api/v1/referral-types`
2. Verify the response status is 200 OK

**Expected Result:**
- A list of referral types is returned including seeded types (WALK_IN, PHONE, ONLINE, AGENT_REFERRAL, STAFF, ALUMNI, PARENT, ADVERTISEMENT)

**Status:** NOT TESTED

---

## TC-RT-002: List active referral types only

**Preconditions:**
- User is logged in
- At least one active and one inactive referral type exist

**Steps:**
1. Send a GET request to `/api/v1/referral-types?activeOnly=true`
2. Verify the response status is 200 OK

**Expected Result:**
- Only active referral types are returned

**Status:** NOT TESTED

---

## TC-RT-003: Create a referral type

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Send a POST request to `/api/v1/referral-types` with body:
   ```json
   {
     "name": "Corporate Partner",
     "code": "CORPORATE",
     "guidelineValue": 10000.00,
     "description": "Corporate partnership referral",
     "isActive": true
   }
   ```
2. Verify the response status is 201 Created

**Expected Result:**
- Referral type is created with the given guideline value

**Status:** NOT TESTED

---

## TC-RT-004: Update a referral type guideline value

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A referral type with code "STAFF" exists

**Steps:**
1. GET the STAFF referral type to find its ID
2. Send a PUT request to `/api/v1/referral-types/{id}` with updated guidelineValue: 5000.00
3. Verify the response shows updated value

**Expected Result:**
- Guideline value is updated. This value will appear as additional amount in enquiry forms

**Status:** NOT TESTED

---

## TC-RT-005: Prevent duplicate referral type codes

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A referral type with code "STAFF" already exists

**Steps:**
1. Send a POST request to create another referral type with code "STAFF"

**Expected Result:**
- Request returns 400 Bad Request with error message about duplicate code

**Status:** NOT TESTED

---

## TC-RT-006: Delete a referral type

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A referral type exists

**Steps:**
1. Send a DELETE request to `/api/v1/referral-types/{id}`
2. Verify 204 No Content response

**Expected Result:**
- Referral type is deleted

**Status:** NOT TESTED
