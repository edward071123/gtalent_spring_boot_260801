CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    name VARCHAR(30) NOT NULL,
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '0:其他, 1:男, 2:女',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:存在, 0:刪除',
    deleted_at DATETIME NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_account (account)
);
