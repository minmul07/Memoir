package minmul.memoir.feature.queue

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.ImageThumbnail
import minmul.memoir.core.design.R
import minmul.memoir.core.design.analysisStatusText
import minmul.memoir.core.design.component.MemoirFab
import minmul.memoir.core.design.llmRuntimeStatusText
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.LlmRuntimeStatus
import minmul.memoir.core.model.QueueItem

@Composable
fun WorkQueueScreen(
    items: List<QueueItem>,
    isLoading: Boolean,
    failed: Boolean,
    modifier: Modifier = Modifier,
    onCancel: (String) -> Unit = {},
    onOpenItem: (String) -> Unit = {},
    onStart: () -> Unit = {},
    actionFailed: Boolean = false,
    serviceFailed: Boolean = false,
    llmStatus: LlmRuntimeStatus = LlmRuntimeStatus.Idle,
) {
    val entries = rememberQueueEntries(items)
    val activeEntry = entries.firstOrNull { it.transition.targetState == QueueLocation.Active }
        ?: entries.firstOrNull { it.transition.currentState == QueueLocation.Active }
    val statusText = activeEntry?.let {
        analysisStatusText(it.item.status, it.item.stage, it.item.attemptCount)
    } ?: llmRuntimeStatusText(llmStatus)
    // Keep the last title during the card's exit, including Ready -> Idle after the last job.
    var lastStatusText by remember { mutableStateOf(statusText.orEmpty()) }
    if (statusText != null && lastStatusText != statusText) lastStatusText = statusText
    val motionScheme = MaterialTheme.motionScheme
    val enter = fadeIn(motionScheme.defaultEffectsSpec()) +
            expandVertically(
                motionScheme.defaultSpatialSpec(),
                expandFrom = Alignment.CenterVertically
            )
    val exit = fadeOut(motionScheme.defaultEffectsSpec()) +
            shrinkVertically(
                motionScheme.defaultSpatialSpec(),
                shrinkTowards = Alignment.CenterVertically
            )

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.queue_item_count, items.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = enter,
                    exit = exit,
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("queue-status-card"),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            AnimatedContent(
                                targetState = statusText ?: lastStatusText,
                                transitionSpec = {
                                    fadeIn(motionScheme.defaultEffectsSpec()) togetherWith
                                            fadeOut(motionScheme.defaultEffectsSpec())
                                },
                                label = "queue-status-title",
                            ) { text ->
                                Text(text, style = MaterialTheme.typography.titleSmall)
                            }
                            entries.forEach { entry ->
                                key(entry.item.jobId) {
                                    entry.transition.AnimatedVisibility(
                                        visible = { it == QueueLocation.Active },
                                        enter = enter,
                                        exit = exit,
                                    ) {
                                        WorkQueueListItem(
                                            entry = entry,
                                            visibilityScope = this,
                                            onCancel = onCancel,
                                            onOpenItem = onOpenItem,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    item(key = "queue-message") {
                        AnimatedVisibility(isLoading, enter = enter, exit = exit) {
                            CircularProgressIndicator()
                        }
                        AnimatedVisibility(failed, enter = enter, exit = exit) {
                            Text(stringResource(R.string.queue_load_failed))
                        }
                        AnimatedVisibility(
                            visible = !isLoading && !failed && entries.isEmpty(),
                            enter = enter,
                            exit = exit,
                        ) {
                            Text(stringResource(R.string.queue_empty))
                        }
                    }
                    items(entries, key = { it.item.jobId }) { entry ->
                        entry.transition.AnimatedVisibility(
                            visible = { it == QueueLocation.Waiting },
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                placementSpec = motionScheme.defaultSpatialSpec(),
                                fadeOutSpec = null,
                            ),
                            enter = enter,
                            exit = exit,
                        ) {
                            WorkQueueListItem(
                                entry = entry,
                                visibilityScope = this,
                                onCancel = onCancel,
                                onOpenItem = onOpenItem,
                            )
                        }
                    }
                }
                AnimatedVisibility(actionFailed, enter = enter, exit = exit) {
                    Text(stringResource(R.string.content_action_failed))
                }
                AnimatedVisibility(serviceFailed, enter = enter, exit = exit) {
                    Text(stringResource(R.string.analysis_service_failed))
                }
            }
            MemoirFab(
                onClick = onStart,
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.analysis_restart),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f),
                enabled = workQueueStartEnabled(items, isLoading, failed),
            )
        }
    }
}

private enum class QueueLocation { Waiting, Active, Removed }

private data class QueueEntryData(
    val item: QueueItem,
    val number: Int,
    val animateEnter: Boolean = false,
)

private data class AnimatedQueueEntry(
    val item: QueueItem,
    val number: Int,
    val transition: Transition<QueueLocation>,
)

