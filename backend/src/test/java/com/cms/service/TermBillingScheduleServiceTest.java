package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.TermBillingScheduleDto;
import com.cms.dto.TermBillingScheduleRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.TermBillingSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.SystemConfigurationRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class TermBillingScheduleServiceTest {

    @Mock
    private TermBillingScheduleRepository scheduleRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private TermInstanceRepository termInstanceRepository;

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    private TermBillingScheduleService service;

    private AcademicYear testAcademicYear;
    private TermInstance oddTermInstance;

    @BeforeEach
    void setUp() {
        service = new TermBillingScheduleService(
            scheduleRepository, academicYearRepository, termInstanceRepository, systemConfigurationRepository);
        testAcademicYear = createAcademicYear(1L, "2026-2027");
        oddTermInstance = createTermInstance(testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));
    }

    @Test
    void shouldCreateNewTermBillingSchedule() {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 7, 31),
            LateFeeType.FLAT, new BigDecimal("500"), 5);

        TermBillingSchedule saved = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 5);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));
        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.empty());
        when(scheduleRepository.save(any(TermBillingSchedule.class))).thenReturn(saved);

        TermBillingScheduleDto dto = service.createOrUpdate(request);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.termType()).isEqualTo(TermType.ODD);
        assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(dto.lateFeeType()).isEqualTo(LateFeeType.FLAT);
        assertThat(dto.lateFeeAmount()).isEqualByComparingTo("500");
        assertThat(dto.graceDays()).isEqualTo(5);

        verify(scheduleRepository).save(any(TermBillingSchedule.class));
    }

    @Test
    void shouldUpdateExistingTermBillingSchedule() {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 8, 15),
            LateFeeType.PER_DAY, new BigDecimal("50"), 0);

        TermBillingSchedule existing = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 5);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));
        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(TermBillingSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TermBillingScheduleDto dto = service.createOrUpdate(request);

        assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(dto.lateFeeType()).isEqualTo(LateFeeType.PER_DAY);
        assertThat(dto.lateFeeAmount()).isEqualByComparingTo("50");
    }

    @Test
    void shouldThrowWhenAcademicYearNotFoundOnCreate() {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            999L, TermType.ODD, LocalDate.of(2026, 7, 31),
            LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrUpdate(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldUpdateByIdDirectly() {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 8, 1),
            LateFeeType.FLAT, new BigDecimal("1000"), 3);

        TermBillingSchedule existing = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 5);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));
        when(scheduleRepository.save(any(TermBillingSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TermBillingScheduleDto dto = service.update(1L, request);

        assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(dto.lateFeeAmount()).isEqualByComparingTo("1000");
        assertThat(dto.graceDays()).isEqualTo(3);
    }

    @Test
    void shouldThrowWhenScheduleNotFoundOnUpdate() {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 7, 31),
            LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetTermBillingSchedule() {
        TermBillingSchedule schedule = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(schedule));

        TermBillingScheduleDto dto = service.getTermBillingSchedule(1L, TermType.ODD);

        assertThat(dto.termType()).isEqualTo(TermType.ODD);
        assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void shouldThrowWhenScheduleNotFoundForGet() {
        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTermBillingSchedule(1L, TermType.EVEN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGetAllForAcademicYear() {
        TermBillingSchedule odd = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);
        TermBillingSchedule even = createSchedule(2L, testAcademicYear, TermType.EVEN,
            LocalDate.of(2027, 1, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(academicYearRepository.existsById(1L)).thenReturn(true);
        when(scheduleRepository.findByAcademicYearId(1L)).thenReturn(List.of(odd, even));

        List<TermBillingScheduleDto> dtos = service.getAllForAcademicYear(1L);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).termType()).isEqualTo(TermType.ODD);
        assertThat(dtos.get(1).termType()).isEqualTo(TermType.EVEN);
    }

    @Test
    void shouldThrowWhenAcademicYearNotFoundForGetAll() {
        when(academicYearRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getAllForAcademicYear(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetById() {
        TermBillingSchedule schedule = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        TermBillingScheduleDto dto = service.getById(1L);

        assertThat(dto.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenScheduleNotFoundById() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldComputeZeroLateFeeWhenOnTime() {
        TermBillingSchedule schedule = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(schedule));

        BigDecimal fee = service.computeLateFee(1L, TermType.ODD, LocalDate.of(2026, 7, 31));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldComputeZeroLateFeeWhenBeforeDueDate() {
        TermBillingSchedule schedule = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(schedule));

        BigDecimal fee = service.computeLateFee(1L, TermType.ODD, LocalDate.of(2026, 7, 15));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldComputeFlatLateFeeWhenLate() {
        TermBillingSchedule schedule = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(schedule));

        BigDecimal fee = service.computeLateFee(1L, TermType.ODD, LocalDate.of(2026, 8, 10));

        assertThat(fee).isEqualByComparingTo("500");
    }

    @Test
    void shouldComputePerDayLateFeeWhenLate() {
        TermBillingSchedule schedule = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.PER_DAY, new BigDecimal("50"), 0);

        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(schedule));

        // 5 days late * 50 per day = 250
        BigDecimal fee = service.computeLateFee(1L, TermType.ODD, LocalDate.of(2026, 8, 5));

        assertThat(fee).isEqualByComparingTo("250");
    }

    @Test
    void shouldRespectGraceDaysInLateFeeComputation() {
        // Due: July 31, grace: 5 days -> effective due: Aug 5
        TermBillingSchedule schedule = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 5);

        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(schedule));

        // Payment on Aug 5 = within grace period -> no late fee
        BigDecimal fee = service.computeLateFee(1L, TermType.ODD, LocalDate.of(2026, 8, 5));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowWhenNoScheduleFoundForComputeLateFee() {
        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.computeLateFee(1L, TermType.EVEN, LocalDate.of(2027, 2, 1)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUseDefaultGraceDaysWhenNullInRequest() {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 7, 31),
            LateFeeType.FLAT, new BigDecimal("500"), null);

        TermBillingSchedule saved = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));
        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.empty());
        when(scheduleRepository.save(any(TermBillingSchedule.class))).thenReturn(saved);

        TermBillingScheduleDto dto = service.createOrUpdate(request);

        assertThat(dto.graceDays()).isEqualTo(0);
    }

    @Test
    void shouldRejectDueDateAfterTermEndDate() {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 12, 1),
            LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));

        assertThatThrownBy(() -> service.createOrUpdate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ODD");

        verify(scheduleRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldRejectDueDateBeforeAdvanceWindow() {
        // ODD term starts 2026-06-01; default advance window is 30 days -> earliest allowed is 2026-05-02.
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 4, 1),
            LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));

        assertThatThrownBy(() -> service.createOrUpdate(request))
            .isInstanceOf(IllegalArgumentException.class);

        verify(scheduleRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldAllowDueDateWithinAdvanceWindowBeforeTermStart() {
        // 10 days before the ODD term's 2026-06-01 start, well within the default 30-day window.
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 5, 22),
            LateFeeType.FLAT, new BigDecimal("500"), 0);

        TermBillingSchedule saved = createSchedule(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 5, 22), LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));
        when(scheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.empty());
        when(scheduleRepository.save(any(TermBillingSchedule.class))).thenReturn(saved);

        TermBillingScheduleDto dto = service.createOrUpdate(request);

        assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 5, 22));
    }

    @Test
    void shouldRespectConfiguredAdvanceDaysOverDefault() {
        // Config overrides the 30-day default down to 5 -> 2026-05-22 (10 days early) should now fail.
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 5, 22),
            LateFeeType.FLAT, new BigDecimal("500"), 0);

        com.cms.model.SystemConfiguration config = new com.cms.model.SystemConfiguration(
            "fee.collection_advance_days", "5", "desc", com.cms.model.enums.ConfigDataType.INTEGER, "FEE", true);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));
        when(systemConfigurationRepository.findByConfigKey("fee.collection_advance_days"))
            .thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.createOrUpdate(request))
            .isInstanceOf(IllegalArgumentException.class);

        verify(scheduleRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldClampNegativeConfiguredAdvanceDaysToZero() {
        // A negative config value would otherwise push "earliest allowed" past the term start,
        // potentially inverting the allowed range. 2026-05-25 is 7 days before ODD's start
        // (2026-06-01) — would pass with the 30-day default, but must fail once clamped to 0.
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.ODD, LocalDate.of(2026, 5, 25),
            LateFeeType.FLAT, new BigDecimal("500"), 0);

        com.cms.model.SystemConfiguration config = new com.cms.model.SystemConfiguration(
            "fee.collection_advance_days", "-10", "desc", com.cms.model.enums.ConfigDataType.INTEGER, "FEE", true);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(oddTermInstance));
        when(systemConfigurationRepository.findByConfigKey("fee.collection_advance_days"))
            .thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.createOrUpdate(request))
            .isInstanceOf(IllegalArgumentException.class);

        verify(scheduleRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldThrowWhenNoTermInstanceConfiguredForDueDateValidation() {
        TermBillingScheduleRequest request = new TermBillingScheduleRequest(
            1L, TermType.EVEN, LocalDate.of(2027, 1, 31),
            LateFeeType.FLAT, new BigDecimal("500"), 0);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrUpdate(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("EVEN");

        verify(scheduleRepository, org.mockito.Mockito.never()).save(any());
    }

    private TermInstance createTermInstance(AcademicYear academicYear, TermType termType,
                                             LocalDate startDate, LocalDate endDate) {
        return new TermInstance(academicYear, termType, startDate, endDate, TermInstanceStatus.PLANNED);
    }

    private AcademicYear createAcademicYear(Long id, String name) {
        AcademicYear ay = new AcademicYear(name,
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), false);
        ay.setId(id);
        ay.setCreatedAt(Instant.now());
        ay.setUpdatedAt(Instant.now());
        return ay;
    }

    private TermBillingSchedule createSchedule(Long id, AcademicYear ay, TermType termType,
                                                LocalDate dueDate, LateFeeType lateFeeType,
                                                BigDecimal lateFeeAmount, Integer graceDays) {
        TermBillingSchedule s = new TermBillingSchedule(ay, termType, dueDate, lateFeeType, lateFeeAmount, graceDays);
        s.setId(id);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }
}
