package minmul.memoir.feature.onboarding

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingPermissionRequestTest {
    @get:Rule
    val compose = createComposeRule()

    private val registry = FakePermissionRegistry()
    private val owner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = registry
    }
    private val permissions = arrayOf(
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
    )
    private var advances = 0

    @Test
    fun `later advances without requesting permissions`() {
        showRoute()

        compose.onNodeWithText(label(R.string.action_later)).performClick()

        compose.runOnIdle {
            assertEquals(1, advances)
            assertEquals(0, registry.launches)
        }
    }

    @Test
    fun `full access already granted advances without requesting permissions`() {
        showRoute(fullAccess = true)

        compose.onNodeWithText(label(R.string.action_request_permission)).performClick()

        compose.runOnIdle {
            assertEquals(1, advances)
            assertEquals(0, registry.launches)
        }
    }

    @Test
    fun `request waits for result and prevents duplicate requests`() {
        showRoute()

        compose.onNodeWithText(label(R.string.action_request_permission)).performClick()
        compose.onNodeWithText(label(R.string.action_request_permission)).assertIsNotEnabled()
            .performClick()
        compose.onNodeWithText(label(R.string.action_later)).assertIsNotEnabled().performClick()

        compose.runOnIdle {
            assertEquals(0, advances)
            assertEquals(1, registry.launches)
            assertArrayEquals(permissions, registry.requestedPermissions)
            registry.respond(emptyMap())
            registry.respond(emptyMap())
            assertEquals(1, advances)
        }
    }

    @Test
    fun `full grant advances`() = requestAndRespond(
        mapOf(permissions[0] to true, permissions[1] to true),
    )

    @Test
    fun `partial grant advances`() = requestAndRespond(
        mapOf(permissions[0] to false, permissions[1] to true),
    )

    @Test
    fun `denial advances`() = requestAndRespond(
        mapOf(permissions[0] to false, permissions[1] to false),
    )

    @Test
    fun `cancellation advances`() = requestAndRespond(emptyMap())

    @Test
    fun `partial access already granted still requests full access`() {
        // The full-access check is false even when selected-photo access exists.
        showRoute(fullAccess = false)

        compose.onNodeWithText(label(R.string.action_request_permission)).performClick()

        compose.runOnIdle {
            assertEquals(1, registry.launches)
            assertEquals(0, advances)
        }
    }

    @Test
    fun `pending request survives state restoration without relaunching`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MemoirTheme {
                    OnboardingPermissionRequestContent(
                        onContinue = { advances++ },
                        permissions = permissions,
                        hasFullPhotoAccess = { false },
                    )
                }
            }
        }
        compose.onNodeWithText(label(R.string.action_request_permission)).performClick()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText(label(R.string.action_request_permission)).assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, registry.launches)
            registry.respond(mapOf(permissions[0] to false))
            assertEquals(1, advances)
        }
    }

    private fun requestAndRespond(result: Map<String, Boolean>) {
        showRoute()
        compose.onNodeWithText(label(R.string.action_request_permission)).performClick()

        compose.runOnIdle {
            assertEquals(0, advances)
            registry.respond(result)
            assertEquals(1, advances)
        }
    }

    private fun showRoute(fullAccess: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MemoirTheme {
                    OnboardingPermissionRequestContent(
                        onContinue = { advances++ },
                        permissions = permissions,
                        hasFullPhotoAccess = { fullAccess },
                    )
                }
            }
        }
    }

    private fun label(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private class FakePermissionRegistry : ActivityResultRegistry() {
        var launches = 0
        var requestedPermissions: Array<String>? = null
        private var requestCode = 0

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            this.requestCode = requestCode
            launches++
            @Suppress("UNCHECKED_CAST")
            requestedPermissions = input as Array<String>
        }

        fun respond(result: Map<String, Boolean>) {
            dispatchResult(requestCode, result)
        }
    }
}
