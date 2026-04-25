package org.readium.r2.testapp.sync

import com.google.gson.annotations.SerializedName

data class SyncBookDTO(
    @SerializedName("identifier")
    val identifier: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("author")
    val author: String?,

    @SerializedName("totalPages")
    val totalPages: Int?,

    @SerializedName("language")
    val language: String?,

    @SerializedName("categoryId")
    val categoryId: Long?,

    @SerializedName("readingStats")
    val readingStats: List<SyncReadingStatDTO>
)

data class SyncReadingStatDTO(
    @SerializedName("date")
    val date: String,

    @SerializedName("pagesRead")
    val pagesRead: Int,

    @SerializedName("hoursRead")
    val hoursRead: Double
)

data class SyncRequestDTO(
    @SerializedName("username")
    val username: String,

    @SerializedName("books")
    val books: List<SyncBookDTO>
)

data class SyncResponseDTO(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("booksCreated")
    val booksCreated: Int,

    @SerializedName("booksUpdated")
    val booksUpdated: Int,

    @SerializedName("statsCreated")
    val statsCreated: Int,

    @SerializedName("statsUpdated")
    val statsUpdated: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("error")
    val error: String?
)