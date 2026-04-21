-- полнотекстовый поиск: generated tsvector column + GIN index
ALTER TABLE ads ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('russian', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('russian', coalesce(description, '')), 'B')
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_ads_search_vector ON ads USING gin(search_vector);
