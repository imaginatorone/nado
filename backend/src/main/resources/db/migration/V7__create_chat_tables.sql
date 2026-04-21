-- чат-комнаты: одна комната на связку покупатель-продавец-объявление
CREATE TABLE chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES ads(id),
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    seller_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ad_id, buyer_id)
);

-- сообщения с возможностью прикрепления файла
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES chat_rooms(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT,
    attachment_path VARCHAR(500),
    attachment_type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_messages_room ON chat_messages(room_id, created_at);
CREATE INDEX idx_chat_rooms_buyer ON chat_rooms(buyer_id);
CREATE INDEX idx_chat_rooms_seller ON chat_rooms(seller_id);
