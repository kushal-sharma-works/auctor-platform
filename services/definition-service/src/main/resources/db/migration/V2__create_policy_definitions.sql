CREATE TABLE policy_definitions (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    conditions JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    jpa_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, version)
);
