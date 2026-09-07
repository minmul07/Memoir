package minmul.memoir.core.ai

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class OcrModelSelectionTest {
    @Test
    fun `disabled installed models are excluded from recognition`() = runTest {
        val backend = FakeOcrModelBackend().apply {
            available += listOf(OcrModel.Korean, OcrModel.Japanese, OcrModel.Latin)
        }
        val manager = DefaultOcrModelManager(backend, backgroundScope)
        assertEquals(
            listOf(OcrModel.Japanese),
            modelsForRecognition(manager, OcrModel.Korean, setOf(OcrModel.Japanese)),
        )
        assertTrue(backend.downloads.isEmpty())
    }

    @Test
    fun `disabling all installed models skips OCR without downloading another`() = runTest {
        val backend = FakeOcrModelBackend().apply { available += OcrModel.Korean }
        val manager = DefaultOcrModelManager(backend, backgroundScope)
        assertTrue(
            modelsForRecognition(
                manager,
                OcrModel.Korean,
                setOf(OcrModel.Japanese)
            ).isEmpty()
        )
        assertTrue(modelsForRecognition(manager, OcrModel.Korean, emptySet()).isEmpty())
        assertTrue(backend.downloads.isEmpty())
    }

    @Test
    fun `default preparation never restores a disabled model`() = runTest {
        val backend = FakeOcrModelBackend()
        val manager = DefaultOcrModelManager(backend, backgroundScope)
        assertEquals(
            listOf(OcrModel.Japanese),
            modelsForRecognition(manager, OcrModel.Korean, setOf(OcrModel.Japanese)),
        )
        assertEquals(listOf(OcrModel.Japanese), backend.downloads)
    }

    @Test
    fun `analysis uses its completed check even if another screen has begun refreshing`() =
        runTest {
            val manager = object : OcrModelManager {
                override val models = MutableStateFlow(
                    OcrModel.entries.map { OcrModelState(it) },
                )

                override suspend fun refresh() = OcrModel.entries.map {
                    OcrModelState(
                        it,
                        if (it == OcrModel.Japanese) OcrModelStatus.Ready
                        else OcrModelStatus.Missing,
                    )
                }

                override suspend fun install(model: OcrModel) {
                    error("unexpected_download")
                }
            }

            assertEquals(listOf(OcrModel.Japanese), modelsForRecognition(manager, OcrModel.Korean))
        }

    @Test
    fun `locale selects its script and unsupported locales fall back to Latin`() {
        val cases = mapOf(
            "ko-KR" to OcrModel.Korean,
            "ja-JP" to OcrModel.Japanese,
            "zh-CN" to OcrModel.Chinese,
            "zh-Hant-TW" to OcrModel.Chinese,
            "yue-HK" to OcrModel.Chinese,
            "hi-IN" to OcrModel.Devanagari,
            "mr-IN" to OcrModel.Devanagari,
            "ne-NP" to OcrModel.Devanagari,
            "sa-IN" to OcrModel.Devanagari,
            "hi-Latn" to OcrModel.Latin,
            "en-US" to OcrModel.Latin,
            "fr-FR" to OcrModel.Latin,
            "ar-SA" to OcrModel.Latin,
            "ru-Cyrl" to OcrModel.Latin,
            "und" to OcrModel.Latin,
        )
        cases.forEach { (tag, expected) ->
            assertEquals(
                expected,
                defaultOcrModel(Locale.forLanguageTag(tag)),
                tag
            )
        }
    }

    @Test
    fun `available subset is used without downloading the locale model`() = runTest {
        val backend = FakeOcrModelBackend().apply {
            available += listOf(OcrModel.Japanese, OcrModel.Latin)
        }
        val manager = DefaultOcrModelManager(backend, backgroundScope)

        val selected = modelsForRecognition(manager, OcrModel.Korean)

        assertEquals(listOf(OcrModel.Japanese, OcrModel.Latin), selected)
        assertTrue(backend.downloads.isEmpty())
    }

    @Test
    fun `no installed models downloads only the default before analysis can proceed`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val backend = FakeOcrModelBackend().apply { gates[OcrModel.Devanagari] = gate }
        val manager = DefaultOcrModelManager(backend, backgroundScope)
        val selection = async { modelsForRecognition(manager, OcrModel.Devanagari) }
        runCurrent()

        assertEquals(listOf(OcrModel.Devanagari), backend.downloads)
        assertFalse(selection.isCompleted)

        gate.complete(Unit)
        assertEquals(listOf(OcrModel.Devanagari), selection.await())
    }

    @Test
    fun `analysis joins a default model download started in settings`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val backend = FakeOcrModelBackend().apply { gates[OcrModel.Korean] = gate }
        val manager = DefaultOcrModelManager(backend, backgroundScope)
        val settings = async { manager.install(OcrModel.Korean) }
        runCurrent()
        val selection = async { modelsForRecognition(manager, OcrModel.Korean) }
        runCurrent()
        assertEquals(listOf(OcrModel.Korean), backend.downloads)

        gate.complete(Unit)
        settings.await()
        assertEquals(listOf(OcrModel.Korean), selection.await())
    }

    @Test
    fun `failed checks do not trigger speculative downloads but available models remain usable`() =
        runTest {
            val backend = FakeOcrModelBackend().apply { checkFailures += OcrModel.Chinese }
            val manager = DefaultOcrModelManager(backend, backgroundScope)

            assertTrue(runCatching { modelsForRecognition(manager, OcrModel.Korean) }.isFailure)
            assertTrue(backend.downloads.isEmpty())

            backend.available += OcrModel.Latin
            assertEquals(listOf(OcrModel.Latin), modelsForRecognition(manager, OcrModel.Korean))
            assertTrue(backend.downloads.isEmpty())
        }

    @Test
    fun `failed default download fails preparation without trying other models`() = runTest {
        val backend = FakeOcrModelBackend().apply { installFailures += OcrModel.Korean }
        val manager = DefaultOcrModelManager(backend, backgroundScope)

        assertTrue(runCatching { modelsForRecognition(manager, OcrModel.Korean) }.isFailure)
        assertEquals(listOf(OcrModel.Korean), backend.downloads)
    }
}
