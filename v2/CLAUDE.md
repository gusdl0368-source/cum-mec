# V26 매크로 V2 — Claude 작업 지침

너는 이 프로젝트를 이어받아서 작업하는 Claude 야. 시작 전에 필독:

## 1. 컨텍스트 흡수

```
1. PROJECT_PLAN.md 정독 — 단일 출처
2. README.md 빠르게 훑기
3. git log --oneline -20 로 최근 진행 확인
```

## 2. 현재 단계 파악

`PROJECT_PLAN.md` 의 "빌드 단계" 섹션에서 어디까지 ✓ 됐는지 확인.
**이 스켈레톤 셋업 시점에는 Phase 1 까지 완료**. Phase 2 부터 시작.

## 3. 작업 방식 (강제)

- **사용자 PC 의 LDPlayer 에 ADB 로 직접 붙어서** 화면 캡처/탭/설치 직접 해
- 사용자한테 "스크린샷 보내줘" 하지 마. 너가 `adb exec-out screencap -p > /tmp/screen.png` 해서 Read 도구로 봐
- 코드 한 줄 고치고 즉시 빌드/설치/테스트
- 한 phase 끝날 때마다 사용자에게 동작 확인 요청 (그 외엔 자율 진행)
- 사용자 개입 필요한 것: 첫 트레이스 6개 녹화 + 단계별 OK 만

## 4. 첫 액션 체크리스트

```bash
# 1. ADB 연결 확인
adb connect 127.0.0.1:5555
adb devices  # device 한 개 떠야 함

# 2. 사용자가 V26 안 켰으면 켜달라고 요청
adb exec-out screencap -p > /tmp/v26.png
# Read 도구로 /tmp/v26.png 확인 — V26 메인 화면이어야 함

# 3. Phase 2 시작 — ScreenCaptureService 부터
```

## 5. 주의

- **APK 패키지명**: `com.v26macro.v2`
- **Kotlin 패키지**: `com.v26macro.v2.*`
- **/sdcard 루트**: `/sdcard/v26-macro/` (대시 포함)
- **빌드 명령**: `cd v2 && ./gradlew assembleDebug`
- **설치 명령**: `adb install -r v2/app/build/outputs/apk/debug/app-debug.apk`

## 6. 코드 스타일

- 한국어 주석 OK (도메인 용어가 한국어이므로)
- 함수/변수명은 영문
- Compose 상태는 ViewModel 에 두기, 화면은 stateless
- 코루틴 스코프는 Service 라이프사이클에 묶기

## 7. 막혔을 때

- 화면 인식 안 됨 → `adb exec-out screencap` 해서 직접 보고 OCR 결과 분석
- 탭 안 먹음 → AccessibilityService 활성화됐는지 확인
- 빌드 실패 → 의존성 버전 충돌. `libs.versions.toml` 확인
- LDPlayer ADB offline → `taskkill /F /IM Ld9BoxHeadless.exe` 후 재시작 (사용자에게 안내)

## 8. 절대 하지 말 것

- 사용자한테 캡처 도구 돌리라고 시키기 (너가 ADB 로 직접 해)
- GitHub 에 PNG 업로드해달라고 시키기 (없어졌음)
- 게임 룰 자세히 물어보기 (트레이스 한 번이면 됨)
- v1 (상위 폴더) 코드 수정 (참조만 OK, 새 코드는 v2/ 안에서만)
