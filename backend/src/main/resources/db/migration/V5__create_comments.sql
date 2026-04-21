CREATE TABLE comments (
    id          BIGSERIAL       PRIMARY KEY,
    content     TEXT            NOT NULL,
    ad_id       BIGINT          NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_ad ON comments(ad_id);
