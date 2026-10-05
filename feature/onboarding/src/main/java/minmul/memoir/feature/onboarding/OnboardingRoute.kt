package minmul.memoir.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = if (progress == OnboardingProgress.COMPLETED) {
                OnboardingProgress.MODEL_SETUP
            } else {
                progress
            },
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
            (slideInHorizontally(
                animationSpec = tween(250),
                initialOffsetX = { it / 6 },
            ) + fadeIn(animationSpec = tween(250))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(250),
                        targetOffsetX = { -it / 6 },
                    ) + fadeOut(animationSpec = tween(250)))
        },
        label = "onboardingStep",
    ) { animatedProgress ->
        when (animatedProgress) {
            OnboardingProgress.PERMISSION -> OnboardingPermissionRequestRoute(
                onContinue = { onAdvance(OnboardingProgress.CRASHLYTICS) },
            )

            OnboardingProgress.CRASHLYTICS -> OnboardingCrashlyticsRequestScreen(
                onContinue = { onAdvance(OnboardingProgress.MODEL_SETUP) },
                onDisagree = onDisagreeCrashlytics,
                isSaving = isSavingCrashlytics,
                hasSaveError = hasCrashlyticsSaveError,
            )

            OnboardingProgress.MODEL_SETUP -> OnboardingModelSetupRoute(
                onComplete = onComplete,
            )

            else -> LandingScreen(
                onContinue = { onAdvance(OnboardingProgress.PERMISSION) },
            )
        }
        }
    }
}
