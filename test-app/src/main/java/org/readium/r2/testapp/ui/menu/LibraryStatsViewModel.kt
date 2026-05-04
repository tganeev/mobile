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

data class LibraryStats(
    val totalPagesRead: Int = 0,
    val totalMinutesRead: Long = 0,
    val plannedCount: Int = 0,
    val inProgressCount: Int = 0,
    val completedCount: Int = 0,
    val totalBooks: Int = 0
)

class LibraryStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App

    private val _stats = MutableStateFlow(LibraryStats())
    val stats: StateFlow<LibraryStats> = _stats.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            app.bookRepository.books().collect { books ->
                _stats.value = calculateStats(books)
            }
        }
    }

    private fun calculateStats(books: List<Book>): LibraryStats {
        var totalPagesRead = 0
        var totalMinutesRead = 0L
        var plannedCount = 0
        var inProgressCount = 0
        var completedCount = 0

        for (book in books) {
            totalPagesRead += book.pagesRead
            totalMinutesRead += book.readingTime / 60

            val status = getBookStatus(book)
            when {
                status.contains("Прочитано") -> completedCount++
                status.contains("В процессе") -> inProgressCount++
                else -> plannedCount++
            }
        }

        return LibraryStats(
            totalPagesRead = totalPagesRead,
            totalMinutesRead = totalMinutesRead,
            plannedCount = plannedCount,
            inProgressCount = inProgressCount,
            completedCount = completedCount,
            totalBooks = books.size
        )
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