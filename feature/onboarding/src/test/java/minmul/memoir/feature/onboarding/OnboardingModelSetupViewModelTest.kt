package minmul.memoir.feature.onboarding

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingModelSetupViewModelTest {
    private val gemma = FakeGemmaModelStore()
    private val ocr = FakeOcrModelManager()
    private val preferences = FakeGemmaModelPreferencesStore()

    @Test
    fun `refresh reports installed states without starting downloads`() = withViewModel { vm ->
        ocr.models.value = listOf(OcrModelState(OcrModel.Korean, OcrModelStatus.Ready))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(OcrModelStatus.Ready, vm.state.value.ocrModels.single().status)
        assertTrue(vm.state.value.gemmaModels.all { it.status == GemmaModelStatus.Missing })
        assertTrue(gemma.installed.isEmpty())
        assertTrue(ocr.installed.isEmpty())
        assertNull(preferences.selected.value)
    }

    @Test
    fun `model progress from shared stores is reflected without refreshing`() =
        withViewModel { vm ->
            gemma.models.value = gemma.models.value.map {
                it.copy(
                    status = GemmaModelStatus.Downloading,
                    downloadedBytes = 42,
                    totalBytes = 100
                )
            }
            advanceUntilIdle()

            assertEquals(0.42f, vm.state.value.gemmaModels.first().progress)
            assertTrue(vm.state.value.gemmaModels.all { it.isDownloading })
        }

    @Test
    fun `stored ready choice is preserved`() = runTest {
        gemma.setStatus(GemmaModel.E4B, GemmaModelStatus.Ready)
        gemma.setStatus(GemmaModel.E2B, GemmaModelStatus.Ready)
        preferences.selected.value = GemmaModel.E2B
        useViewModel { vm ->
            assertEquals(GemmaModel.E2B, preferences.selected.value)
            assertTrue(vm.state.value.gemmaModels.first { it.model == GemmaModel.E2B }.selected)
        }
    }

    @Test
    fun `first ready model is selected when nothing is stored`() = withViewModel { vm ->
        gemma.setStatus(GemmaModel.E2B, GemmaModelStatus.Ready)
        advanceUntilIdle()

        assertEquals(GemmaModel.E2B, preferences.selected.value)
        assertTrue(vm.state.value.gemmaModels.first { it.model == GemmaModel.E2B }.selected)
    }

    @Test
    fun `stored choice is retained while checking or downloading and replaced when missing`() =
        runTest {
            preferences.selected.value = GemmaModel.E2B
            gemma.setStatus(GemmaModel.E4B, GemmaModelStatus.Ready)
            gemma.setStatus(GemmaModel.E2B, GemmaModelStatus.Checking)
            useViewModel {
                for (status in listOf(
                    GemmaModelStatus.Checking,
                    GemmaModelStatus.Pending,
                    GemmaModelStatus.Downloading,
                    GemmaModelStatus.Paused
                )) {
                    gemma.setStatus(GemmaModel.E2B, status)
                    advanceUntilIdle()
                    assertEquals(GemmaModel.E2B, preferences.selected.value)
                }
                gemma.setStatus(GemmaModel.E2B, GemmaModelStatus.Missing)
                advanceUntilIdle()
                assertEquals(GemmaModel.E4B, preferences.selected.value)
            }
        }

    @Test
    fun `radio choice is persisted and shared preference updates are observed`() = runTest {
        gemma.setStatus(GemmaModel.E4B, GemmaModelStatus.Ready)
        gemma.setStatus(GemmaModel.E2B, GemmaModelStatus.Ready)
        preferences.selected.value = GemmaModel.E4B
        useViewModel { vm ->
            vm.selectGemma(GemmaModel.E2B)
            advanceUntilIdle()

            assertEquals(GemmaModel.E2B, preferences.selected.value)
            assertTrue(vm.state.value.savingModels.isEmpty())
            preferences.selected.value = GemmaModel.E4B
            advanceUntilIdle()
            assertTrue(vm.state.value.gemmaModels.first { it.model == GemmaModel.E4B }.selected)
        }
    }

    @Test
    fun `models that are not installed cannot be selected`() = withViewModel { vm ->
        for (status in GemmaModelStatus.entries.filter { it != GemmaModelStatus.Ready }) {
            gemma.setStatus(GemmaModel.E2B, status)
            advanceUntilIdle()
            vm.selectGemma(GemmaModel.E2B)
            advanceUntilIdle()
            assertNull(preferences.selected.value)
        }
    }

    @Test
    fun `selection save failure keeps stored choice and allows retry`() = runTest {
        gemma.setStatus(GemmaModel.E4B, GemmaModelStatus.Ready)
        gemma.setStatus(GemmaModel.E2B, GemmaModelStatus.Ready)
        preferences.selected.value = GemmaModel.E4B
        useViewModel { vm ->
            preferences.saveError = IOException()
            vm.selectGemma(GemmaModel.E2B)
            advanceUntilIdle()

            assertEquals(GemmaModel.E4B, preferences.selected.value)
            assertTrue(vm.state.value.preferencesFailed)
            assertTrue(vm.state.value.savingModels.isEmpty())

            preferences.saveError = null
            vm.selectGemma(GemmaModel.E2B)
            advanceUntilIdle()
            assertFalse(vm.state.value.preferencesFailed)
            assertEquals(GemmaModel.E2B, preferences.selected.value)
        }
    }

    @Test
    fun `preference read failure is recoverable with refresh`() = runTest {
        preferences.readError = IOException()
        useViewModel { vm ->
            assertTrue(vm.state.value.preferencesFailed)
            assertFalse(vm.state.value.preferencesLoaded)

            preferences.readError = null
            vm.refresh()
            advanceUntilIdle()
            assertTrue(vm.state.value.preferencesLoaded)
            assertFalse(vm.state.value.preferencesFailed)
        }
    }

    @Test
    fun `failed automatic selection is retried on refresh`() = runTest {
        gemma.setStatus(GemmaModel.E2B, GemmaModelStatus.Ready)
        preferences.saveError = IOException()
        useViewModel { vm ->
            assertTrue(vm.state.value.preferencesFailed)
            assertNull(preferences.selected.value)

            preferences.saveError = null
            vm.refresh()
            advanceUntilIdle()
            assertEquals(GemmaModel.E2B, preferences.selected.value)
            assertFalse(vm.state.value.preferencesFailed)
        }
    }

    @Test
    fun `Gemma refresh failure does not prevent OCR refresh and can be retried`() =
        withViewModel { vm ->
            gemma.refreshError = IOException()
            vm.refresh()
            advanceUntilIdle()

            assertEquals(1, ocr.refreshCount)
            assertTrue(vm.state.value.refreshFailed)

            gemma.refreshError = null
            vm.refresh()
            advanceUntilIdle()
            assertFalse(vm.state.value.refreshFailed)
        }

    @Test
    fun `Gemma download requests are sent to the shared store`() = withViewModel { vm ->
        vm.installGemma(GemmaModel.E2B)
        advanceUntilIdle()
        assertEquals(listOf(GemmaModel.E2B), gemma.installed)
        assertEquals(
            GemmaModelStatus.Pending,
            vm.state.value.gemmaModels.first { it.model == GemmaModel.E2B }.status
        )
    }

    @Test
    fun `Gemma cancellation is sent to the shared store`() = withViewModel { vm ->
        gemma.setStatus(GemmaModel.E2B, GemmaModelStatus.Downloading)
        vm.cancelGemma(GemmaModel.E2B)
        advanceUntilIdle()
        assertEquals(listOf(GemmaModel.E2B), gemma.cancelled)
    }

    @Test
    fun `OCR download requests are sent to the shared manager`() = withViewModel { vm ->
        vm.installOcr(OcrModel.Korean)
        advanceUntilIdle()
        assertEquals(listOf(OcrModel.Korean), ocr.installed)
        assertEquals(
            OcrModelStatus.Pending,
            vm.state.value.ocrModels.first { it.model == OcrModel.Korean }.status
        )
    }

    private fun withViewModel(block: suspend TestScope.(OnboardingModelSetupViewModel) -> Unit) =
        runTest {
            useViewModel(block)
        }

    private suspend fun TestScope.useViewModel(block: suspend TestScope.(OnboardingModelSetupViewModel) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = OnboardingModelSetupViewModel(gemma, ocr, preferences)
        try {
            advanceUntilIdle()
            block(vm)
        } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}
