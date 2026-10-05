package minmul.memoir

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import minmul.memoir.data.preferences.InformationCollectionReadState
import minmul.memoir.data.preferences.InformationCollectionState
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MemoirApplication : Application() {
    @Inject
    lateinit var informationCollectionState: InformationCollectionState

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Timber.plant(Timber.DebugTree())
        }
        informationCollectionState.start(applicationScope)
        applicationScope.launch {
            when (val result = informationCollectionState.state.first {
                it !is InformationCollectionReadState.Loading
            }) {
                is InformationCollectionReadState.Ready -> Timber.tag("MemoirStartup")
                    .i("information_collection_enabled=%s", result.enabled)

                is InformationCollectionReadState.Failed -> Timber.tag("MemoirStartup")
                    .e(result.cause, "Failed to load information collection preference")

                InformationCollectionReadState.Loading -> Unit
            }
        }
    }
}
