package com.v26macro.v2.llm

/**
 * 디스코드 웹훅으로 일일 실행 결과 푸시. outbound only — LD 클라우드 인바운드 미노출
 * 환경에서도 안전.
 *
 * Phase 4 에서 구현.
 */
class DiscordWebhook(@Suppress("UNUSED_PARAMETER") private val webhookUrl: String) {
    suspend fun postSummary(
        @Suppress("UNUSED_PARAMETER") title: String,
        @Suppress("UNUSED_PARAMETER") body: String,
        @Suppress("UNUSED_PARAMETER") screenshotPath: String? = null,
    ) {
        TODO("Phase 4: OkHttp multipart POST — content + 옵션으로 PNG 첨부")
    }
}
