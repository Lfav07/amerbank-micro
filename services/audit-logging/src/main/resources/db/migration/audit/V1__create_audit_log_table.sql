CREATE TABLE audit_log
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


CREATE INDEX idx_audit_event_type ON audit_log (event_type);
CREATE INDEX idx_audit_actor_id ON audit_log (actor_id);
CREATE INDEX idx_audit_entity_id ON audit_log (entity_id);
CREATE INDEX idx_audit_service_name ON audit_log (service);
CREATE INDEX idx_audit_correlation_id ON audit_log (correlation_id);
CREATE INDEX idx_audit_timestamp ON audit_log (timestamp);


CREATE INDEX idx_audit_actor_timestamp
    ON audit_log (actor_id, timestamp DESC);

CREATE INDEX idx_audit_entity_timestamp
    ON audit_log (entity_id, timestamp DESC);