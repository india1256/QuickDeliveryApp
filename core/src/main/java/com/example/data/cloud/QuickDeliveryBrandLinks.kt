package com.example.data.cloud

import android.content.Intent
import android.net.Uri

/**
 * Centralized production brand domain, legal compliance URLs, and download links
 * shared across Quick Delivery Customer, Rider Partner, and Admin Enterprise apps.
 */
object QuickDeliveryBrandLinks {
    const val PRIMARY_DOMAIN = "quickdelivery.co.in"
    const val PUBLIC_BASE_URL = "https://quickdelivery.co.in"

    // Play Store & legal compliance URLs hosted on quickdelivery.co.in
    const val PRIVACY_POLICY_URL = "$PUBLIC_BASE_URL/privacy-policy"
    const val TERMS_OF_SERVICE_URL = "$PUBLIC_BASE_URL/terms"
    const val DELETE_ACCOUNT_URL = "$PUBLIC_BASE_URL/delete-account"

    // Standalone role download portals & API endpoints
    const val CUSTOMER_APK_URL = "$PUBLIC_BASE_URL/customer-apk"
    const val RIDER_APK_URL = "$PUBLIC_BASE_URL/rider-apk"
    const val ADMIN_APK_URL = "$PUBLIC_BASE_URL/admin-apk"
    const val ALL_IN_ONE_APK_URL = "$PUBLIC_BASE_URL/suite"
    const val LIVE_SYNC_API_URL = "$PUBLIC_BASE_URL/api/live-sync"
    const val APP_UPDATE_MANIFEST_URL = "$PUBLIC_BASE_URL/api/app-update-manifest"
    const val BACKUP_EXPORT_URL = "$PUBLIC_BASE_URL/api/backup-export"

    // Support & administrative contact emails
    const val SUPPORT_EMAIL = "support@quickdelivery.co.in"
    const val ADMIN_BRAND_EMAIL = "admin@quickdelivery.co.in"
    const val OWNER_CONTACT_EMAIL = "ravindergupta141@gmail.com"

    fun buildSupportMailtoIntent(subject: String, body: String): Intent {
        val mailUri = Uri.parse(
            "mailto:$SUPPORT_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )
        return Intent(Intent.ACTION_SENDTO, mailUri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
