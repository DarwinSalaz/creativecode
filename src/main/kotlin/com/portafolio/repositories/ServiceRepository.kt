package com.portafolio.repositories

import com.portafolio.entities.Service
import com.portafolio.models.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface ServiceRepository: JpaRepository<Service, Long> {

    @Query("SELECT s FROM Service s WHERE s.customerId = ?1 and s.state in ('created', 'paying')")
    fun findAllServicesByUser(customerId : Long) : List<Service>?

    @Modifying
    @Query("UPDATE Service s SET s.debt = s.debt - ?1, s.nextPaymentDate = ?2 where s.serviceId = ?3")
    fun updateDebtService(value : BigDecimal, nextPaymentDate: LocalDateTime?, serviceId: Long)

    @Query("SELECT new com.portafolio.models.ServiceSchedule(c.customerId, c.name, c.lastName, c.icon, s.feeValue, s.nextPaymentDate, s.hasExpiredPayment) FROM Service s INNER JOIN Customer c ON (s.customerId = c.customerId) WHERE c.walletId in ?1 and s.state in ('created', 'paying')")
    fun findServicesSchedule(walletIds: List<Int>) : List<ServiceSchedule>?

    @Query("SELECT new com.portafolio.models.ServiceSchedule(c.customerId, c.name, c.lastName, c.icon, s.feeValue, s.nextPaymentDate, s.hasExpiredPayment) FROM Service s INNER JOIN Customer c ON (s.customerId = c.customerId) WHERE s.state in ('created', 'paying')")
    fun findServicesSchedule() : List<ServiceSchedule>?

    @Modifying
    @Query("UPDATE Service s SET s.nextPaymentDate = ?1 where s.serviceId = ?2")
    fun updateNextPaymentDateService(nextPaymentDate: LocalDateTime?, serviceId: Long)

    @Query(nativeQuery = true, value =
    "SELECT s.service_id AS id, \n" +
            "       c.name || ' ' || c.last_name AS client, \n" +
            "       STRING_AGG(p.name || ' x' || sp.quantity, ' \n ') AS products,\n" +
            "       s.service_value AS product_values,\n" +
            "       s.discount AS discount,\n" +
            "       s.total_value AS service_value,\n" +
            "       s.debt AS debt,\n" +
            "       w.name AS wallet,\n" +
            "       u.username AS username,\n" +
            "       s.created_at AS created_at\n" +
            "  FROM services s\n" +
            "       INNER JOIN service_products sp USING (service_id)\n" +
            "       INNER JOIN customers c USING (customer_id)\n" +
            "       INNER JOIN products p USING (product_id)\n" +
            "       INNER JOIN wallets w ON (s.wallet_id = w.wallet_id)\n" +
            "       INNER JOIN application_users u USING (application_user_id)\n" +
            " WHERE s.created_at BETWEEN ?2 AND ?3\n" +
            "   AND s.state != 'canceled'\n" +
            "   AND s.wallet_id = ?1\n" +
            " GROUP BY s.service_id, client, s.service_value, s.discount, s.total_value, s.debt, w.name, u.username, s.created_at\n" +
            " ORDER BY s.created_at ASC")
    fun reportService(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime): List<ServiceReportInt>


    @Query(nativeQuery = true, value =
    "select \n" +
            "p.payment_id as id, \n" +
            "c.name || ' ' || c.last_name as client,\n" +
            "s.service_id as service_id,\n" +
            "p.value as value,\n" +
            "w.name as wallet,\n" +
            "u.username as username,\n" +
            "p.created_at\n" +
            "from payments p \n" +
            "inner join services s using (service_id)\n" +
            "inner join customers c using (customer_id)\n" +
            "inner join wallets w on (s.wallet_id = w.wallet_id)\n" +
            "inner join application_users u on (p.application_user_id = u.application_user_id)\n" +
            "where s.wallet_id = ?1 and p.status != 'canceled' and s.state != 'canceled' and\n" +
            "p.created_at between ?2 and ?3")
    fun reportPayments(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime) : List<PaymentReportInterface>


    @Query(nativeQuery = true, value =
    "select c.name || ' ' || c.last_name as client,\n" +
            "c.cellphone, c.address,\n" +
            "s.total_value, s.debt as debt, \n" +
            "s.pending_fees, next_payment_date\n" +
            "from services s inner join customers c \n" +
            "on s.customer_id = c.customer_id\n" +
            "where s.wallet_id = ?1 and state != 'fully_paid' \n" +
            "and state != 'canceled' and next_payment_date < CURRENT_DATE\n" +
            "and s.created_at between ?2 and ?3")
    fun reportExpiredServices(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime) : List<ExpiredServiceReportInterface>
}