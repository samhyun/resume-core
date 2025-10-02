DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'chat_session'
          AND column_name = 'external_session_id'
    ) THEN
        EXECUTE 'ALTER TABLE chat_session RENAME COLUMN external_session_id TO agent_session_id';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.constraint_column_usage
        WHERE table_name = 'chat_session'
          AND column_name = 'external_session_id'
          AND constraint_name = 'chat_session_external_session_id_key'
    ) THEN
        EXECUTE 'ALTER TABLE chat_session RENAME CONSTRAINT chat_session_external_session_id_key TO chat_session_agent_session_id_key';
    END IF;
END $$;
