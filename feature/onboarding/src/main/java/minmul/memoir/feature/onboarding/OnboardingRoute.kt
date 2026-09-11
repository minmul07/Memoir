package minmul.memoir.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import minmul.memoir.data.preferences.OnboardingProgress

@Composable
fun OnboardingRoute(
    progress: Int,
    onAdvance: (Int) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (progress) {
        OnboardingProgress.PERMISSION -> OnboardingPermissionRequestScreen(
            onContinue = { onAdvance(OnboardingProgress.CRASHLYTICS) },
            modifier = modifier,
        )

        OnboardingProgress.CRASHLYTICS -> OnboardingCrashlyticsRequestScreen(
            onContinue = { onAdvance(OnboardingProgress.MODEL_SETUP) },
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
