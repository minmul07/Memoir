package minmul.memoir

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
    private val openQueueRequest = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
