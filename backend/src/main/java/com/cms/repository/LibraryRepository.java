package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.Library;

@Repository
public interface LibraryRepository extends JpaRepository<Library, Long> {

    List<Library> findAllByOrderByNameAsc();

    Optional<Library> findByCode(String code);
}
