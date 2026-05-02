"""V26 매크로가 사용하는 모든 템플릿 PNG의 목록.

각 항목: (bucket, name, required, description)
required=True 이면 해당 Task가 동작하기 위해 반드시 필요하고,
required=False 면 있으면 좋지만 없어도 Task가 우회 가능.
"""

TEMPLATES = [
    # ── LDPlayer 홈 (게임 자동 실행) ──
    ("launcher", "v26_icon", True, "LDPlayer 홈 화면의 V26(컴프야V26) 아이콘"),

    # ── 공용: V26 메인/팝업/탐색 ──
    # 뒤로가기는 시스템 BACK 키(접근성 GLOBAL_ACTION_BACK)로 처리하므로 별도 템플릿 불필요.
    # 단, 메인에서 BACK 을 한 번 더 누르면 "게임 종료?" 다이얼로그가 뜨므로 exit_cancel 필요.
    ("home", "playball",     True,  "메인 화면의 '플레이볼' 메뉴 버튼 (홈런/스페셜/랭킹/리그 진입구) — 메인임을 식별하는 용도 겸용"),
    ("home", "exit_cancel",  True,  "'게임을 종료하시겠습니까?' 다이얼로그의 취소/아니오 버튼 (메인에서 실수로 BACK 눌렸을 때 자동 취소)"),
    ("home", "popup_close",  False, "일반 팝업의 X 닫기 버튼"),

    # ── 후원금 정산 ──
    ("sponsor", "entry",   True, "메인 화면의 후원금 아이콘"),
    ("sponsor", "claim",   True, "정산/수령 버튼"),
    ("sponsor", "confirm", True, "보상 수령 후 뜨는 확인 버튼 (한 번 또는 여러 번 뜰 수 있음)"),

    # ── 포인트상점 ──
    # 흐름: 메인의 '상점' → 진입 팝업 닫기 → '아이템' 탭 → '포인트상점' 탭
    # 3개 상품 각각 다른 처리:
    #   1) 데일리 럭키 박스 (무료, 0/1) → 카드 탭 → '무료' 버튼
    #   2) 50볼 EVENT (10000P, 0/3) → 카드 탭 → '최대 개수 설정' → '구매'
    #   3) 50볼 단계별 (10000→30000→50000→70000→100000P) → 카드 탭 → 가격 버튼 5번
    ("pointshop", "entry",         True,  "메인 화면의 '상점' 버튼"),
    ("pointshop", "entry_popup_dontshow", False, "상점 첫 진입 광고 팝업의 '오늘은 그만 보기' 체크박스 (한 번 체크하면 다음 실행부터 팝업 안 뜸 — 시간 절약)"),
    ("pointshop", "entry_popup_x",        True,  "상점 첫 진입 광고 팝업의 큰 X 닫기 버튼 (팝업 아래쪽 가운데). 좌표 + 가드=자기 자신 추천"),
    ("pointshop", "item_tab",      True,  "상점 안의 '아이템' 카테고리 탭"),
    ("pointshop", "pointshop_tab", True,  "아이템 카테고리 안의 '포인트상점' 서브탭"),

    ("pointshop", "card_lucky_box", True, "포인트상점 목록의 '데일리 럭키 박스' 카드 (왼쪽 상품)"),
    ("pointshop", "free_button",    True, "데일리 럭키 박스 상세 페이지 하단의 '무료' 보라 버튼"),

    ("pointshop", "card_event_50",  True, "포인트상점 목록의 '50볼 EVENT' 카드 (가운데, EVENT 뱃지 포함)"),
    ("pointshop", "max_quantity",   True, "50볼 EVENT 상세 페이지의 '최대 개수 설정' 버튼"),
    ("pointshop", "buy_button",     True, "50볼 EVENT 상세 페이지 우측의 '구매' 보라 버튼"),

    ("pointshop", "card_tier_50",   True, "포인트상점 목록의 '50볼' 단계별 카드 (오른쪽, EVENT 뱃지 없음)"),
    ("pointshop", "tier_buy",       True, "50볼 단계별 상세 하단의 'P xxx,xxx' 가격 보라 버튼 — 5번 반복 탭. 가격 숫자가 변하므로 P 동전 아이콘 + 보라 배경만 작게 크롭 권장"),

    ("pointshop", "purchase_confirm", False, "구매 확인 다이얼로그의 확인 버튼 (없으면 패스)"),
    ("pointshop", "exit",             False, "상점 종료 X 버튼 (없으면 BACK 키로 처리)"),

    # ── 홈런레이스 ──
    # 흐름: 플레이볼 → 홈런레이스 탭 → (시즌 첫날엔 NEW SEASON 안내 한 번 탭)
    #       → (선수 미등록 시 출전선수 '+' → 정렬 드롭다운 → '풀스윙' → 좌상단 선수 → 확인)
    #       → '플레이' → 결과창 까지 좌상단 '최고스코어' 계속 탭
    #       → (공 친 경우) 타구 경로 화면 '확인' 자동 스킵 → 결과창 → 재도전/확인 → BACK
    #
    # 타구 경로 화면의 '확인' 과 결과창의 '확인' 은 똑같이 생겼다. 매크로는 retry(재도전)
    # 의 존재 유무로 두 화면을 구별한다 (재도전 보이면 결과창, 안 보이면 타구 경로).
    ("homerunrace", "entry",           True,  "플레이볼 안의 '홈런레이스' 탭"),
    ("homerunrace", "season_intro",    False, "새 시즌 시작 안내 화면 — 'NEW SEASON' / '새로운 시즌의 시작' 글자. 시즌 초기화 직후만 뜸. 보이면 한 번 탭해 닫음"),
    ("homerunrace", "register_plus",   False, "출전선수 자리의 큰 '+' 버튼 — 선수 미등록 상태에서만 보임 (선수 등록 흐름 트리거)"),
    ("homerunrace", "sort_menu",       False, "선수 등록 화면의 정렬 드롭다운 (기본 표시 '파워+정확')"),
    ("homerunrace", "sort_fullswing",  False, "정렬 드롭다운에서 '풀스윙' 옵션"),
    ("homerunrace", "first_player",    False, "정렬 후 좌상단 첫 번째 선수 카드 — 위치 고정이라 좌표 저장 + 가드=sort_menu 추천"),
    ("homerunrace", "register_confirm",False, "선수 등록 화면 하단의 '확인' 버튼 (보라색 큰 버튼)"),
    ("homerunrace", "play",            True,  "홈런레이스 플레이/입장 버튼 (PLAY HOMERUN RACE)"),
    ("homerunrace", "top_left_target", True,  "스윙용으로 계속 탭할 좌상단의 '최고스코어' 글씨"),
    ("homerunrace", "retry",           True,  "결과창의 '재도전' 버튼 — 결과창 식별용 (타구 경로 화면엔 없음)"),
    ("homerunrace", "confirm",         True,  "확인 버튼 — 타구 경로(스킵)와 결과창(종료) 양쪽에서 같은 모양으로 사용. 한 장만 캡처"),

    # ── 스페셜매치 (잠재력 60오버롤) ──
    # 흐름: 플레이볼 → 스페셜매치 → 잠재력 → 카루셀에서 60 OVR 카드 보일 때까지 ← 화살표
    #       → 60 매치 START(1차) → SELECT TYPE 화면
    #       → 랜덤픽 플레이 카드 → 직접플레이 OFF 확인 → 게이지 좌측 끝 → START(최종)
    #       → 결과 다음 → 한 번 더 / 확인
    ("specialmatch", "entry",          True,  "플레이볼 안의 '스페셜매치' 탭"),
    ("specialmatch", "jamjeryeok_tab", True,  "스페셜매치 안의 '잠재력' 탭"),
    ("specialmatch", "carousel_left",  True,  "잠재력 카드 카루셀 좌측의 '←' 화살표 — 60 OVR 카드 보일 때까지 누름"),
    ("specialmatch", "match_60ovr",    True,  "잠재력 60 오버롤 카드 식별 — '오버롤 60' 글자 영역 권장"),
    ("specialmatch", "match_start1",   True,  "60 매치 화면 하단의 'START' 보라 버튼 (1차)"),
    ("specialmatch", "select_type_header", True, "SELECT TYPE 화면 식별 — 상단의 '/ SELECT TYPE /' 글자 권장"),
    ("specialmatch", "random_pick_card",   True, "SELECT TYPE 화면의 '랜덤픽 플레이' 카드 (탭하면 선택). 선택/비선택 상태에서 모두 매칭되도록 카드 가운데 사진 위주로 크롭"),
    ("specialmatch", "direct_play_on", False, "'직접 플레이' 토글이 ON 상태에서만 매칭 — 보이면 탭해서 OFF (없어도 동작은 함)"),
    ("specialmatch", "direct_play_off", True, "'직접 플레이' 토글이 OFF 상태에서만 매칭 — start2 의 가드로 사용해서 직접플레이 켜져있으면 START 안 누르도록"),
    ("specialmatch", "gauge_left",     True,  "게이지 슬라이더 좌측 끝 — 여기를 탭하거나 드래그해서 최소값으로 (좌표 저장 + 가드=select_type_header 추천)"),
    ("specialmatch", "start2",         True,  "SELECT TYPE 화면 하단의 'START' 보라 버튼 (최종)"),
    ("specialmatch", "result_next",    True,  "결과창의 '다음' 버튼"),
    ("specialmatch", "play_again",     False, "결과창의 '한 번 더 하기' 버튼 (계속 돌릴 때)"),
    ("specialmatch", "confirm",        True,  "결과창의 '확인' 버튼 (종료)"),

    # ── 랭킹챌린지 ──
    # 흐름: 플레이볼 → 리그모드 → 랭킹챌린지 → '연속 경기' → '경기 진행' 다이얼로그
    #       → 5경기 자동 진행 (매 결과창 next 즉시 탭으로 5초 카운트 스킵)
    #       → '총 5게임 진행 결과' 화면 → '확인' → 메인 복귀
    #       → 갱신 버튼 탭 (무료 3 → 포인트 6 → 스타 6 순서로 자동 라벨 변경)
    #       → 갱신 완료되면 다시 5경기. '금일 갱신 완료' 보이면 전체 종료.
    ("rankingchallenge", "entry",            True, "리그모드 화면의 '랭킹 챌린지' 카드 (좌상단, 트로피 + BRONZE)"),
    ("rankingchallenge", "play_ball",        True, "상대 카드 우측의 'PLAY BALL' 흰색 버튼 — 매치 가능 신호 (5명 다 두면 5번 다 보임). 안 보이면 매크로가 갱신/종료 판단"),
    ("rankingchallenge", "continuous_play",  True, "랭킹챌린지 메인 하단의 '연속 경기' 보라 버튼 (5경기 자동 진행 트리거)"),
    ("rankingchallenge", "proceed",          True, "'알림' 다이얼로그의 '경기 진행' 파란 버튼"),
    ("rankingchallenge", "result_indicator", True, "한 경기 결과 화면 식별 — 'LOSE' / 'WIN' 영역 또는 '경기 결과' 헤더 권장"),
    ("rankingchallenge", "next",             True, "결과 화면의 '다음 경기 시작 (n)' 또는 '다음 (n)' 버튼 — 위치 고정이라 좌표+가드=result_indicator 추천"),
    ("rankingchallenge", "summary_done",     True, "5경기 모두 끝난 뒤 뜨는 '총 5게임 진행 결과' 화면 식별 — 헤더 텍스트 권장"),
    ("rankingchallenge", "summary_confirm",  True, "'총 5게임 진행 결과' 화면 하단의 '확인' 파란 버튼"),
    ("rankingchallenge", "refresh_button",   True, "메인 우상단의 갱신 버튼 — 무료/포인트/스타 모두 같은 위치 (글자만 변함). 좌표+가드=continuous_play 추천"),
    ("rankingchallenge", "refresh_paid_confirm", False, "포인트/스타 갱신 시 한 번 더 뜨는 확인 다이얼로그의 버튼 (무료 갱신은 안 뜸)"),
    ("rankingchallenge", "refresh_done",     False, "갱신 버튼이 '금일 갱신 완료' 상태일 때만 매칭 — 보이면 매크로 종료"),
    ("rankingchallenge", "incomplete_confirm", False, "(드물게) '경기를 완료하지 않은 상대가 있습니다' 팝업의 '확인' — 보통 안 뜸"),

    # ── 리그모드 / 추후 정의 ──
    ("leaguemode",       "entry", False, "(추후) 플레이볼 안의 '리그모드' 탭"),
]

BUCKET_LABELS = {
    "launcher":          "LDPlayer 홈 (게임 실행)",
    "home":              "공용 (메인 화면/팝업)",
    "sponsor":           "후원금 정산",
    "pointshop":         "포인트상점",
    "homerunrace":       "홈런레이스",
    "specialmatch":      "스페셜매치",
    "rankingchallenge":  "랭킹챌린지 (예정)",
    "leaguemode":        "리그모드 (예정)",
}

BUCKET_ORDER = ["launcher", "home", "sponsor", "pointshop", "homerunrace",
                "specialmatch", "rankingchallenge", "leaguemode"]
