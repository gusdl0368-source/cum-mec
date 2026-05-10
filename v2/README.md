# V26 매크로 V2

컴투스 프로야구 V26 일일 일과 자동화 APK.
**개발: 로컬 LDPlayer 9 + ADB / 운영: LD 클라우드 (24/7 자율)**.

## 빠른 시작

```powershell
# 1) ADB 환경 검증
.\scripts\check_adb.ps1

# 2) 빌드 + 설치
.\gradlew assembleDebug
.\scripts\install_apk.ps1

# 3) 앱 실행, 권한 4종 부여 (알림/오버레이/접근성/MediaProjection)

# 4) 플로우 푸시 (있는 경우)
.\scripts\push_flows.ps1

# 5) 앱 메인 화면에서 태스크 토글 → 시작
```

## 단일 출처

`PROJECT_PLAN.md` 가 모든 디자인 결정과 빌드 단계 기록.
새 작업 들어가기 전에 그것 먼저 읽기.

## 폴더 구조

```
v2/
├── PROJECT_PLAN.md       # 마스터 플랜
├── CLAUDE.md             # 다음 세션 Claude 작업 지침
├── README.md             # 이 파일
├── SETUP.md              # 사용자 1회 셋업 가이드
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── app/                  # 안드로이드 모듈
├── scripts/              # PowerShell 헬퍼 (check_adb / install / push_flows)
└── flows/                # /sdcard 로 푸시할 YAML 플로우 템플릿
```

## 진행 상태

- [x] **Phase 1 — 스켈레톤** (Gradle / 권한 UI / 빈 서비스 / 스크립트)
- [ ] Phase 2 — 캡처 + 입력 + 첫 OCR
- [ ] Phase 3 — YAML 엔진 + 후원금 정산
- [ ] Phase 4 — 운영 도구 (실패 갤러리 / 편집기 / 트레이스 / LLM / 디스코드 / 스케줄)
- [ ] Phase 5 — 나머지 5개 태스크
