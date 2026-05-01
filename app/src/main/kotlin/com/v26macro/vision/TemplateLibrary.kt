package com.v26macro.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.v26macro.util.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads template PNGs from assets/templates/<bucket>/<name>.png and caches them.
 *
 * Each Task references templates by ("<bucket>", "<name>") - missing assets log
 * a warning and return null so the runner can skip a step gracefully instead of
 * crashing. Users will be adding template PNGs over time.
 */
class TemplateLibrary(private val ctx: Context) {

    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun load(bucket: String, name: String): Bitmap? {
        val key = "$bucket/$name"
        cache[key]?.let { return it }
        val path = "templates/$bucket/$name.png"
        return try {
            ctx.assets.open(path).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return null
                cache[key] = bmp
                bmp
            }
        } catch (e: Exception) {
            Logger.w("template missing: $path")
            null
        }
    }

    fun has(bucket: String, name: String): Boolean = load(bucket, name) != null

    fun clear() {
        cache.values.forEach { it.recycle() }
        cache.clear()
    }
}
