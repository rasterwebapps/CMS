package com.cms.service;

import java.time.DateTimeException;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.cms.repository.SystemConfigurationRepository;

/**
 * Provides the application-wide ZoneId for sequence period boundary computations.
 * Reads from system_configurations key "app.timezone" with a 5-minute in-memory cache
 * so every sequence generation does not hit the DB.
 * Falls back to UTC if the key is absent or contains an invalid zone ID.
 */
@Service
public class AppTimezoneService {

    private static final String CONFIG_KEY = "app.timezone";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final ZoneId UTC = ZoneId.of("UTC");

    private final SystemConfigurationRepository configRepository;

    private volatile ZoneId cachedZone;
    private volatile long cacheExpiresAt = 0;

    public AppTimezoneService(SystemConfigurationRepository configRepository) {
        this.configRepository = configRepository;
    }

    public ZoneId getZone() {
        long now = System.currentTimeMillis();
        if (cachedZone == null || now >= cacheExpiresAt) {
            cachedZone = resolve();
            cacheExpiresAt = now + CACHE_TTL_MS;
        }
        return cachedZone;
    }

    /** Force cache invalidation — call after updating app.timezone in system_configurations. */
    public void invalidateCache() {
        cacheExpiresAt = 0;
    }

    private ZoneId resolve() {
        return configRepository.findByConfigKey(CONFIG_KEY)
            .map(config -> {
                try {
                    return ZoneId.of(config.getConfigValue());
                } catch (DateTimeException e) {
                    return UTC;
                }
            })
            .orElse(UTC);
    }
}
