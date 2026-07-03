package com.cms.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.NumberSeriesDefinitionRequest;
import com.cms.dto.NumberSeriesDefinitionResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.NumberSequenceCounter;
import com.cms.model.NumberSeriesDefinition;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.NumberSequenceCounterRepository;
import com.cms.repository.NumberSeriesDefinitionRepository;

@Service
@Transactional(readOnly = true)
public class NumberSeriesDefinitionService {

    private static final DateTimeFormatter DAY_LABEL   = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

    private final NumberSeriesDefinitionRepository definitionRepository;
    private final NumberSequenceCounterRepository  counterRepository;
    private final AcademicYearRepository           academicYearRepository;
    private final AppTimezoneService               timezoneService;
    private final ScopeKeyResolver                 scopeKeyResolver;

    public NumberSeriesDefinitionService(
            NumberSeriesDefinitionRepository definitionRepository,
            NumberSequenceCounterRepository counterRepository,
            AcademicYearRepository academicYearRepository,
            AppTimezoneService timezoneService,
            ScopeKeyResolver scopeKeyResolver) {
        this.definitionRepository = definitionRepository;
        this.counterRepository    = counterRepository;
        this.academicYearRepository = academicYearRepository;
        this.timezoneService      = timezoneService;
        this.scopeKeyResolver     = scopeKeyResolver;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    public List<NumberSeriesDefinitionResponse> findAll() {
        LocalDate today = LocalDate.now(timezoneService.getZone());
        Integer currentAyStartYear = resolveCurrentAyStartYear();

        return definitionRepository.findAll().stream()
            .map(def -> toResponse(def, today, currentAyStartYear))
            .toList();
    }

    public NumberSeriesDefinitionResponse findById(Long id) {
        NumberSeriesDefinition def = definitionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Number series not found: " + id));
        LocalDate today = LocalDate.now(timezoneService.getZone());
        return toResponse(def, today, resolveCurrentAyStartYear());
    }

    /** Returns a formatted preview of the next number without touching any counter. */
    public String preview(Long id) {
        NumberSeriesDefinition def = definitionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Number series not found: " + id));
        LocalDate today = LocalDate.now(timezoneService.getZone());
        Integer currentAyStartYear = resolveCurrentAyStartYear();
        String scopeKey = resolveSingleScopeKey(def.getScopeType(), today, currentAyStartYear);
        if (scopeKey == null) return null;

        int lastSeq = counterRepository.findBySeriesCode(def.getSeriesCode())
            .stream()
            .filter(c -> c.getScopeKey().equals(scopeKey))
            .mapToInt(NumberSequenceCounter::getLastSequence)
            .findFirst()
            .orElse(0);

        return format(def, scopeKey, lastSeq + 1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mutations
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public NumberSeriesDefinitionResponse create(NumberSeriesDefinitionRequest req) {
        if (definitionRepository.existsBySeriesCode(req.seriesCode())) {
            throw new IllegalArgumentException("Series code '" + req.seriesCode() + "' already exists.");
        }
        NumberSeriesDefinition def = new NumberSeriesDefinition();
        def.setSeriesCode(req.seriesCode());
        def.setSeriesName(req.seriesName());
        def.setScopeType(req.scopeType());
        def.setPrefix(nullIfBlank(req.prefix()));
        def.setSeparator(req.separator() != null ? req.separator() : "-");
        def.setSequencePadding(req.sequencePadding());
        def.setDescription(req.description());
        def.setActive(true);
        def = definitionRepository.save(def);
        return toResponse(def, LocalDate.now(timezoneService.getZone()), resolveCurrentAyStartYear());
    }

    @Transactional
    public NumberSeriesDefinitionResponse update(Long id, NumberSeriesDefinitionRequest req) {
        NumberSeriesDefinition def = definitionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Number series not found: " + id));

        boolean hasCounters = !counterRepository.findBySeriesCode(def.getSeriesCode()).isEmpty();
        if (hasCounters && !def.getScopeType().equals(req.scopeType())) {
            throw new IllegalArgumentException(
                "Cannot change scope_type once counters exist for this series. "
                + "Current type: " + def.getScopeType() + ". "
                + "Create a new series definition if a different scope is needed.");
        }

        def.setSeriesName(req.seriesName());
        def.setScopeType(req.scopeType());
        def.setPrefix(nullIfBlank(req.prefix()));
        def.setSeparator(req.separator() != null ? req.separator() : "-");
        def.setSequencePadding(req.sequencePadding());
        def.setDescription(req.description());
        def = definitionRepository.save(def);
        return toResponse(def, LocalDate.now(timezoneService.getZone()), resolveCurrentAyStartYear());
    }

    @Transactional
    public void delete(Long id) {
        NumberSeriesDefinition def = definitionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Number series not found: " + id));
        boolean hasCounters = !counterRepository.findBySeriesCode(def.getSeriesCode()).isEmpty();
        if (hasCounters) {
            throw new IllegalArgumentException(
                "Cannot delete a series that has generated numbers. Deactivate it instead.");
        }
        definitionRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private NumberSeriesDefinitionResponse toResponse(NumberSeriesDefinition def,
                                                      LocalDate today,
                                                      Integer currentAyStartYear) {
        String scopeType = def.getScopeType();
        boolean multiScope = "COURSE".equals(scopeType) || "ACADEMIC_YEAR_COURSE".equals(scopeType);
        boolean canEditScopeType = counterRepository.findBySeriesCode(def.getSeriesCode()).isEmpty();

        String currentPeriodLabel = buildPeriodLabel(def, today, currentAyStartYear);
        int currentLastSequence;
        String currentLastGenerated = null;
        String currentNextPreview   = null;

        if (multiScope) {
            // Aggregate across all relevant counters
            List<NumberSequenceCounter> relevant = relevantCounters(def, currentAyStartYear);
            currentLastSequence = relevant.stream().mapToInt(NumberSequenceCounter::getLastSequence).sum();
        } else {
            String scopeKey = resolveSingleScopeKey(scopeType, today, currentAyStartYear);
            Optional<NumberSequenceCounter> counter = scopeKey == null
                ? Optional.empty()
                : counterRepository.findBySeriesCode(def.getSeriesCode()).stream()
                    .filter(c -> c.getScopeKey().equals(scopeKey))
                    .findFirst();

            currentLastSequence = counter.map(NumberSequenceCounter::getLastSequence).orElse(0);
            if (scopeKey != null) {
                currentLastGenerated = currentLastSequence > 0
                    ? format(def, scopeKey, currentLastSequence)
                    : "—";
                currentNextPreview = format(def, scopeKey, currentLastSequence + 1);
            }
        }

        return new NumberSeriesDefinitionResponse(
            def.getId(),
            def.getSeriesCode(),
            def.getSeriesName(),
            def.getScopeType(),
            def.getPrefix(),
            def.getSeparator(),
            def.getSequencePadding(),
            def.getDescription(),
            def.isActive(),
            canEditScopeType,
            currentPeriodLabel,
            currentLastSequence,
            currentLastGenerated,
            currentNextPreview,
            def.getCreatedAt(),
            def.getUpdatedAt()
        );
    }

    /** Scope_key for single-counter scope types. Returns null if context is unavailable. */
    private String resolveSingleScopeKey(String scopeType, LocalDate today, Integer currentAyStartYear) {
        return switch (scopeType) {
            case "NONE"           -> "GLOBAL";
            case "CALENDAR_DAY"   -> scopeKeyResolver.resolveCurrentPeriod("CALENDAR_DAY");
            case "CALENDAR_MONTH" -> scopeKeyResolver.resolveCurrentPeriod("CALENDAR_MONTH");
            case "CALENDAR_YEAR"  -> scopeKeyResolver.resolveCurrentPeriod("CALENDAR_YEAR");
            case "FINANCIAL_MONTH"-> scopeKeyResolver.resolveCurrentPeriod("FINANCIAL_MONTH");
            case "FINANCIAL_YEAR" -> scopeKeyResolver.resolveCurrentPeriod("FINANCIAL_YEAR");
            case "ACADEMIC_YEAR"  -> currentAyStartYear != null
                                        ? String.valueOf(currentAyStartYear)
                                        : null;
            default               -> null; // COURSE, ACADEMIC_YEAR_COURSE handled by relevantCounters
        };
    }

    private List<NumberSequenceCounter> relevantCounters(NumberSeriesDefinition def, Integer currentAyStartYear) {
        List<NumberSequenceCounter> all = counterRepository.findBySeriesCode(def.getSeriesCode());
        return switch (def.getScopeType()) {
            case "COURSE" -> all; // all courses, permanent
            case "ACADEMIC_YEAR_COURSE" -> {
                if (currentAyStartYear == null) yield List.of();
                String prefix = String.valueOf(currentAyStartYear);
                yield all.stream()
                    .filter(c -> c.getScopeKey().startsWith(prefix))
                    .toList();
            }
            default -> all;
        };
    }

    private String buildPeriodLabel(NumberSeriesDefinition def, LocalDate today, Integer ayStartYear) {
        return switch (def.getScopeType()) {
            case "NONE"            -> "All Time";
            case "CALENDAR_DAY"    -> today.format(DAY_LABEL);
            case "CALENDAR_MONTH"  -> today.format(MONTH_LABEL);
            case "CALENDAR_YEAR"   -> String.valueOf(today.getYear());
            case "FINANCIAL_MONTH" -> today.format(MONTH_LABEL);
            case "FINANCIAL_YEAR"  -> {
                // resolveFinancialYear returns e.g. "2526" for FY 2025-26
                String fy = scopeKeyResolver.resolveFinancialYear(today);
                yield "20" + fy.substring(0, 2) + "-" + fy.substring(2);
            }
            case "ACADEMIC_YEAR"   -> ayStartYear != null
                ? ayStartYear + "-" + (ayStartYear % 100 + 1)
                : "—";
            case "COURSE"          -> "Per Course";
            case "ACADEMIC_YEAR_COURSE" -> ayStartYear != null
                ? "Per Course (" + ayStartYear + "-" + String.format("%02d", (ayStartYear + 1) % 100) + ")"
                : "Per Course";
            default -> "—";
        };
    }

    private Integer resolveCurrentAyStartYear() {
        return academicYearRepository.findByIsCurrentTrue()
            .map(ay -> ay.getStartYear())
            .orElse(null);
    }

    private String format(NumberSeriesDefinition def, String scopeKey, int seq) {
        String seqStr     = String.format("%0" + def.getSequencePadding() + "d", seq);
        String sep        = def.getSeparator() != null ? def.getSeparator() : "";
        boolean hasPrefix = def.getPrefix() != null && !def.getPrefix().isBlank();
        boolean inclScope = !"GLOBAL".equals(scopeKey);

        if (!hasPrefix) return inclScope ? (scopeKey + sep + seqStr) : seqStr;
        if (inclScope)  return def.getPrefix() + sep + scopeKey + sep + seqStr;
        return def.getPrefix() + sep + seqStr;
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
