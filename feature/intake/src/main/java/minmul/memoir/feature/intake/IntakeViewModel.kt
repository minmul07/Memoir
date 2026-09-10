package minmul.memoir.feature.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import minmul.memoir.core.model.ItemSource
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.data.content.ImportedOriginal
import minmul.memoir.data.preferences.OnboardingProgress
import minmul.memoir.data.preferences.OnboardingProgressStore
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

data class IntakeDraft(
    val imageUri: String,
    val failed: Boolean,
)

sealed interface IntakeUiState {
    data object Loading : IntakeUiState
    data object OpenOnboarding : IntakeUiState
    data object OpenQueue : IntakeUiState
    data object Finish : IntakeUiState
    data class Confirm(
        val drafts: List<IntakeDraft>,
        val isSubmitting: Boolean,
    ) : IntakeUiState
}

@HiltViewModel
class IntakeViewModel @Inject constructor(
    private val onboardingProgressStore: OnboardingProgressStore,
    private val contentRepository: ContentRepository,
) : ViewModel() {
    private val receivedImageUris = MutableStateFlow<List<String>?>(null)
    private val userAction = MutableStateFlow<UserAction?>(null)
    private val drafts = MutableStateFlow<List<IntakeDraftState>>(emptyList())
    private val isSubmitting = MutableStateFlow(false)
    private var importJob: Job? = null
    private var submitJob: Job? = null
    private var importStarted = false
    private var committed = false
    private var source: ItemSource = ItemSource.Share

    private val onboardingProgress: StateFlow<Int?> = flow {
        onboardingProgressStore.normalizeOnboardingProgress()
        emitAll(onboardingProgressStore.onboardingProgress)
    }
        .map<Int, Int?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val uiState: StateFlow<IntakeUiState> = combine(
        onboardingProgress,
        receivedImageUris,
        drafts,
        isSubmitting,
        userAction,
    ) { progress, imageUris, draftStates, submitting, action ->
        when {
            progress == null || imageUris == null -> IntakeUiState.Loading
            !OnboardingProgress.isComplete(progress) -> IntakeUiState.OpenOnboarding
            imageUris.isEmpty() -> IntakeUiState.Finish
            action == UserAction.Add -> IntakeUiState.OpenQueue
            action == UserAction.Cancel -> IntakeUiState.Finish
            draftStates.size != imageUris.size -> IntakeUiState.Loading
            else -> IntakeUiState.Confirm(
                drafts = draftStates.map { it.toDraft() },
                isSubmitting = submitting,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = IntakeUiState.Loading,
    )

    init {
        viewModelScope.launch {
            combine(onboardingProgress, receivedImageUris) { progress, imageUris ->
                progress to imageUris
            }.collect { (progress, imageUris) ->
                if (
                    progress != null &&
                    imageUris != null &&
                    OnboardingProgress.isComplete(progress) &&
                    imageUris.isNotEmpty()
                ) {
                    startImportOnce(imageUris)
                }
            }
        }
    }

    fun start(imageUris: List<String>, source: ItemSource = ItemSource.Share) {
        if (receivedImageUris.compareAndSet(null, imageUris)) {
            this.source = source
        }
    }

    fun onAdd() {
        if (submitJob?.isActive == true) {
            return
        }
        submitJob = viewModelScope.launch {
            isSubmitting.value = true
            try {
                importJob?.join()
                val ready = drafts.value.mapNotNull { it.imported }
                if (ready.isEmpty()) {
                    return@launch
                }
                val itemSource = source
                repeat(2) { attempt ->
                    try {
                        contentRepository.enqueueImported(ready, itemSource)
                        committed = true
                        userAction.value = UserAction.Add
                        return@launch
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Throwable) {
                        if (attempt > 0) {
                            return@launch
                        }
                        delay(ENQUEUE_RETRY_DELAY_MS.milliseconds)
                    }
                }
            } finally {
                if (userAction.value != UserAction.Add) {
                    isSubmitting.value = false
                }
            }
        }
    }

    fun onCancel() {
        viewModelScope.launch {
            submitJob?.cancelAndJoin()
            importJob?.cancelAndJoin()
            withContext(NonCancellable) {
                contentRepository.discardOriginals(drafts.value.map { it.itemId })
            }
            userAction.value = UserAction.Cancel
        }
    }

    override fun onCleared() {
        val itemIds = drafts.value.map { it.itemId }
        val shouldDiscard = !committed && itemIds.isNotEmpty()
        super.onCleared()
        if (shouldDiscard) {
            contentRepository.discardOriginalsAsync(itemIds)
        }
    }

    private fun startImportOnce(imageUris: List<String>) {
        if (importStarted) {
            return
        }
        importStarted = true
        val initial = imageUris.map { imageUri ->
            IntakeDraftState(
                imageUri = imageUri,
                itemId = UUID.randomUUID().toString(),
            )
        }
        drafts.value = initial
        importJob = viewModelScope.launch {
            supervisorScope {
                initial.forEach { draft ->
                    launch { importDraft(draft) }
                }
            }
        }
    }

    private suspend fun importDraft(draft: IntakeDraftState) {
        try {
            val imported = contentRepository.importOriginal(draft.itemId, draft.imageUri)
            currentCoroutineContext().ensureActive()
            updateDraft(draft.itemId) { current -> current.copy(imported = imported) }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                contentRepository.discardOriginals(listOf(draft.itemId))
            }
            throw cancelled
        } catch (_: Throwable) {
            withContext(NonCancellable) {
                contentRepository.discardOriginals(listOf(draft.itemId))
            }
            updateDraft(draft.itemId) { current -> current.copy(failed = true) }
        }
    }

    private fun updateDraft(
        itemId: String,
        transform: (IntakeDraftState) -> IntakeDraftState,
    ) {
        drafts.update { current ->
            current.map { draft ->
                if (draft.itemId == itemId) transform(draft) else draft
            }
        }
    }

    private enum class UserAction {
        Add,
        Cancel,
    }

    private data class IntakeDraftState(
        val imageUri: String,
        val itemId: String,
        val imported: ImportedOriginal? = null,
        val failed: Boolean = false,
    ) {
        fun toDraft(): IntakeDraft = IntakeDraft(imageUri = imageUri, failed = failed)
    }

    private companion object {
        const val ENQUEUE_RETRY_DELAY_MS = 100L
    }
}
