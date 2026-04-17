package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Agent;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findByIsActiveTrue();

    List<Agent> findByNameContainingIgnoreCase(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
