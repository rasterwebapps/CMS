package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.FeeStateResponse;
import com.cms.repository.FeeStateRepository;

@RestController
@RequestMapping("/fee-states")
public class FeeStateController {

    private final FeeStateRepository feeStateRepository;

    public FeeStateController(FeeStateRepository feeStateRepository) {
        this.feeStateRepository = feeStateRepository;
    }

    @GetMapping
    public ResponseEntity<List<FeeStateResponse>> findAll() {
        List<FeeStateResponse> states = feeStateRepository.findByIsActiveTrueOrderBySortOrderAsc()
            .stream()
            .map(s -> new FeeStateResponse(
                s.getId(), s.getName(), s.getCode(),
                s.isDefault(), s.isFallback(), s.getSortOrder()))
            .toList();
        return ResponseEntity.ok(states);
    }
}
