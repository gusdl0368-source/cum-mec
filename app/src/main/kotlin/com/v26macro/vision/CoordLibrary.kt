package com.v26macro.vision

import android.content.Context
import com.v26macro.util.Logger
import org.json.JSONArray
import org.json.JSONObject

/**
 * 좌표 오버라이드 저장소.
 *
 * `assets/coords.json` 에서 `{ "<bucket>/<name>": [xFrac, yFrac] }` 형식으로 정의된 정규화
 * 좌표(0.0~1.0) 를 읽어둔다. 런타임에는 현재 화면 해상도를 곱해 픽셀 좌표로 환산된다.
 *
 * Task 가 [TemplateLibrary] 대신 이 좌표를 우선 사용하면, 팀 컬러로 배경이 바뀌는
 * 메뉴 버튼처럼 템플릿 매칭이 불안정한 곳도 안정적으로 탭할 수 있다.
 */
class CoordLibrary(private val ctx: Context) {

    private val coords: Map<String, Pair<Float, Float>> by lazy { load() }

    private fun load(): Map<String, Pair<Float, Float>> {
        return try {
            val text = ctx.assets.open("coords.json").bufferedReader().use { it.readText() }
            if (text.isBlank()) return emptyMap()
            val json = JSONObject(text)
            buildMap {
                val it = json.keys()
                while (it.hasNext()) {
                    val key = it.next()
                    val arr = json.opt(key) as? JSONArray ?: continue
                    if (arr.length() < 2) continue
                    val x = arr.getDouble(0).toFloat()
                    val y = arr.getDouble(1).toFloat()
                    put(key, x to y)
                }
            }.also {
                Logger.i("CoordLibrary: ${it.size}개 좌표 로드됨")
            }
        } catch (e: Exception) {
            Logger.w("coords.json 읽기 실패 (없거나 비어있음): ${e.message}")
            emptyMap()
        }
    }

    /** 정규화된 (xFrac, yFrac) 반환. 정의되지 않았으면 null. */
    fun get(bucket: String, name: String): Pair<Float, Float>? = coords["$bucket/$name"]

    fun has(bucket: String, name: String): Boolean = coords.containsKey("$bucket/$name")

    val size: Int get() = coords.size
}
