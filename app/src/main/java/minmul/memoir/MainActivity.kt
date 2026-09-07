package minmul.memoir

import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.navigation.RootNavDisplay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject lateinit var contentRepository: minmul.memoir.data.content.ContentRepository
    private var notificationRequested = false
    private val notificationPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }

    private val openQueueRequest = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationRequested = savedInstanceState?.getBoolean("notificationRequested") ?: false
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                contentRepository.observeQueue().map { it.isNotEmpty() }.distinctUntilChanged().catch { emit(false) }.collect { hasWork ->
                    if (hasWork) {
                        requestAnalysisNotifications()
                        minmul.memoir.background.analysis.AnalysisService.start(this@MainActivity)
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
        if (android.os.Build.VERSION.SDK_INT >= 33 && !notificationRequested &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationRequested = true
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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
