package minmul.memoir

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.feature.intake.IntakeConfirmScreen
import minmul.memoir.intake.IntakeUiState
import minmul.memoir.intake.IntakeViewModel
import minmul.memoir.intake.ShareImageParser

@AndroidEntryPoint
class IntakeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val imageUris = ShareImageParser.parse(intent)
        setContent {
            MemoirTheme {
                val viewModel: IntakeViewModel = hiltViewModel()
                LaunchedEffect(viewModel) {
                    viewModel.start(imageUris)
                }
                IntakeRoute(
                    viewModel = viewModel,
                    onOpenOnboarding = { openMain(openQueue = false) },
                    onOpenQueue = { openMain(openQueue = true) },
                    onFinish = { finish() },
                )
            }
        }
    }

    private fun openMain(openQueue: Boolean) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (openQueue) {
                    putExtra(MainActivity.EXTRA_OPEN_QUEUE, true)
                }
            },
        )
        finish()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IntakeRoute(
    viewModel: IntakeViewModel,
    onOpenOnboarding: () -> Unit,
    onOpenQueue: () -> Unit,
    onFinish: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        when (uiState) {
            IntakeUiState.OpenOnboarding -> onOpenOnboarding()
            IntakeUiState.OpenQueue -> onOpenQueue()
            IntakeUiState.Finish -> onFinish()
            IntakeUiState.Loading,
            is IntakeUiState.Confirm -> Unit
        }
    }
    when (val state = uiState) {
        is IntakeUiState.Confirm -> IntakeConfirmScreen(
            imageUris = state.imageUris.map(Uri::parse),
            onAdd = viewModel::onAdd,
            onCancel = viewModel::onCancel,
        )
        else -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator()
        }
    }
}
