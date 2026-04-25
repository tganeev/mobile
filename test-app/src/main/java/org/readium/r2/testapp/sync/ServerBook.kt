package org.readium.r2.testapp.sync

import com.google.gson.annotations.SerializedName

data class ServerBook(
    @SerializedName("serverIdentifier")
    val serverIdentifier: String,

    @SerializedName("identifier")
    val identifier: String?,

    @SerializedName("title")
    val title: String,

    @SerializedName("author")
    val author: String?,

    @SerializedName("totalPages")
    val totalPages: Int?,

    @SerializedName("currentPage")
    val currentPage: Int,

    @SerializedName("pagesRead")
    val pagesRead: Int,

    @SerializedName("readingTime")
    val readingTime: Long,

    @SerializedName("status")
    val status: String,

    @SerializedName("lastReadDate")
    val lastReadDate: Long?,

    @SerializedName("isDeleted")
    val isDeleted: Boolean = false
)