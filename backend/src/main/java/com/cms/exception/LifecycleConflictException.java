package com.cms.exception;

public class LifecycleConflictException extends RuntimeException {

    private final String code;
    private final String entity;
    private final Long entityId;
    private final Integer blockerCount;

    public LifecycleConflictException(String message, String code, String entity, Long entityId, Integer blockerCount) {
        super(message);
        this.code = code;
        this.entity = entity;
        this.entityId = entityId;
        this.blockerCount = blockerCount;
    }

    public String getCode() {
        return code;
    }

    public String getEntity() {
        return entity;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Integer getBlockerCount() {
        return blockerCount;
    }
}

