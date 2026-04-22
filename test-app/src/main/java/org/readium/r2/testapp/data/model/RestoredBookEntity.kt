package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restored_books")
data class RestoredBookEntity(
    @PrimaryKey
    @ColumnInfo(name = "server_identifier")
    val serverIdentifier: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "author")
    val author: String?,

    @ColumnInfo(name = "total_pages")
    val totalPages: Int?,

    @ColumnInfo(name = "current_page")
    val currentPage: Int,

    @ColumnInfo(name = "reading_time")
    val readingTime: Long,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "last_read_date")
    val lastReadDate: String?
)