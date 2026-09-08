package com.cms.inventory.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.ticket.model.ServiceTicketCategory;

@Repository
public interface ServiceTicketCategoryRepository extends JpaRepository<ServiceTicketCategory, Long>, JpaSpecificationExecutor<ServiceTicketCategory> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<ServiceTicketCategory> findByIsActiveTrueOrderByNameAsc();

    List<ServiceTicketCategory> findAllByOrderByNameAsc();
}
