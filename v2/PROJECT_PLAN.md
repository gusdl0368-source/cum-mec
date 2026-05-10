# V26 매크로 V2 — 마스터 플랜

> 이 문서가 단일 출처(single source of truth). 새 세션 시작할 때 이거 먼저 읽고 작업.

## 목표

컴투스 프로야구 V26 의 일일 일과 6개를 자동화하는 안드로이드 APK.
**개발은 로컬 LDPlayer 9 (Windows PC + ADB), 운영은 LD 클라우드 (24/7 자율 실행)**.
같은 APK 한 개로 둘 다 커버.

## 환경 (확정)

- **에뮬레이터**: LDPlayer 9 (로컬 개발) / LD 클라우드 (운영)
- **해상도**: 1280×720 고정 — 모든 좌표 절대값으로 저장 (정규화 X)
- **로컬 ADB**: `127.0.0.1:5555` (포트 충돌 시 5554/5556/5557)
- **게임 언어**: 한국어
- **외부 노출**: LD 클라우드는 외부 포트 노출 X — APK 내장 UI + 디스코드 웹훅 (outbound) 만

## 자동화할 일과 (순서대로)

1. **후원금 정산**
2. **포인트상점** — 데일리 럭키박스 무료 / 50볼 EVENT 최대구매 / 50볼 단계별 5번
3. **홈런레이스** — 사용자 라운드 0~8 설정
4. **스페셜매치** — 잠재력 60오버롤, 사용자 횟수 0~5
5. **랭킹챌린지** — refreshLevel(무료/포인트/스타) + leaveLastSet 옵션
6. **리그모드** — 라이브 5/5 → 시뮬 전환, 코치조언 → 교체, 일일 한도까지 무한

## 아키텍처 결정 (토론 X)

### A. 핵심 스택
- **언어**: Kotlin 2.0+
- **빌드**: AGP 8.7+, minSdk 26, targetSdk 34, compileSdk 35
- **UI**: Jetpack Compose (BOM 2024.10+) + Navigation
- **비동기**: kotlinx.coroutines + Flow
- **저장**: DataStore (Preferences)
- **스케줄**: WorkManager
- **HTTP outbound**: OkHttp 5

### B. 비전/입력
- **화면 캡처**: `MediaProjection` API (사용자 1회 동의, 5fps 풀)
- **입력**: `AccessibilityService.dispatchGesture()` + `GLOBAL_ACTION_BACK`
- **OCR (메인)**: MLKit `text-recognition-korean` 16.0+
- **템플릿 매칭 (보조)**: OpenCV 4.10 — 텍스트 없는 아이콘 5~7개만
- **비전 LLM 폴백**: Anthropic Claude API (Haiku 4.5) — OCR + 템플릿 둘 다 막힌 화면에서 좌표 받음

### C. 플로우 정의
- **YAML on /sdcard**: `/sdcard/v26-macro/flows/*.yaml` — APK 빌드 없이 수정/푸시 가능
- **DSL**: 데이터 only (코드 인젝션 X), 복잡 로직은 named macro 로 코틀린에 가둠
- **Hot-reload**: 플로우 파일 watcher → 다음 실행에 자동 반영
- **Templates on /sdcard**: `/sdcard/v26-macro/templates/*.png` — 동적 추가/교체

### D. 사용자 인터페이스 (Compose 4 화면)
- **메인**: 태스크 토글 / 일일 스케줄 시각 / 디스코드 URL / 시작·정지
- **라이브**: 마지막 캡처 + OCR 박스 오버레이 + 실시간 로그
- **편집기**: YAML 6개 파일 수정 (저장하면 즉시 반영)
- **실패 갤러리**: 자동 저장된 스크린샷 + 그 시점 OCR 결과 + 막힌 YAML 단계

### E. 운영
- **WorkManager 일일 스케줄**: 사용자가 시각 설정 (기본 06:00 — 서버 리셋 직후)
- **디스코드 웹훅**: 일일 실행 결과 + 실패 스크린샷 자동 푸시
- **트레이스 레코더**: 사용자가 손으로 한 번 플레이 → 스크린/OCR/탭 기록 → YAML 초안 자동 생성

