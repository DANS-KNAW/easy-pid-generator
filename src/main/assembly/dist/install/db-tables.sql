CREATE TABLE seed(
    type VARCHAR(64) NOT NULL,
    value BIGINT NOT NULL,
    PRIMARY KEY (type)
);

CREATE TABLE minted(
    type VARCHAR(64) NOT NULL,
    value VARCHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL,
    PRIMARY KEY (type, value),
    FOREIGN KEY (type) REFERENCES seed (type)
);

GRANT INSERT, SELECT, UPDATE ON seed TO easy_pid_generator;
GRANT INSERT, SELECT ON minted TO easy_pid_generator;
