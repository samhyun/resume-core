CREATE TABLE IF NOT EXISTS app_user_role (
                                             user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role    varchar(64) NOT NULL,
    PRIMARY KEY (user_id, role)
    );

-- role 조회 최적화
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'ix_app_user_role_role'
    ) THEN
CREATE INDEX ix_app_user_role_role ON app_user_role(role);
END IF;
END$$;
