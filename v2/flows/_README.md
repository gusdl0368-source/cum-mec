# 플로우 YAML

이 폴더의 `*.yaml` 은 `scripts\push_flows.ps1` 로 LDPlayer 의
`/sdcard/Android/data/com.v26macro.v2/files/v26-macro/flows/` 에 푸시됨.

매크로는 그 폴더에서 읽어서 실행. **APK 재빌드 없이 흐름 수정 가능.**

## 파일 (Phase 5 에서 채워짐)

- `sponsor.yaml` — 후원금 정산
- `pointshop.yaml` — 포인트상점 (3개 상품)
- `homerunrace.yaml` — 홈런레이스 (라운드 N회)
- `specialmatch.yaml` — 스페셜매치 잠재력 60오버롤 (M회)
- `rankingchallenge.yaml` — 랭킹챌린지 (refreshLevel + leaveLastSet)
- `leaguemode.yaml` — 리그모드 (자율 무한 반복)

## DSL 사양

`v2/PROJECT_PLAN.md` 의 'DSL 사양' 섹션 참조.

## 예시

`sponsor.example.yaml` 참고 (Phase 3 검증용 초안).
