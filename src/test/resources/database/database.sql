CREATE TABLE seed (
    type VARCHAR(64) NOT NULL UNIQUE,
    value BIGINT NOT NULL,
    PRIMARY KEY (type));

CREATE TABLE minted(
    type VARCHAR(64) NOT NULL,
    value VARCHAR(64) NOT NULL,
    created VARCHAR(29) NOT NULL,
    PRIMARY KEY (type, value));