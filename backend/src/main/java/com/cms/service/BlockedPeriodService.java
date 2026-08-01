package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.BlockedPeriodRequest;
import com.cms.dto.BlockedPeriodResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.BlockedPeriod;
import com.cms.model.Period;
import com.cms.model.enums.BlockType;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.PeriodRepository;

@Service
@Transactional(readOnly = true)
public class BlockedPeriodService {

    private final BlockedPeriodRepository blockedPeriodRepository;
    private final PeriodRepository periodRepository;

    public BlockedPeriodService(BlockedPeriodRepository blockedPeriodRepository, PeriodRepository periodRepository) {
        this.blockedPeriodRepository = blockedPeriodRepository;
        this.periodRepository = periodRepository;
    }

    @Transactional
    public BlockedPeriodResponse create(BlockedPeriodRequest request) {
        validateShape(request);
        Period period = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));

        BlockedPeriod block = new BlockedPeriod();
        applyRequest(block, request, period);

        return toResponse(blockedPeriodRepository.save(block));
    }

    public List<BlockedPeriodResponse> findAll() {
        return blockedPeriodRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    public BlockedPeriodResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public BlockedPeriodResponse update(Long id, BlockedPeriodRequest request) {
        BlockedPeriod block = findOrThrow(id);
        validateShape(request);
        Period period = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));

        applyRequest(block, request, period);

        return toResponse(blockedPeriodRepository.save(block));
    }

    @Transactional
    public void delete(Long id) {
        if (!blockedPeriodRepository.existsById(id)) {
            throw new ResourceNotFoundException("Blocked period not found with id: " + id);
        }
        blockedPeriodRepository.deleteById(id);
    }

    private void applyRequest(BlockedPeriod block, BlockedPeriodRequest request, Period period) {
        block.setPeriod(period);
        block.setBlockType(request.blockType());
        block.setReason(request.reason());
        if (request.blockType() == BlockType.ONE_OFF) {
            block.setSpecificDate(request.specificDate());
            block.setDayOfWeek(null);
            block.setRangeStartDate(null);
            block.setRangeEndDate(null);
        } else {
            block.setSpecificDate(null);
            block.setDayOfWeek(request.dayOfWeek());
            block.setRangeStartDate(request.rangeStartDate());
            block.setRangeEndDate(request.rangeEndDate());
        }
    }

    /** Mirrors the "shape" DB CHECK constraint at the application layer so a bad request fails
     *  with a clear message instead of a raw constraint-violation error. */
    private void validateShape(BlockedPeriodRequest request) {
        if (request.blockType() == BlockType.ONE_OFF) {
            if (request.specificDate() == null) {
                throw new IllegalArgumentException("Specific date is required for a one-off block");
            }
        } else {
            if (request.dayOfWeek() == null || request.rangeStartDate() == null || request.rangeEndDate() == null) {
                throw new IllegalArgumentException(
                    "Day of week and a start/end date range are required for a recurring block");
            }
            if (request.rangeEndDate().isBefore(request.rangeStartDate())) {
                throw new IllegalArgumentException("Range end date must not be before range start date");
            }
        }
    }

    private BlockedPeriod findOrThrow(Long id) {
        return blockedPeriodRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Blocked period not found with id: " + id));
    }

    private BlockedPeriodResponse toResponse(BlockedPeriod b) {
        return new BlockedPeriodResponse(
            b.getId(), b.getPeriod().getId(), b.getPeriod().getName(), b.getBlockType(),
            b.getSpecificDate(), b.getDayOfWeek(), b.getRangeStartDate(), b.getRangeEndDate(),
            b.getReason(), b.getCreatedAt(), b.getUpdatedAt());
    }
}