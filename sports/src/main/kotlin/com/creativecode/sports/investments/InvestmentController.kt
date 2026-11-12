package com.creativecode.sports.investments
import com.creativecode.sports.Actor
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("\${sports.api-path:/sports/v1}/investments")
class InvestmentController(private val service:InvestmentService) {
 @GetMapping fun overview(r:HttpServletRequest,@RequestParam(required=false) month:String?)=service.overview(r.getAttribute("sports.actor") as Actor,month)
 @PostMapping fun create(r:HttpServletRequest,@RequestBody input:InvestmentInput)=service.create(r.getAttribute("sports.actor") as Actor,input)
}
