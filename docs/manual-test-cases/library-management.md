# Manual Test Cases — Library Management (R2-M3)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- At least one active student and one active faculty member exist in the system
- User with **LIBRARIAN** role logged in (unless a test specifies otherwise)
- Library Settings configured with at least the default values (loan days, fine rate, max books, max renewals)

---

## 1. Book Catalogue

### TC-LIB-BOOK-001: Navigate to Book Catalogue

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Book Catalogue" in the Library sidebar nav group |
| **Expected** | Book Catalogue list loads; summary cards show Total Books, Available, and Issued counts; table shows columns: Accession No., Title, Authors, Publisher, Shelf, Call No., Status, Actions |

---

### TC-LIB-BOOK-002: Summary card counts are accurate

| Field        | Value |
|--------------|-------|
| **Action**   | Load the Book Catalogue with known books in AVAILABLE and ISSUED status |
| **Expected** | Total, Available, and Issued summary card values match the actual counts in the table |

---

### TC-LIB-BOOK-003: Search by title

| Field        | Value |
|--------------|-------|
| **Action**   | Type a partial book title in the search field |
| **Expected** | Table filters in real-time; only rows whose title, authors, accession number, publisher, or call number contain the search string are shown |

---

### TC-LIB-BOOK-004: Filter by status

| Field        | Value |
|--------------|-------|
| **Action**   | Select "Available" from the Status filter dropdown |
| **Expected** | Only AVAILABLE books shown; count on summary card matches filtered rows |

---

### TC-LIB-BOOK-005: Filter by subject category

| Field        | Value |
|--------------|-------|
| **Action**   | Select a subject category from the Category filter dropdown |
| **Expected** | Only books in that category are shown |

---

### TC-LIB-BOOK-006: Combine search and filter

| Field        | Value |
|--------------|-------|
| **Action**   | Set Status = "Issued" and enter a search term |
| **Expected** | Only rows matching both criteria are shown |

---

### TC-LIB-BOOK-007: Clear filters

| Field        | Value |
|--------------|-------|
| **Action**   | With active filters, click the "Clear Filters" button |
| **Expected** | All filters reset; full book list is shown again |

---

### TC-LIB-BOOK-008: Sort by column

| Field        | Value |
|--------------|-------|
| **Action**   | Click the "Title" column header |
| **Expected** | Table sorts alphabetically by title; clicking again reverses the order |

---

### TC-LIB-BOOK-009: Empty state — no books

| Field        | Value |
|--------------|-------|
| **Action**   | Load the catalogue when no books exist (or apply a filter that matches nothing) |
| **Expected** | Empty state component shown; if no books at all, the action button navigates to Add Book; if filters are active, the action button clears filters |

---

### TC-LIB-BOOK-010: Add Book — navigate to form

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Add Book" button |
| **Expected** | Book form opens with title "Add Book"; all fields empty; Status defaults to "Available" |

---

### TC-LIB-BOOK-011: Add Book — required field validation

| Field        | Value |
|--------------|-------|
| **Action**   | Submit the Add Book form without filling any fields |
| **Expected** | Validation errors shown for Title and Authors (both required); form does not submit |

---

### TC-LIB-BOOK-012: Add Book — accession number async uniqueness check

| Field        | Value |
|--------------|-------|
| **Action**   | Enter an accession number that already exists, then blur the field |
| **Expected** | Async validation triggers (≈350 ms debounce); error "Accession number already exists" appears below the field; Save button remains disabled |

---

### TC-LIB-BOOK-013: Add Book — accession number left blank (auto-generate)

| Field        | Value |
|--------------|-------|
| **Action**   | Submit the form with Accession Number field empty but Title and Authors filled |
| **Expected** | Book is saved; backend auto-generates an accession number; toast "Book added successfully"; redirected to catalogue |

---

### TC-LIB-BOOK-014: Add Book — save with all fields

| Field        | Value |
|--------------|-------|
| **Action**   | Fill all fields (accession number, entry date, title, authors, publisher, year, edition, ISBN, call number, shelf, category, source, vendor, bill number, bill date, price, status, remarks) and click Save |
| **Expected** | Book saved; toast "Book added successfully"; new book visible in catalogue |

---

