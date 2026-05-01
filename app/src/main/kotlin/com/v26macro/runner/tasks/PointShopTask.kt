package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 포인트상점.
 *
 * 실제 V26 포인트상점은 3개 상품으로 구성돼 있고, 각 상품의 구매 흐름이 다르다:
 *
 *   1) 데일리 럭키 박스 (무료, 일일 0/1)
 *      - 카드 탭 → 상세 페이지 → '무료' 보라 버튼 → 보상 수령
 *
 *   2) 50볼 EVENT (10000P, 일일 0/3, 한 번 구매로 3개 동시)
 *      - 카드 탭 → 상세 페이지 → '최대 개수 설정' → '구매' 버튼
 *
 *   3) 50볼 단계별 (10000→30000→50000→70000→100000P, 5단계 하루에 모두 가능)
 *      - 카드 탭 → 상세 페이지 → 'P 10000' 가격 버튼을 5번 반복 탭
 *        (가격 라벨이 자동 증가하므로 같은 위치를 누르면 됨)
 *
 * 각 상품 구매 후엔 BACK 으로 목록으로 돌아가고, 마지막에 X(또는 BACK) 으로 상점 종료.
 *
 * 필요한 템플릿 (assets/templates/pointshop/):
 *   entry, item_tab, pointshop_tab,
 *   card_lucky_box, free_button,
 *   card_event_50, max_quantity, buy_button,
 *   card_tier_50, tier_buy,
 *   purchase_confirm (선택), exit (선택)
 */
class PointShopTask : Task {
    override val kind = TaskKind.PointShop
    private val bucket = kind.bucket

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("포인트상점 시작")

        // 메인의 상점 버튼
        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }
        humanDelay(900L, 300L)
        // 진입 시 자동으로 뜨는 광고/공지 팝업 닫기
        dismissPopups(maxLoops = 3)

        // 아이템 카테고리 → 포인트상점 서브탭
        if (!tapTemplate(bucket, "item_tab", timeoutMs = 6000L)) {
            returnToMainMenu(); return missingAssets("$bucket/item_tab")
        }
        humanDelay()
        if (!tapTemplate(bucket, "pointshop_tab", timeoutMs = 6000L)) {
            returnToMainMenu(); return missingAssets("$bucket/pointshop_tab")
        }
        humanDelay()

        var bought = 0

        // 1) 데일리 럭키 박스 (무료)
        bought += buyFreeBox()

        // 2) 50볼 EVENT (최대 개수 후 1회 구매)
        bought += buyEventPack()

        // 3) 50볼 단계별 (5번 반복)
        bought += buyTierPack()

        // 상점 종료
        if (!tapTemplate(bucket, "exit", timeoutMs = 2000L)) {
            tapBack()
        }
        returnToMainMenu()

        progress("포인트상점 완료: ${bought}건")
        return TaskResult.Success
    }

    // ── 1) 데일리 럭키 박스 (무료) ──────────────────────────────
    private suspend fun TaskContext.buyFreeBox(): Int {
        if (!tapTemplate(bucket, "card_lucky_box", timeoutMs = 3500L)) return 0
        humanDelay()
        val ok = tapTemplate(bucket, "free_button", timeoutMs = 4000L)
        // 보상 획득 팝업 / 안내 닫기
        dismissPopups(maxLoops = 3)
        tapBack()
        humanDelay(800L, 200L)
        return if (ok) 1 else 0
    }

    // ── 2) 50볼 EVENT (최대 개수 설정 → 구매) ───────────────────
    private suspend fun TaskContext.buyEventPack(): Int {
        if (!tapTemplate(bucket, "card_event_50", timeoutMs = 3500L)) return 0
        humanDelay()
        // '최대 개수 설정' — 일일 한도까지 수량을 채움
        tapTemplate(bucket, "max_quantity", timeoutMs = 3500L)
        humanDelay(600L, 200L)
        // '구매' 버튼
        val ok = tapTemplate(bucket, "buy_button", timeoutMs = 4000L)
        // 구매 확인 다이얼로그가 있으면 처리 (없으면 그냥 패스)
        tapTemplate(bucket, "purchase_confirm", timeoutMs = 2500L)
        dismissPopups(maxLoops = 3)
        tapBack()
        humanDelay(800L, 200L)
        return if (ok) 1 else 0
    }

    // ── 3) 50볼 단계별 (가격이 자동 증가, 5번 탭) ───────────────
    private suspend fun TaskContext.buyTierPack(): Int {
        if (!tapTemplate(bucket, "card_tier_50", timeoutMs = 3500L)) return 0
        humanDelay()
        var count = 0
        // 안전 상한 7회 (실제 5회면 모두 구매 완료)
        repeat(7) {
            if (count >= 5) return@repeat
            val match = find(bucket, "tier_buy") ?: return@repeat
            tap(match.centerX, match.centerY)
            humanDelay(600L, 200L)
            // 구매 확인 다이얼로그가 있으면 처리
            tapTemplate(bucket, "purchase_confirm", timeoutMs = 2500L)
            dismissPopups(maxLoops = 2)
            count++
            humanDelay(800L, 250L)
        }
        tapBack()
        humanDelay(800L, 200L)
        return count
    }
}
