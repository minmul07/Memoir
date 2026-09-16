package minmul.memoir.feature.queue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.LlmRuntimeStatus
import minmul.memoir.core.model.QueueItem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class WorkQueueScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val first = QueueItem("1", "item-1", "preview", JobStatus.Queued)
    private val second = QueueItem("2", "item-2", "preview", JobStatus.Queued)
    private val items = mutableStateOf(listOf(first, second))
    private val status = mutableStateOf<LlmRuntimeStatus>(LlmRuntimeStatus.Idle)

    @Test
    fun cardAppearsOnlyWhenModelPreparationStarts() {
        showQueue()
        compose.onNodeWithTag("queue-status-card").assertDoesNotExist()

        compose.runOnIdle { status.value = LlmRuntimeStatus.Loading(GemmaModel.E4B) }
        compose.onNodeWithTag("queue-status-card").assertIsDisplayed()
        assertTrue(bounds("queue-item-1").top >= bounds("queue-status-card").bottom)
        compose.onNodeWithText("Gemma 4 E4B GPU 로드 중").assertIsDisplayed()
    }

    @Test
    fun runningRowMovesAndNarrowsIntoCardWhileSubtitleCollapses() {
        status.value = LlmRuntimeStatus.Loading(GemmaModel.E4B)
        showQueue()
        val before = bounds("queue-item-1")
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            status.value = LlmRuntimeStatus.Ready(GemmaModel.E4B)
            items.value =
                listOf(first.copy(status = JobStatus.Running, stage = JobStage.Ocr), second)
        }
        compose.mainClock.advanceTimeBy(96)
        val during = compose.onAllNodesWithTag("queue-item-1", useUnmergedTree = true)
            .fetchSemanticsNodes().map { it.boundsInRoot }
        compose.mainClock.advanceTimeBy(2_000)
        val after = bounds("queue-item-1")
        val card = bounds("queue-status-card")

        assertTrue(after.width < before.width)
        assertTrue(after.top < before.top)
        assertTrue(during.any { it.width > after.width + 1f && it.width < before.width - 1f })
        assertTrue(after.top >= card.top && after.bottom <= card.bottom)
        compose.onAllNodesWithTag("queue-item-1").assertCountEquals(1)
        compose.onNodeWithText("텍스트 읽는 중").assertIsDisplayed()
        val title = compose.onNodeWithText("이미지 1", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(abs(title.center.y - after.center.y) < 2f)
    }

    @Test
    fun cancellingWaitingRowKeepsItUntilItsSpaceShrinks() {
        showQueue()
        val before = bounds("queue-item-2").top
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { items.value = listOf(second) }
        compose.mainClock.advanceTimeBy(80)
        compose.onNodeWithTag("queue-item-1").assertExists()
        val during = bounds("queue-item-2").top
        compose.mainClock.advanceTimeBy(2_000)
        val after = bounds("queue-item-2").top

        compose.onNodeWithTag("queue-item-1").assertDoesNotExist()
        assertTrue("The remaining row should move gradually", during > after && during < before)
    }

    @Test
    fun newWaitingRowExpandsItsSpaceGradually() {
        val third = first.copy(jobId = "3", itemId = "item-3")
        items.value = listOf(first, third)
        showQueue()
        val before = bounds("queue-item-3").top
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { items.value = listOf(first, second, third) }
        compose.mainClock.advanceTimeBy(80)
        val during = bounds("queue-item-3").top
        compose.mainClock.advanceTimeBy(2_000)
        val after = bounds("queue-item-3").top

        compose.onNodeWithTag("queue-item-2").assertIsDisplayed()
        assertTrue("The new row should open space gradually", during > before && during < after)
    }

    @Test
    fun completionAndNextJobLeaveOnlyNextJobInCard() {
        items.value =
            listOf(first.copy(status = JobStatus.Running, stage = JobStage.Saving), second)
        status.value = LlmRuntimeStatus.Ready(GemmaModel.E4B)
        showQueue()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            items.value = listOf(second.copy(status = JobStatus.Running, stage = JobStage.Ocr))
        }
        compose.mainClock.advanceTimeBy(80)
        compose.onNodeWithTag("queue-item-1").assertExists()
        compose.mainClock.advanceTimeBy(2_000)

        compose.onNodeWithTag("queue-item-1").assertDoesNotExist()
        compose.onAllNodesWithTag("queue-item-2").assertCountEquals(1)
        assertTrue(bounds("queue-item-2").bottom <= bounds("queue-status-card").bottom)
        compose.runOnIdle { items.value = emptyList(); status.value = LlmRuntimeStatus.Idle }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithTag("queue-status-card").assertDoesNotExist()
        compose.onNodeWithText("대기 중인 이미지가 없습니다").assertIsDisplayed()
    }

    @Test
    fun retryReturnsRowToWaitingListAndCanPromoteItAgain() {
        items.value = listOf(first.copy(status = JobStatus.Running, stage = JobStage.Ocr), second)
        status.value = LlmRuntimeStatus.Ready(GemmaModel.E4B)
        showQueue()
        compose.runOnIdle { items.value = listOf(second, first.copy(attemptCount = 1)) }
        assertTrue(bounds("queue-item-1").top > bounds("queue-item-2").top)
        compose.runOnIdle {
            items.value = listOf(second, first.copy(status = JobStatus.Running, attemptCount = 1))
        }
        assertTrue(bounds("queue-item-1").bottom <= bounds("queue-status-card").bottom)
        compose.onAllNodesWithTag("queue-item-1").assertCountEquals(1)
    }

    @Test
    fun removingOffscreenRowsAlsoFinishesAndShowsEmptyState() {
        items.value = (1..40).map { first.copy(jobId = "$it", itemId = "item-$it") }
        showQueue()
        compose.runOnIdle { items.value = emptyList() }
        compose.onNodeWithText("대기 중인 이미지가 없습니다").assertIsDisplayed()
        compose.onNodeWithTag("queue-item-1").assertDoesNotExist()
    }

    @Test
    fun cancellingDuringPromotionDoesNotLeaveSharedContentBehind() {
        status.value = LlmRuntimeStatus.Loading(GemmaModel.E4B)
        showQueue()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            status.value = LlmRuntimeStatus.Ready(GemmaModel.E4B)
            items.value =
                listOf(first.copy(status = JobStatus.Running, stage = JobStage.Ocr), second)
        }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { items.value = listOf(second) }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithTag("queue-item-1").assertDoesNotExist()
        compose.onAllNodesWithTag("queue-item-2").assertCountEquals(1)
    }

    private fun showQueue() {
        compose.setContent {
            MemoirTheme {
                WorkQueueScreen(
                    items = items.value,
                    isLoading = false,
                    failed = false,
                    llmStatus = status.value
                )
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
}
