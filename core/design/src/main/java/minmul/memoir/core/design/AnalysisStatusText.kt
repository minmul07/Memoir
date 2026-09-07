package minmul.memoir.core.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import minmul.memoir.core.model.*

@Composable
fun analysisStatusText(status: JobStatus?, stage: JobStage = JobStage.Waiting, attemptCount: Int = 0): String {
    if (LocalInspectionMode.current) return "…"
    return stringResource(when {
        status == null -> R.string.analysis_unqueued
        status == JobStatus.Succeeded -> R.string.history_filter_completed
        status == JobStatus.Failed -> R.string.history_filter_failed
        status == JobStatus.Cancelled -> R.string.history_filter_cancelled
        attemptCount > 0 -> R.string.analysis_retrying
        status == JobStatus.Running -> when (stage) {
            JobStage.Ocr -> R.string.analysis_ocr
            JobStage.Infer -> R.string.queue_running
            JobStage.Saving -> R.string.analysis_saving
            JobStage.Waiting -> R.string.queue_waiting
        }
        else -> R.string.queue_waiting
    })
}
