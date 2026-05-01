package com.v26macro.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * MLKit text recognition wrapper. Used for reading remaining-attempt counters,
 * scores, etc. Lazy because not every Task needs OCR.
 */
object OcrReader {
    private val recognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    suspend fun read(frame: Bitmap, region: Rect? = null): String {
        val cropped = if (region != null) {
            Bitmap.createBitmap(
                frame,
                region.left.coerceAtLeast(0),
                region.top.coerceAtLeast(0),
                (region.width()).coerceAtMost(frame.width - region.left),
                (region.height()).coerceAtMost(frame.height - region.top),
            )
        } else frame

        val image = InputImage.fromBitmap(cropped, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    /** Convenience: pull the first integer found in the recognized text. */
    suspend fun readInt(frame: Bitmap, region: Rect? = null): Int? {
        val text = read(frame, region)
        return Regex("\\d+").find(text)?.value?.toIntOrNull()
    }
}
