package com.portafolio.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Only the explicit sports profile moves the servlet to root. Public sales URLs stay identical. */
@Configuration
@ConditionalOnProperty(name=["sports.legacy-prefix"],havingValue="true")
class SportsRootRoutingConfig: WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix("/api/portfolio") { it.`package`?.name == "com.portafolio.controllers" }
    }
}
