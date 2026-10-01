package minmul.memoir.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import minmul.memoir.data.preferences.OnboardingProgress

@Composable
fun OnboardingRoute(
    progress: Int,
    onAdvance: (Int) -> Unit,
    onComplete: () -> Unit,
    onDisagreeCrashlytics: () -> Unit,
    modifier: Modifier = Modifier,
    isSavingCrashlytics: Boolean = false,
    hasCrashlyticsSaveError: Boolean = false,
) {
    when (progress) {
        OnboardingProgress.PERMISSION -> OnboardingPermissionRequestRoute(
            onContinue = { onAdvance(OnboardingProgress.CRASHLYTICS) },
            modifier = modifier,
        )

        OnboardingProgress.CRASHLYTICS -> OnboardingCrashlyticsRequestScreen(
            onContinue = { onAdvance(OnboardingProgress.MODEL_SETUP) },
            onDisagree = onDisagreeCrashlytics,
            isSaving = isSavingCrashlytics,
            hasSaveError = hasCrashlyticsSaveError,
            modifier = modifier,
        )

        OnboardingProgress.MODEL_SETUP -> OnboardingModelSetupPage(
            onComplete = onComplete,
            modifier = modifier,
        )

        else -> LandingScreen(
            onContinue = { onAdvance(OnboardingProgress.PERMISSION) },
            modifier = modifier,
        )
    }
}
