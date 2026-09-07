package minmul.memoir

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import minmul.memoir.background.analysis.AnalysisService
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.navigation.RootNavDisplay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var contentRepository: ContentRepository
    private var notificationRequested = false
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private val openQueueRequest = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationRequested = savedInstanceState?.getBoolean("notificationRequested") ?: false
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                contentRepository.observeQueue().map { it.isNotEmpty() }.distinctUntilChanged().catch { emit(false) }.collect { hasWork ->
                    if (hasWork) {
                        requestAnalysisNotifications()
                        AnalysisService.start(this@MainActivity)
                    }
                }
            }
        }
        enableEdgeToEdge()
        applyOpenQueueExtra(intent)
        setContent {
            val openQueue by openQueueRequest.collectAsStateWithLifecycle()
            MemoirTheme {
                RootNavDisplay(
                    openQueue = openQueue,
                    onOpenQueueConsumed = { openQueueRequest.value = false },
                )
            }
        }
    }

    private fun requestAnalysisNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !notificationRequested &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationRequested = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("notificationRequested", notificationRequested)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyOpenQueueExtra(intent)
    }

    private fun applyOpenQueueExtra(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_OPEN_QUEUE, false)) {
            intent.removeExtra(EXTRA_OPEN_QUEUE)
            openQueueRequest.value = true
        }
    }

    companion object {
        const val EXTRA_OPEN_QUEUE = "minmul.memoir.extra.OPEN_QUEUE"
    }
}
