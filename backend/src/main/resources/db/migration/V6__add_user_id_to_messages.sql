ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_messages_user_id'
              AND table_name = 'messages'
        ) THEN
            ALTER TABLE messages
                ADD CONSTRAINT fk_messages_user_id
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
        END IF;
    END$$;

CREATE INDEX IF NOT EXISTS idx_messages_user_id ON messages(user_id);
