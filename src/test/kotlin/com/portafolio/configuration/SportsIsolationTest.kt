package com.portafolio.configuration

import com.creativecode.sports.SportsRuntime
import com.portafolio.controllers.LegacyRoutingProbe
import org.junit.Test
import org.junit.Assert.*
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.MapPropertySource
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping

class SportsIsolationTest {
    @Test fun `sports disabled requires no credentials and creates no pool`() {
        ApplicationContextRunner().withUserConfiguration(SportsModuleConfig::class.java).run { context ->
            assertNull(context.startupFailure)
            assertEquals(0,context.getBeansOfType(SportsRuntime::class.java).size)
            assertFalse(context.containsBean("sportsController"))
        }
    }
    @Test fun `opt in root routing preserves public sales URLs and separates sports`() {
        val context=AnnotationConfigWebApplicationContext()
        context.servletContext=MockServletContext()
        context.environment.propertySources.addFirst(MapPropertySource("test",mapOf("sports.legacy-prefix" to "true")))
        context.register(RoutingConfig::class.java,SportsRootRoutingConfig::class.java)
        context.refresh()
        try {
            val mvc=MockMvcBuilders.webAppContextSetup(context).build()
            mvc.perform(get("/api/portfolio/routing-probe")).andExpect(status().isOk).andExpect(content().string("sales"))
            mvc.perform(get("/api/sports/v1/routing-probe")).andExpect(status().isOk).andExpect(content().string("sports"))
            mvc.perform(get("/routing-probe")).andExpect(status().isNotFound)
            mvc.perform(get("/api/portfolio/api/sports/v1/routing-probe")).andExpect(status().isNotFound)
        } finally {context.close()}
    }
    @Configuration @EnableWebMvc
    open class RoutingConfig {
        @Bean open fun legacy()=LegacyRoutingProbe()
        @Bean open fun sports()=SportsProbe()
    }
    @RestController
    class SportsProbe {
        @GetMapping("/api/sports/v1/routing-probe") fun probe()="sports"
    }
}
