package minmul.memoir

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class InformationCollectionStateTest {
    @Test
    fun `information collection remains unknown until startup load completes`() = runTest {
        val state = InformationCollectionState(FakeInformationCollectionPreferencesStore())
        assertNull(state.informationCollectionEnabled.value)

        launch { state.load() }
        assertNull(state.informationCollectionEnabled.value)
        advanceUntilIdle()

        assertEquals(true, state.informationCollectionEnabled.value)
    }

    @Test
    fun `startup loads the saved information collection preference`() = runTest {
        for (enabled in listOf(false, true)) {
            val state =
                InformationCollectionState(FakeInformationCollectionPreferencesStore(enabled))

            state.load()

            assertEquals(enabled, state.informationCollectionEnabled.value)
        }
    }

    @Test
    fun `preference changes do not change the startup snapshot`() = runTest {
        val preferences = FakeInformationCollectionPreferencesStore(false)
        val state = InformationCollectionState(preferences)
        state.load()

        preferences.setInformationCollectionEnabled(true)

        assertEquals(false, state.informationCollectionEnabled.value)
    }

    @Test
    fun `failed startup load keeps information collection unknown`() = runTest {
        val preferences = FakeInformationCollectionPreferencesStore().apply {
            informationCollectionEnabled = flow { throw IOException("Read failed") }
        }
        val state = InformationCollectionState(preferences)

        state.load()

        assertNull(state.informationCollectionEnabled.value)
    }

    @Test
    fun `cancelled startup load propagates cancellation`() {
        val preferences = FakeInformationCollectionPreferencesStore().apply {
            informationCollectionEnabled = flow { throw CancellationException("Cancelled") }
        }
        val state = InformationCollectionState(preferences)

        assertThrows(CancellationException::class.java) {
            runTest { state.load() }
        }
        assertNull(state.informationCollectionEnabled.value)
    }
}
