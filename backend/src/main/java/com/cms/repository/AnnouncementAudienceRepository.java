package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.AnnouncementAudience;
import com.cms.model.enums.AnnouncementAudienceType;

public interface AnnouncementAudienceRepository extends JpaRepository<AnnouncementAudience, Long> {

    /** Every announcement id with an ALL row, or a ROLE/COHORT/SECTION row matching one of the
     *  caller's own resolved audience keys (their role name's id, their cohort id(s), their
     *  section id(s)) -- callers pass an empty list for a dimension they don't have (e.g. a
     *  guardian's own roleId list is just their PARENT role id; cohortIds/sectionIds come from
     *  each linked ward). */
    @Query("""
        SELECT DISTINCT aa.announcement.id FROM AnnouncementAudience aa
        WHERE aa.audienceType = com.cms.model.enums.AnnouncementAudienceType.ALL
           OR (aa.audienceType = com.cms.model.enums.AnnouncementAudienceType.ROLE AND aa.audienceRefId IN :roleIds)
           OR (aa.audienceType = com.cms.model.enums.AnnouncementAudienceType.COHORT AND aa.audienceRefId IN :cohortIds)
           OR (aa.audienceType = com.cms.model.enums.AnnouncementAudienceType.SECTION AND aa.audienceRefId IN :sectionIds)
        """)
    List<Long> findMatchingAnnouncementIds(@Param("roleIds") List<Long> roleIds,
                                            @Param("cohortIds") List<Long> cohortIds,
                                            @Param("sectionIds") List<Long> sectionIds);

    boolean existsByAudienceTypeAndAudienceRefId(AnnouncementAudienceType type, Long refId);
}
