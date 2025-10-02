-- keycloak_db에 접속
\connect keycloak_db

-- public 스키마 접근/생성 권한 (idempotent)
DO $$
BEGIN
  -- public 스키마는 기본 존재. 사용/생성 권한 부여
EXECUTE 'GRANT USAGE, CREATE ON SCHEMA public TO kc_user';

-- 앞으로 생성될 객체 기본 권한 (선택)
EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO kc_user';
EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO kc_user';
END$$;

-- DB 접속/임시테이블 권한 (중복 허용)
GRANT CONNECT ON DATABASE keycloak_db TO kc_user;
GRANT TEMP    ON DATABASE keycloak_db TO kc_user;

-- 필요 시 public의 광역 권한 축소(선택)
REVOKE ALL ON SCHEMA public FROM PUBLIC;
