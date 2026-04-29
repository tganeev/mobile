package org.readium.r2.testapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.model.*

@Dao
interface BooksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Query("DELETE FROM " + Book.TABLE_NAME + " WHERE " + Book.ID + " = :bookId")
    suspend fun deleteBook(bookId: Long)

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.ID + " = :id")
    suspend fun get(id: Long): Book?

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.IDENTIFIER + " = :identifier")
    suspend fun getBookByIdentifier(identifier: String): Book?

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.IS_DELETED + " = 0 ORDER BY " + Book.CREATION_DATE + " desc")
    fun getAllBooks(): Flow<List<Book>>

    @Update
    suspend fun updateBook(book: Book)

    @Query(
        "UPDATE " + Book.TABLE_NAME +
            " SET " + Book.READING_TIME + " = :readingTime, " +
            Book.PAGES_READ + " = :pagesRead, " +
            Book.PROGRESSION + " = :locator, " +
            Book.LAST_READ_DATE + " = :lastReadDate " +
            " WHERE " + Book.ID + "= :id"
    )
    suspend fun updateReadingStats(
        id: Long,
        readingTime: Long,
        pagesRead: Int,
        locator: String,
        lastReadDate: Long,
    )

    @Query(
        "UPDATE " + Book.TABLE_NAME +
            " SET " + Book.PROGRESSION + " = :locator WHERE " + Book.ID + "= :id"
    )
    suspend fun saveProgression(locator: String, id: Long)



    // ===== МЕТОДЫ ДЛЯ ЗАКЛАДОК =====
    @Query("SELECT * FROM " + Bookmark.TABLE_NAME + " WHERE " + Bookmark.BOOK_ID + " = :bookId")
    fun getBookmarksForBook(bookId: Long): Flow<List<Bookmark>>

    // ===== МЕТОДЫ ДЛЯ ПОДСВЕТОК =====
    @Query(
        "SELECT * FROM ${Highlight.TABLE_NAME} WHERE ${Highlight.BOOK_ID} = :bookId ORDER BY ${Highlight.TOTAL_PROGRESSION} ASC"
    )
    fun getHighlightsForBook(bookId: Long): Flow<List<Highlight>>

    @Query("SELECT * FROM ${Highlight.TABLE_NAME} WHERE ${Highlight.ID} = :highlightId")
    suspend fun getHighlightById(highlightId: Long): Highlight?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight): Long

    @Query(
        "UPDATE ${Highlight.TABLE_NAME} SET ${Highlight.ANNOTATION} = :annotation WHERE ${Highlight.ID} = :id"
    )
    suspend fun updateHighlightAnnotation(id: Long, annotation: String)

    @Query(
        "UPDATE ${Highlight.TABLE_NAME} SET ${Highlight.TINT} = :tint, ${Highlight.STYLE} = :style WHERE ${Highlight.ID} = :id"
    )
    suspend fun updateHighlightStyle(id: Long, style: Highlight.Style, tint: Int)

    @Query("DELETE FROM " + Bookmark.TABLE_NAME + " WHERE " + Bookmark.ID + " = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM ${Highlight.TABLE_NAME} WHERE ${Highlight.ID} = :id")
    suspend fun deleteHighlight(id: Long)

    // ===== МЕТОДЫ ДЛЯ СТАТИСТИКИ ЧТЕНИЯ =====
    @Query("SELECT * FROM reading_stats WHERE book_id = :bookId ORDER BY date ASC")
    fun getReadingStatsForBook(bookId: Long): Flow<List<ReadingStat>>

    @Query("SELECT * FROM reading_stats ORDER BY date ASC")
    fun getAllReadingStats(): Flow<List<ReadingStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingStat(stat: ReadingStat)

    @Query("DELETE FROM reading_stats WHERE book_id = :bookId AND date = :date")
    suspend fun deleteReadingStat(bookId: Long, date: String)

    @Query("SELECT SUM(pages_read) FROM reading_stats WHERE book_id = :bookId")
    suspend fun getTotalPagesRead(bookId: Long): Int?

    // НОВЫЙ МЕТОД: Обновляет или создаёт статистику за конкретную дату
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingStat(stat: ReadingStat)

    @Query("SELECT SUM(hours_read) FROM reading_stats WHERE book_id = :bookId")
    suspend fun getTotalHoursRead(bookId: Long): Double?

    @Query("UPDATE books SET title = :title, author = :author WHERE id = :bookId")
    suspend fun updateBookTitleAndAuthor(bookId: Long, title: String, author: String?)

    @Query("UPDATE books SET reading_time = :seconds WHERE id = :bookId")
    suspend fun updateBookReadingTime(bookId: Long, seconds: Long)

    @Query("UPDATE books SET pages_read = :pages, progression = '{}' WHERE id = :bookId")
    suspend fun updateBookPages(bookId: Long, pages: Int)

    @Query("SELECT * FROM reading_stats WHERE book_id = :bookId AND date = :date")
    suspend fun getReadingStatByDate(bookId: Long, date: String): ReadingStat?

    @Query(
        """
    UPDATE reading_stats 
    SET hours_read = hours_read + :hoursToAdd,
        pages_read = pages_read + :pagesToAdd
    WHERE book_id = :bookId AND date = :date
"""
    )
    suspend fun addReadingTimeToDate(
        bookId: Long,
        date: String,
        hoursToAdd: Double,
        pagesToAdd: Int,
    )

    // ===== МЕТОДЫ ДЛЯ МЯГКОГО УДАЛЕНИЯ И ВОССТАНОВЛЕНИЯ =====
    @Query("UPDATE " + Book.TABLE_NAME + " SET " + Book.IS_DELETED + " = 1 WHERE " + Book.ID + " = :id")
    suspend fun softDeleteBook(id: Long)

    @Query("UPDATE " + Book.TABLE_NAME + " SET " + Book.IS_DELETED + " = 0 WHERE " + Book.ID + " = :id")
    suspend fun restoreBook(id: Long)

    @Query("UPDATE " + Book.TABLE_NAME + " SET " + Book.HAS_FILE + " = :hasFile WHERE " + Book.ID + " = :id")
    suspend fun updateHasFile(id: Long, hasFile: Boolean)

    @Query(
        "UPDATE " + Book.TABLE_NAME +
            " SET " + Book.READING_TIME + " = :readingTime, " +
            Book.PAGES_READ + " = :pagesRead, " +
            Book.CURRENT_PAGE + " = :currentPage, " +
            Book.PROGRESSION + " = :locator, " +
            Book.LAST_READ_DATE + " = :lastReadDate " +
            " WHERE " + Book.ID + " = :id"
    )
    suspend fun updateReadingProgress(
        id: Long,
        readingTime: Long,
        pagesRead: Int,
        currentPage: Int,
        locator: String,
        lastReadDate: Long
    )

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.SERVER_IDENTIFIER + " = :serverIdentifier")
    suspend fun getBookByServerIdentifier(serverIdentifier: String): Book?

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.IS_DELETED + " = 0 OR " + Book.HAS_FILE + " = 0")
    suspend fun getAllBooksForSync(): List<Book>

    // ===== НОВЫЕ МЕТОДЫ ДЛЯ СВЯЗЫВАНИЯ КНИГ С ИСТОРИЕЙ =====

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.SERVER_IDENTIFIER + " = :serverIdentifier AND " + Book.IS_DELETED + " = 0")
    suspend fun findBookByServerIdentifier(serverIdentifier: String): Book?

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.TITLE + " = :title AND " + Book.AUTHOR + " = :author AND " + Book.IS_DELETED + " = 0")
    suspend fun findBookByTitleAndAuthor(title: String, author: String?): Book?

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.TITLE + " = :title AND " + Book.IS_DELETED + " = 0")
    suspend fun findBooksByTitle(title: String): List<Book>

    @Query("UPDATE books SET current_page = :currentPage WHERE id = :bookId")
    suspend fun updateCurrentPage(bookId: Long, currentPage: Int)

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.HAS_FILE + " = 1 AND " + Book.IS_DELETED + " = 0 ORDER BY " + Book.CREATION_DATE + " desc")
    fun getBooksWithFile(): Flow<List<Book>>

    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.IS_DELETED + " = 0 ORDER BY " + Book.CREATION_DATE + " desc")
    fun getAllBooksForHistory(): Flow<List<Book>>

    @Query(
        "UPDATE " + Book.TABLE_NAME + " SET " +
            "href = :href, " +
            "cover = :cover, " +
            "media_type = :mediaType, " +
            "has_file = 1, " +
            "is_deleted = 0, " +
            "last_synced = :lastSynced, " +
            "progression = '{}', " +
            "current_page = 0 " +
            "WHERE " + Book.SERVER_IDENTIFIER + " = :serverIdentifier"
    )
    suspend fun attachFileToExistingBook(
        serverIdentifier: String,
        href: String,
        cover: String,
        mediaType: String,
        lastSynced: Long = System.currentTimeMillis()
    )

    @Query(
        "UPDATE " + Book.TABLE_NAME + " SET " +
            "has_file = 1, " +
            "is_deleted = 0, " +
            "href = :href, " +
            "cover = :cover, " +
            "media_type = :mediaType, " +
            "progression = '{}', " +
            "current_page = 0 " +
            "WHERE " + Book.ID + " = :bookId"
    )
    suspend fun attachFileToBookById(
        bookId: Long,
        href: String,
        cover: String,
        mediaType: String
    )
}