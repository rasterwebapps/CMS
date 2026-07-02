package com.cms.service;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.RevokedToken;
import com.cms.repository.RevokedTokenRepository;

@Service
public class TokenRevocationService {

    private final RevokedTokenRepository repository;

    public TokenRevocationService(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void revoke(String jti, Instant expiresAt) {
        if (!repository.existsByJti(jti)) {
            repository.save(new RevokedToken(jti, expiresAt));
        }
    }

    public boolean isRevoked(String jti) {
        return repository.existsByJti(jti);
    }

    // Hourly cleanup — removes entries whose tokens have already expired naturally
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpired() {
        repository.deleteExpiredBefore(Instant.now());
    }
}
