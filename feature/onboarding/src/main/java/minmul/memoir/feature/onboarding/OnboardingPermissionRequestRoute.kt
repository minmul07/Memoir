package minmul.memoir.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun OnboardingPermissionRequestRoute(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissions = photoPermissionsForSdk(Build.VERSION.SDK_INT)
    OnboardingPermissionRequestContent(
        onContinue = onContinue,
        modifier = modifier,
        permissions = permissions,
        hasFullPhotoAccess = {
            context.checkSelfPermission(permissions.first()) == PackageManager.PERMISSION_GRANTED
        },
    )
}

internal fun photoPermissionsForSdk(sdkInt: Int): Array<String> = when {
    sdkInt >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )

    sdkInt >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@Composable
internal fun OnboardingPermissionRequestContent(
    onContinue: () -> Unit,
    permissions: Array<String>,
    hasFullPhotoAccess: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    var requestInFlight by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }

    fun continueOnce() {
        if (!completed) {
            completed = true
            requestInFlight = false
            onContinue()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Full access, selected photos, denial and cancellation all finish this step.
        continueOnce()
    }

    OnboardingPermissionRequestScreen(
        onContinue = {
            if (!requestInFlight) continueOnce()
        },
        onRequestPermission = {
            if (!requestInFlight && !completed) {
                if (hasFullPhotoAccess()) {
                    continueOnce()
                } else {
                    requestInFlight = true
                    launcher.launch(permissions)
                }
            }
        },
        buttonsEnabled = !requestInFlight && !completed,
        modifier = modifier,
    )
}
