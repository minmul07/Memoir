package minmul.memoir.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InformationCollectionPreferencesTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `missing information collection preference defaults to true`() = runTest {
        withRepository(File(directory, "default.preferences_pb")) { repository ->
            assertEquals(true, repository.informationCollectionEnabled.first())
        }
    }

    @Test
    fun `information collection preference survives reopening the preferences file`() = runTest {
        for (enabled in listOf(false, true)) {
            val file = File(directory, "collection-$enabled.preferences_pb")
            withRepository(file) { repository ->
                repository.setInformationCollectionEnabled(enabled)
                assertEquals(enabled, repository.informationCollectionEnabled.first())
            }

            withRepository(file) { repository ->
                assertEquals(enabled, repository.informationCollectionEnabled.first())
            }
        }
    }

    private suspend fun withRepository(
        file: File,
        block: suspend (UserPreferencesRepository) -> Unit,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            block(UserPreferencesRepository(PreferenceDataStoreFactory.create(scope = scope) { file }))
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }
}
