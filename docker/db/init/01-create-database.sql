-- DB 생성
CREATE DATABASE keycloak_db;
CREATE DATABASE app_db;

-- 전용 유저 생성 (이미 있으면 패스)
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'kc_user') THEN
      CREATE USER kc_user WITH ENCRYPTED PASSWORD 'kc_pass';
END IF;
   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_user') THEN
      CREATE USER app_user WITH ENCRYPTED PASSWORD 'app_pass';
END IF;
END$$;
