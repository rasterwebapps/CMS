CREATE TABLE enquiries (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    program_id BIGINT REFERENCES programs(id),
    enquiry_date DATE NOT NULL,
    source VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    agent_id BIGINT REFERENCES agents(id),
    assigned_to VARCHAR(255),
    remarks TEXT,
    fee_discussed_amount NUMERIC(10, 2),
    converted_student_id BIGINT REFERENCES students(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_enquiries_status ON enquiries(status);
CREATE INDEX idx_enquiries_agent ON enquiries(agent_id);
CREATE INDEX idx_enquiries_program ON enquiries(program_id);
