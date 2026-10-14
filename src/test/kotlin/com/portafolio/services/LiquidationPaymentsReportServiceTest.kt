package com.portafolio.services

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

class LiquidationPaymentsReportServiceTest {
    private val service = LiquidationPaymentsReportService(mock<JdbcTemplate>())

    @Test(expected = IllegalArgumentException::class)
    fun `requires one report scope`() {
        service.generate(null, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects multiple report scopes`() {
        service.generate(1, 2)
    }

    @Test
    fun `maps a closed cash control as a liquidation column`() {
        val resultSet = mock<ResultSet>()
        val closedAt = LocalDateTime.of(2026, 4, 30, 18, 45)
        whenever(resultSet.getLong("cash_control_id")).thenReturn(41L)
        whenever(resultSet.getTimestamp("liquidated_at")).thenReturn(Timestamp.valueOf(closedAt))
        whenever(resultSet.getBigDecimal("value")).thenReturn(BigDecimal("30000"))

        val result = service.mapLiquidationRow(resultSet)

        assertEquals(41L, result.liquidationId)
        assertEquals(41L, result.cashControlId)
        assertEquals(closedAt, result.liquidatedAt)
        assertEquals(BigDecimal("30000"), result.value)
    }
}
