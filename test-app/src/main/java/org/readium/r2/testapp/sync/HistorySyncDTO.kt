package org.readium.r2.testapp.sync

import com.google.gson.annotations.SerializedName

data class HistoryRestoreResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: RestoredData?
)

data class RestoredData(
    @SerializedName("books")
    val books: List<RestoredBook>,
    @SerializedName("history")
    val history: List<RestoredHistoryEvent>,
    @SerializedName("bookmarks")
    val bookmarks: List<RestoredBookmark>,
    @SerializedName("highlights")
    val highlights: List<RestoredHighlight>
)

data class RestoredBook(
    @SerializedName("serverIdentifier")
    val serverIdentifier: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("author")
    val author: String?,
    @SerializedName("totalPages")
    val totalPages: Int?,
    @SerializedName("currentPage")
    val currentPage: Int,
    @SerializedName("readingTime")
    val readingTime: Long,
    @SerializedName("status")
    val status: String,
    @SerializedName("lastReadDate")
    val lastReadDate: String?
)

data class RestoredHistoryEvent(
    @SerializedName("date")
    val date: String,
    @SerializedName("bookIdentifier")
    val bookIdentifier: String,
    @SerializedName("bookTitle")
    val bookTitle: String,
    @SerializedName("eventType")
    val eventType: String,
    @SerializedName("details")
    val details: String
)

data class RestoredBookmark(
    @SerializedName("bookIdentifier")
    val bookIdentifier: String,
    @SerializedName("page")
    val page: Int,
    @SerializedName("note")
    val note: String?
)

data class RestoredHighlight(
    @SerializedName("bookIdentifier")
    val bookIdentifier: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("page")
    val page: Int,
    @SerializedName("color")
    val color: String
)