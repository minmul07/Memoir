package minmul.memoir.feature.onboarding

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class PhotoPermissionsTest {
    @Test
    fun `requests only photo permissions supported by the Android version`() {
        val expectedBySdk = mapOf(
            30 to arrayOf("android.permission.READ_EXTERNAL_STORAGE"),
            32 to arrayOf("android.permission.READ_EXTERNAL_STORAGE"),
            33 to arrayOf("android.permission.READ_MEDIA_IMAGES"),
            34 to arrayOf(
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
            ),
            37 to arrayOf(
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
            ),
        )

        expectedBySdk.forEach { (sdk, permissions) ->
            assertArrayEquals(permissions, photoPermissionsForSdk(sdk), "API $sdk")
        }
    }
}
