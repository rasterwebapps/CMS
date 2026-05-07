package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ScholarshipEligibilityRequest;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentScholarshipEligibility;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentScholarshipEligibilityRepository;

@ExtendWith(MockitoExtension.class)
class StudentScholarshipEligibilityServiceTest {
    @Mock private StudentRepository studentRepository;
    @Mock private StudentScholarshipEligibilityRepository eligibilityRepository;
    @Mock private StudentScholarshipService studentScholarshipService;
    private StudentScholarshipEligibilityService service;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new StudentScholarshipEligibilityService(studentRepository, eligibilityRepository, studentScholarshipService);
        Program program = new Program("Bachelor", "BACH", 4);
        student = new Student("S1", "A", "B", "a@test.com", program, 1, LocalDate.now(), StudentStatus.ACTIVE);
        student.setId(1L);
    }

    @Test
    void shouldCreateUpdateAndVerifyEligibility() {
        when(eligibilityRepository.findByStudentId(1L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(eligibilityRepository.save(any(StudentScholarshipEligibility.class))).thenAnswer(inv -> { StudentScholarshipEligibility e = inv.getArgument(0); e.setId(2L); return e; });
        when(studentScholarshipService.getEligibleScholarships(1L)).thenReturn(List.of());
        var initial = service.getEligibility(1L);
        assertThat(initial.studentId()).isEqualTo(1L);

        StudentScholarshipEligibility existing = new StudentScholarshipEligibility(); existing.setId(2L); existing.setStudent(student);
        when(eligibilityRepository.findByStudentId(1L)).thenReturn(Optional.of(existing));
        var req = new ScholarshipEligibilityRequest(true, true, false, false, new BigDecimal("250000"),
            "INC", "TAHSILDAR", LocalDate.now(),
            "COM", "AUTH", LocalDate.now(),
            "FG", "AUTH", LocalDate.now(),
            "Class 10", "Class 8",
            "123456789012", "00112233445566", "SBIN0001234", "SBI", "Salem Main", true);
        var updated = service.updateEligibility(1L, req, "admin");
        assertThat(updated.isFirstGraduate()).isTrue();
        assertThat(updated.isEconomicallyWeaker()).isTrue();
        assertThat(updated.aadhaarNumberMasked()).isEqualTo("XXXXXXXX9012");
        assertThat(updated.bankIfsc()).isEqualTo("SBIN0001234");
        assertThat(updated.dbtLinked()).isTrue();
        assertThat(student.isFirstGraduate()).isTrue();

        var verified = service.verifyEligibility(1L, "admin", "ok");
        assertThat(verified.verifiedBy()).isEqualTo("admin");
        assertThat(verified.verifiedAt()).isNotNull();
    }
}

