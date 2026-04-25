package org.readium.r2.testapp.data

import androidx.annotation.ColorInt
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import org.joda.time.DateTime
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.data.db.BooksDao
import org.readium.r2.testapp.data.model.Book
import org.readium.r2.testapp.data.model.Bookmark
import org.readium.r2.testapp.data.model.Highlight
import org.readium.r2.testapp.data.model.ReadingStat
import org.readium.r2.testapp.utils.extensions.readium.authorName
import timber.log.Timber

class BookRepository(
    private val booksDao: BooksDao,
) {
    // ===== ОСНОВНЫЕ МЕТОДЫ =====

    fun books(): Flow<List<Book>> = booksDao.getAllBooks()

    suspend fun get(id: Long) = booksDao.get(id)

    suspend fun saveProgression(locator: Locator, bookId: Long) =
        booksDao.saveProgression(locator.toJSON().toString(), bookId)

    suspend fun updateReadingStats(
        bookId: Long,
        readingTime: Long,
        pagesRead: Int,
        locator: Locator,
    ) {
        booksDao.updateReadingStats(
            id = bookId,
            readingTime = readingTime,
            pagesRead = pagesRead,
            locator = locator.toJSON().toString(),
            lastReadDate = System.currentTimeMillis()
        )
    }

    suspend fun getBook(bookId: Long): Book? = booksDao.get(bookId)

    suspend fun updateBook(book: Book) = booksDao.updateBook(book)

    suspend fun updateBookPages(bookId: Long, pages: Int) {
        Timber.d("updateBookPages: bookId=$bookId, pages=$pages")
        booksDao.updateBookPages(bookId, pages)
    }

    suspend fun updateBookReadingTime(bookId: Long, seconds: Long) {
        Timber.d("updateBookReadingTime: bookId=$bookId, seconds=$seconds")
        booksDao.updateBookReadingTime(bookId, seconds)
    }

    suspend fun updateBookTitleAndAuthor(bookId: Long, title: String, author: String?) {
        booksDao.updateBookTitleAndAuthor(bookId, title, author)
    }

    suspend fun insertBook(
        url: Url,
        mediaType: MediaType,
        publication: Publication,
        cover: File,
    ): Long {
        val book = Book(
            creation = DateTime().toDate().time,
            title = publication.metadata.title ?: url.filename,
            author = publication.metadata.authorName,
            identifier = publication.metadata.identifier,
            href = url.toString(),
            mediaType = mediaType,
            progression = "{}",
            cover = cover.path,
            readingTime = 0,
            pagesRead = 0,
            currentPage = 0,
            totalPages = 0,
            lastReadDate = null,
            serverIdentifier = publication.metadata.identifier
        )
        return booksDao.insertBook(book)
    }

    suspend fun deleteBook(id: Long) = booksDao.deleteBook(id)

    // ===== НОВЫЕ МЕТОДЫ ДЛЯ МЯГКОГО УДАЛЕНИЯ =====

    suspend fun softDeleteBook(bookId: Long) = booksDao.softDeleteBook(bookId)

    suspend fun restoreBook(bookId: Long) = booksDao.restoreBook(bookId)

    suspend fun updateHasFile(bookId: Long, hasFile: Boolean) = booksDao.updateHasFile(bookId, hasFile)

    suspend fun updateReadingProgress(
        bookId: Long,
        readingTime: Long,
        pagesRead: Int,
        currentPage: Int,
        locator: Locator,
        totalPages: Int? = null
    ) {
        val finalCurrentPage = if (totalPages != null && currentPage >= totalPages && totalPages > 0) {
            0
        } else {
            currentPage
        }

        booksDao.updateReadingProgress(
            id = bookId,
            readingTime = readingTime,
            pagesRead = pagesRead,
            currentPage = finalCurrentPage,
            locator = locator.toJSON().toString(),
            lastReadDate = System.currentTimeMillis()
        )
    }

    suspend fun getBookByServerIdentifier(serverIdentifier: String): Book? =
        booksDao.getBookByServerIdentifier(serverIdentifier)

    // ===== МЕТОДЫ ДЛЯ СТАТИСТИКИ =====

    fun getReadingStatsForBook(bookId: Long): Flow<List<ReadingStat>> =
        booksDao.getReadingStatsForBook(bookId)

    fun getAllReadingStats(): Flow<List<ReadingStat>> = booksDao.getAllReadingStats()

    suspend fun saveReadingStat(bookId: Long, date: LocalDate, pagesRead: Int, hoursRead: Double) {
        val readingStat = ReadingStat(
            bookId = bookId,
            date = date,
            pagesRead = pagesRead,
            hoursRead = hoursRead
        )
        booksDao.insertReadingStat(readingStat)
    }

    suspend fun deleteReadingStat(bookId: Long, date: LocalDate) {
        booksDao.deleteReadingStat(bookId, date.toString())
    }

    suspend fun insertBookWithoutFile(book: Book): Long {
        return booksDao.insertBook(book)
    }

    suspend fun getTotalPagesRead(bookId: Long): Int = booksDao.getTotalPagesRead(bookId) ?: 0

    suspend fun getTotalHoursRead(bookId: Long): Double = booksDao.getTotalHoursRead(bookId) ?: 0.0

    suspend fun addReadingTime(bookId: Long, seconds: Long) {
        val today = LocalDate.now()
        val hoursToAdd = seconds / 3600.0
        val existingStat = booksDao.getReadingStatByDate(bookId, today.toString())
        if (existingStat == null) {
            val readingStat = ReadingStat(
                bookId = bookId,
                date = today,
                pagesRead = 0,
                hoursRead = hoursToAdd
            )
            booksDao.insertReadingStat(readingStat)
        } else {
            booksDao.addReadingTimeToDate(bookId, today.toString(), hoursToAdd, 0)
        }
        updateTotalReadingTime(bookId)
    }

    private suspend fun updateTotalReadingTime(bookId: Long) {
        val totalHours = booksDao.getTotalHoursRead(bookId) ?: 0.0
        val totalSeconds = (totalHours * 3600).toLong().coerceAtLeast(0)
        booksDao.updateBookReadingTime(bookId, totalSeconds)
    }

    // ===== МЕТОДЫ ДЛЯ ЗАКЛАДОК =====

    fun bookmarksForBook(bookId: Long): Flow<List<Bookmark>> =
        booksDao.getBookmarksForBook(bookId)

    suspend fun insertBookmark(bookId: Long, publication: Publication, locator: Locator): Long {
        val resource = publication.readingOrder.indexOfFirstWithHref(locator.href)!!
        val bookmark = Bookmark(
            creation = DateTime().toDate().time,
            bookId = bookId,
            resourceIndex = resource.toLong(),
            resourceHref = locator.href.toString(),
            resourceType = locator.mediaType.toString(),
            resourceTitle = locator.title.orEmpty(),
            location = locator.locations.toJSON().toString(),
            locatorText = Locator.Text().toJSON().toString()
        )
        return booksDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmarkId: Long) = booksDao.deleteBookmark(bookmarkId)

    // ===== МЕТОДЫ ДЛЯ ВЫДЕЛЕНИЙ =====

    fun highlightsForBook(bookId: Long): Flow<List<Highlight>> =
        booksDao.getHighlightsForBook(bookId)

    suspend fun highlightById(id: Long): Highlight? = booksDao.getHighlightById(id)

    suspend fun addHighlight(
        bookId: Long,
        style: Highlight.Style,
        @ColorInt tint: Int,
        locator: Locator,
        annotation: String,
    ): Long = booksDao.insertHighlight(Highlight(bookId, style, tint, locator, annotation))

    suspend fun updateHighlightAnnotation(id: Long, annotation: String) =
        booksDao.updateHighlightAnnotation(id, annotation)

    suspend fun updateHighlightStyle(id: Long, style: Highlight.Style, @ColorInt tint: Int) =
        booksDao.updateHighlightStyle(id, style, tint)

    suspend fun deleteHighlight(id: Long) = booksDao.deleteHighlight(id)
}