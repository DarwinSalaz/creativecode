package com.portafolio.repositories

import com.portafolio.entities.PaymentSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentScheduleRepository : JpaRepository<PaymentSchedule, Long>