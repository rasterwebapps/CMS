package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ApplicationNumberSequenceResponse;
import com.cms.model.AcademicYear;
import com.cms.model.ApplicationNumberSequence;
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

    @Transactional
    public String nextAdmissionNumber(AcademicYear academicYear) {
        String scopeKey = toAcademicYearCode(academicYear);
        return nextNumber(
            ADMISSION_SERIES,
            "Admission Number",
            "ACADEMIC_YEAR",
            scopeKey,
            "ADM",
            4,
            "Permanent admission reference generated when admission is completed"
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
        ApplicationNumberSequence sequence = sequenceRepository
            .findBySeriesCodeAndScopeKeyForUpdate(seriesCode, scopeKey)
            .orElseGet(() -> sequenceRepository.saveAndFlush(new ApplicationNumberSequence(
                seriesCode, seriesName, scopeType, scopeKey, prefix, sequencePadding, 0, description)));

        int nextSequence = sequence.getLastSequence() + 1;
        sequence.setSeriesName(seriesName);
        sequence.setScopeType(scopeType);
        sequence.setPrefix(prefix);
        sequence.setSequencePadding(sequencePadding);
        sequence.setDescription(description);
        sequence.setLastSequence(nextSequence);
        sequenceRepository.save(sequence);

        return format(prefix, scopeKey, nextSequence, sequencePadding);
    }

    public String toAcademicYearCode(AcademicYear academicYear) {
        if (academicYear == null || academicYear.getName() == null) {
            throw new IllegalArgumentException("Academic year is required for admission number generation");
        }
        String[] parts = academicYear.getName().split("-");
        if (parts.length >= 2 && parts[0].length() >= 4 && parts[1].length() >= 4) {
            return parts[0].substring(parts[0].length() - 2) + parts[1].substring(parts[1].length() - 2);
        }
        int startYear = academicYear.getStartYear();
        return String.format("%02d%02d", startYear % 100, (startYear + 1) % 100);
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
                ? format(sequence.getPrefix(), sequence.getScopeKey(), sequence.getLastSequence(), sequence.getSequencePadding())
                : "—",
            format(sequence.getPrefix(), sequence.getScopeKey(), sequence.getLastSequence() + 1, sequence.getSequencePadding()),
            sequence.getDescription(),
            sequence.getCreatedAt(),
            sequence.getUpdatedAt()
        );
    }

    private String format(String prefix, String scopeKey, int sequence, int padding) {
        return String.format("%s-%s-%0" + padding + "d", prefix, scopeKey, sequence);
    }
}