### TC-LIB-BOOK-015: Edit Book — form pre-populated

| Field        | Value |
|--------------|-------|
| **Action**   | Click the edit icon on any book in the catalogue |
| **Expected** | Book form opens with title "Edit Book"; all fields pre-filled with existing values |

---

### TC-LIB-BOOK-016: Edit Book — accession number uniqueness excludes self

| Field        | Value |
|--------------|-------|
| **Action**   | In edit mode, blur the Accession Number field without changing it |
| **Expected** | No uniqueness error shown (the validator correctly excludes the current book's ID) |

---

### TC-LIB-BOOK-017: Edit Book — save changes

| Field        | Value |
|--------------|-------|
| **Action**   | Modify the Title field and click Save |
| **Expected** | Book updated; toast "Book updated successfully"; updated title visible in catalogue |

---

### TC-LIB-BOOK-018: Delete Book — AVAILABLE status

| Field        | Value |
|--------------|-------|
| **Action**   | Click the delete icon on an AVAILABLE book; confirm in the dialog |
| **Expected** | Book deleted; toast "Book deleted successfully"; book no longer in catalogue |

---

### TC-LIB-BOOK-019: Delete Book — cancel dialog

| Field        | Value |
|--------------|-------|
| **Action**   | Click the delete icon on a book; click Cancel in the confirm dialog |
| **Expected** | Dialog closes; book remains in the catalogue; no toast shown |

---

### TC-LIB-BOOK-020: Delete Book — ISSUED status blocked

| Field        | Value |
|--------------|-------|
| **Action**   | Click the delete icon on an ISSUED book; confirm in the dialog |
| **Expected** | Error toast shown (e.g., "Cannot delete an issued book"); book remains in catalogue |

---

### TC-LIB-BOOK-021: Permission guard — non-LIBRARIAN cannot manage

| Field        | Value |
|--------------|-------|
| **Action**   | Log in as a role without `LIBRARY_CATALOGUE_MANAGE` permission; navigate to Book Catalogue |
| **Expected** | Add Book button is hidden; edit and delete action icons are hidden; list is visible (read-only) |

---

## 2. Issue Desk

### TC-LIB-ISSUE-001: Navigate to Issue Desk

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Issue Desk" in the Library sidebar nav |
| **Expected** | Issue Desk list loads; summary cards show Active Issues, Issued (ISSUED status), and Overdue counts; table shows columns: Accession No., Book Title, Member, Issued Date, Due Date, Returned Date, Status, Fine, Actions |

---

### TC-LIB-ISSUE-002: Summary card counts are accurate

| Field        | Value |
|--------------|-------|
| **Action**   | Load with known ISSUED and OVERDUE records |
| **Expected** | Active Issues = ISSUED + OVERDUE; Issued and Overdue counts match actual rows |

---

### TC-LIB-ISSUE-003: Search issues

| Field        | Value |
|--------------|-------|
| **Action**   | Type a student name or roll number in the search field |
| **Expected** | Table filters to show only matching issues (searches accession number, book title, student name, faculty name, roll number, employee code) |

---

### TC-LIB-ISSUE-004: Filter by issue status

| Field        | Value |
|--------------|-------|
| **Action**   | Select "Overdue" from the Status filter |
| **Expected** | Only OVERDUE issues shown |

---

### TC-LIB-ISSUE-005: Filter by member type

| Field        | Value |
|--------------|-------|
| **Action**   | Select "Faculty" from the Member Type filter |
| **Expected** | Only faculty-issued books shown |

---

### TC-LIB-ISSUE-006: Toggle Overdue Only

| Field        | Value |
|--------------|-------|
| **Action**   | Enable the "Overdue Only" toggle/checkbox |
| **Expected** | Table shows only records with status OVERDUE; toggle is additive with other filters |

---

### TC-LIB-ISSUE-007: Clear all filters

| Field        | Value |
|--------------|-------|
| **Action**   | With active filters applied, click "Clear Filters" |
| **Expected** | All filters reset; full issue list shown |

---

### TC-LIB-ISSUE-008: Issue Book — navigate to form

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Issue Book" button |
| **Expected** | Issue form opens; Member Type defaults to "Student"; Student dropdown populated with active students; accession number field empty |

---

### TC-LIB-ISSUE-009: Issue Book — look up valid available book

| Field        | Value |
|--------------|-------|
| **Action**   | Enter a valid accession number of an AVAILABLE book; click "Look Up" |
| **Expected** | Book details panel appears showing the book's title, authors, and shelf location |

---

### TC-LIB-ISSUE-010: Issue Book — look up non-existent accession number

| Field        | Value |
|--------------|-------|
| **Action**   | Enter a non-existent accession number; click "Look Up" |
| **Expected** | Error message: `No book found with accession number "X"` |

---

### TC-LIB-ISSUE-011: Issue Book — look up already-issued book

| Field        | Value |
|--------------|-------|
| **Action**   | Enter the accession number of an already-ISSUED book; click "Look Up" |
| **Expected** | Error message: `Book "X" is not available (status: ISSUED)` |

---

### TC-LIB-ISSUE-012: Issue Book — submit without selecting member

| Field        | Value |
|--------------|-------|
| **Action**   | Look up a valid book; leave the student dropdown empty; click Save |
| **Expected** | Error toast "Please select a student"; form does not submit |

---

### TC-LIB-ISSUE-013: Issue Book — issue to student (happy path)

| Field        | Value |
|--------------|-------|
| **Action**   | Look up valid book; select Member Type = Student; select a student; set Issued Date; click Save |
| **Expected** | Toast "Book issued successfully"; redirected to Issue Desk; new ISSUED record appears; book status in catalogue changes to ISSUED |

---

### TC-LIB-ISSUE-014: Issue Book — issue to faculty

| Field        | Value |
|--------------|-------|
| **Action**   | Select Member Type = Faculty; select a faculty member; look up valid book; click Save |
| **Expected** | Toast "Book issued successfully"; faculty name and employee code visible in the issue list |

---

### TC-LIB-ISSUE-015: Issue Book — max books exceeded

| Field        | Value |
|--------------|-------|
| **Action**   | Attempt to issue a book to a student who has already reached the max-books limit (as configured in Library Settings) |
| **Expected** | Backend returns error; error toast shown with the backend's message (e.g., "Student has reached maximum book limit"); issue not created |

---

### TC-LIB-ISSUE-016: Return Book — on-time return

| Field        | Value |
|--------------|-------|
| **Action**   | Click the Return icon on an ISSUED, non-overdue record; confirm in the dialog |
| **Expected** | Record status changes to RETURNED; no fine created; toast "Book returned successfully"; book status in catalogue changes back to AVAILABLE |

---

### TC-LIB-ISSUE-017: Return Book — overdue return with fine

| Field        | Value |
|--------------|-------|
| **Action**   | Click the Return icon on an OVERDUE record; read the confirm dialog message; confirm |
| **Expected** | Confirm dialog shows overdue days and fine warning; on confirm, toast includes fine amount and overdue days; record status becomes RETURNED; Fine entry created in Fine Management list |

---

### TC-LIB-ISSUE-018: Return Book — cancel dialog

| Field        | Value |
|--------------|-------|
| **Action**   | Click Return; click Cancel in the confirm dialog |
| **Expected** | Dialog closes; issue record unchanged |

---

### TC-LIB-ISSUE-019: Renew Book — happy path

| Field        | Value |
|--------------|-------|
| **Action**   | Click the Renew icon on an ISSUED record; confirm in the dialog |
| **Expected** | Toast "Book renewed. New due date: [date]"; due date in the table updates; renewal count increments |

---

### TC-LIB-ISSUE-020: Renew Book — max renewals exceeded

| Field        | Value |
|--------------|-------|
| **Action**   | Attempt to renew a book that has already reached the max renewals limit |
| **Expected** | Backend returns error; error toast shown; due date unchanged |

---

### TC-LIB-ISSUE-021: Renew Book — already returned

| Field        | Value |
|--------------|-------|
| **Action**   | Attempt to renew a RETURNED issue record |
| **Expected** | Error toast shown; renew action should not be visible (button hidden for non-active records) |

---

## 3. Fine Management

### TC-LIB-FINE-001: Navigate to Fine Management

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Fine Management" in the Library sidebar nav |
| **Expected** | Fine Management list loads; summary cards show Pending Fines (count), Pending Amount (₹), and Total Collected (₹); table shows: Accession No., Book Title, Member, Overdue Days, Fine Amount, Status, Resolved By, Actions |

---

### TC-LIB-FINE-002: Summary card totals are accurate

| Field        | Value |
|--------------|-------|
| **Action**   | Load with known PENDING and COLLECTED fines |
| **Expected** | Pending count and amount match only PENDING records; Collected amount matches only COLLECTED records |

---

### TC-LIB-FINE-003: Search fines

| Field        | Value |
|--------------|-------|
| **Action**   | Type a member name or accession number in the search field |
| **Expected** | Table filters to show only matching fines |

---

### TC-LIB-FINE-004: Filter by fine status

| Field        | Value |
|--------------|-------|
| **Action**   | Select "Waived" from the Status filter |
| **Expected** | Only WAIVED fines shown |

---

### TC-LIB-FINE-005: Filter by member type

| Field        | Value |
|--------------|-------|
| **Action**   | Select "Student" from the Member Type filter |
| **Expected** | Only student fines shown |

---

### TC-LIB-FINE-006: Waive fine — happy path

| Field        | Value |
|--------------|-------|
| **Action**   | Click the Waive icon on a PENDING fine; read the confirm dialog (shows member name and amount); confirm |
| **Expected** | Fine status changes to WAIVED; "Resolved By" column shows "Waived by [username]"; toast "Fine of ₹X waived for [member]" |

---

### TC-LIB-FINE-007: Collect fine — happy path

| Field        | Value |
|--------------|-------|
| **Action**   | Click the Collect icon on a PENDING fine; confirm in the dialog |
| **Expected** | Fine status changes to COLLECTED; "Resolved By" shows collected date; toast "₹X collected from [member]" |

---

### TC-LIB-FINE-008: Waive/Collect — cancel dialog

| Field        | Value |
|--------------|-------|
| **Action**   | Click Waive or Collect; click Cancel in the confirm dialog |
| **Expected** | Dialog closes; fine status unchanged |

---

### TC-LIB-FINE-009: Waive/Collect — non-PENDING fine blocked

| Field        | Value |
|--------------|-------|
| **Action**   | Attempt to waive or collect a fine already in WAIVED or COLLECTED status |
| **Expected** | Action icons hidden for resolved fines; if somehow triggered, backend returns error and error toast shown |

---

### TC-LIB-FINE-010: Clear filters

| Field        | Value |
|--------------|-------|
| **Action**   | Set status and member type filters; click "Clear Filters" |
| **Expected** | All filters reset; full fine list shown |

---

## 4. My Library (Student / Faculty Portal)

### TC-LIB-MY-001: Navigate to My Library

| Field        | Value |
|--------------|-------|
| **Action**   | Log in as a student or faculty; click "My Library" in the sidebar |
| **Expected** | My Library portal loads with three tabs: Active Issues, History, Search Catalogue |

---

### TC-LIB-MY-002: Active Issues tab — shows current borrows

| Field        | Value |
|--------------|-------|
| **Action**   | View "Active Issues" tab while a book is currently issued to the logged-in user |
| **Expected** | Each active issue shows: book title, accession number, issued date, due date, and status (ISSUED or OVERDUE) |

---

### TC-LIB-MY-003: Overdue banner shown

| Field        | Value |
|--------------|-------|
| **Action**   | Log in as a user with at least one OVERDUE issue |
| **Expected** | A warning banner or indicator is displayed on the Active Issues tab indicating overdue books |

---

### TC-LIB-MY-004: Active Issues tab — empty state

| Field        | Value |
|--------------|-------|
| **Action**   | Log in as a user with no active issues |
| **Expected** | Empty state message shown on the Active Issues tab |

---

### TC-LIB-MY-005: History tab — shows returned and lost books

| Field        | Value |
|--------------|-------|
| **Action**   | Switch to the "History" tab |
| **Expected** | RETURNED and LOST issues shown with returned dates and fine amounts if applicable |

---

### TC-LIB-MY-006: History tab — empty state

| Field        | Value |
|--------------|-------|
| **Action**   | View History tab for a user with no past issues |
| **Expected** | Empty state message shown |

---

### TC-LIB-MY-007: Search Catalogue tab — loads available books on first visit

| Field        | Value |
|--------------|-------|
| **Action**   | Click the "Search Catalogue" tab for the first time in a session |
| **Expected** | Catalogue loads (only AVAILABLE books); list displayed |

---

### TC-LIB-MY-008: Search Catalogue tab — search by title or author

| Field        | Value |
|--------------|-------|
| **Action**   | Type a partial title or author name in the search field |
| **Expected** | Catalogue filters in real-time; only matching books shown |

---

### TC-LIB-MY-009: Search Catalogue tab — filter by category

| Field        | Value |
|--------------|-------|
| **Action**   | Select a subject category from the dropdown |
| **Expected** | Only books in that category shown; combines with text search |

---

## 5. Journals / Periodicals

### TC-LIB-PER-001: Navigate to Journals/Periodicals

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Journals & Periodicals" in the Library sidebar nav |
| **Expected** | Periodicals list loads; summary cards show Total Entries and Active Subscriptions; table shows: Journal Name, Type, Volume/Issue, Year, Copies, Subscription Status, Received Date, Actions |

---

### TC-LIB-PER-002: Summary card counts are accurate

| Field        | Value |
|--------------|-------|
| **Action**   | Load with known periodicals |
| **Expected** | Total matches all rows; Active Subscriptions matches only ACTIVE records |

---

### TC-LIB-PER-003: Search periodicals

| Field        | Value |
|--------------|-------|
| **Action**   | Type a journal name or organization in the search field |
| **Expected** | Table filters to matching rows |

---

### TC-LIB-PER-004: Filter by journal type

| Field        | Value |
|--------------|-------|
| **Action**   | Select "National" or "International" from the Type filter |
| **Expected** | Only periodicals of that type shown |

---

### TC-LIB-PER-005: Filter by subscription status

| Field        | Value |
|--------------|-------|
| **Action**   | Select "Expired" from the Status filter |
| **Expected** | Only expired-subscription periodicals shown |

---

### TC-LIB-PER-006: Clear filters

| Field        | Value |
|--------------|-------|
| **Action**   | With filters active, click "Clear Filters" |
| **Expected** | Full list restored |

---

### TC-LIB-PER-007: Add Periodical — navigate to form

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Add Periodical" button |
| **Expected** | Periodical form opens with empty fields |

---

### TC-LIB-PER-008: Add Periodical — required field validation

| Field        | Value |
|--------------|-------|
| **Action**   | Submit the form without filling required fields |
| **Expected** | Validation errors shown for required fields; form does not submit |

---

### TC-LIB-PER-009: Add Periodical — save (happy path)

| Field        | Value |
|--------------|-------|
| **Action**   | Fill all required fields and click Save |
| **Expected** | Toast "Periodical added successfully"; redirected to periodicals list; new entry visible |

---

### TC-LIB-PER-010: Edit Periodical — form pre-populated

| Field        | Value |
|--------------|-------|
| **Action**   | Click the edit icon on any periodical |
| **Expected** | Form opens with all existing values pre-filled |

---

### TC-LIB-PER-011: Edit Periodical — save changes

| Field        | Value |
|--------------|-------|
| **Action**   | Modify the Copies Count field; click Save |
| **Expected** | Toast "Periodical updated successfully"; updated value visible in list |

---

### TC-LIB-PER-012: Delete Periodical — confirm

| Field        | Value |
|--------------|-------|
| **Action**   | Click the delete icon on a periodical; confirm in the dialog |
| **Expected** | Toast "Periodical entry deleted"; entry removed from list |

---

### TC-LIB-PER-013: Delete Periodical — cancel

| Field        | Value |
|--------------|-------|
| **Action**   | Click delete; click Cancel in the dialog |
| **Expected** | Dialog closes; entry remains |

---

### TC-LIB-PER-014: Permission guard — non-LIBRARIAN cannot manage

| Field        | Value |
|--------------|-------|
| **Action**   | Log in as a role without `LIBRARY_PERIODICAL_MANAGE`; navigate to Periodicals |
| **Expected** | Add button and action icons hidden; list visible (read-only) |

---

## 6. Reports

### TC-LIB-RPT-001: Navigate to Reports

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Reports" in the Library sidebar nav |
| **Expected** | Reports page loads; four tabs visible: Overdue, Fines Summary, Issue History, Accession Register; Overdue tab active by default |

---

### TC-LIB-RPT-002: Overdue tab — shows overdue issues

| Field        | Value |
|--------------|-------|
| **Action**   | View the Overdue tab with known overdue records |
| **Expected** | All OVERDUE issues listed with member name, roll/employee code, book title, due date, and overdue days |

---

### TC-LIB-RPT-003: Overdue tab — empty state

| Field        | Value |
|--------------|-------|
| **Action**   | View Overdue tab when no books are overdue |
| **Expected** | Empty state message shown |

---

### TC-LIB-RPT-004: Fines Summary tab

| Field        | Value |
|--------------|-------|
| **Action**   | Click the "Fines Summary" tab |
| **Expected** | Loads issues that have associated fines; summary shows Total Fine Amount and Pending Amount; each row shows fine status |

---

### TC-LIB-RPT-005: Issue History tab — default load

| Field        | Value |
|--------------|-------|
| **Action**   | Click the "Issue History" tab |
| **Expected** | All issue records loaded; table shows accession number, book title, member, issued date, due date, returned date, status |

---

### TC-LIB-RPT-006: Issue History tab — filter by status

| Field        | Value |
|--------------|-------|
| **Action**   | Select "Returned" from the Status filter; click "Apply" |
| **Expected** | Only RETURNED issues shown; table refreshes |

---

### TC-LIB-RPT-007: Issue History tab — filter by member type

| Field        | Value |
|--------------|-------|
| **Action**   | Select "Faculty" from the Member Type filter; click "Apply" |
| **Expected** | Only faculty issues shown |

---

### TC-LIB-RPT-008: Issue History tab — search within results

| Field        | Value |
|--------------|-------|
| **Action**   | Type a name in the search field |
| **Expected** | Results filtered client-side to matching rows |

---

### TC-LIB-RPT-009: Accession Register tab — default load

| Field        | Value |
|--------------|-------|
| **Action**   | Click the "Accession Register" tab |
| **Expected** | All books loaded in accession register format; columns: Accession No., Entry Date, Title, Authors, Publisher, Year, Edition, Call No., Shelf, Source, Price (₹), Status |

---

### TC-LIB-RPT-010: Accession Register tab — filter by category

| Field        | Value |
|--------------|-------|
| **Action**   | Select a subject category from the filter; click "Apply" |
| **Expected** | Only books of that category shown |

---

### TC-LIB-RPT-011: Accession Register — print

| Field        | Value |
|--------------|-------|
| **Action**   | Click the "Print" button on the Accession Register tab |
| **Expected** | Browser print dialog opens; print layout hides navigation elements |

---

## 7. Book Import

### TC-LIB-IMP-001: Navigate to Book Import

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Book Import" in the Library sidebar nav |
| **Expected** | Import wizard loads showing Step 1 (Template); description of the expected Excel format visible; "Download Template" button present |

---

### TC-LIB-IMP-002: Download import template

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Download Template" |
| **Expected** | Excel file downloaded; wizard advances to Step 2 (Upload); template contains the correct column headers |

---

### TC-LIB-IMP-003: Upload file — select valid Excel file

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Choose File" and select a valid `.xlsx` file |
| **Expected** | File name displayed in the UI; "Validate" button enabled |

---

### TC-LIB-IMP-004: Validate — valid file with no errors

| Field        | Value |
|--------------|-------|
| **Action**   | Upload a correctly filled template; click "Validate" |
| **Expected** | Wizard advances to Step 3 (Result); validation result shows 0 errors, 0 warnings; "Import" button enabled |

---

### TC-LIB-IMP-005: Validate — file with row errors

| Field        | Value |
|--------------|-------|
| **Action**   | Upload a file with some rows missing required fields (e.g., empty Title); click "Validate" |
| **Expected** | Result shows list of ERROR rows with row number and field description; warnings shown separately; "Import" button enabled only if Skip Errored Rows is checked |

---

### TC-LIB-IMP-006: Validate — file with warnings only

| Field        | Value |
|--------------|-------|
| **Action**   | Upload a file where some rows have duplicate accession numbers (warning-level) |
| **Expected** | Result shows 0 errors, N warnings; "Import" button enabled |

---

### TC-LIB-IMP-007: Skip Errored Rows toggle

| Field        | Value |
|--------------|-------|
| **Action**   | After a validation result with errors, uncheck "Skip Errored Rows"; click "Import" |
| **Expected** | Import is blocked or backend rejects; errored rows are not imported; success count reflects only valid rows |

---

### TC-LIB-IMP-008: Execute import — happy path

| Field        | Value |
|--------------|-------|
| **Action**   | Validate a valid file; click "Import" |
| **Expected** | Toast "N books imported successfully"; result screen shows booksImported count; "View Catalogue" button navigates to Book Catalogue; newly imported books appear in catalogue |

---

### TC-LIB-IMP-009: Execute import — partial success with errored rows skipped

| Field        | Value |
|--------------|-------|
| **Action**   | Validate a file with some errors and Skip Errored Rows checked; click "Import" |
| **Expected** | Valid rows imported; toast shows successful count; result shows error rows that were skipped |

---

### TC-LIB-IMP-010: Reset / Start Over

| Field        | Value |
|--------------|-------|
| **Action**   | After a completed import, click "Import More" or "Reset" |
| **Expected** | Wizard resets to Step 1; file selection cleared; previous results cleared |

---

## 8. Library Settings

### TC-LIB-SET-001: Navigate to Library Settings

| Field        | Value |
|--------------|-------|
| **Action**   | Click "Library Settings" in the Library sidebar nav |
| **Expected** | Settings form loads; current values populated for: Student Loan Days, Faculty Loan Days, Student Max Books, Faculty Max Books, Fine Per Day (₹), Max Renewals |

---

### TC-LIB-SET-002: Default values loaded from backend

| Field        | Value |
|--------------|-------|
| **Action**   | View the settings form on first load |
| **Expected** | All 6 fields show the values saved in the database, not just the form's default fallback values |

---

### TC-LIB-SET-003: Validation — required fields

| Field        | Value |
|--------------|-------|
| **Action**   | Clear all fields and click Save |
| **Expected** | Validation errors shown for all 6 required fields; form does not submit |

---

### TC-LIB-SET-004: Validation — numeric bounds

| Field        | Value |
|--------------|-------|
| **Action**   | Enter 0 for Student Loan Days; enter 25 for Student Max Books (max is 20); click Save |
| **Expected** | Validation errors shown for out-of-range fields; form does not submit |

---

### TC-LIB-SET-005: Save settings — happy path

| Field        | Value |
|--------------|-------|
| **Action**   | Change Student Loan Days to 21 and click Save |
| **Expected** | Toast "Library settings saved"; on page reload, the new value is loaded from backend |

---

### TC-LIB-SET-006: Settings affect issue behaviour

| Field        | Value |
|--------------|-------|
| **Action**   | Set Student Max Books to 1; attempt to issue a second book to a student who already has 1 active issue |
| **Expected** | Backend rejects the issue with a max-books error; error toast shown |

---

## 9. Role-Based Access Control

### TC-LIB-RBAC-001: LIBRARIAN role sees full library nav group

| Field        | Value |
|--------------|-------|
| **Action**   | Log in as a user with the LIBRARIAN role |
| **Expected** | Library nav group visible with all sub-links: Book Catalogue, Issue Desk, Fine Management, Journals & Periodicals, Reports, Book Import, Library Settings |

---

### TC-LIB-RBAC-002: Student/Faculty role sees only My Library

| Field        | Value |
|--------------|-------|
| **Action**   | Log in as a student or faculty member (without LIBRARIAN role) |
| **Expected** | Only "My Library" link visible under the Library nav group; librarian-only screens not accessible |

---

### TC-LIB-RBAC-003: Direct URL access without permission is blocked

| Field        | Value |
|--------------|-------|
| **Action**   | As a non-LIBRARIAN user, navigate directly to `/library/books` |
| **Expected** | Access denied or redirected to the dashboard; book catalogue not rendered |

---

### TC-LIB-RBAC-004: Admin role can access library screens

| Field        | Value |
|--------------|-------|
| **Action**   | Log in as a user with DEV_ADMIN or COLLEGE_ADMIN role |
| **Expected** | Full Library nav group accessible; all screens load correctly |
