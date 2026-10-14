package com.portafolio.services

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.CashControl
import com.portafolio.mappers.CashControlMapper
import com.portafolio.repositories.CashControlRepository
import com.portafolio.repositories.CashMovementRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.springframework.aop.framework.ProxyFactory
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import javax.persistence.EntityManager

class CashControlServiceTransactionTest {
    @Test
    fun `daily cash control refresh runs inside an actual transaction`() {
        val entityManager: EntityManager = mock()
        val repository: CashControlRepository = mock()
        val movements: CashMovementRepository = mock()
        val cashControl = CashControl(cashControlId = 7, applicationUserId = 8,
            cash = BigDecimal.ZERO, revenues = BigDecimal.ZERO, expenses = BigDecimal.ZERO,
            commission = BigDecimal.ZERO, active = true, servicesCount = 0)
        whenever(repository.findActiveCashControlByUser(8)).thenReturn(cashControl)
        whenever(movements.findCashMovementsByCashControlId(7)).thenReturn(emptyList())
        doAnswer {
            assertTrue("refresh requires a transaction even when called internally",
                TransactionSynchronizationManager.isActualTransactionActive())
            null
        }.whenever(entityManager).refresh(cashControl)

        val target = CashControlService(entityManager)
        ReflectionTestUtils.setField(target, "repository", repository)
        ReflectionTestUtils.setField(target, "cashMovementRepository", movements)
        ReflectionTestUtils.setField(target, "mapper", CashControlMapper())
        target.utilities = Utilities()
        val manager = object : AbstractPlatformTransactionManager() {
            override fun doGetTransaction(): Any = Any()
            override fun doBegin(transaction: Any, definition: TransactionDefinition) {}
            override fun doCommit(status: DefaultTransactionStatus) {}
            override fun doRollback(status: DefaultTransactionStatus) {}
        }
        val factory = ProxyFactory(target)
        factory.isProxyTargetClass = true
        factory.addAdvice(TransactionInterceptor(manager, AnnotationTransactionAttributeSource()))
        val service = factory.proxy as CashControlService
        val user = ApplicationUser(8, 1, "test", "Test", password = "secret", userProfileId = 1, active = true)

        assertEquals(7L, service.getDailyCashControl(user).cashControlId)
        verify(entityManager).refresh(cashControl)
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive())
    }
}
