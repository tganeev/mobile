package org.readium.r2.testapp.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.history.HistoryRecord
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface HistoryApi {
    @GET("/api/user/restore")
    suspend fun restoreData(
        @Query("username") username: String
    ): HistoryRestoreResponse
}

class HistorySyncManager(
    private val context: Context,
    private val app: Application
) {
    companion object {
        private const val TAG = "HistorySyncManager"
        private const val BASE_URL = "https://my-pkms.ru"
    }

    private val api: HistoryApi by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(HistoryApi::class.java)
    }

    suspend fun restoreAllData(username: String = "test"): Result<List<HistoryRecord>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Restoring data from server for user: $username")
            val response = api.restoreData(username)

            if (!response.success || response.data == null) {
                return@withContext Result.failure(Exception("Server returned error or no data"))
            }

            val data = response.data

            val historyRecords = data.history.map { event ->
                HistoryRecord(
                    date = formatDate(event.date),
                    bookTitle = event.bookTitle,
                    eventType = mapEventType(event.eventType),
                    details = event.details,
                    bookIdentifier = event.bookIdentifier
                )
            }

            Log.d(TAG, "Restored ${historyRecords.size} history records")
            Result.success(historyRecords)

        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}"
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun mapEventType(eventType: String): String {
        return when (eventType) {
            "start" -> "Начал читать"
            "progress" -> "Прогресс"
            "completed" -> "Закончил книгу"
            "bookmark" -> "Закладка"
            "highlight" -> "Выделение"
            "note" -> "Заметка"
            else -> eventType
        }
    }
}