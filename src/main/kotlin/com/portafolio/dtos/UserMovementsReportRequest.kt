package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

data class UserMovementsReportRequest(
    
    @NotNull
    @JsonProperty("application_user_id")
    val applicationUserId: Long,
    
    @NotNull
    @JsonProperty("starts_at")
    val startsAt: LocalDateTime,
    
    @NotNull
    @JsonProperty("ends_at")
    val endsAt: LocalDateTime
)

