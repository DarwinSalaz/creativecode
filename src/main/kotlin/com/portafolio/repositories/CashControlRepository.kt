package com.portafolio.repositories

import com.portafolio.entities.CashControl
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface CashControlRepository: JpaRepository<CashControl, Long> {

    @Query("SELECT c FROM CashControl c WHERE c.applicationUserId = ?1 AND c.active = ?2")
    fun findActiveCashControlByUser(applicationUserId : Long, active : Boolean = true) : CashControl?

    @Modifying
    @Query("UPDATE CashControl c SET c.cash = ?1, c.revenues = ?2, c.expenses = ?3, c.servicesCount = ?4 where c.cashControlId = ?5")
    fun updateCashControlValues(cash : BigDecimal, revenues : BigDecimal, expenses : BigDecimal, servicesCount : Int, cashControlId: Long)

}