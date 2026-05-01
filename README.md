# V26 매크로

컴투스 프로야구 V26의 일과(후원금 정산 → 포인트상점 → 홈런레이스 → 스페셜매치 → 랭킹챌린지 → 리그모드)를
안드로이드 에뮬레이터 안에서 자동으로 돌리는 매크로 APK.

> ⚠️ 게임 매크로는 컴투스 이용약관에 위배될 수 있습니다. 개인 학습/연구 용도로만 사용하세요.

## 동작 방식

- **MediaProjection**으로 에뮬레이터 화면을 실시간 캡처
- **OpenCV 템플릿 매칭**으로 현재 어떤 화면인지 인식
- **AccessibilityService**의 `dispatchGesture()`로 탭/스와이프를 주입
- **루팅 불필요**, 권한만 부여하면 동작

## 빌드 환경

- Android Studio Hedgehog 이상 또는 CLI Gradle 8.10+
- JDK 17
- Android SDK 34, minSdk 24

## 빌드

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

에뮬레이터에 설치:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 사용 순서

1. APK를 에뮬레이터에 설치하고 실행
2. 메인 화면에서 권한 4종을 순서대로 허용
   1. 알림 권한
   2. 다른 앱 위에 그리기 (오버레이)
   3. 접근성 서비스 → "V26 매크로" 켜기
   4. "화면 캡처 시작 + 오버레이 띄우기" → MediaProjection 동의
3. V26으로 화면 전환 → 떠있는 작은 컨트롤 패널에서 일과 체크박스 선택 → "시작"
4. 매크로가 끝나거나 정지하려면 패널의 "정지" / "닫기"

## 템플릿 이미지 준비 (필수)

V26의 화면을 식별하려면 PNG 템플릿이 필요합니다. **이 부분은 사용자가 직접 캡처해야 합니다.**

가이드: [`app/src/main/assets/templates/README.md`](app/src/main/assets/templates/README.md)

요약:
1. 에뮬레이터에서 V26을 띄움
2. 각 화면(후원금/포인트상점/...)에서 식별 버튼을 작은 PNG로 크롭
3. `app/src/main/assets/templates/<일과>/<이름>.png` 로 저장
4. 다시 빌드 → 설치

## 프로젝트 구조

```
app/src/main/kotlin/com/v26macro/
├── MainActivity.kt              # 권한 안내 + 시작 버튼
├── V26MacroApp.kt               # OpenCV 초기화 + 알림 채널
├── overlay/
│   ├── OverlayService.kt        # 플로팅 컨트롤 패널 (포그라운드 서비스)
│   ├── OverlayPanel.kt          # Compose UI
│   └── Settings.kt              # DataStore (선택된 일과 영속화)
├── capture/
│   ├── ScreenCaptureService.kt  # MediaProjection → ImageReader
│   └── FrameProvider.kt         # 최신 프레임 게시 (StateFlow)
├── input/
│   └── GestureService.kt        # AccessibilityService - tap/swipe
├── vision/
│   ├── TemplateMatcher.kt       # OpenCV matchTemplate (다중 스케일)
│   ├── TemplateLibrary.kt       # 템플릿 PNG 캐시
│   └── OcrReader.kt             # MLKit 한글 OCR (잔여 횟수용)
├── runner/
│   ├── MacroRunner.kt           # 일과 순차 실행기
│   ├── TaskState.kt             # TaskKind / TaskResult / RunnerState
│   ├── TaskContext.kt           # find/tap/wait 헬퍼
│   └── tasks/
│       ├── Task.kt
│       ├── Common.kt            # dismissPopupsAndReturnToLobby 등
│       ├── SponsorPayoutTask.kt
│       ├── PointShopTask.kt
│       ├── HomeRunRaceTask.kt
│       ├── SpecialMatchTask.kt
│       ├── RankingChallengeTask.kt
│       └── LeagueModeTask.kt
└── util/
    ├── Logger.kt                # 파일 + Logcat
    └── Wait.kt                  # waitFor / humanDelay
```

## 로그 위치

기기 내: `Android/data/com.v26macro/files/logs/run-<timestamp>.txt`

```bash
adb shell run-as com.v26macro ls files/logs/
adb pull /sdcard/Android/data/com.v26macro/files/logs/ ./logs
```

## 알려진 제약

- 게임 패치로 UI가 바뀌면 템플릿을 다시 캡처해야 합니다.
- 에뮬레이터 해상도가 캡처 시점과 너무 다르면(±25% 이상) 매칭이 실패할 수 있습니다.
- 매크로 실행 중 V26이 백그라운드로 가면 캡처가 멈춥니다.
