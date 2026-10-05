package minmul.memoir.data.preferences

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class InformationCollectionStateTest {
    @Test
    fun `state stays unknown until the saved preference is read`() = runTest {
        for (enabled in listOf(false, true)) {
            val state = InformationCollectionState(FakeStore(enabled))
            assertSame(InformationCollectionReadState.Loading, state.state.value)

            state.start(backgroundScope)
            assertNull(state.state.value.enabled)
            runCurrent()

            assertEquals(InformationCollectionReadState.Ready(enabled), state.state.value)
        }
    }

    @Test
    fun `external preference changes update shared state without subscribers`() = runTest {
        val store = FakeStore(false)
        val state = InformationCollectionState(store)
        state.start(backgroundScope)
        runCurrent()
        assertEquals(false, state.state.value.enabled)

        store.setInformationCollectionEnabled(true)
        runCurrent()

        assertEquals(true, state.state.value.enabled)
    }

    @Test
    fun `starting repeatedly observes preferences only once`() = runTest {
        val store = FakeStore(false)
        var subscriptions = 0
        store.informationCollectionEnabled = flow {
            subscriptions++
            emitAll(store.savedEnabled)
        }
        val state = InformationCollectionState(store)
        state.start(backgroundScope)
        state.start(backgroundScope)
        runCurrent()
        state.start(backgroundScope)
        runCurrent()

        assertEquals(1, subscriptions)
        assertEquals(false, state.state.value.enabled)
    }

    @Test
    fun `read failure retains its cause and does not retry`() = runTest {
        val error = IOException("Read failed")
        val store = FakeStore()
        var subscriptions = 0
        store.informationCollectionEnabled = flow {
            subscriptions++
            throw error
        }
        val state = InformationCollectionState(store)
        state.start(backgroundScope)
        runCurrent()
        state.start(backgroundScope)
        runCurrent()

        assertNull(state.state.value.enabled)
        assertSame(error, (state.state.value as InformationCollectionReadState.Failed).cause)
        assertEquals(1, subscriptions)
    }

    @Test
    fun `flow cancellation is not reported as a read failure`() = runTest {
        val store = FakeStore()
        store.informationCollectionEnabled = flow { throw CancellationException("Cancelled") }
        val state = InformationCollectionState(store)
        state.start(backgroundScope)
        runCurrent()

        assertSame(InformationCollectionReadState.Loading, state.state.value)
    }

    @Test
    fun `application scope cancellation stops observing preferences`() = runTest {
        val store = FakeStore(false)
        val state = InformationCollectionState(store)
        state.start(backgroundScope)
        runCurrent()
        backgroundScope.cancel()
        runCurrent()

        store.setInformationCollectionEnabled(true)
        runCurrent()

        assertEquals(InformationCollectionReadState.Ready(false), state.state.value)
    }

    private class FakeStore(enabled: Boolean = true) : InformationCollectionPreferencesStore {
        val savedEnabled = MutableStateFlow(enabled)
        override var informationCollectionEnabled: Flow<Boolean> = savedEnabled
        override suspend fun setInformationCollectionEnabled(enabled: Boolean) {
            savedEnabled.value = enabled
        }
    }
}
