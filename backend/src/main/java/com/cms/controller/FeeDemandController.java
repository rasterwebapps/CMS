package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.FeeDemandDto;
import com.cms.model.enums.DemandStatus;
import com.cms.service.FeeDemandService;

@RestController
@RequestMapping("/api/fee-demands")
public class FeeDemandController {

    private final FeeDemandService feeDemandService;

    public FeeDemandController(FeeDemandService feeDemandService) {
        this.feeDemandService = feeDemandService;
    }

    @GetMapping
    public ResponseEntity<List<FeeDemandDto>> getDemands(
            @RequestParam(required = false) Long termInstanceId,
            @RequestParam(required = false) Long enrollmentId,
            @RequestParam(required = false) DemandStatus status) {
        if (enrollmentId != null) {
            return ResponseEntity.ok(List.of(feeDemandService.getDemandByEnrollment(enrollmentId)));
        }
        if (termInstanceId != null) {
            if (status != null) {
                return ResponseEntity.ok(
                    feeDemandService.getDemandsByTermInstanceAndStatus(termInstanceId, status));
            }
            return ResponseEntity.ok(feeDemandService.getDemandsByTermInstance(termInstanceId));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeDemandDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(feeDemandService.getById(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<GenerateDemandsResponse> generateDemands(
            @RequestParam Long termInstanceId) {
        int created = feeDemandService.generateDemandsForTermInstance(termInstanceId);
        return ResponseEntity.status(HttpStatus.OK).body(new GenerateDemandsResponse(created));
    }

    /** Simple response wrapper. */
    public record GenerateDemandsResponse(int demandsCreated) {}
}
