# 템플릿 이미지 / 좌표

매크로는 두 가지 방식으로 화면을 처리합니다:

1. **PNG 템플릿 매칭** — 이 폴더의 `<버킷>/<이름>.png` 들을 OpenCV 로 매칭해 화면 위치 인식
2. **좌표 직접 탭** — 한 단계 위 `coords.json` 에 정규화 좌표가 정의돼 있으면 그 위치를 바로 탭. **PNG 보다 우선** 적용됩니다.

`tools/template_capture/start.bat` 도구를 사용하면 두 방식 모두 자동으로 올바른 경로에 저장됩니다.

## 폴더 구조

```
templates/
├── launcher/                        # LDPlayer 홈 (게임 자동 실행용)
│   └── v26_icon.png                 # LDPlayer 홈 화면의 V26(컴프야V26) 아이콘
│
├── home/                            # 공용 (메인/팝업/종료 다이얼로그)
│   ├── playball.png                 # 메인의 '플레이볼' 메뉴 버튼 - 메인 식별 겸 진입구
│   ├── exit_cancel.png              # '게임 종료?' 다이얼로그의 취소 버튼 (메인에서 실수 BACK 시 자동 취소)
│   └── popup_close.png              # 일반 팝업의 X (선택)
│   # 뒤로가기는 템플릿 없이 시스템 BACK 키로 처리합니다 (팀 색깔 무관).
│
├── sponsor/                         # 후원금 정산
│   ├── entry.png                    # 메인의 후원금 아이콘
│   └── claim.png                    # 정산/수령 버튼
│
├── pointshop/                       # 포인트상점 (3개 상품)
│   ├── entry.png                    # 메인의 '상점' 버튼
│   ├── entry_popup_dontshow.png     # (선택) 진입 팝업의 '오늘은 그만 보기' - 한 번 체크하면 그날 안 뜸
│   ├── entry_popup_x.png            # 진입 광고 팝업의 큰 X 닫기 버튼
│   ├── item_tab.png                 # 상점 안의 '아이템' 탭
│   ├── pointshop_tab.png            # 아이템 안의 '포인트상점' 서브탭
│   ├── card_lucky_box.png           # 목록의 '데일리 럭키 박스' 카드 (왼쪽)
│   ├── free_button.png              # 럭키박스 상세의 '무료' 보라 버튼
│   ├── card_event_50.png            # 목록의 '50볼 EVENT' 카드 (가운데, EVENT 뱃지)
│   ├── max_quantity.png             # 50볼 EVENT 상세의 '최대 개수 설정'
│   ├── buy_button.png               # 50볼 EVENT 상세의 '구매' 보라 버튼
│   ├── card_tier_50.png             # 목록의 '50볼' 단계별 카드 (오른쪽)
│   ├── tier_buy.png                 # 50볼 단계별 상세의 'P xxx' 가격 버튼 (5번 반복)
│   ├── purchase_confirm.png         # (선택) 구매 확인 다이얼로그의 확인 버튼
│   └── exit.png                     # (선택) 상점 종료 X 버튼 - 없으면 BACK 키
│
├── homerunrace/                     # 홈런레이스
│   ├── entry.png                    # 플레이볼 안의 '홈런레이스' 탭
│   ├── play.png                     # 플레이/입장 버튼
│   ├── top_left_target.png          # 좌상단 '최고스코어' 글씨 (스윙용 탭 위치)
│   ├── retry.png                    # 결과창의 '재도전' - 결과창임을 구별하는 핵심 식별자
│   └── confirm.png                  # '확인' - 타구 경로 스킵과 결과창 종료 양쪽에서 같은 모양으로 사용
│   # 두 화면의 '확인' 은 시각적으로 동일하므로 한 장만 캡처. 매크로가 retry 유무로 어느 화면인지 판단함
│
├── specialmatch/                    # 스페셜매치 - 잠재력 60오버롤
│   ├── entry.png                    # 플레이볼 안의 '스페셜매치' 탭
│   ├── jamjeryeok_tab.png           # 스페셜매치 안의 '잠재력' 탭
│   ├── carousel_left.png            # 카드 카루셀 좌측 '←' 화살표 (60 OVR 보일 때까지 누름)
│   ├── match_60ovr.png              # 60 OVR 카드 식별 — '오버롤 60' 글자 영역 권장
│   ├── match_start1.png             # 60 매치 화면 하단의 'START' 보라 버튼 (1차)
│   ├── select_type_header.png       # SELECT TYPE 화면 식별 — '/ SELECT TYPE /' 글자
│   ├── random_pick_card.png         # 랜덤픽 플레이 카드 (선택/비선택 모두 매칭되도록 가운데 사진만)
│   ├── direct_play_on.png           # 직접 플레이 토글이 ON 상태 (선택, 보이면 OFF)
│   ├── direct_play_off.png          # 직접 플레이 토글이 OFF 상태 — start2 의 가드로 사용
│   ├── gauge_left.png               # 게이지 좌측 끝 — 좌표로 저장 + 가드=select_type_header 추천
│   ├── start2.png                   # SELECT TYPE 화면 하단의 'START' — 좌표+가드=direct_play_off 추천
│   ├── result_next.png              # 결과창의 '다음' 버튼
│   ├── play_again.png               # '한 번 더 하기' (선택, 계속 돌릴 때)
│   └── confirm.png                  # 결과창의 '확인' 버튼 (종료)
│
├── rankingchallenge/                # (예정 — 이후 흐름 받은 뒤 재정의)
└── leaguemode/                      # (예정)
```

## 캡처 가이드

1. 에뮬레이터에서 V26을 실행하고 각 화면을 띄웁니다.
2. **추천 — 캡처 도구 사용**: `tools/template_capture/start.bat`. 좌측 트리에서 항목 선택 → 화면 드래그 → 저장이면 자동으로 올바른 경로에 들어갑니다.
3. 또는 수동: 에뮬레이터의 스크린샷 기능으로 화면 전체를 PNG로 저장 → 식별/클릭 영역만 60×60 ~ 200×80 픽셀 정도로 작게 크롭 → 정확한 파일명으로 저장.

## 매칭 임계값

`TemplateMatcher.findBest`의 기본 임계값은 0.85입니다. 매칭이 잘 되지 않으면:
- 템플릿을 더 단순한 영역으로 다시 크롭 (텍스트만 / 아이콘만)
- 가격·숫자처럼 매번 바뀌는 부분은 피하기 (예: `buy_tier`는 가격 라벨 빼고 아이콘만 잘라야 5단계 모두 매칭됨)
- 에뮬레이터 해상도와 캡처 시 해상도를 일치시키기
