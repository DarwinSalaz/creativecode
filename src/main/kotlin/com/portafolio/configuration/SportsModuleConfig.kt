package com.portafolio.configuration

import com.creativecode.sports.SportsConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/** The legacy application has no sports beans or connections unless explicitly enabled. */
@Configuration
@ConditionalOnProperty(name = ["sports.enabled"], havingValue = "true")
@Import(SportsConfiguration::class)
class SportsModuleConfig
