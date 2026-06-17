package org.readium.r2.testapp.data.model

import com.google.gson.annotations.SerializedName

data class NoteExport(
    @SerializedName("exportDate")
    val exportDate: String,

    @SerializedName("appVersion")
    val appVersion: String,

    @SerializedName("notes")
    val notes: List<NoteData>
)

data class NoteData(
    @SerializedName("content")
    val content: String,

    @SerializedName("myComment")
    val myComment: String?,

    @SerializedName("bookTitle")
    val bookTitle: String,

    @SerializedName("bookAuthor")
    val bookAuthor: String?,

    @SerializedName("category")
    val category: String,

    @SerializedName("creationDate")
    val creationDate: Long
) {
    fun toNote(): Note {
        return Note(
            content = content,
            myComment = myComment,
            bookTitle = bookTitle,
            bookAuthor = bookAuthor,
            category = category,
            creationDate = creationDate
        )
    }
}