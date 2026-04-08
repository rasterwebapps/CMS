# Manual Test Cases

This directory contains manual test case documentation for each completed feature/module in the College Management System.

## Structure

Each file corresponds to a module or feature area:

- `{module-name}.md` — e.g., `department-management.md`, `student-management.md`

## Template

Use the following template when creating test cases for a completed task:

```markdown
## TC-{MODULE}-{NUMBER}: {Title}

**Preconditions:**
- {List any required setup}

**Steps:**
1. {Step 1}
2. {Step 2}
3. {Step 3}

**Expected Result:**
- {What should happen}

**Status:** NOT TESTED
```

## Guidelines

1. Every completed task from the milestone tracker must have corresponding manual test cases.
2. Name files after the module or feature being tested.
3. Include test cases in the pull request that completes the task.
4. Use the `TC-{MODULE}-{NUMBER}` format for test case IDs (e.g., `TC-DEPT-001`).

## Status Legend

| Status | Meaning |
|--------|---------|
| NOT TESTED | Test case has been written but not yet executed |
| PASS | Test case passed during manual verification |
| FAIL | Test case failed during manual verification |
