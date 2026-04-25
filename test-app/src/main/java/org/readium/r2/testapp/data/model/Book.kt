package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.mediatype.MediaType

@Entity(tableName = Book.TABLE_NAME)
data class Book(
    @PrimaryKey
    @ColumnInfo(name = ID)
    var id: Long? = null,

    @ColumnInfo(name = CREATION_DATE, defaultValue = "CURRENT_TIMESTAMP")
    val creation: Long? = null,

    @ColumnInfo(name = HREF)
    val href: String,

    @ColumnInfo(name = TITLE)
    val title: String?,

    @ColumnInfo(name = AUTHOR)
    val author: String? = null,

    @ColumnInfo(name = IDENTIFIER)
    val identifier: String? = null,

    @ColumnInfo(name = PROGRESSION)
    val progression: String? = null,

    @ColumnInfo(name = MEDIA_TYPE)
    val rawMediaType: String,

    @ColumnInfo(name = COVER)
    val cover: String,

    @ColumnInfo(name = READING_TIME, defaultValue = "0")
    var readingTime: Long = 0,

    @ColumnInfo(name = PAGES_READ, defaultValue = "0")
    var pagesRead: Int = 0,

    @ColumnInfo(name = CURRENT_PAGE, defaultValue = "0")
    var currentPage: Int = 0,

    @ColumnInfo(name = TOTAL_PAGES, defaultValue = "0")
    var totalPages: Int = 0,

    @ColumnInfo(name = LAST_READ_DATE)
    var lastReadDate: Long? = null,

    // НОВЫЕ ПОЛЯ
    @ColumnInfo(name = IS_DELETED, defaultValue = "0")
    var isDeleted: Boolean = false,

    @ColumnInfo(name = HAS_FILE, defaultValue = "1")
    var hasFile: Boolean = true,

    @ColumnInfo(name = LAST_SYNCED, defaultValue = "0")
    var lastSynced: Long = 0,

    @ColumnInfo(name = SERVER_IDENTIFIER)
    var serverIdentifier: String? = null

) : Serializable {

    constructor(
        id: Long? = null,
        creation: Long? = null,
        href: String,
        title: String?,
        author: String? = null,
        identifier: String? = null,
        progression: String? = null,
        mediaType: MediaType,
        cover: String,
        readingTime: Long = 0,
        pagesRead: Int = 0,
        currentPage: Int = 0,
        totalPages: Int = 0,
        lastReadDate: Long? = null,
        isDeleted: Boolean = false,
        hasFile: Boolean = true,
        lastSynced: Long = 0,
        serverIdentifier: String? = null
    ) : this(
        id = id,
        creation = creation,
        href = href,
        title = title,
        author = author,
        identifier = identifier,
        progression = progression,
        rawMediaType = mediaType.toString(),
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

    val url: AbsoluteUrl get() = AbsoluteUrl(href)!!
    val mediaType: MediaType get() = MediaType(rawMediaType)!!

    companion object {
        const val TABLE_NAME = "books"
        const val ID = "id"
        const val CREATION_DATE = "creation_date"
        const val HREF = "href"
        const val TITLE = "title"
        const val AUTHOR = "author"
        const val IDENTIFIER = "identifier"
        const val PROGRESSION = "progression"
        const val MEDIA_TYPE = "media_type"
        const val COVER = "cover"
        const val READING_TIME = "reading_time"
        const val PAGES_READ = "pages_read"
        const val CURRENT_PAGE = "current_page"
        const val TOTAL_PAGES = "total_pages"
        const val LAST_READ_DATE = "last_read_date"
        const val IS_DELETED = "is_deleted"
        const val HAS_FILE = "has_file"
        const val LAST_SYNCED = "last_synced"
        const val SERVER_IDENTIFIER = "server_identifier"
    }
}