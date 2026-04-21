CREATE TABLE images (
    id          BIGSERIAL       PRIMARY KEY,
    ad_id       BIGINT          NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    file_path   VARCHAR(500)    NOT NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_images_ad ON images(ad_id);
