package org.readium.r2.testapp.history

import java.time.LocalDate

data class HistoryTableData(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dates: List<LocalDate>,
    val books: List<BookProgress>,
    val totals: Map<LocalDate, Int>,
    val totalTime: Map<LocalDate, Double>
)

data class BookProgress(
    val bookId: Long,
    val title: String,
    val author: String,
    val status: String,
    val category: String,
    val dailyProgress: Map<LocalDate, Int>,
    val dailyTime: Map<LocalDate, Double>
)