## DSL 사양 (YAML 플로우)

### 단계 (Step) 타입

```yaml
- tap_text: "후원금"                       # 텍스트 찾아서 탭 (가장 흔함)
- tap_text:                                # dict 형태 — 옵션
    text: "확인"
    region: { x: 0, y: 400, w: 1280, h: 200 }   # 화면 영역 제한
    occurrence: 1                          # n번째 매칭 (1-based)
    threshold: 0.85                        # confidence 임계
- tap_when_visible: "정산"                 # wait_text + tap_text 합체
- tap_when_visible_if_present: "확인"      # 있으면 탭, 없으면 패스 (옵셔널 다이얼로그)
- tap_xy: [640, 360]                       # 절대 좌표 탭
- tap_xy:
    coords: [1180, 80]
    guard_text: "포인트 상점"               # 가드 텍스트 보일 때만 안전
- tap_template: "mic_button"               # PNG 템플릿 매칭 후 탭
- wait_text: "정산"                        # 텍스트 보일 때까지 (timeoutMs 옵션)
- wait_text_gone: "로딩 중"                # 텍스트 사라질 때까지
- back: 1                                  # GLOBAL_ACTION_BACK n회
- sleep: 0.7                               # 초 (지연만)
- loop:
    until: text_visible("WIN") | text_visible("LOSE")
    max: 60
    do:
      - tap_xy: [160, 90]
      - sleep: 0.2
- branch:
    if: text_visible("RESUME PLAY")
    then:
      - tap_text: "RESUME PLAY"
    else:
      - tap_text: "PLAY BALL"
- macro: back_to_main                      # 코틀린 정의된 named macro
- llm_fallback:                            # 모르는 화면 → Claude API 에 좌표 요청
    intent: "결과 화면의 '다음' 버튼"
```

### 조건식 (Condition)
- `text_visible("X")` — OCR 에서 X 매칭됨 (fuzzy, 공백 무시)
- `text_not_visible("X")`
- `text_count("X") >= N`
- `template_matched("name")`
- `screen_changed_since(seconds)` — 직전 캡처와 비교

### Named Macros (코틀린)
- `back_to_main` — 메인 화면 ('플레이볼' 텍스트) 까지 BACK 반복 (최대 10회) + exit 다이얼로그 자동 취소
- `dismiss_popups` — 알려진 팝업 자동 닫기
- `wait_loading_done` — '로딩' 텍스트 사라질 때까지

## /sdcard 폴더 레이아웃

```
/sdcard/v26-macro/
├── flows/
│   ├── sponsor.yaml
│   ├── pointshop.yaml
│   ├── homerunrace.yaml
│   ├── specialmatch.yaml
│   ├── rankingchallenge.yaml
│   └── leaguemode.yaml
├── templates/
│   ├── mic_button.png
│   ├── pause_button.png
│   ├── outs_area.png
│   └── ...
├── traces/                # 트레이스 레코더 출력
│   └── sponsor-2026-05-10/
│       ├── 0001.png
│       ├── 0001.ocr.json
│       ├── tap-1.json
│       └── ...
├── runs/                  # 매크로 실행 로그
│   └── 2026-05-10-06-00-00/
│       ├── log.txt
│       └── failure-step-3.png
├── config.yaml            # 사용자 설정 (Discord URL, 스케줄 시각 등)
└── secrets.yaml           # API 키 (있으면 비전 LLM 사용)
```

## 학습된 게임 함정 (미리 처리)

| 함정 | 처리 |
|------|------|
| 같은 모양 '확인' 버튼 여러 화면 | `wait_text` 로 화면 식별자 (헤더) 가드 |
| 'PLAY BALL' ↔ 'RESUME PLAY' 같은 라벨 변경 | OCR 분기 (`branch`) |
| 팀 색깔로 색 변하는 버튼 | 텍스트/좌표만, PNG 금지 |
| 라이브 5/5 체크 | OCR 로 '5/5' 텍스트 직접 |
| 연속경기 어두워져도 동작 | 헤더 텍스트로 화면 식별 |
| 사용자 중간 개입 → RESUME PLAY 화면 | 진입부 항상 체크 |
| 5세트마다 갱신 다이얼로그 (포인트/스타) | `tap_when_visible_if_present` |
| 같은 텍스트가 여러 위치 | `region` + `occurrence` 옵션 |
| OCR 띄어쓰기/자간 | fuzzy match, 공백 정규화 |
| 캡처 풀링 GC 압박 | 비트맵 풀, OCR 트리거 기반 |

