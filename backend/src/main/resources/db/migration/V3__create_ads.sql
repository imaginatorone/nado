CREATE TABLE ads (
    id          BIGSERIAL       PRIMARY KEY,
    title       VARCHAR(255)    NOT NULL,
    description TEXT            NOT NULL,
    price       DECIMAL(15, 2),
    category_id BIGINT          NOT NULL REFERENCES categories(id),
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ads_user       ON ads(user_id);
CREATE INDEX idx_ads_category   ON ads(category_id);
CREATE INDEX idx_ads_status     ON ads(status);
CREATE INDEX idx_ads_created    ON ads(created_at DESC);
CREATE INDEX idx_ads_title_gin  ON ads USING gin(to_tsvector('russian', title));
