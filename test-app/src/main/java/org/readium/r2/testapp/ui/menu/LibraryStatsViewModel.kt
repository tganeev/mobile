package org.readium.r2.testapp.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App
import org.readium.r2.testapp.data.model.Book
import java.time.LocalDate
import kotlinx.coroutines.flow.first

data class LibraryStats(
    val todayPagesRead: Int = 0,
    val todayMinutesRead: Long = 0,
    val plannedCount: Int = 0,
    val inProgressCount: Int = 0,
    val completedCount: Int = 0,
    val totalBooks: Int = 0,
    val totalBooksInHistory: Int = 0
)

class LibraryStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App

    private val _stats = MutableStateFlow(LibraryStats())
    val stats: StateFlow<LibraryStats> = _stats.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            // Получаем ВСЕ книги из истории
            val allBooksFromHistory = app.bookRepository.booksForHistory().first()
            val totalBooksInHistory = allBooksFromHistory.size

            // Получаем статистику чтения за сегодня
            val today = LocalDate.now()
            val allStats = app.bookRepository.getAllReadingStats().first()
            val todayStats = allStats.filter { it.date == today }

            var todayPagesRead = 0
            var todayMinutesRead = 0L

            for (stat in todayStats) {
                todayPagesRead += stat.pagesRead
                todayMinutesRead += (stat.hoursRead * 60).toLong()
            }

            // Рассчитываем статусы
            var plannedCount = 0
            var inProgressCount = 0
            var completedCount = 0

            for (book in allBooksFromHistory) {
                val status = getBookStatus(book)
                when {
                    status.contains("Прочитано") -> completedCount++
                    status.contains("В процессе") -> inProgressCount++
                    else -> plannedCount++
                }
            }

            _stats.value = LibraryStats(
                todayPagesRead = todayPagesRead,
                todayMinutesRead = todayMinutesRead,
                plannedCount = plannedCount,
                inProgressCount = inProgressCount,
                completedCount = completedCount,
                totalBooks = allBooksFromHistory.size,
                totalBooksInHistory = totalBooksInHistory
            )
        }
    }

    private fun getBookStatus(book: Book): String {
        if (book.totalPages > 0) {
            val progress = (book.pagesRead.toDouble() / book.totalPages) * 100
            return when {
                progress >= 100 -> "✅ Прочитано"
                progress > 0 -> "📖 В процессе"
                else -> "📚 В плане"
            }
        }
        return if (book.pagesRead > 0) "📖 В процессе" else "📚 В плане"
    }
}