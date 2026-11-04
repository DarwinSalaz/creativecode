package com.portafolio.controllers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
@RestController
class LegacyRoutingProbe {
    @GetMapping("/routing-probe") fun probe()="sales"
}
