-- (선택) UUID 생성 함수용 확장. 운영 DB 정책에 맞춰 pgcrypto 또는 uuid-ossp 중 택1
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS app_user (
                                        id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),  -- pgcrypto 사용 시
-- id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),         -- uuid-ossp 사용 시 대안

    username       varchar(100) NOT NULL,
    email          varchar(255) NOT NULL,
    password_hash  varchar(255) NOT NULL,      -- bcrypt/argon2 등 해시 저장
    enabled        boolean      NOT NULL DEFAULT true,
    email_verified boolean      NOT NULL DEFAULT false,
    display_name   varchar(100),

    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
    );

-- 케이스 인센서티브(선택): lower(username/email)로 유니크 보장
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'ux_app_user_username_lower'
    ) THEN
CREATE UNIQUE INDEX ux_app_user_username_lower ON app_user (LOWER(username));
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'ux_app_user_email_lower'
    ) THEN
CREATE UNIQUE INDEX ux_app_user_email_lower ON app_user (LOWER(email));
END IF;
END$$;

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION trg_set_timestamp()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_app_user_set_timestamp'
    ) THEN
CREATE TRIGGER trg_app_user_set_timestamp
    BEFORE UPDATE ON app_user
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_timestamp();
END IF;
END$$;