@Composable
private fun rememberQueueEntries(items: List<QueueItem>): List<AnimatedQueueEntry> {
    var previousItems by remember { mutableStateOf(items) }
    var retained by remember {
        mutableStateOf(items.mapIndexed { index, item -> QueueEntryData(item, index + 1) })
    }
    if (previousItems != items) {
        val currentIds = items.mapTo(mutableSetOf()) { it.jobId }
        val retainedIds = retained.mapTo(mutableSetOf()) { it.item.jobId }
        val updated = items.mapIndexed { index, item ->
            QueueEntryData(item, index + 1, animateEnter = item.jobId !in retainedIds)
        }.toMutableList()
        // Removed rows keep their old position and data until their exit finishes.
        retained.forEachIndexed { index, entry ->
            if (entry.item.jobId !in currentIds) updated.add(
                index.coerceAtMost(updated.size),
                entry
            )
        }
        retained = updated
        previousItems = items
    }
    val currentIds = items.mapTo(mutableSetOf()) { it.jobId }
    return retained.map { entry ->
        key(entry.item.jobId) {
            // Own the transition outside LazyColumn so offscreen removals also finish.
            val location = when {
                entry.item.jobId !in currentIds -> QueueLocation.Removed
                entry.item.status == JobStatus.Running -> QueueLocation.Active
                else -> QueueLocation.Waiting
            }
            val state = remember {
                MutableTransitionState(if (entry.animateEnter) QueueLocation.Removed else location)
            }
            state.targetState = location
            val transition = rememberTransition(
                transitionState = state,
                label = "queue-item-${entry.item.jobId}",
            )
            LaunchedEffect(transition.currentState, transition.targetState, transition.isRunning) {
                if (transition.currentState == QueueLocation.Removed &&
                    transition.targetState == QueueLocation.Removed && !transition.isRunning
                ) {
                    retained = retained.filterNot { it.item.jobId == entry.item.jobId }
                }
            }
            AnimatedQueueEntry(entry.item, entry.number, transition)
        }
    }
}

@Composable
private fun SharedTransitionScope.WorkQueueListItem(
    entry: AnimatedQueueEntry,
    visibilityScope: AnimatedVisibilityScope,
    onCancel: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    val enabled = entry.transition.targetState != QueueLocation.Removed
    Row(
        modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState("queue-item-${entry.item.jobId}"),
                animatedVisibilityScope = visibilityScope,
                boundsTransform = BoundsTransform { _, _ -> motionScheme.defaultSpatialSpec() },
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                enter = fadeIn(motionScheme.defaultEffectsSpec()),
                exit = fadeOut(motionScheme.defaultEffectsSpec()),
            )
            .fillMaxWidth()
            .testTag("queue-item-${entry.item.jobId}")
            .clickable(enabled = enabled) { onOpenItem(entry.item.itemId) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageThumbnail(imagePath = entry.item.imagePath, modifier = Modifier.size(64.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.queue_image_number, entry.number),
                style = MaterialTheme.typography.bodyLarge,
            )
            // Both shared-bound copies use the same transition, so the subtitle shrinks
            // while the row travels and the title settles at the vertical center.
            entry.transition.AnimatedVisibility(
                visible = { it == QueueLocation.Waiting },
                enter = expandVertically(motionScheme.defaultSpatialSpec()) +
                        fadeIn(motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(motionScheme.defaultSpatialSpec()) +
                        fadeOut(motionScheme.defaultEffectsSpec()),
            ) {
                Text(
                    text = analysisStatusText(
                        entry.item.status,
                        entry.item.stage,
                        entry.item.attemptCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = { onCancel(entry.item.jobId) }, enabled = enabled) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

internal fun workQueueStartEnabled(
    items: List<QueueItem>,
    isLoading: Boolean,
    failed: Boolean,
): Boolean =
    !isLoading && !failed && items.isNotEmpty() && items.all { it.status == JobStatus.Queued }

@Preview(showBackground = true)
@Composable
private fun WorkQueueScreenPreview() {
    MemoirTheme {
        WorkQueueScreen(items = previewQueueItems(), isLoading = false, failed = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkQueueScreenPreparingPreview() {
    MemoirTheme {
        WorkQueueScreen(
            items = previewQueueItems(), isLoading = false, failed = false,
            llmStatus = LlmRuntimeStatus.Loading(GemmaModel.E4B),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkQueueScreenAnalyzingPreview() {
    MemoirTheme {
        WorkQueueScreen(
            items = previewQueueItems(running = true), isLoading = false, failed = false,
            llmStatus = LlmRuntimeStatus.Ready(GemmaModel.E4B),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkQueueScreenEmptyPreview() {
    MemoirTheme {
        WorkQueueScreen(items = emptyList(), isLoading = false, failed = false)
    }
}

private fun previewQueueItems(running: Boolean = false) = listOf(
    QueueItem(
        "1",
        "1",
        "preview",
        if (running) JobStatus.Running else JobStatus.Queued,
        JobStage.Ocr
    ),
    QueueItem("2", "2", "preview", JobStatus.Queued),
)
