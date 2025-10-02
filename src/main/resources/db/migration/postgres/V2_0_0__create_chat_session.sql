-- src/main/resources/db/migration/V2__chat_session.sql
CREATE TABLE IF NOT EXISTS chat_session (
                                            id                  uuid PRIMARY KEY,                         -- 우리 로컬 세션 ID
                                            user_id             text NOT NULL,                            -- 클라이언트 전달 userId
                                            external_session_id text NOT NULL UNIQUE,                     -- 외부 에이전트 세션 ID (예: session_1759279838)
                                            app_name            text NOT NULL,                            -- 예: resume_adk_agent
                                            state               jsonb NOT NULL,                           -- 외부 응답 전체 state (원본 저장)
                                            purpose             text,                                     -- denorm: state.state.purpose
                                            status              text NOT NULL,                            -- ACTIVE | CLOSED
                                            last_update_time    double precision,                         -- 외부 응답 lastUpdateTime
                                            created_at          timestamptz NOT NULL DEFAULT now(),
    ended_at            timestamptz
    );

-- “한 사용자는 ACTIVE 세션을 1개만” 보장하는 부분 인덱스
CREATE UNIQUE INDEX IF NOT EXISTS ux_chat_session_active_user
    ON chat_session(user_id)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS ix_chat_session_user_created
    ON chat_session(user_id, created_at DESC);
