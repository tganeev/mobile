package org.readium.r2.testapp.notes

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.testapp.Application as App
import org.readium.r2.testapp.BuildConfig
import org.readium.r2.testapp.data.model.Note
import org.readium.r2.testapp.data.model.NoteData
import org.readium.r2.testapp.data.model.NoteExport
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    init {
        loadAllNotes()
    }

    fun loadAllNotes() {
        viewModelScope.launch {
            app.bookRepository.getAllNotes().collect { notes ->
                _notes.value = notes
            }
        }
    }

    fun searchNotes(query: String) {
        viewModelScope.launch {
            app.bookRepository.searchNotes(query).collect { notes ->
                _notes.value = notes
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            app.bookRepository.updateNote(note)
            loadAllNotes()
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            app.bookRepository.deleteNote(noteId)
            loadAllNotes()
        }
    }

    fun deleteAllNotes() {
        viewModelScope.launch {
            app.bookRepository.deleteAllNotes()
            loadAllNotes()
        }
    }

    // ===== ЭКСПОРТ =====

    suspend fun exportNotesToFile(file: File): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                _isExporting.value = true

                // Получаем все заметки
                val notesList = app.bookRepository.getAllNotes().first()

                if (notesList.isEmpty()) {
                    return@withContext ExportResult.Error("Нет заметок для экспорта")
                }

                // Формируем данные для экспорта
                val noteDataList = notesList.map { note ->
                    NoteData(
                        content = note.content,
                        myComment = note.myComment,
                        bookTitle = note.bookTitle,
                        bookAuthor = note.bookAuthor,
                        category = note.category,
                        creationDate = note.creationDate
                    )
                }

                val exportData = NoteExport(
                    exportDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    appVersion = BuildConfig.VERSION_NAME,
                    notes = noteDataList
                )

                // Записываем в файл
                val json = gson.toJson(exportData)
                FileWriter(file).use { writer ->
                    writer.write(json)
                }

                _isExporting.value = false
                ExportResult.Success(notesList.size)

            } catch (e: Exception) {
                Log.e("NotesViewModel", "Export failed", e)
                _isExporting.value = false
                ExportResult.Error(e.message ?: "Неизвестная ошибка при экспорте")
            }
        }
    }

    // ===== ИМПОРТ =====

    suspend fun importNotesFromFile(file: File): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                _isImporting.value = true

                // Проверяем, что файл существует и читается
                if (!file.exists() || !file.canRead()) {
                    return@withContext ImportResult.Error("Файл недоступен для чтения")
                }

                // Читаем и парсим JSON
                val json = FileReader(file).use { reader ->
                    reader.readText()
                }

                val exportData = gson.fromJson(json, NoteExport::class.java)
                    ?: return@withContext ImportResult.Error("Неверный формат файла")

                // Проверяем структуру
                if (exportData.notes.isEmpty()) {
                    return@withContext ImportResult.Error("Файл не содержит заметок")
                }

                // Конвертируем и сохраняем в БД
                var importedCount = 0
                for (noteData in exportData.notes) {
                    try {
                        val note = noteData.toNote()
                        app.bookRepository.insertNote(note)
                        importedCount++
                    } catch (e: Exception) {
                        Log.e("NotesViewModel", "Failed to import note: ${noteData.content}", e)
                    }
                }

                _isImporting.value = false

                // Обновляем список заметок в UI
                loadAllNotes()

                ImportResult.Success(importedCount, exportData.notes.size)

            } catch (e: Exception) {
                Log.e("NotesViewModel", "Import failed", e)
                _isImporting.value = false
                ImportResult.Error(e.message ?: "Неизвестная ошибка при импорте")
            }
        }
    }

    // ===== РЕЗУЛЬТАТЫ ОПЕРАЦИЙ =====

    sealed class ExportResult {
        data class Success(val count: Int) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }

    sealed class ImportResult {
        data class Success(val imported: Int, val total: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun clearImportResult() {
        _importResult.value = null
    }
}