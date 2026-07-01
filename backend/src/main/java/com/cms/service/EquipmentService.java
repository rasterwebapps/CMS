package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EquipmentRequest;
import com.cms.dto.EquipmentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Equipment;
import com.cms.model.Lab;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
        String assetCode = trim(request.assetCode());

        if (assetCode != null && equipmentRepository.existsByAssetCodeIgnoreCase(assetCode)) {
            throw new IllegalArgumentException(
                "Equipment with asset code '" + assetCode + "' already exists");
        }

        Equipment equipment = new Equipment(
            trim(request.name()), assetCode, request.category(), lab, request.status()
        );
        equipment.setSerialNumber(trim(request.serialNumber()));
        equipment.setManufacturer(trim(request.manufacturer()));
        equipment.setModel(trim(request.model()));
        equipment.setPurchaseDate(request.purchaseDate());
        equipment.setPurchasePrice(request.purchasePrice());
        equipment.setWarrantyExpiry(request.warrantyExpiry());
        equipment.setLocation(trim(request.location()));
        equipment.setSpecifications(trim(request.specifications()));

        Equipment saved = equipmentRepository.save(equipment);
        return toResponse(saved);
    }

    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<EquipmentResponse> findPage(String search, Pageable pageable) {
        Specification<Equipment> spec = Specification.where(null);
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                Join<Equipment, Lab> lab = root.join("lab", JoinType.INNER);
                return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("model")), pattern),
                    cb.like(cb.lower(lab.get("name")), pattern)
                );
            });
        }
        return equipmentRepository.findAll(spec, pageable).map(this::toResponse);
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
        String assetCode = trim(request.assetCode());

        if (assetCode != null && equipmentRepository.existsByAssetCodeIgnoreCaseAndIdNot(assetCode, id)) {
            throw new IllegalArgumentException(
                "Equipment with asset code '" + assetCode + "' already exists");
        }

        equipment.setName(trim(request.name()));
        equipment.setAssetCode(assetCode);
        equipment.setSerialNumber(trim(request.serialNumber()));
        equipment.setCategory(request.category());
        equipment.setLab(lab);
        equipment.setManufacturer(trim(request.manufacturer()));
        equipment.setModel(trim(request.model()));
        equipment.setStatus(request.status());
        equipment.setPurchaseDate(request.purchaseDate());
        equipment.setPurchasePrice(request.purchasePrice());
        equipment.setWarrantyExpiry(request.warrantyExpiry());
        equipment.setLocation(trim(request.location()));
        equipment.setSpecifications(trim(request.specifications()));

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

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
