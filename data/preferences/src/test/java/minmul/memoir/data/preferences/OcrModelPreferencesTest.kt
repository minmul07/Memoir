package minmul.memoir.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.OcrModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OcrModelPreferencesTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `model choices survive reopening the preferences file`() = runTest {
        val file = File(directory, "selection.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = UserPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = firstScope) { file },
            )
            assertEquals(emptySet<OcrModel>(), repository.disabledOcrModels.first())
            repository.setOcrModelEnabled(OcrModel.Korean, false)
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = UserPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = secondScope) { file },
            )
            assertEquals(setOf(OcrModel.Korean), repository.disabledOcrModels.first())
        } finally {
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    @Test
    fun `concurrent changes preserve other models and reenabling only changes the target`() =
        runTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val repository = UserPreferencesRepository(
                    PreferenceDataStoreFactory.create(scope = scope) {
                        File(
                            directory,
                            "selection.preferences_pb"
                        )
                    },
                )
                coroutineScope {
                    launch { repository.setOcrModelEnabled(OcrModel.Korean, false) }
                    launch { repository.setOcrModelEnabled(OcrModel.Japanese, false) }
                }
                assertEquals(
                    setOf(OcrModel.Korean, OcrModel.Japanese),
                    repository.disabledOcrModels.first()
                )

                repository.setOcrModelEnabled(OcrModel.Korean, true)
                assertEquals(setOf(OcrModel.Japanese), repository.disabledOcrModels.first())
            } finally {
                scope.coroutineContext[Job]!!.cancelAndJoin()
            }
        }
}
