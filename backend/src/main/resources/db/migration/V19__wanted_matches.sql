-- Phase 4: дедупликация матчей "хочу купить"
CREATE TABLE wanted_matches (
    id          BIGSERIAL PRIMARY KEY,
    request_id  BIGINT NOT NULL REFERENCES want_to_buy(id) ON DELETE CASCADE,
    ad_id       BIGINT NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    score       INT NOT NULL DEFAULT 0,
    seen        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wanted_match UNIQUE (request_id, ad_id)
);

CREATE INDEX idx_wanted_matches_request ON wanted_matches(request_id);
CREATE INDEX idx_wanted_matches_unseen ON wanted_matches(request_id, seen) WHERE seen = FALSE;
