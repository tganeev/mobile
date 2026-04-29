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

    sealed class Event {
        class OpenPublicationError(val error: OpeningError) : Event()
        class LaunchReader(val arguments: ReaderActivityContract.Arguments) : Event()
    }
}