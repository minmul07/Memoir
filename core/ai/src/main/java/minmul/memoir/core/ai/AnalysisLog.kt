package minmul.memoir.core.ai

import timber.log.Timber

/** Metadata only: never pass OCR text, payloads, paths or exception messages. */
object AnalysisLog {
    fun write(message: String) {
        Timber.tag(TAG).v(message)
    }

    private const val TAG = "MemoirAnalysis"
}
