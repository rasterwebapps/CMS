package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.BulkRollNumberAssignmentRequest;
import com.cms.dto.GenerateRollNumbersRequest;
import com.cms.dto.ProgramTransferAnalysis;
import com.cms.dto.ProgramTransferRecord;
import com.cms.dto.ProgramTransferRequest;
import com.cms.dto.RollNumberAssignment;
import com.cms.dto.RollNumberAssignmentRequest;
import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.model.enums.StudentStatus;
import com.cms.service.RollNumberGeneratorService;
import com.cms.service.StudentExportService;
import com.cms.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;
    private final RollNumberGeneratorService rollNumberGeneratorService;
    private final StudentExportService studentExportService;

    public StudentController(StudentService studentService,
                             RollNumberGeneratorService rollNumberGeneratorService,
                             StudentExportService studentExportService) {
        this.studentService = studentService;
        this.rollNumberGeneratorService = rollNumberGeneratorService;
        this.studentExportService = studentExportService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('STUDENT_CREATE')")
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> findAll(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String labBatch,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) String feeStatus,
            @RequestParam(required = false) Boolean activeOnly) {
        List<StudentResponse> students;
        if (programId != null && status != null) {
            // Dual filter: program + status
            students = studentService.findByProgramId(programId).stream()
                .filter(s -> s.status() == status)
                .toList();
        } else if (programId != null) {
            students = studentService.findByProgramId(programId);
        } else if (status != null) {
            students = studentService.findByStatus(status);
        } else if (labBatch != null) {
            students = studentService.findByLabBatch(labBatch);
        } else if (academicYearId != null || feeStatus != null) {
            // Explorer mode: supports academicYear + feeStatus filters with enrichment
            students = studentService.findExplorer(academicYearId, feeStatus);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            students = studentService.findByStatus(StudentStatus.ACTIVE);
        } else {
            students = studentService.findAll();
        }
        return ResponseEntity.ok(students);
    }

    @GetMapping("/explorer")
    public ResponseEntity<Page<StudentResponse>> findExplorer(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "admissionNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(studentService.findExplorer(
            programId, courseId, academicYearId, status, studentType, search, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('STUDENT_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentType,
            @RequestParam(required = false) String search) {

        List<StudentResponse> data = studentService.findExplorerAll(
            programId, courseId, academicYearId, status, studentType, search);

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = studentExportService.toPdf(data);
                String filename = "students-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = studentExportService.toExcel(data);
                String filename = "students-" + LocalDate.now() + ".xlsx";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/without-roll-number")
    public ResponseEntity<List<StudentResponse>> findWithoutRollNumber(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long programId) {
        return ResponseEntity.ok(studentService.findStudentsWithoutRollNumber(courseId, programId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> findById(@PathVariable Long id) {
        StudentResponse response = studentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/roll-number/{rollNumber}")
    public ResponseEntity<StudentResponse> findByRollNumber(@PathVariable String rollNumber) {
        StudentResponse response = studentService.findByRollNumber(rollNumber);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('STUDENT_VIEW')")
    public ResponseEntity<StudentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/roll-number")
    @PreAuthorize("@perm.has('STUDENT_CREATE')")
    public ResponseEntity<StudentResponse> assignRollNumber(
            @PathVariable Long id,
            @Valid @RequestBody RollNumberAssignmentRequest request) {
        return ResponseEntity.ok(studentService.assignRollNumber(id, request.rollNumber()));
    }

    @PostMapping("/bulk-assign-roll-numbers")
    @PreAuthorize("@perm.has('STUDENT_CREATE')")
    public ResponseEntity<List<StudentResponse>> bulkAssignRollNumbers(
            @Valid @RequestBody BulkRollNumberAssignmentRequest request) {
        return ResponseEntity.ok(studentService.bulkAssignRollNumbers(request.assignments()));
    }

    @PostMapping("/generate-roll-numbers")
    @PreAuthorize("@perm.has('STUDENT_CREATE')")
    public ResponseEntity<List<RollNumberAssignment>> generateRollNumbers(
            @Valid @RequestBody GenerateRollNumbersRequest request) {
        List<RollNumberAssignment> assignments = rollNumberGeneratorService.generateAndAssignRollNumbers(request);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/preview-roll-numbers")
    @PreAuthorize("@perm.has('STUDENT_CREATE')")
    public ResponseEntity<List<RollNumberAssignment>> previewRollNumbers(
            @Valid @RequestBody GenerateRollNumbersRequest request) {
        List<RollNumberAssignment> preview = rollNumberGeneratorService.previewRollNumbers(request);
        return ResponseEntity.ok(preview);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('STUDENT_CREATE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/program-transfer-analysis")
    @PreAuthorize("@perm.has('STUDENT_EDIT')")
    public ResponseEntity<ProgramTransferAnalysis> analyzeProgramTransfer(
            @PathVariable Long id,
            @RequestParam Long newProgramId) {
        return ResponseEntity.ok(studentService.analyzeProgramTransfer(id, newProgramId));
    }

    @PostMapping("/{id}/program-transfer")
    @PreAuthorize("@perm.has('STUDENT_EDIT')")
    public ResponseEntity<ProgramTransferRecord> executeProgramTransfer(
            @PathVariable Long id,
            @Valid @RequestBody ProgramTransferRequest request) {
        return ResponseEntity.ok(studentService.executeProgramTransfer(id, request));
    }

    @GetMapping("/{id}/program-transfers")
    @PreAuthorize("@perm.hasAny('STUDENT_VIEW', 'STUDENT_EDIT')")
    public ResponseEntity<List<ProgramTransferRecord>> getTransferHistory(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getTransferHistory(id));
    }
}

