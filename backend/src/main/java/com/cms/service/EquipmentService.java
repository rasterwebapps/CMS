package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EquipmentRequest;
import com.cms.dto.EquipmentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Equipment;
import com.cms.model.Lab;
import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.LabRepository;

@Service
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final LabRepository labRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, LabRepository labRepository) {
        this.equipmentRepository = equipmentRepository;
        this.labRepository = labRepository;
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        Lab lab = labRepository.findById(request.labId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + request.labId()));

        Equipment equipment = new Equipment(
            request.name(), request.assetCode(), request.category(), lab, request.status()
        );
        equipment.setSerialNumber(request.serialNumber());
        equipment.setManufacturer(request.manufacturer());
        equipment.setModel(request.model());
        equipment.setPurchaseDate(request.purchaseDate());
        equipment.setPurchasePrice(request.purchasePrice());
        equipment.setWarrantyExpiry(request.warrantyExpiry());
        equipment.setLocation(request.location());
        equipment.setSpecifications(request.specifications());

        Equipment saved = equipmentRepository.save(equipment);
        return toResponse(saved);
    }

    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public EquipmentResponse findById(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + id));
        return toResponse(equipment);
    }

    public List<EquipmentResponse> findByLabId(Long labId) {
        if (!labRepository.existsById(labId)) {
            throw new ResourceNotFoundException("Lab not found with id: " + labId);
        }
        return equipmentRepository.findByLabId(labId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<EquipmentResponse> findByStatus(EquipmentStatus status) {
        return equipmentRepository.findByStatus(status).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<EquipmentResponse> findByCategory(EquipmentCategory category) {
        return equipmentRepository.findByCategory(category).stream()
            .map(this::toResponse)
            .toList();
    }

    public EquipmentResponse findByAssetCode(String assetCode) {
        Equipment equipment = equipmentRepository.findByAssetCode(assetCode)
            .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with asset code: " + assetCode));
        return toResponse(equipment);
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = equipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + id));

        Lab lab = labRepository.findById(request.labId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + request.labId()));

        equipment.setName(request.name());
        equipment.setAssetCode(request.assetCode());
        equipment.setSerialNumber(request.serialNumber());
        equipment.setCategory(request.category());
        equipment.setLab(lab);
        equipment.setManufacturer(request.manufacturer());
        equipment.setModel(request.model());
        equipment.setStatus(request.status());
        equipment.setPurchaseDate(request.purchaseDate());
        equipment.setPurchasePrice(request.purchasePrice());
        equipment.setWarrantyExpiry(request.warrantyExpiry());
        equipment.setLocation(request.location());
        equipment.setSpecifications(request.specifications());

        Equipment updated = equipmentRepository.save(equipment);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!equipmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Equipment not found with id: " + id);
        }
        equipmentRepository.deleteById(id);
    }

    private EquipmentResponse toResponse(Equipment eq) {
        return new EquipmentResponse(
            eq.getId(),
            eq.getName(),
            eq.getAssetCode(),
            eq.getSerialNumber(),
            eq.getCategory(),
            eq.getLab().getId(),
            eq.getLab().getName(),
            eq.getManufacturer(),
            eq.getModel(),
            eq.getStatus(),
            eq.getPurchaseDate(),
            eq.getPurchasePrice(),
            eq.getWarrantyExpiry(),
            eq.getLocation(),
            eq.getSpecifications(),
            eq.getCreatedAt(),
            eq.getUpdatedAt()
        );
    }
}
