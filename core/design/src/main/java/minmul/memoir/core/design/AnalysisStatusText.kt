package minmul.memoir.core.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.LlmRuntimeStatus

@Composable
fun analysisStatusText(status: JobStatus?, stage: JobStage = JobStage.Waiting, attemptCount: Int = 0): String {
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

@Composable
fun llmRuntimeStatusText(status: LlmRuntimeStatus): String? {
    return when (status) {
        LlmRuntimeStatus.Idle -> null
        LlmRuntimeStatus.Missing -> stringResource(R.string.llm_status_missing)
        is LlmRuntimeStatus.Loading -> stringResource(
            R.string.llm_status_loading,
            gemmaModelName(status.model)
        )

        is LlmRuntimeStatus.Ready -> stringResource(
            R.string.llm_status_ready,
            gemmaModelName(status.model)
        )

        is LlmRuntimeStatus.Failed -> stringResource(
            R.string.llm_status_failed,
            gemmaModelName(status.model),
            status.errorClass
        )
    }
}

@Composable
private fun gemmaModelName(model: GemmaModel): String = stringResource(
    when (model) {
        GemmaModel.E4B -> R.string.model_gemma_4_e4b
        GemmaModel.E2B -> R.string.model_gemma_4_e2b
    },
)
