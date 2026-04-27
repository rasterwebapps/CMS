package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.FeeDemandDto;
import com.cms.dto.FeeCollectionSummaryDto;
import com.cms.dto.StudentFeeLedgerDto;
import com.cms.dto.TermFeePaymentDto;
import com.cms.service.FeeReportService;

@RestController
@RequestMapping("/api/fee-reports")
public class FeeReportController {

    private final FeeReportService feeReportService;

    public FeeReportController(FeeReportService feeReportService) {
        this.feeReportService = feeReportService;
    }

    @GetMapping("/outstanding")
    public ResponseEntity<List<FeeDemandDto>> getOutstandingDemands(
            @RequestParam Long termInstanceId) {
        return ResponseEntity.ok(feeReportService.getOutstandingDemands(termInstanceId));
    }

    @GetMapping("/collection-summary")
    public ResponseEntity<List<FeeCollectionSummaryDto>> getCollectionSummary(
            @RequestParam Long termInstanceId) {
        return ResponseEntity.ok(feeReportService.getCollectionSummary(termInstanceId));
    }

    @GetMapping("/late-fee-collection")
    public ResponseEntity<List<TermFeePaymentDto>> getLateFeeCollection(
            @RequestParam Long termInstanceId) {
        return ResponseEntity.ok(feeReportService.getLateFeeCollection(termInstanceId));
    }

    @GetMapping("/student-ledger")
    public ResponseEntity<StudentFeeLedgerDto> getStudentLedger(
            @RequestParam Long studentId) {
        return ResponseEntity.ok(feeReportService.getStudentLedger(studentId));
    }
}
