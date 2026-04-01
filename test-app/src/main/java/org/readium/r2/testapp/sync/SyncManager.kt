package org.readium.r2.testapp.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.model.Book
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface SyncApi {
    @POST("/api/sync")
    suspend fun syncData(@Body request: SyncRequestDTO): SyncResponseDTO

    @GET("/api/sync/test")
    suspend fun testConnection(): Map<String, String>
}

class SyncManager(
    private val context: Context,
    private val bookRepository: BookRepository
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val BASE_URL = "https://my-pkms.ru"
        private const val CONNECTION_TIMEOUT = 30L // seconds

    }

    private val api: SyncApi by lazy {
        // Создаем логгер для отладки
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Настраиваем OkHttpClient
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .build()

        // Настраиваем Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(SyncApi::class.java)
    }

    /**
     * Проверка соединения с сервером
     */
    suspend fun testConnection(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing connection to $BASE_URL")
            val response = api.testConnection()
            Log.d(TAG, "Connection test successful: $response")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(e)
        }
    }

    /**
     * Синхронизация всех книг и статистики
     */
    suspend fun syncAllBooks(username: String = "test"): Result<SyncResponseDTO> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync for user: $username")

            // Получаем все книги
            val books = bookRepository.books().first()
            Log.d(TAG, "Found ${books.size} books to sync")

            if (books.isEmpty()) {
                Log.d(TAG, "No books to sync")
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

            // Собираем DTO для отправки
            val syncBooks = mutableListOf<SyncBookDTO>()

            for (book in books) {
                try {
                    val stats = bookRepository.getReadingStatsForBook(book.id!!).first()

                    syncBooks.add(
                        SyncBookDTO(
                            identifier = book.identifier ?: generateBookIdentifier(book),
                            title = book.title ?: "",
                            author = book.author,
                            totalPages = null,
                            language = null,
                            categoryId = null,
                            readingStats = stats.map { stat ->
                                SyncReadingStatDTO(
                                    date = stat.date,
                                    pagesRead = stat.pagesRead,
                                    hoursRead = stat.hoursRead
                                )
                            }
                        )
                    )

                    Log.d(TAG, "Prepared book: ${book.title} with ${stats.size} stats records")
                } catch (e: Exception) {
                    Log.e(TAG, "Error preparing book ${book.title}", e)
                }
            }

            val request = SyncRequestDTO(username, syncBooks)
            Log.d(TAG, "Sending sync request with ${syncBooks.size} books")

            val response = api.syncData(request)
            Log.d(TAG, "Sync response: $response")

            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Синхронизация только одной книги
     */
    suspend fun syncBook(bookId: Long, username: String = "test"): Result<SyncResponseDTO> = withContext(Dispatchers.IO) {
        try {
            val book = bookRepository.get(bookId)
            if (book == null) {
                return@withContext Result.failure(Exception("Book not found"))
            }

            val stats = bookRepository.getReadingStatsForBook(bookId).first()

            val syncBook = SyncBookDTO(
                identifier = book.identifier ?: generateBookIdentifier(book),
                title = book.title ?: "",
                author = book.author,
                totalPages = null,
                language = null,
                categoryId = null,
                readingStats = stats.map { stat ->
                    SyncReadingStatDTO(
                        date = stat.date,
                        pagesRead = stat.pagesRead,
                        hoursRead = stat.hoursRead
                    )
                }
            )

            val request = SyncRequestDTO(username, listOf(syncBook))
            val response = api.syncData(request)

            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Sync book failed", e)
            Result.failure(e)
        }
    }

    private fun generateBookIdentifier(book: Book): String {
        // Генерируем идентификатор на основе названия и автора
        val title = book.title ?: ""
        val author = book.author ?: ""
        val identifier = "${title}_${author}_${book.href.hashCode()}".hashCode().toString()
        Log.d(TAG, "Generated identifier for '${book.title}': $identifier")
        return identifier
    }
}