package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.CohortSeatsRequest;
import com.cms.dto.CohortSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Program;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.StudentRepository;

@RestController
@RequestMapping("/cohorts")
public class CohortController {

    private final CohortRepository      cohortRepository;
    private final ProgramRepository     programRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentRepository     studentRepository;

    public CohortController(CohortRepository cohortRepository,
                            ProgramRepository programRepository,
                            AcademicYearRepository academicYearRepository,
                            StudentRepository studentRepository) {
        this.cohortRepository       = cohortRepository;
        this.programRepository      = programRepository;
        this.academicYearRepository = academicYearRepository;
        this.studentRepository      = studentRepository;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('ACADEMIC_YEAR_MANAGE','STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<CohortSummaryResponse>> getCohorts(
            @RequestParam(required = false) Long academicYearId) {
        List<Cohort> cohorts = academicYearId != null
            ? cohortRepository.findByAdmissionAcademicYearId(academicYearId)
            : cohortRepository.findAll();
        return ResponseEntity.ok(cohorts.stream().map(this::toResponse).toList());
    }

    @PostMapping("/initialize")
    @PreAuthorize("@perm.has('ACADEMIC_YEAR_MANAGE')")
    public ResponseEntity<List<CohortSummaryResponse>> initializeCohorts(
            @RequestParam Long academicYearId) {
        AcademicYear ay = academicYearRepository.findById(academicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearId));
        int startYear = ay.getStartYear();
        for (Program program : programRepository.findByStatus(ProgramStatus.ACTIVE)) {
            cohortRepository.findByProgramIdAndAdmissionAcademicYearId(program.getId(), academicYearId)
                .orElseGet(() -> {
                    int endYear = startYear + program.getDurationYears();
                    String code = program.getCode() + "-" + startYear + "-" + endYear;
                    String name = program.getName() + " (" + startYear + "-" + endYear + ")";
                    AcademicYear gradAY = academicYearRepository
                        .findByName(endYear + "-" + (endYear + 1))
                        .orElse(null);
                    Cohort c = new Cohort();
                    c.setProgram(program);
                    c.setAdmissionAcademicYear(ay);
                    c.setExpectedGraduationAcademicYear(gradAY);
                    c.setCohortCode(code);
                    c.setDisplayName(name);
                    c.setStatus(CohortStatus.ACTIVE);
                    return cohortRepository.save(c);
                });
        }
        List<Cohort> result = cohortRepository.findByAdmissionAcademicYearId(academicYearId);
        return ResponseEntity.ok(result.stream().map(this::toResponse).toList());
    }

    @PatchMapping("/{id}/seats")
    @PreAuthorize("@perm.has('ACADEMIC_YEAR_MANAGE')")
    public ResponseEntity<CohortSummaryResponse> updateSeats(
            @PathVariable Long id,
            @RequestBody CohortSeatsRequest request) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found: " + id));
        cohort.setManagementSeats(request.managementSeats());
        cohort.setCounsellingSeats(request.counsellingSeats());
        return ResponseEntity.ok(toResponse(cohortRepository.save(cohort)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('ACADEMIC_YEAR_MANAGE')")
    public ResponseEntity<Void> deleteCohort(@PathVariable Long id) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found: " + id));
        if (studentRepository.existsByCohortId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot delete cohort with enrolled students");
        }
        cohortRepository.delete(cohort);
        return ResponseEntity.noContent().build();
    }

    private CohortSummaryResponse toResponse(Cohort c) {
        String programName = c.getProgram() != null ? c.getProgram().getName() : "—";
        String programCode = c.getProgram() != null ? c.getProgram().getCode() : "—";
        return new CohortSummaryResponse(
            c.getId(), c.getCohortCode(), c.getDisplayName(),
            programName, programCode,
            c.getManagementSeats(), c.getCounsellingSeats(),
            studentRepository.existsByCohortId(c.getId())
        );
    }
}
