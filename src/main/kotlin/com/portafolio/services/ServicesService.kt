package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.entities.Payment
import com.portafolio.entities.Service
import com.portafolio.repositories.ServiceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

@org.springframework.stereotype.Service
class ServicesService {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: ServiceRepository

    @Autowired
    private lateinit var cashControlService: CashControlService

    fun save(service: Service) : Service {

        //validate if the user has an active cash control
        val activeCashControl : CashControl? = cashControlService.findActiveCashControlByUser(service.applicationUserId)

        if(activeCashControl == null) {
            val cashControl = CashControl(
                applicationUserId = service.applicationUserId,
                active = true,
                cash = service.downPayment,
                expenses = BigDecimal.ZERO,
                revenues = service.downPayment,
                startsDate = LocalDateTime.now(),
                servicesCount = 1
            )

            cashControlService.save(cashControl)
        } else {
            cashControlService.updateValueForNewService(activeCashControl, service.downPayment)
        }

        return repository.save(service)
    }

    fun findAllServicesByUser(customerId: Long?) : List<Service> {
        if(customerId == null) return listOf()

        val services : List<Service>? = repository.findAllServicesByUser(customerId)

        return if( services.isNullOrEmpty() ) listOf() else services
    }
}