package org.readium.r2.testapp.history

import java.time.LocalDate

data class HistoryTableData(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dates: List<LocalDate>,
    val books: List<BookProgress>,
    val totalsByDate: Map<LocalDate, Double>,   // суммы страниц по датам
    val totalTimeByDate: Map<LocalDate, Double>, // суммы часов по датам
    val totalPagesSum: Double,                   // общая сумма страниц за период
    val totalHoursSum: Double                    // общая сумма часов за период
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