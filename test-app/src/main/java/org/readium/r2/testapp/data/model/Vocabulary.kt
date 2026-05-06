package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "vocabulary")
data class Vocabulary(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "source_word")
    val sourceWord: String,  // Слово/фраза на исходном языке

    @ColumnInfo(name = "translated_word")
    val translatedWord: String,  // Перевод

    @ColumnInfo(name = "source_language")
    val sourceLanguage: String = "en",  // Код языка оригинала

    @ColumnInfo(name = "target_language")
    val targetLanguage: String = "ru",  // Код языка перевода

    @ColumnInfo(name = "created_date")
    val createdDate: Long = System.currentTimeMillis()
)