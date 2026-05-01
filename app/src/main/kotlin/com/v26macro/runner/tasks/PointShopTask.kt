package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 포인트상점.
 *
 * 흐름:
 *   1. 메인의 '상점' 버튼
 *   2. 진입 팝업 닫기
 *   3. '아이템' 탭
 *   4. '포인트상점' 서브탭
 *   5. 일반 상품 2종 구매: buy_itemN → 최대수량구매 → (확정)
 *   6. 단계별 5종: 같은 위치(buy_tier)를 5번 반복 (10000→30000→50000→70000→100000)
 *   7. X로 종료 → 메인으로 돌아가기
 *
 * 필요한 템플릿: pointshop/entry, item_tab, pointshop_tab, buy_tier, max_buy, exit
 *               (선택) buy_item1, buy_item2, buy_confirm, home/popup_close
 */
class PointShopTask : Task {
    override val kind = TaskKind.PointShop
    private val bucket = kind.bucket

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("포인트상점 시작")

        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }
        humanDelay(900L, 300L)

        // 1) 진입 팝업 닫기 (여러 번 뜰 수 있음)
        dismissPopups(maxLoops = 3)

        // 2) 아이템 탭
        if (!tapTemplate(bucket, "item_tab", timeoutMs = 6000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/item_tab")
        }
        humanDelay()

        // 3) 포인트상점 서브탭
        if (!tapTemplate(bucket, "pointshop_tab", timeoutMs = 6000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/pointshop_tab")
        }
        humanDelay()

        var bought = 0

        // 4) 일반 상품 2종 — buy_item1, buy_item2 가 정의돼 있을 때만 구매
        for (slot in listOf("buy_item1", "buy_item2")) {
            val match = find(bucket, slot) ?: continue
            tap(match.centerX, match.centerY)
            humanDelay()
            // 구매 다이얼로그: 최대수량구매 → 확정
            tapTemplate(bucket, "max_buy", timeoutMs = 4000L)
            tapTemplate(bucket, "buy_confirm", timeoutMs = 3000L)
            // 보상 팝업 닫기
            dismissPopups(maxLoops = 3)
            bought++
            humanDelay(900L, 250L)
        }

        // 5) 단계별 5회 — buy_tier 같은 위치에서 가격이 자동 증가
        var tier = 0
        repeat(7) {  // 안전 상한 (실제 5회면 충분)
            if (tier >= 5) return@repeat
            val match = find(bucket, "buy_tier") ?: return@repeat
            tap(match.centerX, match.centerY)
            humanDelay()
            // 다이얼로그가 뜨는 경우 처리
            tapTemplate(bucket, "max_buy", timeoutMs = 3000L)
            tapTemplate(bucket, "buy_confirm", timeoutMs = 3000L)
            dismissPopups(maxLoops = 2)
            tier++
            bought++
            humanDelay(900L, 250L)
        }

        // 6) X 로 상점 종료
        if (!tapTemplate(bucket, "exit", timeoutMs = 4000L)) {
            tapBack()  // exit 버튼 못 찾으면 시스템 BACK
        }

        // 7) 메인 복귀
        returnToMainMenu()

        progress("포인트상점 완료: ${bought}건 구매")
        return TaskResult.Success
    }
}
