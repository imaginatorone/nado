-- счётчик просмотров объявлений
ALTER TABLE ads ADD COLUMN IF NOT EXISTS view_count BIGINT NOT NULL DEFAULT 0;

-- оценки продавцов от покупателей
CREATE TABLE ratings (
    id          BIGSERIAL       PRIMARY KEY,
    reviewer_id BIGINT          NOT NULL REFERENCES users(id),
    seller_id   BIGINT          NOT NULL REFERENCES users(id),
    score       INT             NOT NULL CHECK (score >= 1 AND score <= 5),
    review      TEXT,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE(reviewer_id, seller_id)
);

CREATE INDEX idx_ratings_seller ON ratings(seller_id);

-- избранные объявления
CREATE TABLE favorites (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    ad_id       BIGINT          NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, ad_id)
);

CREATE INDEX idx_favorites_user ON favorites(user_id);
CREATE INDEX idx_favorites_ad ON favorites(ad_id);
