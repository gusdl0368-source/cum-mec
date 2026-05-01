"""V26 매크로가 사용하는 모든 템플릿 PNG의 목록.

각 항목: (bucket, name, required, description)

required=True 이면 해당 Task가 동작하기 위해 반드시 필요하고,
required=False 면 있으면 좋지만 없어도 Task가 우회 가능.
"""

TEMPLATES = [
    # ── 공용: 로비, 팝업 ──
    ("home", "lobby_button",   True,  "로비/홈으로 가는 버튼 (메뉴바의 홈 아이콘)"),
    ("home", "lobby_indicator",True,  "로비 화면임을 식별하는 고유 요소 (예: 좌하단 메뉴바 일부)"),
    ("home", "popup_close",    False, "일반 팝업의 X / 닫기 버튼"),
    ("home", "popup_confirm",  False, "일반 팝업의 확인 버튼"),
    ("home", "popup_later",    False, "팝업의 '다음에' / '취소' 버튼"),

    # ── 후원금 정산 ──
    ("sponsor", "entry",   True,  "메인 화면의 '후원금' 아이콘"),
    ("sponsor", "claim",   True,  "정산/수령 버튼"),
    ("sponsor", "confirm", False, "수령 확인 팝업의 확인 버튼"),
    ("sponsor", "empty",   False, "'정산할 후원금이 없습니다' 안내"),

    # ── 포인트상점 ──
    ("pointshop", "entry",     True,  "메인의 '포인트상점' 아이콘"),
    ("pointshop", "buy_slot1", False, "사고 싶은 1순위 상품의 '구매' 버튼"),
    ("pointshop", "buy_slot2", False, "2순위 상품 구매 버튼"),
    ("pointshop", "buy_slot3", False, "3순위 상품 구매 버튼"),
    ("pointshop", "buy_slot4", False, "4순위 상품 구매 버튼"),
    ("pointshop", "buy_slot5", False, "5순위 상품 구매 버튼"),
    ("pointshop", "confirm",   True,  "수량 선택 후 구매 확정 버튼"),

    # ── 홈런레이스 ──
    ("homerunrace", "entry",   True,  "메인의 '홈런레이스' 아이콘"),
    ("homerunrace", "start",   True,  "입장/시작 버튼"),
    ("homerunrace", "auto",    False, "자동 진행 토글"),
    ("homerunrace", "result",  True,  "결과 화면 식별 요소"),
    ("homerunrace", "claim",   True,  "결과 보상 수령 버튼"),
    ("homerunrace", "no_more", False, "오늘 가능 횟수 초과 안내"),
    ("homerunrace", "exit",    False, "종료/나가기 버튼"),

    # ── 스페셜매치 ──
    ("specialmatch", "entry",   True,  "스페셜매치 메뉴 아이콘"),
    ("specialmatch", "start",   True,  "매치 시작/입장 버튼"),
    ("specialmatch", "auto",    False, "자동 진행"),
    ("specialmatch", "skip",    False, "결과 스킵 버튼"),
    ("specialmatch", "result",  True,  "결과 화면 식별"),
    ("specialmatch", "claim",   True,  "보상 수령"),
    ("specialmatch", "no_more", False, "입장 불가 안내"),
    ("specialmatch", "exit",    False, "종료 버튼"),

    # ── 랭킹챌린지 ──
    ("rankingchallenge", "entry",         True,  "랭킹챌린지 메뉴 아이콘"),
    ("rankingchallenge", "challenge",     True,  "도전 버튼"),
    ("rankingchallenge", "opponent_pick", False, "상대 선택 화면 식별 요소"),
    ("rankingchallenge", "opponent_slot", False, "상대 첫 슬롯"),
    ("rankingchallenge", "auto",          False, "자동 진행"),
    ("rankingchallenge", "skip",          False, "스킵"),
    ("rankingchallenge", "result",        True,  "결과 화면"),
    ("rankingchallenge", "claim",         True,  "보상 수령"),
    ("rankingchallenge", "no_more",       False, "횟수 소진 안내"),
    ("rankingchallenge", "exit",          False, "종료 버튼"),

    # ── 리그모드 ──
    ("leaguemode", "entry",     True,  "리그모드 메뉴 아이콘"),
    ("leaguemode", "play",      True,  "경기 시작/플레이 버튼"),
    ("leaguemode", "lineup_ok", False, "라인업 확인 후 시작 버튼"),
    ("leaguemode", "auto",      False, "자동 진행"),
    ("leaguemode", "skip",      False, "결과 스킵"),
    ("leaguemode", "result",    True,  "결과 화면"),
    ("leaguemode", "claim",     True,  "보상 수령"),
    ("leaguemode", "no_more",   False, "더 이상 가능한 경기 없음"),
    ("leaguemode", "exit",      False, "종료 버튼"),
]

BUCKET_LABELS = {
    "home":              "공용 (로비/팝업)",
    "sponsor":           "후원금 정산",
    "pointshop":         "포인트상점",
    "homerunrace":       "홈런레이스",
    "specialmatch":      "스페셜매치",
    "rankingchallenge":  "랭킹챌린지",
    "leaguemode":        "리그모드",
}

BUCKET_ORDER = ["home", "sponsor", "pointshop", "homerunrace",
                "specialmatch", "rankingchallenge", "leaguemode"]
