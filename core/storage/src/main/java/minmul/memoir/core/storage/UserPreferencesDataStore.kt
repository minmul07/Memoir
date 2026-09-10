package minmul.memoir.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

object UserPreferencesKeys {
    val DISABLED_OCR_MODELS = stringSetPreferencesKey("disabled_ocr_models")
    val SELECTED_GEMMA_MODEL = stringPreferencesKey("selected_gemma_model")
    val GEMMA_MAX_OUTPUT_TOKEN = intPreferencesKey("gemma_max_output_token")
    val GEMMA_TOP_K = intPreferencesKey("gemma_top_k")
    val GEMMA_TOP_P = doublePreferencesKey("gemma_top_p")
    val GEMMA_TEMPERATURE = doublePreferencesKey("gemma_temperature")
    val GEMMA_THINKING_ENABLED = booleanPreferencesKey("gemma_thinking_enabled")
    val GEMMA_SPECULATIVE_DECODING = booleanPreferencesKey("gemma_speculative_decoding")
    val ANALYSIS_QUEUE_MODE = stringPreferencesKey("analysis_queue_mode")
    val ONBOARDING_PROGRESS = intPreferencesKey("onboarding_progress")
}
