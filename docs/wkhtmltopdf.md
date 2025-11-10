# wkhtmltopdf 설치 & 관리 가이드

Resume Core는 HTML 템플릿을 PDF로 변환할 때 [wkhtmltopdf](https://wkhtmltopdf.org) 바이너리에 의존합니다. 로컬/CI 환경 어디에서나 동일한 결과를 얻으려면 아래 절차에 따라 설치하고, `resume.pdf` 설정으로 경로를 관리하세요.

## 1. 설치 방법

### macOS (Homebrew)
```bash
brew install wkhtmltopdf
```

### Ubuntu / Debian
```bash
sudo apt-get update
sudo apt-get install -y wkhtmltopdf
```

### 기타 Linux / 수동 설치
1. [공식 릴리스 페이지](https://wkhtmltopdf.org/downloads.html)에서 OS에 맞는 패키지를 다운로드합니다.
2. 압축을 해제하고 `wkhtmltopdf` 바이너리를 `/usr/local/bin` 등 PATH에 포함된 위치로 복사합니다.
3. 실행 권한을 부여합니다: `chmod +x /usr/local/bin/wkhtmltopdf`

## 2. 설치 확인
```bash
wkhtmltopdf --version
which wkhtmltopdf
```
위 명령이 버전과 경로를 출력하면 설치가 완료된 것입니다.

## 3. Resume Core와 연동
| 설정 키 | 기본값 | 설명 |
| --- | --- | --- |
| `resume.pdf.wkhtmltopdf-path` | `wkhtmltopdf` | PATH에 없는 경우 바이너리 절대경로를 지정하세요. 환경 변수 `WKHTMLTOPDF_PATH`로도 오버라이드할 수 있습니다. |
| `resume.pdf.timeout-seconds` | `30` | 변환이 지연될 때 wkhtmltopdf 프로세스를 중단하는 타임아웃(초). `WKHTMLTOPDF_TIMEOUT_SECONDS` 로 조정 가능. |

예시 (macOS에서 custom path):
```bash
WKHTMLTOPDF_PATH=/opt/homebrew/bin/wkhtmltopdf \
WKHTMLTOPDF_TIMEOUT_SECONDS=45 \
./gradlew bootRun
```

### Docker Compose + wrapper 스크립트
- `docker/docker-compose.yml`은 저장소 내 `docker/wkhtmltopdf/Dockerfile`을 빌드해 wkhtmltopdf CLI가 포함된 컨테이너를 실행합니다. 로컬 바이너리를 설치하지 않고 다음과 같이 사용할 수 있습니다.
  ```bash
  docker compose -f docker/docker-compose.yml up -d wkhtmltopdf
  WKHTMLTOPDF_PATH=./scripts/wkhtmltopdf.sh ./gradlew bootRun
  ```
- `scripts/wkhtmltopdf.sh`는 HTML/PDF 파일을 컨테이너로 복사해 변환하고, 결과를 다시 호스트로 가져오는 래퍼입니다. 필요하다면 `COMPOSE_FILE`, `WKHTMLTOPDF_SERVICE`, `WKHTMLTOPDF_CONTAINER_DIR` 환경 변수로 경로와 서비스명을 조정할 수 있습니다.

## 4. 자주 발생하는 오류
| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `Failed to execute wkhtmltopdf` 예외 | 바이너리가 PATH에 없거나 실행 권한 부족 | 위 설치 절차를 다시 수행하고, `which wkhtmltopdf`로 경로 확인 |
| `timed out` 메시지 | 변환에 30초 이상 소요 | `resume.pdf.timeout-seconds` 값 증가 또는 템플릿/리소스를 경량화 |
| 한글이 깨짐 | OS에 한글 폰트 미설치 | 시스템 폰트에 Noto Sans CJK 등 한글 폰트를 설치하거나 템플릿에 웹폰트 포함 |

## 5. CI/컨테이너 환경
- Docker 이미지에 wkhtmltopdf를 추가하려면 `apt-get install -y wkhtmltopdf` 명령을 Dockerfile에 포함하세요.
- Headless 환경에서도 동작하지만, 글꼴이 필요하므로 원하는 폰트를 함께 설치하는 것을 권장합니다.

루트 `Dockerfile`은 외부에서 빌드된 JAR(`build/libs/*.jar`)을 복사해 실행하며, 컨테이너 내부에 wkhtmltopdf와 폰트를 설치합니다.

```bash
docker build -t resume-core .
docker run --rm -p 8081:8081 resume-core
```

필요 시 `docs/wkhtmltopdf.md`에 설치 메모를 추가해 팀원의 환경을 공유하세요.
