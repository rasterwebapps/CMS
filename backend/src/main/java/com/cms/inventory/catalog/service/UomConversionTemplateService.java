package com.cms.inventory.catalog.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.UomConversionTemplateLevelRequest;
import com.cms.inventory.catalog.dto.UomConversionTemplateLevelResponse;
import com.cms.inventory.catalog.dto.UomConversionTemplateRequest;
import com.cms.inventory.catalog.dto.UomConversionTemplateResponse;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.model.UomConversionTemplate;
import com.cms.inventory.catalog.model.UomConversionTemplateLevel;
import com.cms.inventory.catalog.repository.UomConversionTemplateRepository;

/**
 * Owns the reusable {@link UomConversionTemplate} master — see the 2026-09-11 "Shared/global UOM
 * conversion templates" decision-log entry. Levels are validated the same way {@code
 * ProductUomChainService.validateLevels} validates a product's own chain (unique ranks, unique
 * units, exactly one level-0 base entry at factor 1, at most one default-purchase level) — kept as
 * a separate, near-identical method here rather than shared/extracted, since the two operate on
 * different request DTOs (this service isn't tied to one product's {@code baseUom}, a template's
 * own {@code baseUomId} plays that role instead).
 */
@Service
@Transactional(readOnly = true)
public class UomConversionTemplateService {

    private final UomConversionTemplateRepository templateRepository;
    private final UomService uomService;

    public UomConversionTemplateService(UomConversionTemplateRepository templateRepository, UomService uomService) {
        this.templateRepository = templateRepository;
        this.uomService = uomService;
    }

    @Transactional
    public UomConversionTemplateResponse create(UomConversionTemplateRequest request) {
        UomConversionTemplate template = new UomConversionTemplate();
        applyRequest(template, request, null);
        return toResponse(templateRepository.save(template));
    }

    public List<UomConversionTemplateResponse> findAll(boolean activeOnly, Long baseUomId) {
        List<UomConversionTemplate> templates;
        if (baseUomId != null) {
            templates = templateRepository.findByBaseUomIdAndIsActiveTrueOrderByNameAsc(baseUomId);
        } else {
            templates = activeOnly ? templateRepository.findByIsActiveTrueOrderByNameAsc() : templateRepository.findAllByOrderByNameAsc();
        }
        return templates.stream().map(this::toResponse).toList();
    }

    public Page<UomConversionTemplateResponse> findPage(String search, Pageable pageable) {
        Specification<UomConversionTemplate> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%");
        };
        return templateRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public UomConversionTemplateResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public UomConversionTemplateResponse update(Long id, UomConversionTemplateRequest request) {
        UomConversionTemplate template = findOrThrow(id);
        applyRequest(template, request, id);
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("UOM conversion template not found with id: " + id);
        }
        templateRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        UomConversionTemplate template = findOrThrow(id);
        template.setIsActive(Boolean.TRUE.equals(request.isActive()));
        UomConversionTemplate saved = templateRepository.save(template);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        return excludeId != null
            ? templateRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId)
            : templateRepository.existsByNameIgnoreCase(trimmed);
    }

    private void applyRequest(UomConversionTemplate template, UomConversionTemplateRequest request, Long excludeId) {
        String name = requireTrimmed(request.name(), "Template name is required");
        if (excludeId != null ? templateRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId) : templateRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A UOM conversion template named '" + name + "' already exists");
        }
        Uom baseUom = uomService.findOrThrow(request.baseUomId());
        validateLevels(baseUom, request.levels());

        template.setName(name);
        template.setDescription(trim(request.description()));
        template.setBaseUom(baseUom);
        if (request.isActive() != null) template.setIsActive(request.isActive());

        template.getLevels().clear();
        for (UomConversionTemplateLevelRequest lr : request.levels()) {
            UomConversionTemplateLevel level = new UomConversionTemplateLevel();
            level.setTemplate(template);
            level.setUom(uomService.findOrThrow(lr.uomId()));
            level.setLevelRank(lr.levelRank());
            level.setFactorToBase(lr.factorToBase());
            level.setIsDefaultPurchase(Boolean.TRUE.equals(lr.isDefaultPurchase()));
            template.getLevels().add(level);
        }
    }

    /** Mirrors {@code ProductUomChainService.validateLevels} — see this class's own javadoc for
     *  why it isn't shared/extracted. */
    private void validateLevels(Uom baseUom, List<UomConversionTemplateLevelRequest> levels) {
        Set<Integer> ranks = new HashSet<>();
        Set<Long> uomIds = new HashSet<>();
        long defaultCount = 0;
        boolean hasBase = false;
        for (UomConversionTemplateLevelRequest lr : levels) {
            if (!ranks.add(lr.levelRank())) {
                throw new IllegalArgumentException("Each level must have a unique rank — rank " + lr.levelRank() + " is repeated");
            }
            if (!uomIds.add(lr.uomId())) {
                throw new IllegalArgumentException("Each level must use a different unit of measure");
            }
            if (lr.levelRank() == 0) {
                hasBase = true;
                if (!lr.uomId().equals(baseUom.getId())) {
                    throw new IllegalArgumentException("Level 0 must be the template's base unit ('" + baseUom.getCode() + "')");
                }
                if (lr.factorToBase().compareTo(BigDecimal.ONE) != 0) {
                    throw new IllegalArgumentException("The base level's conversion factor must be 1");
                }
            }
            if (Boolean.TRUE.equals(lr.isDefaultPurchase())) defaultCount++;
        }
        if (!hasBase) {
            throw new IllegalArgumentException("The template must include level 0 (its base unit)");
        }
        if (defaultCount > 1) {
            throw new IllegalArgumentException("Only one level can be marked as the default purchase unit");
        }
    }

    private UomConversionTemplate findOrThrow(Long id) {
        return templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("UOM conversion template not found with id: " + id));
    }

    private UomConversionTemplateResponse toResponse(UomConversionTemplate t) {
        Uom baseUom = t.getBaseUom();
        List<UomConversionTemplateLevelResponse> levels = new ArrayList<>();
        for (UomConversionTemplateLevel l : t.getLevels()) {
            Uom uom = l.getUom();
            levels.add(new UomConversionTemplateLevelResponse(uom.getId(), uom.getCode(), uom.getName(),
                l.getLevelRank(), l.getFactorToBase(), l.getIsDefaultPurchase()));
        }
        return new UomConversionTemplateResponse(t.getId(), t.getName(), t.getDescription(),
            baseUom.getId(), baseUom.getCode(), baseUom.getName(), t.getIsActive(), levels,
            t.getCreatedAt(), t.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
