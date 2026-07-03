package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.LibraryPeriodical;
import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;

public interface LibraryPeriodicalRepository extends JpaRepository<LibraryPeriodical, Long>, JpaSpecificationExecutor<LibraryPeriodical> {

    List<LibraryPeriodical> findBySubscriptionStatus(SubscriptionStatus status);

    List<LibraryPeriodical> findByJournalType(JournalType journalType);

    List<LibraryPeriodical> findByYear(Integer year);
}
