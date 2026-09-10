package minmul.memoir.feature.queue

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import minmul.memoir.core.design.ImageThumbnail
import minmul.memoir.core.design.R
import minmul.memoir.core.design.analysisStatusText
import minmul.memoir.core.design.llmRuntimeStatusText
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.LlmRuntimeStatus
import minmul.memoir.core.model.QueueItem
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WorkQueueScreen(
    items: List<QueueItem>,
    isLoading: Boolean,
    failed: Boolean,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (String) -> Unit = {},
    onOpenItem: (String) -> Unit = {},
    onStart: () -> Unit = {},
    actionFailed: Boolean = false,
    serviceFailed: Boolean = false,
    llmStatus: LlmRuntimeStatus = LlmRuntimeStatus.Idle,
) {
    val llmStatusText = llmRuntimeStatusText(llmStatus)
    val analyzingText = stringResource(R.string.queue_running)
    var showAnalyzingAfterReady by remember { mutableStateOf(false) }
    LaunchedEffect(llmStatus) {
        if (llmStatus is LlmRuntimeStatus.Ready) {
            showAnalyzingAfterReady = false
            delay(3_000.milliseconds)
            showAnalyzingAfterReady = true
        } else {
            showAnalyzingAfterReady = false
        }
    }
    val statusCardText = if (llmStatus is LlmRuntimeStatus.Ready && showAnalyzingAfterReady) {
        analyzingText
    } else {
        llmStatusText
    }
    val motionScheme = MaterialTheme.motionScheme
    val textSlidePx = with(LocalDensity.current) { 40.dp.roundToPx() }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.queue_item_count, items.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            AnimatedVisibility(statusCardText != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    AnimatedContent(
                        targetState = statusCardText.orEmpty(),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .clipToBounds(),
                        transitionSpec = {
                            slideInHorizontally(
                                animationSpec = motionScheme.defaultSpatialSpec(),
                                initialOffsetX = { textSlidePx },
                            ) + fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                                    slideOutHorizontally(
                                        animationSpec = motionScheme.defaultSpatialSpec(),
                                        targetOffsetX = { -textSlidePx },
                                    ) + fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
                        },
                        label = "llm-status-text",
                    ) { text ->
                        Text(text = text)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    isLoading -> item { CircularProgressIndicator() }
                    failed -> item { Text(stringResource(R.string.queue_load_failed)) }
                    items.isEmpty() -> item { Text(stringResource(R.string.queue_empty)) }
                }
                itemsIndexed(items, key = { _, item -> item.jobId }) { index, item ->
                    ListItem(
                        onClick = { onOpenItem(item.itemId) },
                        trailingContent = {
                            TextButton(onClick = { onCancel(item.jobId) }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        },
                        content = {
                            Text(stringResource(R.string.queue_image_number, index + 1))
                        },
                        supportingContent = {
                            Text(analysisStatusText(item.status, item.stage, item.attemptCount))
                        },
                        leadingContent = {
                            ImageThumbnail(
                                imagePath = item.imagePath,
                                modifier = Modifier.size(64.dp)
                            )
                        },
                    )
                }
            }
            if (actionFailed) Text(stringResource(R.string.content_action_failed))
            if (serviceFailed) Text(stringResource(R.string.analysis_service_failed))
            TextButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.nav_analysis_history))
            }
        }
        if (items.isNotEmpty()) {
            FloatingActionButton(
                onClick = onStart,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (llmStatusText != null) 96.dp else 80.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.analysis_restart),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkQueueScreenPreview() {
    MemoirTheme {
        WorkQueueScreen(
            items = listOf(
                QueueItem("1", "1", "preview", JobStatus.Running, JobStage.Ocr),
                QueueItem("2", "2", "preview", JobStatus.Queued),
            ),
            isLoading = false,
            failed = false,
            onOpenHistory = {},
            llmStatus = LlmRuntimeStatus.Ready(GemmaModel.E4B),
        )
    }
}
