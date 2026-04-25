package org.readium.r2.testapp.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.model.Book
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.LocalDate
import java.util.concurrent.TimeUnit

interface SyncApi {
    @POST("/api/sync")
    suspend fun syncData(@Body request: SyncRequestDTO): SyncResponseDTO

    @GET("/api/sync/test")
    suspend fun testConnection(): Map<String, String>

    @POST("/api/sync/history")
    suspend fun syncHistory(@Body request: SyncHistoryRequest): SyncHistoryResponse
}

class SyncManager(
    private val context: Context,
    private val bookRepository: BookRepository,
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val BASE_URL = "https://my-pkms.ru"
        private const val CONNECTION_TIMEOUT = 30L
        private const val PREFS_NAME = "sync_prefs"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val api: SyncApi by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(SyncApi::class.java)
    }

    private fun getLastSyncTimestamp(): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0)
    }

    private fun saveLastSyncTimestamp(timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }

    suspend fun syncHistoryFromServer(username: String = "test"): Result<SyncHistoryData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting history sync for user: $username")

            val lastSync = getLastSyncTimestamp()
            val request = SyncHistoryRequest(username, if (lastSync > 0) lastSync else null)

            val response = api.syncHistory(request)

            if (!response.success || response.data == null) {
                return@withContext Result.failure(Exception("Server returned error or no data"))
            }

            val data = response.data

            data.books.forEach { serverBook ->
                saveBookFromServer(serverBook)
            }

            data.readingStats.forEach { serverStat ->
                saveReadingStatFromServer(serverStat)
            }

            saveLastSyncTimestamp(data.lastSyncTimestamp)

            Log.d(TAG, "History sync completed: ${data.books.size} books, ${data.readingStats.size} stats")
            Result.success(data)

        } catch (e: Exception) {
            Log.e(TAG, "History sync failed", e)
            Result.failure(e)
        }
    }

    private suspend fun saveBookFromServer(serverBook: SyncBookHistory) {
        val existingBook = bookRepository.getBookByServerIdentifier(serverBook.serverId)

        if (existingBook != null) {
            val bookId = existingBook.id
            if (bookId != null) {
                val updatedBook = existingBook.copy(
                    title = serverBook.title,
                    author = serverBook.author,
                    totalPages = serverBook.totalPages ?: existingBook.totalPages,
                    currentPage = serverBook.currentPage,
                    pagesRead = serverBook.currentPage,
                    readingTime = serverBook.readingTime,
                    lastReadDate = serverBook.lastReadDate,
                    lastSynced = System.currentTimeMillis(),
                    isDeleted = false
                )
                bookRepository.updateBook(updatedBook)
                Log.d(TAG, "Updated book: ${serverBook.title}")
            } else {
                Log.w(TAG, "Book has null id: ${serverBook.serverId}")
            }
        } else {
            // Используем конструктор Book без MediaType объекта
            val newBook = Book(
                id = null,
                creation = System.currentTimeMillis(),
                href = "",
                title = serverBook.title,
                author = serverBook.author,
                identifier = serverBook.serverId,
                progression = null,
                rawMediaType = "application/epub+zip",  //直接用 строку
                cover = "",
                readingTime = serverBook.readingTime,
                pagesRead = serverBook.currentPage,
                currentPage = serverBook.currentPage,
                totalPages = serverBook.totalPages ?: 0,
                lastReadDate = serverBook.lastReadDate,
                isDeleted = false,
                hasFile = false,
                lastSynced = System.currentTimeMillis(),
                serverIdentifier = serverBook.serverId
            )
            bookRepository.insertBookWithoutFile(newBook)
            Log.d(TAG, "Created new book record: ${serverBook.title}")
        }
    }

    private suspend fun saveReadingStatFromServer(serverStat: SyncReadingStatHistory) {
        val book = bookRepository.getBookByServerIdentifier(serverStat.bookServerId)
        val bookId = book?.id
        if (bookId == null) {
            Log.w(TAG, "Book not found for stat: ${serverStat.bookServerId}")
            return
        }

        val date = try {
            LocalDate.parse(serverStat.date)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: ${serverStat.date}")
            return
        }

        bookRepository.saveReadingStat(bookId, date, serverStat.pagesRead, serverStat.hoursRead)
        Log.d(TAG, "Saved reading stat for ${serverStat.bookServerId} on $date: ${serverStat.pagesRead} pages")
    }

    suspend fun syncAllBooks(username: String = "test"): Result<SyncResponseDTO> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync for user: $username")
            val books = bookRepository.books().first()

            if (books.isEmpty()) {
                return@withContext Result.success(
                    SyncResponseDTO(
                        success = true,
                        booksCreated = 0,
                        booksUpdated = 0,
                        statsCreated = 0,
                        statsUpdated = 0,
                        message = "No books to sync",
                        error = null
                    )
                )
            }

            val syncBooks = books.mapNotNull { book ->
                try {
                    val bookId = book.id
                    if (bookId == null) return@mapNotNull null
                    val stats = bookRepository.getReadingStatsForBook(bookId).first()
                    SyncBookDTO(
                        identifier = book.identifier ?: generateBookIdentifier(book),
                        title = book.title ?: "",
                        author = book.author ?: "",
                        totalPages = book.totalPages,
                        language = null,
                        categoryId = null,
                        readingStats = stats.map { stat ->
                            SyncReadingStatDTO(
                                date = stat.date.toString(),
                                pagesRead = stat.pagesRead,
                                hoursRead = stat.hoursRead
                            )
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error preparing book ${book.title}", e)
                    null
                }
            }

            val request = SyncRequestDTO(username, syncBooks)
            val response = api.syncData(request)
            Log.d(TAG, "Sync response success: ${response.success}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }

    private fun generateBookIdentifier(book: Book): String {
        val title = book.title ?: ""
        val author = book.author ?: ""
        return "${title}_${author}_${book.href.hashCode()}".hashCode().toString()
    }
}