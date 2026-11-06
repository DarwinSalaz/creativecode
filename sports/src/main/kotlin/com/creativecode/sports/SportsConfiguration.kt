package com.creativecode.sports

import com.creativecode.sports.finance.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.ResponseEntity
import org.springframework.dao.DataAccessException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/** Owns its pool privately: never registers a DataSource bean that could replace sales auto-configuration. */
class SportsRuntime(env: Environment, json: ObjectMapper): AutoCloseable {
    private val pool = HikariDataSource(HikariConfig().apply {
        jdbcUrl = env.getRequiredProperty("sports.datasource.url")
        username = env.getRequiredProperty("sports.datasource.username")
        password = env.getRequiredProperty("sports.datasource.password")
        poolName = "sports-only"
        maximumPoolSize = 2; minimumIdle = 0; connectionTimeout = 5000; initializationFailTimeout = -1
    })
    val store = PostgresSportsStore(pool,json)
    val auth = SportsAuth(store)
    val service = BookingService(store)
    val finance = FinanceService(PostgresFinanceStore(store,json),json)
    override fun close() = pool.close()
}
@Configuration
class SportsConfiguration {
    @Bean(destroyMethod="close") fun sportsRuntime(env: Environment, json: ObjectMapper) = SportsRuntime(env,json)
    @Bean fun sportsController(runtime: SportsRuntime) = SportsController(runtime.store,runtime.service,runtime.auth)
    @Bean fun financeController(runtime: SportsRuntime) = FinanceController(runtime.finance)
    @Bean fun sportsErrors() = SportsErrors()
    @Bean fun sportsWebConfiguration(runtime: SportsRuntime, env: Environment, json: ObjectMapper): WebMvcConfigurer = object: WebMvcConfigurer {
        override fun addCorsMappings(registry: org.springframework.web.servlet.config.annotation.CorsRegistry) {
            val origins=env.getProperty("sports.allowed-origins", "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if(origins.isNotEmpty()) registry.addMapping(env.getProperty("sports.api-path", "/sports/v1") + "/**")
                .allowedOrigins(*origins.toTypedArray()).allowedMethods("GET", "POST", "PUT", "PATCH", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type").allowCredentials(false).maxAge(600)
        }
        override fun addInterceptors(registry: InterceptorRegistry) {
            val path=env.getProperty("sports.api-path","/sports/v1")
            val origins=env.getProperty("sports.allowed-origins","").split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            registry.addInterceptor(SportsAccessInterceptor(path, origins, json) { runtime.auth.authenticate(it) }).addPathPatterns("$path/**")
        }
    }
}
@RestControllerAdvice(assignableTypes=[SportsController::class,FinanceController::class])
class SportsErrors {
    private val log=org.slf4j.LoggerFactory.getLogger(SportsErrors::class.java)
    @ExceptionHandler(SportsError::class) fun domain(e: SportsError) = ResponseEntity.status(e.statusCode).body(mapOf("code" to e.code,"message" to e.message))
    @ExceptionHandler(HttpMessageNotReadableException::class,MethodArgumentTypeMismatchException::class,IllegalArgumentException::class,java.time.DateTimeException::class)
    fun badInput(e: Exception) = ResponseEntity.badRequest().body(mapOf("code" to "INVALID_INPUT","message" to "Revisa los datos enviados"))
    @ExceptionHandler(DataAccessException::class) fun database(e: DataAccessException): ResponseEntity<Map<String,String>> {
        log.warn("Sports persistence failure: {}", e.javaClass.simpleName)
        val conflict=e is org.springframework.dao.DataIntegrityViolationException || e is org.springframework.dao.ConcurrencyFailureException
        return ResponseEntity.status(if(conflict) 409 else 503).body(mapOf("code" to if(conflict) "CONFLICT" else "TEMPORARILY_UNAVAILABLE","message" to if(conflict) "La operación entra en conflicto con los datos actuales" else "No pudimos confirmar la operación. Actualiza y reintenta."))
    }
}
