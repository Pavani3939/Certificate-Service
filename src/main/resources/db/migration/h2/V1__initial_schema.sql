-- H2 Local & Test Schema

-- Designs Table
CREATE TABLE designs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Programmes Table
CREATE TABLE programmes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Programme Design Assignments (Temporal History)
CREATE TABLE programme_design_assignments (
    id UUID PRIMARY KEY,
    programme_id UUID NOT NULL,
    design_id UUID NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_pda_programme FOREIGN KEY (programme_id) REFERENCES programmes(id),
    CONSTRAINT fk_pda_design FOREIGN KEY (design_id) REFERENCES designs(id)
);

CREATE INDEX idx_pda_programme_effective ON programme_design_assignments (programme_id, effective_from DESC);

-- Certificates Table (Immutable Snapshots + Status & Cancellation)
CREATE TABLE certificates (
    id UUID PRIMARY KEY,
    person_name VARCHAR(255) NOT NULL,
    person_email VARCHAR(255) NOT NULL,
    programme_id UUID NOT NULL,
    programme_name_snapshot VARCHAR(255) NOT NULL,
    design_id UUID NOT NULL,
    design_name_snapshot VARCHAR(255) NOT NULL,
    design_content_snapshot TEXT NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_cert_programme FOREIGN KEY (programme_id) REFERENCES programmes(id),
    CONSTRAINT fk_cert_design FOREIGN KEY (design_id) REFERENCES designs(id)
);

CREATE INDEX idx_certificates_person_email ON certificates (person_email, issued_at DESC);
CREATE INDEX idx_certificates_programme_id ON certificates (programme_id);
CREATE INDEX idx_certificates_prog_person_status ON certificates (programme_id, person_email, status);
