package minmul.memoir.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Onboarding : NavKey

@Serializable
data object Main : NavKey

@Serializable
data object Archive : NavKey

@Serializable
data object AnalysisHistory : NavKey

@Serializable
data class ItemDetail(val itemId: String) : NavKey
