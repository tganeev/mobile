package org.readium.r2.testapp.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.data.model.Note
import org.readium.r2.testapp.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var notes: List<Note> = emptyList()

    fun submitList(list: List<Note>) {
        notes = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding, onNoteClick, onNoteLongClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position], position + 1)
    }

    override fun getItemCount(): Int = notes.size

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onClick: (Note) -> Unit,
        private val onLongClick: (Note) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note, position: Int) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(note.creationDate))

            // 1. Порядковый номер
            binding.noteNumber.text = position.toString()

            // 2. Содержание заметки
            binding.noteContent.text = note.content

            // 3. Дата
            binding.noteDate.text = formattedDate

            // 4. Название книги
            binding.noteBook.text = note.bookTitle

            // 5. Автор
            binding.noteAuthor.text = note.bookAuthor ?: "—"

            // 6. Категория
            binding.noteCategory.text = note.category

            // 7. Мой комментарий (если есть)
            if (!note.myComment.isNullOrEmpty()) {
                binding.noteMyComment.text = "💬 ${note.myComment}"
                binding.noteMyComment.visibility = android.view.View.VISIBLE
            } else {
                binding.noteMyComment.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(note) }
            binding.root.setOnLongClickListener {
                onLongClick(note)
                true
            }
        }
    }
}