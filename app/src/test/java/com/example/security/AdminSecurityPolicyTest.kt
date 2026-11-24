package com.example.security

import com.example.data.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminSecurityPolicyTest {
    @Test
    fun `rejects short or weak Admin passwords and enforces letters plus digits`() {
        assertNotNull(AdminCredentialsManager.validatePassword("123456"))
        assertNotNull(AdminCredentialsManager.validatePassword("12345678"))
        assertNotNull(AdminCredentialsManager.validatePassword("admin123"))
        assertNotNull(AdminCredentialsManager.validatePassword("onlyletters"))
        assertNotNull(AdminCredentialsManager.validatePassword(" Quick@2026 "))

        assertNull(AdminCredentialsManager.validatePassword("Quick@2026"))
        assertNull(AdminCredentialsManager.validatePassword("StrongPass123"))
    }

    @Test
    fun `masks Customer phone to last 4 digits for Rider only after delivery or cancellation`() {
        val rawPhone = "9845012345"

        listOf(
            OrderStatus.SEARCHING_RIDER,
            OrderStatus.RIDER_ASSIGNED,
            OrderStatus.ARRIVED_AT_PICKUP,
            OrderStatus.PICKED_UP,
            OrderStatus.IN_TRANSIT
        ).forEach { liveStatus ->
            assertFalse(QuickDeliverySecurityGuard.isOrderTerminalForRiderPhoneMask(liveStatus))
            assertEquals(rawPhone, QuickDeliverySecurityGuard.maskCustomerPhoneForRider(rawPhone, liveStatus))
        }

        assertTrue(QuickDeliverySecurityGuard.isOrderTerminalForRiderPhoneMask(OrderStatus.DELIVERED))
        assertTrue(QuickDeliverySecurityGuard.isOrderTerminalForRiderPhoneMask(OrderStatus.CANCELLED))
        assertEquals("******2345", QuickDeliverySecurityGuard.maskCustomerPhoneForRider(rawPhone, OrderStatus.DELIVERED))
        assertEquals("******2345", QuickDeliverySecurityGuard.maskCustomerPhoneForRider("+91 98450-12345", OrderStatus.CANCELLED))
    }
}
