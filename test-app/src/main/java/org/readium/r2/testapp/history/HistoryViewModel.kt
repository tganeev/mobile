package org.readium.r2.testapp.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App
import org.readium.r2.testapp.data.model.Book
import org.readium.r2.testapp.data.model.ReadingStat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App

    private val _tableData = MutableStateFlow<HistoryTableData?>(null)
    val tableData: StateFlow<HistoryTableData?> = _tableData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    // Исправлено: используем forLanguageTag вместо устаревшего конструктора
    private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"))

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val yearMonth = _currentMonth.value
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()

                val data = loadHistoryData(startDate, endDate)
                _tableData.value = data
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Ошибка загрузки данных"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadHistoryData(startDate: LocalDate, endDate: LocalDate): HistoryTableData {
        // Получаем все книги
        val books = app.bookRepository.books().first()

        // Получаем всю статистику
        val allStats = app.bookRepository.getAllReadingStats().first()

        // Фильтруем статистику за период
        val statsInRange = allStats.filter { stat ->
            stat.date in startDate..endDate
        }

        // Группируем статистику по книгам и датам
        val statsByBook = statsInRange.groupBy { it.bookId }

        // Формируем список дат периода
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate
        while (currentDate <= endDate) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        // Формируем данные по каждой книге
        val bookProgressList = books.mapNotNull { book ->
            val bookId = book.id ?: return@mapNotNull null

            // Определяем статус книги
            val statusText = when {
                book.pagesRead > 0 && book.pagesRead >= (book.totalPages ?: Int.MAX_VALUE) -> "Завершено"
                book.pagesRead > 0 -> "В процессе"
                else -> "В плане"
            }

            val bookStats = statsByBook[bookId] ?: emptyList()

            val dailyProgress = dates.associateWith { date ->
                bookStats.find { it.date == date }?.pagesRead ?: 0
            }

            BookProgress(
                bookId = bookId,
                title = book.title ?: "Без названия",
                author = book.author ?: "",
                status = statusText,
                category = "0.0",
                dailyProgress = dailyProgress
            )
        }.sortedBy { it.title }

        // Рассчитываем итоги по дням
        val totals = dates.associateWith { date ->
            bookProgressList.sumOf { it.dailyProgress[date] ?: 0 }
        }

        return HistoryTableData(
            periodStart = startDate,
            periodEnd = endDate,
            dates = dates,
            books = bookProgressList,
            totals = totals
        )
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        loadData()
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        loadData()
    }

    fun getFormattedMonth(): String {
        val formatted = _currentMonth.value.format(monthFormatter)
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}