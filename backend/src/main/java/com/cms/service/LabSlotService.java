package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LabSlotRequest;
import com.cms.dto.LabSlotResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LabSlot;
import com.cms.repository.LabSlotRepository;

@Service
@Transactional(readOnly = true)
public class LabSlotService {

    private final LabSlotRepository labSlotRepository;

    public LabSlotService(LabSlotRepository labSlotRepository) {
        this.labSlotRepository = labSlotRepository;
    }

    @Transactional
    public LabSlotResponse create(LabSlotRequest request) {
        Boolean isActive = request.isActive() != null ? request.isActive() : true;

        LabSlot labSlot = new LabSlot(
            request.name(),
            request.startTime(),
            request.endTime(),
            request.slotOrder(),
            isActive
        );

        LabSlot saved = labSlotRepository.save(labSlot);
        return toResponse(saved);
    }

    public List<LabSlotResponse> findAll() {
        return labSlotRepository.findAllByOrderBySlotOrderAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LabSlotResponse> findAllActive() {
        return labSlotRepository.findByIsActiveTrueOrderBySlotOrderAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public LabSlotResponse findById(Long id) {
        LabSlot labSlot = labSlotRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab slot not found with id: " + id));
        return toResponse(labSlot);
    }

    @Transactional
    public LabSlotResponse update(Long id, LabSlotRequest request) {
        LabSlot labSlot = labSlotRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab slot not found with id: " + id));

        labSlot.setName(request.name());
        labSlot.setStartTime(request.startTime());
        labSlot.setEndTime(request.endTime());
        labSlot.setSlotOrder(request.slotOrder());

        if (request.isActive() != null) {
            labSlot.setIsActive(request.isActive());
        }

        LabSlot updated = labSlotRepository.save(labSlot);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!labSlotRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lab slot not found with id: " + id);
        }
        labSlotRepository.deleteById(id);
    }

    private LabSlotResponse toResponse(LabSlot labSlot) {
        return new LabSlotResponse(
            labSlot.getId(),
            labSlot.getName(),
            labSlot.getStartTime(),
            labSlot.getEndTime(),
            labSlot.getSlotOrder(),
            labSlot.getIsActive(),
            labSlot.getCreatedAt(),
            labSlot.getUpdatedAt()
        );
    }
}
