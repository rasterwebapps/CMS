package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ApplicationNumberSequenceResponse;
import com.cms.model.AcademicYear;
import com.cms.model.ApplicationNumberSequence;
import com.cms.model.Course;
import com.cms.repository.ApplicationNumberSequenceRepository;

@Service
@Transactional(readOnly = true)
public class ApplicationNumberSequenceService {

    public static final String ADMISSION_SERIES = "ADMISSION_NUMBER";
    public static final String RECEIPT_SERIES = "RECEIPT_NUMBER";

    private final ApplicationNumberSequenceRepository sequenceRepository;

    public ApplicationNumberSequenceService(ApplicationNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    public List<ApplicationNumberSequenceResponse> findAll() {
        return sequenceRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Generates the next admission number in the format {year}{courseAdmissionCode}{seq}.
     * Example: 2026650001 for BSc Nursing (code=65) in academic year 2026-27.
     * Sequence is unique per (year, course) and resets naturally when the academic year rolls over.
     */
    @Transactional
    public String nextAdmissionNumber(AcademicYear academicYear, Course course) {
        if (course == null || course.getAdmissionNumberCode() == null || course.getAdmissionNumberCode().isBlank()) {
            throw new IllegalStateException(
                "Course must have an admission_number_code configured before an admission number can be generated");
        }
        int year = academicYear.getStartYear();
        String courseCode = course.getAdmissionNumberCode();
        String scopeKey = year + courseCode;
        String prefix = scopeKey;
        return doNextNumber(
            ADMISSION_SERIES,
            "Admission Number",
            "CALENDAR_YEAR_COURSE",
            scopeKey,
            prefix,
            4,
            "Admission number: {year}{courseCode}{seq} — unique per year and course, resets yearly",
            "",
            false
        );
    }

    @Transactional
    public String nextReceiptNumber(int year) {
        return nextNumber(
            RECEIPT_SERIES,
            "Receipt Number",
            "CALENDAR_YEAR",
            String.valueOf(year),
            "RCP",
            5,
            "Global receipt number generated for every payment receipt"
        );
    }

    @Transactional
    public synchronized String nextNumber(String seriesCode, String seriesName, String scopeType, String scopeKey,
                                          String prefix, int sequencePadding, String description) {
        return doNextNumber(seriesCode, seriesName, scopeType, scopeKey, prefix, sequencePadding, description, "-", true);
    }

    private synchronized String doNextNumber(String seriesCode, String seriesName, String scopeType, String scopeKey,
                                             String prefix, int sequencePadding, String description,
                                             String separator, boolean includeScopeInNumber) {
        ApplicationNumberSequence sequence = sequenceRepository
            .findBySeriesCodeAndScopeKeyForUpdate(seriesCode, scopeKey)
            .orElseGet(() -> sequenceRepository.saveAndFlush(new ApplicationNumberSequence(
                seriesCode, seriesName, scopeType, scopeKey, prefix, sequencePadding, 0,
                description, separator, includeScopeInNumber)));

        int nextSequence = sequence.getLastSequence() + 1;
        sequence.setSeriesName(seriesName);
        sequence.setScopeType(scopeType);
        sequence.setPrefix(prefix);
        sequence.setSequencePadding(sequencePadding);
        sequence.setDescription(description);
        sequence.setSeparator(separator);
        sequence.setIncludeScopeInNumber(includeScopeInNumber);
        sequence.setLastSequence(nextSequence);
        sequenceRepository.save(sequence);

        return format(sequence, nextSequence);
    }

    private ApplicationNumberSequenceResponse toResponse(ApplicationNumberSequence sequence) {
        return new ApplicationNumberSequenceResponse(
            sequence.getId(),
            sequence.getSeriesCode(),
            sequence.getSeriesName(),
            sequence.getScopeType(),
            sequence.getScopeKey(),
            sequence.getPrefix(),
            sequence.getSequencePadding(),
            sequence.getLastSequence(),
            sequence.getLastSequence() > 0
                ? format(sequence, sequence.getLastSequence())
                : "—",
            format(sequence, sequence.getLastSequence() + 1),
            sequence.getDescription(),
            sequence.getCreatedAt(),
            sequence.getUpdatedAt()
        );
    }

    private String format(ApplicationNumberSequence seq, int sequence) {
        String seqStr = String.format("%0" + seq.getSequencePadding() + "d", sequence);
        if (!seq.isIncludeScopeInNumber()) {
            return seq.getPrefix() + seqStr;
        }
        String sep = seq.getSeparator() != null ? seq.getSeparator() : "-";
        return seq.getPrefix() + sep + seq.getScopeKey() + sep + seqStr;
    }
}
