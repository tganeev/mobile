package org.readium.r2.testapp.bookshelf

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUrl
import org.readium.r2.testapp.data.model.Book
import org.readium.r2.testapp.data.model.ReadingStat
import org.readium.r2.testapp.reader.OpeningError
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.utils.EventChannel

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<org.readium.r2.testapp.Application>()
    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    val books = app.bookRepository.books()

    // Обновленная функция с параметром pagesRead
    fun updateBookMetadata(bookId: Long, title: String, author: String?, pagesRead: Int) {
        viewModelScope.launch {
            try {
                // Обновляем название и автора
                app.bookRepository.updateBookTitleAndAuthor(bookId, title, author)
                // Обновляем номер страницы
                app.bookRepository.updateBookPages(bookId, pagesRead)

                app.bookRepository.books().firstOrNull()
                android.util.Log.d("BookshelfViewModel", "Book metadata updated: $bookId, title=$title, author=$author, pages=$pagesRead")
            } catch (e: Exception) {
                android.util.Log.e("BookshelfViewModel", "Failed to update book metadata", e)
            }
        }
    }

    // ... остальной код без изменений
    fun deletePublication(book: Book) = viewModelScope.launch {
        app.bookshelf.deleteBook(book)
        app.bookRepository.books().firstOrNull()
    }

    fun importPublicationFromStorage(uri: Uri) {
        app.bookshelf.importPublicationFromStorage(uri)
    }

    fun addPublicationFromStorage(uri: Uri) {
        app.bookshelf.addPublicationFromStorage(uri.toUrl()!! as AbsoluteUrl)
    }

    fun addPublicationFromWeb(url: AbsoluteUrl) {
        app.bookshelf.addPublicationFromWeb(url)
    }

    fun openPublication(bookId: Long) {
        viewModelScope.launch {
            app.readerRepository
                .open(bookId)
                .onFailure {
                    channel.send(Event.OpenPublicationError(it))
                }
                .onSuccess {
                    val arguments = ReaderActivityContract.Arguments(bookId)
                    channel.send(Event.LaunchReader(arguments))
                }
        }
    }

    // НОВЫЙ МЕТОД: Обновление метаданных + коррекция статистики
    fun updateBookMetadata(bookId: Long, title: String, author: String?, newPagesRead: Int, oldPagesRead: Int) {
        viewModelScope.launch {
            try {
                // 1. Обновляем базовые данные и номер страницы
                app.bookRepository.updateBookTitleAndAuthor(bookId, title, author)
                app.bookRepository.updateBookPages(bookId, newPagesRead)

                // 2. Рассчитываем разницу (дельта)
                val delta = newPagesRead - oldPagesRead

                if (delta != 0) {
                    val today = java.time.LocalDate.now()
                    // Получаем текущую запись за сегодня
                    val todayStat = app.bookRepository.getReadingStatsForBook(bookId).firstOrNull()?.find { it.date == today }

                    val currentDailyPages = todayStat?.pagesRead ?: 0
                    // Корректируем (убеждаемся, что не уходит в минус)
                    val correctedDailyPages = maxOf(0, currentDailyPages + delta)

                    // Сохраняем скорректированную запись
                    val updatedStat = ReadingStat(
                        id = todayStat?.id ?: 0,
                        bookId = bookId,
                        date = today,
                        pagesRead = correctedDailyPages,
                        hoursRead = todayStat?.hoursRead ?: 0.0
                    )
                    app.bookRepository.upsertReadingStat(updatedStat)

                    android.util.Log.d("BookshelfViewModel", "Stats corrected by $delta. New daily total: $correctedDailyPages")
                }

                app.bookRepository.books().firstOrNull()
            } catch (e: Exception) {
                android.util.Log.e("BookshelfViewModel", "Failed to update book metadata", e)
            }
        }
    }

    sealed class Event {
        class OpenPublicationError(val error: OpeningError) : Event()
        class LaunchReader(val arguments: ReaderActivityContract.Arguments) : Event()
    }
}