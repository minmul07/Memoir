package minmul.memoir.feature.settings

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import minmul.memoir.core.design.R

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenModelManagement: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openSourceLicensesTitle = stringResource(R.string.settings_open_source_licenses)
    SettingsScreen(
        onBack = onBack,
        analysisQueueMode = state.analysisQueueMode,
        analysisQueueModeLoaded = state.preferencesLoaded,
        analysisQueueModeFailed = state.preferencesFailed,
        onAnalysisQueueModeChange = viewModel::setAnalysisQueueMode,
        onOpenModelManagement = onOpenModelManagement,
        onOpenOpenSourceLicenses = {
            OssLicensesMenuActivity.setActivityTitle(openSourceLicensesTitle)
            context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        },
        onOpenDeveloperOptions = onOpenDeveloperOptions,
        modifier = modifier,
    )
}
