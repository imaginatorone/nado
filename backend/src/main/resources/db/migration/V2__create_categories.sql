CREATE TABLE categories (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    parent_id   BIGINT          REFERENCES categories(id) ON DELETE CASCADE
);

CREATE INDEX idx_categories_parent ON categories(parent_id);
