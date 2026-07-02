package com.cms.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt < :now")
    void deleteExpiredBefore(Instant now);
}
