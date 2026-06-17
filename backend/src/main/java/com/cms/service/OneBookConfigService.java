package com.cms.service;

import org.springframework.stereotype.Service;

import com.cms.repository.SystemConfigurationRepository;

/**
 * Lightweight accessor for OneBook integration configuration keys.
 * Reads directly from the DB on each call — values are short strings so
 * no caching layer is needed; if performance becomes a concern, add a
 * @Cacheable here.
 */
@Service
public class OneBookConfigService {

    private final SystemConfigurationRepository repo;

    public OneBookConfigService(SystemConfigurationRepository repo) {
        this.repo = repo;
    }

    public boolean isEnabled() {
        return "true".equalsIgnoreCase(get("onebook.enabled", "false"));
    }

    public boolean isAllowCashInCms() {
        return "true".equalsIgnoreCase(get("onebook.allow_cash_in_cms", "true"));
    }

    public String getApiUrl() {
        return get("onebook.api_url", "");
    }

    public String getUsername() {
        return get("onebook.username", "");
    }

    public String getPassword() {
        return get("onebook.password", "");
    }

    public String getOrgId() {
        return get("onebook.org_id", "");
    }

    public String getBranchId() {
        return get("onebook.branch_id", "");
    }

    public String getAppName() {
        return get("onebook.app_name", "ONECMS");
    }

    public String getPaperName() {
        return get("onebook.paper_name", "SKS College Of Nursing");
    }

    public String getWebhookSecret() {
        return get("onebook.webhook_secret", "");
    }

    private String get(String key, String defaultValue) {
        return repo.findByConfigKey(key)
                .map(c -> c.getConfigValue())
                .orElse(defaultValue);
    }
}
