package minmul.memoir.data.model

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.GemmaModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreGemmaDownloadIdStoreTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `download ids survive reopening the preferences file`() = runTest {
        val file = File(directory, "gemma_downloads.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = DataStoreGemmaDownloadIdStore(
                PreferenceDataStoreFactory.create(scope = firstScope) { file },
            )
            assertNull(store.get(GemmaModel.E4B))
            store.set(GemmaModel.E4B, 42L)
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = DataStoreGemmaDownloadIdStore(
                PreferenceDataStoreFactory.create(scope = secondScope) { file },
            )
            assertEquals(42L, store.get(GemmaModel.E4B))
            assertNull(store.get(GemmaModel.E2B))
        } finally {
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }
}
