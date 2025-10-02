# Resume Core — Architecture & Package Guide

단일 Spring Boot(WebFlux) 프로젝트에서 **Port / Adapter / UseCase**가 또렷하게 보이도록 구성된 **라이트 CQRS** 템플릿이다.
쓰기(write)는 Postgres(R2DBC) + Outbox, 읽기(read)는 Mongo/Redis 프로젝션으로 분리한다.

---

## Package Structure

```
com.resume.core
├─ App.kt
│
├─ adapter/                                  # Adapters (기술 어댑터)
│  ├─ inbound/
│  │  └─ web/                                # Inbound Adapter: HTTP Controllers (WebFlux)
│  │     ├─ ChatController.kt                # SSE/WebSocket(선택) 스트리밍
│  │     ├─ ResumeCommandController.kt       # CUD 엔드포인트
│  │     └─ ResumeQueryController.kt         # 조회 엔드포인트
│  │
│  └─ outbound/
│     ├─ persistence/
│     │  ├─ command/                         # Write side: Postgres(R2DBC) + Outbox
│     │  │  ├─ entity/{ResumeEntity.kt, OutboxEntity.kt}
│     │  │  ├─ repository/{ResumeCommandAdapter.kt, OutboxAdapter.kt}
│     │  │  └─ config/R2dbcConfig.kt
│     │  └─ query/                            # Read side: Mongo/Redis
│     │     ├─ mongo/
│     │     │  ├─ doc/ResumeViewDoc.kt
│     │     │  ├─ ResumeQueryMongoAdapter.kt
│     │     │  └─ config/ReactiveMongoConfig.kt
│     │     └─ redis/
│     │        ├─ ResumeCacheRedisAdapter.kt
│     │        └─ config/ReactiveRedisConfig.kt
│     ├─ messaging/
│     │  ├─ projection/ResumeProjector.kt     # Outbox → Mongo/Redis 업서트
│     │  └─ polling/OutboxPollingScheduler.kt # Outbox 폴링 소비자
│     └─ client/
│        ├─ WebClientConfig.kt                # ServerBearerExchangeFilterFunction (토큰 릴레이)
│        └─ AiWebClient.kt                    # FastAPI 등 외부 호출
│
├─ application/                               # UseCase 레이어(비즈니스 오케스트레이션)
│  ├─ dto/
│  │  ├─ write/UpdateResumeCommand.kt
│  │  └─ read/{ResumeViewDto.kt, ResumeSummaryDto.kt}
│  └─ usecase/
│     ├─ write/
│     │  ├─ UpdateResumeUseCase.kt
│     │  └─ UpdateResumeService.kt
│     └─ read/
│        ├─ GetResumeUseCase.kt
│        ├─ SearchResumesUseCase.kt
│        └─ ReadServices.kt
│
├─ domain/                                    # 프레임워크 독립(엔티티/정책)
│  ├─ model/{Resume.kt, Section.kt}
│  └─ policy/ResumePolicy.kt
│
├─ port/                                      # Ports (경계 인터페이스)
│  └─ outbound/
│     ├─ command/{ResumeCommandPort.kt, OutboxPort.kt}
│     ├─ query/ResumeQueryPort.kt
│     └─ external/AiChatPort.kt
│
├─ infra/                                     # 인프라(보안/공통)
│  └─ security/ReactiveSecurityConfig.kt      # WebFlux Resource Server(JWT)
│
├─ config/                                    # 횡단 설정
│  ├─ CorsConfig.kt
│  ├─ ObjectMapperConfig.kt
│  └─ ObservabilityConfig.kt
│
└─ shared/                                    # 공용 타입/유틸
   ├─ events/ResumeUpdated.kt
   ├─ error/Exceptions.kt
   ├─ constants/{Topics.kt, SecurityScopes.kt}
   └─ util/{Json.kt, ReactorUtils.kt}
```

---

## Layer Responsibilities

* **adapter.inbound.web**
  HTTP 요청을 **UseCase**로 전달하는 입력 어댑터. 바인딩/검증만 담당하고 비즈니스 로직은 없음.

* **application.usecase**
  유스케이스 구현. **트랜잭션 경계**, 도메인 정책 호출, **Outbox 기록** 등 오케스트레이션 담당.

* **port.outbound**
  저장소/메시징/외부 API 같은 인프라 의존성에 대한 **요구사항(인터페이스)**.
  → 구현은 adapter.outbound가 맡음.

* **adapter.outbound.persistence**
  R2DBC(Postgres) / Mongo / Redis 구현체. CQRS에 맞춰 **command / query**로 분리.

* **adapter.outbound.messaging**
  Outbox 폴링 소비자와 **프로젝터**(읽기 모델 업서트). 멱등/업서트가 핵심.

* **adapter.outbound.client**
  외부 서비스(FastAPI 등) 호출. WebClient에서 **Bearer 토큰 릴레이** 필터 사용.

* **domain**
  순수 모델·정책. 프레임워크 무관.

* **infra.security**
  WebFlux + **JWT 리소스 서버**. 무상태(stateless) 운영, CSRF 비활성화(쿠키 인증 미사용).

---

## CQRS & Data Flow

