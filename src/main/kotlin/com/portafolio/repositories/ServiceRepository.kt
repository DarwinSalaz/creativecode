package com.portafolio.repositories

import com.portafolio.entities.Service
import com.portafolio.models.PaymentReportInterface
import com.portafolio.models.ServiceReport
import com.portafolio.models.ServiceReportInt
import com.portafolio.models.ServiceSchedule
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
    "select s.service_id as id, \n" +
            " c.name || ' ' || c.last_name as client, \n" +
            " STRING_AGG( coalesce(p.name, ' '), ' \n ') as products,\n" +
            " s.service_value as product_values,\n" +
            " s.discount as discount,\n" +
            " s.total_value as service_value,\n" +
            " s.debt as debt,\n" +
            " w.name as wallet,\n" +
            " u.username as username,\n" +
            " s.created_at as created_at\n" +
            " from services s inner join service_products sp using (service_id)\n" +
            " inner join customers c using (customer_id)\n" +
            " inner join products p using (product_id)\n" +
            " inner join wallets w on (s.wallet_id = w.wallet_id)\n" +
            " inner join application_users u using (application_user_id)\n" +
            " where s.created_at between ?2 and ?3 and s.state != 'canceled'\n" +
            " and s.wallet_id = ?1\n" +
            " group by 1,2,4,5,6,7,8,9 order by s.created_at asc")
    fun reportService(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime) : List<ServiceReportInt>

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


}