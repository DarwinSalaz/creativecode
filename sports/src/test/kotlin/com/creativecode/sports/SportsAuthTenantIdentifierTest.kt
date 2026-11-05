package com.creativecode.sports

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SportsAuthTenantIdentifierTest {
    @Test fun `accepts public aliases and legacy UUID identifiers`() {
        assertTrue(validTenantIdentifier("centenario"))
        assertTrue(validTenantIdentifier("06f3a3f7-36dd-406a-b299-08803b72a6d5"))
        assertFalse(validTenantIdentifier("Centenario con espacios"))
        assertFalse(validTenantIdentifier("a"))
    }
}
