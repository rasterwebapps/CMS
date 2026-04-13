# Manual Test Cases — Equipment & Inventory Management (R1-M4.2)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one lab exists in the system

---

## Equipment

### TC-EQUIP-001: Navigate to Equipment List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Equipment" in the sidebar navigation      |
| **Expected**| Equipment list page loads with columns: Name, Model, Lab, Category, Status, Purchase Date, Actions |

---

### TC-EQUIP-002: Search Equipment

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching equipment    |

---

### TC-EQUIP-003: Sort Equipment

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click any sortable column header                 |
| **Expected**| Table sorts by the clicked column; click again to reverse |

---

### TC-EQUIP-004: Add Equipment — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Equipment" button                     |
| **Expected**| Equipment form loads with title "Add Equipment"; Lab dropdown populated |

---

### TC-EQUIP-005: Add Equipment — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name, Lab, and Category fields |

---

### TC-EQUIP-006: Add Equipment — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, select Lab, select Category (e.g., COMPUTER), optionally fill Model, Serial Number, Status, Purchase Date, Purchase Cost, Warranty Expiry; click Create |
| **Expected**| Snackbar shows "Created"; redirected to equipment list; new entry visible |

---

### TC-EQUIP-007: Edit Equipment

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on an equipment row          |
| **Expected**| Form loads with title "Edit Equipment"; all fields pre-populated |

---

### TC-EQUIP-008: Update Equipment

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Modify fields and click "Update"                 |
| **Expected**| Snackbar shows "Updated"; redirected to equipment list; changes reflected |

---

### TC-EQUIP-009: Delete Equipment

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on an equipment row        |
| **Expected**| Confirmation dialog: "Delete Equipment" with item name; confirming removes the item |

---

### TC-EQUIP-010: Cancel Delete Equipment

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Cancel" in the delete confirmation dialog |
| **Expected**| Dialog closes; equipment remains in the list     |

---

### TC-EQUIP-011: Equipment Form — Back Navigation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the back arrow on the equipment form       |
| **Expected**| Navigated back to the equipment list             |

---

### TC-EQUIP-012: Equipment Status Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Status dropdown on the equipment form   |
| **Expected**| Options shown: AVAILABLE, IN_USE, UNDER_REPAIR, DAMAGED, DISPOSED |

---

### TC-EQUIP-013: Equipment Category Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Category dropdown on the equipment form |
| **Expected**| Options shown: COMPUTER, NETWORKING, ELECTRONICS, MECHANICAL, GENERAL |

---

### TC-EQUIP-014: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View equipment list when no equipment exists     |
| **Expected**| Table shows "No data available" message          |

---

## Inventory

### TC-INV-001: Navigate to Inventory List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Inventory" in the sidebar navigation      |
| **Expected**| Inventory list page loads with columns: Name, Lab, Category, Quantity, Unit, Min Stock, Actions |

---

### TC-INV-002: Search Inventory

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching inventory items |

---

### TC-INV-003: Add Inventory Item — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Inventory Item" button                |
| **Expected**| Inventory form loads with title "Add Inventory Item"; Lab dropdown populated |

---

### TC-INV-004: Add Inventory Item — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name, Lab, Category, Quantity, and Unit fields |

---

### TC-INV-005: Add Inventory Item — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, select Lab, select Category (e.g., CONSUMABLE), enter Quantity, select Unit (e.g., PIECES), optionally fill Min Stock Level, Location, Notes; click Create |
| **Expected**| Snackbar shows "Created"; redirected to inventory list; new entry visible |

---

### TC-INV-006: Edit Inventory Item

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on an inventory row          |
| **Expected**| Form loads with title "Edit Inventory Item"; all fields pre-populated |

---

### TC-INV-007: Update Inventory Item

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Modify fields and click "Update"                 |
| **Expected**| Snackbar shows "Updated"; redirected to inventory list; changes reflected |

---

### TC-INV-008: Delete Inventory Item

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on an inventory row        |
| **Expected**| Confirmation dialog: "Delete Inventory Item" with item name; confirming removes the item |

---

### TC-INV-009: Inventory Category Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Category dropdown on the inventory form |
| **Expected**| Options: CONSUMABLE, CHEMICAL, GLASSWARE, TOOL, ELECTRONIC_COMPONENT, STATIONERY, OTHER |

---

### TC-INV-010: Inventory Unit Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Unit dropdown on the inventory form     |
| **Expected**| Options: PIECES, LITERS, KILOGRAMS, METERS, BOXES, PACKETS, SETS |

---

### TC-INV-011: Inventory Form — Notes Field

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Enter text in the Notes textarea field           |
| **Expected**| Notes field accepts multi-line text up to 500 characters |

---

### TC-INV-012: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View inventory list when no items exist          |
| **Expected**| Table shows "No data available" message          |
