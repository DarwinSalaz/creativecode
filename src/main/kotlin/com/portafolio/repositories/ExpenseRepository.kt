package com.portafolio.repositories

import com.portafolio.entities.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ExpenseRepository: JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE e.applicationUserId = ?1")
    fun getExpenses(applicationUserId : Long) : List<Expense>?

}