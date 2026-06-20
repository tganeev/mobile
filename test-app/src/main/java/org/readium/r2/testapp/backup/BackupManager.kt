// файл: src/main/java/org/readium/r2/testapp/backup/BackupManager.kt

package org.readium.r2.testapp.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.BuildConfig
import org.readium.r2.testapp.data.model.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import timber.log.Timber

class BackupManager(private val context: Context) {

    companion object {
        private const val BACKUP_FILE_PREFIX = "PKMS_Backup_"
        private const val BACKUP_FILE_EXTENSION = ".zip"
        private const val DATA_JSON = "data.json"
        private const val BOOKS_DIR = "books/"
        private const val COVERS_DIR = "covers/"
        private const val VERSION = 1
    }

    private val app = context.applicationContext as Application
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    suspend fun exportData(outputUri: Uri, onProgress: (Int) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                onProgress(0)
                Timber.d("Starting export...")

                onProgress(10)
                val backupData = collectDatabaseData()
                val jsonData = gson.toJson(backupData)
                Timber.d("JSON data size: ${jsonData.length} bytes")

                onProgress(20)
                val tempDir = File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                onProgress(30)
                val booksDir = File(tempDir, BOOKS_DIR)
                booksDir.mkdirs()
                copyBookFiles(booksDir, backupData.books)

                onProgress(50)
                val coversDir = File(tempDir, COVERS_DIR)
                coversDir.mkdirs()
                copyCoverFiles(coversDir, backupData.books)

                onProgress(70)
                val zipFile = createZipArchive(tempDir, jsonData)

                onProgress(85)
                saveZipToUri(zipFile, outputUri)

                onProgress(95)
                tempDir.deleteRecursively()
                zipFile.delete()

                onProgress(100)
                Timber.d("Export completed successfully")

            } catch (e: Exception) {
                Timber.e(e, "Export failed")
                throw e
            }
        }
    }

    suspend fun importData(inputUri: Uri, onProgress: (Int) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                onProgress(0)
                Timber.d("Starting import...")

                onProgress(10)
                val tempDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                onProgress(20)
                extractZipArchive(inputUri, tempDir)

                onProgress(40)
                val jsonFile = File(tempDir, DATA_JSON)
                if (!jsonFile.exists()) {
                    throw IllegalStateException("Invalid backup file: data.json not found")
                }
                val jsonData = jsonFile.readText()
                val backupData = gson.fromJson(jsonData, BackupData::class.java)

                if (backupData.version != VERSION) {
                    throw IllegalStateException("Unsupported backup version: ${backupData.version}")
                }

                onProgress(50)
                clearAllData()

                onProgress(60)
                importDatabaseData(backupData)

                onProgress(75)
                val booksSourceDir = File(tempDir, BOOKS_DIR)
                copyBooksToStorage(booksSourceDir)

                onProgress(85)
                val coversSourceDir = File(tempDir, COVERS_DIR)
                copyCoversToStorage(coversSourceDir)

                onProgress(95)
                tempDir.deleteRecursively()

                onProgress(100)
                Timber.d("Import completed successfully")

            } catch (e: Exception) {
                Timber.e(e, "Import failed")
                throw e
            }
        }
    }

    private suspend fun collectDatabaseData(): BackupData {
        val bookRepository = app.bookRepository

        // Получаем ВСЕ книги (включая удалённые, но с историей)
        val allBooks = bookRepository.booksForHistory().first()

        val backupBooks = allBooks.map { book ->
            BackupBook(
                id = book.id,
                creation = book.creation,
                href = book.href,
                title = book.title,
                author = book.author,
                identifier = book.identifier,
                progression = book.progression,
                mediaType = book.rawMediaType,
                cover = book.cover,
                readingTime = book.readingTime,
                pagesRead = book.pagesRead,
                currentPage = book.currentPage,
                totalPages = book.totalPages,
                lastReadDate = book.lastReadDate,
                isDeleted = book.isDeleted,
                hasFile = book.hasFile,
                lastSynced = book.lastSynced,
                serverIdentifier = book.serverIdentifier
            )
        }

        // Собираем все статистики
        val allStats = bookRepository.getAllReadingStats().first()
        val backupStats = allStats.map { stat ->
            BackupReadingStat(
                id = stat.id,
                bookId = stat.bookId,
                date = stat.date.toString(),
                pagesRead = stat.pagesRead,
                hoursRead = stat.hoursRead
            )
        }

        // Собираем закладки
        val allBookmarks = mutableListOf<BackupBookmark>()
        allBooks.forEach { book ->
            book.id?.let { bookId ->
                val bookmarks = bookRepository.bookmarksForBook(bookId).first()
                allBookmarks.addAll(bookmarks.map { bookmark ->
                    BackupBookmark(
                        id = bookmark.id,
                        creation = bookmark.creation,
                        bookId = bookId,
                        resourceIndex = bookmark.resourceIndex,
                        resourceHref = bookmark.resourceHref,
                        resourceType = bookmark.resourceType,
                        resourceTitle = bookmark.resourceTitle,
                        location = bookmark.location,
                        locatorText = bookmark.locatorText
                    )
                })
            }
        }

        // Собираем выделения
        val allHighlights = mutableListOf<BackupHighlight>()
        allBooks.forEach { book ->
            book.id?.let { bookId ->
                val highlights = bookRepository.highlightsForBook(bookId).first()
                allHighlights.addAll(highlights.map { highlight ->
                    BackupHighlight(
                        id = highlight.id,
                        creation = highlight.creation,
                        bookId = bookId,
                        style = highlight.style.value,
                        tint = highlight.tint,
                        href = highlight.href,
                        type = highlight.type,
                        title = highlight.title,
                        totalProgression = highlight.totalProgression,
                        locations = highlight.locations.toJSON().toString(),
                        text = highlight.text.toJSON().toString(),
                        annotation = highlight.annotation
                    )
                })
            }
        }

        // Собираем заметки
        val notes = bookRepository.getAllNotes().first()
        val backupNotes = notes.map { note ->
            BackupNote(
                id = note.id,
                content = note.content,
                myComment = note.myComment,
                bookTitle = note.bookTitle,
                bookAuthor = note.bookAuthor,
                category = note.category,
                creationDate = note.creationDate
            )
        }

        // Собираем слова
        val vocabulary = bookRepository.getAllWords().first()
        val backupVocabulary = vocabulary.map { word ->
            BackupVocabulary(
                id = word.id,
                sourceWord = word.sourceWord,
                translatedWord = word.translatedWord,
                sourceLanguage = word.sourceLanguage,
                targetLanguage = word.targetLanguage,
                createdDate = word.createdDate
            )
        }

        // Собираем записи сна
        val sleepRecords = app.sleepRepository.getAllRecords().first()
        val backupSleepRecords = sleepRecords.map { record ->
            BackupSleepRecord(
                id = record.id,
                date = record.date.toString(),
                wakeTime = record.wakeTime?.toString(),
                bedTime = record.bedTime?.toString(),
                isManual = record.isManual,
                synced = record.synced,
                createdAt = record.createdAt,
                updatedAt = record.updatedAt
            )
        }

        // Собираем каталоги
        val catalogs = app.bookRepository.getAllCatalogs().first()
        val backupCatalogs = catalogs.map { catalog ->
            BackupCatalog(
                id = catalog.id,
                title = catalog.title,
                href = catalog.href,
                type = catalog.type
            )
        }

        return BackupData(
            version = VERSION,
            exportDate = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            books = backupBooks,
            readingStats = backupStats,
            bookmarks = allBookmarks,
            highlights = allHighlights,
            notes = backupNotes,
            vocabulary = backupVocabulary,
            sleepRecords = backupSleepRecords,
            catalogs = backupCatalogs
        )
    }

    private suspend fun copyBookFiles(targetDir: File, books: List<BackupBook>) {
        val storageDir = app.storageDir
        books.forEach { book ->
            try {
                if (book.hasFile) {
                    val fileName = book.href.substringAfterLast("/")
                    val sourceFile = File(storageDir, fileName)
                    if (sourceFile.exists()) {
                        val targetFile = File(targetDir, fileName)
                        sourceFile.copyTo(targetFile, overwrite = true)
                        Timber.d("Copied book file: ${sourceFile.name}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to copy book file for: ${book.title}")
            }
        }
    }

    private suspend fun copyCoverFiles(targetDir: File, books: List<BackupBook>) {
        books.forEach { book ->
            try {
                val sourceFile = File(book.cover)
                if (sourceFile.exists()) {
                    val targetFile = File(targetDir, sourceFile.name)
                    sourceFile.copyTo(targetFile, overwrite = true)
                    Timber.d("Copied cover: ${sourceFile.name}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to copy cover for: ${book.title}")
            }
        }
    }

    private suspend fun createZipArchive(sourceDir: File, jsonData: String): File {
        val zipFile = File(context.cacheDir, "${BACKUP_FILE_PREFIX}${dateFormat.format(Date())}$BACKUP_FILE_EXTENSION")

        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                zos.putNextEntry(ZipEntry(DATA_JSON))
                zos.write(jsonData.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                sourceDir.listFiles()?.forEach { dir ->
                    if (dir.isDirectory) {
                        zipDirectory(zos, dir, "")
                    }
                }
            }
        }

        return zipFile
    }

    private fun zipDirectory(zos: ZipOutputStream, dir: File, parentPath: String) {
        val path = parentPath + if (parentPath.isNotEmpty()) "/" else "" + dir.name + "/"
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                zipDirectory(zos, file, path)
            } else {
                try {
                    zos.putNextEntry(ZipEntry(path + file.name))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to zip file: ${file.name}")
                }
            }
        }
    }

    private suspend fun saveZipToUri(zipFile: File, outputUri: Uri) {
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            FileInputStream(zipFile).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Failed to open output stream for URI: $outputUri")
    }

    private suspend fun extractZipArchive(inputUri: Uri, targetDir: File) {
        context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val targetFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: throw IOException("Failed to open input stream for URI: $inputUri")
    }

    private suspend fun clearAllData() {
        val bookRepository = app.bookRepository

        // Получаем все книги (включая удалённые)
        val allBooks = bookRepository.booksForHistory().first()

        // Удаляем файлы
        allBooks.forEach { book ->
            try {
                File(book.href).delete()
                File(book.cover).delete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete book file")
            }
        }

        // Удаляем все книги из БД (каскадное удаление)
        allBooks.forEach { book ->
            book.id?.let { bookRepository.deleteBook(it) }
        }

        // Удаляем все заметки и слова
        bookRepository.deleteAllNotes()
        bookRepository.deleteAllWords()

        // Очищаем папку хранения
        app.storageDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }

        Timber.d("All data cleared")
    }

    private suspend fun importDatabaseData(backupData: BackupData) {
        val bookRepository = app.bookRepository
        val sleepRepository = app.sleepRepository

        // Сохраняем маппинг старых ID на новые
        val bookIdMap = mutableMapOf<Long, Long>()

        // 1. Импортируем книги (сохраняем serverIdentifier для связи)
        backupData.books.forEach { backupBook ->
            val book = backupBook.toBook()
            // Вставляем книгу
            val newId = bookRepository.insertBookWithoutFile(book)
            if (newId != -1L) {
                backupBook.id?.let { oldId ->
                    bookIdMap[oldId] = newId
                }
                Timber.d("Imported book: ${book.title}, oldId=${backupBook.id}, newId=$newId")
            }
        }

        // 2. Импортируем статистику чтения (историю)
        backupData.readingStats.forEach { backupStat ->
            val newBookId = bookIdMap[backupStat.bookId]
            if (newBookId != null) {
                try {
                    val date = LocalDate.parse(backupStat.date)
                    // Проверяем, есть ли уже такая запись
                    val existingStats = bookRepository.getReadingStatsForBook(newBookId).first()
                    val existing = existingStats.find { it.date == date }

                    if (existing == null) {
                        val stat = ReadingStat(
                            bookId = newBookId,
                            date = date,
                            pagesRead = backupStat.pagesRead,
                            hoursRead = backupStat.hoursRead
                        )
                        bookRepository.upsertReadingStat(stat)
                        Timber.d("Imported reading stat: bookId=$newBookId, date=$date, pages=${backupStat.pagesRead}")
                    } else {
                        // Обновляем существующую запись (суммируем)
                        val updatedStat = existing.copy(
                            pagesRead = existing.pagesRead + backupStat.pagesRead,
                            hoursRead = existing.hoursRead + backupStat.hoursRead
                        )
                        bookRepository.upsertReadingStat(updatedStat)
                        Timber.d("Updated reading stat: bookId=$newBookId, date=$date, pages=${updatedStat.pagesRead}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to import reading stat for bookId=${backupStat.bookId}")
                }
            } else {
                Timber.w("Book not found for reading stat: ${backupStat.bookId}")
            }
        }

        // 3. Импортируем закладки
        backupData.bookmarks.forEach { backupBookmark ->
            val newBookId = bookIdMap[backupBookmark.bookId]
            if (newBookId != null) {
                val bookmark = Bookmark(
                    id = null,
                    creation = backupBookmark.creation,
                    bookId = newBookId,
                    resourceIndex = backupBookmark.resourceIndex,
                    resourceHref = backupBookmark.resourceHref,
                    resourceType = backupBookmark.resourceType,
                    resourceTitle = backupBookmark.resourceTitle,
                    location = backupBookmark.location,
                    locatorText = backupBookmark.locatorText
                )
                bookRepository.insertBookmark(bookmark)
                Timber.d("Imported bookmark: bookId=$newBookId")
            }
        }

        // 4. Импортируем выделения
        backupData.highlights.forEach { backupHighlight ->
            val newBookId = bookIdMap[backupHighlight.bookId]
            if (newBookId != null) {
                val style = Highlight.Style.getOrDefault(backupHighlight.style)
                val locations = try {
                    org.json.JSONObject(backupHighlight.locations)
                } catch (e: Exception) {
                    org.json.JSONObject()
                }
                val text = try {
                    org.json.JSONObject(backupHighlight.text)
                } catch (e: Exception) {
                    org.json.JSONObject()
                }

                val highlight = Highlight(
                    bookId = newBookId,
                    style = style,
                    tint = backupHighlight.tint,
                    href = backupHighlight.href,
                    type = backupHighlight.type,
                    title = backupHighlight.title,
                    totalProgression = backupHighlight.totalProgression,
                    locations = Locator.Locations.fromJSON(locations),
                    text = Locator.Text.fromJSON(text),
                    annotation = backupHighlight.annotation
                )
                bookRepository.insertHighlight(highlight)
                Timber.d("Imported highlight: bookId=$newBookId")
            }
        }

        // 5. Импортируем заметки (они не зависят от bookId)
        backupData.notes.forEach { backupNote ->
            val note = Note(
                content = backupNote.content,
                myComment = backupNote.myComment,
                bookTitle = backupNote.bookTitle,
                bookAuthor = backupNote.bookAuthor,
                category = backupNote.category,
                creationDate = backupNote.creationDate
            )
            bookRepository.insertNote(note)
            Timber.d("Imported note: ${backupNote.bookTitle}")
        }

        // 6. Импортируем слова
        backupData.vocabulary.forEach { backupWord ->
            val word = Vocabulary(
                sourceWord = backupWord.sourceWord,
                translatedWord = backupWord.translatedWord,
                sourceLanguage = backupWord.sourceLanguage,
                targetLanguage = backupWord.targetLanguage,
                createdDate = backupWord.createdDate
            )
            bookRepository.insertWord(word)
        }

        // 7. Импортируем записи сна
        backupData.sleepRecords.forEach { backupRecord ->
            try {
                val date = LocalDate.parse(backupRecord.date)
                val wakeTime = backupRecord.wakeTime?.let { LocalTime.parse(it) }
                val bedTime = backupRecord.bedTime?.let { LocalTime.parse(it) }

                val record = SleepRecord(
                    date = date,
                    wakeTime = wakeTime,
                    bedTime = bedTime,
                    isManual = backupRecord.isManual,
                    synced = backupRecord.synced,
                    createdAt = backupRecord.createdAt,
                    updatedAt = backupRecord.updatedAt
                )
                sleepRepository.insertRecord(record)
                Timber.d("Imported sleep record: $date")
            } catch (e: Exception) {
                Timber.e(e, "Failed to import sleep record")
            }
        }

        // 8. Импортируем каталоги
        backupData.catalogs.forEach { backupCatalog ->
            val catalog = Catalog(
                title = backupCatalog.title,
                href = backupCatalog.href,
                type = backupCatalog.type
            )
            app.bookRepository.insertCatalog(catalog)
        }

        Timber.d("Database import completed. Imported ${bookIdMap.size} books, ${backupData.readingStats.size} stats")
    }

    private suspend fun copyBooksToStorage(sourceDir: File) {
        val storageDir = app.storageDir
        sourceDir.listFiles()?.forEach { sourceFile ->
            try {
                val targetFile = File(storageDir, sourceFile.name)
                sourceFile.copyTo(targetFile, overwrite = true)
                Timber.d("Restored book file: ${sourceFile.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore book file: ${sourceFile.name}")
            }
        }
    }

    private suspend fun copyCoversToStorage(sourceDir: File) {
        val coversDir = app.storageDir.resolve("covers")
        coversDir.mkdirs()

        sourceDir.listFiles()?.forEach { sourceFile ->
            try {
                val targetFile = File(coversDir, sourceFile.name)
                sourceFile.copyTo(targetFile, overwrite = true)
                Timber.d("Restored cover: ${sourceFile.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore cover: ${sourceFile.name}")
            }
        }
    }
}