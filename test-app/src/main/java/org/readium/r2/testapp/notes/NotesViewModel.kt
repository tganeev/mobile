package org.readium.r2.testapp.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App
import org.readium.r2.testapp.data.model.Note

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

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
}