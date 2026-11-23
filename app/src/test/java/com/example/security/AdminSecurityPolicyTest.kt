package com.example.security

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
}
