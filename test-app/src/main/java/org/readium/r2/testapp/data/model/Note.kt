package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "content")
    val content: String,  // Сама заметка (выделенный текст или введенный пользователем)

    @ColumnInfo(name = "my_comment")
    val myComment: String? = null,  // Мой комментарий (можно добавить позже)

    @ColumnInfo(name = "book_title")
    val bookTitle: String,

    @ColumnInfo(name = "book_author")
    val bookAuthor: String? = null,

    @ColumnInfo(name = "category")
    val category: String = "Общее",

    @ColumnInfo(name = "creation_date")
    val creationDate: Long = System.currentTimeMillis()
)