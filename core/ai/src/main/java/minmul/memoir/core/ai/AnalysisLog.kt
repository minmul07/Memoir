package minmul.memoir.core.ai

import android.util.Log

/** Metadata only: never pass OCR text, payloads, paths or exception messages. */
object AnalysisLog {
    fun write(message: String) {
        if (BuildConfig.DEBUG) Log.v("MemoirAnalysis", message)
    }
}