package com.v26macro.v2.storage

import android.content.Context
import java.io.File

/**
 * /sdcard/v26-macro/ 또는 앱 외부 저장 (/sdcard/Android/data/com.v26macro.v2/files/)
 * 둘 중 가능한 곳에서 flows/ templates/ runs/ traces/ 관리.
 *
 * Phase 1: 디렉터리 만들기 + 경로 노출만.
 * Phase 3+: hot-reload watcher.
 */
class FlowStorage(private val context: Context) {

    val root: File by lazy {
        // 우선 앱 전용 외부 저장 — 권한 추가 없이 사용 가능
        // /sdcard/v26-macro/ 가 필요하면 SETUP 에서 MANAGE_EXTERNAL_STORAGE 안내
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        File(base, "v26-macro").apply { mkdirs() }
    }

    val flowsDir: File by lazy { File(root, "flows").apply { mkdirs() } }
    val templatesDir: File by lazy { File(root, "templates").apply { mkdirs() } }
    val runsDir: File by lazy { File(root, "runs").apply { mkdirs() } }
    val tracesDir: File by lazy { File(root, "traces").apply { mkdirs() } }
    val configFile: File by lazy { File(root, "config.yaml") }
    val secretsFile: File by lazy { File(root, "secrets.yaml") }

    fun listFlows(): List<File> =
        flowsDir.listFiles { f -> f.isFile && f.name.endsWith(".yaml") }?.toList().orEmpty()

    /** ADB push 경로 알림용. */
    val adbPushTarget: String
        get() = "/sdcard/Android/data/${context.packageName}/files/v26-macro/flows/"
}