## 빌드 단계

### Phase 1 — 스켈레톤 ✓ (완료, 미리 셋업됨)
- Gradle 프로젝트 (settings/build/libs.versions.toml/wrapper)
- Manifest + 권한 4종 (알림/오버레이/접근성/MediaProjection) Compose UI
- 빈 AccessibilityService + 빈 Application + MainActivity
- check_adb.ps1 / install_apk.ps1 / push_flows.ps1
- 빈 4개 화면 stub (메인/라이브/편집기/실패)

### Phase 2 — 캡처 + 입력 + 첫 OCR
1. `ScreenCaptureService` (MediaProjection → ImageReader → Flow<Bitmap>)
2. `GestureService` 의 tap/back/swipe 채우기
3. `OcrEngine` (MLKit 한국어, fuzzy `findText`)
4. 라이브 화면에 캡처 표시 + OCR 박스 오버레이
5. **검증**: V26 메인 켜고 라이브 화면 보면 '플레이볼' '후원금' 박스 보임

### Phase 3 — YAML 엔진 + 첫 태스크
1. `Step` sealed class + YAML 파서 (snakeyaml-engine)
2. `FlowRunner` — 단계별 실행, 매 단계 로그/스크린샷
3. Named macros: `back_to_main`, `dismiss_popups`
4. `sponsor.yaml` 작성 → `/sdcard` 푸시 → 실행
5. **검증**: 메인에서 시작 → 후원금 정산 자동 완료

### Phase 4 — 운영 도구
1. 실패 갤러리 (자동 저장된 PNG + OCR 텍스트 표시)
2. YAML 편집기 (Compose 안에서 수정/저장)
3. 트레이스 레코더 (REC 버튼 → 화면+OCR+탭 기록)
4. 비전 LLM 폴백 (Claude Haiku API)
5. 디스코드 웹훅 (outbound 결과 알림)
6. WorkManager 일일 스케줄

### Phase 5 — 나머지 5개 태스크
- 사용자가 트레이스 6개 녹화 → 그것 기반으로 YAML 작성
- 각 태스크 1.5~2시간 예상 (트레이스 활용 시)

## 작업 방식

- **너 (Claude) 가 ADB 로 직접 LDPlayer 붙어서** 화면 캡처 / 탭 / 설치 / 디버깅 다 해
- **사용자 개입은 최소화**: 첫 트레이스 6개 녹화 + 단계별 최종 OK 만
- **YAML 만 바꿀 땐 APK 재설치 X** — `adb push` 만
- **Kotlin 변경 시**: `gradlew assembleDebug` → `adb install -r` (1~2분 사이클)
- 막히는 화면은 Claude 가 `adb exec-out screencap` 으로 직접 캡처 → Read 도구로 보고 분석

## 핵심 명령어

```powershell
# ADB 검증 (한 번)
.\scripts\check_adb.ps1

# APK 빌드 + 설치
.\gradlew assembleDebug
.\scripts\install_apk.ps1

# YAML 만 빠르게 푸시
.\scripts\push_flows.ps1

# 로그 보기
adb logcat -s V26Macro:V

# 화면 한 장 (디버깅)
adb exec-out screencap -p > screen.png
```

## 다음 세션 시작 가이드

1. `cd v2`
2. `PROJECT_PLAN.md` (이 파일) 정독
3. 현재 phase 가 어디까지 됐는지 `git log` 로 확인
4. **Phase 2 부터 시작** — `ScreenCaptureService` 구현
5. 한 phase 끝날 때마다 사용자에게 "이거 LDPlayer 에서 동작 확인해봐" 요청
