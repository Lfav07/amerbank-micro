CREATE TABLE audit_logs
(
    event_id       UUID PRIMARY KEY,
    event_type     VARCHAR(64) NOT NULL,
    timestamp      timestamptz NOT NULL,
    service        VARCHAR(64) NOT NULL,

    actor_id       VARCHAR(64),
    entity_id      VARCHAR(64),
    entity_type    VARCHAR(32),

    status         VARCHAR(32) NOT NULL,
    correlation_id VARCHAR(128),

    payload        jsonb       NOT NULL
);


CREATE INDEX idx_audit_event_type ON audit_logs (event_type);
CREATE INDEX idx_audit_actor_id ON audit_logs (actor_id);
CREATE INDEX idx_audit_entity_id ON audit_logs (entity_id);
CREATE INDEX idx_audit_service_name ON audit_logs (service);
CREATE INDEX idx_audit_correlation_id ON audit_logs (correlation_id);
CREATE INDEX idx_audit_timestamp ON audit_logs (timestamp);


CREATE INDEX idx_audit_actor_timestamp
    ON audit_logs (actor_id, timestamp DESC);

CREATE INDEX idx_audit_entity_timestamp
    ON audit_logs (entity_id, timestamp DESC);