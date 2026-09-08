package minmul.memoir

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.feature.intake.IntakeRoute
import minmul.memoir.intake.ShareImageParser

@AndroidEntryPoint
class IntakeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val imageUris = ShareImageParser.parse(intent)
        setContent {
            MemoirTheme {
                IntakeRoute(
                    imageUris = imageUris,
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
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (openQueue) {
                    putExtra(MainActivity.EXTRA_OPEN_QUEUE, true)
                }
            },
        )
        finish()
    }
}
