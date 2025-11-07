CREATE TABLE IF NOT EXISTS entity(
    id BIGSERIAL PRIMARY KEY ,
    name text
);
CREATE TABLE IF NOT EXISTS audit_log(
    id BIGSERIAL PRIMARY KEY ,
    method text,
    create_time timestamp
);