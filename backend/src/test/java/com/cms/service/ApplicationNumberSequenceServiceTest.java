package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.AcademicYear;
import com.cms.model.Course;
import com.cms.model.NumberSequenceCounter;
import com.cms.model.NumberSeriesDefinition;
import com.cms.model.Program;
import com.cms.model.enums.ProgramStatus;
import com.cms.repository.NumberSequenceCounterRepository;
import com.cms.repository.NumberSeriesDefinitionRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationNumberSequenceServiceTest {

    @Mock private NumberSeriesDefinitionRepository definitionRepository;
    @Mock private NumberSequenceCounterRepository  counterRepository;

    private ApplicationNumberSequenceService service;

    private NumberSeriesDefinition admissionDef;
    private NumberSeriesDefinition receiptDef;

    @BeforeEach
    void setUp() {
        service = new ApplicationNumberSequenceService(definitionRepository, counterRepository);

        admissionDef = new NumberSeriesDefinition();
        admissionDef.setSeriesCode("ADMISSION_NUMBER");
        admissionDef.setSeriesName("Admission Number");
        admissionDef.setScopeType("CALENDAR_YEAR_COURSE");
        admissionDef.setSequencePadding(4);
        admissionDef.setSeparator("");
        admissionDef.setActive(true);

        receiptDef = new NumberSeriesDefinition();
        receiptDef.setSeriesCode("RECEIPT_NUMBER");
        receiptDef.setSeriesName("Receipt Number");
        receiptDef.setScopeType("CALENDAR_YEAR");
        receiptDef.setPrefix("RCP");
        receiptDef.setSequencePadding(5);
        receiptDef.setSeparator("-");
        receiptDef.setActive(true);
    }

    // ── nextAdmissionNumber ───────────────────────────────────────────────────

    @Test
    void nextAdmissionNumber_generatesFirstNumberForNewScopeKey() {
        AcademicYear ay = new AcademicYear("2026-2027",
                LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode("65");

        when(definitionRepository.findBySeriesCode("ADMISSION_NUMBER")).thenReturn(Optional.of(admissionDef));
        when(counterRepository.findBySeriesCodeAndScopeKeyForUpdate("ADMISSION_NUMBER", "202665"))
                .thenReturn(Optional.empty());

        NumberSequenceCounter newCounter = new NumberSequenceCounter();
        newCounter.setSeriesCode("ADMISSION_NUMBER");
        newCounter.setScopeKey("202665");
        newCounter.setLastSequence(0);
        when(counterRepository.saveAndFlush(any())).thenReturn(newCounter);
        when(counterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.nextAdmissionNumber(ay, course);

        assertThat(result).isEqualTo("2026650001");
        ArgumentCaptor<NumberSequenceCounter> captor = ArgumentCaptor.forClass(NumberSequenceCounter.class);
        verify(counterRepository).save(captor.capture());
        assertThat(captor.getValue().getLastSequence()).isEqualTo(1);
    }

    @Test
    void nextAdmissionNumber_incrementsExistingCounter() {
        AcademicYear ay = new AcademicYear("2026-2027",
                LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode("68");

        NumberSequenceCounter existing = counterWith("ADMISSION_NUMBER", "202668", 15);

        when(definitionRepository.findBySeriesCode("ADMISSION_NUMBER")).thenReturn(Optional.of(admissionDef));
        when(counterRepository.findBySeriesCodeAndScopeKeyForUpdate("ADMISSION_NUMBER", "202668"))
                .thenReturn(Optional.of(existing));
        when(counterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.nextAdmissionNumber(ay, course);

        assertThat(result).isEqualTo("2026680016");
        assertThat(existing.getLastSequence()).isEqualTo(16);
    }

    @Test
    void nextAdmissionNumber_overflowsBeyondFourDigits() {
        AcademicYear ay = new AcademicYear("2026-2027",
                LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode("65");
        NumberSequenceCounter existing = counterWith("ADMISSION_NUMBER", "202665", 9999);

        when(definitionRepository.findBySeriesCode("ADMISSION_NUMBER")).thenReturn(Optional.of(admissionDef));
        when(counterRepository.findBySeriesCodeAndScopeKeyForUpdate("ADMISSION_NUMBER", "202665"))
                .thenReturn(Optional.of(existing));
        when(counterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.nextAdmissionNumber(ay, course);

        assertThat(result).isEqualTo("202665" + "10000");
        assertThat(existing.getLastSequence()).isEqualTo(10000);
    }

    @Test
    void nextAdmissionNumber_throwsWhenCourseCodeMissing() {
        AcademicYear ay = new AcademicYear("2026-2027",
                LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode(null);

        assertThatThrownBy(() -> service.nextAdmissionNumber(ay, course))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("roll_number_code");
    }

    @Test
    void nextAdmissionNumber_throwsWhenCourseIsNull() {
        AcademicYear ay = new AcademicYear("2026-2027",
                LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);

        assertThatThrownBy(() -> service.nextAdmissionNumber(ay, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nextAdmissionNumber_throwsWhenSeriesDefinitionMissing() {
        AcademicYear ay = new AcademicYear("2026-2027",
                LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode("65");

        when(definitionRepository.findBySeriesCode("ADMISSION_NUMBER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.nextAdmissionNumber(ay, course))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMISSION_NUMBER");
    }

    // ── nextReceiptNumber ─────────────────────────────────────────────────────

    @Test
    void nextReceiptNumber_formatsWithPrefixSeparatorAndPadding() {
        NumberSequenceCounter existing = counterWith("RECEIPT_NUMBER", "2026", 41);

        when(definitionRepository.findBySeriesCode("RECEIPT_NUMBER")).thenReturn(Optional.of(receiptDef));
        when(counterRepository.findBySeriesCodeAndScopeKeyForUpdate("RECEIPT_NUMBER", "2026"))
                .thenReturn(Optional.of(existing));
        when(counterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.nextReceiptNumber(2026);

        assertThat(result).isEqualTo("RCP-2026-00042");
        assertThat(existing.getLastSequence()).isEqualTo(42);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsResponsesEnrichedFromDefinitions() {
        NumberSequenceCounter counter = counterWith("RECEIPT_NUMBER", "2026", 41);

        when(counterRepository.findAll()).thenReturn(List.of(counter));
        when(definitionRepository.findAll()).thenReturn(List.of(receiptDef));

        var result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).seriesCode()).isEqualTo("RECEIPT_NUMBER");
        assertThat(result.get(0).lastSequence()).isEqualTo(41);
        assertThat(result.get(0).prefix()).isEqualTo("RCP");
        assertThat(result.get(0).scopeKey()).isEqualTo("2026");
    }

    @Test
    void findAll_lastGeneratedNumberIsDashWhenLastSequenceIsZero() {
        NumberSequenceCounter counter = counterWith("RECEIPT_NUMBER", "2026", 0);

        when(counterRepository.findAll()).thenReturn(List.of(counter));
        when(definitionRepository.findAll()).thenReturn(List.of(receiptDef));

        var result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lastGeneratedNumber()).isEqualTo("—");
        assertThat(result.get(0).nextPreviewNumber()).isEqualTo("RCP-2026-00001");
    }

    @Test
    void findAll_omitsCountersWithNoMatchingDefinition() {
        NumberSequenceCounter orphan = counterWith("OBSOLETE_SERIES", "2026", 5);

        when(counterRepository.findAll()).thenReturn(List.of(orphan));
        when(definitionRepository.findAll()).thenReturn(List.of(receiptDef));

        var result = service.findAll();

        assertThat(result).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Course courseWithCode(String code) {
        Program program = new Program("BSc Nursing", "BSCN", 4, ProgramStatus.ACTIVE);
        Course course = new Course("BSc Nursing", "BSCN", null, program);
        course.setRollNumberCode(code);
        return course;
    }

    private NumberSequenceCounter counterWith(String seriesCode, String scopeKey, int lastSeq) {
        NumberSequenceCounter c = new NumberSequenceCounter();
        c.setSeriesCode(seriesCode);
        c.setScopeKey(scopeKey);
        c.setLastSequence(lastSeq);
        return c;
    }
}
