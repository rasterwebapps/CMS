package com.cms.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.cms.config.JpaConfig;
import com.cms.model.Speciality;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class SpecialityRepositoryTest {

    @Autowired
    private SpecialityRepository specialityRepository;

    @BeforeEach
    void setUp() {
        specialityRepository.deleteAll();
    }

    @Test
    void shouldSaveSpeciality() {
        Speciality speciality = new Speciality(
            "Computer Science",
            "CS",
            "Speciality of Computer Science",
            null,
            "Dr. John Doe"
        );

        Speciality saved = specialityRepository.save(speciality);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Computer Science");
        assertThat(saved.getCode()).isEqualTo("CS");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindSpecialityById() {
        Speciality speciality = new Speciality(
            "Mathematics",
            "MATH",
            "Speciality of Mathematics",
            null,
            "Dr. Jane Smith"
        );
        Speciality saved = specialityRepository.save(speciality);

        Optional<Speciality> found = specialityRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Mathematics");
    }

    @Test
    void shouldFindSpecialityByCode() {
        Speciality speciality = new Speciality(
            "Physics",
            "PHY",
            "Speciality of Physics",
            null,
            "Dr. Einstein"
        );
        specialityRepository.save(speciality);

        Optional<Speciality> found = specialityRepository.findByCode("PHY");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Physics");
    }

    @Test
    void shouldReturnEmptyWhenFindByCodeNotExists() {
        Optional<Speciality> found = specialityRepository.findByCode("NONEXISTENT");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckIfSpecialityExistsByCode() {
        Speciality speciality = new Speciality(
            "Chemistry",
            "CHEM",
            "Speciality of Chemistry",
            null,
            "Dr. Curie"
        );
        specialityRepository.save(speciality);

        boolean exists = specialityRepository.existsByCode("CHEM");
        boolean notExists = specialityRepository.existsByCode("NONEXISTENT");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldFindAllSpecialities() {
        Speciality dept1 = new Speciality("CS", "CS", "CS Dept", null, "Dr. A");
        Speciality dept2 = new Speciality("Math", "MATH", "Math Dept", null, "Dr. B");
        specialityRepository.save(dept1);
        specialityRepository.save(dept2);

        List<Speciality> specialities = specialityRepository.findAll();

        assertThat(specialities).hasSize(2);
    }

    @Test
    void shouldUpdateSpeciality() {
        Speciality speciality = new Speciality(
            "Biology",
            "BIO",
            "Speciality of Biology",
            null,
            "Dr. Darwin"
        );
        Speciality saved = specialityRepository.save(speciality);

        saved.setName("Life Sciences");
        saved.setHodName("Dr. Updated");
        Speciality updated = specialityRepository.save(saved);

        assertThat(updated.getName()).isEqualTo("Life Sciences");
        assertThat(updated.getHodName()).isEqualTo("Dr. Updated");
    }

    @Test
    void shouldDeleteSpeciality() {
        Speciality speciality = new Speciality(
            "History",
            "HIST",
            "Speciality of History",
            null,
            "Dr. Historian"
        );
        Speciality saved = specialityRepository.save(speciality);
        Long id = saved.getId();

        specialityRepository.deleteById(id);

        Optional<Speciality> found = specialityRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenSpecialityExistsById() {
        Speciality speciality = new Speciality(
            "Geography",
            "GEO",
            "Speciality of Geography",
            null,
            "Dr. Geo"
        );
        Speciality saved = specialityRepository.save(speciality);

        boolean exists = specialityRepository.existsById(saved.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSpecialityNotExistsById() {
        boolean exists = specialityRepository.existsById(999L);

        assertThat(exists).isFalse();
    }

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnSave() {
        Speciality speciality = new Speciality(
            "Art",
            "ART",
            "Speciality of Art",
            null,
            "Dr. Artist"
        );

        Speciality saved = specialityRepository.save(speciality);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }
}
