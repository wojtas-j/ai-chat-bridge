CREATE TABLE IF NOT EXISTS discord_message (
    id BIGSERIAL PRIMARY KEY,
    content VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    discord_nickname VARCHAR(255) NOT NULL
);
