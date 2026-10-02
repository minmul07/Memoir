package minmul.memoir.feature.onboarding

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingModelSetupTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `complete remains available in every model status and with setting errors`() {
        val gemmaStatus = mutableStateOf(GemmaModelStatus.Missing)
        val ocrStatus = mutableStateOf(OcrModelStatus.Missing)
        var completed = 0
        compose.setContent {
            MemoirTheme {
                OnboardingModelSetupPage(
                    onComplete = { completed++ },
                    gemmaModels = GemmaModel.entries.map { GemmaModelState(it, gemmaStatus.value) },
                    recommendedOcrModel = OcrModelState(OcrModel.Korean, ocrStatus.value),
                    onInstallGemma = {},
                    onCancelGemma = {},
                    onSelectGemma = {},
                    onInstallOcr = {},
                    onRefresh = {},
                    preferencesFailed = true,
                    refreshFailed = true,
                )
            }
        }
        for (status in GemmaModelStatus.entries) {
            compose.runOnIdle { gemmaStatus.value = status }
            compose.onNodeWithText(label(R.string.action_done)).assertIsDisplayed()
                .assertIsEnabled().performClick()
        }
        for (status in OcrModelStatus.entries) {
            compose.runOnIdle { ocrStatus.value = status }
            compose.onNodeWithText(label(R.string.action_done)).assertIsDisplayed()
                .assertIsEnabled().performClick()
        }
        compose.runOnIdle {
            assertEquals(
                GemmaModelStatus.entries.size + OcrModelStatus.entries.size,
                completed
            )
        }
    }

    @Test
    fun `installed Gemma choices use radio buttons without delete controls`() {
        var selected: GemmaModel? = null
        showPage(
            gemmaModels = listOf(
                GemmaModelState(GemmaModel.E4B, GemmaModelStatus.Ready, selected = true),
                GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Ready),
            ),
            onSelectGemma = { selected = it },
        )

        compose.onNodeWithText(label(R.string.action_delete)).assertDoesNotExist()
        compose.onNodeWithContentDescription(
            label(
                R.string.gemma_model_select_named,
                label(R.string.model_gemma_4_e4b)
            )
        )
            .assertIsSelected()
        compose.onNodeWithContentDescription(
            label(
                R.string.gemma_model_select_named,
                label(R.string.model_gemma_4_e2b)
            )
        )
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals(GemmaModel.E2B, selected) }
    }

    @Test
    fun `installed OCR shows recommended label and already installed without checkbox`() {
        showPage(ocrState = OcrModelState(OcrModel.Korean, OcrModelStatus.Ready))

        compose.onNodeWithText(label(R.string.onboarding_model_recommended)).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(label(R.string.onboarding_model_already_installed))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(
            label(
                R.string.ocr_model_download_named,
                label(R.string.ocr_model_korean)
            )
        )
            .assertDoesNotExist()
        compose.onNodeWithContentDescription(
            label(
                R.string.ocr_model_enable_named,
                label(R.string.ocr_model_korean)
            )
        )
            .assertDoesNotExist()
        for (res in listOf(
            R.string.ocr_model_japanese,
            R.string.ocr_model_chinese,
            R.string.ocr_model_latin,
            R.string.ocr_model_devanagari
        )) {
            compose.onNodeWithText(label(res)).assertDoesNotExist()
        }
    }

    @Test
    fun `missing recommended OCR download button requests its model`() {
        var installed: OcrModel? = null
        showPage(
            ocrState = OcrModelState(OcrModel.Japanese, OcrModelStatus.Missing),
            onInstallOcr = { installed = it },
        )

        compose.onNodeWithContentDescription(
            label(
                R.string.ocr_model_download_named,
                label(R.string.ocr_model_japanese)
            )
        )
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals(OcrModel.Japanese, installed) }
    }

    @Test
    fun `Gemma download and cancellation controls request the corresponding model`() {
        var installed: GemmaModel? = null
        var cancelled: GemmaModel? = null
        showPage(
            gemmaModels = listOf(
                GemmaModelState(GemmaModel.E4B, GemmaModelStatus.Missing),
                GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Downloading, 42, 100),
            ),
            onInstallGemma = { installed = it },
            onCancelGemma = { cancelled = it },
        )

        compose.onNodeWithContentDescription(
            label(
                R.string.ocr_model_download_named,
                label(R.string.model_gemma_4_e4b)
            )
        )
            .performScrollTo().performClick()
        compose.onNodeWithContentDescription(
            label(
                R.string.gemma_model_cancel_named,
                label(R.string.model_gemma_4_e2b)
            )
        )
            .performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(GemmaModel.E4B, installed)
            assertEquals(GemmaModel.E2B, cancelled)
        }
    }

    private fun showPage(
        gemmaModels: List<GemmaModelState> = GemmaModel.entries.map {
            GemmaModelState(
                it,
                GemmaModelStatus.Missing
            )
        },
        ocrState: OcrModelState = OcrModelState(OcrModel.Korean, OcrModelStatus.Ready),
        onSelectGemma: (GemmaModel) -> Unit = {},
        onInstallOcr: (OcrModel) -> Unit = {},
        onInstallGemma: (GemmaModel) -> Unit = {},
        onCancelGemma: (GemmaModel) -> Unit = {},
    ) {
        compose.setContent {
            MemoirTheme {
                OnboardingModelSetupPage(
                    onComplete = {},
                    gemmaModels = gemmaModels,
                    recommendedOcrModel = ocrState,
                    onInstallGemma = onInstallGemma,
                    onCancelGemma = onCancelGemma,
                    onSelectGemma = onSelectGemma,
                    onInstallOcr = onInstallOcr,
                    onRefresh = {},
                    preferencesLoaded = true,
                )
            }
        }
    }

    private fun label(res: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(res, *args)
}
