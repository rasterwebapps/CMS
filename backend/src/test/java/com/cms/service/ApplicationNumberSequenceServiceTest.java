package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.AcademicYear;
import com.cms.model.ApplicationNumberSequence;
import com.cms.repository.ApplicationNumberSequenceRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationNumberSequenceServiceTest {

    @Mock
    private ApplicationNumberSequenceRepository sequenceRepository;

    private ApplicationNumberSequenceService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationNumberSequenceService(sequenceRepository);
    }

    @Test
    void nextAdmissionNumberCreatesAcademicYearScopedSequence() {
        AcademicYear academicYear = new AcademicYear(
            "2025-2026", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true);

        when(sequenceRepository.findBySeriesCodeAndScopeKeyForUpdate("ADMISSION_NUMBER", "2526"))
            .thenReturn(Optional.empty());
        when(sequenceRepository.saveAndFlush(any(ApplicationNumberSequence.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceRepository.save(any(ApplicationNumberSequence.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String admissionNumber = service.nextAdmissionNumber(academicYear);

        assertThat(admissionNumber).isEqualTo("ADM-2526-0001");
        ArgumentCaptor<ApplicationNumberSequence> captor = ArgumentCaptor.forClass(ApplicationNumberSequence.class);
        verify(sequenceRepository).save(captor.capture());
        assertThat(captor.getValue().getLastSequence()).isEqualTo(1);
        assertThat(captor.getValue().getScopeKey()).isEqualTo("2526");
    }

    @Test
    void nextReceiptNumberIncrementsExistingSequence() {
        ApplicationNumberSequence existing = new ApplicationNumberSequence(
            "RECEIPT_NUMBER", "Receipt Number", "CALENDAR_YEAR", "2026", "RCP", 5, 41,
            "Global receipt number generated for every payment receipt");

        when(sequenceRepository.findBySeriesCodeAndScopeKeyForUpdate("RECEIPT_NUMBER", "2026"))
            .thenReturn(Optional.of(existing));
        when(sequenceRepository.save(any(ApplicationNumberSequence.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String receiptNumber = service.nextReceiptNumber(2026);

        assertThat(receiptNumber).isEqualTo("RCP-2026-00042");
        assertThat(existing.getLastSequence()).isEqualTo(42);
    }
}

