# 템플릿 이미지

각 일과(Task)는 화면을 식별하기 위해 PNG 템플릿을 사용합니다. 에뮬레이터에서 V26 화면을 직접 캡처해 아래 위치에 넣어주세요.

## 폴더 구조

```
templates/
├── launcher/              # LDPlayer 홈 화면 (매크로가 V26 자동 실행할 때 사용)
│   └── v26_icon.png       # LDPlayer 홈 화면의 V26(컴프야V26) 아이콘
├── home/                  # 로비/팝업 공용 (모든 Task에서 사용)
│   ├── lobby_button.png   # 로비로 돌아가는 홈 버튼
│   ├── lobby_indicator.png# 로비 화면임을 식별하는 고유 요소(예: 좌하단 메뉴바)
│   ├── popup_close.png    # 일반 팝업의 X / 닫기 버튼
│   ├── popup_confirm.png  # 일반 팝업의 확인 버튼
│   └── popup_later.png    # "다음에" / "취소" 버튼
├── sponsor/               # 후원금 정산
│   ├── entry.png
│   ├── claim.png
│   ├── confirm.png        (선택)
│   └── empty.png          (선택)
├── pointshop/             # 포인트상점
│   ├── entry.png
│   ├── buy_slot1.png      # 사고 싶은 상품의 "구매" 버튼들 (최대 10개)
│   ├── buy_slot2.png
│   └── confirm.png
├── homerunrace/           # 홈런레이스
│   ├── entry.png
│   ├── start.png
│   ├── auto.png
│   ├── result.png
│   ├── claim.png
│   ├── no_more.png        (선택)
│   └── exit.png
├── specialmatch/          # 스페셜매치
│   ├── entry.png, start.png, auto.png, skip.png(선택),
│   ├── result.png, claim.png, no_more.png(선택), exit.png
├── rankingchallenge/      # 랭킹챌린지
│   ├── entry.png, challenge.png, opponent_pick.png(선택),
│   ├── opponent_slot.png, auto.png, skip.png(선택),
│   ├── result.png, claim.png, no_more.png(선택), exit.png
└── leaguemode/            # 리그모드
    ├── entry.png, play.png, lineup_ok.png(선택),
    ├── auto.png, skip.png(선택), result.png, claim.png,
    ├── no_more.png(선택), exit.png
```

## 캡처 가이드

1. 에뮬레이터에서 V26을 실행하고 각 화면을 띄웁니다.
2. 에뮬레이터의 스크린샷 기능으로 화면 전체를 PNG로 저장합니다.
3. 식별/클릭이 필요한 버튼 영역을 60×60 ~ 200×80 픽셀 정도로 작게 크롭합니다.
4. 배경의 일부를 함께 포함해서 다른 영역과 헷갈리지 않게 하되, 애니메이션이 있는 영역(반짝임, 카운트 숫자)은 피합니다.
5. 같은 폴더에 정확한 파일명으로 저장합니다.

## 매칭 임계값

`TemplateMatcher.findBest`의 기본 임계값은 0.85입니다. 매칭이 잘 되지 않으면:

- 템플릿을 더 단순한 영역으로 다시 크롭 (텍스트만 / 아이콘만)
- 다중 스케일을 더 넓게 잡음 (Task에서 직접 호출 가능)
- 에뮬레이터 해상도와 캡처 시 해상도를 일치시킴
