package com.portafolio.dtos

import javax.validation.constraints.NotNull

data class DeleteServiceRequest(
    @field:NotNull
    val serviceId: Long
) 