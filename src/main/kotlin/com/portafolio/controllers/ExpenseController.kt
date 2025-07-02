package com.portafolio.controllers

import com.portafolio.dtos.ExpenseDto
import com.portafolio.dtos.ExpenseResumeDto
import com.portafolio.dtos.ExpenseListResponseDto
import com.portafolio.dtos.ExpenseDeleteResponseDto
import com.portafolio.mappers.ExpenseMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.ExpenseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE])
class ExpenseController {

    @Autowired
    lateinit var mapper: ExpenseMapper

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var service: ExpenseService

    @DeleteMapping("/expense/delete/{expense_id}")
    fun deleteExpense(@PathVariable("expense_id") expenseId: Long,
               @RequestHeader("Authorization") authorization: String): ResponseEntity<ExpenseDeleteResponseDto> {
        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val username = applicationUserService.verifyToken(token)

        val user = applicationUserRepository.findByUsername(username)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ExpenseDeleteResponseDto(false, "Usuario no autorizado"))

        val result = service.deleteExpense(expenseId)

        return if (result) {
            ResponseEntity.ok(ExpenseDeleteResponseDto(true, "Gasto eliminado exitosamente"))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ExpenseDeleteResponseDto(false, "No se pudo eliminar el gasto"))
        }
    }

    @GetMapping("/expense/list")
    fun listExpenses(
        @RequestParam("wallet_id", required = false) walletId: Int?,
        @RequestParam("start_date", required = false) startDateStr: String?,
        @RequestParam("end_date", required = false) endDateStr: String?,
        @RequestParam("expense_type", required = false) expenseType: String?,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<List<ExpenseListResponseDto>> {
        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val username = applicationUserService.verifyToken(token)

        val user = applicationUserRepository.findByUsername(username)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(emptyList())

        // Parse dates with error handling
        val startDate = try {
            startDateStr?.let { LocalDateTime.of(LocalDate.parse(it), LocalTime.MIN) }
        } catch (e: Exception) {
            null
        }
        
        val endDate = try {
            endDateStr?.let { LocalDateTime.of(LocalDate.parse(it), LocalTime.MAX) }
        } catch (e: Exception) {
            null
        }

        val expenses = service.getExpensesWithFilters(walletId, startDate, endDate, expenseType)

        val response = expenses.map { expense ->
            val expenseUsername = service.getUsernameByUserId(expense.applicationUserId)
            mapper.mapToListResponse(expense, expenseUsername)
        }

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/expense/{expense_id}")
    fun delete(@PathVariable("expense_id") expenseId: Long,
               @RequestHeader("Authorization") authorization: String): ResponseEntity<Any> {
        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val username = applicationUserService.verifyToken(token)

        val user = applicationUserRepository.findByUsername(username)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED")

        val result = service.deleteExpense(expenseId)

        return if (result) {
            ResponseEntity.status(HttpStatus.OK).body(null)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
        }
    }

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

        return expenses.map { mapper.mapReverse(it) }
    }

}