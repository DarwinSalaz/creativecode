package com.portafolio.controllers

import com.portafolio.dtos.ExpenseDto
import com.portafolio.dtos.ExpenseResumeDto
import com.portafolio.mappers.ExpenseMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.ExpenseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class ExpenseController {

    @Autowired
    lateinit var mapper: ExpenseMapper

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var service: ExpenseService

    @PostMapping("/expense/create")
    fun create(@Valid @RequestBody expenseDto : ExpenseDto,
               @RequestHeader("Authorization") authorization: String): ResponseEntity<Any> {
        val username: String = if (expenseDto.username.isNullOrEmpty()) {
            val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            applicationUserService.verifyToken(token)
        } else {
            expenseDto.username!!
        }

        val user = applicationUserRepository.findByUsername(username)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED")

        expenseDto.applicationUserId = user.applicationUserId

        val expenseSaved = service.save(mapper.map(expenseDto))

        return ResponseEntity.status(HttpStatus.OK).body(expenseSaved)
    }

    @GetMapping("/expenses-by-control/{cash_control_id}")
    fun getExpenses(@PathVariable("cash_control_id") cashControlId: Long) : List<ExpenseResumeDto>{
        //val user = applicationUserRepository.findByUsername(username)
        //val expenses = service.getExpenses(user!!.applicationUserId)
        val expenses = service.getExpensesByControlId(cashControlId)

        return expenses.map { mapper.mapReverse(it) } ?: emptyList()
    }

}