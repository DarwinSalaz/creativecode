package com.creativecode.sports

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Test
import org.junit.Assert.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class SportsAccessTest {
    private val a=Actor(UUID.randomUUID(),UUID.randomUUID(),"Empleado",Role.EMPLOYEE)
    private var calls=0
    private val interceptor=SportsAccessInterceptor("/sports/v1",setOf("https://sports.example"),jacksonObjectMapper()) {
        calls++; if(it!="Bearer sports-session") throw SportsError("UNAUTHORIZED","Inicia sesión",401); a
    }
    private fun request(path: String="/sports/v1/courts",method: String="GET",token: String?=null): MockHttpServletRequest {
        val r=MockHttpServletRequest(method,path);r.servletPath=path;if(token!=null)r.addHeader("Authorization",token);return r
    }
    @Test fun `missing token and legacy token denied`() {
        for(token in listOf(null,"Bearer sales-jwt")){val response=MockHttpServletResponse();assertFalse(interceptor.preHandle(request(token=token),response,Any()));assertEquals(401,response.status)}
    }
    @Test fun `public login is exact POST only`() {
        assertTrue(interceptor.preHandle(request("/sports/v1/auth/login","POST"),MockHttpServletResponse(),Any()));assertEquals(0,calls)
        val response=MockHttpServletResponse();assertFalse(interceptor.preHandle(request("/sports/v1/auth/login","GET"),response,Any()));assertEquals(401,response.status)
    }
    @Test fun `actor supplied only by verified session`() {
        val r=request(token="Bearer sports-session");r.addParameter("tenantId",UUID.randomUUID().toString());r.addHeader("X-Role","ADMIN")
        assertTrue(interceptor.preHandle(r,MockHttpServletResponse(),Any()));assertEquals(a,r.getAttribute("sports.actor"))
    }
    @Test fun `untrusted cross origin rejected before authentication`() {
        val r=request(token="Bearer sports-session");r.addHeader("Origin","https://evil.example")
        val response=MockHttpServletResponse();assertFalse(interceptor.preHandle(r,response,Any()));assertEquals(403,response.status);assertEquals(0,calls)
    }
    @Test fun `allowed preflight does not access authentication or database`() {
        val r=request(method="OPTIONS");r.addHeader("Origin","https://sports.example")
        val response=MockHttpServletResponse();assertFalse(interceptor.preHandle(r,response,Any()));assertEquals(204,response.status);assertEquals(0,calls);assertEquals("https://sports.example",response.getHeader("Access-Control-Allow-Origin"))
    }
    @Test fun `authenticated responses must not be cached`() {
        val response=MockHttpServletResponse();assertTrue(interceptor.preHandle(request(token="Bearer sports-session"),response,Any()));assertEquals("no-store",response.getHeader("Cache-Control"))
    }
}
