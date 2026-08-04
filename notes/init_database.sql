CREATE DATABASE project26_v1
    DEFAULT CHARACTER SET = 'utf8mb4';

CREATE DATABASE project26_v1_test
    DEFAULT CHARACTER SET = 'utf8mb4';

CREATE USER 'project26_v1'@'%' IDENTIFIED BY '{your password}';
-- Grant select privilege to all databases;
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
    ON project26_v1.*
    TO 'project26_v1'@'%';

CREATE USER 'project26_v1_test'@'%' IDENTIFIED BY '123123';
-- Grant select privilege to all databases;
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
    ON project26_v1_test.*
    TO 'project26_v1_test'@'%';