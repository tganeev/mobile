package org.readium.r2.testapp.sync

import java.time.LocalDate

data class SyncBookDTO(
    val identifier: String,
    val title: String,
    val author: String? = null,
    val totalPages: Int? = null,
    val language: String? = null,
    val categoryId: Long? = null,
    val readingStats: List<SyncReadingStatDTO> = emptyList()
)

data class SyncReadingStatDTO(
    val date: LocalDate,
    val pagesRead: Int,
    val hoursRead: Double
)

data class SyncRequestDTO(
    val username: String,
    val books: List<SyncBookDTO>
)

data class SyncResponseDTO(
    val success: Boolean,
    val booksCreated: Int,
    val booksUpdated: Int,
    val statsCreated: Int,
    val statsUpdated: Int,
    val message: String,
    val error: String? = null
)