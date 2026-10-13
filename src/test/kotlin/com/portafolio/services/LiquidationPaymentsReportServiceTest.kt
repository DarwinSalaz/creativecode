package com.portafolio.services

import org.junit.Test
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate

class LiquidationPaymentsReportServiceTest {
    private val service = LiquidationPaymentsReportService(mock(JdbcTemplate::class.java))

    @Test(expected = IllegalArgumentException::class)
    fun `requires one report scope`() {
        service.generate(null, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects multiple report scopes`() {
        service.generate(1, 2)
    }
}
