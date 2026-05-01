package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 포인트상점: 정해진 우선순위의 상품들을 위에서부터 자동 구매. 각 상품 슬롯의
 * 템플릿(buy_slot1.png, buy_slot2.png, ...)이 매칭되면 구매 -> 수량/확인 팝업 닫기.
 *
 * Required templates (assets/templates/pointshop/):
 *   - entry.png        메인의 "포인트상점" 아이콘
 *   - buy_slot1..N.png 사용자가 사고 싶은 상품의 "구매" 버튼들
 *   - confirm.png      수량 선택 후 확인 버튼
 *   - sold_out.png     (선택) "매진" 표시 - 보이면 다음 슬롯
 */
class PointShopTask : Task {
    override val kind = TaskKind.PointShop
    private val bucket = kind.bucket

    private val maxSlots = 10

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("포인트상점 시작")

        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }
        humanDelay(900L, 200L)

        var bought = 0
        for (slot in 1..maxSlots) {
            val name = "buy_slot$slot"
            // 슬롯 자체가 없으면 (= 사용자가 정의 안 한 슬롯) 스킵
            val match = find(bucket, name) ?: continue
            tap(match.centerX, match.centerY)
            // 수량 확인/구매 확인 팝업 처리
            tapTemplate(bucket, "confirm", timeoutMs = 4000L)
            // 보상 획득 팝업 닫기
            repeat(2) {
                tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 1500L)
                tapTemplate(BUCKET_HOME, "popup_confirm", timeoutMs = 1500L)
            }
            bought++
            humanDelay()
        }

        dismissPopupsAndReturnToLobby()
        progress("포인트상점 완료: $bought 슬롯 처리")
        return TaskResult.Success
    }
}
