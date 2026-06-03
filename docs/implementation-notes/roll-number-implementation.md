# Roll Number Generation — Implementation Notes

See `docs/ROLL_NUMBER_GENERATION_GUIDE.md` for usage guide and API documentation.

---

## What Was Implemented

### Database (V111 Migration)
- `roll_number_code` column on `courses` table (2-digit course code)
- `roll_number_sequences` table (per-course/per-year sequence tracking)
- System configuration key `ROLL_NUMBER_COLLEGE_CODE` (default: `"959"`)

### Backend
- `RollNumberSequence.java` — entity
- `RollNumberSequenceRepository.java` — with `@Lock(PESSIMISTIC_WRITE)` for thread safety
- `RollNumberGeneratorService.java` — generate, assign, preview
- `GenerateRollNumbersRequest.java` / `RollNumberAssignment.java` — DTOs
- `Course.java` / `CourseRequest.java` / `CourseResponse.java` — added `rollNumberCode` field
- `CourseService.java` — handle `rollNumberCode` in CRUD
- `StudentController.java` — added `POST /students/generate-roll-numbers` and `POST /students/preview-roll-numbers`

### Roll Number Format
```
[CollegeCode(3)][CourseCode(2)][Year(4)][Sequence(3)]
Example: 959652026004
```

### Key Features
- Alphabetic sort before assignment (roll 001 = first alphabetically)
- Preview mode (no DB write)
- Thread-safe via pessimistic DB locking
- Max 999 students per course per year

---

## Remaining Tasks (Frontend)
- Roll Number Assignment screen with preview + confirm flow
- Unit tests for `RollNumberGeneratorService`
- Manual test cases: `docs/manual-test-cases/roll-number-generation.md`
