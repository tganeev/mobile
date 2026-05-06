package org.readium.r2.testapp.vocabulary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App
import org.readium.r2.testapp.data.model.Vocabulary

class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App

    private val _words = MutableStateFlow<List<Vocabulary>>(emptyList())
    val words: StateFlow<List<Vocabulary>> = _words.asStateFlow()

    init {
        loadAllWords()
    }

    fun loadAllWords() {
        viewModelScope.launch {
            app.bookRepository.getAllWords().collect { words ->
                _words.value = words
            }
        }
    }

    fun searchWords(query: String) {
        viewModelScope.launch {
            app.bookRepository.searchWords(query).collect { words ->
                _words.value = words
            }
        }
    }

    suspend fun deleteWord(id: Long) {
        app.bookRepository.deleteWord(id)
        loadAllWords()
    }

    suspend fun deleteAllWords() {
        app.bookRepository.deleteAllWords()
        loadAllWords()
    }

    suspend fun addWord(sourceWord: String, translatedWord: String) {
        val word = Vocabulary(
            sourceWord = sourceWord,
            translatedWord = translatedWord
        )
        app.bookRepository.insertWord(word)
        loadAllWords()
    }
}