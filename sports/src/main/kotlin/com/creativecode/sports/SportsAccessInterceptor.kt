package com.creativecode.sports

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.dao.DataAccessException
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class SportsAccessInterceptor(private val path: String, private val origins: Set<String>, private val json: ObjectMapper,
    private val authenticate: (String?) -> Actor): HandlerInterceptor {
    override fun preHandle(r: HttpServletRequest, s: HttpServletResponse, handler: Any): Boolean {
        s.setHeader("Cache-Control", "no-store")
        s.setHeader("X-Content-Type-Options", "nosniff")
        try {
            val origin=r.getHeader("Origin")
            val standardPort=(r.scheme=="https" && r.serverPort==443)||(r.scheme=="http" && r.serverPort==80)
            val sameOrigin=origin=="${r.scheme}://${r.serverName}${if(standardPort) "" else ":${r.serverPort}"}"
            checkSports(origin==null || sameOrigin || origin in origins,"ORIGIN_FORBIDDEN","Origen no autorizado",403)
            if(origin!=null && origin in origins){s.setHeader("Access-Control-Allow-Origin",origin);s.setHeader("Vary","Origin")}
            if(r.method=="OPTIONS"){
                s.setHeader("Access-Control-Allow-Methods","GET,POST,PUT,PATCH,OPTIONS")
                s.setHeader("Access-Control-Allow-Headers","Authorization,Content-Type")
                s.status=204;return false
            }
            // Only this exact POST endpoint is public. No client-supplied tenant/role is trusted.
            if(!(r.servletPath=="$path/auth/login" && r.method=="POST")) r.setAttribute("sports.actor",authenticate(r.getHeader("Authorization")))
            return true
        } catch(e: SportsError){return reject(s,e.statusCode,e.code,e.message)}
        catch(e: DataAccessException){return reject(s,503,"TEMPORARILY_UNAVAILABLE","No se pudo verificar la sesión. Reintenta en unos segundos.")}
    }
    private fun reject(s: HttpServletResponse,status: Int,code: String,message: String): Boolean {
        s.status=status;s.contentType="application/json";json.writeValue(s.outputStream,mapOf("code" to code,"message" to message));return false
    }
}
