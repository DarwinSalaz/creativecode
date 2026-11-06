package com.creativecode.sports.finance

import com.creativecode.sports.Actor
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.ResponseEntity
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("\${sports.api-path:/sports/v1}/finance")
class FinanceController(private val service:FinanceService) {
 private fun actor(r:HttpServletRequest)=r.getAttribute("sports.actor") as Actor
 @GetMapping fun overview(r:HttpServletRequest)=service.overview(actor(r))
 @GetMapping("/report") fun report(r:HttpServletRequest,@RequestParam month:String)=service.report(actor(r),month)
 @PostMapping("/days") fun day(r:HttpServletRequest,@RequestBody input:DayInput)=service.closeDay(actor(r),input)
 @PostMapping("/accounts") fun bank(r:HttpServletRequest,@RequestBody input:BankInput)=service.bank(actor(r),input)
 @PostMapping("/base") fun base(r:HttpServletRequest,@RequestBody input:BaseInput)=service.base(actor(r),input)
 @PostMapping("/handovers") fun handover(r:HttpServletRequest,@RequestBody input:HandoverInput)=service.handover(actor(r),input)
 @PostMapping("/handovers/{id}/accept") fun accept(r:HttpServletRequest,@PathVariable id:UUID,@RequestBody input:AcceptInput)=service.accept(actor(r),id,input)
 @PostMapping("/expenses") fun expense(r:HttpServletRequest,@RequestBody input:ExpenseInput)=service.expense(actor(r),input)
 @PostMapping("/months") fun month(r:HttpServletRequest,@RequestBody input:MonthInput)=service.closeMonth(actor(r),input)
 @PostMapping("/adjustments") fun adjustment(r:HttpServletRequest,@RequestBody input:AdjustmentInput)=service.adjustment(actor(r),input)
 @PostMapping("/receipts") fun upload(r:HttpServletRequest,@RequestParam file:MultipartFile)=mapOf("id" to service.upload(actor(r),file.originalFilename?:"comprobante",file.contentType?:"",file.bytes))
 @GetMapping("/receipts/{id}") fun receipt(r:HttpServletRequest,@PathVariable id:UUID):ResponseEntity<ByteArray>{val receipt=service.receipt(actor(r),id);return ResponseEntity.ok().header("Content-Type",receipt.contentType).header("Content-Disposition","attachment; filename=\"comprobante-${receipt.id}\"").header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body(receipt.content)}
}
