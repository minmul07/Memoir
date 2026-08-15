package minmul.memoir.data.preferences

object OnboardingProgress {
    const val LANDING = 0
    const val PERMISSION = 1
    const val CRASHLYTICS = 2
    const val MODEL_SETUP = 3
    const val COMPLETED = 4

    fun normalize(progress: Int): Int = when {
        progress >= COMPLETED -> COMPLETED
        progress >= MODEL_SETUP -> MODEL_SETUP
        else -> LANDING
    }

    fun isComplete(progress: Int): Boolean = progress >= COMPLETED
}
