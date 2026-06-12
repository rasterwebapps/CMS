package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import java.util.List;

import com.cms.dto.ApplicationNumberSequenceResponse;
import com.cms.model.AcademicYear;
import com.cms.model.ApplicationNumberSequence;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.model.enums.ProgramStatus;
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

    private Course courseWithCode(String admissionCode) {
        Program program = new Program("BSc Nursing", "BSCN", 4, ProgramStatus.ACTIVE);
        Course course = new Course("BSc Nursing", "BSCN", null, program);
        course.setRollNumberCode(admissionCode);
        return course;
    }

    @Test
    void nextAdmissionNumberGeneratesYearCourseSequence() {
        AcademicYear academicYear = new AcademicYear(
            "2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode("65");

        when(sequenceRepository.findBySeriesCodeAndScopeKeyForUpdate("ADMISSION_NUMBER", "202665"))
            .thenReturn(Optional.empty());
        when(sequenceRepository.saveAndFlush(any(ApplicationNumberSequence.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceRepository.save(any(ApplicationNumberSequence.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String admissionNumber = service.nextAdmissionNumber(academicYear, course);

        assertThat(admissionNumber).isEqualTo("2026650001");
        ArgumentCaptor<ApplicationNumberSequence> captor = ArgumentCaptor.forClass(ApplicationNumberSequence.class);
        verify(sequenceRepository).save(captor.capture());
        assertThat(captor.getValue().getLastSequence()).isEqualTo(1);
        assertThat(captor.getValue().getScopeKey()).isEqualTo("202665");
        assertThat(captor.getValue().isIncludeScopeInNumber()).isFalse();
        assertThat(captor.getValue().getSeparator()).isEqualTo("");
    }

    @Test
    void nextAdmissionNumberIncrementsExistingSequence() {
        AcademicYear academicYear = new AcademicYear(
            "2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode("68");

        ApplicationNumberSequence existing = new ApplicationNumberSequence(
            "ADMISSION_NUMBER", "Admission Number", "CALENDAR_YEAR_COURSE", "202668",
            "202668", 4, 15,
            "Admission number: {year}{courseCode}{seq}", "", false);
        when(sequenceRepository.findBySeriesCodeAndScopeKeyForUpdate("ADMISSION_NUMBER", "202668"))
            .thenReturn(Optional.of(existing));
        when(sequenceRepository.save(any(ApplicationNumberSequence.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String admissionNumber = service.nextAdmissionNumber(academicYear, course);

        assertThat(admissionNumber).isEqualTo("2026680016");
        assertThat(existing.getLastSequence()).isEqualTo(16);
    }

    @Test
    void nextAdmissionNumberOverflowsToFiveDigits() {
        AcademicYear academicYear = new AcademicYear(
            "2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode("65");

        ApplicationNumberSequence existing = new ApplicationNumberSequence(
            "ADMISSION_NUMBER", "Admission Number", "CALENDAR_YEAR_COURSE", "202665",
            "202665", 4, 9999,
            "Admission number: {year}{courseCode}{seq}", "", false);
        when(sequenceRepository.findBySeriesCodeAndScopeKeyForUpdate("ADMISSION_NUMBER", "202665"))
            .thenReturn(Optional.of(existing));
        when(sequenceRepository.save(any(ApplicationNumberSequence.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String admissionNumber = service.nextAdmissionNumber(academicYear, course);

        assertThat(admissionNumber).isEqualTo("202665" + "10000");
        assertThat(existing.getLastSequence()).isEqualTo(10000);
    }

    @Test
    void nextAdmissionNumberThrowsWhenCourseCodeMissing() {
        AcademicYear academicYear = new AcademicYear(
            "2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        Course course = courseWithCode(null);

        assertThatThrownBy(() -> service.nextAdmissionNumber(academicYear, course))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("roll_number_code");
    }

    @Test
    void nextAdmissionNumberThrowsWhenCourseIsNull() {
        AcademicYear academicYear = new AcademicYear(
            "2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);

        assertThatThrownBy(() -> service.nextAdmissionNumber(academicYear, null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findAllReturnsAllSequences() {
        ApplicationNumberSequence seq = new ApplicationNumberSequence(
            "RECEIPT_NUMBER", "Receipt Number", "CALENDAR_YEAR", "2026", "RCP", 5, 41,
            "Global receipt number generated for every payment receipt");
        when(sequenceRepository.findAll()).thenReturn(List.of(seq));

        List<ApplicationNumberSequenceResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).seriesCode()).isEqualTo("RECEIPT_NUMBER");
        assertThat(result.get(0).lastSequence()).isEqualTo(41);
        assertThat(result.get(0).prefix()).isEqualTo("RCP");
        assertThat(result.get(0).scopeKey()).isEqualTo("2026");
    }

    @Test
    void findAllFormatsAdmissionNumberCorrectly() {
        ApplicationNumberSequence seq = new ApplicationNumberSequence(
            "ADMISSION_NUMBER", "Admission Number", "CALENDAR_YEAR_COURSE", "202665",
            "202665", 4, 5,
            "Admission number: {year}{courseCode}{seq}", "", false);
        when(sequenceRepository.findAll()).thenReturn(List.of(seq));

        List<ApplicationNumberSequenceResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lastGeneratedNumber()).isEqualTo("2026650005");
        assertThat(result.get(0).nextPreviewNumber()).isEqualTo("2026650006");
    }

    @Test
    void findAllReturnsSequenceWithZeroLastSequence() {
        ApplicationNumberSequence seq = new ApplicationNumberSequence(
            "ADMISSION_NUMBER", "Admission Number", "CALENDAR_YEAR_COURSE", "202665",
            "202665", 4, 0,
            "Admission number sequence", "", false);
        when(sequenceRepository.findAll()).thenReturn(List.of(seq));

        List<ApplicationNumberSequenceResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lastGeneratedNumber()).isEqualTo("—");
        assertThat(result.get(0).nextPreviewNumber()).isEqualTo("2026650001");
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
