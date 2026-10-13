package com.portafolio.services

import com.portafolio.dtos.LiquidationPaymentsReportDto
import com.portafolio.dtos.LiquidationReportColumnDto
import com.portafolio.dtos.LiquidationReportRowDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp

@Service
class LiquidationPaymentsReportService(private val jdbcTemplate: JdbcTemplate) {

    fun generate(walletId: Int?, cashControlId: Long?): LiquidationPaymentsReportDto {
        require((walletId == null) != (cashControlId == null)) {
            "Debe enviar únicamente wallet_id o cash_control_id"
        }

        val liquidations = findLiquidations(walletId, cashControlId)
        val liquidationIds = liquidations.map { it.liquidationId }
        val rows = findRows(walletId, cashControlId)
        val values = findLiquidationValues(walletId, cashControlId, liquidationIds)

        return LiquidationPaymentsReportDto(
            scopeLabel = if (walletId != null) findWalletName(walletId) else "Liquidación #$cashControlId",
            walletId = walletId,
            cashControlId = cashControlId,
            liquidations = liquidations,
            rows = rows.map { row ->
                row.copy(liquidationValues = values[row.serviceId] ?: emptyMap())
            }
        )
    }

    private fun findLiquidations(walletId: Int?, cashControlId: Long?): List<LiquidationReportColumnDto> {
        val scopeSql = if (walletId != null) {
            "AND EXISTS (SELECT 1 FROM cash_movements m WHERE m.cash_control_id = l.cash_control_id AND m.wallet_id = ? AND m.cash_movement_type IN ('fee_payment', 'cancel_payment'))"
        } else {
            "AND l.cash_control_id = ?"
        }
        val parameter: Any = walletId ?: cashControlId!!

        return jdbcTemplate.query(
            """
            SELECT l.cash_movement_id, l.cash_control_id, l.created_at, l.value
            FROM cash_movements l
            WHERE l.cash_movement_type = 'final_liquidation' $scopeSql
            ORDER BY l.created_at ASC, l.cash_movement_id ASC
            """.trimIndent(),
            arrayOf(parameter)
        ) { rs, _ ->
            LiquidationReportColumnDto(
                liquidationId = rs.getLong("cash_movement_id"),
                cashControlId = rs.getLong("cash_control_id"),
                liquidatedAt = rs.getTimestamp("created_at").toLocalDateTime(),
                value = rs.getBigDecimal("value") ?: BigDecimal.ZERO
            )
        }
    }

    private fun findRows(walletId: Int?, cashControlId: Long?): List<LiquidationReportRowDto> {
        val scopeSql = if (walletId != null) {
            "s.wallet_id = ?"
        } else {
            "EXISTS (SELECT 1 FROM cash_movements scope_m WHERE scope_m.cash_control_id = ? AND scope_m.service_id = s.service_id AND scope_m.cash_movement_type IN ('fee_payment', 'cancel_payment'))"
        }
        val parameter: Any = walletId ?: cashControlId!!

        return jdbcTemplate.query(
            """
            SELECT s.service_id, s.wallet_id, s.created_at, s.fee_value, s.quantity_of_fees,
                   s.down_payment, s.debt, c.name, c.last_name, c.cellphone, c.address,
                   c.identification_number, COALESCE(s.observations, c.observation) AS observation,
                   COALESCE((SELECT STRING_AGG(p.name, '||| ' ORDER BY p.name)
                             FROM service_products sp
                             JOIN products p ON p.product_id = sp.product_id
                             WHERE sp.service_id = s.service_id AND (sp.enabled IS NULL OR sp.enabled = TRUE)), '') AS articles,
                   COALESCE((SELECT SUM(CASE WHEN cm.cash_movement_type = 'fee_payment' THEN pmt.value ELSE -pmt.value END)
                             FROM cash_movements cm
                             JOIN payments pmt ON pmt.payment_id = cm.payment_id
                             WHERE cm.service_id = s.service_id
                               AND cm.cash_movement_type IN ('fee_payment', 'cancel_payment')), 0) AS total_payments
            FROM services s
            JOIN customers c ON c.customer_id = s.customer_id
            WHERE $scopeSql
              AND s.state NOT IN ('canceled', 'deleted')
              AND s.deleted_at IS NULL
            ORDER BY s.created_at ASC, s.service_id ASC
            """.trimIndent(),
            arrayOf(parameter)
        ) { rs, _ ->
            LiquidationReportRowDto(
                serviceId = rs.getLong("service_id"),
                walletId = rs.getInt("wallet_id"),
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                name = rs.getString("name") ?: "",
                lastName = rs.getString("last_name"),
                cellphone = rs.getString("cellphone"),
                address = rs.getString("address"),
                identificationNumber = rs.getString("identification_number"),
                observation = rs.getString("observation"),
                articles = (rs.getString("articles") ?: "").split("||| ").filter { it.isNotBlank() },
                feeValue = rs.getBigDecimal("fee_value") ?: BigDecimal.ZERO,
                quantityOfFees = rs.getInt("quantity_of_fees"),
                totalPayments = rs.getBigDecimal("total_payments") ?: BigDecimal.ZERO,
                downPayment = rs.getBigDecimal("down_payment") ?: BigDecimal.ZERO,
                debt = rs.getBigDecimal("debt") ?: BigDecimal.ZERO,
                liquidationValues = emptyMap()
            )
        }
    }

    private fun findLiquidationValues(
        walletId: Int?,
        cashControlId: Long?,
        liquidationIds: List<Long>
    ): Map<Long, Map<Long, BigDecimal>> {
        if (liquidationIds.isEmpty()) return emptyMap()

        val scopeSql = if (walletId != null) "AND m.wallet_id = ?" else "AND m.cash_control_id = ?"
        val parameter: Any = walletId ?: cashControlId!!
        val placeholders = liquidationIds.joinToString(",") { "?" }
        val parameters = arrayListOf<Any>().apply {
            addAll(liquidationIds)
            add(parameter)
        }.toTypedArray()
        val values = mutableMapOf<Long, MutableMap<Long, BigDecimal>>()

        jdbcTemplate.query(
            """
            SELECT m.service_id, l.cash_movement_id AS liquidation_id,
                   SUM(CASE WHEN m.cash_movement_type = 'fee_payment' THEN p.value ELSE -p.value END) AS value
            FROM cash_movements l
            JOIN cash_movements m ON m.cash_control_id = l.cash_control_id AND m.created_at <= l.created_at
            JOIN payments p ON p.payment_id = m.payment_id
            WHERE l.cash_movement_id IN ($placeholders)
              AND m.cash_movement_type IN ('fee_payment', 'cancel_payment')
              $scopeSql
            GROUP BY m.service_id, l.cash_movement_id
            """.trimIndent(),
            parameters
        ) { rs ->
            val serviceId = rs.getLong("service_id")
            values.getOrPut(serviceId) { mutableMapOf() }[rs.getLong("liquidation_id")] =
                rs.getBigDecimal("value") ?: BigDecimal.ZERO
        }
        return values
    }

    private fun findWalletName(walletId: Int): String = jdbcTemplate.queryForObject(
        "SELECT name FROM wallets WHERE wallet_id = ?",
        arrayOf(walletId),
        String::class.java
    ) ?: "Cartera #$walletId"
}
