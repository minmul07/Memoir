package minmul.memoir.background.analysis

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class AnalysisService : Service() {
    @Inject lateinit var runner: AnalysisRunner
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var work: Job? = null
    private var latestStartId = 0
    private var requests = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        requests++
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            getString(R.string.analysis_channel), NotificationManager.IMPORTANCE_LOW))
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(getString(R.string.analysis_notification))
            .setContentText(getString(R.string.analysis_notification_progress))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .apply {
                if (launchIntent != null) setContentIntent(PendingIntent.getActivity(
                    this@AnalysisService, 0, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }.build()
        try {
            val type = if (Build.VERSION.SDK_INT >= 35)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            startForeground(1, notification, type)
        } catch (_: Exception) {
            mutableFailed.value = true
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        if (work == null) {
            mutableFailed.value = false
            work = scope.launch {
                try {
                    do {
                        val version = requests
                        runner.drain()
                    } while (version != requests)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableFailed.value = true
                } finally {
                    work = null
                    stopSelfResult(latestStartId)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        mutableFailed.value = true
        scope.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "analysis"
        private val mutableFailed = MutableStateFlow(false)
        val failed = mutableFailed.asStateFlow()

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, AnalysisService::class.java))
            } catch (_: Exception) {
                mutableFailed.value = true
            }
        }
    }
}
