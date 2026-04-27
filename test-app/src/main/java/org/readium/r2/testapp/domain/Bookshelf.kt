package org.readium.r2.testapp.domain

import android.net.Uri
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.model.Book
import org.readium.r2.testapp.utils.tryOrLog
import timber.log.Timber

class Bookshelf(
    private val bookRepository: BookRepository,
    private val coverStorage: CoverStorage,
    private val publicationOpener: PublicationOpener,
    private val assetRetriever: AssetRetriever,
    private val publicationRetriever: PublicationRetriever,
) {
    sealed class Event {
        data object ImportPublicationSuccess : Event()
        class ImportPublicationError(val error: ImportError) : Event()
        class ShowLinkDialog(
            val existingBook: Book,
            val newBookData: NewBookData
        ) : Event()
    }

    data class NewBookData(
        val url: AbsoluteUrl,
        val format: Format?,
        val coverUrl: AbsoluteUrl?,
        val publication: Publication,
        val coverFile: File
    )

    val channel: Channel<Event> = Channel(Channel.UNLIMITED)
    private val coroutineScope: CoroutineScope = MainScope()

    fun importPublicationFromStorage(uri: Uri) {
        coroutineScope.launch {
            val result = publicationRetriever.retrieveFromStorage(uri)
            val r = result.getOrNull()
            if (r != null) {
                addBook(r.publication.toUrl(isDirectory = false), r.format, r.coverUrl)
            } else {
                channel.send(Event.ImportPublicationError(ImportError.Publication(PublicationError.Unexpected(DebugError("Import failed")))))
            }
        }
    }

    fun importPublicationFromHttp(url: AbsoluteUrl) {
        coroutineScope.launch {
            val result = publicationRetriever.retrieveFromHttp(url)
            val r = result.getOrNull()
            if (r != null) {
                addBook(r.publication.toUrl(isDirectory = false), r.format, r.coverUrl)
            } else {
                channel.send(Event.ImportPublicationError(ImportError.Publication(PublicationError.Unexpected(DebugError("Import failed")))))
            }
        }
    }

    fun importPublicationFromOpds(publication: Publication) {
        coroutineScope.launch {
            val result = publicationRetriever.retrieveFromOpds(publication)
            val r = result.getOrNull()
            if (r != null) {
                addBook(r.publication.toUrl(isDirectory = false), r.format, r.coverUrl)
            } else {
                channel.send(Event.ImportPublicationError(ImportError.Publication(PublicationError.Unexpected(DebugError("Import failed")))))
            }
        }
    }

    fun addPublicationFromWeb(url: AbsoluteUrl) {
        coroutineScope.launch {
            addBook(url, null, null)
        }
    }

    fun addPublicationFromStorage(url: AbsoluteUrl) {
        coroutineScope.launch {
            addBook(url, null, null)
        }
    }

    private suspend fun addBook(
        url: AbsoluteUrl,
        format: Format? = null,
        coverUrl: AbsoluteUrl? = null,
    ) {
        val assetResult = if (format == null) {
            assetRetriever.retrieve(url)
        } else {
            assetRetriever.retrieve(url, format)
        }

        val asset = assetResult.getOrNull()
        if (asset == null) {
            channel.send(Event.ImportPublicationError(ImportError.Publication(PublicationError.Unexpected(DebugError("Failed to retrieve asset")))))
            return
        }

        val openResult = publicationOpener.open(asset, allowUserInteraction = false)
        val publication = openResult.getOrNull()
        if (publication == null) {
            channel.send(Event.ImportPublicationError(ImportError.Publication(PublicationError.Unexpected(DebugError("Failed to open publication")))))
            return
        }

        val coverFileResult = coverStorage.storeCover(publication, coverUrl)
        val coverFile = coverFileResult.getOrNull()
        if (coverFile == null) {
            channel.send(Event.ImportPublicationError(ImportError.FileSystem(FileSystemError.IO(Exception("Failed to store cover")))))
            return
        }

        // 1. Получаем serverIdentifier из метаданных
        var serverIdentifier = publication.metadata.identifier

        // 2. Если нет - генерируем из названия + автор
        if (serverIdentifier.isNullOrBlank()) {
            val title = publication.metadata.title ?: ""
            val author = publication.metadata.authors.firstOrNull()?.name ?: ""
            serverIdentifier = generateServerIdentifier(title, author)
            Timber.d("Generated serverIdentifier from title+author: $serverIdentifier")
        }

        // 3. Ищем книгу по serverIdentifier
        val existingBookByIdentifier = bookRepository.findBookByServerIdentifier(serverIdentifier)

        if (existingBookByIdentifier != null) {
            // 4. Нашли по identifier - обновляем существующую запись
            Timber.d("Found existing book by serverIdentifier: ${existingBookByIdentifier.id}, linking file")

            bookRepository.attachFileToExistingBook(
                serverIdentifier = serverIdentifier,
                href = url.toString(),
                cover = coverFile.path,
                mediaType = asset.format.mediaType.toString()
            )

            val bookId = existingBookByIdentifier.id
            if (bookId != null) {
                bookRepository.updateBook(
                    existingBookByIdentifier.copy(
                        href = url.toString(),
                        cover = coverFile.path,
                        hasFile = true,
                        isDeleted = false,
                        lastSynced = System.currentTimeMillis()
                    )
                )
            }

            coverFile.delete()
            channel.send(Event.ImportPublicationSuccess)
            return
        }

        // 5. Не нашли по identifier - ищем по названию + автору
        val title = publication.metadata.title
        val author = publication.metadata.authors.firstOrNull()?.name

        if (!title.isNullOrBlank()) {
            val existingBookByTitle = bookRepository.findBookByTitleAndAuthor(title, author)

            if (existingBookByTitle != null) {
                Timber.d("Found existing book by title+author, need user decision")

                val newBookData = NewBookData(
                    url = url,
                    format = asset.format,
                    coverUrl = coverUrl,
                    publication = publication,
                    coverFile = coverFile
                )

                channel.send(Event.ShowLinkDialog(existingBookByTitle, newBookData))
                return
            }
        }

        // 7. Ничего не нашли - создаём новую книгу
        Timber.d("No existing book found, creating new")

        val bookId = bookRepository.insertBook(
            url,
            asset.format.mediaType,
            publication,
            coverFile
        )

        if (bookId == -1L) {
            coverFile.delete()
            channel.send(Event.ImportPublicationError(ImportError.Database(DebugError("Could not insert book into database."))))
            return
        }

        channel.send(Event.ImportPublicationSuccess)
    }

    private fun generateServerIdentifier(title: String, author: String): String {
        val normalizedTitle = title.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
        val normalizedAuthor = author.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
        return "${normalizedTitle}_${normalizedAuthor}".take(100)
    }

    suspend fun attachFileToExistingBook(
        serverIdentifier: String,
        href: String,
        cover: String,
        mediaType: String
    ) {
        bookRepository.attachFileToExistingBook(serverIdentifier, href, cover, mediaType)
    }

    suspend fun refreshBooks() {
        bookRepository.books().firstOrNull()
    }

    suspend fun deleteBook(book: Book) {
        val id = book.id!!
        bookRepository.softDeleteBook(id)
        bookRepository.updateHasFile(id, false)

        tryOrLog { book.url.toFile()?.delete() }
        tryOrLog { File(book.cover).delete() }

        Timber.d("Book soft deleted (statistics preserved): $id, title=${book.title}")
    }
}