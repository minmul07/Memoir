package minmul.memoir.background.analysis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import minmul.memoir.core.ai.AnalysisLog
import javax.inject.Inject

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
        AnalysisLog.write("service start id=$startId requests=$requests active=${work != null}")
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
            AnalysisLog.write("service foreground type=$type")
        } catch (error: Exception) {
            AnalysisLog.write("service failure error=${error.javaClass.simpleName}")
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
                } catch (error: Exception) {
                    AnalysisLog.write("service failure error=${error.javaClass.simpleName}")
                    mutableFailed.value = true
                } finally {
                    AnalysisLog.write("service drain_finished stopId=$latestStartId")
                    work = null
                    stopSelfResult(latestStartId)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        AnalysisLog.write("service timeout id=$startId type=$fgsType")
        mutableFailed.value = true
        scope.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        AnalysisLog.write("service destroyed")
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "analysis"
        private val mutableFailed = MutableStateFlow(false)
        val failed = mutableFailed.asStateFlow()

        fun start(context: Context) {
            try {
                AnalysisLog.write("service start_requested")
                context.startForegroundService(Intent(context, AnalysisService::class.java))
            } catch (error: Exception) {
                AnalysisLog.write("service failure error=${error.javaClass.simpleName}")
                mutableFailed.value = true
            }
        }
    }
}
