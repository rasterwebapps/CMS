package com.cms.inventory.procurement.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.WantedListItem;
import com.cms.inventory.procurement.model.enums.WantedListItemStatus;

@Repository
public interface WantedListItemRepository extends JpaRepository<WantedListItem, Long>, JpaSpecificationExecutor<WantedListItem> {

    /** Every (product, location) pair that already has an unresolved line — the shortage job
     *  skips these rather than creating a duplicate; the pair only becomes eligible again once
     *  its existing line reaches a terminal state (REJECTED/CONVERTED) or is reopened and
     *  resolved. */
    @Query("""
        SELECT CONCAT(w.product.id, '-', w.location.id)
        FROM WantedListItem w
        WHERE w.status IN :unresolvedStatuses
        """)
    Set<String> findUnresolvedProductLocationKeys(@Param("unresolvedStatuses") List<WantedListItemStatus> unresolvedStatuses);
}
