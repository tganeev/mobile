package org.readium.r2.testapp.bookshelf

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Book

class EditBookDialogFragment : DialogFragment() {
    private val bookshelfViewModel: BookshelfViewModel by activityViewModels()
    private lateinit var book: Book
    private lateinit var titleInput: TextInputEditText
    private lateinit var authorInput: TextInputEditText
    private lateinit var titleLayout: TextInputLayout
    // Добавляем поле для страниц
    private lateinit var pagesInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            book = it.getSerializable(ARG_BOOK) as Book
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_edit_book, null)

        // Находим views
        val titleTextInputLayout = view.findViewById<TextInputLayout>(R.id.title_text_input_layout)
        titleLayout = titleTextInputLayout
        titleInput = view.findViewById(R.id.edit_title)
        authorInput = view.findViewById(R.id.edit_author)

        // Инициализируем новое поле
        pagesInput = view.findViewById(R.id.edit_pages_read)

        // Заполняем текущими значениями
        titleInput.setText(book.title ?: "")
        authorInput.setText(book.author ?: "")
        // Устанавливаем текущее значение страниц (если 0, оставляем поле пустым для удобства или ставим 0)
        pagesInput.setText(if (book.pagesRead > 0) book.pagesRead.toString() else "")

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Редактирование книги")
            .setView(view)
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Сохранить") { dialog, _ ->
                saveChanges()
            }
            .create()

        // Настраиваем валидацию после создания диалога
        dialog.setOnShowListener {
            val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = !titleInput.text.isNullOrBlank()
            titleInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val isValid = !s.isNullOrBlank()
                    positiveButton.isEnabled = isValid
                    if (isValid) {
                        titleLayout.error = null
                    } else {
                        titleLayout.error = "Название не может быть пустым"
                    }
                }
            })
        }
        return dialog
    }

    private fun saveChanges() {
        val newTitle = titleInput.text?.toString()?.trim()
        if (newTitle.isNullOrEmpty()) {
            titleLayout.error = "Название не может быть пустым"
            return
        }

        val newAuthor = authorInput.text?.toString()?.trim()

        // Обрабатываем ввод страниц (если пусто, считаем что 0)
        val newPagesRead = pagesInput.text?.toString()?.toIntOrNull() ?: 0

        // Передаем новое значение в ViewModel
        bookshelfViewModel.updateBookMetadata(book.id!!, newTitle, newAuthor, newPagesRead)
        dismiss()
    }

    companion object {
        private const val ARG_BOOK = "book"
        fun newInstance(book: Book): EditBookDialogFragment {
            val fragment = EditBookDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_BOOK, book)
            fragment.arguments = args
            return fragment
        }
    }
}