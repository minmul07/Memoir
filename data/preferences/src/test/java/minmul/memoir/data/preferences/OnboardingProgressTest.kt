package minmul.memoir.data.preferences

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OnboardingProgressTest {
    @Test
    fun `normalize resets progress before model setup to landing`() {
        assertEquals(
            OnboardingProgress.LANDING,
            OnboardingProgress.normalize(OnboardingProgress.LANDING)
        )
        assertEquals(
            OnboardingProgress.LANDING,
            OnboardingProgress.normalize(OnboardingProgress.PERMISSION)
        )
        assertEquals(
            OnboardingProgress.LANDING,
            OnboardingProgress.normalize(OnboardingProgress.CRASHLYTICS)
        )
    }

    @Test
    fun `normalize keeps model setup`() {
        assertEquals(
            OnboardingProgress.MODEL_SETUP,
            OnboardingProgress.normalize(OnboardingProgress.MODEL_SETUP),
        )
    }

    @Test
    fun `normalize keeps completed progress`() {
        assertEquals(
            OnboardingProgress.COMPLETED,
            OnboardingProgress.normalize(OnboardingProgress.COMPLETED),
        )
        assertEquals(
            OnboardingProgress.COMPLETED,
            OnboardingProgress.normalize(OnboardingProgress.COMPLETED + 3),
        )
    }

    @Test
    fun `isComplete is true only at completed or above`() {
        assertFalse(OnboardingProgress.isComplete(OnboardingProgress.MODEL_SETUP))
        assertTrue(OnboardingProgress.isComplete(OnboardingProgress.COMPLETED))
    }
}
