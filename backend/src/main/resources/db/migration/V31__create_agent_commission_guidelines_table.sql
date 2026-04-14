CREATE TABLE agent_commission_guidelines (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(id),
    program_id BIGINT NOT NULL REFERENCES programs(id),
    locality_type VARCHAR(50) NOT NULL,
    suggested_commission NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_commission_guidelines_agent ON agent_commission_guidelines(agent_id);
CREATE INDEX idx_agent_commission_guidelines_program ON agent_commission_guidelines(program_id);
