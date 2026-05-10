# 1회 셋업 (사용자)

## 사전 준비

- LDPlayer 9 설치, 해상도 **1280×720** 으로 설정
- LDPlayer 안에 V26 설치 + 한국어 계정 로그인
- LDPlayer 설정 → '기타' → **ADB 디버깅 켜기**
- (선택) JDK 17 + Android Studio (코드 편집할 거면)

## ADB 검증

PowerShell 에서:

```powershell
cd v2
.\scripts\check_adb.ps1
```

스크립트가 자동으로:
- 시스템 adb 와 LDPlayer 내장 adb 둘 다 시도
- 포트 5555/5554/5556/5557 자동 탐색
- offline 이면 LdBoxHeadless 충돌 워크어라운드 안내
- 성공 시 `config.local.json` 에 정상 엔드포인트 기록

성공하면:
```
[OK] ADB connected: 127.0.0.1:5555
[OK] device 1080×1920 emulator-5554
[OK] saved config.local.json
```

## APK 빌드 + 설치

```powershell
.\gradlew assembleDebug
.\scripts\install_apk.ps1
```

(빌드 첫 회 의존성 다운로드 5~10분)

## 권한 부여 (앱 안에서 안내)

앱 실행 시 차례로:
1. **알림 권한** — Allow
2. **다른 앱 위에 표시** (시스템 설정으로 이동) — V26 매크로 ON
3. **접근성** (시스템 설정으로 이동) — V26 매크로 → 권한 요청 다이얼로그 → '허용'
4. **화면 캡처** — START NOW

## 디스코드 웹훅 (선택)

운영 결과 알림 받을 디스코드 채널에서:
- 채널 설정 → 통합 → 웹후크 → 새 웹후크 → URL 복사
- 앱 메인 화면 → '디스코드 URL' 에 붙여넣기

## 비전 LLM 폴백 (선택)

OCR + 템플릿 둘 다 막히는 화면에서 Claude Haiku 가 좌표 추정.
- https://console.anthropic.com 에서 API 키 발급
- 앱 메인 화면 → 'Anthropic API Key' 에 붙여넣기
- 회당 비용 ~₩30, 평소엔 안 쓰고 모르는 화면 만났을 때만 호출

## LD 클라우드로 옮기기

로컬 검증 끝나면:
- `app\build\outputs\apk\debug\app-debug.apk` 를 LD 클라우드 인스턴스에 업로드/설치
- 같은 권한 4종 부여 (LD 클라우드 원격 뷰로 조작)
- 메인 화면에서 일일 스케줄 시각 + 태스크 토글 + 디스코드 URL 입력
- '자동 실행 켜기' → 닫으면 24/7 자율 운영