```
Controller (inbound)
  → UseCase(Write) ─┐       (tx)
      └─ Postgres(R2DBC)    ── Update + Outbox Append ──┐ Commit
                                                          ▼
                                            OutboxPollingScheduler
                                                          ▼
                                      ResumeProjector (멱등 업서트)
                                                          ▼
                                    Mongo(조회 모델) + Redis(캐시/랭킹)

UseCase(Read)
  → ResumeQueryPort → Mongo/Redis 조회
```

* **쓰기(Command)**: Postgres에서 **도메인 변경 + Outbox 삽입**을 **한 트랜잭션**으로.
* **프로젝션**: Outbox를 폴링/CDC로 소비하여 Mongo/Redis에 **업서트**(멱등) 적용.
* **조회(Query)**: Mongo(주력) + Redis(캐시/랭킹 보조).

---

## Security Model

* **JWT 리소스 서버(WebFlux)**: `Authorization: Bearer <token>`
* **무상태(stateless)**: `NoOpServerSecurityContextRepository`
* **CSRF 비활성화**: 쿠키 기반 인증을 사용하지 않기 때문에 REST API에서는 실효성이 낮음.
* **CORS**: 프런트 도메인 화이트리스트로 엄격 관리.
* **메서드 보안**: 필요 시 `@EnableReactiveMethodSecurity` + `@PreAuthorize`.

---

## Why `persistence/command` vs `persistence/query`

* **command** = 트랜잭션·정합성 중심(쓰기, Outbox, 인덱스: `processed, occurred_at`)
* **query** = 검색/페이징/캐시 중심(읽기, 복합인덱스/텍스트 인덱스)
* 폴더만 봐도 **CQRS 경계**가 눈에 들어오고, 테스트/운영이 명확해진다.

---

## API Endpoints (예시)

* `PUT /api/resumes/{id}` — 이력서 수정 (Write)
* `GET /api/resumes/{id}` — 단건 조회 (Read)
* `GET /api/resumes?q=keyword&userId=...&page=0&size=20` — 검색/목록 (Read)
* `POST /api/chat/stream` (SSE) — 스트리밍 응답

> 인증: `/api/**`는 기본 `authenticated`, 필요 시 롤/스코프 부여.

---

## Outbox Payload (예시)

`ResumeUpdated` 최소 필드:

```json
{
  "aggregateId": "UUID",
  "title": "string",
  "summary": "string",
  "occurredAt": "2025-10-01T12:34:56Z",
  "userId": "UUID"    
}
```

* **멱등 키**: Outbox PK + `aggregateId`/`version`(선택)
* **프로젝션 규칙**: Mongo `save(upsert)` / Redis `SET` or `ZADD XX` 등

---

## Decisions & Conventions

* **CSRF**: 비활성화(무상태 Bearer)
* **상태 변경은 비멱등 메서드만**: `POST/PUT/PATCH/DELETE`
* **DTO 분리**: `application/dto/write` vs `application/dto/read`
* **유스케이스 인터페이스**: `*UseCase`는 포트-인 역할(테스트/교체 용이)
* **로그/메트릭**: Outbox lag, 실패 재시도 카운트, 프로젝션 처리율

---

## Getting Started

1. **DB 준비**

    * Postgres, MongoDB, Redis 로컬 실행 (또는 Docker Compose)
    * Flyway를 쓰면 `db/migration/V1__init.sql`로 스키마 초기화

2. **환경 설정**

    * `src/main/resources/application.yml`에서 R2DBC/Mongo/Redis/JWT 설정

3. **빌드 & 실행**

   ```bash
   ./gradlew bootRun
   ```

4. **간단 호출**

    * 수정: `PUT /api/resumes/{id}` (title/content/summary)
    * 조회: `GET /api/resumes/{id}` / 검색: `GET /api/resumes?q=...`
    * 스트림: `POST /api/chat/stream` (SSE)

---

## Testing (권장 플랜)

* **단위**: UseCase(Service) — 포트를 Mock으로
* **통합**: R2DBC/Mongo/Redis는 Testcontainers로
* **컨트롤러**: `WebTestClient` + JWT 테스트 토큰

---

## Roadmap (선택)

* Outbox 폴링 → **CDC(Kafka/Redpanda)**로 전환 가능
* Mongo 인덱스 고도화(텍스트/부분검색), Redis 랭킹/카운터 추가
* Admin UI 필요 시 별도 앱으로 분리(MVC + 세션/OIDC)

---

### 유지보수 팁

* **폴더가 곧 경계**: Inbound → UseCase → Outbound Port → Outbound Adapter
* **CQRS 변경 용이**: 읽기/쓰기 저장소 교체나 확장은 어댑터 레벨에서 처리
* **YAGNI**: 초기에 단순하게, 필요해지면 핸들러/파이프라인 확장

  curl -X POST http://localhost:8080/api/chat-sessions \
  -H 'Content-Type: application/json' \
  -d '{
  "appName": "resume-agent",
  "userId": "eb2d955e-3713-4efa-9cbb-9121ffe370af",
  "sessionId": "session-001",
  "purpose": "INTERVIEW_PREP"
  }'