package minmul.memoir.feature.settings

import androidx.annotation.StringRes
import minmul.memoir.core.design.R

enum class SettingsDestination(
    @StringRes val labelRes: Int,
) {
    Root(R.string.nav_settings),
    ModelManagement(R.string.nav_model_management),
    DeveloperOptions(R.string.nav_developer_options),
}
