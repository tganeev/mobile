// файл: src/main/java/org/readium/r2/testapp/backup/BackupData.kt

package org.readium.r2.testapp.backup

import com.google.gson.annotations.SerializedName
import org.readium.r2.testapp.data.model.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * Структура данных для экспорта/импорта всей БД
 */
data class BackupData(
    @SerializedName("version")
    val version: Int = 1,

    @SerializedName("exportDate")
    val exportDate: Long = System.currentTimeMillis(),

    @SerializedName("appVersion")
    val appVersion: String = "3.1.2",

    @SerializedName("books")
    val books: List<BackupBook> = emptyList(),

    @SerializedName("readingStats")
    val readingStats: List<BackupReadingStat> = emptyList(),

    @SerializedName("bookmarks")
    val bookmarks: List<BackupBookmark> = emptyList(),

    @SerializedName("highlights")
    val highlights: List<BackupHighlight> = emptyList(),

    @SerializedName("notes")
    val notes: List<BackupNote> = emptyList(),

    @SerializedName("vocabulary")
    val vocabulary: List<BackupVocabulary> = emptyList(),

    @SerializedName("sleepRecords")
    val sleepRecords: List<BackupSleepRecord> = emptyList(),

    @SerializedName("catalogs")
    val catalogs: List<BackupCatalog> = emptyList()
)

// ===== ВСПОМОГАТЕЛЬНЫЕ DATA CLASS ДЛЯ СЕРИАЛИЗАЦИИ =====

data class BackupBook(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("creation")
    val creation: Long? = null,
    @SerializedName("href")
    val href: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("author")
    val author: String? = null,
    @SerializedName("identifier")
    val identifier: String? = null,
    @SerializedName("progression")
    val progression: String? = null,
    @SerializedName("mediaType")
    val mediaType: String,
    @SerializedName("cover")
    val cover: String,
    @SerializedName("readingTime")
    val readingTime: Long = 0,
    @SerializedName("pagesRead")
    val pagesRead: Int = 0,
    @SerializedName("currentPage")
    val currentPage: Int = 0,
    @SerializedName("totalPages")
    val totalPages: Int = 0,
    @SerializedName("lastReadDate")
    val lastReadDate: Long? = null,
    @SerializedName("isDeleted")
    val isDeleted: Boolean = false,
    @SerializedName("hasFile")
    val hasFile: Boolean = true,
    @SerializedName("lastSynced")
    val lastSynced: Long = 0,
    @SerializedName("serverIdentifier")
    val serverIdentifier: String? = null
) {
    fun toBook(): Book = Book(
        id = null, // Сбрасываем ID при импорте
        creation = creation,
        href = href,
        title = title,
        author = author,
        identifier = identifier,
        progression = progression,
        rawMediaType = mediaType,
        cover = cover,
        readingTime = readingTime,
        pagesRead = pagesRead,
        currentPage = currentPage,
        totalPages = totalPages,
        lastReadDate = lastReadDate,
        isDeleted = isDeleted,
        hasFile = hasFile,
        lastSynced = lastSynced,
        serverIdentifier = serverIdentifier
    )
}

data class BackupReadingStat(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("bookId")
    val bookId: Long,
    @SerializedName("date")
    val date: String,
    @SerializedName("pagesRead")
    val pagesRead: Int = 0,
    @SerializedName("hoursRead")
    val hoursRead: Double = 0.0
)

data class BackupBookmark(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("creation")
    val creation: Long? = null,
    @SerializedName("bookId")
    val bookId: Long,
    @SerializedName("resourceIndex")
    val resourceIndex: Long,
    @SerializedName("resourceHref")
    val resourceHref: String,
    @SerializedName("resourceType")
    val resourceType: String,
    @SerializedName("resourceTitle")
    val resourceTitle: String,
    @SerializedName("location")
    val location: String,
    @SerializedName("locatorText")
    val locatorText: String
)

data class BackupHighlight(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("creation")
    val creation: Long? = null,
    @SerializedName("bookId")
    val bookId: Long,
    @SerializedName("style")
    val style: String,
    @SerializedName("tint")
    val tint: Int,
    @SerializedName("href")
    val href: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("totalProgression")
    val totalProgression: Double = 0.0,
    @SerializedName("locations")
    val locations: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("annotation")
    val annotation: String = ""
)

data class BackupNote(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("content")
    val content: String,
    @SerializedName("myComment")
    val myComment: String? = null,
    @SerializedName("bookTitle")
    val bookTitle: String,
    @SerializedName("bookAuthor")
    val bookAuthor: String? = null,
    @SerializedName("category")
    val category: String = "Общее",
    @SerializedName("creationDate")
    val creationDate: Long = System.currentTimeMillis()
)

data class BackupVocabulary(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("sourceWord")
    val sourceWord: String,
    @SerializedName("translatedWord")
    val translatedWord: String,
    @SerializedName("sourceLanguage")
    val sourceLanguage: String = "en",
    @SerializedName("targetLanguage")
    val targetLanguage: String = "ru",
    @SerializedName("createdDate")
    val createdDate: Long = System.currentTimeMillis()
)

data class BackupSleepRecord(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("date")
    val date: String,
    @SerializedName("wakeTime")
    val wakeTime: String? = null,
    @SerializedName("bedTime")
    val bedTime: String? = null,
    @SerializedName("isManual")
    val isManual: Boolean = false,
    @SerializedName("synced")
    val synced: Boolean = false,
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

data class BackupCatalog(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("title")
    val title: String,
    @SerializedName("href")
    val href: String,
    @SerializedName("type")
    val type: Int
)