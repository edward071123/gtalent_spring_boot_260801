CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    gender TINYINT NOT NULL,
    account VARCHAR(64) NOT NULL,
    email VARCHAR(128) NULL,
    password VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_account (account)
);