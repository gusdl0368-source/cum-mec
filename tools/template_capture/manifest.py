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
    ("sponsor", "entry", True, "메인 화면의 후원금 아이콘"),
    ("sponsor", "claim", True, "정산/수령 버튼"),

    # ── 포인트상점 ──
    # 흐름: 메인의 '상점' → 진입 팝업 닫기 → '아이템' 탭 → '포인트상점' 탭
    #       → 7개 상품 구매 (일반 2종 + 단계별 5회) → X로 종료
    ("pointshop", "entry",         True,  "메인 화면의 '상점' 버튼"),
    ("pointshop", "item_tab",      True,  "상점 안의 '아이템' 카테고리 탭"),
    ("pointshop", "pointshop_tab", True,  "아이템 카테고리 안의 '포인트상점' 서브탭"),
    ("pointshop", "buy_item1",     False, "구매할 일반 상품 1번의 구매 버튼 (없으면 스킵)"),
    ("pointshop", "buy_item2",     False, "구매할 일반 상품 2번의 구매 버튼 (없으면 스킵)"),
    ("pointshop", "buy_tier",      True,  "단계별 구매 버튼 — 같은 위치에서 5번 반복 (가격이 10000→30000→…→100000으로 증가). 가격 숫자가 변해도 매칭되도록 아이콘만 작게 크롭 권장"),
    ("pointshop", "max_buy",       True,  "구매 다이얼로그의 '최대수량구매' 버튼"),
    ("pointshop", "buy_confirm",   False, "구매 최종 확정 버튼 (다이얼로그가 한 단계 더 있을 때)"),
    ("pointshop", "exit",          True,  "상점에서 나가는 X 버튼 (보통 좌상단 또는 우상단)"),

    # ── 홈런레이스 ──
    # 흐름: 플레이볼 → 홈런레이스 탭 → '플레이' → 결과창 까지 좌상단 '최고스코어' 계속 탭
    #       → (공 친 경우) 타구경로 화면 '확인' → 결과창 → 재도전/확인 → 뒤로
    ("homerunrace", "entry",            True,  "플레이볼 안의 '홈런레이스' 탭"),
    ("homerunrace", "play",             True,  "홈런레이스 플레이/입장 버튼"),
    ("homerunrace", "top_left_target",  True,  "스윙용으로 계속 탭할 좌상단의 '최고스코어' 글씨"),
    ("homerunrace", "ball_path_confirm",False, "공을 친 경우에만 뜨는 타구 경로 화면의 확인 버튼"),
    ("homerunrace", "retry",            False, "결과창의 '재도전' 버튼 (계속 돌릴 때)"),
    ("homerunrace", "confirm",          True,  "결과창의 '확인' 버튼 (종료할 때)"),

    # ── 스페셜매치 ──
    # 흐름: 플레이볼 → 스페셜매치 → 잠재력 탭 → 60오버롤 매치 → 스타트(1차)
    #       → 랜덤픽플레이 → 게이지 우측 끝까지 드래그 → 직접플레이 OFF 확인
    #       → 스타트(2차) → 결과창 다음 → 한 번 더 / 확인
    ("specialmatch", "entry",          True,  "플레이볼 안의 '스페셜매치' 탭"),
    ("specialmatch", "jamjeryeok_tab", True,  "스페셜매치 안의 '잠재력' 탭"),
    ("specialmatch", "match_60ovr",    True,  "잠재력의 '60오버롤 매치' 입장 버튼"),
    ("specialmatch", "start1",         True,  "라인업 화면 진입을 위한 1차 스타트 버튼"),
    ("specialmatch", "random_pick",    True,  "'랜덤픽플레이' 버튼"),
    ("specialmatch", "gauge_handle",   True,  "게이지 손잡이 — 이 위치에서 우측 끝까지 드래그함"),
    ("specialmatch", "direct_play_on", False, "'직접 플레이' 토글이 켜진 상태에서만 매칭 (보이면 탭해서 끔)"),
    ("specialmatch", "start2",         True,  "최종 스타트 버튼"),
    ("specialmatch", "result_next",    True,  "결과창의 '다음' 버튼"),
    ("specialmatch", "play_again",     False, "결과창의 '한 번 더 하기' 버튼"),
    ("specialmatch", "confirm",        True,  "결과창의 '확인' 버튼 (종료)"),

    # ── 랭킹챌린지 / 리그모드: 사용자 흐름 받은 뒤 추후 정의 ──
    ("rankingchallenge", "entry", False, "(추후) 플레이볼 안의 '랭킹챌린지' 탭"),
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
