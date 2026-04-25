package org.readium.r2.testapp.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App
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

    private val _periodRange = MutableStateFlow<Pair<LocalDate, LocalDate>>(
        YearMonth.now().atDay(1) to YearMonth.now().atEndOfMonth()
    )
    val periodRange: StateFlow<Pair<LocalDate, LocalDate>> = _periodRange.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredTableData = MutableStateFlow<HistoryTableData?>(null)
    val filteredTableData: StateFlow<HistoryTableData?> = _filteredTableData.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _tableData,
                _searchQuery.debounce(300).distinctUntilChanged()
            ) { data, query ->
                if (data == null) return@combine null
                if (query.isBlank()) return@combine data

                val filteredBooks = data.books.filter { book ->
                    book.title.contains(query, ignoreCase = true)
                }

                val totalsByDate = data.dates.associateWith { date ->
                    filteredBooks.sumOf { it.dailyProgress[date] ?: 0 }.toDouble()
                }

                val totalTimeByDate = data.dates.associateWith { date ->
                    filteredBooks.sumOf { it.dailyTime[date] ?: 0.0 }
                }

                val totalPagesSum = totalsByDate.values.sum()
                val totalHoursSum = totalTimeByDate.values.sum()

                data.copy(
                    books = filteredBooks,
                    totalsByDate = totalsByDate,
                    totalTimeByDate = totalTimeByDate,
                    totalPagesSum = totalPagesSum,
                    totalHoursSum = totalHoursSum
                )
            }.collect { filteredData ->
                _filteredTableData.value = filteredData
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadData() {
        val (startDate, endDate) = _periodRange.value
        loadDataForRange(startDate, endDate)
    }

    fun loadDataForRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                _periodRange.value = startDate to endDate
                val data = loadHistoryData(startDate, endDate)
                _tableData.value = data
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Ошибка загрузки данных"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun previousMonth() {
        val (startDate, _) = _periodRange.value
        val month = YearMonth.from(startDate)
        val previousMonth = month.minusMonths(1)
        val newStart = previousMonth.atDay(1)
        val newEnd = previousMonth.atEndOfMonth()
        loadDataForRange(newStart, newEnd)
    }

    fun nextMonth() {
        val (startDate, _) = _periodRange.value
        val month = YearMonth.from(startDate)
        val nextMonth = month.plusMonths(1)
        val newStart = nextMonth.atDay(1)
        val newEnd = nextMonth.atEndOfMonth()
        loadDataForRange(newStart, newEnd)
    }

    fun getFormattedMonth(): String {
        val (startDate, endDate) = _periodRange.value
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return "${startDate.format(formatter)} - ${endDate.format(formatter)}"
    }

    private suspend fun loadHistoryData(startDate: LocalDate, endDate: LocalDate): HistoryTableData {
        val books = app.bookRepository.books().first()
        val allStats = app.bookRepository.getAllReadingStats().first()

        val statsInRange = allStats.filter { stat ->
            stat.date in startDate..endDate
        }

        val statsByBook = statsInRange.groupBy { it.bookId }

        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate
        while (currentDate <= endDate) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        val bookProgressList = books.mapNotNull { book ->
            val bookId = book.id ?: return@mapNotNull null

            val statusText = when {
                book.pagesRead > 0 -> "В процессе"
                else -> "В плане"
            }

            val bookStats = statsByBook[bookId] ?: emptyList()

            val dailyProgress = dates.associateWith { date ->
                bookStats.find { it.date == date }?.pagesRead ?: 0
            }

            val dailyTime = dates.associateWith { date ->
                bookStats.find { it.date == date }?.hoursRead ?: 0.0
            }

            BookProgress(
                bookId = bookId,
                title = book.title ?: "Без названия",
                author = book.author ?: "",
                status = statusText,
                category = "0.0",
                dailyProgress = dailyProgress,
                dailyTime = dailyTime
            )
        }.sortedBy { it.title }

        val totalsByDate = dates.associateWith { date ->
            bookProgressList.sumOf { it.dailyProgress[date] ?: 0 }.toDouble()
        }

        val totalTimeByDate = dates.associateWith { date ->
            bookProgressList.sumOf { it.dailyTime[date] ?: 0.0 }
        }

        val totalPagesSum = totalsByDate.values.sum()
        val totalHoursSum = totalTimeByDate.values.sum()

        return HistoryTableData(
            periodStart = startDate,
            periodEnd = endDate,
            dates = dates,
            books = bookProgressList,
            totalsByDate = totalsByDate,
            totalTimeByDate = totalTimeByDate,
            totalPagesSum = totalPagesSum,
            totalHoursSum = totalHoursSum
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }
}