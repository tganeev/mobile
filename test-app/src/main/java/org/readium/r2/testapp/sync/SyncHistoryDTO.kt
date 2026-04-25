package org.readium.r2.testapp.sync

import com.google.gson.annotations.SerializedName

data class SyncHistoryRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("lastSyncTimestamp")
    val lastSyncTimestamp: Long? = null
)

data class SyncHistoryResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: SyncHistoryData?
)

data class SyncHistoryData(
    @SerializedName("books")
    val books: List<SyncBookHistory>,
    @SerializedName("readingStats")
    val readingStats: List<SyncReadingStatHistory>,
    @SerializedName("lastSyncTimestamp")
    val lastSyncTimestamp: Long
)

data class SyncBookHistory(
    @SerializedName("serverId")
    val serverId: String,
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
    val lastReadDate: Long?
)

data class SyncReadingStatHistory(
    @SerializedName("bookServerId")
    val bookServerId: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("pagesRead")
    val pagesRead: Int,
    @SerializedName("hoursRead")
    val hoursRead: Double
)