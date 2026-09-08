package com.cms.inventory.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.ticket.model.ServiceTicket;

@Repository
public interface ServiceTicketRepository extends JpaRepository<ServiceTicket, Long>, JpaSpecificationExecutor<ServiceTicket> {
}
