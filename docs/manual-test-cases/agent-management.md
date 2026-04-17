# Agent Management — Manual Test Cases

## TC-AGENT-001: List all agents

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Send a GET request to `/api/v1/agents`
2. Verify the response status is 200 OK
3. Verify the response contains a list of agents

**Expected Result:**
- A list of agents is returned, each containing id, name, phone, email, area, locality, isActive, createdAt, and updatedAt

**Status:** NOT TESTED

---

## TC-AGENT-002: Create a new agent

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Send a POST request to `/api/v1/agents` with body:
   ```json
   {
     "name": "John Agent",
     "phone": "9876543210",
     "email": "john.agent@example.com",
     "area": "Salem",
     "locality": "Urban",
     "isActive": true
   }
   ```
2. Verify the response status is 201 Created
3. Verify the returned object has the correct values

**Expected Result:**
- Agent is created and returned with an ID

**Status:** NOT TESTED

---

## TC-AGENT-003: Get agent by ID

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one agent exists

**Steps:**
1. Send a GET request to `/api/v1/agents/{id}` with a valid agent ID
2. Verify the response status is 200 OK

**Expected Result:**
- The agent details are returned

**Status:** NOT TESTED

---

## TC-AGENT-004: Update an agent

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An agent exists

**Steps:**
1. Send a PUT request to `/api/v1/agents/{id}` with updated body
2. Verify the response status is 200 OK
3. Verify the agent fields are updated

**Expected Result:**
- Agent is updated successfully

**Status:** NOT TESTED

---

## TC-AGENT-005: Delete an agent

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An agent exists

**Steps:**
1. Send a DELETE request to `/api/v1/agents/{id}`
2. Verify the response status is 204 No Content
3. Verify the agent is no longer returned in GET list

**Expected Result:**
- Agent is deleted

**Status:** NOT TESTED

---

## TC-AGENT-006: Create agent commission guideline

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An agent and a program exist

**Steps:**
1. Send a POST request to `/api/v1/agent-commission-guidelines` with body:
   ```json
   {
     "agentId": 1,
     "programId": 1,
     "localityType": "URBAN",
     "suggestedCommission": 5000.00
   }
   ```
2. Verify the response status is 201 Created

**Expected Result:**
- Commission guideline is created linking the agent, program, and locality

**Status:** NOT TESTED

---

## TC-AGENT-007: List commission guidelines by agent

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Commission guidelines exist for an agent

**Steps:**
1. Send a GET request to `/api/v1/agent-commission-guidelines?agentId={id}`
2. Verify the response contains guidelines for that agent

**Expected Result:**
- A list of commission guidelines for the specified agent is returned

**Status:** NOT TESTED

---

## TC-AGENT-008: Frontend — Agent list page

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Navigate to `/agents`
2. Verify the agent list page loads with a table
3. Verify search/filter functionality works

**Expected Result:**
- Agents are displayed in a sortable, filterable table

**Status:** NOT TESTED

---

## TC-AGENT-009: Frontend — Agent form (create/edit)

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Navigate to `/agents/new`
2. Fill in agent name, phone, email, area, locality
3. Click Save
4. Verify redirect to agent list
5. Verify the new agent appears

**Expected Result:**
- New agent is created and shown in the list

**Status:** NOT TESTED

---

## TC-AGENT-010: Return 404 for non-existent agent

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Send a GET request to `/api/v1/agents/99999`
2. Verify the response status is 404 Not Found

**Expected Result:**
- A 404 error response is returned

**Status:** NOT TESTED

---

## TC-AGENT-011: Create an agent with allottedSeats

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Navigate to **Agents** from the sidebar
2. Click **Add Agent**
3. Fill in Name: "Test Agent", Phone: "9876543210", Area: "Salem", Allotted Seats: `30`
4. Click **Create**
5. Verify a success message appears
6. Navigate back to the Agents list
7. Verify "Test Agent" appears with **Allotted Seats** column showing `30`

**Expected Result:**
- Agent is created with allottedSeats = 30
- The Agents table shows the Allotted Seats column

**Status:** NOT TESTED

---

## TC-AGENT-012: Allotted Seats is optional

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Create an agent without entering a value in Allotted Seats
2. Verify the agent is saved successfully
3. In the Agents list, verify the Allotted Seats column shows `—` for that agent

**Expected Result:**
- Agent is created successfully without allottedSeats
- List shows `—` for agents with no allotted seats

**Status:** NOT TESTED
