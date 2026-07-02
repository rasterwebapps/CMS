package com.cms.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ApplicationNumberSequenceResponse;
import com.cms.model.AcademicYear;
import com.cms.model.Course;
import com.cms.model.NumberSequenceCounter;
import com.cms.model.NumberSeriesDefinition;
import com.cms.repository.NumberSequenceCounterRepository;
import com.cms.repository.NumberSeriesDefinitionRepository;

@Service
@Transactional(readOnly = true)
public class ApplicationNumberSequenceService {

    public static final String ADMISSION_SERIES    = "ADMISSION_NUMBER";
    public static final String RECEIPT_SERIES      = "RECEIPT_NUMBER";
    public static final String REFUND_SERIES       = "REFUND_NUMBER";
    public static final String COMMISSION_SERIES   = "COMMISSION_NUMBER";
    public static final String DISBURSEMENT_SERIES = "DISBURSEMENT_NUMBER";

    private final NumberSeriesDefinitionRepository definitionRepository;
    private final NumberSequenceCounterRepository  counterRepository;

    public ApplicationNumberSequenceService(
            NumberSeriesDefinitionRepository definitionRepository,
            NumberSequenceCounterRepository counterRepository) {
        this.definitionRepository = definitionRepository;
        this.counterRepository    = counterRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query methods (read-only)
    // ─────────────────────────────────────────────────────────────────────────

    public List<ApplicationNumberSequenceResponse> findAll() {
        Map<String, NumberSeriesDefinition> defs = definitionIndex();
        return counterRepository.findAll().stream()
            .filter(c -> defs.containsKey(c.getSeriesCode()))
            .map(c -> toResponse(defs.get(c.getSeriesCode()), c))
            .toList();
    }

    public Page<ApplicationNumberSequenceResponse> findPage(String search, Pageable pageable) {
        Map<String, NumberSeriesDefinition> defs = definitionIndex();
        Specification<NumberSequenceCounter> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("seriesCode")), pattern),
                cb.like(cb.lower(root.get("scopeKey")), pattern)
            ));
        }
        return counterRepository.findAll(spec, pageable)
            .map(c -> defs.containsKey(c.getSeriesCode())
                ? toResponse(defs.get(c.getSeriesCode()), c)
                : null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Number generation — public API (signatures unchanged from Phase 1)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates the next admission number.
     * Format: {startYear}{admissionNumberCode}{seq} — e.g. 2026650001.
     * scope_key = startYear + admissionNumberCode (e.g. "202665").
     * For retro-admits pass the academic year from the chosen admission date,
     * not the current academic year — the correct historic counter is then used.
     */
    @Transactional
    public String nextAdmissionNumber(AcademicYear academicYear, Course course) {
        if (course == null || course.getRollNumberCode() == null || course.getRollNumberCode().isBlank()) {
            throw new IllegalStateException(
                "Course must have a roll_number_code configured before an admission number can be generated");
        }
        String scopeKey = academicYear.getStartYear() + course.getRollNumberCode();
        return generateNumber(ADMISSION_SERIES, scopeKey);
    }

    @Transactional
    public String nextReceiptNumber(int year) {
        return generateNumber(RECEIPT_SERIES, String.valueOf(year));
    }

    @Transactional
    public String nextRefundNumber(int year) {
        return generateNumber(REFUND_SERIES, String.valueOf(year));
    }

    @Transactional
    public String nextCommissionNumber(int year) {
        return generateNumber(COMMISSION_SERIES, String.valueOf(year));
    }

    @Transactional
    public String nextDisbursementNumber(int year) {
        return generateNumber(DISBURSEMENT_SERIES, String.valueOf(year));
    }

    /**
     * Generic generation method kept for backward-compatibility with any direct callers.
     * seriesName, scopeType, prefix, sequencePadding, description are now owned by the
     * series definition in number_series_definitions — the values passed here are ignored.
     * The series definition must already exist (seeded by migration or created via Phase 3 UI).
     */
    @Transactional
    public synchronized String nextNumber(String seriesCode, String seriesName, String scopeType,
                                          String scopeKey, String prefix, int sequencePadding,
                                          String description) {
        return generateNumber(seriesCode, scopeKey);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal engine
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    private synchronized String generateNumber(String seriesCode, String scopeKey) {
        NumberSeriesDefinition def = definitionRepository.findBySeriesCode(seriesCode)
            .orElseThrow(() -> new IllegalStateException(
                "No series definition found for '" + seriesCode + "'. "
                + "Create it via Settings → Number Sequences before generating numbers."));

        NumberSequenceCounter counter = counterRepository
            .findBySeriesCodeAndScopeKeyForUpdate(seriesCode, scopeKey)
            .orElseGet(() -> {
                NumberSequenceCounter c = new NumberSequenceCounter();
                c.setSeriesCode(seriesCode);
                c.setScopeKey(scopeKey);
                c.setLastSequence(0);
                return counterRepository.saveAndFlush(c);
            });

        int nextSeq = counter.getLastSequence() + 1;
        counter.setLastSequence(nextSeq);
        counterRepository.save(counter);

        return format(def, scopeKey, nextSeq);
    }

    /**
     * Formats a generated number from its definition, scope_key, and sequence integer.
     *
     * Rules:
     *   scope omitted when scope_key == "GLOBAL" (NONE scope type)
     *   prefix absent:  scopeKey + sep + seq  (or just seq when scope omitted)
     *   prefix present: prefix + sep + scopeKey + sep + seq  (or prefix + sep + seq when scope omitted)
     *
     * Examples:
     *   ADMISSION (prefix=null, sep='', scopeKey='202665', seq=41)  → "2026650041"
     *   RECEIPT   (prefix='RCP', sep='-', scopeKey='2026', seq=319) → "RCP-2026-00319"
     *   ASSET     (prefix='AST', sep='-', scopeKey='GLOBAL', seq=5) → "AST-000005"
     *   INVENTORY (prefix=null,  sep='',  scopeKey='GLOBAL', seq=5) → "00005"
     */
    private String format(NumberSeriesDefinition def, String scopeKey, int seq) {
        String seqStr      = String.format("%0" + def.getSequencePadding() + "d", seq);
        String sep         = def.getSeparator() != null ? def.getSeparator() : "";
        boolean hasPrefix  = def.getPrefix() != null && !def.getPrefix().isBlank();
        boolean includeScope = !"GLOBAL".equals(scopeKey);

        if (!hasPrefix) {
            return includeScope ? (scopeKey + sep + seqStr) : seqStr;
        }
        if (includeScope) {
            return def.getPrefix() + sep + scopeKey + sep + seqStr;
        }
        return def.getPrefix() + sep + seqStr;
    }

    private ApplicationNumberSequenceResponse toResponse(NumberSeriesDefinition def,
                                                         NumberSequenceCounter counter) {
        String lastGenerated = counter.getLastSequence() > 0
            ? format(def, counter.getScopeKey(), counter.getLastSequence())
            : "—";
        String nextPreview = format(def, counter.getScopeKey(), counter.getLastSequence() + 1);

        return new ApplicationNumberSequenceResponse(
            counter.getId(),
            def.getSeriesCode(),
            def.getSeriesName(),
            def.getScopeType(),
            counter.getScopeKey(),
            def.getPrefix(),
            def.getSequencePadding(),
            counter.getLastSequence(),
            lastGenerated,
            nextPreview,
            def.getDescription(),
            counter.getCreatedAt(),
            counter.getUpdatedAt()
        );
    }

    private Map<String, NumberSeriesDefinition> definitionIndex() {
        return definitionRepository.findAll().stream()
            .collect(Collectors.toMap(NumberSeriesDefinition::getSeriesCode, d -> d));
    }
}
