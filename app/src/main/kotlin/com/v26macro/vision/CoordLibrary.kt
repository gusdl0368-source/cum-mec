package com.v26macro.vision

import android.content.Context
import com.v26macro.util.Logger
import org.json.JSONArray
import org.json.JSONObject

/**
 * 좌표 오버라이드 저장소.
 *
 * `assets/coords.json` 의 entry 두 형식을 모두 지원:
 *  - 배열 `[xFrac, yFrac]`                            → 가드 없음, 보면 바로 탭
 *  - 객체 `{ "x": ..., "y": ..., "guard": "b/n" }`     → guard 템플릿이 보일 때만 탭
 *
 * Task 가 [TemplateLibrary] 대신 이 좌표를 우선 사용하면, 팀 컬러로 배경이 바뀌는
 * 메뉴 버튼처럼 템플릿 매칭이 불안정한 곳도 안정적으로 탭할 수 있다. guard 가
 * 지정된 경우엔 "이 화면일 때만 탭" 이라는 안전장치가 추가된다.
 */
data class CoordEntry(val xFrac: Float, val yFrac: Float, val guard: String? = null) {
    /** guard 가 "bucket/name" 형식이면 (bucket, name) 으로 분해. 잘못된 형식이면 null. */
    fun guardSplit(): Pair<String, String>? {
        val g = guard?.trim().orEmpty()
        if (g.isEmpty() || !g.contains('/')) return null
        val (b, n) = g.split('/', limit = 2)
        if (b.isBlank() || n.isBlank()) return null
        return b to n
    }
}

class CoordLibrary(private val ctx: Context) {

    private val coords: Map<String, CoordEntry> by lazy { load() }

    private fun load(): Map<String, CoordEntry> {
        return try {
            val text = ctx.assets.open("coords.json").bufferedReader().use { it.readText() }
            if (text.isBlank()) return emptyMap()
            val json = JSONObject(text)
            buildMap {
                val it = json.keys()
                while (it.hasNext()) {
                    val key = it.next()
                    val parsed = parseEntry(json.opt(key)) ?: continue
                    put(key, parsed)
                }
            }.also {
                Logger.i("CoordLibrary: ${it.size}개 좌표 로드됨")
            }
        } catch (e: Exception) {
            Logger.w("coords.json 읽기 실패 (없거나 비어있음): ${e.message}")
            emptyMap()
        }
    }

    private fun parseEntry(raw: Any?): CoordEntry? = when (raw) {
        is JSONArray -> {
            if (raw.length() < 2) null
            else CoordEntry(
                xFrac = raw.getDouble(0).toFloat(),
                yFrac = raw.getDouble(1).toFloat(),
                guard = if (raw.length() >= 3) raw.optString(2).ifBlank { null } else null,
            )
        }
        is JSONObject -> {
            if (!raw.has("x") || !raw.has("y")) null
            else CoordEntry(
                xFrac = raw.getDouble("x").toFloat(),
                yFrac = raw.getDouble("y").toFloat(),
                guard = raw.optString("guard").ifBlank { null },
            )
        }
        else -> null
    }

    /** 정규화 좌표 entry 반환. 정의되지 않았으면 null. */
    fun get(bucket: String, name: String): CoordEntry? = coords["$bucket/$name"]

    fun has(bucket: String, name: String): Boolean = coords.containsKey("$bucket/$name")

    val size: Int get() = coords.size
}
