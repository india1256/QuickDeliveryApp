package com.example

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.core.R as CoreR
import com.example.data.cloud.QuickDeliveryFirebaseInitializer
import com.example.data.cloud.QuickDeliveryOperationsManager
import com.example.data.cloud.ServerAppUpdateManifest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartupLaunchRegressionTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun customerMainActivity_launchesWithoutCrashing() {
        ActivityScenario.launch(CustomerMainActivity::class.java).use { scenario ->
            composeRule.waitForIdle()
            scenario.onActivity { assertNotNull(it) }
        }
    }

    @Test
    fun riderMainActivity_launchesWithoutCrashing() {
        ActivityScenario.launch(RiderMainActivity::class.java).use { scenario ->
            composeRule.waitForIdle()
            scenario.onActivity { assertNotNull(it) }
        }
    }

    @Test
    fun adminMainActivity_launchesWithoutCrashing() {
        ActivityScenario.launch(AdminMainActivity::class.java).use { scenario ->
            composeRule.waitForIdle()
            scenario.onActivity { assertNotNull(it) }
        }
    }

    @Test
    fun mainActivity_launchesWithoutCrashing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitForIdle()
            scenario.onActivity { assertNotNull(it) }
        }
    }

    @Test
    fun firebaseConfigAndLogoResources_resolveNonZeroOnStartup() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertNotEquals(0, CoreR.string.firestore_database_id)
        assertNotEquals(0, CoreR.string.firebase_web_client_id)
        assertNotEquals(0, CoreR.drawable.quick_delivery_exact_logo_1791992953688)
        assertEquals(
            QuickDeliveryFirebaseInitializer.EXPECTED_DATABASE_ID,
            ctx.getString(CoreR.string.firestore_database_id)
        )
        assertTrue(QuickDeliveryFirebaseInitializer.resolveWebClientId(ctx).isNotBlank())
    }

    @Test
    fun serverAppUpdateManifest_detectsNewerBuildAndResolvesRoleUrls() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val manifestJson = JSONObject(
            """
            {
              "ok": true,
              "latestVersionCode": 86,
              "latestVersionName": "6.7.3",
              "forceUpdateRequired": true,
              "updateNotes": "Mandatory release",
              "urls": {
                "CUSTOMER": "https://quickdelivery.co.in/customer-apk",
                "RIDER": "https://quickdelivery.co.in/rider-apk",
                "ADMIN": "https://quickdelivery.co.in/admin-apk",
                "UNIFIED": "https://quickdelivery.co.in/suite"
              }
            }
            """.trimIndent()
        )
        val parsed: ServerAppUpdateManifest? = QuickDeliveryOperationsManager.updateServerAppManifest(ctx, manifestJson)
        assertNotNull(parsed)
        assertTrue(parsed!!.isUpdateAvailableForApp("6.7.2", 85))
        assertFalse(parsed.isUpdateAvailableForApp("6.7.3", 86))
        assertEquals("https://quickdelivery.co.in/admin-apk", parsed.resolveDownloadUrl("ADMIN"))
        assertEquals("https://quickdelivery.co.in/rider-apk", parsed.resolveDownloadUrl("RIDER"))
    }
}
