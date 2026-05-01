package com.v26macro.util

import android.util.Log
import com.v26macro.V26MacroApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val TAG = "V26Macro"

    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val logFile: File by lazy {
        val dir = File(V26MacroApp.instance.getExternalFilesDir(null), "logs").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        File(dir, "run-$stamp.txt")
    }

    fun i(msg: String) {
        Log.i(TAG, msg)
        appendFile("I", msg)
    }

    fun w(msg: String, t: Throwable? = null) {
        Log.w(TAG, msg, t)
        appendFile("W", msg + (t?.let { " :: $it" } ?: ""))
    }

    fun e(msg: String, t: Throwable? = null) {
        Log.e(TAG, msg, t)
        appendFile("E", msg + (t?.let { " :: $it" } ?: ""))
    }

    private fun appendFile(level: String, msg: String) {
        try {
            logFile.appendText("${timeFmt.format(Date())} $level $msg\n")
        } catch (_: Exception) {
            // disk full / permission - keep going
        }
    }
}
