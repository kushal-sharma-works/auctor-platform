CREATE TABLE definitions (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

INSERT INTO definitions (id, name, description)
VALUES ('123', 'sample-definition', 'stored in database');
