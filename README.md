# Resume Core

리액티브 Spring Boot(WebFlux) 기반으로 AI 에이전트와 연동되는 채팅 세션을 중개하는 프로젝트입니다. UseCase → Port → Adapter로 이어지는 클린 아키텍처 경계를 유지하면서 REST Docs + OpenAPI DSL, Testcontainers, MockWebServer 등을 포함한 개발 흐름을 예시로 제시합니다.

## 아키텍처 요약

```
src/main/kotlin/com/resume/core
├─ application/                      # 유스케이스 및 DTO
├─ port/                             # 인바운드/아웃바운드 포트 정의
├─ adapter/                          # inbound / outbound 어댑터 구현
├─ config/                           # 횡단 관심사 (보안, R2DBC 등)
├─ domain/                           # 도메인 모델/정책 (현재 비어 있음)
└─ shared/                           # DSL 및 공용 유틸
```

### 동작 흐름
1. 컨트롤러가 요청을 받아 유스케이스 호출.
2. 유스케이스는 `AiAgentPort`를 통해 외부 AI 에이전트와 통신하고, 세션 정보를 R2DBC(Postgres)에 저장.
3. REST Docs 테스트는 Kotlin DSL을 이용해 필드/헤더/파라미터를 선언적으로 문서화하고 OpenAPI 스펙을 자동 생성.

## 로컬 개발 절차

### 준비물
- JDK 21, Docker, Docker Compose
- (선택) Swagger UI용 Docker 이미지

### 의존 서비스 실행
```bash
docker compose -f docker/docker-compose.yml up -d
```
Postgres 16과 Keycloak 25가 기동됩니다. 종료 시 볼륨을 초기화하려면 `down -v`를 사용하세요.

### 애플리케이션 실행
```bash
./gradlew bootRun
```

### 종료
```bash
docker compose -f docker/docker-compose.yml down -v
```

## 빌드 및 테스트 명령어
| 명령어 | 설명 |
| --- | --- |
| `./gradlew build` | 전체 컴파일 + 테스트 + JAR 생성 |
| `./gradlew test` | 단위/슬라이스 테스트, REST Docs 스니펫 생성 |
| `./gradlew openapi3` | 스니펫을 통합해 OpenAPI( `build/api-spec/resume-core.yaml` ) 생성 |
| `./gradlew bootRun` | WebFlux 애플리케이션 구동 |

## API 문서화 워크플로
1. `ChatControllerDocs` 등 테스트에서 DSL(`fields { ... }`, `headers { ... }`)을 사용해 문서 갱신.
2. `./gradlew test openapi3` 실행으로 스니펫 + OpenAPI YAML 재생성.
3. Swagger UI로 미리보기:
   ```bash
   docker run -p 8888:8080 \
     -e SWAGGER_JSON=/api/swagger.yaml \
     -v "$(pwd)/build/api-spec/resume-core.yaml:/api/swagger.yaml" \
     swaggerapi/swagger-ui
   ```
   `http://localhost:8888` 접속 후 API 명세 확인.

## 코딩 & 문서 컨벤션
- Kotlin 공식 스타일(4 스페이스, 멀티라인에는 trailing comma, 120자 가이드라인).
- 패키지: 소문자 / 클래스·enum: UpperCamelCase / 함수·프로퍼티: lowerCamelCase / 상수: SCREAMING_SNAKE_CASE.
- 의존성 역전: 유스케이스 → 포트 → 어댑터 순으로 참조, 어댑터는 포트/도메인으로 역참조 금지.
- REST Docs DSL은 `"필드명" type STRING optional true means "설명"` 형태로 사용.

## 테스트 지침
- 주요 도구: JUnit 5, MockK, Spring REST Docs API Spec, Testcontainers(Postgres), MockWebServer.
- 테스트 파일명은 `*Tests.kt`, 실제 패키지 구조를 그대로 따릅니다.
- 푸시 전 `./gradlew test`, API 변경 시 `./gradlew openapi3`로 스펙 갱신.
- 빠른 피드백을 위해 슬라이스 테스트(WebFlux/Mongo/Postgres)를 활용하고, 전체 컨텍스트가 필요한 경우에만 `@SpringBootTest`를 사용합니다.

## 보안 & 환경 설정
- `SecurityConfig`는 Keycloak JWT 리소스 서버를 구성합니다. Bearer 토큰이 없으면 `/api/**` 접근이 제한됩니다.
- 실제 비밀 값은 환경 변수 또는 `application-local.yml`(Git 제외)로 관리하세요.
- Docker 기본 계정(Postgres: `postgres/postgres`, Keycloak: `admin/admin`)은 개발 환경 전용입니다.

## 데이터베이스 & 마이그레이션
- Flyway 스크립트: `src/main/resources/db/migration/postgres`.
- 스키마 변경 시 반드시 마이그레이션 파일을 동반하세요 (`V2_0_1__rename_agent_session_column.sql` 참고).
- `ChatSessionEntity`는 JSONB 상태 및 `Persistable` 구현을 통해 클라이언트가 ID를 제공해도 INSERT가 수행되도록 구성했습니다.

## Contribution Guidelines
- Conventional Commits(`feat:`, `fix:`, `docs:` 등) 사용.
- PR에는 의도, 테스트 명령어, 관련 이슈 링크, UI/API 변화 스크린샷/스펙을 포함해 주세요.
- OpenAPI와 문서 테스트를 최신 상태로 유지하세요. CI는 최소 `./gradlew test`를 검증합니다.
