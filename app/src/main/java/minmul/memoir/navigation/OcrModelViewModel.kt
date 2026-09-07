package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import minmul.memoir.core.ai.MultilingualOcrEngine
import javax.inject.Inject

enum class OcrModelState { Checking, Missing, Installing, Ready, Failed }

@HiltViewModel
class OcrModelViewModel @Inject constructor(private val ocr: MultilingualOcrEngine) : ViewModel() {
    private val mutableState = MutableStateFlow(OcrModelState.Checking)
    val state = mutableState.asStateFlow()
    init { check(false) }
    fun install() = check(true)
    private fun check(install: Boolean) {
        if (mutableState.value == OcrModelState.Installing) return
        viewModelScope.launch {
            mutableState.value = if (install) OcrModelState.Installing else OcrModelState.Checking
            try {
                if (install) ocr.install()
                mutableState.value = if (ocr.isAvailable()) OcrModelState.Ready else OcrModelState.Missing
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = OcrModelState.Failed }
        }
    }
}
